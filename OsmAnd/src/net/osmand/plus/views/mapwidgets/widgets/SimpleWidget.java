package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.utils.AndroidUtils.dpToPx;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetstates.SimpleWidgetState;
import net.osmand.util.Algorithms;

public abstract class SimpleWidget extends TextInfoWidget {
	private final SimpleWidgetState simpleWidgetState;
	protected boolean isVerticalWidget;
	private TextView widgetName;

	public SimpleWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetType);
		WidgetsPanel selectedPanel = widgetsPanel != null ? widgetsPanel
				: widgetType.getPanel(customId != null ? customId : widgetType.id, settings);
		setVerticalWidget(selectedPanel);
		this.simpleWidgetState = new SimpleWidgetState(getMyApplication(), customId, widgetType);
		setupViews();
	}

	private void setupViews() {
		LinearLayout mainViewsContainer = (LinearLayout) view;
		mainViewsContainer.removeAllViews();
		UiUtilities.getInflater(mapActivity, nightMode).inflate(isVerticalWidget ? getProperVerticalLayoutId(simpleWidgetState) : R.layout.map_hud_widget, mainViewsContainer);
		findViews();
		updateWidgetView();
	}

	public void updateValueAlign(boolean fullRow) {
		if (SimpleWidgetState.WidgetSize.SMALL == getWidgetSizePref().get()) {
			if (!(container instanceof ConstraintLayout)) {
				return;
			}
			ConstraintSet constraintSet = new ConstraintSet();
			constraintSet.clone((ConstraintLayout) container);
			if (fullRow) {
				constraintSet.connect(R.id.widget_text, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
				constraintSet.connect(R.id.widget_text, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
			} else {
				constraintSet.clear(R.id.widget_text, ConstraintSet.END);
				constraintSet.connect(R.id.widget_text, ConstraintSet.START, R.id.widget_icon, ConstraintSet.END, dpToPx(app, 12));
			}
			constraintSet.applyTo((ConstraintLayout) container);
		} else {
			if (!(container instanceof FrameLayout)) {
				return;
			}
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) textView.getLayoutParams();
			params.gravity = fullRow ? Gravity.CENTER : Gravity.START;
			params.setMarginStart(dpToPx(app, 36));
			params.setMarginEnd(dpToPx(app, fullRow ? 36 : 0));
		}
	}

	private void findViews() {
		container = view.findViewById(R.id.container);
		emptyBanner = view.findViewById(R.id.empty_banner);
		imageView = view.findViewById(R.id.widget_icon);
		textView = view.findViewById(R.id.widget_text);
		textViewShadow = view.findViewById(R.id.widget_text_shadow);
		smallTextViewShadow = view.findViewById(R.id.widget_text_small_shadow);
		smallTextView = view.findViewById(R.id.widget_text_small);
		widgetName = view.findViewById(R.id.widget_name);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.simple_widget_vertical_content_container;
	}

	@LayoutRes
	private static int getProperVerticalLayoutId(@NonNull SimpleWidgetState simpleWidgetState) {
		switch (simpleWidgetState.getWidgetSizePref().get()) {
			case SMALL:
				return R.layout.simple_map_widget_small;
			case LARGE:
				return R.layout.simple_map_widget_large;
			default:
				return R.layout.simple_map_widget_medium;
		}
	}

	public void setVerticalWidget(@NonNull WidgetsPanel panel) {
		isVerticalWidget = panel.isPanelVertical();
	}

	public boolean isVerticalWidget() {
		return isVerticalWidget;
	}

	public void updateWidgetView() {
		if (isVerticalWidget) {
			boolean showIcon = shouldShowIcon();
			AndroidUiHelper.updateVisibility(imageView, showIcon);
			updateWidgetName();
		}
	}

	public boolean shouldShowIcon() {
		return simpleWidgetState.getShowIconPref().get();
	}

	public CommonPreference<Boolean> shouldShowIconPref() {
		return simpleWidgetState.getShowIconPref();
	}

	public OsmandPreference<SimpleWidgetState.WidgetSize> getWidgetSizePref() {
		return simpleWidgetState.getWidgetSizePref();
	}

	public void recreateViewIfNeeded(@NonNull WidgetsPanel panel) {
		boolean oldWidgetOrientation = isVerticalWidget;
		setVerticalWidget(panel);
		if (oldWidgetOrientation != isVerticalWidget) {
			recreateView();
		}
	}

	public void recreateView() {
		ImageView oldImageView = imageView;
		TextView oldTextView = textView;
		TextView oldTextViewShadow = textViewShadow;
		TextView oldSmallTextView = smallTextView;
		TextView oldSmallTextViewShadow = smallTextViewShadow;
		View oldContainer = container;
		View oldEmptyBanner = emptyBanner;

		setupViews();
		findViews();

		imageView.setImageDrawable(oldImageView.getDrawable());
		copyView(imageView, oldImageView);
		view.setOnClickListener(getOnClickListener());
		view.setVisibility(oldContainer.getVisibility());

		copyTextView(textView, oldTextView);
		copyTextView(textViewShadow, oldTextViewShadow);
		copyTextView(smallTextView, oldSmallTextView);
		copyTextView(smallTextViewShadow, oldSmallTextViewShadow);
		copyView(emptyBanner, oldEmptyBanner);

		updateInfo(null);
		updateWidgetView();
	}

	@Override
	public final void updateInfo(@Nullable OsmandMapLayer.DrawSettings drawSettings) {
		boolean shouldHideTopWidgets = (isVerticalWidget && mapActivity.getWidgetsVisibilityHelper().shouldHideTopWidgets());
		boolean emptyValueTextView = Algorithms.isEmpty(textView.getText());
		boolean typeAllowed = widgetType != null && widgetType.isAllowed();
		boolean visible = typeAllowed && !(shouldHideTopWidgets || emptyValueTextView);

		updateVisibility(visible);
		if (typeAllowed && (!shouldHideTopWidgets || emptyValueTextView)) {
			updateSimpleWidgetInfo(drawSettings);
		}
	}

	protected void updateSimpleWidgetInfo(@Nullable OsmandMapLayer.DrawSettings drawSettings) {
	}

	private void updateWidgetName() {
		if (widgetType != null && widgetName != null) {
			widgetName.setText(getString(widgetType.titleId));
		}
	}

	private void copyTextView(@Nullable TextView newTextView, @Nullable TextView oldTextView) {
		if (newTextView != null && oldTextView != null) {
			newTextView.setTextColor(oldTextView.getCurrentTextColor());
			newTextView.setTypeface(oldTextView.getTypeface());
			newTextView.getPaint().setStrokeWidth(oldTextView.getPaint().getStrokeWidth());
			newTextView.getPaint().setStyle(oldTextView.getPaint().getStyle());
			newTextView.setText(oldTextView.getText());
			copyView(newTextView, oldTextView);
		}
	}

	private void copyView(@Nullable View newView, @Nullable View oldTView) {
		if (newView != null && oldTView != null) {
			newView.setFocusable(oldTView.isFocusable());
			newView.setVisibility(oldTView.getVisibility());
			newView.setContentDescription(oldTView.getContentDescription());
		}
	}

	protected View.OnClickListener getOnClickListener() {
		return null;
	}

	public void showIcon(boolean showIcon) {
		AndroidUiHelper.updateVisibility(imageView, showIcon);
		imageView.invalidate();
	}

	public void setImageDrawable(int res) {
		Drawable imageDrawable = iconsCache.getIcon(res, 0);
		if (shouldShowIcon()) {
			if (imageDrawable != null) {
				imageView.setImageDrawable(imageDrawable);
				Object anim = imageView.getDrawable();
				if (anim instanceof AnimationDrawable) {
					((AnimationDrawable) anim).start();
				}
				imageView.setVisibility(View.VISIBLE);
			}
		} else {
			imageView.setVisibility(View.GONE);
		}
		imageView.invalidate();
	}

	@Override
	public void updateColors(@NonNull MapInfoLayer.TextState textState) {
		if (isVerticalWidget) {
			nightMode = textState.night;
			textView.setTextColor(textState.textColor);
			smallTextView.setTextColor(textState.secondaryTextColor);
			widgetName.setTextColor(textState.secondaryTextColor);
			int iconId = getIconId();
			if (iconId != 0) {
				setImageDrawable(iconId);
			}
			view.findViewById(R.id.widget_bg).setBackgroundResource(textState.widgetBackgroundId);
		} else {
			super.updateColors(textState);
		}
	}

	@Override
	protected View getContentView() {
		if (isVerticalWidget) {
			return view;
		} else {
			return container;
		}
	}
}
