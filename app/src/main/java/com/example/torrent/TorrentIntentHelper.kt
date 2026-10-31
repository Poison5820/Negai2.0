package com.example.torrent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.MainActivity

enum class ExternalOpenOutcome {
    OPENED_CHOOSER,
    OPENED_BROWSER_DOWNLOAD,
    COPIED_MAGNET_NO_EXTERNAL_APP,
    ERROR
}

object TorrentIntentHelper {

    private val KNOWN_TORRSERVER_PACKAGES = listOf(
        "ru.yourok.torrserve",
        "ru.yourok.torrserve.atv"
    )

    fun openTorrentInExternalApp(
        context: Context,
        magnetUri: String?,
        torrentFileUrl: String? = null,
        localTorrentUri: Uri? = null,
        title: String = "Торрент"
    ): ExternalOpenOutcome {
        val cleanMagnet = magnetUri?.trim()?.takeIf { it.isNotEmpty() }?.let {
            BencodeTorrentParser.ensurePlayableOrMagnetLink(it, title)
        }

        // 1. If a local .torrent content:// URI was chosen by the user, try opening it in external BitTorrent apps
        if (localTorrentUri != null) {
            val fileIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(localTorrentUri, "application/x-bittorrent")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (hasExternalHandler(context, fileIntent)) {
                if (launchChooserExcludingSelf(context, fileIntent, "Открыть .torrent в приложении")) {
                    return ExternalOpenOutcome.OPENED_CHOOSER
                }
            }
        }

        // 2. Open magnet:?xt=urn:btih:... in external torrent client / player
        if (!cleanMagnet.isNullOrBlank()) {
            val magnetIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanMagnet)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (hasExternalHandler(context, magnetIntent)) {
                if (launchChooserExcludingSelf(context, magnetIntent, "Открыть торрент в приложении")) {
                    return ExternalOpenOutcome.OPENED_CHOOSER
                }
            }

            // If no external magnet handler is installed, try .torrent HTTP download in browser or copy magnet
            if (!torrentFileUrl.isNullOrBlank() && openUrlInBrowser(context, torrentFileUrl)) {
                copyToClipboard(context, cleanMagnet, title)
                return ExternalOpenOutcome.OPENED_BROWSER_DOWNLOAD
            }

            copyToClipboard(context, cleanMagnet, title)
            return ExternalOpenOutcome.COPIED_MAGNET_NO_EXTERNAL_APP
        }

        // 3. Fallback: open .torrent download URL in browser / external downloader
        if (!torrentFileUrl.isNullOrBlank()) {
            if (openUrlInBrowser(context, torrentFileUrl)) {
                return ExternalOpenOutcome.OPENED_BROWSER_DOWNLOAD
            }
        }

        return ExternalOpenOutcome.ERROR
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Magnet"): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        }.getOrDefault(false)
    }

    fun openUrlInBrowser(context: Context, rawUrl: String): Boolean {
        val url = rawUrl.trim()
        if (url.isEmpty()) return false
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun isTorrServerAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return KNOWN_TORRSERVER_PACKAGES.any { pkg ->
            pm.getLaunchIntentForPackage(pkg) != null
        }
    }

    fun launchTorrServerApp(context: Context): Boolean {
        val pm = context.packageManager
        for (pkg in KNOWN_TORRSERVER_PACKAGES) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return runCatching {
                    context.startActivity(launchIntent)
                    true
                }.getOrDefault(false)
            }
        }
        return false
    }

    private fun hasExternalHandler(context: Context, intent: Intent): Boolean {
        return runCatching {
            val handlers = context.packageManager.queryIntentActivities(intent, 0)
            handlers.any { it.activityInfo?.packageName != context.packageName }
        }.getOrDefault(true)
    }

    private fun launchChooserExcludingSelf(
        context: Context,
        targetIntent: Intent,
        chooserTitle: String
    ): Boolean {
        return runCatching {
            val chooser = Intent.createChooser(targetIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    putExtra(
                        Intent.EXTRA_EXCLUDE_COMPONENTS,
                        arrayOf(ComponentName(context, MainActivity::class.java))
                    )
                }
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }
}
