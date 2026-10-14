package com.vinchen.photocomposition;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public final class FaceOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<RectF> boxes = new ArrayList<>();
    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    public void showFaces(List<RectF> faces, boolean ready) {
        boxes.clear();
        for (RectF box : faces) boxes.add(new RectF(box));
        paint.setColor(ready ? Color.rgb(102, 230, 150) : Color.rgb(255, 210, 100));
        invalidate();
    }
    public void clear() { boxes.clear(); invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.clipRect(0, 0, getWidth(), getHeight());
        for (RectF box : boxes) canvas.drawRoundRect(box, 12, 12, paint);
    }
}
