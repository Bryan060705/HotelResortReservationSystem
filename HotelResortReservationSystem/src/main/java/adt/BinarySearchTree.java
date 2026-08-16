package adt;

/**
 * BinarySearchTree stub matching FrontDeskServiceControl requirements.
 */

/**
 * NOTE TO TEAMMATE:
 * This placeholder file was created to resolve project-wide compilation errors
 * caused by missing ADT dependencies in FrontDeskServiceControl and HotelSystemUI.
 *
 * Please implement the complete Binary Search Tree logic (insert, search, delete, 
 * inOrder, etc.) and verify that the application compiles and runs successfully.
 */

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BinarySearchTree<T extends Comparable<T>>
        implements BinarySearchTreeInterface<T> {

    /**
     * Represents one node in the Binary Search Tree.
     */
    private static class Node<T> {

        private T data;
        private Node<T> left;
        private Node<T> right;

        private Node(T data) {
            this.data = data;
        }
    }

    private Node<T> root;
    private int size;

    /**
     * Creates an empty Binary Search Tree.
     */
    public BinarySearchTree() {
        root = null;
        size = 0;
    }

    /**
     * Inserts an item into the tree.
     *
     * @param item item to insert
     */
    @Override
    public void insert(T item) {
        Objects.requireNonNull(item, "Item cannot be null.");

        if (root == null) {
            root = new Node<>(item);
            size++;
            return;
        }

        Node<T> current = root;

        while (true) {
            int comparison = item.compareTo(current.data);

            if (comparison == 0) {
                throw new IllegalArgumentException(
                        "Duplicate item is not allowed.");
            }

            if (comparison < 0) {
                if (current.left == null) {
                    current.left = new Node<>(item);
                    size++;
                    return;
                }

                current = current.left;

            } else {
                if (current.right == null) {
                    current.right = new Node<>(item);
                    size++;
                    return;
                }

                current = current.right;
            }
        }
    }

    /**
     * Searches for an item using Binary Search Tree traversal.
     *
     * Average-case time complexity: O(log n)
     * Worst-case time complexity: O(n)
     *
     * @param item item to search for
     * @return matching item or null if not found
     */
    @Override
    public T search(T item) {
        if (item == null) {
            return null;
        }

        Node<T> current = root;

        while (current != null) {
            int comparison = item.compareTo(current.data);

            if (comparison == 0) {
                return current.data;
            }

            if (comparison < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }

        return null;
    }

    /**
     * Checks whether an item exists in the tree.
     *
     * @param item item to search for
     * @return true if found
     */
    @Override
    public boolean contains(T item) {
        return search(item) != null;
    }

    /**
     * Deletes an item from the tree.
     *
     * Handles:
     * 1. Leaf node
     * 2. Node with one child
     * 3. Node with two children
     *
     * @param item item to delete
     * @return true if deleted
     */
    @Override
    public boolean delete(T item) {
        if (item == null || root == null) {
            return false;
        }

        int originalSize = size;

        root = deleteNode(root, item);

        return size != originalSize;
    }

    /**
     * Recursively deletes a node.
     *
     * @param node current node
     * @param item item to delete
     * @return updated subtree
     */
    private Node<T> deleteNode(Node<T> node, T item) {

        if (node == null) {
            return null;
        }

        int comparison = item.compareTo(node.data);

        if (comparison < 0) {

            node.left = deleteNode(node.left, item);

        } else if (comparison > 0) {

            node.right = deleteNode(node.right, item);

        } else {

            // Case 1: No child
            if (node.left == null && node.right == null) {
                size--;
                return null;
            }

            // Case 2: Only right child
            if (node.left == null) {
                size--;
                return node.right;
            }

            // Case 2: Only left child
            if (node.right == null) {
                size--;
                return node.left;
            }

            // Case 3: Two children
            Node<T> successor = findMinimum(node.right);

            node.data = successor.data;

            node.right = deleteNode(node.right, successor.data);
        }

        return node;
    }

    /**
     * Finds the smallest node in a subtree.
     *
     * @param node subtree root
     * @return smallest node
     */
    private Node<T> findMinimum(Node<T> node) {

        Node<T> current = node;

        while (current.left != null) {
            current = current.left;
        }

        return current;
    }

    /**
     * Returns all items using in-order traversal.
     *
     * In-order traversal produces sorted data.
     *
     * @return sorted array of tree items
     */
    @Override
    @SuppressWarnings("unchecked")
    public T[] inOrder() {

        List<T> result = new ArrayList<>();

        inOrderTraversal(root, result);

        if (result.isEmpty()) {
            return (T[]) new Comparable[0];
        }

        T[] array = (T[]) new Comparable[result.size()];

        return result.toArray(array);
    }

    /**
     * Performs recursive in-order traversal.
     *
     * @param node current node
     * @param result result list
     */
    private void inOrderTraversal(Node<T> node, List<T> result) {

        if (node == null) {
            return;
        }

        inOrderTraversal(node.left, result);

        result.add(node.data);

        inOrderTraversal(node.right, result);
    }

    /**
     * Returns the number of items in the tree.
     *
     * @return tree size
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Checks whether the tree is empty.
     *
     * @return true if empty
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Removes all items from the tree.
     */
    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * Returns a readable tree description.
     *
     * @return tree information
     */
    @Override
    public String toString() {

        if (isEmpty()) {
            return "[Empty Binary Search Tree]";
        }

        StringBuilder builder = new StringBuilder();

        T[] items = inOrder();

        for (T item : items) {
            builder.append(item).append("\n");
        }

        return builder.toString();
    }
}
