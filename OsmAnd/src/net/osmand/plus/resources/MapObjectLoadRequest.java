package net.osmand.plus.resources;

import androidx.annotation.NonNull;

import net.osmand.ResultMatcher;

class MapObjectLoadRequest<T> implements ResultMatcher<T> {

	private final ResourceManager resourceManger;

	protected double topLatitude;
	protected double bottomLatitude;
	protected double leftLongitude;
	protected double rightLongitude;
	protected boolean cancelled;
	protected volatile boolean running;

	public MapObjectLoadRequest(@NonNull ResourceManager resourceManger) {
		this.resourceManger = resourceManger;
	}

	public boolean isContains(double topLatitude, double leftLongitude, double bottomLatitude,
			double rightLongitude) {
		return this.topLatitude >= topLatitude && this.leftLongitude <= leftLongitude
				&& this.rightLongitude >= rightLongitude && this.bottomLatitude <= bottomLatitude;
	}

	public void setBoundaries(double topLatitude, double leftLongitude, double bottomLatitude,
			double rightLongitude) {
		this.topLatitude = topLatitude;
		this.bottomLatitude = bottomLatitude;
		this.leftLongitude = leftLongitude;
		this.rightLongitude = rightLongitude;
	}

	public boolean isRunning() {
		return running && !cancelled;
	}

	public void start() {
		running = true;
	}

	public void finish() {
		running = false;
		// use downloader callback
		resourceManger.getMapTileDownloader().fireLoadCallback(null);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean publish(T object) {
		return true;
	}
}
