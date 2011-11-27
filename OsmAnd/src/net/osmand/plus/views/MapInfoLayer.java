package net.osmand.plus.views;


import java.util.ArrayList;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper.TurnType;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.WindowManager;

public class MapInfoLayer extends OsmandMapLayer {


	private OsmandMapTileView view;
	private boolean showMiniMap = false;
	private final MapActivity map;
	private final RouteLayer routeLayer;
	
	private boolean showArrivalTime = true; 
	
	
	private Path pathForTurn;
	
	private Paint paintBlack;
	private Paint paintText;
	private Paint paintSubText;
	private Paint paintMiniRoute;
	private Paint fillBlack;
	private Paint fillRed;
	private Paint paintAlphaGray;
	private Paint paintRouteDirection;
	
	private RectF boundsForMiniRoute;
	private RectF boundsForLeftTime;
	private RectF boundsForSpeed;
	
	
	private String cachedLeftTimeString = null;
	private long cachedLeftTime = 0;
	private float[] calculations = new float[1];
	
	private int cachedMeters = 0;
	private int cachedZoom = 0;
	
	private String cachedSpeedString = null;
	private float cachedSpeed = 0;
	
	private int centerMiniRouteY;
	private int centerMiniRouteX;
	private float scaleMiniRoute;
	private Matrix pathTransform;
	private TurnType cachedTurnType = null;
	private String cachedExitOut = null;
	private DisplayMetrics dm;
	
	private float scaleCoefficient;
	private float roundCorner;
	private Bitmap compass;
	private Paint paintImg;
	
	private final static int[] pressedState = new int[]{android.R.attr.state_pressed};
	private final static int[] simpleState = new int[]{};
	
	private int LEFT_MARGIN = 10;
	

	private MapInfoControl compassControl;
	private TextInfoControl zoomControl;
	private TextInfoControl distanceControl;
	
	private List<MapInfoControl> leftControls = new ArrayList<MapInfoLayer.MapInfoControl>();
	
	public MapInfoLayer(MapActivity map, RouteLayer layer){
		this.map = map;
		this.routeLayer = layer;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		paintBlack = new Paint();
		paintText = new Paint();
		paintSubText = new Paint();
		WindowManager mgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		scaleCoefficient = dm.density;
		if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
			// large screen
			scaleCoefficient *= 1.5f;
		}
		
		roundCorner = 3 * scaleCoefficient;
		
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setTextSize(23 * scaleCoefficient);
		paintBlack.setAntiAlias(true);
		
		paintText.setStyle(Style.FILL_AND_STROKE);
		paintText.setColor(Color.BLACK);
		paintText.setTextSize(23 * scaleCoefficient);
		paintText.setAntiAlias(true);
		
		paintSubText.setStyle(Style.FILL_AND_STROKE);
		paintSubText.setColor(Color.BLACK);
		paintSubText.setTextSize(15 * scaleCoefficient);
		paintSubText.setFakeBoldText(true);
		paintSubText.setAntiAlias(true);
		
		paintImg = new Paint();
		paintImg.setDither(true);
		paintImg.setFilterBitmap(true);
		paintImg.setAntiAlias(true);
		
		paintAlphaGray = new Paint();
		paintAlphaGray.setStyle(Style.FILL_AND_STROKE);
		paintAlphaGray.setColor(Color.LTGRAY);
		paintAlphaGray.setAlpha(180); // do not make very transparent (to hide route)
		
		paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
		paintRouteDirection.setColor(Color.rgb(100, 0, 255));
		paintRouteDirection.setAntiAlias(true);
		
		fillBlack = new Paint();
		fillBlack.setStyle(Style.FILL_AND_STROKE);
		fillBlack.setColor(Color.BLACK);
		fillBlack.setAntiAlias(true);
		
		paintMiniRoute = new Paint();
		paintMiniRoute.setStyle(Style.STROKE);
		paintMiniRoute.setStrokeWidth(35 * scaleCoefficient);
		paintMiniRoute.setColor(Color.BLUE);
		paintMiniRoute.setStrokeJoin(Join.ROUND);
		paintMiniRoute.setStrokeCap(Cap.ROUND);
		paintMiniRoute.setAntiAlias(true);
		
