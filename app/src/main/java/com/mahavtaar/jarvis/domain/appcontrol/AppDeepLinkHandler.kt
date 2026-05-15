package com.mahavtaar.jarvis.domain.appcontrol

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppDeepLinkHandler {
    fun whatsappMessage(context: Context, number: String, message: String): Boolean {
        return try {
            val uri = Uri.parse("https://wa.me/\$number?text=\${Uri.encode(message)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            true
        } catch (e: Exception) { false }
    }

    fun telegramMessage(context: Context, username: String, message: String): Boolean {
        return try {
            val uri = Uri.parse("tg://resolve?domain=\$username&text=\${Uri.encode(message)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            true
        } catch (e: Exception) { false }
    }

    fun shareText(context: Context, packageName: String?, text: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (packageName != null) intent.setPackage(packageName)
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }
}
