package com.example.liverecognition;

import android.util.Log;
import android.graphics.RectF;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.io.File;

import com.luxand.FSDK;

/**
 * Handles interaction with FSDK library.
 */
public class FacesProcessor {

    /** Maximal number of faces detected on a single frame. */
    private static final int MAX_FACES = 4;

    /** Size of the buffer for name retrieval. */
    private static final long MAX_NAME_SIZE = 256L;

    /** Size of the buffer for facial attribute retrieval. */
    private static final long MAX_ATTRIBUTE_VALUE_LENGTH = 1024L;

    /** Use an improved version of face detection and recognition. */
    private static final boolean USE_NEW_DETECTION = true;

    /** Use iBeta liveness addon for liveness detection. If false uses a simpler, but less accurate model. */
    private static final boolean USE_IBETA_LIVENESS_ADDON = true;

    private static boolean enableLiveness = false;

    /**
     * Stores information about the detected face: bounding box, name, liveness probability.
     * Additionally for iBeta liveness addon provides image quality and liveness error information.
     */
    public static class Face {

        private static final int livenessErrorStringLength = "LivenessError=".length();

        private long id;
        private String name = "";

        private String livenessError = null;

        private final RectF rect = new RectF();
        private final float[] liveness = { -1.f };
        private final float[] imageQuality = { -1.f };
        private final String[] attributeValue = { "" };
        private final FSDK.TFace face = new FSDK.TFace();
        private final FSDK.TFacePosition facePosition = new FSDK.TFacePosition();

        private void setID(final long id) {
            this.id = id;
            this.name = getNameForID(id);

            /* New detection uses different classes and API. */
            if (USE_NEW_DETECTION) {
                FSDK.GetTrackerFace(tracker, 0, id, face);
                rect.set(face.bbox.p0.x, face.bbox.p0.y, face.bbox.p1.x, face.bbox.p1.y);
            } else {
                FSDK.GetTrackerFacePosition(tracker, 0, id, facePosition);

                final var faceWidth = facePosition.w / 2;
                final var faceHeight = (int)(faceWidth * 1.15);
                rect.set(facePosition.xc - faceWidth, facePosition.yc - faceHeight, facePosition.xc + faceWidth, facePosition.yc + faceHeight);
            }

            if (enableLiveness) {
                if (FSDK.GetTrackerFacialAttribute(tracker, 0, id, "Liveness", attributeValue, MAX_ATTRIBUTE_VALUE_LENGTH) != FSDK.FSDKE_OK ||
                    FSDK.GetValueConfidence(attributeValue[0], "Liveness", liveness) != FSDK.FSDKE_OK)
                    liveness[0] = -1.f;

                /* For iBeta liveness addon Tracker additionally outputs image quality and potentially an error description. */
                if (USE_IBETA_LIVENESS_ADDON) {
                    if (FSDK.GetTrackerFacialAttribute(tracker, 0, id, "ImageQuality", attributeValue, MAX_ATTRIBUTE_VALUE_LENGTH) != FSDK.FSDKE_OK ||
                        FSDK.GetValueConfidence(attributeValue[0], "ImageQuality", imageQuality) != FSDK.FSDKE_OK)
                        imageQuality[0] = -1.f;

                    /* If an error occurred during liveness detection, LivenessError attribute stores the error description */
                    if (FSDK.GetTrackerFacialAttribute(tracker, 0, id, "LivenessError", attributeValue, MAX_ATTRIBUTE_VALUE_LENGTH) == FSDK.FSDKE_OK) {
                        livenessError = attributeValue[0].substring(livenessErrorStringLength, attributeValue[0].indexOf(";")).strip();
                    } else {
                        livenessError = null;
                    }
                }
            }
        }

        @NonNull
        public String getName() {
            return name;
        }

        public void setName(final String value) {
            FSDK.LockID(tracker, id);
            FSDK.SetName(tracker, id, value);
            FSDK.UnlockID(tracker, id);
        }

        @NonNull
        public RectF getRect() {
            return rect;
        }

        public float getLiveness() {
            return liveness[0];
        }

        public float getImageQuality() {
            return imageQuality[0];
        }

