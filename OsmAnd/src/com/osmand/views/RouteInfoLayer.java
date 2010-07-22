package com.osmand.views;

import java.util.List;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

import com.osmand.activities.RoutingHelper;
import com.osmand.activities.ShowRouteInfoActivity;
import com.osmand.activities.RoutingHelper.IRouteInformationListener;
import com.osmand.activities.RoutingHelper.RouteDirectionInfo;

public class RouteInfoLayer implements OsmandMapLayer, IRouteInformationListener {

	private final int TEXT_SIZE = 150;

	private OsmandMapTileView view;
	private final RoutingHelper routingHelper;
	private Button next;
	private Button prev;
	private Button info;
	private Handler uiHandler;
	private boolean visible = true;
	private RectF border;
	private RectF textBorder;
	private Paint paintBorder;
	private Paint paintBlack;
	private final LinearLayout layout;
	private int directionInfo = -1;
	private TextView textView;

	private Paint paintLightBorder;
	
	public RouteInfoLayer(RoutingHelper routingHelper, LinearLayout layout){
		this.routingHelper = routingHelper;
		this.layout = layout;
		prev = (Button) layout.findViewById(com.osmand.R.id.PreviousButton);
		next = (Button) layout.findViewById(com.osmand.R.id.NextButton);
		info = (Button) layout.findViewById(com.osmand.R.id.InfoButton);
		routingHelper.addListener(this);
		attachListeners();
		updateVisibility();
	}
	
	private void attachListeners() {
		prev.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				if(routingHelper.getRouteDirections() != null && directionInfo > 0){
					directionInfo--;
					if(routingHelper.getRouteDirections().size() > directionInfo){
						RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
						Location l = routingHelper.getLocationFromRouteDirection(info);
						if(info.descriptionRoute != null){
							textView.setText(info.descriptionRoute);
							textView.layout(0, 0, TEXT_SIZE, (int) ((textView.getPaint().getTextSize()+4) * textView.getLineCount()));
						}
						view.getAnimatedDraggingThread().startMoving(view.getLatitude(), view.getLongitude(),
								l.getLatitude(), l.getLongitude(),
								view.getZoom(), view.getZoom(), view.getSourceTileSize(), view.getRotate(), true);
					}
					
				}
				view.refreshMap();
			}
			
		});
		next.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				if(routingHelper.getRouteDirections() != null && directionInfo < routingHelper.getRouteDirections().size() - 1){
					directionInfo++;
					RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
					Location l = routingHelper.getLocationFromRouteDirection(info);
					if(info.descriptionRoute != null){
						textView.setText(info.descriptionRoute);
						textView.layout(0, 0, TEXT_SIZE, (int) ((textView.getPaint().getTextSize() + 4) * textView.getLineCount()));
					}
					view.getAnimatedDraggingThread().startMoving(view.getLatitude(), view.getLongitude(),
							l.getLatitude(), l.getLongitude(),
							view.getZoom(), view.getZoom(), view.getSourceTileSize(), view.getRotate(), true);
				}
				view.refreshMap();
			}
			
		});
		info.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(view.getContext(), ShowRouteInfoActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				view.getContext().startActivity(intent);
			}
		});
				
		
	}

	public boolean isVisible(){
		return visible && routingHelper.isRouteCalculated() && !routingHelper.isFollowingMode();
	}
	public boolean couldBeVisible(){
		return routingHelper.isRouteCalculated() && !routingHelper.isFollowingMode();
	}
	private void updateVisibility(){
		int vis = isVisible() ? View.VISIBLE : View.INVISIBLE; 
		prev.setVisibility(vis);
		next.setVisibility(vis);
		info.setVisibility(vis);
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		uiHandler = new Handler();
		border = new RectF();
		paintBorder = new Paint();
		paintBorder.setARGB(220, 160, 160, 160);
		paintBorder.setStyle(Style.FILL);
		paintLightBorder = new Paint();
		paintLightBorder.setARGB(130, 220, 220, 220);
		paintLightBorder.setStyle(Style.FILL);
		paintBlack = new Paint();
		paintBlack.setARGB(255, 0, 0, 0);
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setAntiAlias(true);
		
		textView = new TextView(view.getContext());
		LayoutParams lp = new LayoutParams(TEXT_SIZE, LayoutParams.WRAP_CONTENT);
		textView.setLayoutParams(lp);
		textView.setTextSize(16);
		textView.setTextColor(Color.argb(255, 0, 0, 0));
		textView.setMinLines(1);
		textView.setMaxLines(4);
		textView.setGravity(Gravity.CENTER_HORIZONTAL);
		textBorder = new RectF(-2, -1, TEXT_SIZE + 2, 0);
	}

	@Override
	public void onDraw(Canvas canvas) {
		if(isVisible()){
			border.set(layout.getLeft() - 10, layout.getTop() - 3, layout.getRight() - 8, layout.getBottom() + 3);
			canvas.drawRoundRect(border, 5, 5, paintBorder);
			canvas.drawRoundRect(border, 5, 5, paintBlack);
			List<RouteDirectionInfo> dir = routingHelper.getRouteDirections();
			if(dir != null && directionInfo < dir.size() && directionInfo >= 0){
				canvas.rotate(view.getRotate(), view.getCenterPointX(), view.getCenterPointY());
				RouteDirectionInfo info = dir.get(directionInfo);
				
				Location loc = routingHelper.getLocationFromRouteDirection(info);
				int x = view.getRotatedMapXForPoint(loc.getLatitude(), loc.getLongitude());
				int y = view.getRotatedMapYForPoint(loc.getLatitude(), loc.getLongitude());
				canvas.drawCircle(x, y, 5, paintBorder);
				canvas.drawCircle(x, y, 5, paintBlack);
				
				if (textView.getText().length() > 0) {
					canvas.translate(x - textView.getWidth() / 2, y - textView.getHeight() - 12);
					int c = textView.getLineCount();
					textBorder.bottom = textView.getHeight() + 2;
					canvas.drawRect(textBorder, paintLightBorder);
					canvas.drawRect(textBorder, paintBlack);
					textView.draw(canvas);
					if (c == 0) {
						// special case relayout after on draw method
						textView.layout(0, 0, TEXT_SIZE, (int) ((textView.getPaint().getTextSize() + 4) * textView.getLineCount()));
						view.refreshMap();
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
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		return false;
	}

	@Override
	public void newRouteIsCalculated() {
		uiHandler.post(new Runnable(){
			@Override
			public void run() {
				directionInfo = -1;
				if(!routingHelper.isFollowingMode()){
					visible = true;
				}
				updateVisibility();
			}
		});
		
	}

	public boolean isUserDefinedVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
		updateVisibility();
	}
	@Override
	public void routeWasCancelled() {
		uiHandler.post(new Runnable(){
			@Override
			public void run() {
				directionInfo = -1;
				updateVisibility();
			}
		});
	}
	
	
}
