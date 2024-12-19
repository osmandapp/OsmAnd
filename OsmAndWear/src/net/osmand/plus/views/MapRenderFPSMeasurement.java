package net.osmand.plus.views;

import android.os.SystemClock;

public class MapRenderFPSMeasurement {

	private static final int HALF_FRAME_BUFFER_LENGTH = 20;

	private long startMs = 0;
	private int startFrameId;
	private long middleMs = 0;
	private int middleFrameId;
	private float fps;

	public float getFps() {
		return fps;
	}

	void calculateFPS(int frameId) {
		long now = SystemClock.elapsedRealtime();
		if (frameId > startFrameId && now > startMs && startMs != 0) {
			fps = 1000.0f / (now - startMs) * (frameId - startFrameId);
		} else {
			fps = 0;
		}
		if (startFrameId == 0 || (middleFrameId - startFrameId) > HALF_FRAME_BUFFER_LENGTH) {
			startMs = middleMs;
			startFrameId = middleFrameId;
		}
		if (middleFrameId == 0 || (frameId - middleFrameId) > HALF_FRAME_BUFFER_LENGTH) {
			middleMs = now;
			middleFrameId = frameId;
		}
	}
}
