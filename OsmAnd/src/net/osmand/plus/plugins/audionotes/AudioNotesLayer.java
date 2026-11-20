package net.osmand.plus.plugins.audionotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;

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
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.gallery.controller.GalleryPagerController;
import net.osmand.plus.gallery.data.GalleryKey;
import net.osmand.plus.gallery.library.MediaLibraryEntry;
import net.osmand.plus.gallery.library.MediaMapPoints;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.plus.views.layers.MapSelectionRules;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.AudioNotesTileProvider;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.media.domain.MediaType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AudioNotesLayer extends OsmandMapLayer implements
		IContextMenuProvider, ContextMenuLayer.IMoveObjectProvider {

	private static final int START_ZOOM = 10;
	private final Context ctx;
	private final AudioVideoNotesPlugin plugin;
	private ContextMenuLayer contextMenuLayer;
	private MediaMapPoints mediaPoints;
	private volatile boolean mediaPointsChanged;
	private boolean changeMarkerPositionMode;
	private WindowInsetsCompat windowInsets;

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
		if (mediaPoints == null) {
			mediaPoints = new MediaMapPoints(getApplication(), () -> {
				mediaPointsChanged = true;
				view.refreshMap();
			});
		}
		mediaPoints.start();
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		if (mediaPoints != null) {
			mediaPoints.stop();
		}
	}

	@NonNull
	private List<MediaLibraryEntry> getMediaEntries() {
		return mediaPoints != null ? mediaPoints.getEntries() : Collections.emptyList();
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
			List<MediaLibraryEntry> mediaEntries = getMediaEntries();
			int movableObjectsCount = contextMenuLayer.isInChangeMarkerPositionMode()
					&& contextMenuLayer.getMoveableObject() instanceof Recording ? 1 : 0;
			int objectsCount = objects.size() - movableObjectsCount + mediaEntries.size();
			if (audioNotesTileProvider != null && (mediaPointsChanged
					|| objectsCount != audioNotesTileProvider.getPoints31().size())) {
				clearAudioVideoNotes();
			}
			mediaPointsChanged = false;
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
				for (MediaLibraryEntry entry : mediaEntries) {
					KLatLon location = entry.getLocation();
					if (location != null) {
						audioNotesTileProvider.addToData(location.getLatitude(), location.getLongitude(), entry.getType(), textScale);
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
				List<MediaLibraryEntry> fullMedia = new ArrayList<>();
				for (MediaLibraryEntry entry : getMediaEntries()) {
					KLatLon location = entry.getLocation();
					if (location == null || !latlon.contains(location.getLongitude(), location.getLatitude(),
							location.getLongitude(), location.getLatitude())) {
						continue;
					}
					float x = tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
					float y = tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
					LatLon latLon = new LatLon(location.getLatitude(), location.getLongitude());
					if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
						PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(ctx,
								ContextCompat.getColor(ctx, R.color.audio_video_icon_color), true);
						pointImageDrawable.setAlpha(0.8f);
						pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
						smallObjectsLatLon.add(latLon);
					} else {
						fullMedia.add(entry);
						fullObjectsLatLon.add(latLon);
					}
				}
				for (Recording o : fullObjects) {
					float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
					drawRecording(canvas, o, x, y, textScale);
				}
				for (MediaLibraryEntry entry : fullMedia) {
					KLatLon location = entry.getLocation();
					float x = tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
					float y = tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
					PointImageDrawable icon = getMediaIcon(entry.getType());
					icon.setAlpha(0.8f);
					icon.drawPoint(canvas, x, y, textScale, false);
				}
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
	}

	private void drawRecording(@NonNull Canvas canvas, @NonNull Recording recording, float x, float y, float textScale) {
		PointImageDrawable icon = getRecordingIcon(recording);
		icon.setAlpha(0.8f);
		icon.drawPoint(canvas, x, y, textScale, false);
	}

	@NonNull
	private PointImageDrawable getRecordingIcon(@NonNull Recording o) {
		if (o.isPhoto()) {
			return getPhotoNoteIcon();
		} else if (o.isAudio()) {
			return getAudioNoteIcon();
		} else {
			return getVideoNoteIcon();
		}
	}

	@NonNull
	private PointImageDrawable getMediaIcon(@NonNull MediaType type) {
		if (type == MediaType.PHOTO) {
			return getPhotoNoteIcon();
		} else if (type == MediaType.AUDIO) {
			return getAudioNoteIcon();
		} else {
			return getVideoNoteIcon();
		}
	}

	@NonNull
	public PointImageDrawable getAudioNoteIcon() {
		return createPointImageDrawable(R.drawable.mx_special_microphone);
	}

	@NonNull
	public PointImageDrawable getPhotoNoteIcon() {
		return createPointImageDrawable(R.drawable.mx_special_photo_camera);
	}

	@NonNull
	public PointImageDrawable getVideoNoteIcon() {
		return createPointImageDrawable(R.drawable.mx_special_video_camera);
	}

	@NonNull
	private PointImageDrawable createPointImageDrawable(int iconId) {
		int iconColor = ColorUtilities.getColor(ctx, R.color.audio_video_icon_color);
		return PointImageUtils.getOrCreate(ctx, iconColor, true, iconId);
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
	public void setWindowInsets(@NonNull WindowInsetsCompat windowInsets) {
		this.windowInsets = windowInsets;
		AudioVideoNoteRecordingMenu menu = plugin.getRecordingMenu();
		if (menu != null) menu.update();
	}

	@Nullable
	public WindowInsetsCompat getWindowInsets() {
		return windowInsets;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof Recording rec) {
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
		} else if (o instanceof MediaLibraryEntry entry) {
			return new PointDescription(PointDescription.POINT_TYPE_LOCATION, entry.getTitle());
		}
		return null;
	}

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		// Media other than notes opens in the media viewer, as from the Media tab
		MapActivity mapActivity = getMapActivity();
		if (o instanceof MediaLibraryEntry entry && mapActivity != null && mediaPoints != null) {
			GalleryPagerController.show(mapActivity, GalleryKey.MediaLibrary.INSTANCE, entry.getId(),
					mediaPoints.getOrderedIds(), true);
			return true;
		}
		return false;
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result, @NonNull MapSelectionRules rules) {
		PointF point = result.getPoint();
		RotatedTileBox tileBox = result.getTileBox();
		Collection<Recording> recordings = plugin.getAllRecordings();
		List<MediaLibraryEntry> mediaEntries = getMediaEntries();
		if ((Algorithms.isEmpty(recordings) && mediaEntries.isEmpty()) || tileBox.getZoom() < START_ZOOM) {
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
		for (Recording recording : recordings) {
			double lat = recording.getLatitude();
			double lon = recording.getLongitude();

			boolean add = mapRenderer != null
					? NativeUtilities.isPointInsidePolygon(lat, lon, touchPolygon31)
					: tileBox.isLatLonNearPixel(lat, lon, point.x, point.y, radius);
			if (add) {
				result.collect(recording, this);
			}
		}
		for (MediaLibraryEntry entry : mediaEntries) {
			KLatLon location = entry.getLocation();
			if (location == null) {
				continue;
			}
			double lat = location.getLatitude();
			double lon = location.getLongitude();
			boolean add = mapRenderer != null
					? NativeUtilities.isPointInsidePolygon(lat, lon, touchPolygon31)
					: tileBox.isLatLonNearPixel(lat, lon, point.x, point.y, radius);
			if (add) {
				result.collect(entry, this);
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof Recording) {
			return new LatLon(((Recording) o).getLatitude(), ((Recording) o).getLongitude());
		} else if (o instanceof MediaLibraryEntry entry) {
			KLatLon location = entry.getLocation();
			return location != null ? new LatLon(location.getLatitude(), location.getLongitude()) : null;
		}
		return null;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof Recording;
	}

	@Override
	public Object getMoveableObjectIcon(@NonNull Object o) {
		if (o instanceof Recording recording) {
			PointImageDrawable icon = getRecordingIcon(recording);
			icon.setAlpha(0.8f);
			return icon;
		}
		return null;
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