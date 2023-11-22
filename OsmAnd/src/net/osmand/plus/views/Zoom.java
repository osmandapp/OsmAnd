package net.osmand.plus.views;

import androidx.annotation.NonNull;

public class Zoom {

	private int baseZoom;
	private float zoomFloatPart;
	private float zoomAnimation;

	private final int minZoomBase;
	private final int maxZoomBase;

	public Zoom(int baseZoom, float zoomFloatPart, int minZoomBase, int maxZoomBase) {
		this.baseZoom = baseZoom;
		this.zoomFloatPart = zoomFloatPart;
		this.minZoomBase = minZoomBase;
		this.maxZoomBase = maxZoomBase;
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
		return baseZoom < maxZoomBase || baseZoom == maxZoomBase && zoomFloatPart < 0;
	}

	public boolean isZoomOutAllowed() {
		return baseZoom > minZoomBase || baseZoom == minZoomBase && zoomFloatPart > 0;
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

	public void applyZoomDelta(float delta) {
		float normalizedDelta = normalizeZoomDelta(baseZoom, delta);
		zoomFloatPart += normalizedDelta;
	}

	public void calculateAnimatedZoom(int currentZoomBase, float zoomDelta) {
		zoomAnimation = normalizeZoomDelta(currentZoomBase, zoomDelta);
	}

	private float normalizeZoomDelta(int currentZoomBase, float zoomDelta) {
		while (zoomFloatPart + zoomDelta >= 0.5 && baseZoom + 1 <= maxZoomBase) {
			zoomDelta--;
			baseZoom++;
		}
		while (zoomFloatPart + zoomDelta < -0.5 && baseZoom - 1 >= minZoomBase) {
			zoomDelta++;
			baseZoom--;
		}

		// Extend zoom float part from [-0.5 ... +0.5) to [-0.6 ... +0.6)
		// Example: previous zoom was 15 + 0.3f. With deltaZoom = 0.25f,
		// zoom will become 15 + 0.55f, not 16 - 0.45f
		if (baseZoom + 1 == currentZoomBase && zoomFloatPart + zoomDelta >= 0.4f) {
			baseZoom++;
			float invertedZoomFloatPart = (zoomFloatPart + zoomDelta) - 1.0f;
			zoomDelta = invertedZoomFloatPart - zoomFloatPart;
		} else if (baseZoom - 1 == currentZoomBase && zoomFloatPart + zoomDelta < -0.4f) {
			baseZoom--;
			float invertedZoomFloatPart = 1.0f + (zoomFloatPart + zoomDelta);
			zoomDelta = invertedZoomFloatPart - zoomFloatPart;
		}

		boolean zoomInOverflow = baseZoom == maxZoomBase && zoomFloatPart + zoomDelta > 0;
		boolean zoomOutOverflow = baseZoom == minZoomBase && zoomFloatPart + zoomDelta < 0;
		if (zoomInOverflow || zoomOutOverflow) {
			zoomDelta = -zoomFloatPart;
		}

		return zoomDelta;
	}

	private void checkZoomBounds() {
		if (baseZoom == maxZoomBase) {
			zoomFloatPart = Math.min(0, zoomFloatPart);
		} else if (baseZoom > maxZoomBase) {
			baseZoom = maxZoomBase;
			zoomFloatPart = 0;
		}

		if (baseZoom == minZoomBase) {
			zoomFloatPart = Math.max(0, zoomFloatPart);
		} else if (baseZoom < minZoomBase) {
			baseZoom = minZoomBase;
			zoomFloatPart = 0;
		}
	}

	@NonNull
	public static Zoom checkZoomBounds(int baseZoom, float zoomFloatPart, int minZoomBase, int maxZoomBase) {
		Zoom zoom = new Zoom(baseZoom, zoomFloatPart, minZoomBase, maxZoomBase);
		zoom.checkZoomBounds();
		return zoom;
	}

	public static class ComplexZoom {

		public final int base;
		public final float floatPart;

		public ComplexZoom(int base, float floatPart) {
			this.base = base;
			this.floatPart = floatPart;
		}

		boolean equals(int base, float floatPart) {
			return this.base == base && this.floatPart == floatPart;
		}
	}
}