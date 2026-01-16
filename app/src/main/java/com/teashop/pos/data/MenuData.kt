package com.teashop.pos.data

import com.teashop.pos.data.entity.Item
import java.util.UUID

object MenuData {
    val initialItems = listOf(
        // TEA & HOT DRINKS
        Item(UUID.randomUUID().toString(), "Dum Tea", "Tea", globalPrice = 12.0),
        Item(UUID.randomUUID().toString(), "Nattu Sakkarai Tea", "Tea", globalPrice = 15.0),
        Item(UUID.randomUUID().toString(), "Ginger Tea", "Tea", globalPrice = 15.0),
        Item(UUID.randomUUID().toString(), "Masala Tea", "Tea", globalPrice = 15.0),
        Item(UUID.randomUUID().toString(), "Lemon Tea", "Tea", globalPrice = 15.0),
        Item(UUID.randomUUID().toString(), "Ginger Lemon Tea", "Tea", globalPrice = 20.0),
        Item(UUID.randomUUID().toString(), "Coffee", "Coffee", globalPrice = 20.0),
        Item(UUID.randomUUID().toString(), "Sukku Coffee", "Coffee", globalPrice = 25.0),
        Item(UUID.randomUUID().toString(), "Kullad Tea", "Tea", globalPrice = 20.0),
        Item(UUID.randomUUID().toString(), "Kashmiri Chai", "Tea", globalPrice = 20.0),
        Item(UUID.randomUUID().toString(), "Green Tea", "Tea", globalPrice = 25.0),
        Item(UUID.randomUUID().toString(), "Boost", "Health Drink", globalPrice = 25.0),
        Item(UUID.randomUUID().toString(), "Horlicks", "Health Drink", globalPrice = 25.0),

        // MILK & SPECIAL DRINKS
        Item(UUID.randomUUID().toString(), "Badam Milk", "Milk Drink", globalPrice = 20.0),
        Item(UUID.randomUUID().toString(), "Rose Milk", "Flavoured Milk", globalPrice = 45.0),
        Item(UUID.randomUUID().toString(), "Chilled Badam Milk", "Flavoured Milk", globalPrice = 45.0),

        // MAGGIE
        Item(UUID.randomUUID().toString(), "Veg Maggie", "Maggie", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Egg Maggie", "Maggie", globalPrice = 60.0),

        // FRESH JUICES
        Item(UUID.randomUUID().toString(), "Lemon Juice", "Fresh Juice", globalPrice = 20.0),
        Item(UUID.randomUUID().toString(), "Watermelon Juice", "Fresh Juice", globalPrice = 30.0),
        Item(UUID.randomUUID().toString(), "Muskmelon Juice", "Fresh Juice", globalPrice = 40.0),
        Item(UUID.randomUUID().toString(), "Sathukudi Juice", "Fresh Juice", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Apple Juice", "Fresh Juice", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Pomegranate Juice", "Fresh Juice", globalPrice = 60.0),
        Item(UUID.randomUUID().toString(), "Red Banana Juice", "Fresh Juice", globalPrice = 60.0),
        Item(UUID.randomUUID().toString(), "Fig Juice", "Fresh Juice", globalPrice = 60.0),

        // MILK SHAKES
        Item(UUID.randomUUID().toString(), "Vanilla Shake", "Milkshake", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Strawberry Shake", "Milkshake", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Chocolate Shake", "Milkshake", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Mango Milkshake", "Milkshake", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Butterscotch Shake", "Milkshake", globalPrice = 55.0),
        Item(UUID.randomUUID().toString(), "Dairy Milk Shake", "Milkshake", globalPrice = 55.0),
        Item(UUID.randomUUID().toString(), "5 Star Milkshake", "Milkshake", globalPrice = 55.0),
        Item(UUID.randomUUID().toString(), "Oreo Milkshake", "Milkshake", globalPrice = 60.0),
        Item(UUID.randomUUID().toString(), "Pista Milkshake", "Milkshake", globalPrice = 60.0),
        Item(UUID.randomUUID().toString(), "Blackcurrant Shake", "Milkshake", globalPrice = 60.0),
        Item(UUID.randomUUID().toString(), "KitKat Milkshake", "Milkshake", globalPrice = 70.0),
        Item(UUID.randomUUID().toString(), "Red Banana Shake", "Milkshake", globalPrice = 70.0),
        Item(UUID.randomUUID().toString(), "Dates Milkshake", "Milkshake", globalPrice = 70.0),

        // COOLERS
        Item(UUID.randomUUID().toString(), "Nannari Sarbath", "Cooler", globalPrice = 30.0),
        Item(UUID.randomUUID().toString(), "Pal Sarbath", "Cooler", globalPrice = 30.0),
        Item(UUID.randomUUID().toString(), "Lemon Mint Cooler", "Cooler", globalPrice = 35.0),
        Item(UUID.randomUUID().toString(), "Matka Lassi", "Cooler", globalPrice = 45.0),
        Item(UUID.randomUUID().toString(), "Cold Coffee", "Cooler", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Fizz Mojito", "Cooler", globalPrice = 55.0),

        // ICE CREAMS
        Item(UUID.randomUUID().toString(), "Vannila Ice Cream", "Ice Cream", globalPrice = 40.0),
        Item(UUID.randomUUID().toString(), "Chocolate Ice Cream", "Ice Cream", globalPrice = 40.0),
        Item(UUID.randomUUID().toString(), "Fruit Falooda", "Ice Cream", globalPrice = 80.0),
        Item(UUID.randomUUID().toString(), "Royal Falooda", "Ice Cream", globalPrice = 120.0),

        // FALOODA
        Item(UUID.randomUUID().toString(), "Falooda", "Falooda", globalPrice = 80.0),

        // FRIES
        Item(UUID.randomUUID().toString(), "French Fries", "Fries", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Smiley", "Fries", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Veg Nuggets", "Fries", globalPrice = 60.0),

        // BREAD ITEMS
        Item(UUID.randomUUID().toString(), "Bread Omelet", "Bread Item", globalPrice = 50.0),
        Item(UUID.randomUUID().toString(), "Cheese Bread Omelet", "Bread Item", globalPrice = 60.0),

        // SANDWICHES
        Item(UUID.randomUUID().toString(), "Veg Sandwich", "Sandwich", globalPrice = 40.0),
        Item(UUID.randomUUID().toString(), "Cheese Sandwich", "Sandwich", globalPrice = 60.0),
        Item(UUID.randomUUID().toString(), "Paneer Sandwich", "Sandwich", globalPrice = 70.0),

        // FRIED CHICKEN
        Item(UUID.randomUUID().toString(), "Chicken Lollipop (2 pcs)", "Fried Chicken", globalPrice = 60.0),
        Item(UUID.randomUUID().toString(), "Chicken Popcorn (100 g)", "Fried Chicken", globalPrice = 80.0),
        Item(UUID.randomUUID().toString(), "Chicken Burger", "Burger", globalPrice = 100.0),

        // SNACKS
        Item(UUID.randomUUID().toString(), "Masala Vadai", "Snack", globalPrice = 5.0),
        Item(UUID.randomUUID().toString(), "Medhu Vadai", "Snack", globalPrice = 5.0),
        Item(UUID.randomUUID().toString(), "Vazhaikkai Bajji", "Snack", globalPrice = 5.0),
        Item(UUID.randomUUID().toString(), "Chilli Bajji", "Snack", globalPrice = 5.0),
        Item(UUID.randomUUID().toString(), "Onion Bajji", "Snack", globalPrice = 5.0),
        Item(UUID.randomUUID().toString(), "Potato Bajji", "Snack", globalPrice = 5.0),
        Item(UUID.randomUUID().toString(), "Masala Bonda", "Snack", globalPrice = 10.0),
        Item(UUID.randomUUID().toString(), "Sundal", "Snack", globalPrice = 10.0),
        Item(UUID.randomUUID().toString(), "Mini Samosa", "Snack", globalPrice = 10.0),
        Item(UUID.randomUUID().toString(), "Samosa", "Snack", globalPrice = 10.0)
    )
}
