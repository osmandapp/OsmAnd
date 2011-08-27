package net.osmand.plus.views;

import java.util.List;

import net.osmand.FavouritePoint;
import net.osmand.osm.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.R;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.WindowManager;
import android.widget.Toast;

public class FavoritesLayer implements OsmandMapLayer, ContextMenuLayer.IContextMenuProvider {

	private static final int startZoom = 6;
	private static final int radius = 15;
	
	private OsmandMapTileView view;
	private Path path;
	private Path pathDst;
	private Paint paint;
	private Matrix matrix;
	private Paint paintBlack;
	private DisplayMetrics dm;
	private FavouritesDbHelper favorites;
	
	
	public FavoritesLayer(){
	}
	
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		path = new Path();
		pathDst = new Path();
		int coef1 = (int) (radius * dm.density);
		int coef2 = (int) (radius * dm.density/2);
		float a = (float) (Math.PI/ 5);
		path.moveTo(FloatMath.sin(0)*coef1, -FloatMath.cos(0)*coef1);
		for (int j = 1; j < 10; j++) {
			if (j % 2 == 1) {
				path.lineTo(FloatMath.sin(j * a) * coef2, -FloatMath.cos(j * a) * coef2);
			} else {
				path.lineTo(FloatMath.sin(j * a) * coef1, -FloatMath.cos(j * a) * coef1);
			}
		}
		matrix = new Matrix();
		path.close();
		
		paint = new Paint();
		paint.setStyle(Style.FILL);
		paint.setARGB(200, 255, 150, 0);
		paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setARGB(255, 0, 0, 0);
		paintBlack.setAntiAlias(true);
		paintBlack.setStrokeWidth(2);
		
		favorites = view.getApplication().getFavorites();
		
	}

	@Override
	public void destroyLayer() {
		
	}
	

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
	
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, boolean nightMode, boolean moreDetail) {
		if (view.getZoom() >= startZoom) {
			// request to load
			for (FavouritePoint o : favorites.getFavouritePoints()) {
				if (o.getLatitude() >= latLonBounds.bottom && o.getLatitude() <= latLonBounds.top  && o.getLongitude() >= latLonBounds.left 
						&& o.getLongitude() <= latLonBounds.right ) {
					int x = view.getMapXForPoint(o.getLongitude());
					int y = view.getMapYForPoint(o.getLatitude());
					matrix.setTranslate(x, y);
					path.transform(matrix, pathDst);
					canvas.drawPath(pathDst, paint);
					canvas.drawPath(pathDst, paintBlack);
				}
			}
		}
	}
	
	
	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}
	
	public FavouritePoint getFavoriteFromPoint(PointF point) {
		FavouritePoint result = null;
		float r = radius * dm.density;
		int ex = (int) point.x;
		int ey = (int) point.y;
		for (FavouritePoint n : favorites.getFavouritePoints()) {
			int x = view.getRotatedMapXForPoint(n.getLatitude(), n.getLongitude());
			int y = view.getRotatedMapYForPoint(n.getLatitude(), n.getLongitude());
			if (Math.abs(x - ex) <= r && Math.abs(y - ey) <= r) {
				r = Math.max(Math.abs(x - ex), Math.abs(y - ey));
				result = n;
			}
		}
		return result;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		FavouritePoint fav = getFavoriteFromPoint(point);
		if(fav != null){
			String format = view.getContext().getString(R.string.favorite) + " : " + fav.getName();  //$NON-NLS-1$
			Toast.makeText(view.getContext(), format, Toast.LENGTH_LONG).show();
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
	public Object getPointObject(PointF point) {
		return getFavoriteFromPoint(point);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof FavouritePoint){
			return new LatLon(((FavouritePoint)o).getLatitude(), ((FavouritePoint)o).getLongitude());
		}
		return null;
	}
	

}


