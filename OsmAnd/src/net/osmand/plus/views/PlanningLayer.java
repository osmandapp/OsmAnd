package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MeasurementActivity;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

public class PlanningLayer extends OsmandMapLayer{

	public interface IContextMenuProvider {
		
		public void collectObjectsFromPoint(PointF point, List<Object> o);
		
		public LatLon getObjectLocation(Object o);
		
		public String getObjectDescription(Object o);
		
		public String getObjectName(Object o);
		
		public DialogInterface.OnClickListener getActionListener(List<String> actionsList, Object o);
	}
	
	private IContextMenuProvider selectedContextProvider;
	private List<Object> selectedObjects = new ArrayList<Object>();
	private OsmandMapTileView view;
	private final MeasurementActivity measurementActivity;
	
	private Bitmap targetIcon;		//Bitmap to use in measurement mode
	private Paint locationPaint;
	private Paint measurementPaint;	//For measurement point plotting
	private Paint tempPaint;	//For dynamic changes to measurement points
	private Paint trailingPointsPaint;	//For measurement points following selected point
	private DisplayMetrics dm;
	private TextView textView;
	private int BASE_TEXT_SIZE = 170;
	private int SHADOW_OF_LEG = 5;
	private Drawable boxLeg;
	private float scaleCoefficient = 1;
	private Rect textPadding;

	private LatLon latLon;
	private LatLon tempPoint;	//For measurement mode support
	private LatLon tempPoint2;	//For measurement mode support
	
	private boolean newPointFlag = true;	//Used to block new distance point creation for menu or text box clicks
	private boolean scrollingFlag = false;		//For measurement point dragging
	
	public PlanningLayer(MapActivity activity){
		measurementActivity = activity.getMeasurementActivity();
	}

