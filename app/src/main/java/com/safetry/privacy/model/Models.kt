package com.safetry.privacy.model

import android.graphics.RectF

enum class DetectionCategory {
    FACE,
    LICENSE_PLATE,
    STREET_SIGN,
    ID_BADGE,
    TEXT_DOCUMENT
}

data class DetectionResult(
    val label: String,
    val category: DetectionCategory,
    val confidence: Float,
    val boundingBox: RectF
)

data class ExifData(
    val foundFields: List<String>,
    val hasGpsData: Boolean,
    val hasDeviceInfo: Boolean,
    val hasTimestamp: Boolean
)

data class PrivacyReport(
    val risks: List<String>,
    val detectionSummary: List<String>,
    val scoreBefore: Int,
    val scoreAfter: Int,
    val exifData: ExifData,
    val detections: List<DetectionResult>
)
