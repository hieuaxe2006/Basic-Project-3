package com.socialapp.data.local

import android.content.Context
import com.google.firebase.Timestamp
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.socialapp.data.model.Post
import com.socialapp.data.model.Story
import com.socialapp.data.model.User
import com.socialapp.data.model.Notification
import com.socialapp.data.model.Group
import java.io.File
import java.lang.reflect.Type

class TimestampAdapter : JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {
    override fun serialize(src: Timestamp, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("seconds", src.seconds)
        obj.addProperty("nanoseconds", src.nanoseconds)
        return obj
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Timestamp {
        val obj = json.asJsonObject
        val seconds = obj.get("seconds").asLong
        val nanoseconds = obj.get("nanoseconds").asInt
        return Timestamp(seconds, nanoseconds)
    }
}

data class CachedFeedData(
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
    val userMap: Map<String, User> = emptyMap(),
    val likedIds: Set<String> = emptySet(),
    val savedIds: Set<String> = emptySet()
)

data class CachedProfileData(
    val user: User? = null,
    val postedPosts: List<Post> = emptyList(),
    val savedPosts: List<Post> = emptyList()
)

class LocalCacheManager(context: Context) {
    private val contextRef = context.applicationContext
    private val feedCacheFile = File(contextRef.cacheDir, "feed_cache.json")
    private val notifCacheFile = File(contextRef.cacheDir, "notif_cache.json")
    private val groupsCacheFile = File(contextRef.cacheDir, "groups_cache.json")
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(Timestamp::class.java, TimestampAdapter())
        .create()

    fun saveFeedCache(data: CachedFeedData) {
        try {
            val json = gson.toJson(data)
            feedCacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadFeedCache(): CachedFeedData? {
        return try {
            if (feedCacheFile.exists()) {
                val json = feedCacheFile.readText()
                val type = object : TypeToken<CachedFeedData>() {}.type
                gson.fromJson<CachedFeedData>(json, type)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveNotificationsCache(notifications: List<Notification>) {
        try {
            val json = gson.toJson(notifications)
            notifCacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadNotificationsCache(): List<Notification>? {
        return try {
            if (notifCacheFile.exists()) {
                val json = notifCacheFile.readText()
                val type = object : TypeToken<List<Notification>>() {}.type
                gson.fromJson<List<Notification>>(json, type)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveProfileCache(uid: String, data: CachedProfileData) {
        try {
            val file = File(contextRef.cacheDir, "profile_${uid}_cache.json")
            val json = gson.toJson(data)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadProfileCache(uid: String): CachedProfileData? {
        return try {
            val file = File(contextRef.cacheDir, "profile_${uid}_cache.json")
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<CachedProfileData>() {}.type
                gson.fromJson<CachedProfileData>(json, type)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveGroupsCache(groups: List<Group>) {
        try {
            val json = gson.toJson(groups)
            groupsCacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadGroupsCache(): List<Group>? {
        return try {
            if (groupsCacheFile.exists()) {
                val json = groupsCacheFile.readText()
                val type = object : TypeToken<List<Group>>() {}.type
                gson.fromJson<List<Group>>(json, type)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
