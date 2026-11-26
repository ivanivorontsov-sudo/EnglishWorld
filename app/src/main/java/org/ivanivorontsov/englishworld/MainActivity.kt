package org.ivanivorontsov.englishworld

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.ivanivorontsov.englishworld.ai.ApiSettings
import org.ivanivorontsov.englishworld.ai.ChatEngine
import org.ivanivorontsov.englishworld.ai.ChatTurn
import org.ivanivorontsov.englishworld.ai.SecureKeyStore
import org.ivanivorontsov.englishworld.data.CefrLevel
import org.ivanivorontsov.englishworld.data.Curriculum
import org.ivanivorontsov.englishworld.data.Lesson
import org.ivanivorontsov.englishworld.data.ProgressStore
import org.ivanivorontsov.englishworld.data.ReviewGrade
import org.ivanivorontsov.englishworld.data.ReviewCard
import org.ivanivorontsov.englishworld.data.StudyEngine
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    private var recognizer: SpeechRecognizer? = null
    private var speechResult: ((String) -> Unit)? = null
    private var speechPartial: ((String) -> Unit)? = null
    private var speechError: ((String) -> Unit)? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initVoice()
        setContent { EnglishWorldApp(this) }
    }

    private fun initVoice() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(this) }.getOrNull()
            } else null
            if (recognizer == null) recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(this) }.getOrNull()
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Не расслышала фразу. Попробуйте говорить чуть медленнее."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Речь не услышана. Нажмите на микрофон и попробуйте ещё раз."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознавание уже занято. Повторите через секунду."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения на микрофон."
                        else -> "Ошибка распознавания речи ($error). Проверьте службу Android."
                    }
                    speechError?.invoke(message)
                }
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) speechResult?.invoke(text) else speechError?.invoke("Речь не распознана. Попробуйте ещё раз.")
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { speechPartial?.invoke(it) }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.94f)
                ttsReady = true
            }
        }
    }

    fun startListening(languageTag: String, onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (String) -> Unit) {
        val engine = recognizer
        if (engine == null) {
            onError("На телефоне не найдена служба распознавания речи. Включите Speech Recognition в Android.")
            return
        }
        speechPartial = onPartial
        speechResult = onFinal
        speechError = onError
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this@MainActivity)) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
        runCatching { engine.startListening(intent) }
            .onFailure { onError("Не удалось запустить микрофон: ${it.localizedMessage ?: "ошибка Android"}") }
    }

    fun stopListening() { runCatching { recognizer?.stopListening() } }

    fun speak(text: String, languageTag: String = "en-US") {
        if (!ttsReady || text.isBlank()) return
        tts?.language = Locale.forLanguageTag(languageTag)
        tts?.speak(text.take(1800), TextToSpeech.QUEUE_FLUSH, null, "englishworld-answer")
    }

    override fun onDestroy() {
        runCatching { recognizer?.cancel() }
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

private enum class Page { HOME, CHAT, LESSONS, REVIEW, PROGRESS, SETTINGS }
private data class ChatMessage(val role: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnglishWorldApp(activity: MainActivity) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("englishworld_settings", Context.MODE_PRIVATE) }
    val progress = remember { ProgressStore(context) }
    val secrets = remember { SecureKeyStore(context) }
    val scope = rememberCoroutineScope()

    var page by remember { mutableStateOf(Page.HOME) }
    var providerId by remember { mutableStateOf(prefs.getString("provider", "qwen") ?: "qwen") }
    var apiBase by remember { mutableStateOf(prefs.getString("api_base", "https://api.openai.com/v1") ?: "https://api.openai.com/v1") }
    var apiModel by remember { mutableStateOf(prefs.getString("api_model", "gpt-4.1-mini") ?: "gpt-4.1-mini") }
    var apiKey by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(prefs.getString("level", "B1") ?: "B1") }
    var accent by remember { mutableStateOf(prefs.getString("accent", "en-US") ?: "en-US") }
    var autoSpeak by remember { mutableStateOf(prefs.getBoolean("auto_speak", true)) }
    var draft by remember { mutableStateOf("") }
    var partial by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var settingsNotice by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var webLoaded by remember { mutableStateOf(false) }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var completed by remember { mutableStateOf(progress.completedLessons()) }
    var cardsVersion by remember { mutableStateOf(0) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val provider = ChatEngine.providers.firstOrNull { it.id == providerId } ?: ChatEngine.providers.first()

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            listening = true
            status = "Listening… speak in English."
            activity.startListening(accent, { partial = it }, { result ->
                draft = result; partial = ""; listening = false; status = "Фраза распознана. Нажмите «Отправить»."
            }, { error -> status = error; listening = false; partial = "" })
        } else status = "Для голосового ввода нужно разрешение на микрофон."
    }

    fun beginListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            listening = true
            partial = ""
            status = "Listening… speak in English."
            activity.startListening(accent, { partial = it }, { result ->
                draft = result; partial = ""; listening = false; status = "Фраза распознана. Нажмите «Отправить»."
            }, { error -> status = error; listening = false; partial = "" })
        } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    suspend fun sendToWebChat(view: WebView, text: String): String {
        var attempts = 0
        while (!webLoaded && attempts++ < 40) delay(500)
        if (!webLoaded) throw IllegalStateException("Чат ещё загружается. Попробуйте через несколько секунд.")
        if (evaluateJs(view, ChatEngine.STATE_SCRIPT) != "ok") {
            throw IllegalStateException("Сначала войдите в аккаунт провайдера в окне чата выше.")
        }
        val prompt = ChatEngine.learnerPrompt(text, level)
        val injected = evaluateJs(view, ChatEngine.injectScript(prompt))
        if (injected != "ok" && injected != "typed?") throw IllegalStateException("Не удалось найти поле ввода на странице ИИ.")
        if (evaluateJs(view, ChatEngine.SUBMIT_SCRIPT) == "noinput") throw IllegalStateException("Поле ввода чата исчезло. Обновите страницу.")
        var latest = ""
        repeat(45) {
            delay(900)
            val obj = runCatching { JSONObject(evaluateJs(view, ChatEngine.READ_REPLY_SCRIPT)) }.getOrNull()
            val current = obj?.optString("text").orEmpty()
            if (current.length > latest.length) latest = current
            if (obj?.optBoolean("stable") == true && latest.length > 15) return latest
        }
        if (latest.length > 20) return latest
        throw IllegalStateException("ИИ не прислал ответ вовремя. Проверьте страницу чата и попробуйте снова.")
    }

    fun sendMessage(raw: String = draft) {
        val text = raw.trim()
        if (text.isBlank() || sending) return
        draft = ""
        partial = ""
        status = ""
        messages.add(ChatMessage("user", text))
        sending = true
        scope.launch {
            try {
                val answer = if (provider.isWebChat) {
                    val view = webView ?: throw IllegalStateException("Окно веб-чата ещё не готово. Подождите загрузки.")
                    sendToWebChat(view, text)
                } else {
                    val history = messages.dropLast(1).takeLast(10).map {
                        ChatTurn(if (it.role == "user") "user" else "assistant", it.text)
                    }
                    ChatEngine.askApi(ApiSettings(apiBase, apiModel, secrets.readApiKey()), level, history, text)
                }
                messages.add(ChatMessage("assistant", answer))
                if (autoSpeak) activity.speak(ChatEngine.spokenText(answer), accent)
                status = "Ответ готов. Можно продолжать разговор."
            } catch (error: Exception) {
                status = error.message ?: "Не удалось получить ответ."
            } finally {
                sending = false
            }
        }
    }

    Scaffold(bottomBar = {
        NavigationBar {
            val items = listOf(
                Triple(Page.HOME, "Домой", "⌂"), Triple(Page.CHAT, "Говорить", "🎙"),
                Triple(Page.LESSONS, "Уроки", "▤"), Triple(Page.REVIEW, "Повтор", "↻"),
                Triple(Page.PROGRESS, "Прогресс", "◷")
            )
            items.forEach { (target, title, icon) ->
                NavigationBarItem(
                    selected = page == target,
                    onClick = { page = target; selectedLesson = null },
                    icon = { Text(icon, fontSize = 19.sp) },
                    label = { Text(title) }
                )
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("EnglishWorld", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Speak your way to B2", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                TextButton(onClick = { page = Page.SETTINGS }) { Text("Настройки") }
            }
            when (page) {
                Page.HOME -> HomeScreen(level, completed.size, Curriculum.lessons.size,
                    { page = Page.CHAT }, { page = Page.LESSONS }, { page = Page.REVIEW }, { page = Page.PROGRESS })
                Page.CHAT -> ChatScreen(
                    providerId, provider.title, messages, draft, { draft = it }, partial, listening, sending,
                    status, webLoaded,
                    onMic = { if (listening) { activity.stopListening(); listening = false } else beginListening() },
                    onSend = { sendMessage() },
                    onSpeakLast = { messages.lastOrNull { it.role == "assistant" }?.let { activity.speak(ChatEngine.spokenText(it.text), accent) } },
                    webContent = {
                        if (provider.isWebChat) key(provider.id) {
                            AndroidView(
                                modifier = Modifier.fillMaxWidth().height(210.dp),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        webView = this
                                        CookieManager.getInstance().setAcceptCookie(true)
                                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.databaseEnabled = true
                                        settings.allowFileAccess = false
                                        settings.allowContentAccess = false
                                        settings.userAgentString = settings.userAgentString
                                            .replace("; wv", "").replace(Regex("Version/\\S+ "), "")
                                        webChromeClient = WebChromeClient()
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                CookieManager.getInstance().flush()
                                                webLoaded = true
                                            }
                                        }
                                        loadUrl(provider.url!!)
                                    }
                                },
                                update = { webView = it }
                            )
                        }
                    }
                )
                Page.LESSONS -> LessonsScreen(
                    selectedLesson, completed, { selectedLesson = it },
                    onStart = { lesson ->
                        selectedLesson = null
                        page = Page.CHAT
                        draft = lesson.speakingTask + "\n\nPlease guide me through this speaking task at my current level. Correct only the most important mistakes."
                    },
                    onComplete = { lesson ->
                        progress.markLessonCompleted(lesson.id)
                        completed = progress.completedLessons()
                    }
                )
                Page.REVIEW -> ReviewScreen(progress.allCards(), cardsVersion) { card, grade ->
                    progress.saveCard(StudyEngine.schedule(card, grade, System.currentTimeMillis()))
                    cardsVersion++
                }
                Page.PROGRESS -> ProgressScreen(completed.size, Curriculum.lessons.size)
                Page.SETTINGS -> SettingsScreen(
                    providerId, { id ->
                        providerId = id
                        prefs.edit().putString("provider", id).apply()
                        webLoaded = false
                        webView = null
                    },
                    apiBase, { apiBase = it }, apiModel, { apiModel = it },
                    apiKey, { apiKey = it }, level, { level = it; prefs.edit().putString("level", it).apply() },
                    accent, { accent = it; prefs.edit().putString("accent", it).apply() },
                    autoSpeak, { autoSpeak = it; prefs.edit().putBoolean("auto_speak", it).apply() },
                    settingsNotice,
                    onSave = {
                        prefs.edit().putString("api_base", apiBase.trim()).putString("api_model", apiModel.trim()).apply()
                        if (apiKey.isNotBlank()) secrets.saveApiKey(apiKey)
                        apiKey = ""
                        settingsNotice = "Настройки сохранены. Введённый ключ зашифрован Android Keystore."
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    level: String, completed: Int, total: Int,
    onSpeak: () -> Unit, onLessons: () -> Unit, onReview: () -> Unit, onPlan: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Твоя цель — уверенный B2", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Текущий уровень: $level. Главный тренажёр — разговор с ИИ. За ошибки здесь не ставят двойки и не звонят родителям.")
                Button(onClick = onSpeak, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text("🎙  Начать голосовой разговор", fontSize = 16.sp)
                }
            }
        }
        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Путь по программе", fontWeight = FontWeight.SemiBold)
                Text("$completed из $total уроков завершено")
                LinearProgressIndicator(
                    progress = if (total == 0) 0f else completed.toFloat() / total,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("4 уровня · 32 практических задания · от первых фраз до аргументации на B2", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onLessons) { Text("Открыть уроки") }
            }
        }
        Text("Ежедневные 20 минут", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Curriculum.strands.forEachIndexed { index, strand ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${(index + 1) * 5} мин", modifier = Modifier.width(58.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(strand.title, fontWeight = FontWeight.SemiBold)
                        Text(strand.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth()) { Text("Повторить фразы по расписанию") }
        TextButton(onClick = onPlan) { Text("Посмотреть критерии готовности к B2") }
    }
}

