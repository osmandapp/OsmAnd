package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.plus.views.mapwidgets.widgetstates.SimpleWidgetState;

import java.util.List;

public abstract class SimpleWidget extends TextInfoWidget {
	private final SimpleWidgetState simpleWidgetState;
	protected boolean isVerticalWidget;
	private TextView widgetName;

	public SimpleWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel, @NonNull SimpleWidgetState simpleWidgetState) {
		super(mapActivity, widgetType, customId, getLayoutId(mapActivity, simpleWidgetState, customId, widgetType, widgetsPanel));
		WidgetsPanel selectedPanel = widgetsPanel != null ? widgetsPanel
				: widgetType.getPanel(customId != null ? customId : widgetType.id, settings);
		setVerticalWidget(selectedPanel);
		this.simpleWidgetState = simpleWidgetState;
		findViews();
		updateWidgetView();
	}

	@LayoutRes
	protected static int getLayoutId(@NonNull MapActivity mapActivity, @NonNull SimpleWidgetState simpleWidgetState, @Nullable String customId, @NonNull WidgetType widgetType, @Nullable WidgetsPanel panel) {
		WidgetsPanel selectedPanel = panel != null ? panel
				: widgetType.getPanel(customId != null ? customId : widgetType.id, mapActivity.getMyApplication().getSettings());
		return selectedPanel.isPanelVertical() ? getProperVerticalLayoutId(simpleWidgetState) : R.layout.map_hud_widget;
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

	protected static SimpleWidgetState createSimpleWidgetState(@NonNull OsmandApplication app, @Nullable String customId, @NonNull WidgetType widgetType) {
		return new SimpleWidgetState(app, customId, widgetType);
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
			recreateView(panel);
		}
	}

	public void recreateView(@Nullable WidgetsPanel panel) {
		View oldView = view;
		ImageView oldImageView = imageView;
		TextView oldTextView = textView;
		TextView oldTextViewShadow = textViewShadow;
		TextView oldSmallTextView = smallTextView;
		TextView oldSmallTextViewShadow = smallTextViewShadow;
		View oldContainer = container;
		View oldEmptyBanner = emptyBanner;
		View oldBottomDivider = bottomDivider;

		createView(getLayoutId(mapActivity, simpleWidgetState, customId, widgetType, panel));
		findViews();

		imageView.setImageDrawable(oldImageView.getDrawable());
		copyView(imageView, oldImageView);
		view.setOnClickListener(getOnClickListener());
		copyView(view, oldView);

		copyTextView(textView, oldTextView);
		copyTextView(textViewShadow, oldTextViewShadow);
		copyTextView(smallTextView, oldSmallTextView);
		copyTextView(smallTextViewShadow, oldSmallTextViewShadow);
		copyView(container, oldContainer);
		copyView(emptyBanner, oldEmptyBanner);
		copyView(bottomDivider, oldBottomDivider);

		updateInfo(null);
		updateWidgetView();
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

	protected void findViews() {
		container = view.findViewById(R.id.container);
		emptyBanner = view.findViewById(R.id.empty_banner);
		imageView = view.findViewById(R.id.widget_icon);
		textView = view.findViewById(R.id.widget_text);
		textViewShadow = view.findViewById(R.id.widget_text_shadow);
		smallTextViewShadow = view.findViewById(R.id.widget_text_small_shadow);
		smallTextView = view.findViewById(R.id.widget_text_small);
		bottomDivider = view.findViewById(R.id.bottom_divider);
		widgetName = view.findViewById(R.id.widget_name);
	}

	public void showIcon(boolean showIcon) {
		AndroidUiHelper.updateVisibility(imageView, showIcon);
		imageView.invalidate();
	}

	@Override
	protected int getBackgroundResource(@NonNull MapInfoLayer.TextState textState) {
		return isVerticalWidget ? ColorUtilities.getWidgetBackgroundColorId(isNightMode()) : textState.widgetBackgroundId;
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
	public void attachView(@NonNull ViewGroup container, @NonNull WidgetsPanel panel, int order,
						   @NonNull List<MapWidget> followingWidgets) {
		super.attachView(container, panel, order, followingWidgets);
		boolean showBottomDivider = shouldShowBottomDivider(followingWidgets);
		showHideDivider(showBottomDivider);
	}

	private boolean shouldShowBottomDivider(@NonNull List<MapWidget> followingWidgets) {
		if (isVerticalWidget) {
			WidgetsVisibilityHelper visibilityHelper = mapActivity.getWidgetsVisibilityHelper();
			boolean showTopCoordinates = visibilityHelper.shouldShowTopCoordinatesWidget();
			WidgetsPanel widgetsPanel = widgetType.getPanel(customId != null ? customId : widgetType.id, settings);
			if (widgetsPanel == WidgetsPanel.TOP) {
				for (MapWidget widget : followingWidgets) {
					if (widget instanceof MapMarkersBarWidget || widget instanceof SimpleWidget || (widget instanceof CoordinatesBaseWidget && showTopCoordinates)) {
						return false;
					}
				}
			} else {
				return false;
			}
			return true;
		}
		return false;
	}

	private void showHideDivider(boolean show) {
		AndroidUiHelper.updateVisibility(bottomDivider, !show);
	}
}
