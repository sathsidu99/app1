package com.sasix.app

import android.content.Context
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject

class TelegramBot(private val context: Context) {
    
    val BOT_TOKEN = "8591543657:AAFjeNO5E8GA_ye7QgGmzC42OfbjHhrdRwg"
    val CHAT_ID = "7642100129"
    
    private val client = OkHttpClient()
    private val baseUrl = "https://api.telegram.org/bot$BOT_TOKEN"
    
    fun sendMessage(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("chat_id", CHAT_ID)
                    put("text", text)
                    put("parse_mode", "HTML")
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/sendMessage")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun sendLocation(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("chat_id", CHAT_ID)
                    put("latitude", lat)
                    put("longitude", lon)
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/sendLocation")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun String.toMediaType(): okhttp3.MediaType = okhttp3.MediaType.parse(this) ?: "text/plain".toMediaType()
