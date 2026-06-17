# Add project specific ProGuard rules here.

# 保留行号信息，方便调试
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 临时保留日志（调试用，发布时可移除）
-assumenosideeffects class android.util.Log {
    # 注释掉以下行以保留日志
    # public static int v(...);
    # public static int d(...);
    # public static int i(...);
}

# ==================== SpeechRecognizer ====================
-keep class android.speech.** { *; }
-keep interface android.speech.** { *; }

# ==================== GSYVideoPlayer ====================
-keep class com.shuyu.gsyvideoplayer.** { *; }
-keep class tv.danmaku.ijk.** { *; }
-dontwarn tv.danmaku.ijk.**

# GSY 阿里云播放器模块
-keep class com.shuyu.aliplay.** { *; }
-keep interface com.shuyu.aliplay.** { *; }

# ==================== ExoPlayer ====================
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# ==================== 阿里云播放器 ====================
-keep class com.alivc.** { *; }
-keep class com.aliyun.** { *; }
-keep class com.cicada.** { *; }
-keep class com.alibaba.** { *; }
-dontwarn com.alivc.**
-dontwarn com.aliyun.**
-dontwarn com.cicada.**
-dontwarn com.alibaba.**

# 阿里云播放器 JNI 相关
-keepclasseswithmembers class * {
    native <methods>;
}
-keep class com.alivc.player.** { *; }
-keep class com.aliyun.player.** { *; }
-keep class com.aliyun.player.nativeclass.** { *; }
-keep class com.aliyun.player.bean.** { *; }
-keep interface com.aliyun.player.** { *; }

# 阿里云 FFmpeg
-keep class com.alivc.conan.** { *; }
-dontwarn com.alivc.conan.**

# ==================== Tesseract OCR ====================
-keep class com.googlecode.tesseract.android.** { *; }
-keep class org.opencv.** { *; }
-dontwarn com.googlecode.tesseract.android.**

# ==================== ML Kit ====================
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_translate.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.mlkit.**

# ML Kit 翻译 - 反射调用需要保留
-keep class com.google.mlkit.nl.translate.** { *; }
-keep class com.google.mlkit.common.model.** { *; }
-keepclassmembers class com.google.mlkit.nl.translate.TranslateLanguage {
    public static final java.lang.String *;
}
-keepclassmembers class com.google.mlkit.nl.translate.TranslatorOptions$Builder {
    public <methods>;
}
-keepclassmembers class com.google.mlkit.nl.translate.Translation {
    public static <methods>;
}
-keepclassmembers class com.google.mlkit.common.model.DownloadConditions$Builder {
    public <methods>;
}

# ==================== Vosk 语音识别 ====================
-keep class org.vosk.** { *; }
-keep class com.alphacephei.vosk.** { *; }
-dontwarn org.vosk.**

# JNA (Vosk 依赖)
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
-dontwarn java.awt.**

# ==================== OrangePlayer Library ====================
-keep class com.orange.playerlibrary.** { *; }
-keep interface com.orange.playerlibrary.** { *; }

# ==================== 弹幕库 ====================
-keep class master.flame.danmaku.** { *; }
-dontwarn master.flame.danmaku.**

# ==================== Glide ====================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# ==================== OkHttp ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ==================== 通用规则 ====================
# 保留 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# 保留 Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留 R 文件
-keep class **.R$* { *; }

# 保留注解
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
