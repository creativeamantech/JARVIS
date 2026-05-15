# 🤖 JARVIS — Android AI Voice Assistant
### Advanced Specification Prompt for AI Coding Assistants
> Use with: Google Jules · Cursor · Gemini · Google AI Studio · Android Studio Gemini

---

## 🧠 PROJECT OVERVIEW

Build a production-grade, fully offline **Jarvis-style Android Voice Assistant** app named **"J.A.R.V.I.S"** (Just A Rather Very Intelligent System). The assistant must feel alive — cinematic UI, real-time voice conversation, and autonomous task execution — all powered locally by the **Gemma 4 2B (e2b)** model via **Google AI Edge / MediaPipe LLM Inference API** on-device.

**Package Name:** `com.mahavtaar.jarvis`
**Min SDK:** 26 (Android 8.0)
**Target SDK:** 35
**Language:** Kotlin
**UI:** Jetpack Compose + Material3
**Architecture:** MVVM + Clean Architecture + Hilt DI
**Build System:** Gradle (Kotlin DSL)

---

## 🎯 CORE CAPABILITIES

### 1. Voice Conversation Engine
- Continuous Wake Word Detection: **"Hey Jarvis"** using PocketSphinx or Vosk offline
- Speech-to-Text (STT): Android native `SpeechRecognizer` (offline mode) + fallback to Vosk
- Text-to-Speech (TTS): Android `TextToSpeech` engine with a **deep, calm, British male voice** (Jarvis personality)
  - Custom TTS pitch: `0.85f`, speed: `0.95f` for authoritative tone
- Voice Activity Detection (VAD): silence threshold auto-detection, 1.5s trailing silence cutoff
- Push-to-Talk fallback button for noisy environments

### 2. On-Device LLM Brain — Gemma 4 2B (e2b)
- Engine: **Google AI Edge SDK** (`com.google.ai.edge.aicore`) OR **MediaPipe Tasks GenAI**
- Model: `gemma-4-2b-it` (instruction-tuned, INT4 quantized `.bin` or `.task` format)
- Model loading: from `/sdcard/jarvis/models/gemma4-2b-it-int4.bin` or internal app storage
- Inference: async streaming token-by-token, display tokens in real-time on UI
- Context window: maintain rolling 4096-token conversation history with summarization fallback
- System Prompt injected at session start (see System Prompt section below)

### 3. Autonomous Task Execution Engine
Jarvis must parse LLM responses for **intent tags** and execute real Android actions:

| Intent | Action |
|--------|--------|
| `[CALL:contact_name]` | Open dialer / initiate call via Intent |
| `[SMS:number:message]` | Send SMS via SmsManager |
| `[OPEN_APP:package_name]` | Launch installed app |
| `[WEB_SEARCH:query]` | Open browser with query |
| `[REMINDER:title:datetime]` | Create AlarmManager reminder |
| `[CALENDAR:title:datetime:duration]` | Add event to Google Calendar via Intent |
| `[SETTINGS:wifi/bluetooth/brightness/volume]` | Toggle system settings |
| `[CAMERA:photo/video]` | Launch camera app |
| `[FLASHLIGHT:on/off]` | Toggle flashlight via CameraManager |
| `[CLIPBOARD:text]` | Copy text to clipboard |
| `[NOTIFICATION_READ]` | Read latest notifications aloud |
| `[BATTERY_STATUS]` | Report battery percentage + charging state |
| `[WEATHER:city]` | Fetch weather via wttr.in (lightweight, no API key) |
| `[PLAY_MUSIC:query]` | Search & play via Spotify/YT intent |
| `[TIMER:seconds:label]` | Set countdown timer |
| `[CALCULATE:expression]` | Evaluate math expressions locally |
| `[TRANSLATE:text:language]` | ML Kit on-device translation |
| `[CONTACTS_SEARCH:name]` | Query contacts database |
| `[LOCATION_STATUS]` | Report current GPS location |
| `[SCREEN_BRIGHTNESS:0-100]` | Adjust screen brightness |
| `[DO_NOT_DISTURB:on/off]` | Toggle DND mode |
| `[WHATSAPP_MSG:number:message]` | Send WhatsApp message via deep link + Accessibility |
| `[WHATSAPP_CALL:number]` | Initiate WhatsApp voice call |
| `[TELEGRAM_MSG:username:message]` | Send Telegram message via deep link + Accessibility |
| `[TELEGRAM_CALL:username]` | Initiate Telegram voice call |
| `[APP_MSG:package:message]` | Send message in any open messaging app via Accessibility |
| `[APP_CLICK:package:content_desc]` | Click a UI element in a target app via Accessibility |
| `[APP_SCROLL:package:direction]` | Scroll up/down/left/right in a target app |
| `[APP_BACK:package]` | Simulate back button in a specific app |
| `[APP_CLOSE:package]` | Force-close an app via ActivityManager |
| `[APP_LIST]` | Read aloud all installed/running apps |
| `[APP_USAGE:package]` | Report usage stats for a specific app |
| `[SCREEN_READ]` | Read aloud all visible text on current screen via Accessibility |
| `[GLOBAL_BACK]` | Simulate global Android back gesture |
| `[GLOBAL_HOME]` | Simulate Home button press |
| `[GLOBAL_RECENTS]` | Open Recents/multitasking screen |
| `[SHARE_TEXT:package:text]` | Share text to a specific app |

