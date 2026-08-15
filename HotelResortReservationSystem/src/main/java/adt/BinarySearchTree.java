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

public class BinarySearchTree<T> {

    private static final Object[] EMPTY_ARRAY = new Object[0];

    public BinarySearchTree() {
    }

    public void insert(T entry) {
        // TODO
    }

    public T search(T key) {
        // TODO
        return null;
    }

    public boolean delete(T entry) {
        // TODO
        return true;
    }

    public Object[] inOrder() {
        // Returns Object[] as expected by FrontDeskServiceControl.getAllGuests()
        return EMPTY_ARRAY;
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public void clear() {
    }
}