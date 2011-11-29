package net.osmand.plus.views;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

public class ContextMenuLayer extends OsmandMapLayer {
	
	

	public interface IContextMenuProvider {
	
		public Object getPointObject(PointF point);
		
		public LatLon getObjectLocation(Object o);
		
		public String getObjectDescription(Object o);
		
		public DialogInterface.OnClickListener getActionListener(List<String> actionsList, Object o);
	}
	

	private LatLon latLon;
	private IContextMenuProvider selectedContextProvider;
	private Object selectedObject;
	
	private TextView textView;
	private DisplayMetrics dm;
	private OsmandMapTileView view;
	private int BASE_TEXT_SIZE = 170;
	private int SHADOW_OF_LEG = 5;
	
	private final MapActivity activity;
	private Drawable boxLeg;
	private float scaleCoefficient = 1;
	
	public ContextMenuLayer(MapActivity activity){
		this.activity = activity;
	}
	
	@Override
	public void destroyLayer() {
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
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
		textView.setTextSize(15 * scaleCoefficient);
		textView.setTextColor(Color.argb(255, 0, 0, 0));
		textView.setMinLines(1);
//		textView.setMaxLines(15);
		textView.setGravity(Gravity.CENTER_HORIZONTAL);
		
		textView.setClickable(true);
		
		textView.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
	}

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, boolean nightMode) {
		if(latLon != null){
			int x = view.getRotatedMapXForPoint(latLon.getLatitude(), latLon.getLongitude());
			int y = view.getRotatedMapYForPoint(latLon.getLatitude(), latLon.getLongitude());
			
			int tx = x - boxLeg.getMinimumWidth() / 2;
			int ty = y - boxLeg.getMinimumHeight() + SHADOW_OF_LEG;
			canvas.translate(tx, ty);
			boxLeg.draw(canvas);
			canvas.translate(-tx, -ty);
			
			if (textView.getText().length() > 0) {
				canvas.translate(x - textView.getWidth() / 2, ty - textView.getBottom());
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
	
	private void layoutText() {
		Rect padding = new Rect();
		if(textView.getLineCount() > 0) {
			textView.getBackground().getPadding(padding);
		}
		int w = BASE_TEXT_SIZE; 
		int h = (int) ((textView.getPaint().getTextSize()	+4) * textView.getLineCount());
		textView.layout(0, -padding.bottom - padding.top, w, h);
	}
	
	public void setLocation(LatLon loc, String description){
		latLon = loc;
		if(latLon != null){
			if(description == null || description.length() == 0){
				description = MessageFormat.format(view.getContext().getString(R.string.point_on_map), 
						latLon.getLatitude(), latLon.getLongitude());
			}
			textView.setText(description);
		} else {
			textView.setText(""); //$NON-NLS-1$
		}
		layoutText();
	}
	

	@Override
	public boolean onLongPressEvent(PointF point) {
		if(pressedInTextView(point.x, point.y)){
			setLocation(null, ""); //$NON-NLS-1$
			view.refreshMap();
			return true;
		}
		
		selectedContextProvider = null;
		selectedObject = null;
		for(OsmandMapLayer l : view.getLayers()){
			if(l instanceof ContextMenuLayer.IContextMenuProvider){
				selectedObject = ((ContextMenuLayer.IContextMenuProvider) l).getPointObject(point);
				if(selectedObject != null){
					selectedContextProvider = (IContextMenuProvider) l;
					break;
				}
			}
		}
		
		LatLon latLon = view.getLatLonFromScreenPoint(point.x, point.y);
		String description = ""; 
		
		if(selectedObject != null){
			description = selectedContextProvider.getObjectDescription(selectedObject);
			LatLon l = selectedContextProvider.getObjectLocation(selectedObject);
			if(l != null){
				latLon = l;
			}
		}
		setLocation(latLon, description);
		view.refreshMap();
		return true;
	}
	
	@Override
	public boolean drawInScreenPixels() {
		return true;
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
	public boolean onSingleTap(PointF point) {
		if (pressedInTextView(point.x, point.y)) {
			if (selectedObject != null) {
				ArrayList<String> l = new ArrayList<String>();
				OnClickListener listener = selectedContextProvider.getActionListener(l, selectedObject);
				activity.contextMenuPoint(latLon.getLatitude(), latLon.getLongitude(), l, listener);
			} else {
				activity.contextMenuPoint(latLon.getLatitude(), latLon.getLongitude());
			}
			return true;
		}
		return false;
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
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

}
