package com.astro.onward.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Always-available reference: the shake, the template, the swap list. */
@Composable
fun CheatSheetScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Cheat sheet", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "The whole system on one page. When in doubt, come back here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { ShakeCard() }
        item { TemplateCard() }
        item { SwapsCard() }
    }
}

@Composable
private fun SheetCard(title: String, emoji: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ShakeCard() {
    SheetCard("The shake", "🥤") {
        Text(
            "Formula: protein + carb + fat + something filling — miss one and you're hungry in an hour.",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(10.dp))
        Text("Base", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(
            "1 scoop whey isolate · ½–1 banana (or a handful of frozen berries) · " +
                "2–3 tbsp oats blended in (this is what makes it a meal) · " +
                "1 tbsp peanut butter or a few walnuts · water or low-fat milk.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        Text("Variations", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        VariationRow("Chocolate–peanut", "choc whey + banana + peanut butter + oats")
        VariationRow("Berry", "vanilla whey + frozen berries + oats + splash of yogurt")
        VariationRow("Coffee", "vanilla whey + cold coffee + banana + oats")
        Spacer(Modifier.height(10.dp))
        Text(
            "Weak blender? Blend the oats dry first. Shake \"doesn't fill you up\"? " +
                "You skipped the oats or the nuts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VariationRow(name: String, recipe: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text("›", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            buildString { append(name); append(": "); append(recipe) },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TemplateCard() {
    SheetCard("The template", "🍽️") {
        Text(
            "protein + veg + a carb",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Dinner = a lighter version of lunch. That's the whole rule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class Swap(val from: String, val to: String, val note: String)

private val swaps = listOf(
    Swap("Pork", "chicken breast/thigh, turkey, or fish", "aim for fish twice a week — macrou, sardines, somon; canned counts"),
    Swap("White bread", "small amounts of wholegrain/rye (secară)", "or potatoes, rice, bulgur, oats — not zero carbs, just off white bread"),
    Swap("Smântână", "Greek yogurt (iaurt grecesc)", "works in ciorbă, on potatoes, in sauces"),
    Swap("Fried", "baked, boiled, grilled, or air-fried", "olive oil instead of lard/untură"),
)

@Composable
private fun SwapsCard() {
    SheetCard("The swaps — doctor's rules", "🔁") {
        swaps.forEachIndexed { index, swap ->
            if (index > 0) {
                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            swap.from,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "  →  ",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            swap.to,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        swap.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