@Composable
private fun ChatScreen(
    providerId: String, providerTitle: String, messages: List<ChatMessage>,
    draft: String, onDraft: (String) -> Unit, partial: String,
    listening: Boolean, sending: Boolean, status: String, webLoaded: Boolean,
    onMic: () -> Unit, onSend: () -> Unit, onSpeakLast: () -> Unit,
    webContent: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Голосовой репетитор", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(providerTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Text(if (sending) "думает…" else if (listening) "слушает…" else "готов", color = MaterialTheme.colorScheme.secondary)
        }
        if (providerId != "api") {
            webContent()
            Text(
                if (webLoaded) "Веб-чат готов. Войди в аккаунт прямо здесь, если это первый запуск."
                else "Загружается страница провайдера…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Say anything in English.", fontWeight = FontWeight.Bold)
                        Text("Задай вопрос голосом или текстом: про времена, произношение, идиомы, письмо, экзамены или просто поговори.")
                        Text("ИИ ответит по-английски, объяснит важную ошибку и задаст следующий вопрос.")
                    }
                }
            }
            items(messages) { msg ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (msg.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Column(Modifier.padding(13.dp)) {
                            Text(if (msg.role == "user") "YOU" else "ENGLISHWORLD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(msg.text)
                        }
                    }
                }
            }
            if (sending) item { Text("Формулирую ответ…", color = MaterialTheme.colorScheme.secondary) }
        }
        if (partial.isNotBlank()) Text("🎙 $partial", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        OutlinedTextField(
            value = draft, onValueChange = onDraft, modifier = Modifier.fillMaxWidth(),
            label = { Text("Your English question") }, placeholder = { Text("How do I use the present perfect?") },
            maxLines = 3
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onMic, modifier = Modifier.weight(1f).height(50.dp), enabled = !sending) {
                Text(if (listening) "■ Стоп" else "🎙 Говорить")
            }
            Button(onClick = onSend, modifier = Modifier.weight(1f).height(50.dp), enabled = draft.isNotBlank() && !sending) {
                Text(if (sending) "…" else "Отправить")
            }
            OutlinedButton(onClick = onSpeakLast, modifier = Modifier.size(width = 58.dp, height = 50.dp)) { Text("🔊") }
        }
    }
}

