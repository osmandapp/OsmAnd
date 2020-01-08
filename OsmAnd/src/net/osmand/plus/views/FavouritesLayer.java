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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Pair;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.views.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;

import java.util.ArrayList;
import java.util.List;

public class FavouritesLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider,
	ContextMenuLayer.IMoveObjectProvider, MapTextProvider<FavouritePoint> {

	protected int startZoom = 6;
	
	protected OsmandMapTileView view;
	private Paint paint;
	private FavouritesDbHelper favorites;
	private MapMarkersHelper mapMarkersHelper;
	protected List<FavouritePoint> cache = new ArrayList<>();
	private MapTextLayer textLayer;
	private Paint paintIcon;
	private Bitmap pointSmall;
	@ColorInt
	private int defaultColor;
	@ColorInt
	private int grayColor;

	private OsmandSettings settings;

	private ContextMenuLayer contextMenuLayer;
	
	protected String getObjName() {
		return view.getContext().getString(R.string.favorite);
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
		mapMarkersHelper = view.getApplication().getMapMarkersHelper();
		textLayer = view.getLayerByClass(MapTextLayer.class);
		paintIcon = new Paint();
		pointSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_white_shield_small);
		defaultColor = ContextCompat.getColor(view.getContext(), R.color.color_favorite);
		grayColor = ContextCompat.getColor(view.getContext(), R.color.color_favorite_gray);
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}
	
	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return (Math.abs(objx - ex) <= radius * 1.5 && Math.abs(objy - ey) <= radius * 1.5) ;
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
			FavouritePoint objectInMotion = (FavouritePoint) contextMenuLayer.getMoveableObject();
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			MapMarker mapMarker = mapMarkersHelper.getMapMarker(objectInMotion);
			drawBigPoint(canvas, objectInMotion, pf.x, pf.y, mapMarker);
		}
	}
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		cache.clear();
		if (this.settings.SHOW_FAVORITES.get() && favorites.isFavoritesLoaded()) {
			if (tileBox.getZoom() >= startZoom) {
				float iconSize = FavoriteImageDrawable.getOrCreate(view.getContext(), 0,
						 true, (FavouritePoint) null).getIntrinsicWidth() * 3 / 2.5f;
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

				// request to load
				final QuadRect latLonBounds = tileBox.getLatLonBounds();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				for (FavoriteGroup group : favorites.getFavoriteGroups()) {
					List<Pair<FavouritePoint, MapMarker>> fullObjects = new ArrayList<>();
					boolean synced = mapMarkersHelper.getMarkersGroup(group) != null;
					for (FavouritePoint o : group.getPoints()) {
						double lat = o.getLatitude();
						double lon = o.getLongitude();
						if (o.isVisible() && o != contextMenuLayer.getMoveableObject()
								&& lat >= latLonBounds.bottom && lat <= latLonBounds.top
								&& lon >= latLonBounds.left && lon <= latLonBounds.right) {
							MapMarker marker = null;
							if (synced) {
								if ((marker = mapMarkersHelper.getMapMarker(o)) == null) {
									continue;
								}
							}
							cache.add(o);
							float x = tileBox.getPixXFromLatLon(lat, lon);
							float y = tileBox.getPixYFromLatLon(lat, lon);

							if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
								@ColorInt
								int color;
								if (marker != null && marker.history) {
									color = grayColor;
								} else {
									color = o.getColor() == 0 || o.getColor() == Color.BLACK ? defaultColor : o.getColor();
								}
								paintIcon.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
								canvas.drawBitmap(pointSmall, x - pointSmall.getWidth() / 2, y - pointSmall.getHeight() / 2, paintIcon);
								smallObjectsLatLon.add(new LatLon(lat, lon));
							} else {
								fullObjects.add(new Pair<>(o, marker));
								fullObjectsLatLon.add(new LatLon(lat, lon));
							}
						}
					}
					for (Pair<FavouritePoint, MapMarker> pair : fullObjects) {
						FavouritePoint o = pair.first;
						float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
						float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
						drawBigPoint(canvas, o, x, y, pair.second);
					}
				}
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
		if(isTextVisible()) {
			textLayer.putData(this, cache);
		}

	}

	private void drawBigPoint(Canvas canvas, FavouritePoint o, float x, float y, @Nullable MapMarker marker) {
		FavoriteImageDrawable fid;
		boolean history = false;
		if (marker != null) {
			fid = FavoriteImageDrawable.getOrCreateSyncedIcon(view.getContext(), o.getColor(), o);
			history = marker.history;
		} else {
			fid = FavoriteImageDrawable.getOrCreate(view.getContext(), o.getColor(), true, o);
		}
		fid.drawBitmapInCenter(canvas, x, y, history);
	}
	
	
	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	public void getFavoriteFromPoint(RotatedTileBox tb, PointF point, List<? super FavouritePoint> res) {
		int r = getDefaultRadiusPoi(tb);
		int ex = (int) point.x;
		int ey = (int) point.y;
		for (FavouritePoint n : favorites.getFavouritePoints()) {
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
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res, boolean unknownLocation) {
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
	public boolean isTextVisible() {
		return settings.SHOW_POI_LABEL.get();
	}

	@Override
	public boolean isFakeBoldText() {
		return false;
	}


	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof FavouritePoint;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o,
									   @NonNull LatLon position,
									   @Nullable ApplyMovedObjectCallback callback) {
		boolean result = false;
		if (o instanceof FavouritePoint) {
			favorites.editFavourite((FavouritePoint) o, position.getLatitude(), position.getLongitude());
			favorites.lookupAddress((FavouritePoint) o);
			result = true;
		}
		if (callback != null) {
			callback.onApplyMovedObject(result, o);
		}
	}
}


