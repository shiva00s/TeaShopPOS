# Firebase Entities
-keep class com.teashop.pos.data.entity.** { *; }
-keepnames class com.teashop.pos.data.entity.** { *; }

# Keep Hilt and Dagger
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class * extends androidx.lifecycle.ViewModel

# Razorpay
-keep class com.razorpay.** {*;}
-dontwarn com.razorpay.**