		fillRed = new Paint();
		fillRed.setStyle(Style.FILL_AND_STROKE);
		fillRed.setColor(Color.RED);
		fillRed.setAntiAlias(true);
		
		boundsForSpeed = new RectF(35, 32, 110, 64);
		boundsForMiniRoute = new RectF(0, 64, 96, 196);
		
		boundsForLeftTime = new RectF(0, 0, 75, 32);

		// Scale to have proper view
		scaleRect(boundsForSpeed);
		scaleRect(boundsForMiniRoute);
		scaleRect(boundsForLeftTime);
		
		centerMiniRouteX = (int) (boundsForMiniRoute.width()/2);
		centerMiniRouteY= (int) (boundsForMiniRoute.top + 3 * boundsForMiniRoute.height() /4);
		scaleMiniRoute = 0.15f;
		
		pathForTurn = new Path();
		pathTransform = new Matrix();
		pathTransform.postScale(scaleCoefficient, scaleCoefficient);
		pathTransform.postTranslate(boundsForMiniRoute.left, boundsForMiniRoute.top);
		
		compass = BitmapFactory.decodeResource(view.getResources(), R.drawable.compass);
		
		LEFT_MARGIN = (int) (LEFT_MARGIN * scaleCoefficient);
		compassControl = createCompassControl(R.drawable.box_top);
		zoomControl = new TextInfoControl(R.drawable.box_top, paintText, paintSubText);
		distanceControl = new TextInfoControl(R.drawable.box_top, paintText, paintSubText) {
			@Override
			public boolean isClickable() {
				return true;
			}
			@Override
			public void click() {
				AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
				LatLon pointToNavigate = view.getSettings().getPointToNavigate();
				if(pointToNavigate != null){
					int fZoom = view.getZoom() < 15 ? 15 : view.getZoom(); 
					thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), 
							fZoom, true);
				}
			}
		};
		leftControls.add(compassControl);
		leftControls.add(zoomControl);
		leftControls.add(distanceControl);
		relayoutLeftControls(compassControl);
		
		showArrivalTime = view.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME.get();
	}

	private void scaleRect(RectF r){
		r.bottom *= scaleCoefficient;
		r.left *= scaleCoefficient;
		r.right *= scaleCoefficient;
		r.top *= scaleCoefficient;
	}
	
	public void relayoutLeftControls(MapInfoControl... cs){
		for(MapInfoControl c : cs) {
			c.prefferedLayout();
		}
		int w = 0;
		for(MapInfoControl c : leftControls) {
			w = Math.max(w, c.getWidth());
		}
		for(MapInfoControl c : leftControls) {
			c.layout(w, c.getHeight());
		}
		
	}
	
	public boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && oldDist - dist < 100 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
			return false;
		}
		return true;
	}
	
	private void updateDistanceToGo() {
		if(map.getPointToNavigate() != null){
			int d = 0;
			if(map.getRoutingHelper().isRouterEnabled()){
				d = map.getRoutingHelper().getLeftDistance();
			} 
			if (d == 0) {
				Location.distanceBetween(view.getLatitude(), view.getLongitude(), map.getPointToNavigate().getLatitude(), map
						.getPointToNavigate().getLongitude(), calculations);
				d = (int) calculations[0];
			}
			if(distChanged(cachedMeters, d)){
				cachedMeters = d;
				if(cachedMeters <= 20){
					cachedMeters = 0;
					distanceControl.setText(null, null);
				} else {
					String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, map);
					int ls = ds.lastIndexOf(' ');
					if(ls == -1) {
						distanceControl.setText(ds, null);	
					} else {
						distanceControl.setText(ds.substring(0, ls), ds.substring(ls + 1));
					}
					
					relayoutLeftControls(distanceControl);
				}
			}
		} else {
			cachedMeters = 0;
			distanceControl.setText(null, null);
		}
	}
	
	@Override
	public void onDraw(Canvas canvas, RectF latlonBounds, RectF tilesRect, boolean nightMode) {
		// prepare data (left distance, speed)
		updateDistanceToGo();
		if(view.getZoom() != cachedZoom){
			zoomControl.setText(view.getZoom()+"", "zm");
			relayoutLeftControls(zoomControl);
		}
		
		// draw speed 	
		if(map.getLastKnownLocation() != null && map.getLastKnownLocation().hasSpeed()){
			if(Math.abs(map.getLastKnownLocation().getSpeed() - cachedSpeed) > .3f){
				cachedSpeed = map.getLastKnownLocation().getSpeed();
				cachedSpeedString = OsmAndFormatter.getFormattedSpeed(cachedSpeed, map); 
				float right = paintBlack.measureText(cachedSpeedString) + 8 * scaleCoefficient + boundsForSpeed.left;
				boundsForSpeed.right = right;
			}
			if(cachedSpeed > 0){
				canvas.drawRoundRect(boundsForSpeed, roundCorner, roundCorner, paintAlphaGray);
				canvas.drawRoundRect(boundsForSpeed, roundCorner, roundCorner, paintBlack);
				canvas.drawText(cachedSpeedString, boundsForSpeed.left + 8 * scaleCoefficient, boundsForSpeed.bottom - 9f * scaleCoefficient, paintBlack);
			}
		}
		
		// draw route information
		drawRouteInfo(canvas);
		
		int h = 0;
		for(int i=0; i<leftControls.size(); i++){
			h += leftControls.get(i).getHeight();
		}
		canvas.translate(LEFT_MARGIN, 0);
		for (int i = leftControls.size() - 1; i >= 0; i--) {
			h -= leftControls.get(i).getHeight();
			canvas.save();
			canvas.translate(0, h);
			if(leftControls.get(i).isVisible()) {
				leftControls.get(i).onDraw(canvas);
			}
			canvas.restore();
		}
	}
	
	
	private void drawRouteInfo(Canvas canvas) {
		if(routeLayer != null && routeLayer.getHelper().isRouterEnabled()){
			if (routeLayer.getHelper().isFollowingMode()) {
				int d = routeLayer.getHelper().getDistanceToNextRouteDirection();
				if (showMiniMap || d == 0) {
					if (!routeLayer.getPath().isEmpty()) {
						canvas.save();
						canvas.clipRect(boundsForMiniRoute);
						canvas.drawRoundRect(boundsForMiniRoute, roundCorner, roundCorner, paintAlphaGray);
						canvas.drawRoundRect(boundsForMiniRoute, roundCorner, roundCorner, paintBlack);
						canvas.translate(centerMiniRouteX - view.getCenterPointX(), centerMiniRouteY - view.getCenterPointY());
						canvas.scale(scaleMiniRoute, scaleMiniRoute, view.getCenterPointX(), view.getCenterPointY());
						canvas.rotate(view.getRotate(), view.getCenterPointX(), view.getCenterPointY());
						canvas.drawCircle(view.getCenterPointX(), view.getCenterPointY(), 3 / scaleMiniRoute, fillBlack);
						canvas.drawPath(routeLayer.getPath(), paintMiniRoute);
						canvas.restore();
					}
				} else {
					canvas.drawRoundRect(boundsForMiniRoute, roundCorner, roundCorner, paintAlphaGray);
					canvas.drawRoundRect(boundsForMiniRoute, roundCorner, roundCorner, paintBlack);
					RouteDirectionInfo next = routeLayer.getHelper().getNextRouteDirectionInfo();
					if (next != null) {
						if (!Algoritms.objectEquals(cachedTurnType, next.turnType)) {
							cachedTurnType = next.turnType;
							calcTurnPath(pathForTurn, cachedTurnType, pathTransform);
							if (cachedTurnType.getExitOut() > 0) {
								cachedExitOut = cachedTurnType.getExitOut() + ""; //$NON-NLS-1$
							} else {
								cachedExitOut = null;
							}
						}
						canvas.drawPath(pathForTurn, paintRouteDirection);
						canvas.drawPath(pathForTurn, paintBlack);
						if (cachedExitOut != null) {
							canvas.drawText(cachedExitOut, boundsForMiniRoute.centerX() - 6 * scaleCoefficient, 
									boundsForMiniRoute.centerY() - 9 * scaleCoefficient, paintBlack);
						}
						canvas.drawText(OsmAndFormatter.getFormattedDistance(d, map), boundsForMiniRoute.left + 10 * scaleCoefficient, 
								boundsForMiniRoute.bottom - 9 * scaleCoefficient, paintBlack);
					}
				}
			}
			
			boolean followingMode = routeLayer.getHelper().isFollowingMode();
			int time = routeLayer.getHelper().getLeftTime();
			if(time == 0){
				cachedLeftTime = 0;
				cachedLeftTimeString = null;
			} else {
				if(followingMode && showArrivalTime){
					long toFindTime = time * 1000 + System.currentTimeMillis();
					if(Math.abs(toFindTime - cachedLeftTime) > 30000){
						cachedLeftTime = toFindTime;
						if(DateFormat.is24HourFormat(map)){
							cachedLeftTimeString = DateFormat.format("kk:mm",toFindTime).toString(); //$NON-NLS-1$
						} else {
							cachedLeftTimeString = DateFormat.format("k:mm aa",toFindTime).toString(); //$NON-NLS-1$
						}
						boundsForLeftTime.left = - paintBlack.measureText(cachedLeftTimeString) - 10 * scaleCoefficient + boundsForLeftTime.right;
					}
				} else {
					if(Math.abs(time - cachedLeftTime) > 30){
						cachedLeftTime = time;
						int hours = time / (60 * 60);
						int minutes = (time / 60) % 60;
						cachedLeftTimeString = String.format("%d:%02d", hours, minutes); //$NON-NLS-1$
						boundsForLeftTime.left = - paintBlack.measureText(cachedLeftTimeString) - 10 * scaleCoefficient + boundsForLeftTime.right;
					}
				}
			}
			if(cachedLeftTimeString != null) {
				int w = (int) (boundsForLeftTime.right - boundsForLeftTime.left); 
				boundsForLeftTime.right = view.getWidth();
				boundsForLeftTime.left = view.getWidth() - w;
				canvas.drawRoundRect(boundsForLeftTime, roundCorner, roundCorner, paintAlphaGray);
				canvas.drawRoundRect(boundsForLeftTime, roundCorner, roundCorner, paintBlack);
				canvas.drawText(cachedLeftTimeString, boundsForLeftTime.left + 5 * scaleCoefficient, boundsForLeftTime.bottom - 9 * scaleCoefficient, paintBlack);
				
			}
		}
	}
	

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	
	@Override
	public void onTouchEvent(MotionEvent event) {
		int x = (int)event.getX();
		int y = (int)event.getY();
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			MapInfoControl control = detectLeftControl(x, y);
			if (control != null && control.isClickable()) {
				control.setPressed(true);
				view.refreshMap();
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
			for(MapInfoControl c : leftControls){
				c.setPressed(false);
				view.refreshMap();
			}
		}
	}


	@Override
	public boolean onSingleTap(PointF point) {
		if (routeLayer != null && routeLayer.getHelper().isRouterEnabled()) {
			if (boundsForMiniRoute.contains(point.x, point.y) && routeLayer.getHelper().isFollowingMode()) {
				showMiniMap = !showMiniMap;
				view.refreshMap();
				return true;
			}
			if (boundsForLeftTime.contains(point.x, point.y) && routeLayer.getHelper().isFollowingMode()) {
				showArrivalTime = !showArrivalTime;
				view.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME.set(showArrivalTime);
				view.refreshMap();
				return true;
			}
		}
		MapInfoControl control = detectLeftControl((int)point.x, (int)point.y);
		if (control != null && control.isClickable()) {
			control.click();
			return true;
		}
		return false;
	}
	
	private MapInfoControl detectLeftControl(int x, int y) {
		x -= LEFT_MARGIN;
		if (y >= 0 && x >= 0) {
			for (int i = 0; i < leftControls.size(); i++) {
				if (leftControls.get(i).isVisible()) {
					y -= leftControls.get(i).getHeight();
					if (y <= 0) {
						if (x <= leftControls.get(i).getWidth()) {
							return leftControls.get(i);
						} else {
							return null;
						}
					}
				}
			}
		}
		return null;
	}
	
	private MapInfoControl createCompassControl(int boxTop) {
		final int mw = (int) compass.getWidth() ;
		final int mh = (int) compass.getHeight() ;
		MapInfoControl control = new MapInfoControl(boxTop) {
			
			@Override
			public int getMeasuredWidth() {
				return mw;
			}
			@Override
			public int getMeasuredHeight() {
				return mh;
			}
			
			@Override
			public boolean isClickable() {
				return true;
			}
			
			@Override
			public void click() {
				map.switchRotateMapMode();
			}
			
			@Override
			void onDraw(Canvas cv) {
				super.onDraw(cv);
				cv.rotate(view.getRotate(), mw / 2, mh / 2);
				cv.drawBitmap(compass, 0, 0, paintImg);
			}
		};
		return control;
	}
	
	
	public abstract class MapInfoControl {
		int width = 0;
		int height = 0;
		boolean pressed = false;
		Drawable background;
		
		public MapInfoControl(int background){
			this.background = view.getResources().getDrawable(background).mutate();
		}
		
		public boolean isClickable(){
			return false;
		}
		
		public boolean isVisible() {
			return true;
		}
		
		public void click() {
		}
		
		public void setPressed(boolean pressed) {
			if(this.pressed != pressed) {
				this.pressed = pressed;
				if(pressed) {
					background.setState(pressedState);
				} else {
					background.setState(simpleState);
				}
				Rect padding = new Rect();
				if(background.getPadding(padding)) {
					background.setBounds(-padding.left, -padding.top,
							width + padding.right, height + padding.bottom);
				}
			}
		}
		
		public int getWidth() {
			return width;
		}
		
		public int getHeight() {
			return isVisible() ? height : 0;
		}
		
		public void prefferedLayout(){
			layout(getMeasuredWidth(), getMeasuredHeight());
		}
		
		public void layout(int w, int h) {
			this.width = w;
			this.height = h;
			Rect padding = new Rect();
			if(background.getPadding(padding)) {
				background.setBounds(-padding.left, -padding.top,
						w + padding.right, h + padding.bottom);
			}
		}
		
		public abstract int getMeasuredWidth();
		
		public abstract int getMeasuredHeight();
		
		void onDraw(Canvas cv) {
			background.draw(cv);
		}
	}
	
	public class TextInfoControl extends MapInfoControl {
		
		String text;
		Paint textPaint;
		String subtext;
		Paint subtextPaint;
		
		public TextInfoControl(int background, Paint textPaint,
				Paint subtextPaint) {
			super(background);
			this.textPaint = textPaint;
			this.subtextPaint = subtextPaint;
		}
		
		public void setText(String text, String subtext) {
			this.text = text;
			this.subtext = subtext;
		}
		
		@Override
		public int getMeasuredWidth() {
			int w = 0;
			if(text != null) {
				w += textPaint.measureText(text) + 3 * scaleCoefficient;
				if(subtext != null) {
					w += textPaint.measureText(subtext) + 4 * scaleCoefficient;
				}
			}
			return w;
		}
		
		@Override
		void onDraw(Canvas cv) {
			super.onDraw(cv);
			if(isVisible()) {
				cv.drawText(text, 3 * scaleCoefficient, getHeight() - scaleCoefficient,
						textPaint);
				if(subtext != null) {
					cv.drawText(subtext, 7 * scaleCoefficient + textPaint.measureText(text), 
							getHeight() - scaleCoefficient, subtextPaint);
				}
			}
		}
		
		@Override
		public boolean isVisible() {
			return text != null && text.length() > 0;
		}
		
		@Override
		public int getMeasuredHeight() {
			return (int) (5 * scaleCoefficient + Math.max(textPaint.getTextSize(), subtextPaint.getTextSize()));
		}
		
		
		
	}
	

	// draw path 96x96
	public static void calcTurnPath(Path pathForTurn, TurnType turnType, Matrix transform) {
		if(turnType == null){
			return;
		}
		pathForTurn.reset();

		int c = 48;
		int w = 16;
		pathForTurn.moveTo(c, 94);
		float sarrowL = 30; // side of arrow
		float harrowL = (float) Math.sqrt(2) * sarrowL; // hypotenuse of arrow
		float spartArrowL = (float) ((sarrowL - w / Math.sqrt(2)) / 2);
		float hpartArrowL = (float) (harrowL - w) / 2;

		if (TurnType.C.equals(turnType.getValue())) {
			int h = 65;

			pathForTurn.rMoveTo(w / 2, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rLineTo(hpartArrowL, 0);
			pathForTurn.rLineTo(-harrowL / 2, -harrowL / 2); // center
			pathForTurn.rLineTo(-harrowL / 2, harrowL / 2);
			pathForTurn.rLineTo(hpartArrowL, 0);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.TR.equals(turnType.getValue())|| TurnType.TL.equals(turnType.getValue())) {
			int b = TurnType.TR.equals(turnType.getValue())? 1 : -1;
			int h = 36;
			float quadShiftX = 22;
			float quadShiftY = 22;

			pathForTurn.rMoveTo(-b * 8, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rQuadTo(0, -quadShiftY, b * quadShiftX, -quadShiftY);
			pathForTurn.rLineTo(0, hpartArrowL);
			pathForTurn.rLineTo(b * harrowL / 2, -harrowL / 2); // center
			pathForTurn.rLineTo(-b * harrowL / 2, -harrowL / 2);
			pathForTurn.rLineTo(0, hpartArrowL);
			pathForTurn.rQuadTo(-b * (quadShiftX + w), 0, -b * (quadShiftX + w), quadShiftY + w);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.TSLR.equals(turnType.getValue()) || TurnType.TSLL.equals(turnType.getValue())) {
			int b = TurnType.TSLR.equals(turnType.getValue()) ? 1 : -1;
			int h = 40;
			int quadShiftY = 22;
			float quadShiftX = (float) (quadShiftY / (1 + Math.sqrt(2)));
			float nQuadShiftX = (sarrowL - 2 * spartArrowL) - quadShiftX - w;
			float nQuadShifty = quadShiftY + (sarrowL - 2 * spartArrowL);

			pathForTurn.rMoveTo(-b * 4, 0);
			pathForTurn.rLineTo(0, -h /* + partArrowL */);
			pathForTurn.rQuadTo(0, -quadShiftY + quadShiftX /*- partArrowL*/, b * quadShiftX, -quadShiftY /*- partArrowL*/);
			pathForTurn.rLineTo(b * spartArrowL, spartArrowL);
			pathForTurn.rLineTo(0, -sarrowL); // center
			pathForTurn.rLineTo(-b * sarrowL, 0);
			pathForTurn.rLineTo(b * spartArrowL, spartArrowL);
			pathForTurn.rQuadTo(b * nQuadShiftX, -nQuadShiftX, b * nQuadShiftX, nQuadShifty);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.TSHR.equals(turnType.getValue()) || TurnType.TSHL.equals(turnType.getValue())) {
			int b = TurnType.TSHR.equals(turnType.getValue()) ? 1 : -1;
			int h = 45;
			float quadShiftX = 22;
			float quadShiftY = -(float) (quadShiftX / (1 + Math.sqrt(2)));
			float nQuadShiftX = -(sarrowL - 2 * spartArrowL) - quadShiftX - w;
			float nQuadShiftY = -quadShiftY + (sarrowL - 2 * spartArrowL);

			pathForTurn.rMoveTo(-b * 8, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rQuadTo(0, -(quadShiftX - quadShiftY), b * quadShiftX, quadShiftY);
			pathForTurn.rLineTo(-b * spartArrowL, spartArrowL);
			pathForTurn.rLineTo(b * sarrowL, 0); // center
			pathForTurn.rLineTo(0, -sarrowL);
			pathForTurn.rLineTo(-b * spartArrowL, spartArrowL);
			pathForTurn.rCubicTo(b * nQuadShiftX / 2, nQuadShiftX / 2, b * nQuadShiftX, nQuadShiftX / 2, b * nQuadShiftX, nQuadShiftY);
			pathForTurn.rLineTo(0, h);
		} else if(TurnType.TU.equals(turnType.getValue())) {
			int h = 54;
			float quadShiftX = 13;
			float quadShiftY = 13;

			pathForTurn.rMoveTo(28, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rQuadTo(0, -(quadShiftY+w), -(quadShiftX+w), -(quadShiftY+w));
			pathForTurn.rQuadTo(-(quadShiftX+w), 0, -(quadShiftX+w), (quadShiftY+w));
			pathForTurn.rLineTo(-hpartArrowL, 0);
			pathForTurn.rLineTo(harrowL/2, harrowL/2); // center
			pathForTurn.rLineTo(harrowL/2, -harrowL/2);
			pathForTurn.rLineTo(-hpartArrowL, 0);
			pathForTurn.rQuadTo(0, -quadShiftX, quadShiftX, -quadShiftY);
			pathForTurn.rQuadTo(quadShiftX, 0, quadShiftX, quadShiftY);
			pathForTurn.rLineTo(0, h);
		} else if (turnType != null && turnType.isRoundAbout()) {
			float t = turnType.getTurnAngle();
			if (t >= 170 && t < 220) {
				t = 220;
			} else if (t > 160 && t < 170) {
				t = 160;
			}
			float sweepAngle = (t - 360) - 180;
			if (sweepAngle < -360) {
				sweepAngle += 360;
			}
			float r1 = 32f;
			float r2 = 24f;
			float angleToRot = 0.3f;
			
			pathForTurn.moveTo(48, 48 + r1 + 8);
			pathForTurn.lineTo(48, 48 + r1);
			RectF r = new RectF(48 - r1, 48 - r1, 48 + r1, 48 + r1);
			pathForTurn.arcTo(r, 90, sweepAngle);
			float angleRad = (float) ((180 + sweepAngle)*Math.PI/180f);
			
			pathForTurn.lineTo(48 + (r1 + 4) * FloatMath.sin(angleRad), 48 - (r1 + 4) * FloatMath.cos(angleRad));
			pathForTurn.lineTo(48 + (r1 + 6) * FloatMath.sin(angleRad + angleToRot/2), 48 - (r1 + 6) * FloatMath.cos(angleRad + angleToRot/2));
			pathForTurn.lineTo(48 + (r1 + 12) * FloatMath.sin(angleRad - angleToRot/2), 48 - (r1 + 12) * FloatMath.cos(angleRad - angleToRot/2));
			pathForTurn.lineTo(48 + (r1 + 6) * FloatMath.sin(angleRad - 3*angleToRot/2), 48 - (r1 + 6) * FloatMath.cos(angleRad - 3*angleToRot/2));
			pathForTurn.lineTo(48 + (r1 + 4) * FloatMath.sin(angleRad - angleToRot), 48 - (r1 + 4) * FloatMath.cos(angleRad - angleToRot));
			pathForTurn.lineTo(48 + r2 * FloatMath.sin(angleRad - angleToRot), 48 - r2 * FloatMath.cos(angleRad - angleToRot));
			
			r.set(48 - r2, 48 - r2, 48 + r2, 48 + r2);
			pathForTurn.arcTo(r, 360 + sweepAngle + 90, -sweepAngle);
			pathForTurn.lineTo(40, 48 + r2);
			pathForTurn.lineTo(40, 48 + r1 + 8);
			pathForTurn.close();
		}
		pathForTurn.close();
		if(transform != null){
			pathForTurn.transform(transform);
		}
	}



}
