package net.osmand.plus.mapillary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

public class MapillaryLayer extends MapTileLayer {

	private LatLon selectedImageLocation;
	private Float selectedImageCameraAngle;
	private Bitmap selectedImage;
	private Bitmap headingImage;
	private Paint paintIcon;

	public MapillaryLayer() {
		super(false);
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		super.initLayer(view);
		paintIcon = new Paint();
		selectedImage = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_default_location);
		headingImage = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pedestrian_location_view_angle);
	}

	public LatLon getSelectedImageLocation() {
		return selectedImageLocation;
	}

	public void setSelectedImageLocation(LatLon selectedImageLocation) {
		this.selectedImageLocation = selectedImageLocation;
	}

	public Float getSelectedImageCameraAngle() {
		return selectedImageCameraAngle;
	}

	public void setSelectedImageCameraAngle(Float selectedImageCameraAngle) {
		this.selectedImageCameraAngle = selectedImageCameraAngle;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		super.onPrepareBufferImage(canvas, tileBox, drawSettings);
		if (selectedImageLocation != null) {
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
		}
	}
}
