package net.osmand.plus.views;

import androidx.annotation.NonNull;

public class Zoom {

	private int baseZoom;
	private float zoomFloatPart;
	private float zoomAnimation;

	private final int minZoom;
	private final int maxZoom;

	public Zoom(int baseZoom, float zoomFloatPart, int minZoom, int maxZoom) {
		this.baseZoom = baseZoom;
		this.zoomFloatPart = zoomFloatPart;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
	}

	public int getBaseZoom() {
		return baseZoom;
	}

	public float getZoomFloatPart() {
		return zoomFloatPart;
	}

	public float getZoomAnimation() {
		return zoomAnimation;
	}

	public boolean isZoomInAllowed() {
		return baseZoom < maxZoom || baseZoom == maxZoom && zoomFloatPart < 0;
	}

	public boolean isZoomOutAllowed() {
		return baseZoom > minZoom || baseZoom == minZoom && zoomFloatPart > 0;
	}

	public void zoomIn() {
		changeZoom(1);
	}

	public void zoomOut() {
		changeZoom(-1);
	}

	public void changeZoom(int step) {
		baseZoom += step;
		checkZoomBounds();
	}

	public void calculateAnimatedZoom(int currentBaseZoom, float deltaZoom) {
		while (zoomFloatPart + deltaZoom >= 0.5 && baseZoom + 1 <= maxZoom) {
			deltaZoom--;
			baseZoom++;
		}
		while (zoomFloatPart + deltaZoom < -0.5 && baseZoom - 1 >= minZoom) {
			deltaZoom++;
			baseZoom--;
		}

		// Extend zoom float part from [-0.5 ... +0.5) to [-0.6 ... +0.6)
		// Example: previous zoom was 15 + 0.3f. With deltaZoom = 0.25f,
		// zoom will become 15 + 0.55f, not 16 - 0.45f
		if (baseZoom + 1 == currentBaseZoom && zoomFloatPart + deltaZoom >= 0.4f) {
			baseZoom++;
			float invertedZoomFloatPart = (zoomFloatPart + deltaZoom) - 1.0f;
			deltaZoom = invertedZoomFloatPart - zoomFloatPart;
		} else if (baseZoom - 1 == currentBaseZoom && zoomFloatPart + deltaZoom < -0.4f) {
			baseZoom--;
			float invertedZoomFloatPart = 1.0f + (zoomFloatPart + deltaZoom);
			deltaZoom = invertedZoomFloatPart - zoomFloatPart;
		}

		boolean zoomInOverflow = baseZoom == maxZoom && zoomFloatPart + deltaZoom > 0;
		boolean zoomOutOverflow = baseZoom == minZoom && zoomFloatPart + deltaZoom < 0;
		if (zoomInOverflow || zoomOutOverflow) {
			deltaZoom = -zoomFloatPart;
		}

		zoomAnimation = deltaZoom;
	}

	private void checkZoomBounds() {
		if (baseZoom == maxZoom) {
			zoomFloatPart = Math.min(0, zoomFloatPart);
		} else if (baseZoom > maxZoom) {
			baseZoom = maxZoom;
			zoomFloatPart = 0;
		}

		if (baseZoom == minZoom) {
			zoomFloatPart = Math.max(0, zoomFloatPart);
		} else if (baseZoom < minZoom) {
			baseZoom = minZoom;
			zoomFloatPart = 0;
		}
	}

	@NonNull
	public static Zoom checkZoomBounds(int baseZoom, float zoomFloatPart, int maxZoom, int minZoom) {
		Zoom zoom = new Zoom(baseZoom, zoomFloatPart, maxZoom, minZoom);
		zoom.checkZoomBounds();
		return zoom;
	}
}