**Intent Parser:** Use Regex pattern matching on LLM output. LLM wraps action commands in `[TAG:params]` format. Strip tags before speaking the response aloud.

---

## 📱 APP CONTROL LAYER — Accessibility + Deep Links

Jarvis must be able to **read and interact with any installed app** — especially WhatsApp, Telegram, Instagram, YouTube, etc. — using a two-tier strategy:

### Tier 1 — Deep Links (No Special Permission, Instant)
Use Android `Intent` and URI schemes to open apps and pre-fill actions. Works without Accessibility Service but cannot auto-confirm/send.

```kotlin
// AppDeepLinkHandler.kt
object AppDeepLinkHandler {

    // WhatsApp: open chat with number + pre-filled message
    fun whatsappMessage(context: Context, number: String, message: String) {
        val uri = Uri.parse("https://wa.me/$number?text=${Uri.encode(message)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // WhatsApp Business variant
    fun whatsappBusinessMessage(context: Context, number: String, message: String) {
        val uri = Uri.parse("https://wa.me/$number?text=${Uri.encode(message)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp.w4b")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // WhatsApp Call
    fun whatsappCall(context: Context, number: String) {
        val uri = Uri.parse("tel:$number")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // Telegram: open chat with username
    fun telegramMessage(context: Context, username: String, message: String) {
        val uri = Uri.parse("tg://resolve?domain=$username&text=${Uri.encode(message)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // Telegram: send to phone number
    fun telegramMessageByPhone(context: Context, phone: String, message: String) {
        val uri = Uri.parse("tg://resolve?phone=$phone&text=${Uri.encode(message)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // Share text to any app
    fun shareToApp(context: Context, packageName: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Generic: open any app by package
    fun openApp(context: Context, packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(this)
        }
    }
}
```

---

### Tier 2 — Accessibility Service (Full UI Control)
`JarvisAccessibilityService` extends `AccessibilityService` and acts as Jarvis's eyes and hands on the screen. It can **read text, click buttons, type messages, and scroll** inside any app.

#### AndroidManifest Registration
```xml
<service
    android:name=".core.accessibility.JarvisAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true"
    android:label="JARVIS App Control">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

#### res/xml/accessibility_service_config.xml
```xml
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagRequestEnhancedWebAccessibility"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:canRequestFilterKeyEvents="true"
    android:description="@string/accessibility_description"
    android:notificationTimeout="100"
    android:packageNames="" />
    <!-- Leave packageNames empty = monitor ALL apps -->
