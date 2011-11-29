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
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
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

	private final MapActivity map;
	private final RouteLayer routeLayer;
	private OsmandMapTileView view;
	
	private boolean showMiniMap = false;
	private boolean showArrivalTime = true;
	
	private Paint paintBlack;
	private Paint paintText;
	private Paint paintSubText;
	private Paint paintMiniRoute;
	private Paint fillBlack;
	private Paint fillRed;
	private Paint paintRouteDirection;
	
	private long cachedLeftTime = 0;
	private float[] calculations = new float[1];
	private int cachedMeters = 0;
	private float cachedSpeed = 0;
	private float cachedRotate = 0;
	
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
	private NextTurnInfoControl nextTurnInfo;
	
	private ImageView backToLocation;
	private ImageView compassView;
	private View progressBar;
	
	// groups
	private List<MapInfoControl> leftControls = new ArrayList<MapInfoLayer.MapInfoControl>();
	private List<MapInfoControl> rightControls = new ArrayList<MapInfoLayer.MapInfoControl>();
	private ViewGroup statusBar;
	private Drawable statusBarBackground;
	
	// currently pressed view
	private View pressedView = null;
	
	
	
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
		

		nextTurnInfo = new NextTurnInfoControl(R.drawable.box_top, paintText, paintSubText);
		nextTurnInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMiniMap = !showMiniMap;
				view.refreshMap();
			}
		});
		leftControls.add(nextTurnInfo);
		showArrivalTime = view.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME.get();
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
		// prepare data (left distance, speed, compass)
		updateDistanceToGo();
		updateSpeedInfo();
		updateTimeLeftInfo();
		if(view.getRotate() != cachedRotate) {
			cachedRotate = view.getRotate();
			compassView.invalidate();
		}
		if(nextTurnInfo.isVisible() != (nextTurnInfo.getMeasuredHeight() > 0)) {
			relayoutLeftControls(nextTurnInfo);
		}
		
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
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		compassView = new ImageView(view.getContext()) {
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
		params.leftMargin = (int) (10 * scaleCoefficient);
		params.rightMargin = (int) (1 * scaleCoefficient);
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
	
	public class NextTurnInfoControl extends MapInfoControl {
		
		private final float scaleMiniRoute = 0.15f;
		private final float width = 96 * scaleCoefficient;
		private final float height = 96 * scaleCoefficient;
		
		private final float centerMiniRouteY = 3 * height /4;
		private final float centerMiniRouteX = width / 2;
		
		private Path pathForTurn = new Path();
		private Matrix pathTransform = new Matrix();
		
		private TurnType cachedTurnType = null;
		private String cachedExitOut = null;
		private final Paint textPaint;
		private final Paint subtextPaint;

		public NextTurnInfoControl(int background, Paint textPaint, Paint subtextPaint) {
			super(background);
			this.textPaint = textPaint;
			this.subtextPaint = subtextPaint;
		}
		
		public boolean isVisible() {
			// TODO
			if (routeLayer != null && routeLayer.getHelper().isRouterEnabled() /*&& routeLayer.getHelper().isFollowingMode()*/) {
				int d = routeLayer.getHelper().getDistanceToNextRouteDirection();
				if (d > 0 || (showMiniMap && !routeLayer.getPath().isEmpty())) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			super.onLayout(changed, left, top, right, bottom);
			pathTransform = new Matrix();
			pathTransform.postScale(scaleCoefficient, scaleCoefficient);
			pathTransform.postTranslate(left, top);
		}
		
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			if(!isVisible()) {
				setMeasuredDimension(0, 0);
			} else if(showMiniMap){
				setMeasuredDimension((int) width, (int) height);
			} else {
				int h = (int) (5 * scaleCoefficient + Math.max(textPaint.getTextSize(), subtextPaint.getTextSize()));
				setMeasuredDimension((int) width, (int) height + h);
			}
		}
		
		
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			// TODO
			if (routeLayer != null /*&& routeLayer.getHelper().isFollowingMode()*/) {
				int d = routeLayer.getHelper().getDistanceToNextRouteDirection();
				if (showMiniMap || d == 0) {
					if (!routeLayer.getPath().isEmpty()) {
						canvas.save();
						canvas.translate(centerMiniRouteX - view.getCenterPointX(), centerMiniRouteY - view.getCenterPointY());
						canvas.scale(scaleMiniRoute, scaleMiniRoute, view.getCenterPointX(), view.getCenterPointY());
						canvas.rotate(view.getRotate(), view.getCenterPointX(), view.getCenterPointY());
						canvas.drawCircle(view.getCenterPointX(), view.getCenterPointY(), 3 / scaleMiniRoute, fillBlack);
						canvas.drawPath(routeLayer.getPath(), paintMiniRoute);
						canvas.restore();
					}
				} else {
					RouteDirectionInfo next = routeLayer.getHelper().getNextRouteDirectionInfo();
					if (next != null) {
						if (!Algoritms.objectEquals(cachedTurnType, next.turnType)) {
							cachedTurnType = next.turnType;
							TurnPathHelper.calcTurnPath(pathForTurn, cachedTurnType, pathTransform);
							if (cachedTurnType.getExitOut() > 0) {
								cachedExitOut = cachedTurnType.getExitOut() + ""; //$NON-NLS-1$
							} else {
								cachedExitOut = null;
							}
						}
						canvas.drawPath(pathForTurn, paintRouteDirection);
						canvas.drawPath(pathForTurn, paintBlack);
						// TODO test
						if (cachedExitOut != null) {
							canvas.drawText(cachedExitOut, (getLeft() + getRight()) / 2 - 6 * scaleCoefficient, (getTop() + getBottom())
									/ 2 - 9 * scaleCoefficient, paintBlack);
						}
						
						String text = OsmAndFormatter.getFormattedDistance(cachedMeters, map);
						String subtext = null;
						int ls = text.lastIndexOf(' ');
						if (ls != -1) {
							subtext = text.substring(ls + 1);
							text = text.substring(0, ls);
						}
						// TODO align center
						int margin = (int) (10 * scaleCoefficient);
						canvas.drawText(text, margin + getLeft(), getBottom() - 3 * scaleCoefficient, textPaint);
						if (subtext != null) {
							canvas.drawText(subtext, getLeft() + margin + 2 * scaleCoefficient + textPaint.measureText(text), getBottom()
									- 3 * scaleCoefficient, subtextPaint);
						}
					}
				}
			}
		}
	}



}
