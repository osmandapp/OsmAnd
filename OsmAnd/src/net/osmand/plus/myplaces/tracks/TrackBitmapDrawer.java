package net.osmand.plus.myplaces.tracks;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.JOIN_SEGMENTS;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.Renderable.CurrentTrack;
import net.osmand.plus.views.Renderable.RenderableSegment;
import net.osmand.plus.views.Renderable.StandardTrack;

public class TrackBitmapDrawer extends MapBitmapDrawer {

	private final GPXFile gpxFile;
	private final GpxDataItem dataItem;

	private final Paint paint = new Paint();
	private final Paint paintIcon = new Paint();
	private final Bitmap pointSmall;
	private final Drawable locationIcon;

	private Bitmap trackBitmap;
	private LatLon selectedLocation;

	@ColorInt
	public int defaultTrackColor;
	@ColorInt
	public int currentTrackColor;

	public TrackBitmapDrawer(@NonNull OsmandApplication app, @NonNull MapDrawParams params,
	                         @NonNull GPXFile gpxFile, @Nullable GpxDataItem dataItem) {
		super(app, params);
		this.gpxFile = gpxFile;
		this.dataItem = dataItem;

		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(AndroidUtils.dpToPx(app, 4f));

		currentTrackColor = app.getSettings().CURRENT_TRACK_COLOR.get();
		pointSmall = BitmapFactory.decodeResource(app.getResources(), R.drawable.ic_white_shield_small);
		locationIcon = AppCompatResources.getDrawable(app, R.drawable.map_location_default);

		addListener(new MapBitmapDrawerListener() {
			@Override
			public void onBitmapDrawn(boolean success) {
				if (success) {
					refreshTrackBitmap();
				}
			}
		});
	}

	public void setDefaultTrackColor(@ColorInt int defaultTrackColor) {
		this.defaultTrackColor = defaultTrackColor;
	}

	@Nullable
	public LatLon getSelectedLocation() {
		return selectedLocation;
	}

	public void setSelectedLocation(@Nullable LatLon selectedLocation) {
		this.selectedLocation = selectedLocation;
	}

	public boolean isNonInitialized() {
		return tileBox == null || mapBitmap == null || trackBitmap == null;
	}

	protected void createTileBox() {
		QuadRect rect = gpxFile.getRect();
		tileBox = new RotatedTileBox.RotatedTileBoxBuilder()
				.setLocation(rect.centerY(), rect.centerX())
				.setZoom(15)
				.density(params.density)
				.setMapDensity(params.density)
				.setPixelDimensions(params.widthPixels, params.heightPixels, 0.5f, 0.5f).build();

		while (tileBox.getZoom() < 17 && tileBox.containsLatLon(rect.top, rect.left) && tileBox.containsLatLon(rect.bottom, rect.right)) {
			tileBox.setZoom(tileBox.getZoom() + 1);
		}
		while (tileBox.getZoom() >= 7 && (!tileBox.containsLatLon(rect.top, rect.left) || !tileBox.containsLatLon(rect.bottom, rect.right))) {
			tileBox.setZoom(tileBox.getZoom() - 1);
		}
	}

	public void refreshTrackBitmap() {
		if (mapBitmap != null && !mapBitmap.isRecycled() && isDrawingAllowed()) {
			SelectedGpxFile selectedGpxFile = getSelectedGpxFile();

			Bitmap bitmap = mapBitmap.copy(mapBitmap.getConfig(), true);
			Canvas canvas = new Canvas(bitmap);
			drawTrack(canvas, selectedGpxFile);
			drawPoints(canvas, selectedGpxFile);
			trackBitmap = bitmap;

			Bitmap pointBitmap = drawSelectedPoint();
			for (MapBitmapDrawerListener listener : listeners) {
				if (pointBitmap != null && listener.isBitmapSelectionSupported()) {
					listener.onBitmapDrawn(pointBitmap);
				} else {
					listener.onBitmapDrawn(trackBitmap);
				}
			}
		}
	}

	@NonNull
	private SelectedGpxFile getSelectedGpxFile() {
		if (gpxFile.showCurrentTrack) {
			return app.getSavingTrackHelper().getCurrentTrack();
		} else {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
			if (selectedGpxFile == null) {
				selectedGpxFile = new SelectedGpxFile();
				if (dataItem != null) {
					selectedGpxFile.setJoinSegments(dataItem.getParameter(JOIN_SEGMENTS));
				}
			}
			selectedGpxFile.setGpxFile(gpxFile, app);
			return selectedGpxFile;
		}
	}

	private void drawTrack(@NonNull Canvas canvas, @NonNull SelectedGpxFile selectedGpxFile) {
		GpxDataItem item = !selectedGpxFile.isShowCurrentTrack() ? dataItem : null;
		for (TrkSegment segment : selectedGpxFile.getPointsToDisplay()) {
			int color = getTrackColor(selectedGpxFile, segment, item);
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
			color = segment.getColor(defaultTrackColor);
		}
		if (color == 0) {
			color = gpxFile.getColor(defaultTrackColor);
		}
		return color == 0 ? defaultTrackColor : color;
	}

	private void drawPoints(@NonNull Canvas canvas, @NonNull SelectedGpxFile selectedGpxFile) {
		int color = selectedGpxFile.getColor();
		int pointsColor = color == 0 ? ContextCompat.getColor(app, R.color.gpx_color_point) : color;
		for (WptPt point : selectedGpxFile.getGpxFile().getPoints()) {
			float x = tileBox.getPixXFromLatLon(point.lat, point.lon);
			float y = tileBox.getPixYFromLatLon(point.lat, point.lon);

			int pointColor = point.getColor(pointsColor) | 0xff000000;
			paintIcon.setColorFilter(new PorterDuffColorFilter(pointColor, PorterDuff.Mode.MULTIPLY));
			canvas.drawBitmap(pointSmall, x - pointSmall.getWidth() / 2f, y - pointSmall.getHeight() / 2f, paintIcon);
		}
	}

	@Nullable
	private Bitmap drawSelectedPoint() {
		if (trackBitmap != null && tileBox != null && selectedLocation != null) {
			paintIcon.setColorFilter(null);

			float x = tileBox.getPixXFromLatLon(selectedLocation.getLatitude(), selectedLocation.getLongitude());
			float y = tileBox.getPixYFromLatLon(selectedLocation.getLatitude(), selectedLocation.getLongitude());

			Bitmap bitmap = trackBitmap.copy(trackBitmap.getConfig(), true);
			locationIcon.setBounds((int) x - locationIcon.getIntrinsicWidth() / 2,
					(int) y - locationIcon.getIntrinsicHeight() / 2,
					(int) x + locationIcon.getIntrinsicWidth() / 2,
					(int) y + locationIcon.getIntrinsicHeight() / 2);
			locationIcon.draw(new Canvas(bitmap));
			return bitmap;
		}
		return null;
	}

	public void updateSelectedPoint(double lat, double lon) {
		selectedLocation = new LatLon(lat, lon);
		Bitmap bitmap = drawSelectedPoint();
		for (MapBitmapDrawerListener listener : listeners) {
			if (bitmap != null && listener.isBitmapSelectionSupported() && isDrawingAllowed()) {
				listener.onBitmapDrawn(bitmap);
			}
		}
	}
}