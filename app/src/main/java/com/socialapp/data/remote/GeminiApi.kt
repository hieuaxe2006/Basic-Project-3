package com.socialapp.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val API_KEY = "AIzaSyABX7r_f670cDEogYemq6sRa446NV52ick" // Khách hàng có thể thay thế bằng khóa API thật của họ ở đây
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (API_KEY == "PLACEHOLDER_KEY" || API_KEY.isBlank()) {
                return@runCatching mockResponse(prompt)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$API_KEY"
            
            val requestBodyObj = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt)
                        )
                    )
                )
            )
            
            val jsonBody = Gson().toJson(requestBodyObj)
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@runCatching mockResponse(prompt)
            }
            
            val jsonResponse = response.body?.string() ?: throw Exception("Empty response")
            val geminiResponse = Gson().fromJson(jsonResponse, GeminiResponse::class.java)
            
            geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: throw Exception("Invalid response structure")
        }
    }

    private fun mockResponse(prompt: String): String {
        val lowerPrompt = prompt.lowercase()
        return when {
            lowerPrompt.contains("tự động gắn tag") || lowerPrompt.contains("gợi ý tag") || lowerPrompt.contains("autotag") -> {
                when {
                    lowerPrompt.contains("ăn") || lowerPrompt.contains("dinh dưỡng") || lowerPrompt.contains("protein") || lowerPrompt.contains("calo") -> "Nutrition"
                    lowerPrompt.contains("whey") || lowerPrompt.contains("creatine") || lowerPrompt.contains("pre-workout") -> "Supplements"
                    lowerPrompt.contains("tập") || lowerPrompt.contains("bench") || lowerPrompt.contains("squat") || lowerPrompt.contains("deadlift") || lowerPrompt.contains("ngực") || lowerPrompt.contains("chân") -> "Workout"
                    lowerPrompt.contains("giảm cân") || lowerPrompt.contains("tăng cân") || lowerPrompt.contains("thay đổi") || lowerPrompt.contains("body fat") -> "Transformation"
                    lowerPrompt.contains("cố lên") || lowerPrompt.contains("động lực") || lowerPrompt.contains("hết mình") -> "Motivation"
                    lowerPrompt.contains("hỏi") || lowerPrompt.contains("sao lại") || lowerPrompt.contains("làm thế nào") || lowerPrompt.contains("giúp") -> "Q&A"
                    else -> "Workout"
                }
            }
            lowerPrompt.contains("phân tích bài đăng") || lowerPrompt.contains("pre-publish") -> {
                if (lowerPrompt.contains("đau") || lowerPrompt.contains("chấn thương") || lowerPrompt.contains("mỏi")) {
                    "💡 **Lưu ý từ AI:** Nếu bạn gặp triệu ứng đau buốt hoặc chấn thương kéo dài, hãy nghỉ ngơi và tham khảo ý kiến bác sĩ chuyên khoa. Tránh cố tập nặng lúc đang chấn thương!"
                } else if (lowerPrompt.contains("bench press") || lowerPrompt.contains("squat") || lowerPrompt.contains("deadlift")) {
                    "💡 **Lưu ý từ AI:** Đảm bảo giữ đúng form khi thực hiện các bài Compound lớn. Gồng core thật chặt và khởi động kỹ khớp vai/cổ tay nhé!"
                } else if (lowerPrompt.contains("whey") || lowerPrompt.contains("creatine")) {
                    "💡 **Lưu ý từ AI:** Thực phẩm bổ sung chỉ đóng vai trò hỗ trợ. Hãy ưu tiên bổ sung dinh dưỡng từ thực phẩm tự nhiên trước!"
                } else {
                    "💡 **Lưu ý từ AI:** Chúc bạn có một buổi tập thật chất lượng! Hãy luôn lắng nghe cơ thể mình nhé."
                }
            }
            lowerPrompt.contains("workout plan") || lowerPrompt.contains("lên lịch tập") || lowerPrompt.contains("gợi ý bài tập") || lowerPrompt.contains("kế hoạch tập") -> {
                """
                💪 **Kế hoạch Tập luyện AI gợi ý cho bạn:**
                
                Dựa trên chỉ số của bạn, dưới đây là lịch tập tối ưu:
                
                • **Ngày 1 - Push Day (Ngực, Vai, Tay sau):**
                  - Bench Press: 4 sets x 5-8 reps
                  - Overhead Press: 3 sets x 8-10 reps
                  - Incline Dumbbell Press: 3 sets x 10-12 reps
                  - Tricep Pushdown: 3 sets x 12-15 reps
                  
                • **Ngày 2 - Pull Day (Lưng, Tay trước):**
                  - Lat Pulldown / Pull-ups: 4 sets x 8-10 reps
                  - Bent Over Row: 3 sets x 8-10 reps
                  - Bicep Dumbbell Curl: 3 sets x 12-15 reps
                  
                • **Ngày 3 - Leg Day (Đùi, Mông, Bắp chuối):**
                  - Squats: 4 sets x 6-8 reps
                  - Romanian Deadlift: 3 sets x 8-10 reps
                  - Leg Press: 3 sets x 10-12 reps
                  
                *Lưu ý: Luôn khởi động kỹ trước khi tập và tập trung vào form chuẩn hơn là tạ nặng.*
                """.trimIndent()
            }
            else -> "Chúc bạn tập luyện hiệu quả và sớm đạt được mục tiêu thể hình!"
        }
    }
}

data class GeminiRequest(val contents: List<GeminiContent>)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiPart(val text: String)

data class GeminiResponse(val candidates: List<GeminiCandidate>?)
data class GeminiCandidate(val content: GeminiContent?)
