package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.FavouritePoint;
import net.osmand.access.AccessibleToast;
import net.osmand.osm.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.R;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

public class FavoritesLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	private static final int startZoom = 6;
	
	private OsmandMapTileView view;
	private Paint paint;
	private DisplayMetrics dm;
	private FavouritesDbHelper favorites;
	private Bitmap favoriteIcon;
	
	
	public FavoritesLayer(){
	}
	
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		
		favorites = view.getApplication().getFavorites();
		
		favoriteIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_favourite);
		
	}

	@Override
	public void destroyLayer() {
		
	}
	

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		if (view.getZoom() >= startZoom) {
			// request to load
			for (FavouritePoint o : favorites.getFavouritePoints()) {
				if (o.getLatitude() >= latLonBounds.bottom && o.getLatitude() <= latLonBounds.top  && o.getLongitude() >= latLonBounds.left 
						&& o.getLongitude() <= latLonBounds.right ) {
					int x = view.getRotatedMapXForPoint(o.getLatitude(), o.getLongitude());
					int y = view.getRotatedMapYForPoint(o.getLatitude(), o.getLongitude());
					canvas.drawBitmap(favoriteIcon, x - favoriteIcon.getWidth() / 2, 
							y - favoriteIcon.getHeight() / 2, paint);
				}
			}
		}
	}
	
	
	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}
	
	public void getFavoriteFromPoint(PointF point, List<? super FavouritePoint> res) {
		float r = 80;
		int ex = (int) point.x;
		int ey = (int) point.y;
		int w = favoriteIcon.getWidth() / 2;
		int h = favoriteIcon.getHeight() / 2;
		for (FavouritePoint n : favorites.getFavouritePoints()) {
			int x = view.getRotatedMapXForPoint(n.getLatitude(), n.getLongitude());
			int y = view.getRotatedMapYForPoint(n.getLatitude(), n.getLongitude());
			if (Math.abs(x - ex) <= w && Math.abs(y - ey) <= h) {
				float newr = Math.max(Math.abs(x - ex), Math.abs(y - ey));
				if (newr < r) {
					res.add(n);
				}
			}
		}
	}

	@Override
	public boolean onSingleTap(PointF point) {
		List<FavouritePoint> favs = new ArrayList<FavouritePoint>();
		getFavoriteFromPoint(point, favs);
		if(!favs.isEmpty()){
			StringBuilder res = new StringBuilder();
			int i = 0;
			for(FavouritePoint fav : favs) {
				if (i++ > 0) {
					res.append("\n\n");
				}
				res.append(view.getContext().getString(R.string.favorite) + " : " + fav.getName());  //$NON-NLS-1$
			}
			AccessibleToast.makeText(view.getContext(), res.toString(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}


	@Override
	public OnClickListener getActionListener(List<String> actionsList, Object o) {
		return null;
	}
	

	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof FavouritePoint){
			return view.getContext().getString(R.string.favorite) + " : " + ((FavouritePoint)o).getName(); //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public String getObjectName(Object o) {
		if(o instanceof FavouritePoint){
			return ((FavouritePoint)o).getName(); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, List<Object> res) {
		getFavoriteFromPoint(point, res);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof FavouritePoint){
			return new LatLon(((FavouritePoint)o).getLatitude(), ((FavouritePoint)o).getLongitude());
		}
		return null;
	}
	

}


