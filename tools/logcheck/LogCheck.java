import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 离线验证：日志存储从「1..N 序号 key」迁移到「GUID key + order 表」，
 * 并按 children 关系重建树。只验证算法与数据流，不依赖 Android / MMKV / Gson。
 */
public class LogCheck {

    // 模拟 LogSave 的存储：key -> 序列化后的日志（这里用简化文本表示）
    static class Store {
        final Map<String, String> kv = new HashMap<>();

        void put(String key, String value) { kv.put(key, value); }
        String get(String key) { return kv.get(key); }
        void remove(String key) { kv.remove(key); }
    }

    // 一条日志的最小模型：uid + children
    static class Log {
        String uid;
        List<String> children = new ArrayList<>();

        Log(String uid) { this.uid = uid; }
    }

    // 用 "uid|child1,child2" 编码，便于在 Store 里存取
    static String encode(Log log) {
        return log.uid + "|" + String.join(",", log.children);
    }

    static Log decode(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("\\|", -1);
        Log log = new Log(parts[0]);
        if (parts.length > 1 && !parts[1].isEmpty()) {
            for (String c : parts[1].split(",")) log.children.add(c);
        }
        return log;
    }

    static final String COUNT = "count";
    static final String ORDER = "order";

    /** 迁移：把 1..N 的 key 改写成 GUID，建立 order 表，并改写 children 引用 */
    static void migrate(Store store) {
        String rawOrder = store.get(ORDER);
        if (rawOrder != null && !rawOrder.isEmpty()) return;
        int count = store.get(COUNT) == null ? 0 : Integer.parseInt(store.get(COUNT));
        if (count <= 0) return;

        Map<String, String> oldToNew = new LinkedHashMap<>();
        Map<String, Log> infos = new LinkedHashMap<>();
        List<String> order = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            String oldKey = String.valueOf(i);
            Log log = decode(store.get(oldKey));
            if (log == null) continue;
            String newUid = UUID.randomUUID().toString();
            oldToNew.put(oldKey, newUid);
            infos.put(newUid, log);
            order.add(newUid);
        }

        for (Map.Entry<String, Log> entry : infos.entrySet()) {
            Log log = entry.getValue();
            log.uid = entry.getKey();
            List<String> mapped = new ArrayList<>();
            for (String child : log.children) {
                mapped.add(oldToNew.getOrDefault(child, child));
            }
            log.children = mapped;
            store.put(log.uid, encode(log));
        }

        for (String oldKey : oldToNew.keySet()) store.remove(oldKey);
        store.put(ORDER, String.join(",", order));
        store.remove(COUNT);
    }

    /** 新写入：始终用 GUID，追加到 order 表 */
    static void addLog(Store store, Log log) {
        if (log.uid == null || log.uid.isEmpty()) log.uid = UUID.randomUUID().toString();
        store.put(log.uid, encode(log));
        List<String> order = order(store);
        if (!order.contains(log.uid)) {
            order.add(log.uid);
            store.put(ORDER, String.join(",", order));
        }
    }

    static List<String> order(Store store) {
        List<String> order = new ArrayList<>();
        String raw = store.get(ORDER);
        if (raw == null || raw.isEmpty()) return order;
        for (String s : raw.split(",")) {
            if (s.isEmpty()) continue;
            if (store.get(s) == null) continue;   // 丢弃失效引用
            order.add(s);
        }
        return order;
    }

    static class Node {
        Log log;
        int depth;
        List<Node> children = new ArrayList<>();

        Node(Log log, int depth) {
            this.log = log;
            this.depth = depth;
        }
    }

    /** 重建树：按 order 遍历，按 children 递归，父节点在前 */
    static List<Node> buildTree(Store store) {
        List<String> order = order(store);
        Map<String, Log> byUid = new LinkedHashMap<>();
        for (String uid : order) {
            Log log = decode(store.get(uid));
            if (log != null) byUid.put(uid, log);
        }
        Set<String> childUids = new HashSet<>();
        for (Log log : byUid.values()) childUids.addAll(log.children);

        List<Node> roots = new ArrayList<>();
        for (String uid : byUid.keySet()) {
            if (childUids.contains(uid)) continue;
            roots.add(buildNode(byUid, uid, 0));
        }
        return roots;
    }

    static Node buildNode(Map<String, Log> byUid, String uid, int depth) {
        Log log = byUid.get(uid);
        Node node = new Node(log, depth);
        if (log == null) return node;
        for (String child : log.children) {
            if (!byUid.containsKey(child)) continue;
            node.children.add(buildNode(byUid, child, depth + 1));
        }
        return node;
    }

    static int failures = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        // 场景：老数据 3 条日志，1 → 2 → 3（父子孙），key 是序号
        Store store = new Store();
        store.put("1", "1|2");
        store.put("2", "2|3");
        store.put("3", "3|");
        store.put(COUNT, "3");

        migrate(store);

        List<String> order = order(store);
        check("迁移后 order 有 3 条", order.size() == 3);
        check("旧序号 key 已删除", store.get("1") == null && store.get("2") == null && store.get("3") == null);
        check("COUNT 已删除", store.get(COUNT) == null);
        check("新 key 都是 GUID 形式", order.stream().allMatch(u -> u.length() > 20 && u.contains("-")));

        Log parent = decode(store.get(order.get(0)));
        check("父日志 children 仍指向子日志的 GUID",
                parent != null && parent.children.size() == 1 && parent.children.get(0).equals(order.get(1)));

        List<Node> roots = buildTree(store);
        check("只有一个根节点", roots.size() == 1);
        if (roots.size() == 1) {
            Node root = roots.get(0);
            check("根深度为 0", root.depth == 0);
            check("一级子节点 1 个", root.children.size() == 1);
            if (root.children.size() == 1) {
                Node child = root.children.get(0);
                check("子节点深度为 1", child.depth == 1);
                check("孙节点深度为 2", child.children.size() == 1 && child.children.get(0).depth == 2);
            }
        }

        // 场景：迁移后再写一条新日志，应追加到末尾且不破坏层级
        Log extra = new Log(null);
        addLog(store, extra);
        List<String> order2 = order(store);
        check("新日志追加到 order 末尾", order2.size() == 4 && order2.get(3).equals(extra.uid));
        check("旧层级不受影响", buildTree(store).size() == 2);

        // 场景：children 里引用一条已删除日志，重建时应忽略而不是显示空行
        Log orphan = decode(store.get(order.get(0)));
        orphan.children.add("deleted-uid");
        store.put(orphan.uid, encode(orphan));
        List<Node> roots3 = buildTree(store);
        boolean orphanSkipped = roots3.stream().allMatch(r -> r.children.stream().noneMatch(c -> c.log == null));
        check("失效的 children 引用被忽略", orphanSkipped);

        System.out.println(failures == 0 ? "\n全部通过" : "\n有 " + failures + " 项失败");
        System.exit(failures == 0 ? 0 : 1);
    }
}
