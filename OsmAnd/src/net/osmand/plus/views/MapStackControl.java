package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MapStackControl extends ViewGroup {
	List<MapInfoControl> stackViews = new ArrayList<MapInfoControl>();
	List<MapInfoControl> collapsedViews = new ArrayList<MapInfoControl>();
	ImageView expandView;
	// by default opened
	private boolean isCollapsed = false;
	private boolean isCollapsible = true;
	
	private Drawable topDrawable;
	List<Drawable> cacheStackDrawables = new ArrayList<Drawable>();
	private int stackDrawable;

	public MapStackControl(Context context) {
		super(context);
		final Bitmap arrowDown = BitmapFactory.decodeResource(context.getResources(), R.drawable.arrow_down);
		final Bitmap arrowUp = BitmapFactory.decodeResource(context.getResources(), R.drawable.arrow_up);
		final Paint paintImg = new Paint();
		paintImg.setAntiAlias(true);
		expandView = new ImageView(context) {
			@Override
			protected void onDraw(Canvas canvas) {
				super.onDraw(canvas);
				int cx = (getLeft() + getRight()) / 2 - getLeft();
				int t = (int) (10 * MapInfoLayer.scaleCoefficient); 

				if (!isCollapsed) {
					canvas.drawBitmap(arrowUp, cx - arrowUp.getWidth() / 2, t , paintImg);
				} else {
					canvas.drawBitmap(arrowDown, cx - arrowDown.getWidth() / 2, t , paintImg);
				}
			}
		};
		expandView.setImageDrawable(context.getResources().getDrawable(R.drawable.box_expand));
		expandView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isCollapsed = !isCollapsed;
				MapStackControl.this.requestLayout();
				MapStackControl.this.invalidate();
			}
		});
		MapStackControl.this.addView(expandView);
	}
	
	public void setExpandImageDrawable(Drawable d) {
		expandView.setImageDrawable(d);
	}
	
	public void setTopDrawable(Drawable topDrawable) {
		this.topDrawable = topDrawable;
	}
	
	public void setStackDrawable(int stackDrawable) {
		this.stackDrawable = stackDrawable;
		this.cacheStackDrawables.clear();
	}

	public void updateInfo() {
		for (MapInfoControl v : stackViews) {
			v.updateInfo();
		}
		// update even if collapsed to know if view becomes visible
		for (MapInfoControl v : collapsedViews) {
			v.updateInfo();
		}
	}
	
	
	public void addStackView(MapInfoControl v) {
		stackViews.add(v);
		MapStackControl.this.addView(v, 1);
	}

	public void addCollapsedView(MapInfoControl v) {
		collapsedViews.add(v);
		MapStackControl.this.addView(v, 1);
	}
	
	public List<MapInfoControl> getStackViews() {
		return stackViews;
	}
	
	public List<MapInfoControl> getCollapsedViews() {
		return collapsedViews;
	}
	
	public List<MapInfoControl> getAllViews(){
		List<MapInfoControl> l = new ArrayList<MapInfoControl>();
		l.addAll(stackViews);
		l.addAll(collapsedViews);
		return l;
	}

	public boolean isCollapsed() {
		return isCollapsed;
	}

	public boolean isCollapsible() {
		return isCollapsible;
	}
	
	
	private Drawable getStackDrawable(int i){
		while(i >= cacheStackDrawables.size()) {
			cacheStackDrawables.add(getResources().getDrawable(stackDrawable).mutate());
		}
		return cacheStackDrawables.get(i);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = 0;
		int h = 0;
		int prevBot = 0;
		boolean first = true;
		int cacheStack = 0;
		if (stackViews != null) {
			for (MapInfoControl c : stackViews) {
				if (c.getVisibility() != View.GONE) {
					c.setBackgroundDrawable(first ? topDrawable : getStackDrawable(cacheStack++ ));
					first = false;
					c.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
					w = Math.max(w, c.getMeasuredWidth());
					if (h > 0) {
						h -= c.getPaddingTop();
						h -= prevBot; 
					}
					h += c.getMeasuredHeight();
					prevBot = c.getPaddingBottom();
				}
			}
			isCollapsible = false;
			for (MapInfoControl c : collapsedViews) {
				if (c.getVisibility() != View.GONE) {
					isCollapsible = true;
					if (!isCollapsed) {
						c.setBackgroundDrawable(first ? topDrawable : getStackDrawable(cacheStack++ ));
						first = false;
						c.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
						w = Math.max(w, c.getMeasuredWidth());
						h -= c.getPaddingBottom();
						if (h > 0) {
							h -= c.getPaddingTop();
							h -= prevBot; 
						}
						h += c.getMeasuredHeight();
						prevBot = c.getPaddingBottom();
					} else {
						if (h == 0) {
							// measure one of the figure if it is collapsed and no top elements
							c.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
							h += c.getPaddingTop();
						}
					}
					
				}
			}
			if (isCollapsible) {
//				h -= prevBot;
				h += expandView.getDrawable().getMinimumHeight();
				w = Math.max(w, expandView.getDrawable().getMinimumWidth());
			}
		}
		setMeasuredDimension(w, h);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int y = 0;
		int cw = right - left;
		for (View c : stackViews) {
			if (c.getVisibility() != View.GONE) {
				if (y > 0) {
					y -= c.getPaddingTop();
				}
				c.layout(0, y, cw, y + c.getMeasuredHeight());
				y += c.getMeasuredHeight();
				y -= c.getPaddingBottom();
			}
		}

		for (View c : collapsedViews) {
			if (!isCollapsed) {
				if (c.getVisibility() != View.GONE) {
					if (y > 0) {
						y -= c.getPaddingTop();
					}
					c.layout(0, y, cw, y + c.getMeasuredHeight());
					y += c.getMeasuredHeight();
					y -= c.getPaddingBottom();
				}
			} else {
				c.layout(0, 0, 0, 0);
				if(y == 0){
					y += c.getPaddingTop();
				}
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