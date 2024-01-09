package net.osmand.plus.myplaces.tracks;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.JOIN_SEGMENTS;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.RotatedTileBox.RotatedTileBoxBuilder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.Renderable.CurrentTrack;
import net.osmand.plus.views.Renderable.RenderableSegment;
import net.osmand.plus.views.Renderable.StandardTrack;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class TrackBitmapDrawer {

	private static final Log log = PlatformUtil.getLog(TrackBitmapDrawer.class);

	private final OsmandApplication app;

	private final TracksDrawParams drawParams;

	private boolean drawEnabled;
	private RotatedTileBox rotatedTileBox;
	private Bitmap mapBitmap;
	private Bitmap mapTrackBitmap;

	private final GPXFile gpxFile;
	private final GpxDataItem gpxDataItem;
	private LatLon selectedPointLatLon;

	private final Paint paint = new Paint();
	private final Paint paintIcon = new Paint();
	private final Bitmap pointSmall;
	private final LayerDrawable selectedPoint;

	private int currentTrackColor;
	private final int defPointColor;

	private final List<TrackBitmapDrawerListener> listeners = new ArrayList<>();

	public interface TrackBitmapDrawerListener {
		void onTrackBitmapDrawing();

		void onTrackBitmapDrawn(boolean success);

		boolean isTrackBitmapSelectionSupported();

		void drawTrackBitmap(Bitmap bitmap);
	}

	public TrackBitmapDrawer(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                         @NonNull TracksDrawParams drawParams, @Nullable GpxDataItem gpxDataItem) {
		this.app = app;
		this.gpxFile = gpxFile;
		this.drawParams = drawParams;
		this.gpxDataItem = gpxDataItem;

		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(AndroidUtils.dpToPx(app, 4f));

		defPointColor = ContextCompat.getColor(app, R.color.gpx_color_point);
		pointSmall = BitmapFactory.decodeResource(app.getResources(), R.drawable.ic_white_shield_small);
		selectedPoint = (LayerDrawable) AppCompatResources.getDrawable(app, R.drawable.map_location_default);
	}

	public void addListener(TrackBitmapDrawerListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeListener(TrackBitmapDrawerListener l) {
		listeners.remove(l);
	}

	public void clearListeners() {
		listeners.clear();
	}

	public void notifyDrawing() {
		for (TrackBitmapDrawerListener l : listeners) {
			l.onTrackBitmapDrawing();
		}
	}

	public void notifyDrawn(boolean success) {
		for (TrackBitmapDrawerListener l : listeners) {
			l.onTrackBitmapDrawn(success);
		}
	}

	public boolean isDrawEnabled() {
		return drawEnabled;
	}

	public void setDrawEnabled(boolean drawEnabled) {
		this.drawEnabled = drawEnabled;
	}

	public GPXFile getGpx() {
		return gpxFile;
	}

	public GpxDataItem getGpxDataItem() {
		return gpxDataItem;
	}

	public LatLon getSelectedPointLatLon() {
		return selectedPointLatLon;
	}

	public void setSelectedPointLatLon(LatLon selectedPointLatLon) {
		this.selectedPointLatLon = selectedPointLatLon;
	}

	public boolean isNonInitialized() {
		return rotatedTileBox == null || mapBitmap == null || mapTrackBitmap == null;
	}

	public boolean initAndDraw() {
		QuadRect rect = gpxFile.getRect();
		if (rect != null && rect.left != 0 && rect.top != 0) {
			notifyDrawing();

			double clat = rect.bottom / 2 + rect.top / 2;
			double clon = rect.left / 2 + rect.right / 2;
			RotatedTileBoxBuilder boxBuilder = new RotatedTileBoxBuilder()
					.setLocation(clat, clon)
					.setZoom(15)
					.density(drawParams.density)
					.setMapDensity(drawParams.density)
					.setPixelDimensions(drawParams.widthPixels, drawParams.heightPixels, 0.5f, 0.5f);

			rotatedTileBox = boxBuilder.build();
			while (rotatedTileBox.getZoom() < 17 && rotatedTileBox.containsLatLon(rect.top, rect.left) && rotatedTileBox.containsLatLon(rect.bottom, rect.right)) {
				rotatedTileBox.setZoom(rotatedTileBox.getZoom() + 1);
			}
			while (rotatedTileBox.getZoom() >= 7 && (!rotatedTileBox.containsLatLon(rect.top, rect.left) || !rotatedTileBox.containsLatLon(rect.bottom, rect.right))) {
				rotatedTileBox.setZoom(rotatedTileBox.getZoom() - 1);
			}

			DrawSettings drawSettings = new DrawSettings(!app.getSettings().isLightContent(), true);
			ResourceManager resourceManager = app.getResourceManager();
			MapRenderRepositories renderer = resourceManager.getRenderer();
			if (resourceManager.updateRenderedMapNeeded(rotatedTileBox, drawSettings)) {
				resourceManager.updateRendererMap(rotatedTileBox, interrupted -> app.runInUIThread(() -> {
					if (isDrawEnabled()) {
						mapBitmap = renderer.getBitmap();
						boolean success = mapBitmap != null;
						notifyDrawn(success);

						if (success) {
							refreshTrackBitmap();
						}
					}
				}), true);
			}
			return true;
		}
		return false;
	}

	public void refreshTrackBitmap() {
		currentTrackColor = app.getSettings().CURRENT_TRACK_COLOR.get();
		if (mapBitmap != null && !mapBitmap.isRecycled() && isDrawEnabled()) {
			SelectedGpxFile sf;
			GPXFile gpxFile = getGpx();
			if (gpxFile != null) {
				if (gpxFile.showCurrentTrack) {
					sf = app.getSavingTrackHelper().getCurrentTrack();
				} else {
					sf = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
					if (sf == null) {
						sf = new SelectedGpxFile();
						GpxDataItem gpxDataItem = getGpxDataItem();
						if (gpxDataItem != null) {
							sf.setJoinSegments(gpxDataItem.getParameter(JOIN_SEGMENTS));
						}
					}
					sf.setGpxFile(gpxFile, app);
				}
				Bitmap bmp = mapBitmap.copy(mapBitmap.getConfig(), true);
				Canvas canvas = new Canvas(bmp);
				drawTrack(canvas, rotatedTileBox, sf);
				drawPoints(canvas, rotatedTileBox, sf);
				mapTrackBitmap = bmp;
				Bitmap selectedPointBitmap = drawSelectedPoint();
				for (TrackBitmapDrawerListener l : listeners) {
					if (selectedPointBitmap != null && l.isTrackBitmapSelectionSupported()) {
						l.drawTrackBitmap(selectedPointBitmap);
					} else {
						l.drawTrackBitmap(mapTrackBitmap);
					}
				}
			}
		}
	}

	private void drawTrack(Canvas canvas, RotatedTileBox tileBox, SelectedGpxFile selectedGpxFile) {
		GpxDataItem gpxDataItem = null;
		if (!selectedGpxFile.isShowCurrentTrack()) {
			gpxDataItem = getGpxDataItem();
		}
		List<TrkSegment> segments = selectedGpxFile.getPointsToDisplay();
		for (TrkSegment segment : segments) {
			int color = getTrackColor(selectedGpxFile, segment, gpxDataItem);
			if (segment.renderer == null && !segment.points.isEmpty()) {
				if (selectedGpxFile.isShowCurrentTrack()) {
					segment.renderer = new CurrentTrack(segment.points);
				} else {
					segment.renderer = new StandardTrack(segment.points, 17.2);
				}
			}
			paint.setColor(color);
			if (segment.renderer instanceof RenderableSegment) {
				((RenderableSegment) segment.renderer).drawSegment(tileBox.getZoom(), paint, canvas, tileBox);
			}
		}
	}

	private int getTrackColor(@NonNull SelectedGpxFile selectedGpxFile, @NonNull TrkSegment segment, @Nullable GpxDataItem dataItem) {
		int color = dataItem != null ? dataItem.getParameter(COLOR) : 0;
		if (selectedGpxFile.isShowCurrentTrack()) {
			color = currentTrackColor;
		}
		if (color == 0) {
			color = segment.getColor(drawParams.trackColor);
		}
		if (color == 0) {
			color = gpxFile.getColor(drawParams.trackColor);
		}
		return color == 0 ? drawParams.trackColor : color;
	}

	private void drawPoints(Canvas canvas, RotatedTileBox tileBox, SelectedGpxFile g) {
		List<WptPt> pts = g.getGpxFile().getPoints();
		@ColorInt
		int fileColor = g.getColor() == 0 ? defPointColor : g.getColor();
		for (WptPt o : pts) {
			float x = tileBox.getPixXFromLatLon(o.lat, o.lon);
			float y = tileBox.getPixYFromLatLon(o.lat, o.lon);

			int pointColor = o.getColor(fileColor) | 0xff000000;
			paintIcon.setColorFilter(new PorterDuffColorFilter(pointColor, PorterDuff.Mode.MULTIPLY));
			canvas.drawBitmap(pointSmall, x - pointSmall.getWidth() / 2, y - pointSmall.getHeight() / 2, paintIcon);
		}
	}

	private Bitmap drawSelectedPoint() {
		if (mapTrackBitmap != null && rotatedTileBox != null && selectedPointLatLon != null) {
			float x = rotatedTileBox.getPixXFromLatLon(selectedPointLatLon.getLatitude(), selectedPointLatLon.getLongitude());
			float y = rotatedTileBox.getPixYFromLatLon(selectedPointLatLon.getLatitude(), selectedPointLatLon.getLongitude());
			paintIcon.setColorFilter(null);
			Bitmap bmp = mapTrackBitmap.copy(mapTrackBitmap.getConfig(), true);
			Canvas canvas = new Canvas(bmp);
			selectedPoint.setBounds((int) x - selectedPoint.getIntrinsicWidth() / 2,
					(int) y - selectedPoint.getIntrinsicHeight() / 2,
					(int) x + selectedPoint.getIntrinsicWidth() / 2,
					(int) y + selectedPoint.getIntrinsicHeight() / 2);
			selectedPoint.draw(canvas);
			return bmp;
		} else {
			return null;
		}
	}

	public void updateSelectedPoint(double lat, double lon) {
		selectedPointLatLon = new LatLon(lat, lon);
		Bitmap bmp = drawSelectedPoint();
		for (TrackBitmapDrawerListener l : listeners) {
			if (bmp != null && l.isTrackBitmapSelectionSupported() && isDrawEnabled()) {
				l.drawTrackBitmap(bmp);
			}
		}
	}

	public static class TracksDrawParams {

		private final float density;
		private final int widthPixels;
		private final int heightPixels;
		private final int trackColor;

		public TracksDrawParams(float density, int widthPixels, int heightPixels, int trackColor) {
			this.density = density;
			this.widthPixels = widthPixels;
			this.heightPixels = heightPixels;
			this.trackColor = trackColor;
		}
	}
}
