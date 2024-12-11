package net.osmand.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.widgets.TextViewEx;

public class ComplexButton extends FrameLayout {

	private TextViewEx textTv;
	private TextViewEx subTextTv;
	private ImageView iconIv;

	public ComplexButton(@NonNull Context context) {
		super(context);
		inflateView();
	}

	public ComplexButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		inflateView();
		initView(attrs);
	}

	public ComplexButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		inflateView();
		initView(attrs);
	}

	private void inflateView() {
		View view = LayoutInflater.from(getContext()).inflate(R.layout.button_with_icon_and_subtext, this);
		textTv = view.findViewById(R.id.text);
		subTextTv = view.findViewById(R.id.sub_text);
		iconIv = view.findViewById(R.id.icon);
	}

	private void initView(@Nullable AttributeSet attrs) {
		TypedArray a = getContext().getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.ComplexButton,
				0, 0);
		try {
			textTv.setText(a.getString(R.styleable.ComplexButton_setText));
			textTv.setTextColor(a.getColor(R.styleable.ComplexButton_setTextColor, Color.WHITE));
			subTextTv.setText(a.getString(R.styleable.ComplexButton_setSubText));
			subTextTv.setTextColor(a.getColor(R.styleable.ComplexButton_setSubTextColor, Color.WHITE));
			iconIv.setImageResource(a.getResourceId(R.styleable.ComplexButton_setIcon, 0));
		} finally {
			a.recycle();
		}
	}

	public void setText(String text) {
		textTv.setText(text);
	}

	public void setText(int textRes) {
		textTv.setText(textRes);
	}

	public void setTextColor(int color) {
		textTv.setTextColor(color);
	}

	public void setSubText(String subText) {
		subTextTv.setText(subText);
	}

	public void setSubText(int subTextRes) {
		subTextTv.setText(subTextRes);
	}

	public void setSubTextColor(int color) {
		subTextTv.setTextColor(color);
	}

	public void setIcon(int iconRes) {
		iconIv.setImageResource(iconRes);
	}

	public void setIcon(Drawable drawable) {
		iconIv.setImageDrawable(drawable);
	}
}
