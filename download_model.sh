#!/bin/bash
# Helper script to download TFLite model for Privacy Ghost

MODEL_DIR="app/src/main/assets/models"
mkdir -p "$MODEL_DIR"

echo "Downloading EfficientDet-Lite0 TFLite model..."

# Try downloading from TF Hub (requires manual download due to redirect)
echo ""
echo "Please download the model manually:"
echo ""
echo "OPTION 1 - EfficientDet-Lite0 (Recommended, better accuracy):"
echo "  URL: https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_efficientdet_lite0_detection_metadata_1.tflite"
echo "  Save as: $MODEL_DIR/detect.tflite"
echo ""
echo "OPTION 2 - MobileNetSSD v1 (Faster, less accurate):"
echo "  URL: https://storage.googleapis.com/download.tensorflow.org/models/tflite/coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip"
echo "  Extract detect.tflite and save as: $MODEL_DIR/detect.tflite"
echo ""
echo "OPTION 3 - Auto download with wget:"

if command -v wget &> /dev/null; then
    wget -O "$MODEL_DIR/detect.tflite" \
        "https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_efficientdet_lite0_detection_metadata_1.tflite" \
        && echo "✅ Model downloaded successfully!" \
        || echo "❌ Auto download failed. Please download manually."
else
    echo "wget not found. Please download manually using the URLs above."
fi