```

#### JarvisAccessibilityService.kt
```kotlin
@AndroidEntryPoint
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        instance = this
        super.onServiceConnected()
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* no-op */ }
    override fun onInterrupt() { /* no-op */ }

    // ── READ SCREEN TEXT ──────────────────────────────────────
    fun readScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        return extractNodeText(root)
    }

    private fun extractNodeText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        if (!node.text.isNullOrEmpty()) sb.appendLine(node.text)
        if (!node.contentDescription.isNullOrEmpty()) sb.appendLine(node.contentDescription)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { sb.append(extractNodeText(it)) }
        }
        return sb.toString()
    }

    // ── CLICK BY CONTENT DESCRIPTION ─────────────────────────
    fun clickByContentDesc(desc: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(desc)
        return nodes.firstOrNull()?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            true
        } ?: false
    }

    // ── CLICK BY VIEW ID ─────────────────────────────────────
    fun clickByViewId(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        return nodes.firstOrNull()?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            true
        } ?: false
    }

    // ── TYPE TEXT INTO FOCUSED FIELD ─────────────────────────
    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        // Find any editable/focused field
        val editNode = findEditableNode(root) ?: return false
        editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findEditableNode(child)?.let { return it }
            }
        }
        return null
    }

    // ── SCROLL ───────────────────────────────────────────────
    fun scrollDown(): Boolean = performGlobalAction(GESTURE_SWIPE_UP)
    fun scrollUp(): Boolean = performGlobalAction(GESTURE_SWIPE_DOWN)

    // ── GLOBAL ACTIONS ───────────────────────────────────────
    fun pressBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun pressRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)

    // ── SEND WHATSAPP MESSAGE (Full Auto) ────────────────────
    // Step 1: Open WhatsApp chat via deep link (AppDeepLinkHandler)
    // Step 2: Wait 1200ms for WhatsApp to load
    // Step 3: typeText(message) into the message field
    // Step 4: clickByViewId("com.whatsapp:id/send") OR clickByContentDesc("Send")
    suspend fun sendWhatsAppMessageFull(number: String, message: String): Boolean {
        // Called by TaskExecutor after deep link opens WhatsApp
        delay(1200) // Wait for WhatsApp chat to load
        val typed = typeText(message)
        if (!typed) return false
        delay(300)
        return clickByViewId("com.whatsapp:id/send")
            || clickByContentDesc("Send")
    }

    // ── SEND TELEGRAM MESSAGE (Full Auto) ────────────────────
    suspend fun sendTelegramMessageFull(message: String): Boolean {
        delay(1200)
        val typed = typeText(message)
        if (!typed) return false
        delay(300)
        return clickByViewId("org.telegram.messenger:id/send")
            || clickByContentDesc("Send")
    }

    // ── DISMISS NOTIFICATION ─────────────────────────────────
    fun dismissAllNotifications() {
        performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
    }
}
```

---

### Tier 3 — Usage Stats (App Monitoring)
```kotlin
// UsageStatsHelper.kt
class UsageStatsHelper @Inject constructor(private val context: Context) {

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestPermission(activity: Activity) {
        activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun getTopApp(): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 5000 // last 5 seconds
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    fun getAppUsageToday(packageName: String): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis()
        )
        return stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }
}
```

---

### Accessibility Permission Onboarding Flow
When Accessibility is NOT enabled, show a **dedicated onboarding card** in the UI:
```
┌──────────────────────────────────────────┐
│  ⚠  JARVIS App Control — Disabled       │
│                                          │
│  To control WhatsApp, Telegram & other  │
│  apps hands-free, enable the JARVIS     │
│  Accessibility Service.                 │
│                                          │
│  [  ENABLE NOW  ]   [ Skip for now ]   │
└──────────────────────────────────────────┘
```
Button opens: `Settings.ACTION_ACCESSIBILITY_SETTINGS`

Check status with:
```kotlin
fun isAccessibilityEnabled(context: Context): Boolean {
    val service = "${context.packageName}/.core.accessibility.JarvisAccessibilityService"
    val enabled = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(service, ignoreCase = true)
}
```

---

### Supported Apps Reference Table

| App | Package | Deep Link Send | Accessibility Auto-Send | View ID (Send Btn) |
|-----|---------|---------------|------------------------|-------------------|
| WhatsApp | `com.whatsapp` | ✅ | ✅ | `com.whatsapp:id/send` |
| WhatsApp Business | `com.whatsapp.w4b` | ✅ | ✅ | `com.whatsapp.w4b:id/send` |
| Telegram | `org.telegram.messenger` | ✅ | ✅ | `org.telegram.messenger:id/send` |
| Telegram X | `org.thunderdog.challegram` | ✅ | ✅ | content desc: "Send" |
| Instagram | `com.instagram.android` | ✅ (DMs) | ⚠ complex | Find by content desc |
| Gmail | `com.google.android.gm` | ✅ Intent | ⚠ | `com.google.android.gm:id/send` |
| Messages (Google) | `com.google.android.apps.messaging` | ✅ SMS | ✅ | content desc: "Send" |
| Signal | `org.thoughtcrime.securesms` | ✅ `sgnl://` | ✅ | content desc: "Send" |
| Snapchat | `com.snapchat.android` | ⚠ limited | ⚠ | UI varies |
| YouTube | `com.google.android.youtube` | ✅ search | ✅ scroll/play | — |
| Spotify | `com.spotify.music` | ✅ `spotify:` | ✅ | — |
| Chrome | `com.android.chrome` | ✅ URL intent | ✅ | — |

