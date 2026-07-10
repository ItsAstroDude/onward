package com.astro.onward.data

/**
 * All baked-in content: the cardiologist-shaped 7-day rotation, meal option
 * pools, reminder defaults and the starter shopping list. Bump SEED_VERSION
 * if seeds change and you want existing installs to receive additions.
 */
object SeedData {

    const val SEED_VERSION = 1

    const val SHAKE_DEFAULT = "The shake — whey + banana + oats + peanut butter"
    const val LUNCH_TEMPLATE = "protein + veg + a carb"
    const val DINNER_TEMPLATE = "lighter: protein + veg, easy on the carb"

    val planDays = listOf(
        PlanDay(0, SHAKE_DEFAULT, LUNCH_TEMPLATE, "Baked chicken thighs + roasted vegetables + potatoes", DINNER_TEMPLATE, "Vegetable ciorbă (soured with yogurt) + side salad"),
        PlanDay(1, SHAKE_DEFAULT, LUNCH_TEMPLATE, "Baked mackerel with lemon & garlic + bulgur + salad", DINNER_TEMPLATE, "Vegetable omelette + tomato salad"),
        PlanDay(2, SHAKE_DEFAULT, LUNCH_TEMPLATE, "Turkey + vegetable stir-fry over rice", DINNER_TEMPLATE, "Grilled chicken + big salad"),
        PlanDay(3, SHAKE_DEFAULT, LUNCH_TEMPLATE, "Ciorbă de pui (yogurt) + grilled chicken breast + potatoes", DINNER_TEMPLATE, "Canned tuna (in water) salad + veg"),
        PlanDay(4, SHAKE_DEFAULT, LUNCH_TEMPLATE, "Baked salmon + rice + steamed veg", DINNER_TEMPLATE, "Vegetable soup + boiled egg + salad"),
        PlanDay(5, SHAKE_DEFAULT, LUNCH_TEMPLATE, "Chicken + roasted vegetables + potatoes (batch-cook day)", DINNER_TEMPLATE, "Omelette with veg, or leftovers"),
        PlanDay(6, SHAKE_DEFAULT, LUNCH_TEMPLATE, "Grilled fish + bulgur + salad", DINNER_TEMPLATE, "Light: Greek yogurt + fruit + nuts, or leftover soup"),
    )

    val mealOptions = listOf(
        // Breakfast = the shake, in its variations.
        MealOption(slot = MealSlot.BREAKFAST, example = SHAKE_DEFAULT),
        MealOption(slot = MealSlot.BREAKFAST, example = "Chocolate–peanut shake — choc whey + banana + peanut butter + oats"),
        MealOption(slot = MealSlot.BREAKFAST, example = "Berry shake — vanilla whey + frozen berries + oats + splash of yogurt"),
        MealOption(slot = MealSlot.BREAKFAST, example = "Coffee shake — vanilla whey + cold coffee + banana + oats"),

        MealOption(slot = MealSlot.LUNCH, example = "Baked chicken thighs + roasted vegetables + potatoes"),
        MealOption(slot = MealSlot.LUNCH, example = "Baked mackerel with lemon & garlic + bulgur + salad"),
        MealOption(slot = MealSlot.LUNCH, example = "Turkey + vegetable stir-fry over rice"),
        MealOption(slot = MealSlot.LUNCH, example = "Ciorbă de pui (yogurt) + grilled chicken breast + potatoes"),
        MealOption(slot = MealSlot.LUNCH, example = "Baked salmon + rice + steamed veg"),
        MealOption(slot = MealSlot.LUNCH, example = "Grilled fish + bulgur + salad"),
        MealOption(slot = MealSlot.LUNCH, example = "Grilled chicken breast + rice + salad"),

        MealOption(slot = MealSlot.DINNER, example = "Vegetable ciorbă (soured with yogurt) + side salad"),
        MealOption(slot = MealSlot.DINNER, example = "Vegetable omelette + tomato salad"),
        MealOption(slot = MealSlot.DINNER, example = "Grilled chicken + big salad"),
        MealOption(slot = MealSlot.DINNER, example = "Canned tuna (in water) salad + veg"),
        MealOption(slot = MealSlot.DINNER, example = "Vegetable soup + boiled egg + salad"),
        MealOption(slot = MealSlot.DINNER, example = "Light: Greek yogurt + fruit + nuts"),
    )

    const val OPTION_LEFTOVERS = "Leftovers"

    // Bit 0 = Monday .. bit 6 = Sunday.
    const val DAILY = 0b1111111
    private const val SUNDAY = 0b1000000

    val reminders = listOf(
        ReminderSetting(1, "Breakfast shake", "🥤 Breakfast shake — blend & go", enabled = true, hour = 8, minute = 30, daysMask = DAILY),
        ReminderSetting(2, "Lunch", "🍗 Lunch — protein + veg + a carb", enabled = true, hour = 12, minute = 30, daysMask = DAILY),
        ReminderSetting(3, "Snack check", "🍎 Snack check — fruit / yogurt / nuts, not junk", enabled = true, hour = 16, minute = 0, daysMask = DAILY),
        ReminderSetting(4, "Dinner", "🥗 Dinner — lighter version of lunch", enabled = true, hour = 19, minute = 0, daysMask = DAILY),
        ReminderSetting(5, "Check off today", "✅ Check off today — keep the streak", enabled = true, hour = 21, minute = 30, daysMask = DAILY, destination = "today"),
        ReminderSetting(6, "Grocery + prep", "🛒 Grocery + prep — stock up & batch-cook", enabled = true, hour = 11, minute = 0, daysMask = SUNDAY, destination = "shopping"),
    )

    val shoppingStaples = listOf(
        "Whey isolate (plain/vanilla/choc — avoid “gainer”, watch sodium)",
        "Oats",
        "Bananas",
        "Frozen berries",
        "Peanut butter",
        "Walnuts",
        "Greek yogurt",
        "Chicken (breast + thighs)",
        "Turkey",
        "Mackerel / macrou",
        "Salmon / somon",
        "Canned tuna in water",
        "Eggs",
        "Potatoes",
        "Rice",
        "Bulgur",
        "Mixed vegetables (fresh + frozen)",
        "Salad greens",
        "Tomatoes",
        "Lemons",
        "Garlic",
        "Olive oil",
        "Low-fat milk",
    ).mapIndexed { i, name -> ShoppingItem(name = name, position = i) }
}
