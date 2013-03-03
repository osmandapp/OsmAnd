package net.osmand.plus.planning;
/**
 * 
 * Used by the planning plugin to:
 * 1. Display a track of manually placed sequential locations (points) on a map.
 * 2. Drag manually placed points to new locations.
 * 3. Display the total/partial distance of a manually generated track.
 * 4. Establish and control control buttons for zooming the screen display.
 * 
 * @author Car Michael
 */

import java.util.ArrayList;
import java.util.List;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.planning.PlanningPlugin;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.text.TextPaint;
import android.widget.Button;

public class PlanningLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider{
	
	private List<Object> selectedObjects = new ArrayList<Object>();
	private OsmandMapTileView view;
	private Bitmap targetIcon;		//Bitmap to use in measurement mode
	private Paint locationPaint;
	private Paint measurementPaint;	//For measurement point plotting
	private Paint tempPaint;	//For dynamic changes to measurement points
	private Paint trailingPointsPaint;	//For measurement points following selected point
	private DisplayMetrics dm;
	public TextView textView;
	private final int BASE_TEXT_SIZE = 170;
	private final int SHADOW_OF_LEG = 5;
	private final int CLOSE_BTN = 6;
	private Drawable boxLeg;
	private float scaleCoefficient = 1;
	private Rect textPadding;
	private LatLon latLon;
	private LatLon tempPoint;
	private LatLon tempPoint2;	
	private boolean newPointFlag = true;	//Used to block new distance point creation for menu or text box clicks
	private boolean scrollingFlag = false;		//For measurement point dragging
	private OsmandApplication application;
	private MapActivity activity;
	private PlanningPlugin planningPlugin;
	public boolean measureDistanceMode = false;	//Status of distance measurement mode
	public int measurementPointInsertionIndex = -1;	//distance measurement point array insertion index
	public int selectedMeasurementPointIndex = -1;	//For distance measurement point display
	public int colourChangeIndex = -1;	//to support distance measurement point display colours
	public boolean longInfoFlag = false;	//to provide control of info provided for measurement points
	private float cumMeasuredDistance=0;	//Distance along path for distance measurement mode
	public List<LatLon> measurementPoints = new ArrayList<LatLon>();	//Path points for distance measurement
	private LatLon screenPointLatLon;		//To provide access to current point lat/lon
	public final int maxMeasurementPointDisplayRadius = 11;
	public final int minMeasurementPointDisplayRadius = 3;
	public final int maxMeasurementPointSelectionRadius = 21;
	public final int minMeasurementPointSelectionRadius = 3;
	public final int defaultMeasurementPointDisplayRadius = 7;
	public final int defaultMeasurementPointSelectionRadius = 11;
	private Button displayZoomInButton;
	private Button displayZoomOutButton;
	public final float MAX_DISPLAY_FACTOR = 4.0f;	//display zoom limits
	public final float MIN_DISPLAY_FACTOR = 1.0f;
	private final int DEFAULT_TEXT_SIZE = 14;
	private ImageView closeButton;
	private String description;
	private LayoutParams lp;
	private TextPaint zoomTextPaint;
	private int baseTextSize = 0;
	private int closeBtn = 0;
	private int shadowOfLeg = 0;
	
	public PlanningLayer(MapActivity activity){
		this.activity = activity;
		application = (OsmandApplication)activity.getApplication();
	}

