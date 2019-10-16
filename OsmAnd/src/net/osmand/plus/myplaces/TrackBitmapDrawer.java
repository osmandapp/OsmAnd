package net.osmand.plus.myplaces;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.AsyncLoadingThread.OnMapLoadedListener;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.Renderable;

import java.util.ArrayList;
import java.util.List;

public class TrackBitmapDrawer {

	private OsmandApplication app;
	private QuadRect rect;
	private float density;
	private int widthPixels;
	private int heightPixels;

	private boolean drawEnabled;
	private RotatedTileBox rotatedTileBox;
	private Bitmap mapBitmap;
	private Bitmap mapTrackBitmap;

	private GPXFile gpxFile;
	private GpxDataItem gpxDataItem;
	private LatLon selectedPointLatLon;

	private int trackColor;
	private int currentTrackColor;
	private Paint paint;
	private Bitmap selectedPoint;
	private int defPointColor;
	private Paint paintIcon;
	private Bitmap pointSmall;

	private List<TrackBitmapDrawerListener> listeners = new ArrayList<>();

	public interface TrackBitmapDrawerListener {
		void onTrackBitmapDrawing();
		void onTrackBitmapDrawn();

		boolean isTrackBitmapSelectionSupported();
		void drawTrackBitmap(Bitmap bitmap);
	}

	public TrackBitmapDrawer(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                         @Nullable GpxDataItem gpxDataItem, @NonNull QuadRect rect, float density, int widthPixels, int heightPixels) {
		this.density = density;
		this.widthPixels = widthPixels;
		this.heightPixels = heightPixels;
		this.rect = rect;
		this.app = app;
		this.gpxFile = gpxFile;
		this.gpxDataItem = gpxDataItem;

		paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(AndroidUtils.dpToPx(app, 4f));
		defPointColor = ContextCompat.getColor(app, R.color.gpx_color_point);
		paintIcon = new Paint();
		pointSmall = BitmapFactory.decodeResource(app.getResources(), R.drawable.map_white_shield_small);
		selectedPoint = BitmapFactory.decodeResource(app.getResources(), R.drawable.map_default_location);
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

	public void notifyDrawn() {
		for (TrackBitmapDrawerListener l : listeners) {
			l.onTrackBitmapDrawn();
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

	public int getTrackColor() {
		return trackColor;
	}

	public void setTrackColor(int trackColor) {
		this.trackColor = trackColor;
	}

	public boolean isNonInitialized() {
		return rotatedTileBox == null || mapBitmap == null || mapTrackBitmap == null;
	}

	public boolean initAndDraw() {
		if (rect != null && rect.left != 0 && rect.top != 0) {
			notifyDrawing();

			double clat = rect.bottom / 2 + rect.top / 2;
			double clon = rect.left / 2 + rect.right / 2;
			RotatedTileBox.RotatedTileBoxBuilder boxBuilder = new RotatedTileBox.RotatedTileBoxBuilder()
					.setLocation(clat, clon)
					.setZoom(15)
					.density(density)
					.setMapDensity(density)
					.setPixelDimensions(widthPixels, heightPixels, 0.5f, 0.5f);

			rotatedTileBox = boxBuilder.build();
			while (rotatedTileBox.getZoom() < 17 && rotatedTileBox.containsLatLon(rect.top, rect.left) && rotatedTileBox.containsLatLon(rect.bottom, rect.right)) {
				rotatedTileBox.setZoom(rotatedTileBox.getZoom() + 1);
			}
			while (rotatedTileBox.getZoom() >= 7 && (!rotatedTileBox.containsLatLon(rect.top, rect.left) || !rotatedTileBox.containsLatLon(rect.bottom, rect.right))) {
				rotatedTileBox.setZoom(rotatedTileBox.getZoom() - 1);
			}

			final DrawSettings drawSettings = new DrawSettings(!app.getSettings().isLightContent(), true);
			final ResourceManager resourceManager = app.getResourceManager();
			final MapRenderRepositories renderer = resourceManager.getRenderer();
			if (resourceManager.updateRenderedMapNeeded(rotatedTileBox, drawSettings)) {
				resourceManager.updateRendererMap(rotatedTileBox, new OnMapLoadedListener() {
					@Override
					public void onMapLoaded(boolean interrupted) {
						app.runInUIThread(new Runnable() {
							@Override
							public void run() {
								if (isDrawEnabled()) {
									mapBitmap = renderer.getBitmap();
									if (mapBitmap != null) {
										notifyDrawn();
										refreshTrackBitmap();
									}
								}
							}
						});
					}
				});
			}
			return true;
		}
		return false;
	}

	public void refreshTrackBitmap() {
		currentTrackColor = app.getSettings().CURRENT_TRACK_COLOR.get();
		if (mapBitmap != null && isDrawEnabled()) {
			GpxSelectionHelper.SelectedGpxFile sf;
			GPXFile gpxFile = getGpx();
			if (gpxFile != null) {
				if (gpxFile.showCurrentTrack) {
					sf = app.getSavingTrackHelper().getCurrentTrack();
				} else {
					sf = new GpxSelectionHelper.SelectedGpxFile();
					sf.setGpxFile(gpxFile);
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

	private void drawTrack(Canvas canvas, RotatedTileBox tileBox, GpxSelectionHelper.SelectedGpxFile g) {
		GpxDataItem gpxDataItem = null;
		if (!g.isShowCurrentTrack()) {
			gpxDataItem = getGpxDataItem();
		}
		List<TrkSegment> segments = g.getPointsToDisplay();
		for (TrkSegment ts : segments) {
			int color = gpxDataItem != null ? gpxDataItem.getColor() : 0;
			if (g.isShowCurrentTrack()) {
				color = currentTrackColor;
			}
			if (color == 0) {
				color = ts.getColor(trackColor);
			}
			if (ts.renderer == null && !ts.points.isEmpty()) {
				if (g.isShowCurrentTrack()) {
					ts.renderer = new Renderable.CurrentTrack(ts.points);
				} else {
					ts.renderer = new Renderable.StandardTrack(ts.points, 17.2);
				}
			}
			paint.setColor(color == 0 ? trackColor : color);
			if (ts.renderer instanceof Renderable.RenderableSegment) {
				((Renderable.RenderableSegment) ts.renderer).drawSegment(tileBox.getZoom(), paint, canvas, tileBox);
			}
		}
	}

	private void drawPoints(Canvas canvas, RotatedTileBox tileBox, GpxSelectionHelper.SelectedGpxFile g) {
		List<GPXUtilities.WptPt> pts = g.getGpxFile().getPoints();
		@ColorInt
		int fileColor = g.getColor() == 0 ? defPointColor : g.getColor();
		for (GPXUtilities.WptPt o : pts) {
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
			canvas.drawBitmap(selectedPoint, x - selectedPoint.getWidth() / 2, y - selectedPoint.getHeight() / 2, paintIcon);
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
}
