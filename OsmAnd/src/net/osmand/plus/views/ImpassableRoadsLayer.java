package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.Map;

/**
 * Created by Denis on
 * 20.03.2015.
 */
public class ImpassableRoadsLayer extends OsmandMapLayer {

	private static final int startZoom = 10;
	private final MapActivity activity;
	private Bitmap roadWorkIcon;
	private OsmandMapTileView view;
	private Paint paint;
	private Map<Long, Location> missingRoads;

	public ImpassableRoadsLayer(MapActivity activity) {
		this.activity = activity;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		roadWorkIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.ic_action_road_works_dark);
		paint = new Paint();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			for (long id : getMissingRoads().keySet()) {
				Location location = getMissingRoads().get(id);
				float x = tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
				float y = tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
				float left = x - roadWorkIcon.getWidth() / 2;
				float top = y - roadWorkIcon.getHeight() / 2;
				canvas.drawBitmap(roadWorkIcon, left, top, paint);
			}
		}
	}

	public Map<Long, Location> getMissingRoads() {
		if(missingRoads == null) {
			missingRoads = activity.getMyApplication().getDefaultRoutingConfig().getImpassableRoadLocations();
		}
		return missingRoads;
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
}
