package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class BaseTravelCard {

	private static final int INVALID_POSITION = -1;
	private static final int DEFAULT_VALUE = -1;

	protected View view;
	private View.OnClickListener onLeftButtonClickListener;
	private View.OnClickListener onRightButtonClickListener;

	@LayoutRes
	protected int layoutId;
	@ColorRes
	protected int bottomDividerColorId;

	private Object tag;
	private OsmandApplication app;

	protected Drawable image;
	protected String rightBottomButtonText;
	protected String leftBottomButtonText;
	protected String description;
	protected String title;

	protected int position;

	public View getView() {
		return view;
	}

	public Object getTag() {
		return tag;
	}

	public BaseTravelCard(View view,
	                      @LayoutRes int layoutId,
	                      Object tag,
	                      Drawable image,
	                      View.OnClickListener onLeftButtonClickListener,
	                      View.OnClickListener onRightButtonClickListener,
	                      int position,
	                      String rightBottomButtonText,
	                      String leftBottomButtonText,
	                      @ColorRes int bottomDividerColorId,
	                      String description,
	                      String title) {
		this.view = view;
		this.layoutId = layoutId;
		this.image = image;
		this.tag = tag;
		this.onLeftButtonClickListener = onLeftButtonClickListener;
		this.onRightButtonClickListener = onRightButtonClickListener;
		this.position = position;
		this.rightBottomButtonText = rightBottomButtonText;
		this.leftBottomButtonText = leftBottomButtonText;
		this.bottomDividerColorId = bottomDividerColorId;
		this.description = description;
		this.title = title;
	}

	protected BaseTravelCard() {

	}

	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		View view = getView(app, container, nightMode);
		if (image != null) {
			ImageView imageView = (ImageView) view.findViewById(R.id.background_image);
			imageView.setImageDrawable(image);
		}
		if (title != null) {
			((TextView) view.findViewById(R.id.title)).setText(title);
		}
		if (description != null) {
			((TextView) view.findViewById(R.id.description)).setText(description);
		}
		if (bottomDividerColorId != DEFAULT_VALUE) {
			view.findViewById(R.id.bottom_divider).setBackgroundColor(getResolvedColor(bottomDividerColorId));
		}
		if (leftBottomButtonText != null) {
			view.findViewById(R.id.left_bottom_button).setOnClickListener(onLeftButtonClickListener);
			((TextView) view.findViewById(R.id.left_bottom_button_text)).setText(leftBottomButtonText);
		}
		if (rightBottomButtonText != null) {

			if (bottomDividerColorId != DEFAULT_VALUE) {
				View buttonsDivider = view.findViewById(R.id.bottom_buttons_divider);
				buttonsDivider.setVisibility(View.VISIBLE);
				buttonsDivider.setBackgroundColor(getResolvedColor(bottomDividerColorId));
			}

			View rightButton = view.findViewById(R.id.right_bottom_button);
			rightButton.setVisibility(View.VISIBLE);
			rightButton.setOnClickListener(onRightButtonClickListener);
			((TextView) rightButton.findViewById(R.id.right_bottom_button_text)).setText(rightBottomButtonText);
		}
		if (position != INVALID_POSITION) {
			container.addView(view, position);
		} else {
			container.addView(view);
		}
	}

	private View getView(OsmandApplication app, ViewGroup parent, boolean nightMode) {
		if (view != null) {
			return view;
		}
		this.app = app;
		if (layoutId != DEFAULT_VALUE) {
			final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
			return view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
					.inflate(layoutId, parent, false);
		}
		throw new RuntimeException("BaseTravelCard must have specified view or layoutId.");
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(app, colorId);
	}

	public static class Builder {

		protected View customView;
		protected View.OnClickListener onLeftButtonClickListener;
		protected View.OnClickListener onRightButtonClickListener;

		@LayoutRes
		protected int layoutId = DEFAULT_VALUE;
		@ColorRes
		protected int dividerColorId = DEFAULT_VALUE;

		protected Drawable image;
		protected String rightBottomButtonText;
		protected String leftBottomButtonText;
		protected String description;
		protected String title;
		protected Object tag;

		protected int position = INVALID_POSITION;

		public Builder setCustomView(View customView) {
			this.customView = customView;
			return this;
		}

		public Builder setTag(Object tag) {
			this.tag = tag;
			return this;
		}

		public Builder setLayoutId(@LayoutRes int layoutId) {
			this.layoutId = layoutId;
			return this;
		}

		public Builder setBackgroundImage(Drawable image) {
			this.image = image;
			return this;
		}

		public Builder setOnLeftButtonClickListener(View.OnClickListener onLeftButtonClickListener) {
			this.onLeftButtonClickListener = onLeftButtonClickListener;
			return this;
		}

		public Builder setOnRightButtonClickListener(View.OnClickListener onRightButtonClickListener) {
			this.onRightButtonClickListener = onRightButtonClickListener;
			return this;
		}

		public Builder setRightButtonText(String rightBottomButtonText) {
			this.rightBottomButtonText = rightBottomButtonText;
			return this;
		}

		public Builder setLeftButtonText(String leftBottomButtonText) {
			this.leftBottomButtonText = leftBottomButtonText;
			return this;
		}

		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}

		public Builder setDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder setPosition(int position) {
			this.position = position;
			return this;
		}

		public Builder setBottomDividerColorId(@ColorRes int dividerColorId) {
			this.dividerColorId = dividerColorId;
			return this;
		}

		public BaseTravelCard create() {
			return new BaseTravelCard(customView,
					layoutId,
					tag,
					image,
					onLeftButtonClickListener,
					onRightButtonClickListener,
					position,
					rightBottomButtonText,
					leftBottomButtonText,
					dividerColorId,
					description,
					title);
		}
	}
}
