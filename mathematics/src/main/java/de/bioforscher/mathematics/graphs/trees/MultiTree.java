package de.bioforscher.mathematics.graphs.trees;

public class MultiTree<T> {

    private MultiTreeNode<T> root;

    public MultiTree(T rootData) {
        this.root = new MultiTreeNode<>(rootData);
    }

    public MultiTreeNode<T> getRoot() {
        return this.root;
    }

    public void setRoot(MultiTreeNode<T> root) {
        this.root = root;
    }

    public void addChildToRoot(MultiTreeNode<T> child) {
        this.root.addChild(child);
    }

    public void traversePreOrder(MultiTreeNode<T> node) {
        if (node == null) {
            return;
        }
        // visit() here
        node.getChildren().forEach(this::traversePreOrder);
    }

    public void traverseInOrder(MultiTreeNode<T> node) {
        if (node == null) {
            return;
        }
        for (MultiTreeNode<T> n : node.getChildren()) {
            traverseInOrder(n);
            // visit() here
        }
    }

}