	private void initUI() {
		locationPaint = new Paint();
		locationPaint.setAntiAlias(true);
		locationPaint.setFilterBitmap(true);
		locationPaint.setDither(true);
		
		measurementPaint = new Paint();	//For drawing measurement points
		measurementPaint.setColor(Color.RED);
		measurementPaint.setAlpha(80);
		measurementPaint.setStrokeWidth(2);
		measurementPaint.setAntiAlias(true);
		
		trailingPointsPaint = new Paint();
		trailingPointsPaint.setColor(Color.BLUE);
		trailingPointsPaint.setAlpha(80);
		trailingPointsPaint.setAntiAlias(true);
		trailingPointsPaint.setStrokeWidth(1);
					
	}

	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		scaleCoefficient  = dm.density;
		if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
			// large screen
			scaleCoefficient *= 1.5f;
		}
		BASE_TEXT_SIZE = (int) (BASE_TEXT_SIZE * scaleCoefficient);
		SHADOW_OF_LEG = (int) (SHADOW_OF_LEG * scaleCoefficient);
		
		boxLeg = view.getResources().getDrawable(R.drawable.box_leg);
		boxLeg.setBounds(0, 0, boxLeg.getMinimumWidth(), boxLeg.getMinimumHeight());
		
		textView = new TextView(view.getContext());
		LayoutParams lp = new LayoutParams(BASE_TEXT_SIZE, LayoutParams.WRAP_CONTENT);
		textView.setLayoutParams(lp);
		textView.setTextSize(15);
		textView.setTextColor(Color.argb(255, 0, 0, 0));
		textView.setMinLines(1);
		textView.setGravity(Gravity.CENTER_HORIZONTAL);
		
		textView.setClickable(true);
		
		textView.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
		textPadding = new Rect();
		textView.getBackground().getPadding(textPadding);
	}
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		if(!measurementActivity.getMeasureDistanceMode() || view == null){
			return;
		}
		
		targetIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.info_target);	//bitmap to use in measurement mode

		int size = 0;	
		int locationX = 0;
		int locationY = 0;
		int index = measurementActivity.getColourChangeIndex();
		size = measurementActivity.measurementPoints.size();
		if(size > 0){
			for (int i=0;i< size; i++){
				locationX=view.getMapXForPoint(measurementActivity.measurementPoints.get(i).getLongitude());
				locationY=view.getMapYForPoint(measurementActivity.measurementPoints.get(i).getLatitude());
				if(i==0){
					canvas.drawBitmap(targetIcon, locationX - targetIcon.getWidth()/5, locationY - targetIcon.getHeight(), locationPaint);
				}else{	//Change drawing colour if there is a point insertion or movement
					if(i > index && index >= 0){
						tempPaint = trailingPointsPaint;
					}else{
						tempPaint = measurementPaint;
					}
					canvas.drawLine(view.getMapXForPoint(measurementActivity.measurementPoints.get(i-1).getLongitude()),
							view.getMapYForPoint(measurementActivity.measurementPoints.get(i-1).getLatitude()), locationX, locationY, tempPaint);
					canvas.drawCircle(locationX, locationY, measurementActivity.getMeasurementPointRadius() * dm.density, tempPaint);
				}
			}
		}
		
		if(latLon != null){
			int x = view.getRotatedMapXForPoint(latLon.getLatitude(), latLon.getLongitude());
			int y = view.getRotatedMapYForPoint(latLon.getLatitude(), latLon.getLongitude());
			
			int tx = x - boxLeg.getMinimumWidth() / 2;
			int ty = y - boxLeg.getMinimumHeight() + SHADOW_OF_LEG;
			canvas.translate(tx, ty);
			boxLeg.draw(canvas);
			canvas.translate(-tx, -ty);
			
			if (textView.getText().length() > 0) {
				canvas.translate(x - textView.getWidth() / 2, ty - textView.getBottom() + textPadding.bottom - textPadding.top);
				int c = textView.getLineCount();
				
				textView.draw(canvas);
				if (c == 0) {
					// special case relayout after on draw method
					layoutText();
					view.refreshMap();
				}
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

	private void layoutText() {
		Rect padding = new Rect();
		if (textView.getLineCount() > 0) {
			textView.getBackground().getPadding(padding);
		}
		int w = BASE_TEXT_SIZE;
		int h = (int) ((textView.getPaint().getTextSize() + 4) * textView.getLineCount());
		
		textView.layout(0, -padding.bottom, w, h + padding.top);
	}
	
	public void setLocation(LatLon loc, String description){
		int index = measurementActivity.getSelectedMeasurementPointIndex();	//For measurement point distance display
		latLon = loc;
		if(latLon != null){
			if(description == null || description.length() == 0){
				
				if(!measurementActivity.getMeasureDistanceMode()){	//Test for distance measurement mode
					description = view.getContext().getString(R.string.point_on_map, 
							latLon.getLatitude(), latLon.getLongitude());
				}else{	//display cumulative measurement data
					if(newPointFlag){	//do not add point for menu activity
						if(measurementActivity.getMeasurementPointInsertionIndex() < 0) {
							measurementActivity.measurementPoints.add(latLon);
							index = measurementActivity.measurementPoints.size() - 1;
						}else{
							measurementActivity.measurementPoints.add(measurementActivity.getMeasurementPointInsertionIndex(), latLon);
							index = measurementActivity.getMeasurementPointInsertionIndex();
							measurementActivity.setMeasurementPointInsertionIndex(-1);		//clear insertion flag
						}
						measurementActivity.setColourChangeIndex(index);	//To assist point display colour coding
						measurementActivity.setSelectedMeasurementPointIndex(index);
						newPointFlag = false;
					}
					if(index == 0){
						if(measurementActivity.getLongInfoFlag()){
							description = view.getContext().getString(R.string.point_on_map,
									measurementActivity.measurementPoints.get(0).getLatitude(), measurementActivity.measurementPoints.get(0).getLongitude());	
						}else{
							description = view.getContext().getString(R.string.start_point);								
						}
					}else{
						measurementActivity.setCumMeasuredDistance(calculatePathDistance(index));
						if(measurementActivity.getLongInfoFlag()){
							description = "Location:\n Lat: " + String.format("%3.6f", measurementActivity.measurementPoints.get(index).getLatitude()) + "\nLon: " + 
									String.format("%3.6f", measurementActivity.measurementPoints.get(index).getLongitude()) + '\n' + "Distance: " +
									OsmAndFormatter.getFormattedDistance(measurementActivity.getCumMeasuredDistance(), view.getContext());
						}else{
							description = OsmAndFormatter.getFormattedDistance(measurementActivity.getCumMeasuredDistance(), view.getContext());
						}
					}
				}
			}
			textView.setText(description);
		} else {
			textView.setText(""); //$NON-NLS-1$
		}
		layoutText();
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		if(!measurementActivity.getMeasureDistanceMode()) return false;
		
		newPointFlag = true;	//Enable new measurement point creation
		if(pressedInTextView(point.x, point.y)){
			setLocation(null, ""); //$NON-NLS-1$
			view.refreshMap();
			return true;
		}
				
		LatLon latLon = view.getLatLonFromScreenPoint(point.x, point.y);
		StringBuilder description = new StringBuilder(); 
		
		if (!selectedObjects.isEmpty()) {
			if (selectedObjects.size() > 1) {
				description.append("1. ");
			}
			description.append(selectedContextProvider.getObjectDescription(selectedObjects.get(0)));
			for (int i = 1; i < selectedObjects.size(); i++) {
				description.append("\n" + (i + 1) + ". ").append(selectedContextProvider.getObjectDescription(selectedObjects.get(i)));
			}
			LatLon l = selectedContextProvider.getObjectLocation(selectedObjects.get(0));
			if (l != null) {
				latLon = l;
			}
		}		
		setLocation(latLon, description.toString());
		view.refreshMap();
		return true;
	}

	@Override
	public boolean onSingleTap(PointF point) {
		if (!measurementActivity.getMeasureDistanceMode()) return false;
		if(pressedInTextView(point.x, point.y)){	//Test if a measurement point text box has been clicked
				if(measurementActivity.getSelectedMeasurementPointIndex() >= 0){	//only set if there has been a change
					measurementActivity.setScreenPointLatLon (measurementActivity.measurementPoints.get(measurementActivity.getSelectedMeasurementPointIndex()));
				}
				measurementActivity.createMeasurementMenu();
		}else{

		//Test if a measurement point has been clicked
			int pointIndex = measurementActivity.isMeasurementPointSelected(point);
			if(pointIndex >= 0){
				measurementActivity.setSelectedMeasurementPointIndex(pointIndex);	//only save index of point if one has been selected 
				measurementActivity.saveScreenPoint();
				measurementActivity.createMeasurementMenu();
			}else{
				measurementActivity.setScreenPointLatLon (view.getLatLonFromScreenPoint(point.x, point.y));	//point or text not selected
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (!measurementActivity.getMeasureDistanceMode()) return false;		//Check if in measurement mode
		if(scrollingFlag) return true;	//delay activity until scrolling has finished
		PointF point = new PointF(e1.getX(), e1.getY());
		int pointIndex = measurementActivity.isMeasurementPointSelected(point);
		if(pointIndex >= 0){
			measurementActivity.setSelectedMeasurementPointIndex( pointIndex);	//save index of point selected	
			scrollingFlag = true;	//must remain true until next touch event
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (!measurementActivity.getMeasureDistanceMode()) return false;	//Check if in measurement mode
		if(scrollingFlag) return true;	//block fling while dragging a point. Note scroll event occurs before fling event
		return false;
	}

	public boolean pressedInTextView(float px, float py) {
		if (latLon != null) {
			Rect bs = textView.getBackground().getBounds();
			int x = (int) (px - view.getRotatedMapXForPoint(latLon.getLatitude(), latLon.getLongitude()));
			int y = (int) (py - view.getRotatedMapYForPoint(latLon.getLatitude(), latLon.getLongitude()));
			x += bs.width() / 2;
			y += bs.height() + boxLeg.getMinimumHeight() - SHADOW_OF_LEG;
			if (bs.contains(x, y)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(!measurementActivity.getMeasureDistanceMode())return false;	

		if (event.getAction() == MotionEvent.ACTION_DOWN) {	//must clear at start of new event, not end of last event to ensure fling is blocked
			scrollingFlag = false;
		}
		
		if (event.getAction() == MotionEvent.ACTION_UP) {	//Support for dragging measurement point
			if(scrollingFlag){
				if(measurementActivity.getSelectedMeasurementPointIndex() >= 0){	//move selected point to new location
					measurementActivity.measurementPoints.set(measurementActivity.getSelectedMeasurementPointIndex(),view.getLatLonFromScreenPoint(event.getX(), event.getY()));
					measurementActivity.setColourChangeIndex(measurementActivity.getSelectedMeasurementPointIndex());
					displayIntermediatePointInfo(measurementActivity.getColourChangeIndex());
					measurementActivity.saveScreenPoint();
					
					view.refreshMap();
				}
			}
		}
					
		if (latLon != null) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if(pressedInTextView(event.getX(), event.getY())){
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
	
	//Methods to support measurement mode
	public void displayIntermediatePointInfo(int index){	//Display info for intermediate measurement points
		String description ="";
		latLon = measurementActivity.measurementPoints.get(index);
		if (index == 0){
			if(measurementActivity.getLongInfoFlag()){
				description = view.getContext().getString(R.string.start_point) + ": \nLat: " + String.format("%3.6f",
						measurementActivity.measurementPoints.get(index).getLatitude()) + String.format("\nLon: %3.6f", measurementActivity.measurementPoints.get(index).getLongitude());					
			}else{
				description = view.getContext().getString(R.string.start_point);								
			}
		}else{
			measurementActivity.setCumMeasuredDistance(calculatePathDistance(index));
			if(measurementActivity.getLongInfoFlag()){
				description = "Location:\n Lat: " + String.format("%3.6f", measurementActivity.measurementPoints.get(index).getLatitude()) + "\nLon: " + 
					String.format("%3.6f", measurementActivity.measurementPoints.get(index).getLongitude()) + '\n' + "Distance: " +
					OsmAndFormatter.getFormattedDistance(measurementActivity.getCumMeasuredDistance(), view.getContext());
			}else{
				description = OsmAndFormatter.getFormattedDistance(measurementActivity.getCumMeasuredDistance(), view.getContext());
			}
		}
		textView.setText(description);
		layoutText();
	}
	
	public float calculatePathDistance(int index){	//Calculate distance between points
		// index is the array index of the last point to include in the calculation
		float[] calculatedDistance = new float[1];
		float distance = 0;
		for (int i = 1; i <= index; i++){
			tempPoint = new LatLon(0, 0);	//temporary object to save to measurement point list.
			tempPoint2 = new LatLon(0,0);	//temporary object to save to measurement point list.
			tempPoint = measurementActivity.measurementPoints.get(i - 1);
			tempPoint2 = measurementActivity.measurementPoints.get(i);
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

}
