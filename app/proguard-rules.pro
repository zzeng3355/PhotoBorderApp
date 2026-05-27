# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Exif data classes
-keep class com.zzeng.photoborder.data.model.** { *; }

# Keep metadata-extractor
-keep class com.drew.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
