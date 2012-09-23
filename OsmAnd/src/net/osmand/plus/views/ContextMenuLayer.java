package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.osm.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

public class ContextMenuLayer extends OsmandMapLayer {
	
	public interface IContextMenuProvider {
	
		public void collectObjectsFromPoint(PointF point, List<Object> o);
		
		public LatLon getObjectLocation(Object o);
		
		public String getObjectDescription(Object o);
		
		public String getObjectName(Object o);
		
	}

	private LatLon latLon;
	private Map<Object, IContextMenuProvider> selectedObjects = new LinkedHashMap<Object, IContextMenuProvider>();
	
	private TextView textView;
	private ImageView closeButton;
	private DisplayMetrics dm;
	private OsmandMapTileView view;
	private int BASE_TEXT_SIZE = 170;
	private int SHADOW_OF_LEG = 5;
	private int CLOSE_BTN = 3;
	
	private final MapActivity activity;
	private Drawable boxLeg;
	private float scaleCoefficient = 1;
	private Rect textPadding;
	
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
		CLOSE_BTN = (int) (CLOSE_BTN * scaleCoefficient);
		
		boxLeg = view.getResources().getDrawable(R.drawable.box_leg);
		boxLeg.setBounds(0, 0, boxLeg.getMinimumWidth(), boxLeg.getMinimumHeight());
		
		textView = new TextView(view.getContext());
		LayoutParams lp = new LayoutParams(BASE_TEXT_SIZE, LayoutParams.WRAP_CONTENT);
		textView.setLayoutParams(lp);
		textView.setTextSize(15);
		textView.setTextColor(Color.argb(255, 0, 0, 0));
		textView.setMinLines(1);
//		textView.setMaxLines(15);
		textView.setGravity(Gravity.CENTER_HORIZONTAL);
		
		textView.setClickable(true);
		
		textView.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
		textPadding = new Rect();
		textView.getBackground().getPadding(textPadding);
		
