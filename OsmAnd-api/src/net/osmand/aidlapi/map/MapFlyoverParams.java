package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Camera flight over the map through key frames, rendered off the screen frame by frame into an MP4 video.
 * Each key frame has a camera (center, zoom, azimuth, elevation angle) and the time in ms when it is reached;
 * the camera moves smoothly between them. OsmAnd fills in the result fields.
 */
public class MapFlyoverParams extends AidlParams {

	private double[] latitudes = new double[0];
	private double[] longitudes = new double[0];
	private float[] zooms = new float[0];
	private float[] azimuths = new float[0];
	private float[] elevationAngles = new float[0];
	private long[] timesMs = new long[0];
	private int width = 1280;
	private int height = 720;
	private int fps = 30;
	private String fileName = "";
	private String spec = "";
	private String warnings = "";

	private String filePath = "";
	private String error = "";
	private int frames;
	private long renderTimeMs;
	private float maxScreenSpeed;

	public MapFlyoverParams(double[] latitudes, double[] longitudes, float[] zooms, float[] azimuths,
	                        float[] elevationAngles, long[] timesMs, int width, int height, int fps, String fileName) {
		this.latitudes = latitudes;
		this.longitudes = longitudes;
		this.zooms = zooms;
		this.azimuths = azimuths;
		this.elevationAngles = elevationAngles;
		this.timesMs = timesMs;
		this.width = width;
		this.height = height;
		this.fps = fps;
		this.fileName = fileName;
	}

	public MapFlyoverParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<MapFlyoverParams> CREATOR = new Creator<MapFlyoverParams>() {
		@Override
		public MapFlyoverParams createFromParcel(Parcel in) {
			return new MapFlyoverParams(in);
		}

		@Override
		public MapFlyoverParams[] newArray(int size) {
			return new MapFlyoverParams[size];
		}
	};

	public double[] getLatitudes() {
		return latitudes;
	}

	public double[] getLongitudes() {
		return longitudes;
	}

	public float[] getZooms() {
		return zooms;
	}

	/**
	 * @return map rotation in degrees per key frame: the direction that is up on the screen, 0 is north
	 */
	public float[] getAzimuths() {
		return azimuths;
	}

	/**
	 * @return camera angle above the horizon per key frame: 90 is a flat map
	 */
	public float[] getElevationAngles() {
		return elevationAngles;
	}

	/**
	 * @return time of each key frame from the start of the video, ms, ascending
	 */
	public long[] getTimesMs() {
		return timesMs;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getFps() {
		return fps;
	}

	/**
	 * @return name of the video file without a folder; empty for a name from the current time
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @return full path of the written video, empty when it failed
	 */
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * @return why the video was not written, empty on success
	 */
	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public int getFrames() {
		return frames;
	}

	public void setFrames(int frames) {
		this.frames = frames;
	}

	public long getRenderTimeMs() {
		return renderTimeMs;
	}

	public void setRenderTimeMs(long renderTimeMs) {
		this.renderTimeMs = renderTimeMs;
	}

	/**
	 * @return fastest movement of the map on the screen, video pixels per second; above ~250 sharp lines strobe
	 */
	public float getMaxScreenSpeed() {
		return maxScreenSpeed;
	}

	public void setMaxScreenSpeed(float maxScreenSpeed) {
		this.maxScreenSpeed = maxScreenSpeed;
	}

	/**
	 * @return the whole flyover request as JSON (tracks, panels, OsmAnd widgets...), see the AI Connector flyover guide;
	 * key frames and video size above take precedence
	 */
	public String getSpec() {
		return spec;
	}

	public void setSpec(String spec) {
		this.spec = spec;
	}

	/**
	 * @return what OsmAnd did not understand in the request and ignored, empty when all was used
	 */
	public String getWarnings() {
		return warnings;
	}

	public void setWarnings(String warnings) {
		this.warnings = warnings;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putDoubleArray("latitudes", latitudes);
		bundle.putDoubleArray("longitudes", longitudes);
		bundle.putFloatArray("zooms", zooms);
		bundle.putFloatArray("azimuths", azimuths);
		bundle.putFloatArray("elevationAngles", elevationAngles);
		bundle.putLongArray("timesMs", timesMs);
		bundle.putInt("width", width);
		bundle.putInt("height", height);
		bundle.putInt("fps", fps);
		bundle.putString("fileName", fileName);
		bundle.putString("spec", spec);
		bundle.putString("warnings", warnings);
		bundle.putString("filePath", filePath);
		bundle.putString("error", error);
		bundle.putInt("frames", frames);
		bundle.putLong("renderTimeMs", renderTimeMs);
		bundle.putFloat("maxScreenSpeed", maxScreenSpeed);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		latitudes = orEmpty(bundle.getDoubleArray("latitudes"));
		longitudes = orEmpty(bundle.getDoubleArray("longitudes"));
		zooms = orEmpty(bundle.getFloatArray("zooms"));
		azimuths = orEmpty(bundle.getFloatArray("azimuths"));
		elevationAngles = orEmpty(bundle.getFloatArray("elevationAngles"));
		timesMs = bundle.getLongArray("timesMs") != null ? bundle.getLongArray("timesMs") : new long[0];
		width = bundle.getInt("width", 1280);
		height = bundle.getInt("height", 720);
		fps = bundle.getInt("fps", 30);
		fileName = bundle.getString("fileName", "");
		spec = bundle.getString("spec", "");
		warnings = bundle.getString("warnings", "");
		filePath = bundle.getString("filePath", "");
		error = bundle.getString("error", "");
		frames = bundle.getInt("frames");
		renderTimeMs = bundle.getLong("renderTimeMs");
		maxScreenSpeed = bundle.getFloat("maxScreenSpeed");
	}

	private static double[] orEmpty(double[] a) {
		return a != null ? a : new double[0];
	}

	private static float[] orEmpty(float[] a) {
		return a != null ? a : new float[0];
	}
}
