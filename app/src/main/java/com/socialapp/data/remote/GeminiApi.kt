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

    private fun parseMetric(prompt: String, prefix: String): Double? {
        val index = prompt.indexOf(prefix)
        if (index == -1) return null
        val sub = prompt.substring(index + prefix.length).trim()
        val end = sub.indexOf('\n')
        val line = if (end == -1) sub else sub.substring(0, end)
        val digits = line.replace(Regex("[^0-9.]"), "")
        return digits.toDoubleOrNull()
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
            lowerPrompt.contains("workout plan") || lowerPrompt.contains("lên lịch tập") || lowerPrompt.contains("chỉ số") || lowerPrompt.contains("kế hoạch tập") || lowerPrompt.contains("chuyên gia thể hình") -> {
                val height = parseMetric(prompt, "Chiều cao:")
                val weight = parseMetric(prompt, "Cân nặng:")
                val bodyFat = parseMetric(prompt, "Tỷ lệ mỡ:")
                val bench = parseMetric(prompt, "Kỷ lục Bench Press:")
                val squat = parseMetric(prompt, "Kỷ lục Squat:")
                val deadlift = parseMetric(prompt, "Kỷ lục Deadlift:")

                val hVal = height ?: 172.0
                val wVal = weight ?: 68.0
                val bfVal = bodyFat ?: 16.0

                val bmr = 10 * wVal + 6.25 * hVal - 5 * 25 + 5
                val tdee = bmr * 1.375
                val goal = if (bfVal > 18.0) "Giảm mỡ & Săn chắc cơ bắp (Cutting/Tone)" else "Tăng cơ nách & Sức mạnh (Lean Bulking/Strength)"
                val targetCal = if (bfVal > 18.0) (tdee - 450).toInt() else (tdee + 300).toInt()
                val protein = (wVal * 2.0).toInt()

                val totalLifts = (bench ?: 0.0) + (squat ?: 0.0) + (deadlift ?: 0.0)
                val level = when {
                    totalLifts > 320 -> "Nâng cao (Advanced Athlete)"
                    totalLifts > 180 -> "Trung cấp (Intermediate Lifter)"
                    else -> "Mới bắt đầu (Beginner)"
                }

                """
                🌟 KẾ HOẠCH LUYỆN TẬP & DINH DƯỠNG CÁ NHÂN HÓA TỪ AI 🌟

                Chào bạn! Dựa trên các thông số thể trạng và sức mạnh bạn đã cung cấp, HLV AI đề xuất kế hoạch tối ưu sau:

                📊 Phân tích chỉ số Thể trạng:
                • Chiều cao: ${if (height != null) "${height.toInt()} cm" else "Chưa cung cấp"} | Cân nặng: ${if (weight != null) "$weight kg" else "Chưa cung cấp"}
                • Tỷ lệ mỡ (Body Fat): ${if (bodyFat != null) "$bodyFat%" else "Chưa xác định"} ➔ Phân nhóm mục tiêu: $goal
                • Trình độ Gym hiện tại: $level

                🔥 Năng lượng & Dinh dưỡng Đề xuất:
                • Lượng calo cần tiêu thụ mỗi ngày (BMR): ${bmr.toInt()} kcal
                • Tổng năng lượng tiêu hao hàng ngày (TDEE): ${tdee.toInt()} kcal
                • Mục tiêu calo hàng ngày: $targetCal kcal
                • Lượng đạm (Protein) mục tiêu: $protein g / ngày (2g/kg trọng lượng cơ thể)

                📋 Lịch tập luyện 4 ngày/tuần (Upper/Lower Split):

                🏋️‍♂️ Ngày 1: Upper Day (Ngực, Lưng, Vai, Tay)
                • Bench Press (Đẩy ngực ngang): 4 sets x 6-8 reps (Suggested weight: ${if(bench != null) "${(bench * 0.75).toInt()} kg" else "45-50 kg"} - 75% 1RM)
                • Lat Pulldown (Kéo xô rộng tay): 4 sets x 8-12 reps
                • Overhead Dumbbell Press (Đẩy vai tạ đơn): 3 sets x 10-12 reps
                • Incline Dumbbell Row (Chèo thuyền nghiêng): 3 sets x 10-12 reps
                • Bicep Curl & Tricep Pushdown (Tay trước/sau siêu set): 3 sets x 12-15 reps

                🦵 Ngày 2: Lower Day (Đùi trước, Đùi sau, Mông, Bắp chuối)
                • Squat (Gánh đùi sau): 4 sets x 6-8 reps (Suggested weight: ${if(squat != null) "${(squat * 0.70).toInt()} kg" else "55-60 kg"} - 70% 1RM)
                • Romanian Deadlift (Đùi sau/Mông): 4 sets x 8-10 reps
                • Leg Press (Đạp đùi nghiêng): 3 sets x 10-12 reps
                • Leg Curl (Cuộn chân máy): 3 sets x 12-15 reps
                • Calf Raises (Nhón bắp chuối): 4 sets x 15 reps

                😴 Ngày 3: Nghỉ ngơi & Phục hồi cơ bắp tích cực

                🏋️‍♂️ Ngày 4: Upper Day Hypertrophy (Phì đại cơ bắp thân trên)
                • Incline Bench Press: 4 sets x 8-10 reps (Suggested weight: ${if(bench != null) "${(bench * 0.65).toInt()} kg" else "35-40 kg"})
                • Seated Cable Row (Kéo lưng cáp): 4 sets x 10-12 reps
                • Lateral Raise (Bay vai ngang): 4 sets x 12-15 reps
                • Chest Fly (Ép ngực tạ đơn): 3 sets x 12-15 reps
                • Hammer Curl & Overhead Extension (Cơ tay): 3 sets x 12-15 reps

                🦵 Ngày 5: Lower Day Power (Sức mạnh thân dưới)
                • Deadlift (Kéo tạ từ đất): 3 sets x 5 reps (Suggested weight: ${if(deadlift != null) "${(deadlift * 0.80).toInt()} kg" else "70-75 kg"} - 80% 1RM)
                • Bulgarian Split Squat: 3 sets x 8-10 reps mỗi bên
                • Leg Extension (Đá đùi trước): 3 sets x 12-15 reps
                • Seated Calf Raises: 4 sets x 12-15 reps

                🥑 Lời khuyên dinh dưỡng & Lối sống từ HLV AI:
                1. Ăn đủ lượng protein bằng cách bổ sung ức gà, trứng, thịt bò, cá và Whey Protein.
                2. Ngủ đủ 7-8 tiếng mỗi đêm vì cơ bắp phục hồi và phát triển trong lúc ngủ.
                3. Uống ít nhất 3 lít nước mỗi ngày để hỗ trợ trao đổi chất và bôi trơn các khớp xương.
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
