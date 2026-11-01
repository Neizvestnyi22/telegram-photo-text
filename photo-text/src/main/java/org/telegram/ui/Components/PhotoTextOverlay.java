package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Drawn above the photo in PhotoViewer: dims the picture, lights up recognized lines with a
 * sweep, and lets the user tap lines to select them. imageMatrix maps photo pixels to view
 * coordinates (PhotoViewer's current scale and translation).
 */
public class PhotoTextOverlay extends View {

    public interface Listener {
        void onSelectionChanged(ArrayList<PhotoText.Line> selected);
    }

    private final Paint dim = new Paint();
    private final Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedBox = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Matrix imageMatrix = new Matrix();
    private ArrayList<PhotoText.Line> lines = new ArrayList<>();
    private final HashSet<Integer> selected = new HashSet<>();
    private long shownAt;
    private Listener listener;

    public PhotoTextOverlay(Context context) {
        super(context);
        dim.setColor(0x73000000);
        box.setColor(0x40FFFFFF);
        selectedBox.setColor(0x663390EC);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(AndroidUtilities.dp(1.5f));
        edge.setColor(0xFF3390EC);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setImageMatrix(Matrix m) {
        imageMatrix.set(m);
        invalidate();
    }

    public void show(ArrayList<PhotoText.Line> lines) {
        this.lines = lines;
        selected.clear();
        shownAt = SystemClock.elapsedRealtime();
        setVisibility(VISIBLE);
        invalidate();
    }

    public void hide() {
        setVisibility(GONE);
        selected.clear();
    }

    public boolean isShown2() {
        return getVisibility() == VISIBLE;
    }

    public RectF lineRect(int i) {
        RectF r = new RectF(lines.get(i).box);
        r.inset(-AndroidUtilities.dp(4) / scale(), -AndroidUtilities.dp(3) / scale());
        imageMatrix.mapRect(r);
        return r;
    }

    public int count() {
        return lines.size();
    }

    public int find(String part) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).text.contains(part)) {
                return i;
            }
        }
        return -1;
    }

    private float scale() {
        float[] v = new float[9];
        imageMatrix.getValues(v);
        return v[Matrix.MSCALE_X];
    }

    public void selectAll() {
        for (int i = 0; i < lines.size(); i++) {
            selected.add(i);
        }
        notifySelection();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long t = SystemClock.elapsedRealtime() - shownAt;
        float appear = Math.min(1f, t / 250f);
        dim.setAlpha((int) (0x73 * appear));
        canvas.drawRect(0, 0, getWidth(), getHeight(), dim);
        // The highlight sweeps top-down once, like a scanner, then stays.
        float sweep = Math.min(1f, t / 700f);
        float limit = getHeight() * sweep;
        float r = AndroidUtilities.dp(6);
        for (int i = 0; i < lines.size(); i++) {
            RectF lr = lineRect(i);
            if (lr.top > limit) {
                continue;
            }
            if (selected.contains(i)) {
                canvas.drawRoundRect(lr, r, r, selectedBox);
                canvas.drawRoundRect(lr, r, r, edge);
            } else {
                canvas.drawRoundRect(lr, r, r, box);
            }
        }
        if (sweep < 1f || appear < 1f) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_UP) {
            for (int i = 0; i < lines.size(); i++) {
                if (lineRect(i).contains(e.getX(), e.getY())) {
                    if (!selected.remove(i)) {
                        selected.add(i);
                    }
                    notifySelection();
                    invalidate();
                    return true;
                }
            }
        }
        return true;
    }

    private void notifySelection() {
        ArrayList<PhotoText.Line> list = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (selected.contains(i)) {
                list.add(lines.get(i));
            }
        }
        if (listener != null) {
            listener.onSelectionChanged(list);
        }
    }
}
