/*
 * Author: Bryan Won Chu Ming
 * Implements a dynamically resized binary max heap without java.util.PriorityQueue.
 */
package adt;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

public class HeapPriorityQueue<T> implements PriorityQueueInterface<T> {
    private static final int DEFAULT_CAPACITY = 10;

    private final Comparator<? super T> comparator;
    private Object[] elements;
    private int size;

    // Creates an empty max heap and receives the priority comparison rule.
    public HeapPriorityQueue(Comparator<? super T> comparator) {
        this.comparator = Objects.requireNonNull(comparator, "Comparator is required.");
        elements = new Object[DEFAULT_CAPACITY];
    }

    @Override
    // Adds a new item and moves it upward until the heap rule is correct.
    public void enqueue(T item) {
        Objects.requireNonNull(item, "Priority queue does not accept null items.");
        ensureCapacity();
        elements[size] = item;
        siftUp(size);
        size++;
    }

    @Override
    // Removes the root item, which has the highest priority.
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Priority queue is empty.");
        }
        return removeAt(0);
    }

    @Override
    // Reads the root item without removing it.
    public T getFront() {
        if (isEmpty()) {
            throw new NoSuchElementException("Priority queue is empty.");
        }
        return elementAt(0);
    }

    @Override
    // Uses linear searching to remove the best item that matches a condition.
    public T removeHighestMatching(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "Search condition is required.");
        int bestIndex = -1;
        for (int index = 0; index < size; index++) {
            T candidate = elementAt(index);
            if (condition.test(candidate)
                    && (bestIndex < 0
                    || comparator.compare(candidate, elementAt(bestIndex)) > 0)) {
                bestIndex = index;
            }
        }
        return bestIndex < 0 ? null : removeAt(bestIndex);
    }

    @Override
    // Returns an item from one position of the internal array.
    public T getEntry(int position) {
        if (position < 0 || position >= size) {
            throw new IndexOutOfBoundsException("Invalid position: " + position);
        }
        return elementAt(position);
    }

    @Override
    // Uses linear searching and the entity's equals method.
    public boolean contains(T item) {
        for (int index = 0; index < size; index++) {
            if (Objects.equals(elementAt(index), item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    // Makes an independent copy without changing the original heap.
    public PriorityQueueInterface<T> copy() {
        HeapPriorityQueue<T> copy = new HeapPriorityQueue<>(comparator);
        for (int index = 0; index < size; index++) {
            copy.enqueue(elementAt(index));
        }
        return copy;
    }

    @Override
    // Returns the current number of heap items.
    public int size() {
        return size;
    }

    @Override
    // Checks whether the heap contains no items.
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    // Resets the heap to an empty array.
    public void clear() {
        elements = new Object[DEFAULT_CAPACITY];
        size = 0;
    }

    // Removes an item at a selected array position and repairs the heap.
    private T removeAt(int index) {
        T removed = elementAt(index);
        int lastIndex = size - 1;
        Object replacement = elements[lastIndex];
        elements[lastIndex] = null;
        size--;

        if (index < size) {
            elements[index] = replacement;
            int parentIndex = parentIndex(index);
            if (index > 0
                    && comparator.compare(elementAt(index), elementAt(parentIndex)) > 0) {
                siftUp(index);
            } else {
                siftDown(index);
            }
        }
        return removed;
    }

    // Moves an item upward while it has higher priority than its parent.
    private void siftUp(int startingIndex) {
        int currentIndex = startingIndex;
        while (currentIndex > 0) {
            int parentIndex = parentIndex(currentIndex);
            if (comparator.compare(elementAt(currentIndex), elementAt(parentIndex)) <= 0) {
                break;
            }
            swap(currentIndex, parentIndex);
            currentIndex = parentIndex;
        }
    }

    // Moves an item downward while a child has higher priority.
    private void siftDown(int startingIndex) {
        int currentIndex = startingIndex;
        while (leftChildIndex(currentIndex) < size) {
            int largerChildIndex = leftChildIndex(currentIndex);
            int rightChildIndex = rightChildIndex(currentIndex);
            if (rightChildIndex < size
                    && comparator.compare(elementAt(rightChildIndex),
                            elementAt(largerChildIndex)) > 0) {
                largerChildIndex = rightChildIndex;
            }
            if (comparator.compare(elementAt(currentIndex),
                    elementAt(largerChildIndex)) >= 0) {
                break;
            }
            swap(currentIndex, largerChildIndex);
            currentIndex = largerChildIndex;
        }
    }

    // Calculates the parent position of a child.
    private int parentIndex(int childIndex) {
        return (childIndex - 1) / 2;
    }

    // Calculates the left child position of a parent.
    private int leftChildIndex(int parentIndex) {
        return 2 * parentIndex + 1;
    }

    // Calculates the right child position of a parent.
    private int rightChildIndex(int parentIndex) {
        return 2 * parentIndex + 2;
    }

    // Exchanges two items in the heap array.
    private void swap(int firstIndex, int secondIndex) {
        Object temporary = elements[firstIndex];
        elements[firstIndex] = elements[secondIndex];
        elements[secondIndex] = temporary;
    }

    // Doubles the array size when there is no free space.
    private void ensureCapacity() {
        if (size < elements.length) {
            return;
        }
        Object[] expanded = new Object[elements.length * 2];
        System.arraycopy(elements, 0, expanded, 0, elements.length);
        elements = expanded;
    }

    @SuppressWarnings("unchecked")
    // Converts an Object array value back to the generic item type.
    private T elementAt(int index) {
        return (T) elements[index];
    }
}
