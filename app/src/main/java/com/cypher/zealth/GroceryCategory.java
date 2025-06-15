package com.cypher.zealth;

import java.util.ArrayList;
import java.util.List;

public class GroceryCategory {
    public String title;
    public boolean expanded;
    public List<GroceryItem> items = new ArrayList<>();

    public static GroceryCategory of(String title, List<GroceryItem> items) {
        GroceryCategory c = new GroceryCategory();
        c.title = title;
        c.items = items;
        c.expanded = false;
        return c;
    }

    public int totalCount() {
        return items == null ? 0 : items.size();
    }

    public int checkedCount() {
        if (items == null) return 0;
        int c = 0;
        for (GroceryItem it : items) if (it.checked) c++;
        return c;
    }
}
