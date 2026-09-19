package net.osmand.view;

import net.osmand.plus.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Button;

public class ExpandableButton extends Button {

	private float maxWidth;

	public ExpandableButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		if(attrs != null) {
			TypedArray ar = context.obtainStyledAttributes(attrs, R.styleable.ExpandableView);
			maxWidth = ar.getDimension(R.styleable.ExpandableView_maxVisibleWidth, 0);
			if (ar != null) {
				ar.recycle();
			}
		}
	}
	
	public ExpandableButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if(attrs != null) {
			TypedArray ar = context.obtainStyledAttributes(attrs, R.styleable.ExpandableView);
			maxWidth = ar.getDimension(R.styleable.ExpandableView_maxVisibleWidth, 0);
			if (ar != null) {
				ar.recycle();
			}
		}
    }

	public ExpandableButton(Context context) {
		this(context, null);
	}
	
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if(maxWidth >0){
		if(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
			widthMeasureSpec = MeasureSpec.makeMeasureSpec((int) maxWidth, MeasureSpec.AT_MOST);
		} else  if(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST && (MeasureSpec.getSize(widthMeasureSpec)) > maxWidth){
			widthMeasureSpec = MeasureSpec.makeMeasureSpec((int) maxWidth, MeasureSpec.AT_MOST);
		} else  if(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY && (MeasureSpec.getSize(widthMeasureSpec)) > maxWidth){
			widthMeasureSpec = MeasureSpec.makeMeasureSpec((int) maxWidth, MeasureSpec.EXACTLY);
		}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

}
