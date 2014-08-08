package net.osmand.plus.views.controls;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;


public abstract class MapControls {

	protected MapActivity mapActivity;
	protected float scaleCoefficient;
	protected Handler showUIHandler;
	protected int shadowColor;
	private boolean visible;
	private long delayTime;

	protected int gravity = Gravity.BOTTOM | Gravity.RIGHT;
	protected int margin;
	protected int vmargin;
	protected int width;
	protected int height;
	protected Runnable notifyClick;
	private int extraVerticalMargin;

	public MapControls(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		this.mapActivity = mapActivity;
		this.showUIHandler = showUIHandler;
		this.scaleCoefficient = scaleCoefficient;
	}


	public void setGravity(int gravity) {
		this.gravity = gravity;
	}

	public void setMargin(int margin) {
		this.margin = margin;
	}
	public void setVerticalMargin(int vmargin) {
		this.vmargin = vmargin;
	}

	protected ImageButton addImageButton(FrameLayout parent, int stringId, int resourceId) {
		Context ctx = mapActivity;
		ImageButton  button = new ImageButton(ctx);
		applyAttributes(ctx, parent, button, stringId, resourceId, 0);
		return button;
	}

	public int getTotalVerticalMargin() {
		return extraVerticalMargin + vmargin;
	}

	protected Button addButton(FrameLayout parent, int stringId, int resourceId) {
		return addButton(parent, stringId, resourceId, 0);
	}
		protected Button addButton(FrameLayout parent, int stringId, int resourceId, int extraMargin) {
		Context ctx = mapActivity;
		Button button = new Button(ctx);
		applyAttributes(ctx, parent, button, stringId, resourceId, extraMargin);
		return button;
	}

	public void setNotifyClick(Runnable notifyClick) {
		this.notifyClick = notifyClick;
	}

	protected void notifyClicked() {
		if(notifyClick != null) {
			notifyClick.run();
		}
	}


	private void applyAttributes(Context ctx, FrameLayout parent, View button, int stringId, int resourceId,
			int extraMargin) {
		button.setContentDescription(ctx.getString(stringId));
		button.setBackgroundResource(resourceId);
		Drawable d = ctx.getResources().getDrawable(resourceId);
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(d.getMinimumWidth(), d.getMinimumHeight(),
				gravity);
		if((gravity & Gravity.LEFT) == Gravity.LEFT) {
			params.leftMargin = margin + extraMargin;
		} else {
			params.rightMargin = margin + extraMargin;
		}
		if((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
			params.bottomMargin = vmargin + extraVerticalMargin;
		} else {
			params.topMargin = vmargin + extraVerticalMargin;
		}
		button.setLayoutParams(params);
		parent.addView(button);
		button.setEnabled(true);
		mapActivity.accessibleContent.add(button);
	}

	public int getGravity() {
		return gravity;
	}

	protected void removeButton(FrameLayout layout, View b) {
		layout.removeView(b);
		mapActivity.accessibleContent.remove(b);
	}

	public void updateTextColor(int textColor, int shadowColor) {
		this.shadowColor = shadowColor;
	}

	public final void init(FrameLayout layout) {
		initControls(layout);
	}

	public final void show(FrameLayout layout) {
		visible = true;
		showControls(layout);
	}

	public final void showWithDelay(final FrameLayout layout, final long delay) {
		this.delayTime = System.currentTimeMillis() + delay;
		if(!visible) {
			visible = true;
			showControls(layout);
			runWithDelay(layout, delay);
			mapActivity.getMapView().refreshMap();
		}
	}

	private void runWithDelay(final FrameLayout layout, final long delay) {
		showUIHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				long ctime = System.currentTimeMillis();
				if(MapControls.this.delayTime <= ctime) {
					if (MapControls.this.delayTime != 0) {
						MapControls.this.delayTime = 0;
						visible = false;
						hideControls(layout);
						mapActivity.getMapView().refreshMap();
					}
				} else {
					runWithDelay(layout, MapControls.this.delayTime - ctime);
				}
			}
		}, delay);
	}

	public final void hide(FrameLayout layout) {
		if(this.delayTime == 0) {
			visible = false;
			hideControls(layout);
		}
	}

	public final void forceHide(FrameLayout layout) {
		delayTime = 0;
		visible = false;
		hideControls(layout);
		mapActivity.getMapView().refreshMap();
	}

	public boolean isVisible() {
		return visible;
	}

	protected boolean isLeft() {
		return (Gravity.LEFT & gravity) == Gravity.LEFT;
	}

	public boolean isBottom() {
		return (Gravity.BOTTOM & gravity) == Gravity.BOTTOM;
	}


	protected void initControls(FrameLayout layout) {
	}

	protected abstract void hideControls(FrameLayout layout);

	protected abstract void showControls(FrameLayout layout);


	public abstract void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode);


	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		return false;
	}

	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	public void setExtraVerticalMargin(int extraVerticalMargin) {
		this.extraVerticalMargin = extraVerticalMargin;
	}

	public int getExtraVerticalMargin() {
		return this.extraVerticalMargin;
	}
}