		closeButton = new ImageView(view.getContext());
		lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		closeButton.setLayoutParams(lp);
		closeButton.setImageDrawable(view.getResources().getDrawable(R.drawable.headliner_close));
		closeButton.setClickable(true);
	}

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
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
				canvas.translate(textView.getWidth() - closeButton.getWidth() - CLOSE_BTN, CLOSE_BTN);
				closeButton.draw(canvas);
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
		if (textView.getLineCount() > 0) {
			textView.getBackground().getPadding(padding);
		}
		int w = BASE_TEXT_SIZE;
		int h = (int) ((textView.getPaint().getTextSize() + 4) * textView.getLineCount());
		
		textView.layout(0, -padding.bottom, w, h + padding.top);
		int minw = closeButton.getDrawable().getMinimumWidth();
		int minh = closeButton.getDrawable().getMinimumHeight();
		closeButton.layout(0, 0, minw, minh);
	}
	
	public void setLocation(LatLon loc, String description){
		latLon = loc;
		if(latLon != null){
			if(description == null || description.length() == 0){
				description = view.getContext().getString(R.string.point_on_map, 
						latLon.getLatitude(), latLon.getLongitude());
			}
			textView.setText(description);
		} else {
			textView.setText(""); //$NON-NLS-1$
		}
		layoutText();
	}
	

	public void setSelections(Map<Object, IContextMenuProvider> selections) {
		if (selections != null) {
			selectedObjects = selections;
		} else {
			selectedObjects.clear();
		}
		if (!selectedObjects.isEmpty()) {
			Entry<Object, IContextMenuProvider> e = selectedObjects.entrySet().iterator().next();
			latLon = e.getValue().getObjectLocation(e.getKey());
		}
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		if (!view.getSettings().SCROLL_MAP_BY_GESTURES.get()) {
			if (!selectedObjects.isEmpty())
				view.showMessage(activity.getNavigationHint(latLon));
			return true;
		}
		
		if(pressedInTextView(point.x, point.y) > 0){
			setLocation(null, ""); //$NON-NLS-1$
			view.refreshMap();
			return true;
		}
		
		selectedObjects.clear();
		List<Object> s = new ArrayList<Object>();
		LatLon latLon = null;
		for(OsmandMapLayer l : view.getLayers()){
			if(l instanceof ContextMenuLayer.IContextMenuProvider){
				s.clear();
				((ContextMenuLayer.IContextMenuProvider) l).collectObjectsFromPoint(point, s);
				for(Object o : s) {
					selectedObjects.put(o, ((ContextMenuLayer.IContextMenuProvider) l));
					if(latLon == null) {
						latLon = ((ContextMenuLayer.IContextMenuProvider) l).getObjectLocation(o);
					}
				}
			}
		}
		if(latLon == null) {
			latLon = view.getLatLonFromScreenPoint(point.x, point.y);
		}
		String description = getSelectedObjectDescription();
		setLocation(latLon, description);
		view.refreshMap();
		return true;
	}
	
	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	
	public int pressedInTextView(float px, float py) {
		if (latLon != null) {
			Rect bs = textView.getBackground().getBounds();
			Rect closes = closeButton.getDrawable().getBounds();
			int x = (int) (px - view.getRotatedMapXForPoint(latLon.getLatitude(), latLon.getLongitude()));
			int y = (int) (py - view.getRotatedMapYForPoint(latLon.getLatitude(), latLon.getLongitude()));
			x += bs.width() / 2;
			y += bs.height() + boxLeg.getMinimumHeight() - SHADOW_OF_LEG;
			int dclosex = x - bs.width() + CLOSE_BTN + closes.width();
			int dclosey = y + CLOSE_BTN;
			if(closes.intersects(dclosex - CLOSE_BTN, dclosey - CLOSE_BTN, dclosex + CLOSE_BTN, dclosey + CLOSE_BTN)) {
				return 2;
			} else if (bs.contains(x, y)) {
				return 1;
			}
		}
		return 0;
	}
	
	public String getSelectedObjectName(){
		return getSelectedObjectInfo(true);
	}
	
	public String getSelectedObjectDescription(){
		return getSelectedObjectInfo(false);
	}
	
	private String getSelectedObjectInfo(boolean name){
		if(!selectedObjects.isEmpty()){
			StringBuilder description = new StringBuilder(); 
			if (selectedObjects.size() > 1) {
				description.append("1. ");
			}
			Iterator<Entry<Object, IContextMenuProvider>> it = selectedObjects.entrySet().iterator();
			int i = 0;
			while(it.hasNext()) {
				Entry<Object, IContextMenuProvider> e = it.next();
				if( i > 0) {
					description.append("\n" + (i + 1) + ". ");
				}
				if(name) {
					description.append(e.getValue().getObjectName(e.getKey()));
				} else {
					description.append(e.getValue().getObjectDescription(e.getKey()));
				}
				i++;
			}
			return description.toString();
		}
		return null;
	}

	@Override
	public boolean onSingleTap(PointF point) {
		boolean nativeMode = view.getSettings().SCROLL_MAP_BY_GESTURES.get();
		int val = pressedInTextView(point.x, point.y);
		if (val == 2) {
			setLocation(null, ""); //$NON-NLS-1$
			view.refreshMap();
			return true;
		} else if (val == 1 || !nativeMode) {
			if (!selectedObjects.isEmpty()) {
				showContextMenuForSelectedObjects();
			} else if (nativeMode) {
				activity.getMapActions().contextMenuPoint(latLon.getLatitude(), latLon.getLongitude());
			}
			return true;
		}
		return false;
	}

	private void showContextMenuForSelectedObjects() {
		final ContextMenuAdapter menuAdapter = new ContextMenuAdapter(activity);
		if (selectedObjects.size() > 1) {
			Builder builder = new AlertDialog.Builder(view.getContext());
			String[] d = new String[selectedObjects.size()];
			final List<Object> s = new ArrayList<Object>();
			int i = 0;
			Iterator<Entry<Object, IContextMenuProvider>> it = selectedObjects.entrySet().iterator();
			while(it.hasNext()) {
				Entry<Object, IContextMenuProvider> e = it.next();
				d[i++] = e.getValue().getObjectDescription(e.getKey());
				s.add(e.getKey());
			}
			builder.setItems(d, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Object selectedObj = s.get(which);
					for (OsmandMapLayer layer : view.getLayers()) {
						layer.populateObjectContextMenu(selectedObj, menuAdapter);
					}
					activity.getMapActions().contextMenuPoint(latLon.getLatitude(), latLon.getLongitude(), menuAdapter, selectedObj);
				}
			});
			builder.show();
		} else {
			Object selectedObj = selectedObjects.keySet().iterator().next();
			for (OsmandMapLayer layer : view.getLayers()) {
				layer.populateObjectContextMenu(selectedObj, menuAdapter);
			}
			activity.getMapActions().contextMenuPoint(latLon.getLatitude(), latLon.getLongitude(), menuAdapter, selectedObj);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (latLon != null) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				int vl = pressedInTextView(event.getX(), event.getY());
				if(vl == 1){
					textView.setPressed(true);
					view.refreshMap();
				} else if(vl == 2){
					closeButton.setPressed(true);
					view.refreshMap();
				}
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
			if(textView.isPressed()) {
				textView.setPressed(false);
				view.refreshMap();
			}
			if(closeButton.isPressed()) {
				closeButton.setPressed(false);
				view.refreshMap();
			}
		}
		return false;
	}

	public void setSelectedObject(Object toShow) {
		selectedObjects.clear();
		if(toShow != null){
			for(OsmandMapLayer l : view.getLayers()){
				if(l instanceof ContextMenuLayer.IContextMenuProvider){
					String des = ((ContextMenuLayer.IContextMenuProvider) l).getObjectDescription(toShow);
					if(des != null) {
						selectedObjects.put(toShow, (IContextMenuProvider) l);
					}
				}
			}
		}
	}

}