> ⚠ = Partial support — deep link opens app but Accessibility interaction depends on app version.

---

## 🖥️ UI / UX DESIGN — CINEMATIC JARVIS AESTHETIC

### Theme
- **Dark theme ONLY** — deep space black `#050508`
- **Primary accent:** Electric arc blue `#00BFFF` (Deep Sky Blue)
- **Secondary accent:** Holographic cyan `#00FFD1`
- **Warning/alert:** Iron Man gold `#FFD700`
- **Text:** Clean white `#E8F4FD` with subtle blue tint
- **Font:** `Orbitron` (Google Fonts) for headers/HUD elements, `Exo 2` for body text

### Screens

#### 1. MAIN ASSISTANT SCREEN (Home)
```
┌─────────────────────────────────────────┐
│  ⚡ J.A.R.V.I.S          [⚙] [💤] [📋] │
│─────────────────────────────────────────│
│                                         │
│         [ ANIMATED HOLOGRAPHIC         │
│           ARC REACTOR / WAVEFORM       │
│           VISUALIZER — center screen ] │
│                                         │
│   STATUS: ONLINE  |  MODEL: GEMMA 4   │
│   BATTERY: 87%   |  TEMP: 34°C        │
│─────────────────────────────────────────│
│  ┌─────────────────────────────────┐   │
│  │ Conversation Transcript Feed    │   │
│  │ (scrollable, auto-scroll down)  │   │
│  │                                 │   │
│  │ 👤 You: Set a reminder for 9 AM │   │
│  │ 🤖 JARVIS: Of course, sir. Re- │   │
│  │    minder set for 9:00 AM.     │   │
│  │    [✓ ACTION: REMINDER SET]    │   │
│  └─────────────────────────────────┘   │
│─────────────────────────────────────────│
│  [🎙 HOLD TO SPEAK]  [⌨ TYPE]  [🔇]   │
└─────────────────────────────────────────┘
```

#### 2. CENTER VISUALIZER COMPONENT
- **Arc Reactor Ring:** Animated concentric glowing rings using Canvas/DrawScope
- **Idle state:** Slowly pulsing blue rings with rotation, 3-second period
- **Listening state:** Rings expand rapidly, waveform bars appear inside (microphone amplitude data)
- **Thinking state:** Orbiting electron particles around center core, loading shimmer
- **Speaking state:** Waveform animation synced to TTS playback amplitude
- Implement using `Canvas` in Jetpack Compose with `rememberCoroutineScope` + `LaunchedEffect`

#### 3. HUD OVERLAY ELEMENTS
- Corner scan lines (decorative, CSS-style drawn with Canvas)
- Real-time clock in top-right (digital, Orbitron font)
- Network/model status indicators (dot + label)
- Subtle scanline overlay on entire screen (10% opacity horizontal lines)
- Ambient glow from bottom: blue radial gradient