        public String getLivenessError() {
            return livenessError;
        }

    }

    /**
     * Wraps an array of Face objects.
     * Detection results are allocated once on startup to increase performance.
     */
    public static class DetectionResult {

        private int size = 0;
        private final Face[] buffer = new Face[MAX_FACES];

        public DetectionResult() {
            for (var i = 0; i < MAX_FACES; ++i)
                buffer[i] = new Face();
        }

        public int getSize() {
            return size;
        }

        @NonNull
        public Face getFace(final int index) {
            return buffer[index];
        }

        private void loadFaces() {
            for (int i = 0; i < faceCount[0]; ++i)
                buffer[i].setID(FacesProcessor.ids[i]);

            size = (int)faceCount[0];
        }
    }

    /**
     * Wraps a matching result obtained from tracker.
     * Stores the matched id name and similarity or the error if matching finished with one.
     */
    public static class MatchingResult {

        private final int error;
        private final String name;
        private final FSDK.IDSimilarity similarity;

        public MatchingResult(final int error) {
            this.name = "";
            this.error = error;
            this.similarity = null;
        }

        public MatchingResult(final FSDK.IDSimilarity similarity) {
            this.name = getNameForID(similarity.ID);
            this.error = FSDK.FSDKE_OK;
            this.similarity = similarity;
        }

        public int getError() {
            return error;
        }

        public boolean isError() {
            return error != FSDK.FSDKE_OK;
        }

        public boolean hasMatch() {
            return similarity != null;
        }

        @NonNull
        public String getName() {
            return name;
        }

        public float getSimilarity() {
            return similarity == null ? 0.f : similarity.similarity;
        }

        public long getID() {
            return similarity == null ? -1 : similarity.ID;
        }
    }

    private static final long[] faceCount = { 0 };
    private static final long[] ids = new long[MAX_FACES];
    private static final FSDK.HTracker tracker = new FSDK.HTracker();
    private static final YUVToRGBConverter yuvToRGBConverter = new YUVToRGBConverter();
    private static final FSDK.FSDK_IMAGEMODE rgbImageMode = new FSDK.FSDK_IMAGEMODE() {{ mode = FSDK.FSDK_IMAGEMODE.FSDK_IMAGE_COLOR_24BIT; }};

    /* Create two Detection results and alternate between them to save reallocations */
    private static int bufferIndex = 0;
    private static final DetectionResult[] detectionResults = { new DetectionResult(), new DetectionResult() };

    private static String assetsPath = "";
    private static FSDK.HImage image = new FSDK.HImage();
    private static FSDK.HImage rotatedImage = new FSDK.HImage();

    /* FaceSDK library is activated here */
    public static boolean initialize(final Application application, final String assetsPath) {
        if (FSDK.ActivateLibrary("Insert the license key here") != FSDK.FSDKE_OK)
            return false;

        FSDK.Initialize();

        /* Copy iBeta liveness addon assets to the cache directory. */
        FSDK.PrepareData(application);

        FacesProcessor.assetsPath = assetsPath;
        return true;
    }

    private static String getNameForID(final long id) {
        final String[] value = { "" };
        FSDK.LockID(tracker, id);
        FSDK.GetName(tracker, id, value, MAX_NAME_SIZE);
        FSDK.UnlockID(tracker, id);
        return value[0];
    }

    private static void setTrackerParameters() {
        var parameters = USE_NEW_DETECTION
            /* FaceDetection2PatchSize sets the image size used for face detection. Lower values increase performance, but decrease accuracy
            * Threshold and Threshold2 control face matching, new recognition uses lower threshold (values as low as 0.7 work well), compared to the default one. */
            ? "FaceDetection2PatchSize=256;Threshold=0.8;Threshold2=0.9"
            : "HandleArbitraryRotations=false;DetermineFaceRotationAngle=false;InternalResizeWidth=256;FaceDetectionThreshold=5";

        if (enableLiveness)
            parameters += ";DetectLiveness=true";

        if (USE_IBETA_LIVENESS_ADDON)
            /* Disabling liveness smoothing when using iBeta plugin.
            * This way tracker reports liveness score as soon as it's available. */
            parameters += ";SmoothAttributeLiveness=false;LivenessFramesCount=1";

        FSDK.SetTrackerMultipleParameters(tracker, parameters, new int[1]);

        if (USE_IBETA_LIVENESS_ADDON && FSDK.SetParameter("LivenessModel", "external:dataDir=" + assetsPath) != FSDK.FSDKE_OK)
            Log.e("luxand_fsdk", "Error while initializing external liveness model");
    }

