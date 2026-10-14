package com.vinchen.photocomposition;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Guides share the preview's bounds and are never drawn into the saved photo. */
public final class GridOverlayView extends View {
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GridOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        shadow.setColor(Color.argb(100, 0, 0, 0));
        shadow.setStrokeWidth(2 * density);
        line.setColor(Color.argb(170, 255, 255, 255));
        line.setStrokeWidth(density);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGrid(canvas, shadow);
        drawGrid(canvas, line);
    }

    private void drawGrid(Canvas canvas, Paint paint) {
        for (int i = 1; i < 3; i++) {
            float x = getWidth() * i / 3f;
            float y = getHeight() * i / 3f;
            canvas.drawLine(x, 0, x, getHeight(), paint);
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
    }
}
