package de.nms.nmsrecipes.model;

import java.util.List;

public record TreeNode(String id, String key, String label, String type, String detail, List<TreeNode> children) {

    public TreeNode {
        children = List.copyOf(children);
    }
}
