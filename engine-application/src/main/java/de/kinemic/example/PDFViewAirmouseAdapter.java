/*
 * Copyright (c) 2018 Kinemic GmbH. All rights reserved.
 */

package de.kinemic.example;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.util.Log;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;

import de.kinemic.gesture.AirmousePalmDirection;
import de.kinemic.gesture.Engine;
import de.kinemic.gesture.OnAirmouseEventListener;

public class PDFViewAirmouseAdapter implements OnAirmouseEventListener, OnDrawListener, LifecycleObserver {
    private static final String TAG = PDFViewAirmouseAdapter.class.getSimpleName();

    private final static float MAX_ZOOM = 14f;
    private final static float MIN_ZOOM = 1f;
    private final static long REFRESH_TIMEOUT = 250L;

    private final static float ZOOM_THRESHOLD = 0.01f;

    /* these may need some adjusting to have a good feel */
    private final static float PAN_FACTOR = 25f;
    private final static float ZOOM_FACTOR = 0.025f;

    private Engine engine;
    private PDFView pdfView;
    private Handler handler;

    private boolean airmouseValid = false;
    private float airmouseX, airmouseY;
    private AirmousePalmDirection airmouseMode = AirmousePalmDirection.INCONCLUSIVE;
    private boolean airmouseActive = false;

    private final Paint pointerPaint = new Paint();
    private final Paint bandPaint = new Paint();

    private Runnable refresher = () -> pdfView.loadPages();
    
    PDFViewAirmouseAdapter(LifecycleOwner owner, Engine engine, PDFView pdfView) {
        pointerPaint.setColor(Color.RED);
        bandPaint.setColor(Color.BLUE);
        
        handler = new Handler();
        this.engine = engine;
        this.pdfView = pdfView;

        owner.getLifecycle().addObserver(this);
    }

    boolean isAirmouseActive() {
        return airmouseActive;
    }

    void setAirmouseActive(boolean active) {
        if (active == airmouseActive) return;
        if (active) {
            airmouseActive = true;
            engine.startAirmouse();
            Log.d(TAG, "airmouse started");
        } else {
            airmouseActive = false;
            engine.stopAirmouse();
            Log.d(TAG, "airmouse stopped");
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void registerListeners() {
        engine.registerOnAirmouseEventListener(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void unregisterListeners() {
        engine.unregisterOnAirmouseEventListener(this);
    }

    /* used to correctly calculate dx when x wraps from -180 to 180 and 180 to -180 */
    private float wrapDx(float dx) {
        if (dx > 180.0) return dx - 360.0f;
        else if (dx < -180.0) return dx + 360.0f;
        else return dx;
    }

    private void refreshWhenIdle() {
        handler.removeCallbacks(refresher);
        handler.postDelayed(refresher, REFRESH_TIMEOUT);
    }

    @Override
    public void onMove(float x, float y, float wrist_angle, @NonNull AirmousePalmDirection palmFacing) {
        //Log.d(TAG, "onMove: " + x + ", " + y + ", " + EnumNames.fromFacing(palmFacing));
        if (!airmouseValid || palmFacing != airmouseMode) {
            airmouseX = x;
            airmouseY = y;
            airmouseValid = true;
            airmouseMode = palmFacing;
            pdfView.invalidate(); // for onDraw
            return;
        }

        float dx = wrapDx(airmouseX - x);
        float dy = (airmouseY - y);

        //Log.v(TAG, String.format("onMove(%.2f, %.2f, %s), dx: %.2f, dy: %.2f", x, y, palmFacing, dx, dy));

        switch (palmFacing) {
            case DOWNWARDS: /* grabbing */
                pdfView.moveRelativeTo(dx * (-1) * PAN_FACTOR, dy * PAN_FACTOR);
                refreshWhenIdle();
                break;
            case UPWARDS: /* zooming */
                float zoom = dy * ZOOM_FACTOR;

                if ((zoom > ZOOM_THRESHOLD && pdfView.getZoom() > MIN_ZOOM) || (zoom < -ZOOM_THRESHOLD && pdfView.getZoom() < MAX_ZOOM)) {
                    pdfView.zoomCenteredRelativeTo(1f-zoom, new PointF(pdfView.getWidth() / 2, pdfView.getHeight() / 2));
                    refreshWhenIdle();
                }
                break;
            case SIDEWAYS: /* neutral (hovering) */
            case INCONCLUSIVE:
        }

        airmouseX = x;
        airmouseY = y;
        pdfView.invalidate(); // for onDraw
    }

    @Override
    public void onClick() {
        Log.d(TAG, "airmouse 'click'");
        airmouseValid = false;
        engine.startAirmouse(); /* this resets the origin to the current orientation */
    }

    /* only for debug purposes, register this object as onDraw Listener */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onLayerDrawn(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {
        if (airmouseActive) {
            float pointerX = canvas.getClipBounds().centerX() + (canvas.getClipBounds().width() * airmouseX / 180.0f) / 2.f;
            float pointerY = canvas.getClipBounds().centerY() - (canvas.getClipBounds().height() * airmouseY / 90.0f) / 2.f;
            float radius = canvas.getClipBounds().width() * 0.1f;
            float band_height_radius = radius * 0.30f;
            float band_width_radius = radius * 0.7f;
            float band_radius = band_height_radius * 0.8f;

            canvas.drawCircle(
                    pointerX,
                    pointerY,
                    radius, pointerPaint);

            if (airmouseMode == AirmousePalmDirection.DOWNWARDS) {
                canvas.drawRoundRect(
                        pointerX - band_width_radius,
                        pointerY - band_height_radius - radius,
                        pointerX + band_width_radius,
                        pointerY + band_height_radius - radius,
                        band_radius,
                        band_radius,
                        bandPaint);
            } else if (airmouseMode == AirmousePalmDirection.UPWARDS) {
                canvas.drawRoundRect(
                        pointerX - band_width_radius,
                        pointerY - band_height_radius + radius,
                        pointerX + band_width_radius,
                        pointerY + band_height_radius + radius,
                        band_radius,
                        band_radius,
                        bandPaint);
            } else if (airmouseMode == AirmousePalmDirection.SIDEWAYS) {
                canvas.drawRoundRect(
                        pointerX - band_height_radius + radius,
                        pointerY - band_width_radius,
                        pointerX + band_height_radius + radius,
                        pointerY + band_width_radius,
                        band_radius,
                        band_radius,
                        bandPaint);
            }
        }
    }
}
