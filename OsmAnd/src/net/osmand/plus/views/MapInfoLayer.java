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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

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
	
	private long cachedLeftTime = 0;
	private float[] calculations = new float[1];
	private int cachedMeters = 0;
	private float cachedSpeed = 0;
	
	private int centerMiniRouteY;
	private int centerMiniRouteX;
	private float scaleMiniRoute;
	private Matrix pathTransform;
	private TurnType cachedTurnType = null;
	private String cachedExitOut = null;
	private DisplayMetrics dm;
	
	private float scaleCoefficient;
	private Paint paintImg;
	
	// layout pseudo-constants
	private int STATUS_BAR_MARGIN_X = 10;
	private int MARGIN_Y = 10;
	
	// controls
	private TextInfoControl distanceControl;
	private TextInfoControl speedControl;
	private TextInfoControl leftTimeControl;
	
	private ImageView backToLocation;
	private View progressBar;
	
	// groups
	private List<MapInfoControl> leftControls = new ArrayList<MapInfoLayer.MapInfoControl>();
	private List<MapInfoControl> rightControls = new ArrayList<MapInfoLayer.MapInfoControl>();
	private ViewGroup statusBar;
	
	// currently pressed view
	private View pressedView = null;
	private Drawable statusBarBackground;
	
	public MapInfoLayer(MapActivity map, RouteLayer layer){
		this.map = map;
		this.routeLayer = layer;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		WindowManager mgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		scaleCoefficient = dm.density;
		if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
			// large screen
			scaleCoefficient *= 1.5f;
		}
		
		FrameLayout parent = (FrameLayout) view.getParent();
		
		paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setTextSize(23 * scaleCoefficient);
		paintBlack.setAntiAlias(true);
		
		paintText = new Paint();
		paintText.setStyle(Style.FILL_AND_STROKE);
		paintText.setColor(Color.BLACK);
		paintText.setTextSize(23 * scaleCoefficient);
		paintText.setAntiAlias(true);

		paintSubText = new Paint();
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
		
		boundsForMiniRoute = new RectF(0, 64, 96, 196);
		

		// Scale to have proper view
		scaleRect(boundsForMiniRoute);
		
		centerMiniRouteX = (int) (boundsForMiniRoute.width()/2);
		centerMiniRouteY= (int) (boundsForMiniRoute.top + 3 * boundsForMiniRoute.height() /4);
		scaleMiniRoute = 0.15f;
		
		pathForTurn = new Path();
		pathTransform = new Matrix();
		pathTransform.postScale(scaleCoefficient, scaleCoefficient);
		pathTransform.postTranslate(boundsForMiniRoute.left, boundsForMiniRoute.top);
		
		
		STATUS_BAR_MARGIN_X = (int) (STATUS_BAR_MARGIN_X * scaleCoefficient);
		statusBar = createStatusBar();
		parent.addView(statusBar);
		
		MARGIN_Y = statusBar.getMeasuredHeight() ;
		Drawable time = view.getResources().getDrawable(R.drawable.info_time);
		speedControl = new TextInfoControl(R.drawable.box_top, null, 0, paintText, paintSubText);
		leftTimeControl = new TextInfoControl(R.drawable.box_top, time, 0, paintText, paintSubText);
		leftTimeControl.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showArrivalTime = !showArrivalTime;
				view.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME.set(showArrivalTime);
				view.refreshMap();
			}
		});
		distanceControl = new TextInfoControl(R.drawable.box_top, view.getResources().getDrawable(R.drawable.info_target), 0,
				paintText, paintSubText);
		distanceControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
				LatLon pointToNavigate = view.getSettings().getPointToNavigate();
				if(pointToNavigate != null){
					int fZoom = view.getZoom() < 15 ? 15 : view.getZoom(); 
					thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), 
							fZoom, true);
				}
			}
		});
		rightControls.add(distanceControl);
		rightControls.add(speedControl);
		rightControls.add(leftTimeControl);
		
		
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
			c.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		}
		int w = 0;
		for(MapInfoControl c : leftControls) {
			w = Math.max(w, c.getMeasuredWidth());
		}
		int x = STATUS_BAR_MARGIN_X;
		int y = MARGIN_Y;
		for(MapInfoControl c : leftControls) {
			c.layout(x, y, x + w, y + c.getMeasuredHeight());
			y += c.getMeasuredHeight();
		}
	}
	
	public void relayoutRightControls(MapInfoControl... cs){
		for(MapInfoControl c : cs) {
			c.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		}
		int w = 0;
		for(MapInfoControl c : rightControls) {
			w = Math.max(w, c.getMeasuredWidth());
		}
		int x = view.getWidth() -  STATUS_BAR_MARGIN_X - w;
		int y = MARGIN_Y;
		for(MapInfoControl c : rightControls) {
			c.layout(x, y, x + w, y + c.getMeasuredHeight());
			y += c.getMeasuredHeight();
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
				}
				relayoutRightControls(distanceControl);
			}
		} else if(cachedMeters != 0){
			cachedMeters = 0;
			distanceControl.setText(null, null);
			relayoutRightControls(distanceControl);
		}
	}
	
	@Override
	public void onDraw(Canvas canvas, RectF latlonBounds, RectF tilesRect, boolean nightMode) {
		// prepare data (left distance, speed)
		updateDistanceToGo();
		updateSpeedInfo();
		updateTimeLeftInfo();
		
		
		// draw route information
		drawRouteInfo(canvas);
		
		// draw left controls
		for (int i = leftControls.size() - 1; i >= 0; i--) {
			if(leftControls.get(i).getMeasuredHeight() > 0) {
				leftControls.get(i).onDraw(canvas);
			}
		}
		for (int i = rightControls.size() - 1; i >= 0; i--) {
			if(rightControls.get(i).getMeasuredHeight() > 0) {
				rightControls.get(i).onDraw(canvas);
			}
		}
		statusBarBackground.draw(canvas);
	}

	private void updateSpeedInfo() {
		// draw speed 	
		if(map.getLastKnownLocation() != null){
			if(map.getLastKnownLocation().hasSpeed()) {
				if(Math.abs(map.getLastKnownLocation().getSpeed() - cachedSpeed) > .3f){
					cachedSpeed = map.getLastKnownLocation().getSpeed();
					String ds = OsmAndFormatter.getFormattedSpeed(cachedSpeed, map);
					int ls = ds.lastIndexOf(' ');
					if(ls == -1) {
						speedControl.setText(ds, null);	
					} else {
						speedControl.setText(ds.substring(0, ls), ds.substring(ls + 1));
					}
					relayoutRightControls(speedControl);
				}	
			} else if(cachedSpeed != 0) {
				cachedSpeed = 0;
				speedControl.setText(null, null);
				relayoutRightControls(speedControl);
			}
		}
	}
	
	private void updateTimeLeftInfo() {
		int time = 0;
		if (routeLayer != null && routeLayer.getHelper().isRouterEnabled()) {
			boolean followingMode = routeLayer.getHelper().isFollowingMode();
			time = routeLayer.getHelper().getLeftTime();
			if (time != 0) {
				if (followingMode && showArrivalTime) {
					long toFindTime = time * 1000 + System.currentTimeMillis();
					if (Math.abs(toFindTime - cachedLeftTime) > 30000) {
						cachedLeftTime = toFindTime;
						if (DateFormat.is24HourFormat(map)) {
							leftTimeControl.setText(DateFormat.format("kk:mm", toFindTime).toString(), null); //$NON-NLS-1$
						} else {
							leftTimeControl.setText(DateFormat.format("k:mm aa", toFindTime).toString(), null); //$NON-NLS-1$
						}
						relayoutRightControls(leftTimeControl);
					}
				} else {
					if (Math.abs(time - cachedLeftTime) > 30) {
						cachedLeftTime = time;
						int hours = time / (60 * 60);
						int minutes = (time / 60) % 60;
						leftTimeControl.setText(String.format("%d:%02d", hours, minutes), null); //$NON-NLS-1$
					}
					relayoutRightControls(leftTimeControl);
				}
			}
		}
		if (time == 0 && cachedLeftTime != 0) {
			cachedLeftTime = 0;
			leftTimeControl.setText(null, null);
			relayoutRightControls(leftTimeControl);
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
						// TODO draw rect boundsForMiniRoute
						canvas.translate(centerMiniRouteX - view.getCenterPointX(), centerMiniRouteY - view.getCenterPointY());
						canvas.scale(scaleMiniRoute, scaleMiniRoute, view.getCenterPointX(), view.getCenterPointY());
						canvas.rotate(view.getRotate(), view.getCenterPointX(), view.getCenterPointY());
						canvas.drawCircle(view.getCenterPointX(), view.getCenterPointY(), 3 / scaleMiniRoute, fillBlack);
						canvas.drawPath(routeLayer.getPath(), paintMiniRoute);
						canvas.restore();
					}
				} else {
					// TODO draw rect boundsForMiniRoute
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
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			pressedView = null;
			ArrayList<View> l = new ArrayList<View>(leftControls);
			l.addAll(rightControls);
			for (View v : l) {
				if (v.getMeasuredHeight() > 0 && v.isClickable()) {
					if (v.getLeft() <= x && x <= v.getRight() && v.getTop() <= y && y <= v.getBottom()) {
						pressedView = v;
						break;
					}
				}
			}
			if (pressedView != null) {
				pressedView.setPressed(true);
				view.refreshMap();
			}
			return pressedView != null;
		}
		boolean pressed = pressedView != null;
		if (pressed && (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
			pressedView.setPressed(false);
			if (pressedView.getLeft() <= x && x <= pressedView.getRight() && pressedView.getTop() <= y && y <= pressedView.getBottom()) {
				pressedView.performClick();
			}
			view.refreshMap();
		}
		return pressed;
	}

	
	public ImageView getBackToLocation() {
		return backToLocation;
	}
	
	public View getProgressBar() {
		return progressBar;
	}

	@Override
	public boolean onSingleTap(PointF point) {
		if (routeLayer != null && routeLayer.getHelper().isRouterEnabled()) {
			if (boundsForMiniRoute.contains(point.x, point.y) && routeLayer.getHelper().isFollowingMode()) {
				showMiniMap = !showMiniMap;
				view.refreshMap();
				return true;
			}
		}
		return false;
	}
	
	
	private ViewGroup createStatusBar() {
		final Drawable compass = view.getResources().getDrawable(R.drawable.compass);
		final int mw = (int) compass.getMinimumWidth() ;
		final int mh = (int) compass.getMinimumHeight() ;
		statusBarBackground = view.getResources().getDrawable(R.drawable.box_top).mutate();
		LinearLayout statusBar = new LinearLayout(view.getContext()) {
			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
				super.onLayout(changed, left, top, right, bottom);
				Rect padding = new Rect();
				if (statusBarBackground.getPadding(padding)) {
					statusBarBackground.setBounds(-padding.left + left, top 
							-padding.top, right + padding.right, bottom + padding.bottom);
				}
			}
		};
		statusBar.setOrientation(LinearLayout.HORIZONTAL);
		FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, 
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		flp.leftMargin = STATUS_BAR_MARGIN_X;
		flp.rightMargin = STATUS_BAR_MARGIN_X;
		flp.gravity = Gravity.TOP;
		statusBar.setLayoutParams(flp);
		statusBar.setBackgroundColor(Color.argb(100, 200, 200, 200));
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		//params.leftMargin = (int) (5 * scaleCoefficient);
		ImageView compassView = new ImageView(view.getContext()) {
			@Override
			protected void onDraw(Canvas canvas) {
				canvas.save();
				canvas.translate(getLeft(), getTop());
				canvas.rotate(view.getRotate(), mw / 2, mh / 2);
				compass.draw(canvas);
				canvas.restore();
			}
		};
		compassView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.switchRotateMapMode();
			}
		});
		compassView.setImageDrawable(compass);
		statusBar.addView(compassView, params);
		
		// Space
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1);
		TextView space = new TextView(view.getContext());
		statusBar.addView(space, params);

		// Map and progress
		Drawable globusDrawable = view.getResources().getDrawable(R.drawable.globus);
		
		params = new LinearLayout.LayoutParams(globusDrawable.getMinimumWidth(), globusDrawable.getMinimumHeight());
		FrameLayout fl = new FrameLayout(view.getContext());
		statusBar.addView(fl, params);
		
		FrameLayout.LayoutParams fparams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		ImageView globus = new ImageView(view.getContext());
		globus.setImageDrawable(globusDrawable);
		globus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.getMapLayers().selectMapLayer(view);
			}
		});
		fl.addView(globus, fparams);
		
		fparams = new FrameLayout.LayoutParams(globusDrawable.getMinimumWidth(), globusDrawable.getMinimumHeight());
		progressBar = new View(view.getContext());
		progressBar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.backToLocationImpl();
			}
		});
		fl.addView(progressBar, fparams);
		
		params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		backToLocation = new ImageView(view.getContext());
		backToLocation.setImageDrawable(view.getResources().getDrawable(R.drawable.back_to_loc));
		backToLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.backToLocationImpl();
			}
		});
		statusBar.addView(backToLocation, params);
		
		statusBar.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		return statusBar;
	}
	
	
	public abstract class MapInfoControl extends View {
		int width = 0;
		int height = 0;
		boolean pressed = false;
		Drawable background;
		
		public MapInfoControl(int background){
			super(view.getContext());
			this.background = view.getResources().getDrawable(background).mutate();
		}
		
		@Override
		public void setPressed(boolean pressed) {
			super.setPressed(pressed);
			background.setState(getDrawableState());
			Rect padding = new Rect();
			if (background.getPadding(padding)) {
				background.setBounds(-padding.left + getLeft(), getTop() 
						-padding.top, getRight() + padding.right, getBottom() + padding.bottom);
			}
		}
		
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			super.onLayout(changed, left, top, right, bottom);
			Rect padding = new Rect();
			if (background.getPadding(padding)) {
				background.setBounds(-padding.left + left, top 
						-padding.top, right + padding.right, bottom + padding.bottom);
			}
		}
		
		@Override
		protected void onDraw(Canvas cv) {
			background.draw(cv);
		}
	}
	
	public class TextInfoControl extends MapInfoControl {
		
		String text;
		Paint textPaint;
		String subtext;
		Paint subtextPaint;
		int leftMargin = 0;
		private Drawable imageDrawable;
		
		public TextInfoControl(int background, Drawable drawable, int leftMargin, Paint textPaint,
				Paint subtextPaint) {
			super(background);
			this.leftMargin = leftMargin;
			this.imageDrawable = drawable;
			this.textPaint = textPaint;
			this.subtextPaint = subtextPaint;
		}
		
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			super.onLayout(changed, left, top, right, bottom);
			if(imageDrawable != null) {
				// Unknown reason to add 3*scaleCoefficient
				imageDrawable.setBounds(getLeft(), 
						getTop() + (int) (3*scaleCoefficient), 
						getLeft() + imageDrawable.getMinimumWidth(), 
						getTop() +  imageDrawable.getMinimumHeight() + 
						(int)(3*scaleCoefficient));
			}
		}
		
		public void setText(String text, String subtext) {
			this.text = text;
			this.subtext = subtext;
			requestLayout();
		}
		
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			// ignore attributes
			int w = 0;
			int h = 0;
			if (text != null) {
				if(imageDrawable != null) {
					w += imageDrawable.getMinimumWidth();
				}
				w += leftMargin;
				w += textPaint.measureText(text);
				if (subtext != null) {
					w += subtextPaint.measureText(subtext) + 2 * scaleCoefficient;
				}
				
				h = (int) (5 * scaleCoefficient + Math.max(textPaint.getTextSize(), subtextPaint.getTextSize()));
				if(imageDrawable != null) {
					h = Math.max(h, (int)(imageDrawable.getMinimumHeight()));
				}
			}
			setMeasuredDimension(w, h);
		}
		
		@Override
		protected void onDraw(Canvas cv) {
			super.onDraw(cv);
			if (isVisible()) {
				int margin = 0;
				if(imageDrawable != null) {
					imageDrawable.draw(cv);
					margin = imageDrawable.getBounds().width();
				}
				margin += leftMargin;
				cv.drawText(text, margin + getLeft(), getBottom() - 3 * scaleCoefficient, textPaint);
				if (subtext != null) {
					cv.drawText(subtext, getLeft() + margin + 2 * scaleCoefficient + textPaint.measureText(text), getBottom()
							- 3 * scaleCoefficient, subtextPaint);
				}
			}
		}
		
		public boolean isVisible() {
			return text != null && text.length() > 0;
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
