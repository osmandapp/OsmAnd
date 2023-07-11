package net.osmand.plus.views;

import net.osmand.core.android.MapRendererView;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.utils.NativeUtilities;

import androidx.annotation.NonNull;

public class Zoom {

	private static final float MIN_METERS_TO_TARGET = 50;

	private final OsmandMapTileView mapView;

	private int baseZoom;
	private float zoomFloatPart;
	private float zoomAnimation;

	private final int minZoom;
	private final int maxZoom;
	private final boolean considerTerrain;

	public Zoom(@NonNull OsmandMapTileView mapView, int baseZoom, float zoomFloatPart, int minZoom, int maxZoom) {
		this(mapView, baseZoom, zoomFloatPart, minZoom, maxZoom, false);
	}

	public Zoom(@NonNull OsmandMapTileView mapView, int baseZoom, float zoomFloatPart, int minZoom, int maxZoom, boolean considerTerrain) {
		this.mapView  = mapView;
		this.baseZoom = baseZoom;
		this.zoomFloatPart = zoomFloatPart;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		this.considerTerrain = considerTerrain;
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

	public boolean isZoomInAllowedOnTerrain() {
		MapRendererView mapRenderer = mapView.getMapRenderer();
		if (mapRenderer == null || !considerTerrain || !isTerrainEnabled()) {
			return true;
		}

		return Math.floor(NativeUtilities.getMetersToFixedElevatedTarget(mapRenderer)) > MIN_METERS_TO_TARGET;
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
		float requestedZoom = baseZoom + step + zoomFloatPart;

		if (step > 0) {
			MapRendererView mapRenderer = mapView.getMapRenderer();
			float maxZoom = (mapRenderer != null && considerTerrain && isTerrainEnabled())
					? recalculateMaxZoomOnTerrain(mapRenderer)
					: this.maxZoom;

			if (requestedZoom > maxZoom) {
				int maxZoomBase = Math.round(maxZoom);
				float maxZoomFloatPart = maxZoom - maxZoomBase;

				if (baseZoom + 1 == maxZoomBase && maxZoomFloatPart < -0.4f) {
					zoomFloatPart = 1 + maxZoomFloatPart;
				} else {
					baseZoom = maxZoomBase;
					zoomFloatPart = maxZoomFloatPart;
				}
			} else {
				baseZoom += step;
			}
		} else {
			if (requestedZoom < minZoom) {
				baseZoom = minZoom;
				zoomFloatPart = 0;
			} else {
				baseZoom += step;
			}
		}
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
		MapRendererView mapRenderer = mapView.getMapRenderer();
		float maxZoom = mapRenderer != null ? recalculateMaxZoomOnTerrain(mapRenderer) : this.maxZoom;
		int maxZoomBase = Math.round(maxZoom);
		float maxZoomFloatPart = maxZoom - maxZoomBase;
		float requiredZoom = baseZoom + zoomFloatPart;

		if (requiredZoom > maxZoom) {
			baseZoom = maxZoomBase;
			zoomFloatPart = maxZoomFloatPart;
		}

		if (baseZoom == minZoom) {
			zoomFloatPart = Math.max(0, zoomFloatPart);
		} else if (baseZoom < minZoom) {
			baseZoom = minZoom;
			zoomFloatPart = 0;
		}
	}

	private float recalculateMaxZoomOnTerrain(@NonNull MapRendererView mapRenderer) {
		double metersToFixedElevatedTarget = NativeUtilities.getMetersToFixedElevatedTarget(mapRenderer);
		double metersToTargetOnPlane = NativeUtilities.getMetersToTargetOnPlane(mapRenderer);
		double leftZoomInMeters = metersToFixedElevatedTarget - MIN_METERS_TO_TARGET;

		boolean aboveSeaLevel = metersToFixedElevatedTarget <= metersToTargetOnPlane;
		if (!aboveSeaLevel) {
			return maxZoom;
		}

		// If zoom has negative float part (17 -0.35), convert it to positive (16 +0.65)
		double currentZoom = mapView.getZoom() + mapView.getRotatedTileBox().getZoomFloatPart();
		int currentIntZoom = (int) currentZoom;
		double currentZoomPositiveFloatPart = currentZoom - currentIntZoom;

		// Get rid of zoom float part
		double currentFloatPartZoomOutMeters = metersToTargetOnPlane * currentZoomPositiveFloatPart;
		metersToTargetOnPlane += currentFloatPartZoomOutMeters;
		leftZoomInMeters += currentFloatPartZoomOutMeters;

		// Zoom out if camera is under terrain
		while (Math.floor(leftZoomInMeters) < 0) {
			double zoomOutMeters = metersToTargetOnPlane;
			leftZoomInMeters += zoomOutMeters;
			metersToTargetOnPlane += zoomOutMeters;
			currentIntZoom--;
		}

		// Make all possible integer zoom ins
		int allowedIntZoomIns = 0;
		while (leftZoomInMeters > metersToTargetOnPlane / 2) {
			double zoomInMeters = metersToTargetOnPlane / 2;
			metersToTargetOnPlane -= zoomInMeters;
			leftZoomInMeters -= metersToTargetOnPlane;
			allowedIntZoomIns++;
		}

		double allowedZoomInFloatPart = metersToTargetOnPlane / (metersToTargetOnPlane - leftZoomInMeters) - 1;
		double maxAllowedZoom = currentIntZoom + allowedIntZoomIns + allowedZoomInFloatPart;
		return (float) Math.min(maxZoom, maxAllowedZoom);
	}

	@NonNull
	public static Zoom checkZoomBounds(@NonNull OsmandMapTileView mapView, int baseZoom, float zoomFloatPart, int minZoom, int maxZoom, boolean considerTerrain) {
		Zoom zoom = new Zoom(mapView, baseZoom, zoomFloatPart, minZoom, maxZoom, considerTerrain);
		zoom.checkZoomBounds();
		return zoom;
	}

	private static boolean isTerrainEnabled() {
		OsmandDevelopmentPlugin developmentPlugin = PluginsHelper.getActivePlugin(OsmandDevelopmentPlugin.class);
		return developmentPlugin != null && developmentPlugin.is3DMapsEnabled();
	}
}