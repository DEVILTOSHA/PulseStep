package com.example.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemini access through Firebase AI Logic. Requests go via Firebase's proxy, so no Gemini API key
 * is shipped inside the APK. Needs the app registered in a Firebase project (google-services.json);
 * without it every call fails and callers fall back to the rule-based engine.
 */
object GeminiClient {
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(MODEL_NAME)
    }

    suspend fun askGemini(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val text = model.generateContent(prompt).text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
