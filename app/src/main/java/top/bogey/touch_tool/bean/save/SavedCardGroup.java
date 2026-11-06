package top.bogey.touch_tool.bean.save;

import java.util.HashSet;
import java.util.Set;

/**
 * 蓝图里暂存的卡片组：只记录动作 id，按任务存档，不参与任务本身的导出。
 */
public class SavedCardGroup {
    private String name = "";
    private Set<String> ids = new HashSet<>();

    public SavedCardGroup() {
    }

    public SavedCardGroup(String name, Set<String> ids) {
        this.name = name;
        this.ids = ids;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getIds() {
        if (ids == null) ids = new HashSet<>();
        return ids;
    }

    public void setIds(Set<String> ids) {
        this.ids = ids;
    }
}
