package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;

import java.util.ArrayList;
import java.util.List;

public class FavoritesLayer  extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider,
	ContextMenuLayer.IMoveObjectProvider, MapTextProvider<FavouritePoint> {

	protected int startZoom = 6;
	
	protected OsmandMapTileView view;
	private Paint paint;
	private FavouritesDbHelper favorites;
	protected List<FavouritePoint> cache = new ArrayList<>();
	private MapTextLayer textLayer;
	private ContextMenuLayer contextMenuLayer;
	private Paint paintIcon;
	private Bitmap pointSmall;
	private int defaultColor;

	private OsmandSettings settings;


	
	protected String getObjName() {
		return view.getContext().getString(R.string.favorite);
	}
	
	protected List<? extends FavouritePoint> getPoints() {
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
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
		paintIcon = new Paint();
		pointSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_white_shield_small);
		defaultColor = ContextCompat.getColor(view.getContext(), R.color.color_favorite);
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
		if (contextMenuLayer.getMoveableObject() instanceof FavouritePoint) {
			FavouritePoint fp = (FavouritePoint) contextMenuLayer.getMoveableObject();
			FavoriteImageDrawable fid = FavoriteImageDrawable.getOrCreate(view.getContext(), fp.getColor(), true);
			PointF pf = contextMenuLayer.getMoveableCenterPoint(tileBox);
			fid.drawBitmapInCenter(canvas, pf.x, pf.y);
		}
	}
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		cache.clear();
		if (this.settings.SHOW_FAVORITES.get()) {
			if (tileBox.getZoom() >= startZoom) {
				float iconSize = FavoriteImageDrawable.getOrCreate(view.getContext(), 0,
						 true).getIntrinsicWidth() * 3 / 2.5f;
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

				// request to load
				final QuadRect latLonBounds = tileBox.getLatLonBounds();
				List<FavouritePoint> fullObjects = new ArrayList<>();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				for (FavouritePoint o : getPoints()) {
					if (!o.isVisible()) {
						continue;
					}
					float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());

					if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
						@ColorInt
						int col = o.getColor() == 0 || o.getColor() == Color.BLACK ? defaultColor : o.getColor();
						paintIcon.setColorFilter(new PorterDuffColorFilter(col, PorterDuff.Mode.MULTIPLY));
						canvas.drawBitmap(pointSmall, x - pointSmall.getWidth() / 2, y - pointSmall.getHeight() / 2, paintIcon);
						smallObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
					} else {
						fullObjects.add(o);
						fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
					}
				}
				for (FavouritePoint o : fullObjects) {
					if (o != contextMenuLayer.getMoveableObject()) {
						drawPoint(canvas, tileBox, latLonBounds, o);
					}
				}
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
		if(textLayer.isVisible()) {
			textLayer.putData(this, cache);
		}

	}


	private void drawPoint(Canvas canvas, RotatedTileBox tileBox, final QuadRect latLonBounds, FavouritePoint o) {
		if (o.isVisible() && o.getLatitude() >= latLonBounds.bottom && o.getLatitude() <= latLonBounds.top  && o.getLongitude() >= latLonBounds.left
				&& o.getLongitude() <= latLonBounds.right ) {
			cache.add(o);
			int x = (int) tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
			FavoriteImageDrawable fid = FavoriteImageDrawable.getOrCreate(view.getContext(), o.getColor(), true);
			fid.drawBitmapInCenter(canvas, x, y);
		}
	}
	
	
	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	public void getFavoriteFromPoint(RotatedTileBox tb, PointF point, List<? super FavouritePoint> res) {
		int r = (int) (15 * tb.getDensity());
		int ex = (int) point.x;
		int ey = (int) point.y;
		for (FavouritePoint n : getPoints()) {
			getFavFromPoint(tb, res, r, ex, ey, n);
		}
	}

	private void getFavFromPoint(RotatedTileBox tb, List<? super FavouritePoint> res, int r, int ex, int ey,
			FavouritePoint n) {
		if (n.isVisible()) { 
			int x = (int) tb.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
			int y = (int) tb.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
			if (calculateBelongs(ex, ey, x, y, r)) {
				res.add(n);
			}
		}
	}

	
	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof FavouritePoint){
			return ((FavouritePoint) o).getPointDescription(view.getContext()); //$NON-NLS-1$
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
	public boolean isObjectClickable(Object o) {
		return o instanceof FavouritePoint && o != contextMenuLayer.getMoveableObject();
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res) {
		if (this.settings.SHOW_FAVORITES.get() && tileBox.getZoom() >= startZoom) {
			getFavoriteFromPoint(tileBox, point, res);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof FavouritePoint){
			return new LatLon(((FavouritePoint)o).getLatitude(), ((FavouritePoint)o).getLongitude());
		}
		return null;
	}

	@Override
	public LatLon getTextLocation(FavouritePoint o) {
		return new LatLon(o.getLatitude(), o.getLongitude());
	}

	@Override
	public int getTextShift(FavouritePoint o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity());
	}

	@Override
	public String getText(FavouritePoint o) {
		return PointDescription.getSimpleName(o, view.getContext());
	}

	
	@Override
	public boolean isObjectMoveable(Object o) {
		return o instanceof FavouritePoint;
	}

	@Override
	public boolean applyNewObjectPosition(Object o, LatLon position) {
		if(o instanceof FavouritePoint) {
			favorites.editFavourite((FavouritePoint) o, position.getLatitude(), position.getLongitude());			
			return true;
		}
		return false;
	}
}


