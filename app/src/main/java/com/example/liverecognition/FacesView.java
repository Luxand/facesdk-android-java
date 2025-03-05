package com.example.liverecognition;

import android.view.View;
import android.util.AttributeSet;

import android.content.Context;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Locale;

/**
 * Displays face boxes on screen.
 */
public class FacesView extends View {

    private final StringBuilder faceText = new StringBuilder();

    /** Transformation matrix from image space to screen space. */
    private Matrix facesTransform = null;
    private FacesProcessor.DetectionResult detectionResult = new FacesProcessor.DetectionResult();

    private static final Paint greenPaint = new Paint() {{
        setColor(Color.GREEN);
        setStyle(Style.STROKE);
        setStrokeWidth(5f);
    }};

    private static final Paint redPaint = new Paint() {{
        setColor(Color.RED);
        setStyle(Style.STROKE);
        setStrokeWidth(5f);
    }};

    private static final Paint greenTextPaint = new Paint() {{
        setColor(Color.GREEN);
        setStyle(Style.FILL_AND_STROKE);
        setTextAlign(Align.CENTER);
        setTextSize(45f);
    }};

    private static final Paint redTextPaint = new Paint() {{
        setColor(Color.RED);
        setStyle(Style.FILL_AND_STROKE);
        setTextAlign(Align.CENTER);
        setTextSize(45f);
    }};

    public FacesView(Context context, final AttributeSet attributeSet) {
        super(context, attributeSet);

        setWillNotDraw(false);
    }

    public Matrix getFacesTransform() {
        return facesTransform;
    }

    public void setFacesTransform(final Matrix facesTransform) {
        this.facesTransform = facesTransform;
    }

    public void setDetectionResult(final FacesProcessor.DetectionResult detectionResult) {
        /* If the transform matrix is set map faces from image space to screen space. */
        if (facesTransform != null) {
            for (var i = 0; i < detectionResult.getSize(); ++i) {
                final var rect = detectionResult.getFace(i).getRect();
                facesTransform.mapRect(rect);
            }
        }

        synchronized (this) {
            this.detectionResult = detectionResult;
        }

        postInvalidate();
    }

    private static String formatString(final String format, Object... args) {
        return String.format(Locale.getDefault(), format, args);
    }

    private String getStringResource(@StringRes final int id) {
        return getResources().getString(id);
    }

    private static void appendString(final StringBuilder stringBuilder, final String string) {
        if (stringBuilder.length() > 0)
            stringBuilder.append("; ");

        stringBuilder.append(string);
    }

    private boolean checkLiveness(final FacesProcessor.Face face, final StringBuilder faceText) {
        /* Negative liveness values indicated that liveness value couldn't be obtained, i.e. not enough frames have been collected to output liveness value. */
        if (!FacesProcessor.isLivenessEnabled() || face.getLiveness() < 0)
            return true;

        /* When using IBeta liveness addon handle errors and image quality first. */
        if (FacesProcessor.useIBetaLivenessAddon()) {
            if (face.getLivenessError() != null) {
                appendString(faceText, face.getLivenessError());
                return false;
            }

            if (face.getImageQuality() < 0.5) {
                appendString(faceText, formatString(getStringResource(R.string.low_image_quality), face.getImageQuality()));
                return false;
            }

            appendString(faceText, formatString(getStringResource(R.string.image_quality), face.getImageQuality()));
        }

        if (face.getLiveness() < 0.5) {
            appendString(faceText, formatString(getStringResource(R.string.fake_face), face.getLiveness()));
            return false;
        }

        appendString(faceText, formatString(getStringResource(R.string.live_face), face.getLiveness()));
        return true;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (facesTransform == null)
            return;

        synchronized (this) {
            for (var i = 0; i < detectionResult.getSize(); ++i) {
                final var face = detectionResult.getFace(i);
                final var rect = face.getRect();

                faceText.setLength(0);
                final var liveFace = checkLiveness(face, faceText);

                canvas.drawRect(rect, liveFace ? greenPaint : redPaint);

                final var name = face.getName();
                if (!name.isEmpty())
                    appendString(faceText, name);

                canvas.drawText(faceText.toString(), rect.centerX(), rect.bottom + 50, liveFace ? greenTextPaint : redTextPaint);
            }
        }
    }

    public FacesProcessor.Face getFaceContainingPoint(final float x, final float y) {
        synchronized (this) {
            for (var i = 0; i < detectionResult.getSize(); ++i)
                if (detectionResult.getFace(i).getRect().contains(x, y))
                    return detectionResult.getFace(i);
        }

        return null;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
