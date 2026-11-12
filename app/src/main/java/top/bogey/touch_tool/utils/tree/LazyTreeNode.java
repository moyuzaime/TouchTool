package top.bogey.touch_tool.utils.tree;

import androidx.annotation.Nullable;

import java.util.List;

public class LazyTreeNode extends TreeNode {
    private final ITreeNodeDataLoader loader;
    private final Object flag;

    public LazyTreeNode(ITreeNodeDataLoader loader, Object flag) {
        this.loader = loader;
        this.flag = flag;
    }

    // 排查用：拿到该节点的存储 flag（日志 uid / 序号）
    public Object getFlag() {
        return flag;
    }

    @Nullable
    @Override
    public ILazyTreeNodeData getData() {
        if (nodeData == null) {
            nodeData = loader.loadData(flag);
            if (nodeData instanceof ILazyTreeNodeData lazyTreeNodeData) {
                for (Object obj : lazyTreeNodeData.getChildrenFlags()) {
                    LazyTreeNode treeNode = new LazyTreeNode(loader, obj);
                    addChild(treeNode);
                    treeNode.setDepth(depth + 1);
                }
            }
        }
        return (ILazyTreeNodeData) nodeData;
    }

    @Override
    public List<TreeNode> getChildren() {
        getData();
        return children;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }
}