#### 4. SETTINGS SCREEN
- Model file path picker
- Wake word sensitivity slider
- TTS voice selector
- Context window size (512 / 1024 / 2048 / 4096 tokens)
- System prompt editor (editable textarea)
- Permissions status card (mic, contacts, SMS, notifications, location, etc.)
- Model load status: `LOADING → READY → ERROR` with memory usage display

#### 5. QUICK ACTIONS GRID (expandable bottom sheet)
- 8 quick-action tiles: Call, Message, Remind, Timer, Weather, Flashlight, Settings, Apps
- Each tile: icon + label, tap triggers Jarvis to verbally confirm & execute

---

## 🏗️ PROJECT ARCHITECTURE

```
com.mahavtaar.jarvis/
├── app/
│   ├── di/                         # Hilt Modules
│   │   ├── AppModule.kt
│   │   ├── LLMModule.kt
│   │   └── AudioModule.kt
│   ├── ui/
│   │   ├── main/
│   │   │   ├── MainScreen.kt       # Primary Compose screen
│   │   │   ├── MainViewModel.kt    # StateFlow + events
│   │   │   └── components/
│   │   │       ├── ArcReactorVisualizer.kt
│   │   │       ├── ConversationFeed.kt
│   │   │       ├── VoiceButton.kt
│   │   │       ├── HUDOverlay.kt
│   │   │       └── StatusBar.kt
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt
│   │   │   └── SettingsViewModel.kt
│   │   └── theme/
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── core/
│   │   ├── llm/
│   │   │   ├── GemmaEngine.kt      # Gemma 4 2B inference wrapper
│   │   │   ├── ConversationManager.kt  # History + token management
│   │   │   └── StreamingCallback.kt
│   │   ├── voice/
│   │   │   ├── SpeechRecognitionManager.kt
│   │   │   ├── TTSManager.kt
│   │   │   ├── WakeWordDetector.kt
│   │   │   └── AudioAmplitudeAnalyzer.kt
│   │   ├── tasks/
│   │   │   ├── IntentParser.kt     # Parse [TAG:params] from LLM
│   │   │   ├── TaskExecutor.kt     # Execute parsed intents
│   │   │   └── tasks/             # Individual task handlers
│   │   │       ├── PhoneTask.kt
│   │   │       ├── ReminderTask.kt
│   │   │       ├── SystemTask.kt
│   │   │       ├── WeatherTask.kt
│   │   │       └── AppLaunchTask.kt
│   │   └── permissions/
│   │       └── PermissionManager.kt
│   └── data/
│       ├── preferences/
│       │   └── JarvisPreferences.kt  # DataStore
│       └── model/
│           ├── ChatMessage.kt
│           ├── AssistantState.kt
│           └── TaskResult.kt
```

---

## 🧬 SYSTEM PROMPT (Injected into Gemma 4)