@Composable
private fun LessonsScreen(
    selected: Lesson?, completed: Set<String>, onSelect: (Lesson?) -> Unit,
    onStart: (Lesson) -> Unit, onComplete: (Lesson) -> Unit
) {
    if (selected != null) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { onSelect(null) }) { Text("← Все уроки") }
            Text(selected.level.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(selected.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Фокус: ${selected.focus}", fontWeight = FontWeight.SemiBold)
            Text(selected.goal)
            Text("Полезные фразы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            selected.phrases.forEach { phrase -> Card { Text(phrase, Modifier.padding(12.dp)) } }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Speaking challenge", fontWeight = FontWeight.Bold)
                    Text(selected.speakingTask)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { onStart(selected) }, modifier = Modifier.fillMaxWidth()) { Text("Начать голосовую практику") }
                }
            }
            OutlinedButton(onClick = { onComplete(selected) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (selected.id in completed) "Урок уже отмечен" else "Отметить урок завершённым")
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Программа от A1 до B2", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Каждый урок заканчивается разговорной задачей. Нажми на карточку, чтобы увидеть цель и начать говорить.")
            }
            CefrLevel.entries.forEach { cefr ->
                item {
                    Text(cefr.label, modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                items(Curriculum.lessons.filter { it.level == cefr }) { lesson ->
                    Card(onClick = { onSelect(lesson) }, shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(lesson.title, fontWeight = FontWeight.SemiBold)
                                Text(lesson.focus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            Text(if (lesson.id in completed) "✓" else "›", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewScreen(cards: List<ReviewCard>, version: Int, onGrade: (ReviewCard, ReviewGrade) -> Unit) {
    val now = System.currentTimeMillis()
    val due = remember(cards, version) { StudyEngine.due(cards, now) }
    var index by remember(version) { mutableStateOf(0) }
    var showAnswer by remember(version, index) { mutableStateOf(false) }
    val card = due.getOrNull(index)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Интервальное повторение", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Вспоминай фразу до того, как откроешь перевод. Чем честнее оценка, тем полезнее расписание.")
        if (card == null) {
            Card {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("На сегодня всё!", style = MaterialTheme.typography.headlineSmall)
                    Text("Карточек к повторению сейчас нет. Мозг может выдохнуть — но только до следующего интервала.")
                }
            }
        } else {
            Text("Карточка ${index + 1} из ${due.size}", color = MaterialTheme.colorScheme.secondary)
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(card.front, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (showAnswer) {
                        Spacer(Modifier.height(22.dp))
                        Text(card.back, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            if (!showAnswer) {
                Button(onClick = { showAnswer = true }, modifier = Modifier.fillMaxWidth()) { Text("Показать перевод") }
            } else {
                Text("Насколько легко вспомнил?", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ReviewGrade.entries.forEach { grade ->
                        OutlinedButton(
                            onClick = { onGrade(card, grade); index++; showAnswer = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(when (grade) {
                                ReviewGrade.AGAIN -> "Снова"
                                ReviewGrade.HARD -> "Трудно"
                                ReviewGrade.GOOD -> "Хорошо"
                                ReviewGrade.EASY -> "Легко"
                            }, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(completed: Int, total: Int) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Проверка пути к B2", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("$completed / $total уроков завершено")
        LinearProgressIndicator(progress = if (total == 0) 0f else completed.toFloat() / total, modifier = Modifier.fillMaxWidth())
        Text("B2 — это не только список слов. Нужно уверенно участвовать в обсуждениях, развивать аргументы и объяснять преимущества и недостатки разных вариантов.")
        val criteria = listOf(
            "Vocabulary & grammar" to "Широкая лексика и сложные конструкции без постоянного поиска слов.",
            "Coherence" to "Развёрнутый ответ с понятной логикой, связками и выводом.",
            "Pronunciation" to "Понятная речь с управлением ударением, ритмом и интонацией.",
            "Interactive communication" to "Умение поддерживать беседу, уточнять и вежливо не соглашаться.",
            "Fluency" to "Объяснение позиции с небольшими паузами, а не только заученные фразы."
        )
        criteria.forEach { (title, description) ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("Сейчас приложение показывает учебный прогресс, а не официальный сертификат CEFR. Автоматическая оценка экзамена — отдельный этап разработки.")
    }
}

@Composable
private fun SettingsScreen(
    providerId: String, onProvider: (String) -> Unit,
    apiBase: String, onApiBase: (String) -> Unit,
    apiModel: String, onApiModel: (String) -> Unit,
    apiKey: String, onApiKey: (String) -> Unit,
    level: String, onLevel: (String) -> Unit,
    accent: String, onAccent: (String) -> Unit,
    autoSpeak: Boolean, onAutoSpeak: (Boolean) -> Unit,
    notice: String, onSave: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Настройки обучения", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Выбери способ подключения ИИ. Веб-режим использует официальный сайт и твой аккаунт; API-режим подключается к совместимому серверу.")
        Text("Провайдер", fontWeight = FontWeight.SemiBold)
        ChatEngine.providers.forEach { item ->
            FilterChip(selected = providerId == item.id, onClick = { onProvider(item.id) }, label = { Text(item.title) })
        }
        if (providerId == "api") {
            Text("OpenAI-compatible API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(apiBase, onApiBase, modifier = Modifier.fillMaxWidth(), label = { Text("Base URL") }, singleLine = true)
            OutlinedTextField(apiModel, onApiModel, modifier = Modifier.fillMaxWidth(), label = { Text("Model") }, singleLine = true)
            OutlinedTextField(
                apiKey, onApiKey, modifier = Modifier.fillMaxWidth(),
                label = { Text("API key (сохраняется зашифрованным)") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true
            )
            Text("Для Pollinations API укажите https://gen.pollinations.ai/v1 и свой действующий ключ. Ключ не встроен в приложение и не передаётся автору.")
        } else {
            Text("Открой вкладку «Говорить» и войди в сервис в верхнем окне. Сессия хранится в WebView на этом устройстве. Провайдер может менять интерфейс сайта, и автоматическую отправку иногда потребуется обновить.")
        }
        Text("Текущий уровень", fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CefrLevel.entries.forEach { item ->
                FilterChip(selected = level == item.name, onClick = { onLevel(item.name) }, label = { Text(item.name) })
            }
        }
        Text("Вариант произношения", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = accent == "en-US", onClick = { onAccent("en-US") }, label = { Text("American") })
            FilterChip(selected = accent == "en-GB", onClick = { onAccent("en-GB") }, label = { Text("British") })
        }
        FilterChip(selected = autoSpeak, onClick = { onAutoSpeak(!autoSpeak) }, label = { Text(if (autoSpeak) "🔊 Озвучивать ответы: да" else "Озвучивать ответы: нет") })
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Сохранить настройки") }
        if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.primary)
        Text("Приватность: история диалога не сохраняется приложением. Голос распознаётся системной службой Android; офлайн-режим зависит от наличия языкового пакета. Текст запроса отправляется выбранному ИИ-провайдеру.")
    }
}

private suspend fun evaluateJs(view: WebView, script: String): String =
    suspendCancellableCoroutine { continuation ->
        view.post {
            if (!continuation.isActive) return@post
            view.evaluateJavascript(script) { raw ->
                if (continuation.isActive) continuation.resume(ChatEngine.parseJavascriptString(raw))
            }
        }
    }
