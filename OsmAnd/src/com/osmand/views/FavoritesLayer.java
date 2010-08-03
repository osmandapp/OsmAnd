package com.osmand.views;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.WindowManager;
import android.widget.Toast;

import com.osmand.R;
import com.osmand.activities.FavouritesActivity;
import com.osmand.activities.MapActivity;
import com.osmand.activities.FavouritesActivity.FavouritePoint;
import com.osmand.activities.FavouritesActivity.FavouritesDbHelper;
import com.osmand.osm.MapUtils;

public class FavoritesLayer implements OsmandMapLayer {

	private static final int startZoom = 6;
	private static final int radius = 15;
	
	private OsmandMapTileView view;
	private List<FavouritePoint> favouritePoints;
	private Rect pixRect = new Rect();
	private RectF tileRect = new RectF();
	private Path path;
	private Path pathDst;
	private Paint paint;
	private Matrix matrix;
	private Paint paintBlack;
	private final MapActivity activity;
	private DisplayMetrics dm;
	
	
	public FavoritesLayer(MapActivity activity){
		this.activity = activity;
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
		
		pixRect.set(0, 0, view.getWidth(), view.getHeight());
		reloadFavorites(view.getContext());
	}

	@Override
	public void destroyLayer() {
		
	}
	
	public void reloadFavorites(Context ctx){
		FavouritesDbHelper helper = new FavouritesActivity.FavouritesDbHelper(ctx);
		favouritePoints = helper.getFavouritePoints();
		helper.close();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (view.getZoom() >= startZoom) {
			pixRect.set(0, 0, view.getWidth(), view.getHeight());
			view.calculateTileRectangle(pixRect, view.getCenterPointX(), view.getCenterPointY(), view.getXTile(), view.getYTile(),
							tileRect);
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);

			// request to load
			for (FavouritePoint o : favouritePoints) {
				if (o.getLatitude() <= topLatitude && o.getLatitude() >= bottomLatitude && o.getLongitude() >= leftLongitude
						&& o.getLongitude() <= rightLongitude) {
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
		FavouritePoint fav = getFavoriteFromPoint(point);
		if(fav != null && activity != null){
			activity.contextMenuPoint(fav.getLatitude(), fav.getLongitude(), false);
			return true;
		}
		return false;
	}
	
	public FavouritePoint getFavoriteFromPoint(PointF point){
		FavouritePoint result = null;
		float r = radius * dm.density;
		if (favouritePoints != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			for (int i = 0; i < favouritePoints.size(); i++) {
				FavouritePoint n = favouritePoints.get(i);
				int x = view.getRotatedMapXForPoint(n.getLatitude(), n.getLongitude());
				int y = view.getRotatedMapYForPoint(n.getLatitude(), n.getLongitude());
				if (Math.abs(x - ex) <= r && Math.abs(y - ey) <= r) {
					r = Math.max(Math.abs(x - ex), Math.abs(y - ey));
					result = n;
				}
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
	
	

}


