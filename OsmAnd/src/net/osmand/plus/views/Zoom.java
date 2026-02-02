package net.osmand.plus.views;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;

public class Zoom {

	private int baseZoom;
	private float zoomFloatPart;
	private float zoomAnimation;

	private final float minZoom;
	private final float maxZoom;

	public Zoom(int baseZoom, float zoomFloatPart, int minZoom, int maxZoom) {
		this.baseZoom = baseZoom;
		this.zoomFloatPart = zoomFloatPart;
		this.minZoom = (float) minZoom;
		this.maxZoom = (float) maxZoom;
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

	public boolean isZoomInAllowed(MapRendererView mapRendererView) {
        return zoomFloatPart + baseZoom < (mapRendererView != null ? mapRendererView.getMaxZoomLevel() : maxZoom);
	}

	public boolean isZoomOutAllowed(MapRendererView mapRendererView) {
        return zoomFloatPart + baseZoom > (mapRendererView != null ? mapRendererView.getMinZoomLevel() : minZoom);
	}

	public void zoomIn(MapRendererView mapRendererView) {
		changeZoom(mapRendererView, 1);
	}

	public void zoomOut(MapRendererView mapRendererView) {
		changeZoom(mapRendererView, -1);
	}

	public void partialChangeZoom(MapRendererView mapRendererView, float deltaZoom) {
		while (zoomFloatPart + deltaZoom >= 0.5) {
			deltaZoom--;
			baseZoom++;
		}
		while (zoomFloatPart + deltaZoom < -0.5) {
			deltaZoom++;
			baseZoom--;
		}
		zoomFloatPart += deltaZoom;
		checkZoomBounds(mapRendererView, minZoom, maxZoom);
	}

	public void changeZoom(MapRendererView mapRendererView, int step) {
		baseZoom += step;
		checkZoomBounds(mapRendererView, minZoom, maxZoom);
	}

	public void calculateAnimatedZoom(MapRendererView mapRendererView, int currentBaseZoom, float deltaZoom) {
		while (zoomFloatPart + deltaZoom >= 0.5) {
			deltaZoom--;
			baseZoom++;
		}
		while (zoomFloatPart + deltaZoom < -0.5) {
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

        float minZoom = this.minZoom;
        float maxZoom = this.maxZoom;
        if (mapRendererView != null) {
            minZoom = mapRendererView.getMinZoomLevel();
            maxZoom = mapRendererView.getMaxZoomLevel();
        }

        if (baseZoom == (int) maxZoom && zoomFloatPart + deltaZoom > maxZoom - baseZoom) {
            deltaZoom = maxZoom - baseZoom - zoomFloatPart;
        }
        else if (baseZoom == (int) Math.ceil(minZoom) && zoomFloatPart + deltaZoom < minZoom - baseZoom) {
            deltaZoom = minZoom - baseZoom - zoomFloatPart;
        }

		zoomAnimation = deltaZoom;
	}

    private void clampZoom(float minZoom, float maxZoom) {
        float zoom = (float) baseZoom + zoomFloatPart;
        if (zoom > maxZoom) {
            baseZoom = (int) maxZoom;
            zoomFloatPart = maxZoom - baseZoom;
        }

        if (zoom < minZoom) {
            baseZoom = (int) Math.ceil(minZoom);
            zoomFloatPart = minZoom - baseZoom;
        }
    }

    private void checkZoomBounds(MapRendererView mapRendererView, float minZoom, float maxZoom) {
        if (mapRendererView != null)
            clampZoom(mapRendererView.getMinZoomLevel(), mapRendererView.getMaxZoomLevel());
        else
            clampZoom(minZoom, maxZoom);
    }

	@NonNull
	public static Zoom checkZoomBounds(MapRendererView mapRendererView, float zoom, int minZoom, int maxZoom) {
		return checkZoomBounds(mapRendererView, (int) zoom, zoom - (int) zoom, minZoom, maxZoom);
	}

	@NonNull
	public static Zoom checkZoomBounds(MapRendererView mapRendererView, int baseZoom, float zoomFloatPart, int minZoom, int maxZoom) {
		Zoom zoom = new Zoom(baseZoom, zoomFloatPart, minZoom, maxZoom);
		zoom.checkZoomBounds(mapRendererView, minZoom, maxZoom);
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