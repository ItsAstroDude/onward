package com.astro.onward.updates

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.astro.onward.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-updater against GitHub Releases (the repo is public, so no auth).
 * Flow: check → "vX ready" pill/card → DownloadManager fetches the release APK
 * → UpdateInstallReceiver hands it to the package installer. Release builds
 * must always be signed with the same keystore or devices will refuse the
 * update (see CLAUDE.md).
 */
object Updates {

    private const val REPO = "ItsAstroDude/onward"
    private const val PREFS = "updates"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    data class Release(val version: String, val apkUrl: String)

    /** Non-null when a newer release with an APK asset exists. */
    val available = MutableStateFlow<Release?>(null)
    val checking = MutableStateFlow(false)
    val statusText = MutableStateFlow<String?>(null)

    suspend fun check(context: Context, force: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong("last_check", 0)
        if (!force && System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return
        if (checking.value) return

        checking.value = true
        try {
            val body = withContext(Dispatchers.IO) {
                val conn = URL("https://api.github.com/repos/$REPO/releases/latest")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                try {
                    conn.inputStream.bufferedReader().readText()
                } finally {
                    conn.disconnect()
                }
            }
            val release = JSONObject(body)
            val remote = release.getString("tag_name").removePrefix("v")
            var apkUrl: String? = null
            val assets = release.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
            }
            prefs.edit().putLong("last_check", System.currentTimeMillis()).apply()
            if (apkUrl != null && isNewer(remote, BuildConfig.VERSION_NAME)) {
                available.value = Release(remote, apkUrl)
                statusText.value = "Version $remote is ready"
            } else {
                available.value = null
                statusText.value = "Up to date (v${BuildConfig.VERSION_NAME})"
            }
        } catch (_: Exception) {
            statusText.value = "Couldn't check — no connection?"
        } finally {
            checking.value = false
        }
    }

    /** Numeric semver compare; ignores any non-digit noise per segment. */
    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.split(".").map { seg ->
            seg.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** Kick off the APK download; the receiver launches the installer on completion. */
    fun download(context: Context, release: Release) {
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("Onward v${release.version}")
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationInExternalFilesDir(context, null, "onward-v${release.version}.apk")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val id = context.getSystemService(DownloadManager::class.java).enqueue(request)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong("download_id", id).apply()
        statusText.value = "Downloading v${release.version}…"
    }
}

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val expected = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
            .getLong("download_id", -2L)
        if (id != expected) return

        val uri = context.getSystemService(DownloadManager::class.java)
            .getUriForDownloadedFile(id) ?: return
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }
}
