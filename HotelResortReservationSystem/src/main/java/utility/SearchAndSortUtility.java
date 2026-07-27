/*
 * Author: Bryan Won Chu Ming
 * Provides reusable searching and sorting algorithms for management reports.
 */
package utility;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public final class SearchAndSortUtility {
    // Prevents the utility class from being created as an object.
    private SearchAndSortUtility() {
    }

    // Checks every item and places matching items into a supplied array.
    public static <T> int linearSearch(T[] items, T[] matches,
            Predicate<? super T> condition) {
        Objects.requireNonNull(items, "Items are required.");
        Objects.requireNonNull(matches, "Result array is required.");
        Objects.requireNonNull(condition, "Search condition is required.");
        if (matches.length < items.length) {
            throw new IllegalArgumentException(
                    "Result array must be large enough for all possible matches.");
        }

        int matchCount = 0;
        for (T item : items) {
            if (condition.test(item)) {
                matches[matchCount++] = item;
            }
        }
        return matchCount;
    }

    // Searches a sorted array by repeatedly checking the middle item.
    public static <T> T binarySearchByTextKey(T[] sortedItems,
            Function<? super T, String> keyExtractor, String targetKey) {
        Objects.requireNonNull(sortedItems, "Sorted items are required.");
        Objects.requireNonNull(keyExtractor, "Key extractor is required.");
        Objects.requireNonNull(targetKey, "Target key is required.");

        int low = 0;
        int high = sortedItems.length - 1;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            T candidate = sortedItems[middle];
            int comparison = keyExtractor.apply(candidate)
                    .compareToIgnoreCase(targetKey.trim());
            if (comparison == 0) {
                return candidate;
            }
            if (comparison < 0) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return null;
    }

    // Sorts an array by dividing it into smaller sections and merging them.
    public static <T> void mergeSort(T[] items,
            Comparator<? super T> comparator) {
        Objects.requireNonNull(items, "Items are required.");
        Objects.requireNonNull(comparator, "Comparator is required.");
        if (items.length < 2) {
            return;
        }
        T[] temporary = items.clone();
        mergeSort(items, temporary, 0, items.length - 1, comparator);
    }

    // Recursively divides an array section into two halves.
    private static <T> void mergeSort(T[] items, T[] temporary,
            int first, int last, Comparator<? super T> comparator) {
        if (first >= last) {
            return;
        }
        int middle = first + (last - first) / 2;
        mergeSort(items, temporary, first, middle, comparator);
        mergeSort(items, temporary, middle + 1, last, comparator);
        merge(items, temporary, first, middle, last, comparator);
    }

    // Combines two sorted array sections into one sorted section.
    private static <T> void merge(T[] items, T[] temporary,
            int first, int middle, int last,
            Comparator<? super T> comparator) {
        for (int index = first; index <= last; index++) {
            temporary[index] = items[index];
        }

        int leftIndex = first;
        int rightIndex = middle + 1;
        int targetIndex = first;
        while (leftIndex <= middle && rightIndex <= last) {
            if (comparator.compare(temporary[leftIndex],
                    temporary[rightIndex]) <= 0) {
                items[targetIndex++] = temporary[leftIndex++];
            } else {
                items[targetIndex++] = temporary[rightIndex++];
            }
        }
        while (leftIndex <= middle) {
            items[targetIndex++] = temporary[leftIndex++];
        }
        while (rightIndex <= last) {
            items[targetIndex++] = temporary[rightIndex++];
        }
    }
}
