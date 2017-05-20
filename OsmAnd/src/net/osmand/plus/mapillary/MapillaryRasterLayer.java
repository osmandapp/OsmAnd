package net.osmand.plus.mapillary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.R;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;

class MapillaryRasterLayer extends MapTileLayer implements MapillaryLayer {

	private LatLon selectedImageLocation;
	private Float selectedImageCameraAngle;
	private Bitmap selectedImage;
	private Bitmap headingImage;
	private Paint paintIcon;

	MapillaryRasterLayer() {
		super(false);
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		super.initLayer(view);
		paintIcon = new Paint();
		selectedImage = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_mapillary_location);
		headingImage = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_mapillary_location_view_angle);
	}

	@Override
	public void setSelectedImageLocation(LatLon selectedImageLocation) {
		this.selectedImageLocation = selectedImageLocation;
	}

	@Override
	public void setSelectedImageCameraAngle(Float selectedImageCameraAngle) {
		this.selectedImageCameraAngle = selectedImageCameraAngle;
	}

	private void drawSelectedPoint(Canvas canvas, RotatedTileBox tileBox) {
		if (selectedImageLocation != null) {
			canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			float x = tileBox.getPixXFromLatLon(selectedImageLocation.getLatitude(), selectedImageLocation.getLongitude());
			float y = tileBox.getPixYFromLatLon(selectedImageLocation.getLatitude(), selectedImageLocation.getLongitude());
			if (selectedImageCameraAngle != null) {
				canvas.save();
				canvas.rotate(selectedImageCameraAngle - 180, x, y);
				canvas.drawBitmap(headingImage, x - headingImage.getWidth() / 2,
						y - headingImage.getHeight() / 2, paintIcon);
				canvas.restore();
			}
			canvas.drawBitmap(selectedImage, x - selectedImage.getWidth() / 2, y - selectedImage.getHeight() / 2, paintIcon);
			canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		}
	}

	@Override
	public void drawTileMap(Canvas canvas, RotatedTileBox tileBox) {
		ITileSource map = this.map;
		if (map == null) {
			return;
		}
		int maxZoom = map.getMaximumZoomSupported();
		if (tileBox.getZoom() > maxZoom) {
			return;
		}
		super.drawTileMap(canvas, tileBox);
		drawSelectedPoint(canvas, tileBox);
	}
}
