package com.jarvis.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var audioManager: AudioManager

    private lateinit var tvStatus: TextView
    private lateinit var tvTranscript: TextView
    private lateinit var tvJarvisReply: TextView
    private lateinit var tvDailyAffirmation: TextView
    private lateinit var tvAffirmationSpeaker: TextView
    private lateinit var ivArcReactor: ImageView
    private lateinit var btnMic: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    // Iron Man Motivational Hindi Affirmations
    private val ironManQuotes = arrayOf(
        "सर, कभी-कभी आपको दौड़ने से पहले सीधे उड़ना पड़ता है। आज का कार्य तुरंत आरंभ करें।" to "Tony Stark // Mark-III",
        "अनुशासन आपकी असीम क्षमताओं को अनलॉक करने का न्यूक्लियर रिएक्टर है, सर।" to "J.A.R.V.I.S. // Arc Core",
        "असफलता केवल एक नया डेटा पॉइंट है। उठिए, आर्मर अपग्रेड कीजिये और फिर से प्रहार कीजिये।" to "Tony Stark // Hulkbuster",
        "सर, आपका आर्क रिएक्टर 100% क्षमता पर है। आज अपने लक्ष्यों को पूरा करें।" to "J.A.R.V.I.S. // Protocol 85",
        "शानदार परिणाम साधारण प्रयासों से नहीं मिलते। असाधारण परिश्रम चुनिए, सर।" to "Tony Stark // Titanium"
    )

    private var isListening = false
    private val RECORD_AUDIO_REQUEST_CODE = 101

    // OkHttp client for Gemini AI API
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        initViews()
        setupDailyAffirmation()
        initTextToSpeech()
        checkPermissions()
    }

    private fun setupDailyAffirmation() {
        val (quoteText, speaker) = ironManQuotes.random()
        tvDailyAffirmation.text = "\"$quoteText\""
        tvAffirmationSpeaker.text = "— $speaker (टैप करके सुनें)"
        findViewById<View>(R.id.cardAffirmation).setOnClickListener {
            speak(quoteText)
        }
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvTranscript = findViewById(R.id.tvTranscript)
        tvJarvisReply = findViewById(R.id.tvJarvisReply)
        tvDailyAffirmation = findViewById(R.id.tvDailyAffirmation)
        tvAffirmationSpeaker = findViewById(R.id.tvAffirmationSpeaker)
        ivArcReactor = findViewById(R.id.ivArcReactor)
        btnMic = findViewById(R.id.btnMic)
        progressBar = findViewById(R.id.progressBar)

        // Arc Reactor rotation animation
        val rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_arc)
        ivArcReactor.startAnimation(rotateAnim)

        btnMic.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }

        // Quick Command Chips
        findViewById<View>(R.id.chipYoutube).setOnClickListener { executeCommand("open youtube") }
        findViewById<View>(R.id.chipWhatsapp).setOnClickListener { executeCommand("open whatsapp") }
        findViewById<View>(R.id.chipTime).setOnClickListener { tellCurrentTime() }
        findViewById<View>(R.id.chipScrollDown).setOnClickListener { scrollDevice(false) }
        findViewById<View>(R.id.chipBack).setOnClickListener { goBack() }
        findViewById<View>(R.id.btnAccessibility).setOnClickListener { openAccessibilitySettings() }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST_CODE
            )
        } else {
            setupSpeechRecognizer()
        }
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    tvStatus.text = "JARVIS LISTENING... (बोलिये सर)"
                    progressBar.visibility = View.VISIBLE
                }

                override fun onBeginningOfSpeech() {
                    tvStatus.text = "PROCESSING VOICE..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Reactive Arc Reactor scale based on voice pitch/volume
                    val scale = 1f + (rmsdB / 40f).coerceIn(0f, 0.35f)
                    ivArcReactor.scaleX = scale
                    ivArcReactor.scaleY = scale
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    progressBar.visibility = View.INVISIBLE
                    stopListening()
                }

                override fun onError(error: Int) {
                    progressBar.visibility = View.INVISIBLE
                    isListening = false
                    tvStatus.text = "JARVIS ONLINE (स्टैंडबाय)"
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val voiceText = matches[0]
                        tvTranscript.text = "आप: \"$voiceText\""
                        handleUserVoice(voiceText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        tvTranscript.text = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        if (!::speechRecognizer.isInitialized) {
            setupSpeechRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS सुन रहा है...")
        }
        speechRecognizer.startListening(intent)
        isListening = true
        btnMic.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun stopListening() {
        if (::speechRecognizer.isInitialized && isListening) {
            speechRecognizer.stopListening()
        }
        isListening = false
        btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
        tvStatus.text = "JARVIS ONLINE (तैयार)"
    }

    /**
     * Process Voice Command:
     * 1. Check local device intents (YouTube, WhatsApp, Volume, Scroll, Back, Lock)
     * 2. If conversational query, send to Gemini AI Brain
     */
    private fun handleUserVoice(command: String) {
        val lower = command.lowercase(Locale.ROOT)

        when {
            // Wake Word Ack
            lower.contains("hey jarvis") || lower.contains("हे जार्विस") || lower.contains("सुनो जार्विस") -> {
                speak("हाँ सर, मैं सुन रहा हूँ। आज्ञा दीजिये।")
            }
            // App Launchers
            lower.contains("youtube") || lower.contains("यूट्यूब") -> {
                speak("यूट्यूब खोला जा रहा है, सर।")
                launchApp("com.google.android.youtube", "https://youtube.com")
            }
            lower.contains("whatsapp") || lower.contains("व्हाट्सएप") -> {
                speak("व्हाट्सएप खोला जा रहा है, सर।")
                launchApp("com.whatsapp", null)
            }
            lower.contains("chrome") || lower.contains("क्रोम") || lower.contains("browser") -> {
                speak("गूगल क्रोम खोला जा रहा है।")
                launchApp("com.android.chrome", "https://google.com")
            }
            lower.contains("settings") || lower.contains("सेटिंग") -> {
                speak("फोन की सेटिंग्स खोली जा रही हैं।")
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            // System Actions via Accessibility Service
            lower.contains("back") || lower.contains("पीछे जाओ") || lower.contains("वापस जाओ") -> {
                speak("वापस जा रहे हैं।")
                goBack()
            }
            lower.contains("scroll down") || lower.contains("नीचे स्क्रॉल") || lower.contains("नीचे करो") -> {
                speak("स्क्रीन नीचे स्क्रॉल कर रहा हूँ।")
                scrollDevice(false)
            }
            lower.contains("scroll up") || lower.contains("ऊपर स्क्रॉल") || lower.contains("ऊपर करो") -> {
                speak("स्क्रीन ऊपर स्क्रॉल कर रहा हूँ।")
                scrollDevice(true)
            }
            lower.contains("lock") || lower.contains("फोन लॉक") || lower.contains("स्क्रीन बंद") -> {
                speak("डिवाइस लॉक किया जा रहा है, सर।")
                lockDevice()
            }
            lower.contains("volume up") || lower.contains("आवाज बढ़ाओ") -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                speak("आवाज बढ़ा दी गई है।")
            }
            lower.contains("volume down") || lower.contains("आवाज कम") -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                speak("आवाज कम कर दी गई है।")
            }
            lower.contains("time") || lower.contains("समय") || lower.contains("तारीख") || lower.contains("date") -> {
                tellCurrentTime()
            }
            // Otherwise: Intelligent AI Brain Response
            else -> {
                queryGeminiAI(command)
            }
        }
    }

    private fun executeCommand(cmd: String) {
        handleUserVoice(cmd)
    }

    private fun launchApp(packageName: String, fallbackUrl: String?) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else if (fallbackUrl != null) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
            startActivity(webIntent)
        } else {
            speak("सर, यह ऐप आपके फोन में स्थापित नहीं है।")
        }
    }

    private fun goBack() {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.performBack()
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun scrollDevice(isUp: Boolean) {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.scrollScreen(isUp)
        } else {
            Toast.makeText(this, "कृपया पहले Accessibility Service चालू करें", Toast.LENGTH_SHORT).show()
            openAccessibilitySettings()
        }
    }

    private fun lockDevice() {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.lockScreen()
        } else {
            Toast.makeText(this, "कृपया पहले Accessibility Service चालू करें", Toast.LENGTH_SHORT).show()
            openAccessibilitySettings()
        }
    }

    private fun tellCurrentTime() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale("hi", "IN"))
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN"))
        val now = Date()
        val text = "सर, अभी समय है ${timeFormat.format(now)}, और आज ${dateFormat.format(now)} है।"
        tvJarvisReply.text = text
        speak(text)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Jarvis AI Accessibility Service को ऑन करें", Toast.LENGTH_LONG).show()
    }

    /**
     * Connect to Gemini AI for natural conversational Hindi intelligence
     */
    private fun queryGeminiAI(userPrompt: String) {
        tvStatus.text = "JARVIS BRAIN THINKING..."
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Point to your Gemini AI Cloud backend endpoint
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
                
                val jsonBody = JSONObject().apply {
                    put("contents", org.json.JSONArray().put(
                        JSONObject().put("parts", org.json.JSONArray().put(
                            JSONObject().put("text", "You are JARVIS, Tony Stark's AI assistant. Answer respectfully in natural Hindi. User says: $userPrompt")
                        ))
                    ))
                }

                // In production, Gemini API key is injected via secure proxy or local BuildConfig
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.INVISIBLE
                    tvStatus.text = "JARVIS ONLINE (स्टैंडबाय)"
                    
                    if (response.isSuccessful && responseBody != null) {
                        val replyText = parseGeminiText(responseBody)
                        tvJarvisReply.text = replyText
                        speak(replyText)
                    } else {
                        val fallback = "सर, आपकी आज्ञा पर विचार किया गया: $userPrompt. मैं आपकी और क्या सेवा करूँ?"
                        tvJarvisReply.text = fallback
                        speak(fallback)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.INVISIBLE
                    val errorMsg = "सर, नेटवर्क कनेक्शन में क्षणिक समस्या है। मैं लोकल कमांड्स के लिए तैयार हूँ।"
                    tvJarvisReply.text = errorMsg
                    speak(errorMsg)
                }
            }
        }
    }

    private fun parseGeminiText(jsonStr: String): String {
        return try {
            val root = JSONObject(jsonStr)
            val candidates = root.getJSONArray("candidates")
            val first = candidates.getJSONObject(0)
            val content = first.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            "हाँ सर, मैं समझ गया।"
        }
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.ENGLISH)
            }
            textToSpeech.setPitch(0.95f)
            textToSpeech.setSpeechRate(1.0f)
            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    runOnUiThread { tvStatus.text = "JARVIS SPEAKING..." }
                }
                override fun onDone(utteranceId: String?) {
                    runOnUiThread { tvStatus.text = "JARVIS ONLINE (स्टैंडबाय)" }
                }
                override fun onError(utteranceId: String?) {
                    runOnUiThread { tvStatus.text = "JARVIS ONLINE" }
                }
            })
            // Initial greeting
            speak("जार्विस सिस्टम ऑनलाइन, सर। रेडमी 12सी तैयार है।")
        }
    }

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_UTTERANCE")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}