package com.cypher.zealth;

public class GroceryItem {
    public String name;
    public boolean checked;

    public static GroceryItem of(String name, boolean checked) {
        GroceryItem i = new GroceryItem();
        i.name = name;
        i.checked = checked;
        return i;
    }
}
