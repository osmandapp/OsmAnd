package net.osmand.plus.views;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.widget.ArrayAdapter;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavoritesTreeFragment;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;

import java.util.ArrayList;
import java.util.List;

public class FavoritesLayer  extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider, MapTextProvider<LocationPoint> {

	protected int startZoom = 6;
	
	protected OsmandMapTileView view;
	private Paint paint;
	private FavouritesDbHelper favorites;
	protected List<LocationPoint> cache = new ArrayList<LocationPoint>();
	private MapTextLayer textLayer;

	private OsmandSettings settings;
//	private Bitmap d;

	
	protected Class<? extends LocationPoint> getFavoriteClass() {
		return (Class<? extends LocationPoint>) FavouritePoint.class;
	}
	
	protected String getObjName() {
		return view.getContext().getString(R.string.favorite);
	}
	
	protected List<? extends LocationPoint> getPoints() {
		return favorites.getFavouritePoints();
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		settings = view.getApplication().getSettings();
		favorites = view.getApplication().getFavorites();
		textLayer = view.getLayerByClass(MapTextLayer.class);
//		favoriteIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_favourite);
		
	}
	
	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return (Math.abs(objx - ex) <= radius * 2 && Math.abs(objy - ey) <= radius * 2) ;
//		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
		//return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
	}

	@Override
	public void destroyLayer() {
		
	}
	

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		cache.clear();
		if (this.settings.SHOW_FAVORITES.get()) {
			if (tileBox.getZoom() >= startZoom) {
				// request to load
				final QuadRect latLonBounds = tileBox.getLatLonBounds();
				for (LocationPoint o : getPoints()) {
					drawPoint(canvas, tileBox, latLonBounds, o);
				}

			}
		}
		if(textLayer.isVisible()) {
			textLayer.putData(this, cache);
		}

	}


	private void drawPoint(Canvas canvas, RotatedTileBox tileBox, final QuadRect latLonBounds, LocationPoint o) {
		if (o.isVisible() && o.getLatitude() >= latLonBounds.bottom && o.getLatitude() <= latLonBounds.top  && o.getLongitude() >= latLonBounds.left
				&& o.getLongitude() <= latLonBounds.right ) {
			cache.add(o);
			int x = (int) tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
			FavoriteImageDrawable fid = FavoriteImageDrawable.getOrCreate(view.getContext(), o.getColor(), tileBox.getDensity());
			fid.drawBitmapInCenter(canvas, x, y);
//					canvas.drawBitmap(favoriteIcon, x - favoriteIcon.getWidth() / 2, 
//							y - favoriteIcon.getHeight(), paint);
		}
	}
	
	
	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	public void getFavoriteFromPoint(RotatedTileBox tb, PointF point, List<? super LocationPoint> res) {
		int r = (int) (15 * tb.getDensity());
		int ex = (int) point.x;
		int ey = (int) point.y;
		for (LocationPoint n : getPoints()) {
			getFavFromPoint(tb, res, r, ex, ey, n);
		}
	}

	private void getFavFromPoint(RotatedTileBox tb, List<? super LocationPoint> res, int r, int ex, int ey,
			LocationPoint n) {
		if (n.isVisible()) { 
			int x = (int) tb.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
			int y = (int) tb.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
			if (calculateBelongs(ex, ey, x, y, r)) {
				res.add(n);
			}
		}
	}

	@Override
	public String getObjectDescription(Object o) {
		Class<? extends LocationPoint> fcl = getFavoriteClass();
		if(o!= null && fcl.isInstance(o)) {
			return PointDescription.getSimpleName((LocationPoint) o, view.getContext()) ;
		}
		return null;
	}

	
	
	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof LocationPoint){
			return ((LocationPoint) o).getPointDescription(view.getContext()); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res) {
		getFavoriteFromPoint(tileBox, point, res);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof LocationPoint){
			return new LatLon(((LocationPoint)o).getLatitude(), ((LocationPoint)o).getLongitude());
		}
		return null;
	}

	@Override
	public LatLon getTextLocation(LocationPoint o) {
		return new LatLon(o.getLatitude(), o.getLongitude());
	}

	@Override
	public int getTextShift(LocationPoint o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity());
	}

	@Override
	public String getText(LocationPoint o) {
		return PointDescription.getSimpleName(o, view.getContext());
	}
	

}


