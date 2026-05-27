package com.zzeng.photoborder.engine

import android.content.Context
import com.google.gson.Gson
import com.zzeng.photoborder.data.model.Template
import java.io.File
import java.io.FileOutputStream

class TemplateManager(private val context: Context) {

    private val gson = Gson()
    private val templatesDir by lazy {
        File(context.filesDir, "templates").apply { mkdirs() }
    }

    fun loadTemplate(name: String): Template? {
        return try {
            // First check user templates
            val userTemplate = File(templatesDir, "$name.json")
            if (userTemplate.exists()) {
                return gson.fromJson(userTemplate.readText(), Template::class.java)
            }

            // Then check assets
            context.assets.open("templates/$name.json").use { stream ->
                val json = stream.bufferedReader().use { it.readText() }
                gson.fromJson(json, Template::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun saveTemplate(template: Template): Boolean {
        return try {
            val file = File(templatesDir, "${template.templateName}.json")
            file.writeText(gson.toJson(template))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun importTemplate(jsonString: String): Boolean {
        return try {
            val template = gson.fromJson(jsonString, Template::class.java)
            saveTemplate(template)
        } catch (e: Exception) {
            false
        }
    }

    fun listTemplates(): List<String> {
        val userTemplates = templatesDir.listFiles { _, name ->
            name.endsWith(".json")
        }?.map { it.nameWithoutExtension } ?: emptyList()

        val assetTemplates = try {
            context.assets.list("templates")?.map {
                it.removeSuffix(".json")
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return (userTemplates + assetTemplates).distinct()
    }

    fun deleteTemplate(name: String): Boolean {
        val file = File(templatesDir, "$name.json")
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
