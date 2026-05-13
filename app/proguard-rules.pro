-optimizationpasses 5

-dontwarn com.qmuiteam.qmui.widget.popup.QMUINormalPopup$AnimStyle

# pixiv-login 库：Gson 反序列化 OAuth 响应，sealed class 被混淆后无法实例化
-keep class com.github.soxia.** { *; }
-keepclassmembers class com.github.soxia.** { *; }

# 登录回调相关类：Gson 反序列化 OAuth token
-keep class ceui.pixiv.login.** { *; }
-keepclassmembers class ceui.pixiv.login.** { *; }

# Gson 反射所需元数据
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Gson 库自身：TypeAdapterFactory 通过 ServiceLoader 注册
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 实体模型：Retrofit 响应、SharedPreferences、Room JSON 列、备份恢复
-keep class ceui.lisa.models.** { *; }
-keepclassmembers class ceui.lisa.models.** { *; }

# 网络响应包装类（注意是 model 单数，非 models）
-keep class ceui.lisa.model.** { *; }
-keepclassmembers class ceui.lisa.model.** { *; }

# Kotlin data 模型：新功能及 ObjectPool 缓存
-keep class ceui.loxia.** { *; }
-keepclassmembers class ceui.loxia.** { *; }

# 数据库实体及 Settings：Gson 备份/恢复
-keep class ceui.lisa.database.** { *; }
-keepclassmembers class ceui.lisa.database.** { *; }
-keep class ceui.lisa.utils.Settings { *; }
-keepclassmembers class ceui.lisa.utils.Settings { *; }

# KSP 生成的 ViewHolderFactory：通过反射加载，且 key 使用编译期类名 hashCode
# 混淆后类名 hashCode 变化导致 viewType 匹配失败 → RuntimeException
-keep class ceui.pixiv.ui.viewholdermap.ViewHolderFactory { *; }
-keepnames class * extends ceui.pixiv.ui.common.ListItemHolder

# @SerializedName 字段兜底：防止遗漏的模型类字段被重命名
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Serializable：Activity/Fragment 间通过 Bundle 传递
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
