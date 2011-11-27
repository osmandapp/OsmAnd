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
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;
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
	private static final int BASE_TEXT_SIZE = 170;
	private static final int MARGIN_Y_TO_BOX = 12;
	private int textSize = BASE_TEXT_SIZE;
	
	private Paint paintLightBorder;
	private Paint paintBlack;
	private Paint paintBorder;
	private final MapActivity activity;
	private Rect padding = new Rect();
	
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
		textSize = (int) (BASE_TEXT_SIZE * dm.density);
		
		paintLightBorder = new Paint();
		paintLightBorder.setARGB(130, 220, 220, 220);
		paintLightBorder.setStyle(Style.FILL);
		paintBlack = new Paint();
		paintBlack.setARGB(255, 0, 0, 0);
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setAntiAlias(true);
		paintBorder = new Paint();
		paintBorder.setARGB(220, 160, 160, 160);
		paintBorder.setStyle(Style.FILL);
		
		textView = new TextView(view.getContext());
		LayoutParams lp = new LayoutParams(textSize, LayoutParams.WRAP_CONTENT);
		textView.setLayoutParams(lp);
		textView.setTextSize(16);
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
			canvas.drawCircle(x, y, 5 * dm.density, paintBorder);
			canvas.drawCircle(x, y, 5 * dm.density, paintBlack);
			
			if (textView.getText().length() > 0) {
				canvas.translate(x - textView.getWidth() / 2, y - textView.getHeight() - MARGIN_Y_TO_BOX);
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
		if(textView.getLineCount() > 0) {
			textView.getBackground().getPadding(padding);
		}
		int w = textSize; 
		int h = (int) ((textView.getPaint().getTextSize()	+4) * textView.getLineCount()) + 
				padding.bottom + padding.top;
		textView.layout(0, 0, w, h);
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
			y += bs.height() + MARGIN_Y_TO_BOX;
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
