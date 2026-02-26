package net.osmand.plus.views;

class FPSMeasurement {

	private int fpsMeasureCount;
	private int fpsMeasureMs;
	private long fpsFirstMeasurement;
	private float fps;

	public float getFps() {
		return fps;
	}

	void calculateFPS(long start, long end) {
		fpsMeasureMs += end - start;
		fpsMeasureCount++;
		if (fpsMeasureCount > 10 || (start - fpsFirstMeasurement) > 400) {
			fpsFirstMeasurement = start;
			fps = (1000f * fpsMeasureCount / fpsMeasureMs);
			fpsMeasureCount = 0;
			fpsMeasureMs = 0;
		}
	}
}
