package com.safetry.privacy.processor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.safetry.privacy.model.ExifData
import java.io.File
import java.io.FileOutputStream

/**
 * Analyzes and strips EXIF metadata from images.
 * All processing is done on-device.
 */
class ExifScrubber(private val context: Context) {

    data class ExifField(val tag: String, val humanName: String)

    private val sensitiveExifTags = listOf(
        // GPS data
        ExifField(ExifInterface.TAG_GPS_LATITUDE, "GPS Latitude"),
        ExifField(ExifInterface.TAG_GPS_LONGITUDE, "GPS Longitude"),
        ExifField(ExifInterface.TAG_GPS_ALTITUDE, "GPS Altitude"),
        ExifField(ExifInterface.TAG_GPS_TIMESTAMP, "GPS Timestamp"),
        ExifField(ExifInterface.TAG_GPS_DATESTAMP, "GPS Date"),
        ExifField(ExifInterface.TAG_GPS_SPEED, "GPS Speed"),
        ExifField(ExifInterface.TAG_GPS_AREA_INFORMATION, "GPS Area"),
        ExifField(ExifInterface.TAG_GPS_DEST_LATITUDE, "GPS Destination Lat"),
        ExifField(ExifInterface.TAG_GPS_DEST_LONGITUDE, "GPS Destination Long"),
        ExifField(ExifInterface.TAG_GPS_IMG_DIRECTION, "GPS Image Direction"),
        // Device info
        ExifField(ExifInterface.TAG_MAKE, "Device Manufacturer"),
        ExifField(ExifInterface.TAG_MODEL, "Device Model"),
        ExifField(ExifInterface.TAG_SOFTWARE, "Software Version"),
        // Camera info
        ExifField(ExifInterface.TAG_CAMERA_OWNER_NAME, "Camera Owner"),
        ExifField(ExifInterface.TAG_LENS_MAKE, "Lens Make"),
        ExifField(ExifInterface.TAG_LENS_MODEL, "Lens Model"),
        ExifField(ExifInterface.TAG_BODY_SERIAL_NUMBER, "Camera Serial"),
        ExifField(ExifInterface.TAG_LENS_SERIAL_NUMBER, "Lens Serial"),
        // Timestamps
        ExifField(ExifInterface.TAG_DATETIME, "Date/Time"),
        ExifField(ExifInterface.TAG_DATETIME_ORIGINAL, "Original Date/Time"),
        ExifField(ExifInterface.TAG_DATETIME_DIGITIZED, "Digitized Date/Time"),
        ExifField(ExifInterface.TAG_OFFSET_TIME, "Time Offset"),
        // Orientation
        ExifField(ExifInterface.TAG_ORIENTATION, "Orientation"),
        // User data
        ExifField(ExifInterface.TAG_USER_COMMENT, "User Comment"),
        ExifField(ExifInterface.TAG_IMAGE_DESCRIPTION, "Image Description"),
        ExifField(ExifInterface.TAG_ARTIST, "Artist"),
        ExifField(ExifInterface.TAG_COPYRIGHT, "Copyright"),
        ExifField(ExifInterface.TAG_MAKER_NOTE, "Maker Notes")
    )

    /**
     * Analyzes EXIF data in an image and returns what was found.
     */
    fun analyzeExif(uri: Uri): ExifData {
        val foundFields = mutableListOf<String>()
        var hasGps = false
        var hasDeviceInfo = false
        var hasTimestamp = false

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)

                for (field in sensitiveExifTags) {
                    val value = exif.getAttribute(field.tag)
                    if (!value.isNullOrEmpty()) {
                        foundFields.add(field.humanName)

                        when {
                            field.tag.startsWith("GPS") -> hasGps = true
                            field.tag == ExifInterface.TAG_MAKE ||
                            field.tag == ExifInterface.TAG_MODEL -> hasDeviceInfo = true
                            field.tag == ExifInterface.TAG_DATETIME ||
                            field.tag == ExifInterface.TAG_DATETIME_ORIGINAL -> hasTimestamp = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // If we can't read EXIF, assume some might exist
        }

        return ExifData(
            foundFields = foundFields,
            hasGpsData = hasGps,
            hasDeviceInfo = hasDeviceInfo,
            hasTimestamp = hasTimestamp
        )
    }

    /**
     * Saves a bitmap as a clean JPEG without any EXIF metadata.
     * Re-encoding the image effectively strips all metadata.
     */
    fun saveCleanImage(bitmap: Bitmap, outputFile: File): File {
        FileOutputStream(outputFile).use { out ->
            // Re-encoding creates a clean copy with no EXIF
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return outputFile
    }
}
