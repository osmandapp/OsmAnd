package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MapStackControl extends ViewGroup {
	List<MapInfoControl> stackViews = new ArrayList<MapInfoControl>();
	List<MapInfoControl> collapsedViews = new ArrayList<MapInfoControl>();
	ImageView expandView;
	private boolean isCollapsed = true;
	private boolean isCollapsible = true;

	public MapStackControl(Context context) {
		super(context);
		final Bitmap arrowDown = BitmapFactory.decodeResource(context.getResources(), R.drawable.arrow_down);
		final Bitmap arrowUp = BitmapFactory.decodeResource(context.getResources(), R.drawable.arrow_up);
		final Paint paintImg = new Paint();
		paintImg.setAntiAlias(true);
		setChildrenDrawingOrderEnabled(true);
		expandView = new ImageView(context) {
			@Override
			protected void onDraw(Canvas canvas) {
				super.onDraw(canvas);
				int cx = (getLeft() + getRight()) / 2 - getLeft();
				int t = (int) (getBottom() - getTop() - 12 * MapInfoLayer.scaleCoefficient);

				if (!isCollapsed) {
					canvas.drawBitmap(arrowUp, cx - arrowUp.getWidth() / 2, t - arrowUp.getHeight(), paintImg);
				} else {
					canvas.drawBitmap(arrowDown, cx - arrowDown.getWidth() / 2, t - arrowUp.getHeight(), paintImg);
				}
			}
		};
		expandView.setImageDrawable(context.getResources().getDrawable(R.drawable.box_expand).mutate());
		expandView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isCollapsed = !isCollapsed;
				if (!isCollapsed) {
					for (MapInfoControl l : collapsedViews) {
						l.updateInfo();
					}
				}
				MapStackControl.this.requestLayout();

			}
		});
		MapStackControl.this.addView(expandView);
	}

	public void updateInfo() {
		for (MapInfoControl v : stackViews) {
			v.updateInfo();
		}
		if (!isCollapsed) {
			for (MapInfoControl v : collapsedViews) {
				v.updateInfo();
			}
		}
	}

	@Override
	protected int getChildDrawingOrder(int childCount, int i) {
		// start from expand view
		if (i == 0) {
			return 0;
		}
		return childCount - i;
	}

	public void addStackView(MapInfoControl v) {
		stackViews.add(v);
		MapStackControl.this.addView(v);
	}

	public void addCollapsedView(MapInfoControl v) {
		collapsedViews.add(v);
		MapStackControl.this.addView(v);
	}

	public boolean isCollapsed() {
		return isCollapsed;
	}

	public boolean isCollapsible() {
		return isCollapsible;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = 0;
		int h = 0;
		if (stackViews != null) {
			for (View c : stackViews) {
				c.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				w = Math.max(w, c.getMeasuredWidth());
				h += c.getMeasuredHeight();
			}
			isCollapsible = false;
			for (View c : collapsedViews) {
				c.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				if (c.getMeasuredHeight() > 0) {
					w = Math.max(w, c.getMeasuredWidth());
					h += c.getMeasuredHeight();
					isCollapsible = true;
				}
			}
			if (isCollapsible) {
				h += expandView.getDrawable().getMinimumHeight();
			}
		}
		setMeasuredDimension(w, h);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int y = 0;
		int cw = right - left;
		for (View c : stackViews) {
			c.layout(0, y, cw, y + c.getMeasuredHeight());
			y += c.getMeasuredHeight();
		}

		for (View c : collapsedViews) {
			if (!isCollapsed) {
				c.setVisibility(VISIBLE);
				c.layout(0, y, cw, y + c.getMeasuredHeight());
				y += c.getMeasuredHeight();
			} else {
				c.setVisibility(GONE);
			}
		}

		if (isCollapsible) {
			expandView.setVisibility(VISIBLE);
			int w = expandView.getDrawable().getMinimumWidth();
			int h = expandView.getDrawable().getMinimumHeight();
			expandView.layout((cw - w) / 2, y, (cw + w) / 2, y + h);
		} else {
			expandView.setVisibility(GONE);
		}
	}

}