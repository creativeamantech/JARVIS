package com.mahavtaar.jarvis.domain.task

sealed class JarvisIntent {
    data class Flashlight(val on: Boolean) : JarvisIntent()
    data class Timer(val seconds: Int, val label: String?) : JarvisIntent()
    data class Calculate(val expression: String) : JarvisIntent()
    object BatteryStatus : JarvisIntent()
    data class Clipboard(val text: String) : JarvisIntent()
    data class OpenApp(val appName: String) : JarvisIntent()
    data class WebSearch(val query: String) : JarvisIntent()

    data class Call(val contactName: String) : JarvisIntent()
    data class Sms(val number: String, val message: String) : JarvisIntent()
    data class Reminder(val title: String, val datetime: String) : JarvisIntent()

    data class SettingsToggle(val setting: String, val state: String) : JarvisIntent()
    data class SetBrightness(val level: Int) : JarvisIntent()
    data class DoNotDisturb(val on: Boolean) : JarvisIntent()
}

data class ParsedResponse(
    val spokenText: String,
    val intents: List<JarvisIntent>
)
