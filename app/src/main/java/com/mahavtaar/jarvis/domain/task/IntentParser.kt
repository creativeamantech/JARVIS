package com.mahavtaar.jarvis.domain.task

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentParser @Inject constructor() {
    private val tagRegex = "\\[([A-Z_]+)(?::([^\\]]+))?]".toRegex()

    // Matches a well-formed tag like [TAG:params] or an incomplete tag starting with [ that continues to the end of string
    // e.g. "I'll do that right away. [FLASHLIG" -> hides "[FLASHLIG"
    private val streamingCleanRegex = "\\[([A-Z_]+(?::[^\\]]*]?)?)?\$|\\[([A-Z_]+)(?::([^\\]]+))?]".toRegex()

    fun cleanStreamingText(rawStreamText: String): String {
        return rawStreamText.replace(streamingCleanRegex, "").trim()
    }

    fun parse(rawResponse: String): ParsedResponse {
        val intents = mutableListOf<JarvisIntent>()
        var spokenText = rawResponse

        val matches = tagRegex.findAll(rawResponse)
        for (match in matches) {
            val tag = match.groups[1]?.value
            val paramsRaw = match.groups[2]?.value
            val params = paramsRaw?.split(":") ?: emptyList()

            when (tag) {
                "FLASHLIGHT" -> {
                    val on = params.firstOrNull()?.equals("on", ignoreCase = true) ?: true
                    intents.add(JarvisIntent.Flashlight(on))
                }
                "TIMER" -> {
                    val seconds = params.firstOrNull()?.toIntOrNull() ?: 60
                    val label = params.getOrNull(1)
                    intents.add(JarvisIntent.Timer(seconds, label))
                }
                "CALCULATE" -> {
                    paramsRaw?.let { intents.add(JarvisIntent.Calculate(it)) }
                }
                "BATTERY_STATUS" -> intents.add(JarvisIntent.BatteryStatus)
                "CLIPBOARD" -> {
                    paramsRaw?.let { intents.add(JarvisIntent.Clipboard(it)) }
                }
                "OPEN_APP" -> {
                    paramsRaw?.let { intents.add(JarvisIntent.OpenApp(it)) }
                }
                "WEB_SEARCH" -> {
                    paramsRaw?.let { intents.add(JarvisIntent.WebSearch(it)) }
                }
                "CALL" -> {
                    paramsRaw?.let { intents.add(JarvisIntent.Call(it)) }
                }
                "SMS" -> {
                    val number = params.firstOrNull()
                    val message = if (params.size > 1) params.subList(1, params.size).joinToString(":") else ""
                    if (number != null && message.isNotEmpty()) {
                        intents.add(JarvisIntent.Sms(number, message))
                    }
                }
                "REMINDER" -> {
                    val title = params.firstOrNull() ?: "Reminder"
                    val datetime = params.getOrNull(1) ?: ""
                    intents.add(JarvisIntent.Reminder(title, datetime))
                }
                "SETTINGS" -> {
                    val settingName = params.firstOrNull()?.lowercase()
                    val state = params.getOrNull(1)?.lowercase() ?: "toggle"
                    if (settingName != null) {
                        intents.add(JarvisIntent.SettingsToggle(settingName, state))
                    }
                }
                "SCREEN_BRIGHTNESS" -> {
                    val level = params.firstOrNull()?.toIntOrNull() ?: 50
                    intents.add(JarvisIntent.SetBrightness(level))
                }
                "DO_NOT_DISTURB" -> {
                    val on = params.firstOrNull()?.equals("on", ignoreCase = true) ?: true
                    intents.add(JarvisIntent.DoNotDisturb(on))
                }
            }
        }

        spokenText = spokenText.replace(tagRegex, "").trim()

        return ParsedResponse(spokenText, intents)
    }
}
