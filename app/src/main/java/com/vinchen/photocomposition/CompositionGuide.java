package com.vinchen.photocomposition;

/** Experimental face-position rules; no body bounds or aesthetic score are inferred. */
public final class CompositionGuide {
    public enum Hint {
        WAITING("正在识别人脸…"), NO_FACE("请将一人的脸部放入画面"),
        MULTIPLE("检测到多人，暂不提供单人指导"),
        UNCERTAIN("请让脸部朝向镜头，再判断位置"),
        SMALL("人脸较小，靠近后再判断"), EDGE("脸部靠近边缘，请留些空间"),
        CLOSE("脸部占比较大，可稍退后"), LEFT("← 向左移动手机"),
        RIGHT("向右移动手机 →"), UP("↑ 向上调整取景"),
        DOWN("↓ 向下调整取景"), READY("可以拍 · 脸部位置合适"),
        ERROR("识别暂不可用，仍可手动拍照");
        public final String text;
        Hint(String text) { this.text = text; }
    }

    private Hint candidate = Hint.WAITING;
    private Hint displayed = Hint.WAITING;
    private long candidateSince;
    private long displayedSince;
    private int samples;

    public static Hint evaluate(int count, float left, float top, float right, float bottom,
                                float yaw, float roll) {
        if (count == 0) return Hint.NO_FACE;
        if (count != 1) return Hint.MULTIPLE;
        if (!Float.isFinite(left + top + right + bottom + yaw + roll)
                || right <= left || bottom <= top) return Hint.UNCERTAIN;
        if (Math.abs(yaw) > 35 || Math.abs(roll) > 30) return Hint.UNCERTAIN;
        if (left < .035f || right > .965f || top < .035f || bottom > .965f) return Hint.EDGE;
        if (bottom - top < .10f || right - left < .08f) return Hint.SMALL;
        if (bottom - top > .55f || right - left > .60f) return Hint.CLOSE;
        float x = (left + right) / 2;
        float y = (top + bottom) / 2;
        // Moving the camera left shifts a stationary subject right in the preview.
        if (x < .28f) return Hint.LEFT;
        if (x > .72f) return Hint.RIGHT;
        if (y < .22f) return Hint.UP;
        if (y > .52f) return Hint.DOWN;
        return Hint.READY;
    }

    public Hint update(Hint next, long now) {
        // Immediately remove stale positive guidance when detection is unreliable.
        if (next == Hint.NO_FACE || next == Hint.MULTIPLE || next == Hint.UNCERTAIN
                || next == Hint.ERROR || next == Hint.SMALL) {
            reset();
            displayed = next;
            displayedSince = now;
            return displayed;
        }
        if (candidate != next) {
            candidate = next;
            candidateSince = now;
            samples = 1;
        } else samples++;
        if (displayed == Hint.READY && next != Hint.READY) {
            displayed = Hint.WAITING;
            displayedSince = now;
        }
        if (samples >= 3 && now - candidateSince >= 500 && now - displayedSince >= 800) {
            displayed = candidate;
            displayedSince = now;
        }
        return displayed;
    }

    public void reset() {
        displayed = candidate = Hint.WAITING;
        candidateSince = displayedSince = 0;
        samples = 0;
    }
}
