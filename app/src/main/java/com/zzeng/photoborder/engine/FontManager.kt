package com.zzeng.photoborder.engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zzeng.photoborder.data.model.FontConfig

class FontManager(private val context: Context) {

    private val gson = Gson()
    private var fonts: List<FontInfo> = emptyList()

    data class FontInfo(
        val id: String,
        val name: String,
        val family: String,
        val file: String,
        val category: String,
        val style: String
    )

    init {
        loadFonts()
    }

    private fun loadFonts() {
        try {
            context.assets.open("fonts/fonts_config.json").use { stream ->
                val json = stream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<Map<String, List<FontInfo>>>() {}.type
                val data: Map<String, List<FontInfo>> = gson.fromJson(json, type)
                fonts = data["fonts"] ?: emptyList()
            }
        } catch (e: Exception) {
            // Load default fonts if config not found
            fonts = getDefaultFonts()
        }
    }

    fun listFonts(): List<FontInfo> = fonts

    fun getFontName(id: String): String? {
        return fonts.find { it.id == id }?.name
    }

    fun getFontFamily(id: String): String? {
        return fonts.find { it.id == id }?.family
    }

    fun getFontFile(id: String): String? {
        return fonts.find { it.id == id }?.file
    }

    private fun getDefaultFonts(): List<FontInfo> {
        return listOf(
            FontInfo("yahei", "微软雅黑", "Microsoft YaHei", "msyh.ttc", "sans-serif", "modern"),
            FontInfo("arial", "Arial", "Arial", "arial.ttf", "sans-serif", "modern"),
            FontInfo("times", "Times New Roman", "Times New Roman", "times.ttf", "serif", "classic")
        )
    }
}
