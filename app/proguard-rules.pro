# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


-keep class org.eclipse.**
-keep class io.ktor.**
-keep class org.torproject.**
-keep public class org.eclipse.**
-keep public class org.torproject.**
-keep public class io.ktor.**

-keep public class org.torproject.** {
  *;
}
-keep public class org.eclipse.** {
  *;
}
-keep public class io.ktor.** {
  *;
}
-keep class org.torproject.** {
  *;
}
-keep class org.eclipse.** {
  *;
}
-keep class io.ktor.** {
  *;
}

-keepclassmembers public class org.torproject.** {
  *;
}
-keepclassmembers public class org.eclipse.** {
  *;
}
-keepclassmembers public class io.ktor.** {
  *;
}
-keepclassmembers class org.eclipse.** {
  *;
}
-keepclassmembers class org.torproject.** {
  *;
}
-keepclassmembers class io.ktor.** {
  *;
}