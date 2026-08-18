/*
 * Author: CARRET CHONG KAR LOKE
 */

package adt;

public interface BinarySearchTreeInterface<T extends Comparable<T>> {

    /**
     * Adds an item into the Binary Search Tree.
     *
     * @param item item to be added
     */
    void insert(T item);

    /**
     * Searches for an item using the item's natural ordering.
     *
     * @param item item to search for
     * @return the matching item, or null if not found
     */
    T search(T item);

    /**
     * Checks whether the tree contains the specified item.
     *
     * @param item item to search for
     * @return true if the item exists, otherwise false
     */
    boolean contains(T item);

    /**
     * Removes an item from the Binary Search Tree.
     *
     * @param item item to remove
     * @return true if the item was removed, otherwise false
     */
    boolean delete(T item);

    /**
     * Displays all items using in-order traversal.
     *
     * @return an array containing the items in sorted order
     */
    T[] inOrder();

    /**
     * Returns the number of items stored in the tree.
     *
     * @return number of items
     */
    int size();

    /**
     * Checks whether the tree is empty.
     *
     * @return true if empty
     */
    boolean isEmpty();

    /**
     * Removes all items from the tree.
     */
    void clear();
}
