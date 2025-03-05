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

-keep public class com.luxand.FSDK$FSDK_VIDEOCOMPRESSIONTYPE {
    public int type;
}

-keep public class com.luxand.FSDK$FSDK_IMAGEMODE {
    public int mode;
}

-keep public class com.luxand.FSDK$HImage {
    protected int himage;
}

-keep public class com.luxand.FSDK$HCamera {
    protected int hcamera;
}

-keep public class com.luxand.FSDK$HTracker {
    protected int htracker;
}

-keep public class com.luxand.FSDK$TFacePosition {
    public int xc;
    public int yc;
    public int w;
    public int padding;
    public double angle;
}

-keep public class com.luxand.FSDK$TFaces {
    public com.luxand.FSDK$TFacePosition[] faces;
    public int maxFaces;
}

-keep public class com.luxand.FSDK$TPoint {
    public int x;
    public int y;
}

-keep public class com.luxand.FSDK$BBox {
    public com.luxand.FSDK$TPoint p0;
    public com.luxand.FSDK$TPoint p1;
}

-keep public class com.luxand.FSDK$TFace {
    public com.luxand.FSDK$BBox bbox;
    public com.luxand.FSDK$TPoint[] features;
}

-keep public class com.luxand.FSDK$TFaces2 {
    public com.luxand.FSDK$TFace[] faces;
    public int maxFaces;
}

-keep public class com.luxand.FSDK$FSDK_Features {
    public com.luxand.FSDK$TPoint[] features;
}

-keep public class com.luxand.FSDK$FSDK_FaceTemplate {
    public byte[] template;
}

-keep public class com.luxand.FSDK$IDSimilarity {
    public long ID;
    public float similarity;
}
