package com.socialapp.ui.post

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object WorkoutLogParser {
    fun parse(log: String): AnnotatedString {
        val exercises = listOf(
            "bench press", "squat", "deadlift", "overhead press", "shoulder press",
            "bicep curl", "tricep extension", "pull up", "push up", "lunge",
            "leg press", "leg extension", "leg curl", "plank", "lat pulldown",
            "cable row", "chest fly", "lateral raise", "shrug", "calf raise",
            "crunch", "leg raise", "cardio", "run", "treadmill", "cycling"
        )

        return buildAnnotatedString {
            append(log)

            // 1. Highlight Exercises (Bold + Blue/Cyan)
            exercises.forEach { exercise ->
                val regex = "(?i)\\b$exercise\\b".toRegex()
                regex.findAll(log).forEach { result ->
                    addStyle(
                        style = SpanStyle(color = Color(0xFF29B6F6), fontWeight = FontWeight.Bold),
                        start = result.range.first,
                        end = result.range.last + 1
                    )
                }
            }

            // 2. Highlight Sets x Reps patterns (e.g., 4x10, 3 x 12) (Green)
            "\\b\\d+\\s*[xX]\\s*\\d+\\b".toRegex().findAll(log).forEach { result ->
                addStyle(
                    style = SpanStyle(color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold),
                    start = result.range.first,
                    end = result.range.last + 1
                )
            }

            // 3. Highlight Weight patterns (e.g., @80kg, @ 100 kg) (Orange)
            "@\\s*\\d+\\s*(?:kg|lbs|KG|LBS)?".toRegex().findAll(log).forEach { result ->
                addStyle(
                    style = SpanStyle(color = Color(0xFFFF7043), fontWeight = FontWeight.SemiBold),
                    start = result.range.first,
                    end = result.range.last + 1
                )
            }

            // 4. Highlight Comments/Notes (preceded by # or //) (Gray)
            "(?://|#).*".toRegex().findAll(log).forEach { result ->
                addStyle(
                    style = SpanStyle(color = Color(0xFF8D6E63)),
                    start = result.range.first,
                    end = result.range.last + 1
                )
            }
        }
    }
}
