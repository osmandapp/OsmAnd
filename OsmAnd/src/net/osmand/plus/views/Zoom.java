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
	public static Zoom checkZoomBounds(float zoom, int minZoom, int maxZoom) {
		return checkZoomBounds((int) zoom, zoom - (int) zoom, minZoom, maxZoom);
	}

	@NonNull
	public static Zoom checkZoomBounds(int baseZoom, float zoomFloatPart, int minZoom, int maxZoom) {
		Zoom zoom = new Zoom(baseZoom, zoomFloatPart, minZoom, maxZoom);
		zoom.checkZoomBounds();
		return zoom;
	}

	// Example: getDistanceAfterZoom(100, 10, 11) == 50
	public static float getDistanceAfterZoom(float distance, float startZoom, float endZoom) {
		int startZoomBase = (int) startZoom;
		float startZoomFloatPart = startZoom - startZoomBase;

		float distanceNoStartZoomFloatPart = distance * floatPartToVisual(startZoomFloatPart);

		float zoomDeltaFromStartZoomBase = endZoom - startZoomBase;
		float zoomFactorFromStartIntZoom = (1 << (int) Math.abs(zoomDeltaFromStartZoomBase))
				* (Math.abs(zoomDeltaFromStartZoomBase - (int) zoomDeltaFromStartZoomBase) + 1.0f);
		if (zoomDeltaFromStartZoomBase < 0.0f) {
			zoomFactorFromStartIntZoom = 1.0f / zoomFactorFromStartIntZoom;
		}

		return distanceNoStartZoomFloatPart / zoomFactorFromStartIntZoom;
	}

	// Example: fromDistanceRatio(100, 200, 15.5f) == 14.5f
	public static float fromDistanceRatio(double startDistance, double endDistance, float startZoom) {
		int startIntZoom = (int) startZoom;
		float startZoomFloatPart = startZoom - startIntZoom;
		double startDistanceIntZoom = startDistance * floatPartToVisual(startZoomFloatPart);
		double log2 = Math.log(startDistanceIntZoom / endDistance) / Math.log(2);
		int intZoomDelta = (int) log2;
		double startDistanceIntZoomed = intZoomDelta >= 0
				? startDistanceIntZoom / (1 << intZoomDelta)
				: startDistanceIntZoom * (1 << -intZoomDelta);
		float zoomFloatPartDelta = visualToFloatPart((float) (startDistanceIntZoomed / endDistance));
		return startIntZoom + intZoomDelta + zoomFloatPartDelta;
	}

	public static float floatPartToVisual(float zoomFloatPart) {
		return zoomFloatPart >= 0
				? 1.0f + zoomFloatPart
				: 1.0f + zoomFloatPart / 2.0f;
	}

	public static float visualToFloatPart(float visualZoom) {
		return visualZoom >= 1.0f
				? visualZoom - 1.0f
				: (visualZoom - 1.0f) * 2.0f;
	}

	public static class ComplexZoom {
		public int base;
		public float floatPart;

		public ComplexZoom(float zoom) {
			this(Math.round(zoom), zoom - Math.round(zoom));
		}

		public ComplexZoom(int base, float floatPart) {
			this.base = base;
			this.floatPart = floatPart;
		}

		public float fullZoom() {
			return base + floatPart;
		}

		@NonNull
		public static ComplexZoom fromPreferredBase(float zoom, int preferredZoomBase) {
			float floatPart = zoom - (int) zoom;
			if (floatPart >= 0.4f && (int) zoom + 1 == preferredZoomBase) {
				return new ComplexZoom(preferredZoomBase, zoom - preferredZoomBase);
			} else if (floatPart < 0.6f && (int) zoom == preferredZoomBase) {
				return new ComplexZoom(preferredZoomBase, zoom - preferredZoomBase);
			} else {
				return new ComplexZoom(zoom);
			}
		}
	}
}