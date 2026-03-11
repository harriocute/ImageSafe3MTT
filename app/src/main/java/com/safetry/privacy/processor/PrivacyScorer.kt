package com.safetry.privacy.processor

import com.safetry.privacy.model.DetectionResult
import com.safetry.privacy.model.DetectionCategory
import com.safetry.privacy.model.ExifData
import com.safetry.privacy.model.PrivacyReport

/**
 * Generates privacy scores and reports based on detected risks.
 */
class PrivacyScorer {

    /**
     * Generate a full privacy report from EXIF analysis and AI detections.
     */
    fun generateReport(exifData: ExifData, detections: List<DetectionResult>): PrivacyReport {
        val risks = mutableListOf<String>()
        val detectionSummary = mutableListOf<String>()
        var riskScore = 0 // Higher = more risk = lower privacy score

        // EXIF risks
        if (exifData.hasGpsData) {
            risks.add("GPS location data removed ✓")
            riskScore += 3
        }
        if (exifData.hasDeviceInfo) {
            risks.add("Device model/make metadata removed ✓")
            riskScore += 1
        }
        if (exifData.hasTimestamp) {
            risks.add("Photo timestamp removed ✓")
            riskScore += 1
        }
        if (exifData.foundFields.isNotEmpty() && !exifData.hasGpsData && !exifData.hasDeviceInfo && !exifData.hasTimestamp) {
            risks.add("${exifData.foundFields.size} metadata fields removed ✓")
            riskScore += 1
        }

        // Detection risks
        val faceCount = detections.count { it.category == DetectionCategory.FACE }
        val plateCount = detections.count { it.category == DetectionCategory.LICENSE_PLATE }
        val signCount = detections.count { it.category == DetectionCategory.STREET_SIGN }
        val badgeCount = detections.count { it.category == DetectionCategory.ID_BADGE }
        val docCount = detections.count { it.category == DetectionCategory.TEXT_DOCUMENT }

        if (faceCount > 0) {
            risks.add("$faceCount face(s) detected and blurred ✓")
            detectionSummary.add("$faceCount face(s) blurred")
            riskScore += faceCount * 2
        }
        if (plateCount > 0) {
            risks.add("$plateCount license plate(s) detected and blurred ✓")
            detectionSummary.add("$plateCount license plate(s) blurred")
            riskScore += plateCount * 2
        }
        if (signCount > 0) {
            risks.add("$signCount street sign(s) detected and blurred ✓")
            detectionSummary.add("$signCount street sign(s) blurred")
            riskScore += signCount
        }
        if (badgeCount > 0) {
            risks.add("$badgeCount ID badge(s) detected and blurred ✓")
            detectionSummary.add("$badgeCount ID badge(s) blurred")
            riskScore += badgeCount * 2
        }
        if (docCount > 0) {
            risks.add("$docCount text document(s) detected and blurred ✓")
            detectionSummary.add("$docCount text document(s) blurred")
            riskScore += docCount * 2
        }

        // Calculate scores (1-10 scale, 10 = most private)
        // Before score: based on what risks existed
        val scoreBefore = calculateScoreBefore(riskScore)

        // After score: very high since we've removed everything
        val scoreAfter = calculateScoreAfter(detections, exifData)

        return PrivacyReport(
            risks = risks,
            detectionSummary = detectionSummary,
            scoreBefore = scoreBefore,
            scoreAfter = scoreAfter,
            exifData = exifData,
            detections = detections
        )
    }

    private fun calculateScoreBefore(riskScore: Int): Int {
        return when {
            riskScore == 0 -> 9
            riskScore <= 2 -> 7
            riskScore <= 4 -> 5
            riskScore <= 6 -> 3
            riskScore <= 8 -> 2
            else -> 1
        }
    }

    private fun calculateScoreAfter(detections: List<DetectionResult>, exifData: ExifData): Int {
        // After processing, score should be high
        // Slight deduction if many things needed redaction (context inference risk)
        val totalRedactions = detections.size +
            (if (exifData.hasGpsData) 1 else 0) +
            (if (exifData.foundFields.isNotEmpty()) 1 else 0)

        return when {
            totalRedactions == 0 -> 10
            totalRedactions <= 3 -> 9
            totalRedactions <= 6 -> 8
            else -> 7
        }
    }
}