	private void initUI() {
		locationPaint = new Paint();
		locationPaint.setAntiAlias(true);
		locationPaint.setFilterBitmap(true);
		locationPaint.setDither(true);
		
		measurementPaint = new Paint();	//For drawing measurement points
		measurementPaint.setColor(Color.RED);
		measurementPaint.setAlpha(120);
		measurementPaint.setStrokeWidth(2);
		measurementPaint.setAntiAlias(true);
		
		trailingPointsPaint = new Paint();
		trailingPointsPaint.setColor(Color.BLUE);
		trailingPointsPaint.setAlpha(120);
		trailingPointsPaint.setAntiAlias(true);
		trailingPointsPaint.setStrokeWidth(2);		
	}

	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		scaleCoefficient  = dm.density;
		if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
			//for large screen
			scaleCoefficient *= 1.5f;
		}
		
		if(planningPlugin.getMeasurementPointSelectionRadius() < minMeasurementPointSelectionRadius ||
				planningPlugin.getMeasurementPointSelectionRadius() > maxMeasurementPointSelectionRadius){
			planningPlugin.setMeasurementPointSelectionRadius(defaultMeasurementPointSelectionRadius);
		}
		if(planningPlugin.getMeasurementPointDisplayRadius() < minMeasurementPointSelectionRadius ||
				planningPlugin.getMeasurementPointDisplayRadius() > maxMeasurementPointDisplayRadius){
			planningPlugin.setMeasurementPointDisplayRadius(defaultMeasurementPointDisplayRadius);
		}
			
		baseTextSize = (int) (BASE_TEXT_SIZE * scaleCoefficient);
		shadowOfLeg = (int) (SHADOW_OF_LEG * scaleCoefficient);
		closeBtn = (int) (CLOSE_BTN * scaleCoefficient);	
		boxLeg = view.getResources().getDrawable(R.drawable.box_leg);
		boxLeg.setBounds(0, 0, boxLeg.getMinimumWidth(), boxLeg.getMinimumHeight());	
		textView = new TextView(view.getContext());
		lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		textView.setLayoutParams(lp);
		textView.setTextSize(DEFAULT_TEXT_SIZE);
		textView.setTextColor(Color.argb(255, 0, 0, 0));
		textView.setMinLines(1);
		textView.setGravity(Gravity.CENTER_HORIZONTAL);		
		textView.setClickable(true);		
		textView.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
		textPadding = new Rect();
		textView.getBackground().getPadding(textPadding);		
		FrameLayout parent = (FrameLayout) view.getParent();
		initZoomButtons(view, parent);
		
		closeButton = new ImageView(view.getContext());
		lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		closeButton.setLayoutParams(lp);
		closeButton.setImageDrawable(view.getResources().getDrawable(R.drawable.headliner_close));
		closeButton.setClickable(true);
		int minw = closeButton.getDrawable().getMinimumWidth();
		int minh = closeButton.getDrawable().getMinimumHeight();
		closeButton.layout(0, 0, minw, minh);
		
		if(latLon != null){
			setLocation(latLon);
		}
	}
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		if(!planningPlugin.getPlanningMode() || view == null){
			return;
		}
		
		targetIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.info_target);	//bitmap to use in measurement mode
		float displayScale = planningPlugin.getDisplayScaleFactor();	//additional scaling for selected display zoom
		int size = 0;	
		int locationX = 0;
		int locationY = 0;
		int index = colourChangeIndex;
		size = measurementPoints.size();
		if(size > 0){
			for (int i = 0;i< size; i++){
				locationX = view.getMapXForPoint(measurementPoints.get(i).getLongitude());
				locationY = view.getMapYForPoint(measurementPoints.get(i).getLatitude());
				if(i==0){
					canvas.drawBitmap(targetIcon, locationX - targetIcon.getWidth()/5, locationY - targetIcon.getHeight(), locationPaint);
				}else{	//Change drawing colour if there is a point insertion or movement
					if(i > index && index >= 0){
						tempPaint = trailingPointsPaint;
					}else{
						tempPaint = measurementPaint;
					}
					if (!application.getSettings().USE_HIGH_RES_MAPS.get()){	//only scale if user has not maintained hi-res settings
						tempPaint.setStrokeWidth(displayScale * 2);
					}else{
						tempPaint.setStrokeWidth(2);				
					}
					canvas.drawLine(view.getMapXForPoint(measurementPoints.get(i-1).getLongitude()),
							view.getMapYForPoint(measurementPoints.get(i-1).getLatitude()), locationX, locationY, tempPaint);
					canvas.drawCircle(locationX, locationY, planningPlugin.getMeasurementPointDisplayRadius() * scaleCoefficient, tempPaint);
				}
			}
		}
		
		if(latLon != null){
			int x = view.getRotatedMapXForPoint(latLon.getLatitude(), latLon.getLongitude());
			int y = view.getRotatedMapYForPoint(latLon.getLatitude(), latLon.getLongitude());
			
			int tx = x - boxLeg.getMinimumWidth() / 2;
			int ty = y - boxLeg.getMinimumHeight() + shadowOfLeg;
			canvas.translate(tx, ty);
			boxLeg.draw(canvas);
			canvas.translate(-tx, -ty);
			
			if (textView.getText().length() > 0) {
				if(!longInfoFlag){	//adjust info textbox size depending on message type
					textView.setGravity(Gravity.LEFT);
				}else{
					textView.setGravity(Gravity.CENTER_HORIZONTAL);
				}
				canvas.translate(x - textView.getWidth() / 2, ty - textView.getBottom() + textPadding.bottom - textPadding.top);
				textView.draw(canvas);
				canvas.translate(textView.getWidth() - closeButton.getWidth(), closeBtn / 2);
				closeButton.draw(canvas);					
			}
		}
	}
	
	@Override
	public void destroyLayer() {	
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	private void layoutText(int lines) {
		Rect padding = new Rect();
		if (lines > 0) {
			textView.getBackground().getPadding(padding);
		}

		textView.setTextSize(DEFAULT_TEXT_SIZE + (int)(1.5f * planningPlugin.getDisplayScaleFactor()));	//partially scale text size

		int w = 0;
		if(!longInfoFlag){	//adjust info textbox size depending on message type
			w = (int)textView.getPaint().measureText((String) textView.getText()) + closeButton.getDrawable().getMinimumWidth() + 10;
		}else{
			w = baseTextSize;
		}
		int h = (int) ((textView.getPaint().getTextSize() + 4) * lines);
		textView.layout(0, -padding.bottom, w, h + padding.top + 10);
	}
	
	/**
	 * Method to:
	 * 1. Add new points to measurement track
	 * 2. Determine information to be displayed for the selected point
	 */
	public void setLocation(LatLon loc){
		int index = selectedMeasurementPointIndex;	//For measurement point distance display
		int lines = 0;
		latLon = loc;
		if(latLon != null){			
			if(!planningPlugin.getPlanningMode()){	//Test for distance measurement mode
				description = view.getContext().getString(R.string.point_on_map, 
						latLon.getLatitude(), latLon.getLongitude());
			}else{	//display cumulative measurement data
				if(newPointFlag){	//do not add point for menu activity
					if(measurementPointInsertionIndex < 0) {
						measurementPoints.add(latLon);
						index = measurementPoints.size() - 1;
					}else{
						measurementPoints.add(measurementPointInsertionIndex, latLon);
						index = measurementPointInsertionIndex;
						measurementPointInsertionIndex = -1;		//clear insertion flag
					}
					colourChangeIndex = index;	//To assist point display colour coding
					selectedMeasurementPointIndex = index;
					newPointFlag = false;
				}
				if(index == 0){
					if(longInfoFlag){
						description = view.getContext().getString(R.string.start_point) +"\n Lat: " + String.format("%3.6f",
								measurementPoints.get(0).getLatitude()) + "\nLon: " + 
								String.format("%3.6f", measurementPoints.get(0).getLongitude());;
						lines = 3;							
					}else{
						description = view.getContext().getString(R.string.start_point);								
						lines = 1;
					}
				}else{
					cumMeasuredDistance = calculatePathDistance(index);
					String ds = OsmAndFormatter.getFormattedDistance(cumMeasuredDistance, application);
					if(longInfoFlag){
						description = view.getContext().getString(R.string.point_on_map,
								measurementPoints.get(index).getLatitude(),
								measurementPoints.get(index).getLongitude()) + '\n' + "Dist.: " + ds;
						lines = 4;
					}else{
						description = ds;
						lines = 1;
					}
				}
			}
			textView.setText(description);
		} else {
			textView.setText("");
		}
		layoutText(lines);
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		if(!planningPlugin.getPlanningMode()) return false;		
		if (!view.getSettings().SCROLL_MAP_BY_GESTURES.get()) {
			if (!selectedObjects.isEmpty())
				view.showMessage(activity.getNavigationHint(latLon));
			return true;
		}		
		newPointFlag = true;	//Enable new measurement point creation
		if(pressedInTextView(point.x, point.y) == 2){	//close button pressed
			setLocation(null);
			view.refreshMap();
			return true;
		}
		if(pressedInTextView(point.x, point.y) == 1) return true;	//pressed in text box				
		LatLon latLon = view.getLatLonFromScreenPoint(point.x, point.y);
		StringBuilder description = new StringBuilder(); 
		
		if (!selectedObjects.isEmpty()) {
			if (selectedObjects.size() > 1) {
				description.append("1. ");
			}
			description.append(getObjectDescription(selectedObjects.get(0)));
			for (int i = 1; i < selectedObjects.size(); i++) {
				description.append("\n" + (i + 1) + ". ").append(getObjectDescription(selectedObjects.get(i)));
			}
			LatLon l = getObjectLocation(selectedObjects.get(0));
			if (l != null) {
				latLon = l;
			}
		}		
		setLocation(latLon);
		view.refreshMap();
		return true;
	}

	@Override
	public boolean onSingleTap(PointF point) {
		if (!planningPlugin.getPlanningMode()) return false;
		if(pressedInTextView(point.x, point.y) > 0){	//Test if a measurement point text box has been clicked
			if(pressedInTextView(point.x, point.y) == 2){
				setLocation(null);	//close button was selected
				view.refreshMap();
				return true;
			}
			if(selectedMeasurementPointIndex >= 0){	//only set if there has been a change
				setScreenPointLatLon (measurementPoints.get(selectedMeasurementPointIndex));
			}
			planningPlugin.createMeasurementMenu();
		}else{	//text box not clicked
		//Test if a measurement point has been clicked
			int pointIndex = isMeasurementPointSelected(point, scaleCoefficient);
			if(pointIndex >= 0){
				selectedMeasurementPointIndex = pointIndex;	//only save index of point if one has been selected 
				saveScreenPoint();
				planningPlugin.createMeasurementMenu();
			}else{
				setScreenPointLatLon (view.getLatLonFromScreenPoint(point.x, point.y));	//point or text not selected
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (!planningPlugin.getPlanningMode()) return false;		//Check if in measurement mode
		if(scrollingFlag) return true;	//delay activity until scrolling has finished
		PointF point = new PointF(e1.getX(), e1.getY());
		int pointIndex = isMeasurementPointSelected(point, scaleCoefficient);
		if(pointIndex >= 0){
			selectedMeasurementPointIndex = pointIndex;	//save index of point selected	
			scrollingFlag = true;	//must remain true until next touch event
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (!planningPlugin.getPlanningMode()) return false;	//Check if in measurement mode
		if(scrollingFlag) return true;	//block fling while dragging a point. Note scroll event occurs before fling event
		return false;
	}

	/**
	 * Method to check if an information text box is displayed at the screen point tapped.
	 */
	public int pressedInTextView(float px, float py) {
		if (latLon != null) {
			Rect bs = textView.getBackground().getBounds();
			Rect closes = closeButton.getDrawable().getBounds();
			int x = (int) (px - view.getRotatedMapXForPoint(latLon.getLatitude(), latLon.getLongitude()));
			int y = (int) (py - view.getRotatedMapYForPoint(latLon.getLatitude(), latLon.getLongitude()));
			x += bs.width() / 2;
			y += bs.height() + boxLeg.getMinimumHeight() - shadowOfLeg;
			int dclosex = x - bs.width() + closeBtn + closes.width();
			int dclosey = y + closeBtn;
			if(closes.intersects(dclosex - closeBtn, dclosey - closeBtn, dclosex + closeBtn, dclosey + closeBtn)) {
				return 2;
			} else if (bs.contains(x, y)) {
				return 1;
			}
		}
		return 0;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(!planningPlugin.getPlanningMode())return false;	

		if (event.getAction() == MotionEvent.ACTION_DOWN) {	//must clear at start of new event, not end of last event to ensure fling is blocked
			scrollingFlag = false;
		}
		
		if (event.getAction() == MotionEvent.ACTION_UP) {	//Support for dragging measurement point
			if(scrollingFlag){
				if(selectedMeasurementPointIndex >= 0){	//move selected point to new location
					measurementPoints.set(selectedMeasurementPointIndex,view.getLatLonFromScreenPoint(event.getX(), event.getY()));
					colourChangeIndex = selectedMeasurementPointIndex;
					displayIntermediatePointInfo(colourChangeIndex);
					saveScreenPoint();
					
					view.refreshMap();
				}
			}
		}
					
		if (latLon != null) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if(pressedInTextView(event.getX(), event.getY()) > 0){
					textView.setPressed(true);
					view.refreshMap();
				}
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
			if(textView.isPressed()) {
				textView.setPressed(false);
				view.refreshMap();
			}
		}
		return false;
	}
	
	/**
	 * Method to calculate the length of the measurement track between the 
	 * start point and the point selected.
	 */
	public void displayIntermediatePointInfo(int index){	//Display info for intermediate measurement points
		int lines = 0;
		String description ="";
		latLon = measurementPoints.get(index);
		if (index == 0){
			if(longInfoFlag){
				description = view.getContext().getString(R.string.start_point) + ": \nLat: " + String.format("%3.6f",
						measurementPoints.get(index).getLatitude()) + String.format("\nLon: %3.6f", measurementPoints.get(index).getLongitude());					
				lines = 3;
			}else{
				description = view.getContext().getString(R.string.start_point);								
				lines = 1;
			}
		}else{
			cumMeasuredDistance = calculatePathDistance(index);
			if(longInfoFlag){
				description = view.getContext().getString(R.string.point_on_map,
						measurementPoints.get(index).getLatitude(),
						measurementPoints.get(index).getLongitude()) + '\n' + "Dist.: " + 
						OsmAndFormatter.getFormattedDistance(cumMeasuredDistance, application);
				lines = 4;
			}else{
				description = OsmAndFormatter.getFormattedDistance(cumMeasuredDistance, application);
				lines = 1;
			}
		}
		textView.setText(description);
		layoutText(lines);
	}
	
	/**
	 * Method to calculate the total length of the measurement track between the 
	 * start point and the end point.
	 */
	public float calculatePathDistance(int index){	//Calculate distance between points
		// index is the array index of the last point to include in the calculation
		float[] calculatedDistance = new float[1];
		float distance = 0;
		for (int i = 1; i <= index; i++){
			tempPoint = new LatLon(0, 0);	//temporary object to save to measurement point list.
			tempPoint2 = new LatLon(0,0);	//temporary object to save to measurement point list.
			tempPoint = measurementPoints.get(i - 1);
			tempPoint2 = measurementPoints.get(i);
			Location.distanceBetween(tempPoint.getLatitude(), tempPoint.getLongitude(), tempPoint2.getLatitude(),
					tempPoint2.getLongitude(), calculatedDistance);
			distance += calculatedDistance[0];
		}
		return distance;
	}
	
	public void setNewPointFlag(boolean status){	//Controls new measurement point creation during menu activity
		newPointFlag = status;
	}
	
	public boolean getNewPointFlag(){
		return newPointFlag;
	}

	public void setCumMeasuredDistance(float dist){	//distance measured
		cumMeasuredDistance=dist;
	}

	public float getCumMeasuredDistance(){	//access distance measured
		return cumMeasuredDistance;
	}

	public LatLon getScreenPointLatLon(){	//accessor for local point lat lon storage
		return screenPointLatLon;
	}

	public void setScreenPointLatLon(LatLon point) {	//Save current screen point values
		screenPointLatLon = point;
	}
        
	public void saveScreenPoint() {	//Save current screen point
		screenPointLatLon = measurementPoints.get(selectedMeasurementPointIndex);
	}
        
	public boolean checkMeasurementPointSize(int radius){	//check if proposed change to measurement point display size is within defined range
		 return radius <= maxMeasurementPointDisplayRadius && radius >= minMeasurementPointDisplayRadius;
	}
	
	public boolean checkMeasurementPointSelectionSize(int radius){	//Check if proposed change to measurement point selection area is within defined range
		return (radius <= maxMeasurementPointSelectionRadius && radius >= minMeasurementPointSelectionRadius);
	}

	/**
	 * Method to determine if a measurement track point exists within the 
	 * defined selection radius at the screen location tapped.
	 */
	public int isMeasurementPointSelected(PointF point, float scaleCoefficient){	//test if point on map is a point in measurement set
		int locationX = 0;
		int locationY = 0;
		int index = -1;	//indicates no match found
		int size = measurementPoints.size();
		if(size > 0){
			for (int i = 0;i < size; i++){
				locationX = view.getMapXForPoint(measurementPoints.get(i).getLongitude());
				locationY = view.getMapYForPoint(measurementPoints.get(i).getLatitude());
				if(Math.abs(locationX - point.x) < planningPlugin.getMeasurementPointSelectionRadius() * scaleCoefficient &&
						Math.abs(locationY - point.y) < planningPlugin.getMeasurementPointSelectionRadius() * scaleCoefficient){
					index = i;	//a point has been found in the detection area
					break;
				}
			}
		}
		return index;
	}
	
	public int getSelectedMeasurementPointIndex(){	//Save measurement point selected
		return selectedMeasurementPointIndex;
	}
	
	public void setSelectedMeasurementPointIndex(int index){	//Access selected measurement point index
		selectedMeasurementPointIndex = index;
	}
	
	public void setLongInfoFlag(boolean status){	//Support distance measuring info length selection
		longInfoFlag = status;
	}

	public boolean getLongInfoFlag(){	//Support distance measuring info length selection
		return longInfoFlag;
	}

	public int getColourChangeIndex(){	//Indicate measurement point where display colour should change
		return colourChangeIndex;
	}
	
	public void setColourChangeIndex(int index){	//indicate measurement point where display colour should change
		colourChangeIndex = index;
	}
	
	public int getMeasurementPointInsertionIndex() {	//Access distance measurement point insertion
		return measurementPointInsertionIndex;
	}

	public void setMeasurementPointInsertionIndex(int index) {	//Set distance measurement point insertion
		measurementPointInsertionIndex = index;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, List<Object> o) {
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return latLon;
	}
	
	@Override
	public String getObjectDescription(Object o) {
		return "Planning Plugin";
	}
	
	@Override
	public String getObjectName(Object o) {
		return view.getContext().getString(R.string.osmand_planning_plugin_name);
	}
	
	/**
	 * Method to establish display zoom control buttons on the layer.
	 */
	private void initZoomButtons(final OsmandMapTileView view, FrameLayout parent) {
		int minimumWidth = view.getResources().getDrawable(R.drawable.map_scale_up_button).getMinimumWidth();
		
		ImageView bottomShadow = new ImageView(view.getContext());
		bottomShadow.setBackgroundResource(R.drawable.bottom_shadow);
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM);
		params.setMargins(0, 0, 0, 0);
		parent.addView(bottomShadow, params);
		
		zoomTextPaint = new TextPaint();
		zoomTextPaint.setTextSize(18 * scaleCoefficient);
		zoomTextPaint.setAntiAlias(true);
		zoomTextPaint.setFakeBoldText(true);		
		
		displayZoomInButton = new Button(view.getContext());
		displayZoomInButton.setBackgroundResource(R.drawable.map_scale_up_button);
		displayZoomInButton.getBackground().setAlpha(120);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.LEFT);
		params.setMargins(0, 3*minimumWidth, 0, 0);
		parent.addView(displayZoomInButton, params);
		
		displayZoomOutButton = new Button(view.getContext());
		displayZoomOutButton.setBackgroundResource(R.drawable.map_scale_down_button);
		displayZoomOutButton.getBackground().setAlpha(120);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.LEFT);
		params.setMargins(0, 4*minimumWidth , 0, 0);
		parent.addView(displayZoomOutButton, params);
		planningPlugin.setMapDisplayZoomButtonVisibility(true);
		
		displayZoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				float factor = planningPlugin.getDisplayScaleFactor();
				if(!application.getSettings().USE_HIGH_RES_MAPS.get()){
					if(factor < MAX_DISPLAY_FACTOR){
						planningPlugin.setDisplayScaleFactor(factor + 0.5f);
						planningPlugin.setDisplayScaleChangedFlag(true);
						if(textView.getLineCount() > 0) layoutText(textView.getLineCount());
						view.refreshMap(true);
					}
				}
			}
		});
		
		displayZoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				float factor = planningPlugin.getDisplayScaleFactor();
				if(!application.getSettings().USE_HIGH_RES_MAPS.get()){
					if(factor > MIN_DISPLAY_FACTOR){
						planningPlugin.setDisplayScaleFactor(factor - 0.5f);
						planningPlugin.setDisplayScaleChangedFlag(true);
						if(textView.getLineCount() > 0) layoutText(textView.getLineCount());
						view.refreshMap(true);
					}
				}
			}
		});
		if(!planningPlugin.getMapDisplayZoomButtonVisibility()) hideDisplayZoomButtons();
	}
	
	public void hideDisplayZoomButtons(){
		displayZoomInButton.setVisibility(View.INVISIBLE);
		displayZoomOutButton.setVisibility(View.INVISIBLE);
		planningPlugin.setMapDisplayZoomButtonVisibility(false);
	}
	
	public void showDisplayZoomButtons(){
		displayZoomInButton.setVisibility(View.VISIBLE);
		displayZoomOutButton.setVisibility(View.VISIBLE);
		planningPlugin.setMapDisplayZoomButtonVisibility(true);		
	}
	
	public OsmandMapTileView getPlanningView() {
		return view;
	}
	
	public void setPlanningPlugin(PlanningPlugin planningPlugin){
		this.planningPlugin = planningPlugin;
	}
	
	public PlanningPlugin getPlanningPlugin(){
		return planningPlugin;
	}
	
	/**
	 * Method to replace the measurement track points with points loaded from a GPX file.
	 */
	public void showGPXPlan(GPXFile data){
		LatLon latLon = null;
		if(data != null){
			measurementPoints.clear();
	    	for (int i = 0; i < data.points.size(); i++){
	    		latLon = new LatLon(data.points.get(i).lat, data.points.get(i).lon);
	    		measurementPoints.add(latLon);
	    	}
			colourChangeIndex = measurementPoints.size() - 1;
			selectedMeasurementPointIndex = colourChangeIndex;
			displayIntermediatePointInfo(colourChangeIndex);
		}
		view.refreshMap(true);
    }
}
