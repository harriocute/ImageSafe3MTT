# Add project specific ProGuard rules here.

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.task.** { *; }

# Apache Commons Imaging
-keep class org.apache.commons.imaging.** { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep model classes
-keep class com.safetry.privacy.model.** { *; }

# Keep data store
-keep class androidx.datastore.** { *; }