```
You are J.A.R.V.I.S. — Just A Rather Very Intelligent System — an advanced AI assistant running entirely on-device. You serve your user with the calm authority, wit, and precision of Jarvis from Iron Man.

PERSONALITY:
- Formal yet warm. Address the user as "sir" or "ma'am" or by their name if known.
- Highly capable, never flustered. Acknowledge limitations gracefully.
- Occasionally inject dry British wit, but stay efficient.
- Never say "I'm just an AI" or make excuses. Solve problems.

RESPONSE RULES:
- Keep responses concise. 1-3 sentences unless detail is requested.
- Always speak in first person as JARVIS.
- When you perform a task, wrap the action in a tag. Example: "Certainly, sir. [REMINDER:Team Meeting:2024-01-15T09:00:00]"
- For calculations, answer immediately: "That would be 4,096, sir."
- For unknown information, say "I don't have access to that data currently, sir."
- Never break character. You ARE Jarvis.

AVAILABLE ACTIONS (use these tags exactly):
[CALL:name], [SMS:number:message], [OPEN_APP:package], [WEB_SEARCH:query],
[REMINDER:title:ISO_datetime], [CALENDAR:title:ISO_datetime:minutes],
[SETTINGS:wifi_on/wifi_off/bt_on/bt_off/flashlight_on/flashlight_off],
[TIMER:seconds:label], [WEATHER:city], [BATTERY_STATUS],
[NOTIFICATION_READ], [TRANSLATE:text:lang_code], [DO_NOT_DISTURB:on/off],
[WHATSAPP_MSG:number:message], [WHATSAPP_CALL:number],
[TELEGRAM_MSG:username:message], [TELEGRAM_CALL:username],
[APP_MSG:package:message], [APP_CLICK:package:content_desc],
[APP_SCROLL:package:up/down], [APP_CLOSE:package],
[APP_LIST], [APP_USAGE:package], [SCREEN_READ],
[GLOBAL_BACK], [GLOBAL_HOME], [GLOBAL_RECENTS],
[SHARE_TEXT:package:text]

EXAMPLES of app control:
User: "Send a WhatsApp to Raj saying I'll be late"
→ "Certainly, sir. [WHATSAPP_MSG:+91XXXXXXXXXX:I'll be late]"

User: "What's on my screen right now?"
→ "Let me scan the display, sir. [SCREEN_READ]"

User: "Open Telegram and message Ankit"
→ "Opening Telegram for Ankit, sir. [TELEGRAM_MSG:ankit:Hello]"

Current context: {DYNAMIC_CONTEXT}
```

**Dynamic Context** is injected at runtime: time, date, battery %, WiFi state, device name.

---

## ⚙️ KEY IMPLEMENTATION DETAILS

### Gemma 4 2B Integration

```kotlin
// GemmaEngine.kt
class GemmaEngine @Inject constructor(
    private val context: Context,
    private val prefs: JarvisPreferences
) {
    private var llmInference: LlmInference? = null

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(prefs.modelPath)
                .setMaxTokens(prefs.contextWindowSize)
                .setTemperature(0.7f)
                .setTopK(40)
                .setTopP(0.95f)
                .setRandomSeed(42)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateResponse(
        prompt: String,
        onToken: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        llmInference?.generateResponseAsync(
            prompt,
            { partial, done ->
                onToken(partial)
                if (done) onComplete(partial)
            }
        ) ?: onError(IllegalStateException("Engine not initialized"))
    }
}
```

### Intent Parser

```kotlin
// IntentParser.kt
object IntentParser {
    private val TAG_PATTERN = Regex("""\[([A-Z_]+):([^\]]*)\]""")

    data class ParsedResponse(
        val spokenText: String,
        val intents: List<JarvisIntent>
    )

    fun parse(rawResponse: String): ParsedResponse {
        val intents = mutableListOf<JarvisIntent>()
        val cleanText = TAG_PATTERN.replace(rawResponse) { match ->
            val tag = match.groupValues[1]
            val params = match.groupValues[2].split(":")
            intents.add(JarvisIntent.fromTag(tag, params))
            "" // Remove tag from spoken text
        }.trim()
        return ParsedResponse(cleanText, intents)
    }
}

sealed class JarvisIntent {
    data class MakeCall(val contact: String) : JarvisIntent()
    data class SendSMS(val number: String, val message: String) : JarvisIntent()
    data class OpenApp(val packageName: String) : JarvisIntent()
    data class SetReminder(val title: String, val dateTime: String) : JarvisIntent()
    data class ToggleFlashlight(val on: Boolean) : JarvisIntent()
    data class GetWeather(val city: String) : JarvisIntent()
    data class SetTimer(val seconds: Int, val label: String) : JarvisIntent()
    // ... all intents
    
    companion object {
        fun fromTag(tag: String, params: List<String>): JarvisIntent = when (tag) {
            "CALL" -> MakeCall(params[0])
            "SMS" -> SendSMS(params[0], params.getOrElse(1) { "" })
            "OPEN_APP" -> OpenApp(params[0])
            "REMINDER" -> SetReminder(params[0], params.getOrElse(1) { "" })
            "FLASHLIGHT" -> ToggleFlashlight(params[0] == "on")
            "WEATHER" -> GetWeather(params[0])
            "TIMER" -> SetTimer(params[0].toIntOrNull() ?: 60, params.getOrElse(1) { "Timer" })
            else -> object : JarvisIntent() {}
        }
    }
}
```

