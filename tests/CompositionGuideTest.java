package com.vinchen.photocomposition;

import static com.vinchen.photocomposition.CompositionGuide.Hint.*;

/** Plain JVM checks for movement direction and temporal safety, independent of Android. */
public final class CompositionGuideTest {
    private static int checks;
    private static void expect(Object wanted, Object actual) {
        checks++;
        if (wanted != actual) throw new AssertionError("Expected " + wanted + ", got " + actual);
    }
    public static void main(String[] args) {
        expect(NO_FACE, CompositionGuide.evaluate(0,0,0,0,0,0,0));
        expect(MULTIPLE, CompositionGuide.evaluate(2,0,0,0,0,0,0));
        expect(UNCERTAIN, CompositionGuide.evaluate(1,Float.NaN,.2f,.6f,.4f,0,0));
        expect(UNCERTAIN, CompositionGuide.evaluate(1,.4f,.2f,.6f,.4f,50,0));
        expect(EDGE, CompositionGuide.evaluate(1,.01f,.2f,.2f,.4f,0,0));
        expect(SMALL, CompositionGuide.evaluate(1,.4f,.3f,.45f,.36f,0,0));
        expect(CLOSE, CompositionGuide.evaluate(1,.2f,.1f,.8f,.8f,0,0));
        expect(LEFT, CompositionGuide.evaluate(1,.1f,.2f,.3f,.4f,0,0));
        expect(RIGHT, CompositionGuide.evaluate(1,.7f,.2f,.9f,.4f,0,0));
        expect(UP, CompositionGuide.evaluate(1,.4f,.04f,.6f,.24f,0,0));
        expect(DOWN, CompositionGuide.evaluate(1,.4f,.6f,.6f,.8f,0,0));
        expect(READY, CompositionGuide.evaluate(1,.4f,.2f,.6f,.4f,0,0));
        expect(READY, CompositionGuide.evaluate(1,.24f,.2f,.44f,.4f,0,0));
        CompositionGuide g = new CompositionGuide();
        expect(WAITING, g.update(READY,1000));
        expect(WAITING, g.update(READY,1200));
        expect(READY, g.update(READY,1600));
        expect(NO_FACE, g.update(NO_FACE,1610));
        expect(NO_FACE, g.update(READY,1620));
        g.reset();
        expect(WAITING, g.update(LEFT,2000));
        expect(WAITING, g.update(RIGHT,2200));
        expect(WAITING, g.update(LEFT,2400));
        expect(WAITING, g.update(LEFT,2600));
        expect(LEFT, g.update(LEFT,3000));
        expect(MULTIPLE, g.update(MULTIPLE,3010));
        g.reset();
        g.update(READY,4000); g.update(READY,4200); g.update(READY,4600);
        expect(WAITING, g.update(RIGHT,4610));
        expect(ERROR, g.update(ERROR,4620));
        System.out.println("PASS: " + checks + " composition and stability checks");
    }
}
