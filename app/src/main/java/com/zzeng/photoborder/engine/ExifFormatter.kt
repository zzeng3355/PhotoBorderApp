package com.zzeng.photoborder.engine

import com.zzeng.photoborder.data.model.ExifData
import com.zzeng.photoborder.data.model.FormatRule
import com.zzeng.photoborder.data.model.Template

class ExifFormatter {

    fun process(exifData: ExifData, template: Template): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Camera brand
        val brand = simplifyBrand(exifData.make, template.brandModelMap)
        result["camera_brand"] = brand

        // Camera model
        result["camera_model"] = simplifyModel(exifData.model, template.modelSimplification)

        // Lens model
        result["lens_model"] = simplifyLens(exifData.lensModel)

        // Aperture
        result["aperture"] = formatAperture(exifData.aperture)

        // Shutter speed
        result["shutter"] = formatShutterSpeed(exifData.shutterSpeed)

        // ISO
        result["iso"] = exifData.iso

        // Focal length
        result["focal"] = formatFocalLength(exifData.focalLength)

        // Date
        result["date"] = formatDate(exifData.dateTimeOriginal)

        return result
    }

    private fun simplifyBrand(brand: String, brandMap: Map<String, String>): String {
        return brandMap[brand] ?: brand
    }

    private fun simplifyModel(model: String, simplificationMap: Map<String, String>): String {
        simplificationMap.forEach { (key, value) ->
            if (model.contains(key)) {
                return value
            }
        }
        return model
    }

    private fun simplifyLens(lens: String): String {
        if (lens.isBlank() || lens == "N/A") return ""
        return lens.replace(Regex("^(NIKKOR|SONY|Canon|FUJINON|SIGMA|TAMRON)\\s*"), "").trim()
    }

    private fun formatAperture(aperture: String): String {
        if (aperture.isBlank()) return ""
        return try {
            val value = aperture.replace("f/", "").replace("F", "").toFloat()
            if (value == value.toInt().toFloat()) {
                "F${value.toInt()}"
            } else {
                "F$value"
            }
        } catch (e: Exception) {
            aperture
        }
    }

    private fun formatShutterSpeed(shutter: String): String {
        if (shutter.isBlank()) return ""
        return try {
            if (shutter.contains("/")) {
                shutter
            } else {
                val value = shutter.toFloat()
                if (value >= 1) {
                    "${value.toInt()}s"
                } else {
                    val denom = (1 / value).toInt()
                    "1/${denom}s"
                }
            }
        } catch (e: Exception) {
            shutter
        }
    }

    private fun formatFocalLength(focal: String): String {
        if (focal.isBlank()) return ""
        return try {
            focal.replace("mm", "").replace(" ", "").toFloat().toInt().toString()
        } catch (e: Exception) {
            focal
        }
    }

    private fun formatDate(dateStr: String): String {
        if (dateStr.isBlank() || dateStr == "N/A") return ""
        return try {
            val parts = dateStr.split(" ")[0].split(":")
            val year = parts[0]
            val monthNum = parts[1].toInt()
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = months[monthNum]
            "Photo by ZZeng @ $monthName $year"
        } catch (e: Exception) {
            ""
        }
    }
}
