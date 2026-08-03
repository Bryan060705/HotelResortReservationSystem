/*
 * Author: Bong Xin Yee
 * Implements a dynamically resized array-based LIFO stack 
 */
package adt;

import java.util.NoSuchElementException;

public class ArrayStack<T> implements StackInterface<T> {

    private static final int DEFAULT_CAPACITY = 10;

    private Object[] elements;
    private int size;

    public ArrayStack() {
        elements = new Object[DEFAULT_CAPACITY];
        size = 0;
    }

    @Override
    public void push(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        ensureCapacity();
        elements[size++] = item;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty.");
        }

        int topIndex = size - 1;
        T item = (T) elements[topIndex];
        elements[topIndex] = null; // Prevent memory leak
        size--;

        return item;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty.");
        }
        return (T) elements[size - 1];
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    private void ensureCapacity() {
        if (size == elements.length) {
            Object[] newArray = new Object[elements.length * 2];
            for (int i = 0; i < elements.length; i++) {
                newArray[i] = elements[i];
            }
            elements = newArray;
        }
    }
}