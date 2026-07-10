package com.astro.onward.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astro.onward.ui.theme.Citrus
import com.astro.onward.ui.theme.OffWhite
import com.astro.onward.ui.theme.Pine

/**
 * First run only. Key behavior: reminders stay OFF unless "I'm ready" is
 * tapped — the user may still be gathering supplies and shouldn't be nagged.
 */
@Composable
fun OnboardingScreen(onDone: (startReminders: Boolean) -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onDone(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("››", style = MaterialTheme.typography.displaySmall, color = Citrus)
        Spacer(Modifier.height(12.dp))
        Text("Onward.", style = MaterialTheme.typography.displaySmall, color = OffWhite)
        Spacer(Modifier.height(16.dp))
        Text(
            "A missed day is never a failure.\nYou're just onward to the next meal.",
            style = MaterialTheme.typography.bodyLarge,
            color = OffWhite.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))
        Bullet("One tap a day", "\"Hit the pattern today?\" That's the whole job. Mostly followed the shape? That's a yes.")
        Bullet("A forgiving streak", "One free miss a week never breaks it. No shame, ever.")
        Bullet("The plan on rotation", "Shake for breakfast, protein + veg + a carb after. Swaps, cheat sheet and shopping list built in.")

        Spacer(Modifier.height(40.dp))
        Spacer(Modifier.weight(1f))

        Text(
            "Ready to start?",
            style = MaterialTheme.typography.headlineSmall,
            color = OffWhite,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Citrus, contentColor = OffWhite),
        ) {
            Text("I'm ready — remind me", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onDone(false) }, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Not yet — still stocking up (no reminders)",
                color = OffWhite.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun Bullet(title: String, body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("›", style = MaterialTheme.typography.titleLarge, color = Citrus)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = OffWhite)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = OffWhite.copy(alpha = 0.75f))
        }
    }
}
