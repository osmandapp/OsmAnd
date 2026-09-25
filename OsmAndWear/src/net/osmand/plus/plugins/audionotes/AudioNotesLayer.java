package net.osmand.plus.plugins.audionotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.AudioNotesTileProvider;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AudioNotesLayer extends OsmandMapLayer implements
		IContextMenuProvider, ContextMenuLayer.IMoveObjectProvider {

	private static final int START_ZOOM = 10;
	private final Context ctx;
	private final AudioVideoNotesPlugin plugin;
	private ContextMenuLayer contextMenuLayer;
	private boolean changeMarkerPositionMode;

	//OpenGL
	private AudioNotesTileProvider audioNotesTileProvider;

	public AudioNotesLayer(@NonNull Context context, @NonNull AudioVideoNotesPlugin plugin) {
		super(context);
		this.ctx = context;
		this.plugin = plugin;
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	public int getRadiusPoi(RotatedTileBox tb) {
		int r;
		if (tb.getZoom() < START_ZOOM) {
			r = 0;
		} else {
			r = 15;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof Recording) {
			Recording objectInMotion = (Recording) contextMenuLayer.getMoveableObject();
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			float textScale = getTextScale();
			drawRecording(canvas, objectInMotion, pf.x, pf.y, textScale);
			if (!changeMarkerPositionMode) {
				changeMarkerPositionMode = true;
				clearAudioVideoNotes();
			}
		} else if (changeMarkerPositionMode) {
			changeMarkerPositionMode = false;
			clearAudioVideoNotes();
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);

		OsmandApplication app = getApplication();
		MapRendererView mapRenderer = getMapRenderer();

		if (mapRenderer != null) {
			if (app.getResourceManager().isReloadingIndexes()) {
				clearAudioVideoNotes();
				return;
			}

			DataTileManager<Recording> recs = plugin.getRecordings();
			List<Recording> objects =  recs.getAllObjects();
			int objectsCount = objects.size() - (contextMenuLayer.isInChangeMarkerPositionMode()
					&& contextMenuLayer.getMoveableObject() instanceof Recording ? 1 : 0);
			if (audioNotesTileProvider != null && objectsCount != audioNotesTileProvider.getPoints31().size()) {
				clearAudioVideoNotes();
			}
			if (audioNotesTileProvider == null) {
				audioNotesTileProvider = new AudioNotesTileProvider(ctx, getPointsOrder(), view.getDensity());
			}
			if (objectsCount > 0 && audioNotesTileProvider.getPoints31().isEmpty()) {
				float textScale = getTextScale();
				for (Recording o : objects) {
					if (o != contextMenuLayer.getMoveableObject()) {
						audioNotesTileProvider.addToData(o, textScale);
					}
				}
				audioNotesTileProvider.drawSymbols(mapRenderer);
			}
		} else {
			if (tileBox.getZoom() >= START_ZOOM) {
				float textScale = getTextScale();
				float iconSize = getIconSize(app);
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
				DataTileManager<Recording> recs = plugin.getRecordings();
				QuadRect latlon = tileBox.getLatLonBounds();
				List<Recording> objects = recs.getObjects(latlon.top, latlon.left, latlon.bottom, latlon.right);
				List<Recording> fullObjects = new ArrayList<>();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				for (Recording o : objects) {
					if (o != contextMenuLayer.getMoveableObject()) {
						float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
						float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
						if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
							PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(ctx,
									ContextCompat.getColor(ctx, R.color.audio_video_icon_color), true);
							pointImageDrawable.setAlpha(0.8f);
							pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
							smallObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
						} else {
							fullObjects.add(o);
							fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
						}
					}
				}
				for (Recording o : fullObjects) {
					float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
					drawRecording(canvas, o, x, y, textScale);
				}
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
	}

	private void drawRecording(Canvas canvas, Recording o, float x, float y, float textScale) {
		int iconId;
		if (o.isPhoto()) {
			iconId = R.drawable.mx_special_photo_camera;
		} else if (o.isAudio()) {
			iconId = R.drawable.mx_special_microphone;
		} else {
			iconId = R.drawable.mx_special_video_camera;
		}
		PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(ctx,
				ContextCompat.getColor(ctx, R.color.audio_video_icon_color), true, iconId);
		pointImageDrawable.setAlpha(0.8f);
		pointImageDrawable.drawPoint(canvas, x, y, textScale, false);
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		clearAudioVideoNotes();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof Recording) {
			Recording rec = (Recording) o;
			if (rec.getFile().exists()) {
				String recName = rec.getName(ctx, true);
				if (Algorithms.isEmpty(recName)) {
					return new PointDescription(rec.getSearchHistoryType(), view.getResources().getString(R.string.recording_default_name));
				}
				return new PointDescription(rec.getSearchHistoryType(), recName);
			} else {
				boolean reloadingIndexes = getApplication().getResourceManager().isReloadingIndexes();
				if (!reloadingIndexes) {
					plugin.deleteRecording(rec, true);
				}
			}
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (tileBox.getZoom() >= START_ZOOM) {
			getRecordingsFromPoint(point, tileBox, objects);
		}
	}

	public void getRecordingsFromPoint(PointF point, RotatedTileBox tileBox, List<? super Recording> am) {
		Collection<Recording> allRecordings = plugin.getAllRecordings();
		if (Algorithms.isEmpty(allRecordings)) {
			return;
		}

		MapRendererView mapRenderer = getMapRenderer();
		float radius = getScaledTouchRadius(getApplication(), getRadiusPoi(tileBox)) * TOUCH_RADIUS_MULTIPLIER;
		List<PointI> touchPolygon31 = null;
		if (mapRenderer != null) {
			touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius);
			if (touchPolygon31 == null) {
				return;
			}
		}

		for (Recording recording : allRecordings) {
			double lat = recording.getLatitude();
			double lon = recording.getLongitude();

			boolean add = mapRenderer != null
					? NativeUtilities.isPointInsidePolygon(lat, lon, touchPolygon31)
					: tileBox.isLatLonNearPixel(lat, lon, point.x, point.y, radius);
			if (add) {
				am.add(recording);
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof Recording) {
			return new LatLon(((Recording) o).getLatitude(), ((Recording) o).getLongitude());
		}
		return null;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof Recording;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position, @Nullable ApplyMovedObjectCallback callback) {
		boolean result = false;
		if (o instanceof Recording) {
			result = ((Recording) o).setLocation(position);
		}
		if (callback != null) {
			callback.onApplyMovedObject(result, o);
		}
	}

	public void clearAudioVideoNotes() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || audioNotesTileProvider == null) {
			return;
		}
		audioNotesTileProvider.clearSymbols(mapRenderer);
		audioNotesTileProvider = null;
	}
}