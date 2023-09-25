package net.osmand.plus.views.mapwidgets.widgets;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.plus.views.mapwidgets.widgetstates.SimpleWidgetState;

import java.util.List;

public abstract class SimpleWidget extends TextInfoWidget {
	protected boolean isVerticalWidget;
	private TextView widgetName;
	private SimpleWidgetState simpleWidgetState;

	public SimpleWidget(@NonNull MapActivity mapActivity, @Nullable WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetType, customId, getLayoutId(mapActivity, customId, widgetType, widgetsPanel));
		if (widgetsPanel != null) {
			setVerticalWidget(widgetsPanel);
		} else if (widgetType != null) {
			String id = customId != null ? customId : widgetType.id;
			setVerticalWidget(widgetType.getPanel(id, settings));
		}
		this.simpleWidgetState = new SimpleWidgetState(getMyApplication(), customId, widgetType);
		findViews();
		updateWidgetView();
	}

	protected static int getLayoutId(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetType widgetType, @Nullable WidgetsPanel panel) {
		if (panel != null) {
			return panel.isPanelVertical() ? R.layout.simple_map_widget : R.layout.map_hud_widget;
		} else if (widgetType != null) {
			WidgetsPanel widgetsPanel = widgetType.getPanel(customId != null ? customId : widgetType.id, mapActivity.getMyApplication().getSettings());
			return widgetsPanel.isPanelVertical() ? R.layout.simple_map_widget : R.layout.map_hud_widget;
		}
		return R.layout.map_hud_widget;
	}

	public void setVerticalWidget(@NonNull WidgetsPanel panel) {
		isVerticalWidget = panel.isPanelVertical();
	}

	private void updateWidgetView() {
		if (isVerticalWidget) {
			SimpleWidgetState.WidgetSize widgetSize = simpleWidgetState.getWidgetSizePref().get();
			textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, app.getResources().getDimensionPixelSize(widgetSize.valueSizeId));
			view.setMinimumHeight(app.getResources().getDimensionPixelSize(widgetSize.minWidgetHeightId));

			boolean showIcon = simpleWidgetState.getShowIconPref().get();
			AndroidUiHelper.updateVisibility(imageView, showIcon);

			if (widgetSize == SimpleWidgetState.WidgetSize.S) {
				setupSmallWidgetSize();
			} else {
				setupNormalWidgetSize();
			}
			updateWidgetName();
		}
	}

	private void setupSmallWidgetSize() {
		ConstraintLayout constraintLayout = (ConstraintLayout) container;
		ConstraintSet constraintSet = new ConstraintSet();
		constraintSet.clone(constraintLayout);
		constraintSet.clear(R.id.widget_name, ConstraintSet.START);
		constraintSet.connect(R.id.widget_name, ConstraintSet.END, R.id.container, ConstraintSet.END, 0);
		constraintSet.connect(R.id.widget_name, ConstraintSet.TOP, R.id.container, ConstraintSet.TOP, 0);

		constraintSet.clear(R.id.widget_text_small, ConstraintSet.TOP);
		constraintSet.connect(R.id.widget_text_small, ConstraintSet.TOP, R.id.widget_name, ConstraintSet.BOTTOM, 0);
		constraintSet.connect(R.id.widget_text_small, ConstraintSet.END, R.id.container, ConstraintSet.END, 0);

		constraintSet.clear(R.id.widget_text, ConstraintSet.END);
		constraintSet.clear(R.id.widget_text, ConstraintSet.TOP);
		constraintSet.connect(R.id.widget_text, ConstraintSet.START, R.id.widget_icon, ConstraintSet.END, 12);
		constraintSet.connect(R.id.widget_text, ConstraintSet.TOP, R.id.container, ConstraintSet.TOP, 0);

		constraintSet.clear(R.id.widget_icon, ConstraintSet.TOP);
		constraintSet.connect(R.id.widget_icon, ConstraintSet.TOP, R.id.container, ConstraintSet.TOP, 0);
		constraintSet.applyTo(constraintLayout);
	}

	private void setupNormalWidgetSize() {
		ConstraintLayout constraintLayout = (ConstraintLayout) container;
		ConstraintSet constraintSet = new ConstraintSet();
		constraintSet.clone(constraintLayout);
		constraintSet.clear(R.id.widget_name, ConstraintSet.END);
		constraintSet.connect(R.id.widget_name, ConstraintSet.START, R.id.container, ConstraintSet.START, 0);
		constraintSet.connect(R.id.widget_name, ConstraintSet.TOP, R.id.container, ConstraintSet.TOP, 0);

		constraintSet.clear(R.id.widget_text_small, ConstraintSet.TOP);
		constraintSet.connect(R.id.widget_text_small, ConstraintSet.TOP, R.id.container, ConstraintSet.TOP, 0);
		constraintSet.connect(R.id.widget_text_small, ConstraintSet.END, R.id.container, ConstraintSet.END, 0);

		constraintSet.clear(R.id.widget_text, ConstraintSet.START);
		constraintSet.clear(R.id.widget_text, ConstraintSet.END);
		constraintSet.clear(R.id.widget_text, ConstraintSet.TOP);
		constraintSet.connect(R.id.widget_text, ConstraintSet.START, R.id.container, ConstraintSet.START, 0);
		constraintSet.connect(R.id.widget_text, ConstraintSet.END, R.id.container, ConstraintSet.END, 0);
		constraintSet.connect(R.id.widget_text, ConstraintSet.TOP, R.id.widget_name, ConstraintSet.BOTTOM, 0);

		constraintSet.clear(R.id.widget_icon, ConstraintSet.TOP);
		constraintSet.connect(R.id.widget_icon, ConstraintSet.TOP, R.id.widget_name, ConstraintSet.BOTTOM, 0);
		constraintSet.applyTo(constraintLayout);
	}

	public void recreateViewIfNeeded(@NonNull WidgetsPanel panel) {
		boolean oldWidgetOrientation = isVerticalWidget;
		setVerticalWidget(panel);
		if (oldWidgetOrientation != isVerticalWidget) {
			View oldView = view;
			ImageView oldImageView = imageView;
			TextView oldTextView = textView;
			TextView oldTextViewShadow = textViewShadow;
			TextView oldSmallTextView = smallTextView;
			TextView oldSmallTextViewShadow = smallTextViewShadow;
			View oldContainer = container;
			View oldEmptyBanner = emptyBanner;
			View oldBottomDivider = bottomDivider;

			createView(getLayoutId(mapActivity, customId, widgetType, panel));
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

	protected void setTextNoUpdateVisibility(String text, String subtext) {
		setContentDescription(combine(text, subtext));
		if (text == null) {
			setText("");
		} else {
			setText(text);
		}
		if (subtext == null) {
			setSmallText("");
		} else {
			setSmallText(subtext);
		}
	}

	private void setText(String text) {
		textView.setText(text);
		if (textViewShadow != null) {
			textViewShadow.setText(text);
		}
	}

	private void setSmallText(String text) {
		smallTextView.setText(text);
		if (smallTextViewShadow != null) {
			smallTextViewShadow.setText(text);
		}
	}

	@Override
	public void updateColors(@NonNull MapInfoLayer.TextState textState) {
		super.updateColors(textState);
		updateTextColor(smallTextView, null, textState.textColor, textState.textShadowColor,
				textState.textBold, textState.textShadowRadius);
		updateTextColor(textView, null, textState.textColor, textState.textShadowColor,
				textState.textBold, textState.textShadowRadius);
		int iconId = getIconId();
		if (iconId != 0) {
			setImageDrawable(iconId);
		}

		view.setBackgroundResource(ColorUtilities.getWidgetBackgroundColorId(isNightMode()));
		bottomDivider.setBackgroundResource(textState.widgetDividerColorId);
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
