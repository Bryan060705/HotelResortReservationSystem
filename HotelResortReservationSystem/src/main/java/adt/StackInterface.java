/*
 * Author: Bong Xin Yee
 * ADT acknowledgement: This interface and its implementation were written
 * specifically for this assignment. The array-based LIFO stack concept
 * follows the standard stack data structure taught in data structures and
 * algorithms courses.
 */
package adt;

public interface StackInterface<T> {
    /**
     * Description: Adds a new item to the top of the stack.
     * Precondition: The item must not be null.
     * Postcondition: The item becomes the new top and the size increases
     * by one.
     */
    void push(T item);

    /**
     * Description: Removes and returns the item at the top of the stack.
     * Precondition: The stack must not be empty.
     * Postcondition: The top item is removed and the size decreases by one.
     */
    T pop();

    /**
     * Description: Returns the item at the top of the stack without
     * removing it.
     * Precondition: The stack must not be empty.
     * Postcondition: The stack remains unchanged.
     */
    T peek();

    /**
     * Description: Checks whether the stack currently holds no items.
     * Precondition: None.
     * Postcondition: The stack remains unchanged.
     */
    boolean isEmpty();

    /**
     * Description: Returns how many items are currently stored.
     * Precondition: None.
     * Postcondition: The stack remains unchanged.
     */
    int size();
}