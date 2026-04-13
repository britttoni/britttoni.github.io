package com.example.cs360_project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

//class supports sorting, filtering, building lookup maps,and binary search for exact matches.

public class InventoryUtils {

    //Sorts inventory items alphabetically by item name.

    public static void sortByName(ArrayList<InventoryItem> items) {
        Collections.sort(items, Comparator.comparing(item -> item.getName().toLowerCase()));
    }

    //Sorts inventory items by quantity in descending order.

    public static void sortByQuantityDescending(ArrayList<InventoryItem> items) {
        Collections.sort(items, (item1, item2) ->
                Integer.compare(item2.getQuantity(), item1.getQuantity()));
    }

    //Filters inventory items by checking whether the item name contains the search input

    public static ArrayList<InventoryItem> filterByName(ArrayList<InventoryItem> items, String query) {
        ArrayList<InventoryItem> filtered = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(items);
            return filtered;
        }

        String lowerQuery = query.toLowerCase().trim();

        for (InventoryItem item : items) {
            if (item.getName().toLowerCase().contains(lowerQuery)) {
                filtered.add(item);
            }
        }

        return filtered;
    }

    //Builds a HashMap for faster lookup by item ID.

    public static HashMap<Integer, InventoryItem> buildItemMap(ArrayList<InventoryItem> items) {
        HashMap<Integer, InventoryItem> map = new HashMap<>();

        for (InventoryItem item : items) {
            map.put(item.getId(), item);
        }

        return map;
    }

    // Performs binary search for an exact item name match.

    public static InventoryItem binarySearchByExactName(ArrayList<InventoryItem> sortedItems, String targetName) {
        if (sortedItems == null || sortedItems.isEmpty() || targetName == null) {
            return null;
        }

        int left = 0;
        int right = sortedItems.size() - 1;
        String searchTarget = targetName.toLowerCase().trim();

        while (left <= right) {
            int middle = (left + right) / 2;
            String middleName = sortedItems.get(middle).getName().toLowerCase();

            int comparison = middleName.compareTo(searchTarget);

            if (comparison == 0) {
                return sortedItems.get(middle);
            } else if (comparison < 0) {
                left = middle + 1;
            } else {
                right = middle - 1;
            }
        }

        return null;
    }
}