package com.zzeng.photoborder.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import com.zzeng.photoborder.data.model.ExifData
import com.zzeng.photoborder.data.model.Template
import java.io.File
import java.io.FileOutputStream

class BorderEngine(private val context: Context) {

    private val templateManager = TemplateManager(context)
    private val exifFormatter = ExifFormatter()
    private val logoCache = mutableMapOf<String, Bitmap>()

    fun processImage(
        imageUri: Uri,
        templateName: String = "zzeng_minimal",
        outputFile: File
    ): Result<File> {
        return try {
            val template = templateManager.loadTemplate(templateName)
                ?: return Result.failure(Exception("Template not found: $templateName"))

            val bitmap = loadBitmap(imageUri)
                ?: return Result.failure(Exception("Failed to load image"))

            val exifData = readExif(imageUri)
            val processedData = exifFormatter.process(exifData, template)
            val resultBitmap = renderBorder(bitmap, template, processedData)

            saveImage(resultBitmap, outputFile)
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun readExif(uri: Uri): ExifData {
        return try {
            val exifReader = com.zzeng.photoborder.data.exif.ExifReader(context)
            exifReader.readExif(uri)
        } catch (e: Exception) {
            ExifData()
        }
    }

    private fun renderBorder(
        originalBitmap: Bitmap,
        template: Template,
        data: Map<String, String>
    ): Bitmap {
        val origWidth = originalBitmap.width
        val origHeight = originalBitmap.height
        val isPortrait = origHeight > origWidth

        val borderRatio = if (isPortrait) {
            template.layout.borderHeightRatioPortrait
        } else {
            template.layout.borderHeightRatioLandscape
        }
        val borderHeight = if (isPortrait) {
            (origWidth * borderRatio).toInt()
        } else {
            (origHeight * borderRatio).toInt()
        }

        val baseWidth = template.baseResolution.width
        val scale = origWidth.toFloat() / baseWidth.toFloat()

        val newHeight = origHeight + borderHeight
        val resultBitmap = Bitmap.createBitmap(origWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        val bgPaint = Paint().apply {
            color = Color.parseColor(template.layout.backgroundColor)
        }
        canvas.drawRect(
            0f, origHeight.toFloat(),
            origWidth.toFloat(), newHeight.toFloat(),
            bgPaint
        )

        if (isPortrait) {
            renderPortrait(canvas, template, data, origWidth, origHeight, borderHeight, scale)
        } else {
            renderLandscape(canvas, template, data, origWidth, origHeight, borderHeight, scale)
        }

        return resultBitmap
    }

    private fun renderLandscape(
        canvas: Canvas,
        template: Template,
        data: Map<String, String>,
        origWidth: Int,
        origHeight: Int,
        borderHeight: Int,
        scale: Float
    ) {
        val sideMargin = (origWidth * template.layout.sideMarginRatio).toInt()
        val centerGap = (origWidth * template.layout.centerGapRatio).toInt()
        val brandModelSpacing = (borderHeight * 0.12).toInt()

        val textPaint = Paint().apply { isAntiAlias = true }
        val brand = data["camera_brand"] ?: ""
        val model = data["camera_model"] ?: ""
        val lens = data["lens_model"] ?: ""
        val params = buildParamsString(data, false)
        val photoBy = data["date"] ?: ""

        var logoWidth = 0
        var logoHeight = 0
        var logoBitmap: Bitmap? = null

        template.brandLogos[brand]?.let { logoConfig ->
            val targetHeight = (borderHeight * logoConfig.heightRatio * scale).toInt()
            logoBitmap = loadLogoBitmap(logoConfig.file, targetHeight)
            logoBitmap?.let {
                logoWidth = it.width
                logoHeight = it.height
            }
        }

        val fontCamera = template.fonts["camera"] ?: return
        val fontLens = template.fonts["lens"] ?: return
        val fontParams = template.fonts["params"] ?: return
        val fontPhotoBy = template.fonts["photo_by"] ?: return
        val fontZZ = template.fonts["zz"] ?: return

        textPaint.textSize = borderHeight * fontCamera.sizeRatio * scale
        val modelWidth = textPaint.measureText(model)
        val cameraLineWidth = logoWidth + brandModelSpacing + modelWidth

        var leftColWidth = cameraLineWidth.toInt()
        if (lens.isNotEmpty()) {
            textPaint.textSize = borderHeight * fontLens.sizeRatio * scale
            leftColWidth = maxOf(leftColWidth, textPaint.measureText(lens).toInt())
        }

        var rightColWidth = 0
        if (params.isNotEmpty()) {
            textPaint.textSize = borderHeight * fontParams.sizeRatio * scale
            rightColWidth = textPaint.measureText(params).toInt()
        }

        if (photoBy.isNotEmpty()) {
            val prefix = "Photo by "
            val suffix = photoBy.replace("Photo by ZZeng @ ", "")
            val fullSuffix = "eng @ $suffix"
            textPaint.textSize = borderHeight * fontPhotoBy.sizeRatio * scale
            val prefixWidth = textPaint.measureText(prefix)
            val suffixWidth = textPaint.measureText(fullSuffix)
            textPaint.textSize = borderHeight * fontZZ.sizeRatio * scale
            val zWidth = textPaint.measureText("Z")
            val zzWidth = (zWidth * 1.45).toInt()
            val photoByWidth = prefixWidth + zzWidth + suffixWidth
            rightColWidth = maxOf(rightColWidth, photoByWidth.toInt())
        }

        val totalContentWidth = leftColWidth + centerGap + rightColWidth
        val availableWidth = origWidth - sideMargin * 2
        var dynamicScale = 1.0f

        if (totalContentWidth > availableWidth) {
            dynamicScale = availableWidth.toFloat() / totalContentWidth.toFloat() * 0.92f
        }

        val finalScale = scale * dynamicScale

        val cameraY = origHeight + borderHeight * 0.24f
        val lensY = origHeight + borderHeight * 0.54f
        val paramsY = origHeight + borderHeight * 0.26f
        val photoY = origHeight + borderHeight * 0.54f

        var currentX = sideMargin.toFloat()
        logoBitmap?.let { logo ->
            val logoY = cameraY + borderHeight * fontCamera.sizeRatio * finalScale - logoHeight +
                    (template.brandLogos[brand]?.baselineOffset ?: 0)
            canvas.drawBitmap(logo, currentX, logoY, null)
            currentX += logoWidth + brandModelSpacing
        }

        textPaint.apply {
            this.textSize = borderHeight * fontCamera.sizeRatio * finalScale
            color = Color.parseColor(fontCamera.color)
        }
        canvas.drawText(model, currentX, cameraY, textPaint)

        if (lens.isNotEmpty()) {
            textPaint.apply {
                this.textSize = borderHeight * fontLens.sizeRatio * finalScale
                color = Color.parseColor(fontLens.color)
            }
            canvas.drawText(lens, sideMargin.toFloat(), lensY, textPaint)
        }

        val rightX = origWidth - sideMargin
        if (params.isNotEmpty()) {
            textPaint.apply {
                this.textSize = borderHeight * fontParams.sizeRatio * finalScale
                color = Color.parseColor(fontParams.color)
            }
            val paramsWidth = textPaint.measureText(params)
            canvas.drawText(params, rightX - paramsWidth, paramsY, textPaint)
        }

        if (photoBy.isNotEmpty()) {
            val prefix = "Photo by "
            val suffix = photoBy.replace("Photo by ZZeng @ ", "")
            val fullSuffix = "eng @ $suffix"

            textPaint.textSize = borderHeight * fontPhotoBy.sizeRatio * finalScale
            val prefixWidth = textPaint.measureText(prefix)
            val suffixWidth = textPaint.measureText(fullSuffix)
            textPaint.textSize = borderHeight * fontZZ.sizeRatio * finalScale
            val zWidth = textPaint.measureText("Z")
            val zzWidth = (zWidth * 1.45).toInt()
            val totalWidth = prefixWidth + zzWidth + suffixWidth
            val startX = rightX - totalWidth

            textPaint.apply {
                this.textSize = borderHeight * fontPhotoBy.sizeRatio * finalScale
                color = Color.parseColor(fontPhotoBy.color)
            }
            canvas.drawText(prefix, startX, photoY, textPaint)

            val zzX = startX + prefixWidth
            textPaint.apply {
                color = Color.parseColor("#FFD700")
                this.textSize = borderHeight * fontZZ.sizeRatio * finalScale
            }
            canvas.drawText("Z", zzX, photoY, textPaint)
            val offset = (zWidth * 0.45).toInt()
            canvas.drawText("Z", zzX + offset, photoY, textPaint)

            textPaint.apply {
                color = Color.parseColor(fontPhotoBy.color)
                this.textSize = borderHeight * fontPhotoBy.sizeRatio * finalScale
            }
            canvas.drawText(fullSuffix, zzX + zzWidth, photoY, textPaint)
        }
    }

    private fun renderPortrait(
        canvas: Canvas,
        template: Template,
        data: Map<String, String>,
        origWidth: Int,
        origHeight: Int,
        borderHeight: Int,
        scale: Float
    ) {
        val sideMargin = (origWidth * 0.05).toInt()
        val gap = (origWidth * 0.012).toInt()
        val brand = data["camera_brand"] ?: ""
        val model = data["camera_model"] ?: ""
        val lens = data["lens_model"] ?: ""
        val params = buildParamsString(data, true)
        val photoBy = data["date"] ?: ""

        val textPaint = Paint().apply { isAntiAlias = true }
        val fontCamera = template.fonts["camera"] ?: return
        val fontLens = template.fonts["lens"] ?: return
        val fontParams = template.fonts["params"] ?: return
        val fontPhotoBy = template.fonts["photo_by"] ?: return
        val fontZZ = template.fonts["zz"] ?: return

        var logoWidth = 0
        var logoHeight = 0
        var logoBitmap: Bitmap? = null

        template.brandLogos[brand]?.let { logoConfig ->
            val targetHeight = (borderHeight * logoConfig.heightRatio * scale).toInt()
            logoBitmap = loadLogoBitmap(logoConfig.file, targetHeight)
            logoBitmap?.let {
                logoWidth = it.width
                logoHeight = it.height
            }
        }

        textPaint.textSize = borderHeight * fontCamera.sizeRatio * scale
        val modelWidth = textPaint.measureText(model)

        val itemsWidth = mutableListOf<Int>()
        itemsWidth.add(logoWidth + gap + modelWidth.toInt())

        if (params.isNotEmpty()) {
            textPaint.textSize = borderHeight * fontParams.sizeRatio * scale
            itemsWidth.add(textPaint.measureText(params).toInt())
        }

        if (photoBy.isNotEmpty()) {
            val prefix = "Photo by "
            val suffix = photoBy.replace("Photo by ZZeng @ ", "")
            val fullSuffix = "eng @ $suffix"
            textPaint.textSize = borderHeight * fontPhotoBy.sizeRatio * scale
            val prefixWidth = textPaint.measureText(prefix)
            val suffixWidth = textPaint.measureText(fullSuffix)
            textPaint.textSize = borderHeight * fontZZ.sizeRatio * scale
            val zWidth = textPaint.measureText("Z")
            val zzWidth = (zWidth * 1.45).toInt()
            itemsWidth.add((prefixWidth + zzWidth + suffixWidth).toInt())
        }

        val totalWidth = itemsWidth.sum() + gap * (itemsWidth.size - 1)
        val availableWidth = origWidth - sideMargin * 2

        if (totalWidth <= availableWidth) {
            val y = origHeight + borderHeight * 0.32f
            var currentX = sideMargin.toFloat()

            logoBitmap?.let { logo ->
                val logoY = y + borderHeight * fontCamera.sizeRatio * scale - logoHeight +
                        (template.brandLogos[brand]?.baselineOffset ?: 0)
                canvas.drawBitmap(logo, currentX, logoY, null)
                currentX += logoWidth + gap
            }

            textPaint.apply {
                this.textSize = borderHeight * fontCamera.sizeRatio * scale
                color = Color.parseColor(fontCamera.color)
            }
            canvas.drawText(model, currentX, y, textPaint)
            currentX += modelWidth.toInt() + gap * 2

            if (params.isNotEmpty()) {
                textPaint.apply {
                    this.textSize = borderHeight * fontParams.sizeRatio * scale
                    color = Color.parseColor(fontParams.color)
                }
                canvas.drawText(params, currentX, y, textPaint)
                currentX += itemsWidth.getOrElse(1) { 0 } + gap * 2
            }

            if (photoBy.isNotEmpty()) {
                val prefix = "Photo by "
                val suffix = photoBy.replace("Photo by ZZeng @ ", "")
                val fullSuffix = "eng @ $suffix"
                textPaint.textSize = borderHeight * fontPhotoBy.sizeRatio * scale
                val prefixWidth = textPaint.measureText(prefix)
                val zWidth = textPaint.measureText("Z")
                val zzWidth = (zWidth * 1.45).toInt()

                textPaint.apply {
                    color = Color.parseColor(fontPhotoBy.color)
                    this.textSize = borderHeight * fontPhotoBy.sizeRatio * scale
                }
                canvas.drawText(prefix, currentX, y, textPaint)

                val zzX = currentX + prefixWidth
                textPaint.apply {
                    color = Color.parseColor("#FFD700")
                    this.textSize = borderHeight * fontZZ.sizeRatio * scale
                }
                canvas.drawText("Z", zzX, y, textPaint)
                val offset = (zWidth * 0.45).toInt()
                canvas.drawText("Z", zzX + offset, y, textPaint)

                textPaint.apply {
                    color = Color.parseColor(fontPhotoBy.color)
                    this.textSize = borderHeight * fontPhotoBy.sizeRatio * scale
                }
                canvas.drawText(fullSuffix, zzX + zzWidth, y, textPaint)
            }
        } else {
            val line1Y = origHeight + borderHeight * 0.22f
            val line2Y = origHeight + borderHeight * 0.58f
            var currentX = sideMargin.toFloat()

            logoBitmap?.let { logo ->
                val logoY = line1Y + borderHeight * fontCamera.sizeRatio * scale - logoHeight +
                        (template.brandLogos[brand]?.baselineOffset ?: 0)
                canvas.drawBitmap(logo, currentX, logoY, null)
                currentX += logoWidth + gap
            }

            textPaint.apply {
                this.textSize = borderHeight * fontCamera.sizeRatio * scale
                color = Color.parseColor(fontCamera.color)
            }
            canvas.drawText(model, currentX, line1Y, textPaint)

            if (params.isNotEmpty()) {
                textPaint.apply {
                    this.textSize = borderHeight * fontParams.sizeRatio * scale
                    color = Color.parseColor(fontParams.color)
                }
                val paramsWidth = textPaint.measureText(params)
                canvas.drawText(params, origWidth - sideMargin - paramsWidth, line1Y, textPaint)
            }

            if (lens.isNotEmpty()) {
                textPaint.apply {
                    this.textSize = borderHeight * fontLens.sizeRatio * scale
                    color = Color.parseColor(fontLens.color)
                }
                canvas.drawText(lens, sideMargin.toFloat(), line2Y, textPaint)
            }

            if (photoBy.isNotEmpty()) {
                val prefix = "Photo by "
                val suffix = photoBy.replace("Photo by ZZeng @ ", "")
                val fullSuffix = "eng @ $suffix"
                textPaint.textSize = borderHeight * fontPhotoBy.sizeRatio * scale
                val prefixWidth = textPaint.measureText(prefix)
                val suffixWidth = textPaint.measureText(fullSuffix)
                textPaint.textSize = borderHeight * fontZZ.sizeRatio * scale
                val zWidth = textPaint.measureText("Z")
                val zzWidth = (zWidth * 1.45).toInt()
                val totalW = prefixWidth + zzWidth + suffixWidth
                val startX = origWidth - sideMargin - totalW

                textPaint.apply {
                    color = Color.parseColor(fontPhotoBy.color)
                    this.textSize = borderHeight * fontPhotoBy.sizeRatio * scale
                }
                canvas.drawText(prefix, startX, line2Y, textPaint)

                val zzX = startX + prefixWidth
                textPaint.apply {
                    color = Color.parseColor("#FFD700")
                    this.textSize = borderHeight * fontZZ.sizeRatio * scale
                }
                canvas.drawText("Z", zzX, line2Y, textPaint)
                val offset = (zWidth * 0.45).toInt()
                canvas.drawText("Z", zzX + offset, line2Y, textPaint)

                textPaint.apply {
                    color = Color.parseColor(fontPhotoBy.color)
                    this.textSize = borderHeight * fontPhotoBy.sizeRatio * scale
                }
                canvas.drawText(fullSuffix, zzX + zzWidth, line2Y, textPaint)
            }
        }
    }

    private fun buildParamsString(data: Map<String, String>, compact: Boolean): String {
        val parts = mutableListOf<String>()
        data["aperture"]?.let { if (it.isNotEmpty()) parts.add(it) }
        data["shutter"]?.let { if (it.isNotEmpty()) parts.add(it) }
        data["iso"]?.let {
            if (it.isNotEmpty()) {
                parts.add(if (compact) "ISO$it" else "ISO $it")
            }
        }
        data["focal"]?.let { if (it.isNotEmpty()) parts.add("${it}mm") }
        return parts.joinToString(" | ")
    }

    private fun loadLogoBitmap(fileName: String, targetHeight: Int): Bitmap? {
        val cacheKey = "${fileName}_${targetHeight}"
        logoCache[cacheKey]?.let { return it }

        return try {
            context.assets.open("logos/$fileName").use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)

                val scale = targetHeight.toFloat() / options.outHeight.toFloat()
                val targetWidth = (options.outWidth * scale).toInt()

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
                }

                context.assets.open("logos/$fileName").use { decodeStream ->
                    val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
                    bitmap?.let {
                        val scaled = Bitmap.createScaledBitmap(it, targetWidth, targetHeight, true)
                        logoCache[cacheKey] = scaled
                        scaled
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun saveImage(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }
}