### Arc Reactor Visualizer (Compose Canvas)

```kotlin
@Composable
fun ArcReactorVisualizer(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing))
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    Canvas(modifier = modifier.size(220.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 2

        // Outer glow rings
        listOf(1.0f, 0.85f, 0.7f, 0.55f).forEachIndexed { i, scale ->
            val alpha = when (state) {
                AssistantState.LISTENING -> (amplitude * 0.8f + 0.2f)
                AssistantState.THINKING -> pulse * (1f - i * 0.15f)
                else -> (1f - i * 0.2f) * 0.6f
            }
            drawCircle(
                color = Color(0xFF00BFFF).copy(alpha = alpha * 0.3f),
                radius = baseRadius * scale * pulse,
                center = center,
                style = Stroke(width = (4 - i).dp.toPx())
            )
        }

        // Rotating arc segments (Jarvis HUD feel)
        rotate(rotation, center) {
            for (i in 0..5) {
                val angle = i * 60f
                drawArc(
                    color = Color(0xFF00FFD1).copy(alpha = 0.7f),
                    startAngle = angle, sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.6f, center.y - baseRadius * 0.6f),
                    size = Size(baseRadius * 1.2f, baseRadius * 1.2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Center core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00BFFF), Color(0xFF003366)),
                center = center, radius = baseRadius * 0.25f
            ),
            radius = baseRadius * 0.25f, center = center
        )
    }
}
```

---

## 📦 GRADLE DEPENDENCIES

```kotlin
// build.gradle.kts (app)
dependencies {
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ViewModel + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // MediaPipe / AI Edge LLM Inference
    implementation("com.google.mediapipe:tasks-genai:0.10.22")
    // OR Google AI Edge:
    // implementation("com.google.ai.edge.aicore:aicore:0.0.1-exp01")

    // ML Kit Translation (on-device)
    implementation("com.google.mlkit:translate:17.0.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // OkHttp (for weather wttr.in)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Google Fonts (Orbitron, Exo 2)
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.5")

    // Lottie (optional, for animations)
    implementation("com.airbnb.android:lottie-compose:6.6.0")
}
```

---

## 🔐 PERMISSIONS (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.FLASHLIGHT" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- ══ APP CONTROL PERMISSIONS ══ -->

<!-- Accessibility Service — UI control of WhatsApp, Telegram, etc. -->
<!-- Declared via <service> tag; user must enable in Settings > Accessibility -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"
    tools:ignore="ProtectedPermissions" />

<!-- Usage Stats — detect foreground app, per-app screen time -->
<!-- Granted via Settings > Apps > Special App Access > Usage Access -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />

<!-- Query any installed app (required Android 11+) -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />

<!-- Kill/close background apps -->
<uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />

<!-- Detect if a specific app is running -->
<uses-permission android:name="android.permission.GET_TASKS" />

<!-- Gesture injection for swipe/tap (Accessibility-backed) -->
<uses-permission android:name="android.permission.INJECT_EVENTS"
    tools:ignore="ProtectedPermissions" />

<!-- Screen content capture (for SCREEN_READ intent) -->
<uses-permission android:name="android.permission.READ_FRAME_BUFFER"
    tools:ignore="ProtectedPermissions" />

<!-- Install/manage apps (for APP_LIST intent) -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<!-- System alert window — overlay for Jarvis HUD while using other apps -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- Needed to keep Accessibility Service alive in background -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

---

## 🔁 CONVERSATION FLOW STATE MACHINE

