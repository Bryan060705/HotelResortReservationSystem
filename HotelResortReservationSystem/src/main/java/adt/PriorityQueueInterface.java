/*
 * Author: Bryan Won Chu Ming
 * ADT acknowledgement: This interface and its implementation were written
 * specifically for this assignment. The max-heap concept follows the standard
 * binary heap algorithm taught in data structures and algorithms courses.
 */
package adt;

import java.util.function.Predicate;

public interface PriorityQueueInterface<T> {
    /**
     * Description: Adds a new item according to its priority.
     * Precondition: The item must not be null.
     * Postcondition: The item is stored and the size increases by one.
     */
    void enqueue(T item);

    /**
     * Description: Removes and returns the highest-priority item.
     * Precondition: The priority queue must not be empty.
     * Postcondition: The highest-priority item is removed and the size
     * decreases by one.
     */
    T dequeue();

    /**
     * Description: Returns the highest-priority item without removing it.
     * Precondition: The priority queue must not be empty.
     * Postcondition: The priority queue remains unchanged.
     */
    T getFront();

    /**
     * Description: Removes the highest-priority item that matches a condition.
     * Precondition: The condition must not be null.
     * Postcondition: The matching item is removed, or null is returned when
     * no item matches.
     */
    T removeHighestMatching(Predicate<? super T> condition);

    /**
     * Description: Returns the item stored at a selected position.
     * Precondition: The position must be between zero and size minus one.
     * Postcondition: The priority queue remains unchanged.
     */
    T getEntry(int position);

    /**
     * Description: Checks whether an equal item is stored.
     * Precondition: None.
     * Postcondition: The priority queue remains unchanged.
     */
    boolean contains(T item);

    /**
     * Description: Creates an independent copy of this priority queue.
     * Precondition: None.
     * Postcondition: This priority queue remains unchanged.
     */
    PriorityQueueInterface<T> copy();

    /**
     * Description: Returns the number of stored items.
     * Precondition: None.
     * Postcondition: The priority queue remains unchanged.
     */
    int size();

    /**
     * Description: Checks whether no items are stored.
     * Precondition: None.
     * Postcondition: The priority queue remains unchanged.
     */
    boolean isEmpty();

    /**
     * Description: Removes every stored item.
     * Precondition: None.
     * Postcondition: The priority queue is empty and its size is zero.
     */
    void clear();
}
