/*
 * Author: Bong Xin Yee
 * Implements a dynamically resized array-based LIFO stack without
 * java.util.Stack.
 */
package adt;

import java.util.NoSuchElementException;
import java.util.Objects;

public class ArrayStack<T> implements StackInterface<T> {
    private static final int DEFAULT_CAPACITY = 10;

    private Object[] elements;
    private int size;

    // Creates an empty stack with room for a small number of items.
    public ArrayStack() {
        elements = new Object[DEFAULT_CAPACITY];
    }

    @Override
    // Places a new item at the top of the stack.
    public void push(T item) {
        Objects.requireNonNull(item, "Stack does not accept null items.");
        ensureCapacity();
        elements[size] = item;
        size++;
    }

    @Override
    // Removes and returns the item currently at the top of the stack.
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty.");
        }
        int topIndex = size - 1;
        T item = elementAt(topIndex);
        elements[topIndex] = null;
        size--;
        return item;
    }

    @Override
    // Reads the top item without removing it.
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty.");
        }
        return elementAt(size - 1);
    }

    @Override
    // Checks whether the stack currently holds any items.
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    // Returns how many items are currently stored.
    public int size() {
        return size;
    }

    // Doubles the backing array when no free position remains.
    private void ensureCapacity() {
        if (size == elements.length) {
            Object[] expanded = new Object[elements.length * 2];
            System.arraycopy(elements, 0, expanded, 0, elements.length);
            elements = expanded;
        }
    }

    // Reads and casts one stored item back to its original type.
    @SuppressWarnings("unchecked")
    private T elementAt(int index) {
        return (T) elements[index];
    }
}