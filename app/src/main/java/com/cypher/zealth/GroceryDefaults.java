package com.cypher.zealth;

import java.util.ArrayList;
import java.util.List;

public class GroceryDefaults {

    public static List<GroceryCategory> build() {
        List<GroceryCategory> cats = new ArrayList<>();

        cats.add(GroceryCategory.of("Produce", toItems(
                "Leafy greens", "Tomatoes", "Onions", "Garlic", "Berries", "Bananas", "Apples", "Avocados"
        )));
        cats.add(GroceryCategory.of("Proteins", toItems(
                "Eggs", "Beans", "Lentils", "Chicken", "Fish", "Tofu", "Greek yogurt"
        )));
        cats.add(GroceryCategory.of("Whole grains", toItems(
                "Oats", "Brown rice", "Whole wheat bread", "Quinoa"
        )));
        cats.add(GroceryCategory.of("Dairy or alternatives", toItems(
                "Milk", "Yogurt", "Cheese", "Soy milk"
        )));
        cats.add(GroceryCategory.of("Pantry", toItems(
                "Olive oil", "Peanut butter", "Canned tomatoes", "Spices", "Nuts"
        )));
        cats.add(GroceryCategory.of("Hydration", toItems(
                "Water", "Herbal tea"
        )));
        cats.add(GroceryCategory.of("Household", toItems(
                "Soap", "Toothpaste", "Sanitary products"
        )));

        return cats;
    }

    private static List<GroceryItem> toItems(String... names) {
        List<GroceryItem> items = new ArrayList<>();
        for (String n : names) items.add(GroceryItem.of(n, false));
        return items;
    }
}
