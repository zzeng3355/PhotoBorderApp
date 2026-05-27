package com.zzeng.photoborder.data.exif

import android.content.Context
import android.net.Uri
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.zzeng.photoborder.data.model.ExifData
import java.io.InputStream

class ExifReader(private val context: Context) {

    fun readExif(uri: Uri): ExifData {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val metadata = ImageMetadataReader.readMetadata(inputStream)

            val exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
            val exifSubIFD = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)

            ExifData(
                make = cleanString(exifIFD0?.getDescription(ExifIFD0Directory.TAG_MAKE)),
                model = cleanString(exifIFD0?.getDescription(ExifIFD0Directory.TAG_MODEL)),
                lensModel = cleanString(exifSubIFD?.getDescription(ExifSubIFDDirectory.TAG_LENS_MODEL)),
                aperture = cleanString(exifSubIFD?.getDescription(ExifSubIFDDirectory.TAG_FNUMBER)),
                shutterSpeed = cleanString(exifSubIFD?.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)),
                iso = cleanString(exifSubIFD?.getDescription(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)),
                focalLength = cleanString(exifSubIFD?.getDescription(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)),
                dateTimeOriginal = cleanString(exifSubIFD?.getDescription(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL))
            )
        } catch (e: Exception) {
            ExifData()
        } finally {
            inputStream?.close()
        }
    }

    private fun cleanString(value: String?): String {
        return value?.trim()?.replace("\"", "") ?: ""
    }
}