```
IDLE ──[wake word / tap]──► LISTENING
LISTENING ──[silence / stop]──► PROCESSING_STT
PROCESSING_STT ──[text ready]──► THINKING (LLM inference)
THINKING ──[response ready]──► PARSING_INTENTS
PARSING_INTENTS ──[intents extracted]──► EXECUTING_TASKS (parallel)
EXECUTING_TASKS + SPEAKING ──[TTS done]──► IDLE

IDLE ──[hold button]──► LISTENING (PTT mode)
Any state ──[error]──► ERROR ──[auto-recover 3s]──► IDLE
```

Implement as a `sealed class AssistantState` and manage via `StateFlow<AssistantState>` in `MainViewModel`.

---

## 🔔 BACKGROUND SERVICE

Run a **Foreground Service** (`JarvisService.kt`) for persistent wake word listening:
- Shows persistent notification: "⚡ JARVIS is active"
- Binds to MainActivity when in foreground
- Low battery mode: pauses wake word detection when battery < 10%
- Killed/restart recovery via `START_STICKY`

---

## 📋 FEATURE CHECKLIST FOR IMPLEMENTATION

### Phase 1 — Core MVP
- [ ] Project setup with all dependencies
- [ ] Jetpack Compose dark theme with Orbitron/Exo2 fonts
- [ ] MainScreen layout with placeholder visualizer
- [ ] GemmaEngine with model loading + streaming
- [ ] Basic STT → Gemma → TTS pipeline
- [ ] Conversation feed UI

### Phase 2 — Voice & UI
- [ ] Arc Reactor Canvas visualizer with state animations
- [ ] Wake word detection ("Hey Jarvis")
- [ ] Hold-to-speak PTT button with waveform
- [ ] HUD scan line overlay + corner decorations
- [ ] Settings screen with DataStore persistence

### Phase 3 — Task Engine
- [ ] IntentParser regex engine (all 35+ intents)
- [ ] TaskExecutor with all core intents
- [ ] Permission management flow
- [ ] Weather fetch via wttr.in
- [ ] Flashlight, brightness, DND, volume controls

### Phase 4 — App Control Layer
- [ ] `AppDeepLinkHandler` for WhatsApp, Telegram, Signal, Gmail
- [ ] `JarvisAccessibilityService` with screen read, type, click, scroll
- [ ] Accessibility onboarding card + enable flow
- [ ] `UsageStatsHelper` for foreground app detection + screen time
- [ ] `SYSTEM_ALERT_WINDOW` overlay (floating Jarvis HUD on top of other apps)
- [ ] Auto-send flow: deep link → wait → typeText → click Send
- [ ] `SCREEN_READ` intent — read screen aloud via TTS
- [ ] App list & force-close intents
- [ ] Global navigation gestures (back, home, recents)

### Phase 5 — Polish & Extras
- [ ] Foreground service + persistent notification
- [ ] ML Kit translation
- [ ] Quick actions bottom sheet
- [ ] Notification reader
- [ ] Contacts search integration
- [ ] Error recovery & offline graceful degradation
- [ ] Onboarding screen (model download guide)

---

## 📁 MODEL SETUP INSTRUCTIONS (in app README)

1. Download **Gemma 4 2B IT INT4** from [Google AI Edge Model Gallery](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)
2. Place the `.bin` or `.task` file at: `/sdcard/jarvis/models/gemma4-2b-it-int4.bin`
3. Grant storage permissions on first launch
4. App will detect and load the model automatically

**RAM Requirement:** ~2.5GB RAM minimum for smooth inference (INT4 quantized)
**Storage:** ~1.5GB for model file

---

## 🎬 ADDITIONAL PERSONALITY TOUCHES

- On app launch: play a short **startup sound** (synthetic "JARVIS online" beep sequence)
- First message always: *"Good [morning/afternoon/evening], sir. All systems are operational. How may I assist you today?"*
- After 5 minutes idle: *"Standing by, sir."* (spoken softly)
- Battery low detection: *"I should inform you, sir, that power reserves are at [X]%. I recommend connecting to a power source."*
- Error handling: *"I'm afraid I encountered a difficulty, sir. Allow me to try again."*

---

*Generated for: Mahavtaar Enterprises | Specification version 1.1 — App Control Update | Powered by Claude Sonnet 4.6*
