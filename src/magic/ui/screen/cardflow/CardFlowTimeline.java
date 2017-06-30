package magic.ui.screen.cardflow;

import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.callback.TimelineCallback;
import org.pushingpixels.trident.ease.Spline;

class CardFlowTimeline extends Timeline {

    /**
     * Sets custom pulse behavior - higher frame rate, lower frame rate or dynamic frame rate.
     * <p>
     * By default, Trident timelines are driven by a dedicated thread that wakes up every 40ms and
     * updates all the timelines. When the CPU is not heavily used this results in 25 frames-per-second
     * refresh rate for Trident-driven UI animations - consistent with the frame rate of theatrical films
     * and non-interlaced PAL television standard.
     * <p>
     * (see https://kenai.com/projects/trident/pages/CustomPulseSource)
     *
     * Must be run before any instance of Timeline is created in the application otherwise it will
     * generate the "cannot replace the pulse source thread once it's running..." error.
     */
//    static {
//        try {
//            TridentConfig.getInstance().setPulseSource(() -> {
//                try {
//                    Thread.sleep(30);
//                } catch (InterruptedException ex) {
//                    LOGGER.log(Level.WARNING, null, ex);
//                }
//            });
//        } catch (RuntimeException ex) {
//            LOGGER.log(Level.WARNING, ex.getMessage());
//        }
//    }

    CardFlowTimeline(TimelineCallback aCallback, long durationMs) {
        setDuration(durationMs);
        setEase(new Spline(0.8f));
        addCallback(aCallback);
    }

}