    public static boolean isLivenessEnabled() {
        return enableLiveness;
    }

    public static boolean useIBetaLivenessAddon() {
        return USE_IBETA_LIVENESS_ADDON;
    }

    public static boolean toggleLiveness() {
        FSDK.SetTrackerParameter(tracker, "DetectLiveness", enableLiveness ? "false" : "true");
        return enableLiveness = !enableLiveness;
    }

    public static boolean load(final File file) {
        if (FSDK.LoadTrackerMemoryFromFile(tracker, file.getAbsolutePath()) != FSDK.FSDKE_OK) {
            FSDK.CreateTracker(tracker);
            clear();

            return true;
        }

        setTrackerParameters();

        final String[] value = { "" };
        FSDK.GetTrackerParameter(tracker, "DetectionVersion", value, 16);

        /* Return False if detection version of loaded tracker memory doesn't match.
        * Using tracker with a wrong detection version is not allowed and leads to incorrect results. */
        return USE_NEW_DETECTION
            ? Integer.parseInt(value[0]) == 2
            : Integer.parseInt(value[0]) == 1;
    }

    public static void clear() {
        synchronized (tracker) {
            FSDK.ClearTracker(tracker);
            if (USE_NEW_DETECTION)
                FSDK.SetTrackerParameter(tracker, "DetectionVersion", "2");
            setTrackerParameters();
        }
    }

    public static void save(final File file) {
        FSDK.SaveTrackerMemoryToFile(tracker, file.getAbsolutePath());
    }

    @NonNull
    public static DetectionResult accept(final ImageProxy imageProxy) {
        FSDK.LoadImageFromBuffer(image, yuvToRGBConverter.convert(imageProxy), imageProxy.getWidth(), imageProxy.getHeight(), imageProxy.getWidth() * 3, rgbImageMode);

        final var rotation = imageProxy.getImageInfo().getRotationDegrees();
        if (rotation != 0) {
            FSDK.CreateEmptyImage(rotatedImage);
            FSDK.RotateImage90(image, rotation / 90, rotatedImage);
            FSDK.FreeImage(image);

            final var newImage = rotatedImage;
            rotatedImage = image;
            image = newImage;
        }

        final var result = detectionResults[(bufferIndex += 1) % detectionResults.length];

        synchronized (tracker) {
            FSDK.FeedFrame(tracker, 0, image, faceCount, ids);
            result.loadFaces();
        }

        FSDK.FreeImage(image);

        return result;
    }

    @NonNull
    public static MatchingResult matchFace(final String imagePath) {
        final var image = new FSDK.HImage();
        var result = FSDK.LoadImageFromFile(image, imagePath);

        if (result != FSDK.FSDKE_OK)
            return new MatchingResult(result);

        final var faceTemplate = new FSDK.FSDK_FaceTemplate();

        /* Functions utilizing new detection usually have 2 attached to them.
        * Using incorrect functions will lead to undefined behaviour. */
        result = USE_NEW_DETECTION
            ? FSDK.GetFaceTemplate2(image, faceTemplate)
            : FSDK.GetFaceTemplate(image, faceTemplate);

        if (result != FSDK.FSDKE_OK)
            return new MatchingResult(result);

        final var count = new long[1];
        final var buffer = new FSDK.IDSimilarity[1];

        /* New face recognition uses a lower matching threshold. */
        result = FSDK.TrackerMatchFaces(tracker, faceTemplate, USE_NEW_DETECTION ? 0.7f : 0.992f, buffer, count);

        if (count[0] == 0)
            return new MatchingResult(result);

        return new MatchingResult(buffer[0]);
    }
}
