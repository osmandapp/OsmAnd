package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget.MAX_SHIELDS_QUANTITY;
import static net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget.setShieldImage;

import static java.lang.Math.min;

import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.CurrentStreetName.RoadShield;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.TurnDrawable;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsContextMenu;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.IComplexWidget;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportMultiRow;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportVerticalPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgetstates.ResizableWidgetState;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;

import java.util.List;

public class NextTurnBaseWidget extends TextInfoWidget implements IComplexWidget, ISupportVerticalPanel, ISupportWidgetResizing, ISupportMultiRow {

	private static final int DISTANCE_CHANGE_THRESHOLD = 10;

	protected boolean horizontalMini;
	protected int deviatedPath;
	protected int nextTurnDistance;
	private TurnDrawable turnDrawable;

	private ImageView topImageView;
	private TextView topTextView;
	private ViewGroup bottomLayout;

	private TurnType turnType;
	private TextView distanceView;
	private TextView distanceSubView;
	private TextView streetView;
	private TextView exitView;
	private ImageView arrowView;
	private LinearLayout shieldImagesContainer;
	private LinearLayout bg;


	@Nullable
	protected String customId;
	private List<RoadShield> cachedRoadShields;
	private final ResizableWidgetState widgetState;
	protected TextState textState;
	private boolean isFullRow;
	protected boolean verticalWidget;

	public NextTurnBaseWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @NonNull WidgetType widgetType, @Nullable WidgetsPanel panel, boolean horizontalMini) {
		super(mapActivity, widgetType);
		this.horizontalMini = horizontalMini;
		this.customId = customId;
		widgetState = new ResizableWidgetState(app, customId, widgetType);

		WidgetsPanel selectedPanel = panel != null ? panel : widgetType.getPanel(customId != null ? customId : widgetType.id, settings);
		setVerticalWidget(selectedPanel);
		setupViews();

		turnDrawable = new TurnDrawable(mapActivity, horizontalMini);
		if (verticalWidget) {
			setVerticalImage(turnDrawable);
		} else if (horizontalMini) {
			setImageDrawable(turnDrawable, false);
			setTopImageDrawable(null, null);
		} else {
			setImageDrawable(null, true);
			setTopImageDrawable(turnDrawable, "");
		}

		updateVisibility(false);
	}

	public void setVerticalWidget(@NonNull WidgetsPanel panel) {
		verticalWidget = panel.isPanelVertical();
	}

	private void setupViews() {
		LinearLayout container = (LinearLayout) view;
		container.removeAllViews();

		int layoutId = getContentLayoutId();
		UiUtilities.getInflater(mapActivity, nightMode).inflate(layoutId, container);
		findViews();
		updateWidgetView();
		view.setOnLongClickListener(v -> {
			WidgetsContextMenu.showMenu(view, mapActivity, widgetType, customId, null, verticalWidget, nightMode);
			return true;
		});
	}

	public void updateWidgetView() {
		if (verticalWidget) {
			app.getOsmandMap().getMapLayers().getMapInfoLayer().updateRow(this);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.simple_widget_vertical_content_container;
	}

	@LayoutRes
	protected int getContentLayoutId() {
		return verticalWidget ? getProperVerticalLayoutId(widgetState) : R.layout.map_hud_widget;
	}

	@LayoutRes
	private int getProperVerticalLayoutId(@NonNull ResizableWidgetState resizableWidgetState) {
		return switch (resizableWidgetState.getWidgetSizePref().get()) {
			case SMALL -> R.layout.navigation_widget_small;
			case MEDIUM ->
					isFullRow ? R.layout.navigation_widget_full : R.layout.navigation_widget_half;
			case LARGE ->
					isFullRow ? R.layout.navigation_widget_full_large : R.layout.navigation_widget_half_large;
		};
	}

	private void findViews() {
		container = view.findViewById(R.id.container);
		if (verticalWidget) {
			bg = view.findViewById(R.id.widget_bg);
			distanceView = view.findViewById(R.id.distance_text);
			distanceSubView = view.findViewById(R.id.distance_sub_text);
			streetView = view.findViewById(R.id.street_text);
			exitView = view.findViewById(R.id.map_exit_ref);
			arrowView = view.findViewById(R.id.arrow_icon);
			shieldImagesContainer = view.findViewById(R.id.map_shields_container);
		} else {
			topImageView = view.findViewById(R.id.widget_top_icon);
			topTextView = view.findViewById(R.id.widget_top_icon_text);
			bottomLayout = view.findViewById(R.id.widget_bottom_layout);
			emptyBanner = view.findViewById(R.id.empty_banner);
			imageView = view.findViewById(R.id.widget_icon);
			textView = view.findViewById(R.id.widget_text);
			textViewShadow = view.findViewById(R.id.widget_text_shadow);
			smallTextViewShadow = view.findViewById(R.id.widget_text_small_shadow);
			smallTextView = view.findViewById(R.id.widget_text_small);
			bottomDivider = view.findViewById(R.id.bottom_divider);
		}
	}

	public void setTurnType(TurnType turnType) {
		if (verticalWidget) {
			this.turnType = turnType;
			if (turnDrawable.setTurnType(turnType)) {
				setVerticalImage(turnDrawable);
			}
		} else {
			boolean vis = updateVisibility(turnType != null);
			if (turnDrawable.setTurnType(turnType) || vis) {
				turnDrawable.setTextPaint(topTextView.getPaint());
				if (horizontalMini) {
					setImageDrawable(turnDrawable, false);
				} else {
					setTopImageDrawable(turnDrawable, "");
				}
			}
		}
	}

	private boolean setRoadShield(@NonNull List<RoadShield> shields) {
		if (!Algorithms.isEmpty(shields)) {
			boolean isShieldSet = false;
			shieldImagesContainer.removeAllViews();
			int maxShields = min(shields.size(), MAX_SHIELDS_QUANTITY);
			for (int i = 0; i < maxShields; i++) {
				RoadShield shield = shields.get(i);
				isShieldSet |= setShieldImage(shield, mapActivity, shieldImagesContainer, isNightMode());
			}
			return isShieldSet;
		}
		return false;
	}

	public void setStreetName(@Nullable CurrentStreetName streetName) {
		if (streetName == null || !verticalWidget) {
			AndroidUiHelper.updateVisibility(exitView, false);
			AndroidUiHelper.updateVisibility(shieldImagesContainer, false);
			return;
		}
		List<RoadShield> shields = streetName.shields;
		if (!shields.isEmpty() && app.getRendererRegistry().getCurrentSelectedRenderer() != null) {
			if(!shields.equals(cachedRoadShields) || (shields.equals(cachedRoadShields) && shieldImagesContainer.getChildCount() == 0)){
				if (setRoadShield(shields)) {
					AndroidUiHelper.updateVisibility(shieldImagesContainer, true);
					int indexOf = streetName.text.indexOf("»");
					if (indexOf > 0) {
						streetName.text = streetName.text.substring(indexOf);
					}
				} else {
					AndroidUiHelper.updateVisibility(shieldImagesContainer, false);
				}
			} else AndroidUiHelper.updateVisibility(shieldImagesContainer, shields.equals(cachedRoadShields));
			cachedRoadShields = shields;
		} else if (shields.isEmpty()) {
			AndroidUiHelper.updateVisibility(shieldImagesContainer, false);
			cachedRoadShields = null;
		}

		if (Algorithms.isEmpty(streetName.exitRef)) {
			AndroidUiHelper.updateVisibility(exitView, false);
		} else {
			exitView.setText(streetName.exitRef);
			AndroidUiHelper.updateVisibility(exitView, true);
		}

		if (Algorithms.isEmpty(streetName.text)) {
			streetView.setText("");
		} else if (!streetName.text.equals(streetView.getText().toString())) {
			streetView.setText(streetName.text);
		}
	}

	public void setVerticalImage(@Nullable TurnDrawable imageDrawable) {
		if (imageDrawable != null) {
			arrowView.setImageDrawable(imageDrawable);
			arrowView.invalidate();
		}
	}

	public void setTopImageDrawable(@Nullable Drawable imageDrawable, @Nullable String topText) {
		boolean hasImage = imageDrawable != null;
		if (hasImage) {
			topImageView.setImageDrawable(imageDrawable);
			topTextView.setText(topText == null ? "" : topText);

			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
			lp.gravity = Gravity.CENTER_HORIZONTAL;
			bottomLayout.setLayoutParams(lp);
			bottomLayout.invalidate();
		} else {
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
			lp.gravity = Gravity.NO_GRAVITY;
			bottomLayout.setLayoutParams(lp);
		}

		AndroidUiHelper.updateVisibility(topImageView, hasImage);
		AndroidUiHelper.updateVisibility(topTextView, hasImage);

		topTextView.invalidate();
		topImageView.invalidate();
	}

	public void setTurnImminent(int turnImminent, boolean deviatedFromRoute) {
		if (turnDrawable.getTurnImminent() != turnImminent || turnDrawable.isDeviatedFromRoute() != deviatedFromRoute) {
			turnDrawable.setTurnImminent(turnImminent, deviatedFromRoute);
		}
	}

	public void setDeviatePath(int deviatePath) {
		if (isDistanceChanged(this.deviatedPath, deviatePath)) {
			this.deviatedPath = deviatePath;
			updateDistance();
		}
	}

	public void setTurnDistance(int nextTurnDistance) {
		if (isDistanceChanged(this.nextTurnDistance, nextTurnDistance)) {
			this.nextTurnDistance = nextTurnDistance;
			updateDistance();
		}
	}

	private boolean isDistanceChanged(int oldDistance, int distance) {
		return oldDistance == 0 || Math.abs(oldDistance - distance) >= DISTANCE_CHANGE_THRESHOLD;
	}

	private void updateDistance() {
		int deviatePath = turnDrawable.isDeviatedFromRoute() ? deviatedPath : nextTurnDistance;
		String distance = OsmAndFormatter.getFormattedDistance(deviatePath, app, OsmAndFormatter.OsmAndFormatterParams.USE_LOWER_BOUNDS);

		String text;
		String subText = null;
		int ls = distance.lastIndexOf(' ');
		if (ls == -1) {
			text = distance;
		} else {
			text = distance.substring(0, ls);
			subText = distance.substring(ls + 1);
		}

		if (verticalWidget) {
			distanceView.setText(text);
			distanceSubView.setText(subText == null ? "" : subText);
		} else {
			setTextNoUpdateVisibility(text, subText);
		}

		TurnType turnType = getTurnType();
		if (turnType != null) {
			setContentDescription(distance + " " + RouteCalculationResult.toString(turnType, app, false));
		} else {
			setContentDescription(distance);
		}
	}

	public TurnType getTurnType() {
		return turnDrawable.getTurnType();
	}

	@Override
	public void updateColors(@NonNull TextState textState) {
		this.textState = textState;
		if (verticalWidget) {
			updateVerticalWidgetColors(textState);
		} else {
			super.updateColors(textState);
			updateTextColor(topTextView, null, textState.textColor, textState.textShadowColor, textState.textBold,
					textState.textShadowRadius);
		}
	}

	protected void updateVerticalWidgetColors(@NonNull TextState textState) {
		nightMode = textState.night;
		int exitRefTextColorId = isNightMode()
				? R.color.text_color_primary_dark
				: R.color.widgettext_day;
		exitView.setTextColor(ContextCompat.getColor(app, exitRefTextColorId));

		distanceView.setTextColor(ContextCompat.getColor(app, exitRefTextColorId));
		distanceSubView.setTextColor(ColorUtilities.getSecondaryTextColor(mapActivity, nightMode));
		streetView.setTextColor(ColorUtilities.getSecondaryTextColor(mapActivity, nightMode));

		bg.setBackgroundResource(textState.widgetBackgroundId);
	}

	@Override
	public void updateFullRowState(boolean fullRow) {
		if (isFullRow != fullRow) {
			isFullRow = fullRow;
			recreateView();
			if (textState != null) {
				updateColors(textState);
			}
			updateInfo(null);
		}
	}

	@Override
	public final void updateInfo(@Nullable DrawSettings drawSettings) {
		if (!verticalWidget) {
			updateNavigationInfo(drawSettings);
			return;
		}

		boolean shouldHideTopWidgets = mapActivity.getWidgetsVisibilityHelper().shouldHideVerticalWidgets();
		boolean typeAllowed = widgetType != null && widgetType.isAllowed();
		boolean hasInfoToDisplay = (turnDrawable.getTurnType() != null || turnType != null || nextTurnDistance != 0);
		boolean visible = typeAllowed && !shouldHideTopWidgets && hasInfoToDisplay;
		updateVisibility(visible);
		if (typeAllowed && !shouldHideTopWidgets) {
			updateNavigationInfo(drawSettings);
		}
	}

	void updateNavigationInfo(@Nullable DrawSettings drawSettings) {

	}

	@Override
	public boolean updateVisibility(boolean visible) {
		boolean updatedVisibility = super.updateVisibility(visible);
		if (verticalWidget && updatedVisibility) {
			app.getOsmandMap().getMapLayers().getMapInfoLayer().updateRow(this);
		}
		return updatedVisibility;
	}

	@Override
	protected View getContentView() {
		return verticalWidget ? view : container;
	}

	@Override
	public void recreateViewIfNeeded(@NonNull WidgetsPanel panel) {
		boolean oldWidgetOrientation = verticalWidget;
		setVerticalWidget(panel);
		if (oldWidgetOrientation != verticalWidget) {
			recreateView();
		}
	}

	@Override
	public void updateValueAlign(boolean fullRow) {
	}

	@NonNull
	@Override
	public OsmandPreference<WidgetSize> getWidgetSizePref() {
		return widgetState.getWidgetSizePref();
	}

	@Override
	public void recreateView() {
		View oldContainer = container;
		if (verticalWidget) {
			TextView oldDistanceView = distanceView;
			TextView oldDistanceSubView = distanceSubView;
			TextView oldStreetView = streetView;
			TextView oldExitView = exitView;
			ImageView oldArrowView = arrowView;
			View oldShieldContainer = shieldImagesContainer;
			TurnType type = turnDrawable.getTurnType();
			int turnImminent = turnDrawable.getTurnImminent();
			boolean deviatedFromRoute = turnDrawable.isDeviatedFromRoute();
			setupViews();
			turnDrawable = new TurnDrawable(mapActivity, horizontalMini);
			turnDrawable.setTurnType(type);
			turnDrawable.setTurnImminent(turnImminent,deviatedFromRoute);
			setVerticalImage(turnDrawable);
			copyView(shieldImagesContainer, oldShieldContainer);
			copyView(arrowView, oldArrowView);
			copyTextView(distanceView, oldDistanceView);
			copyTextView(distanceSubView, oldDistanceSubView);
			copyTextView(streetView, oldStreetView);
			copyTextView(exitView, oldExitView);
		} else {
			ImageView oldImageView = imageView;
			TextView oldTextView = textView;
			TextView oldTextViewShadow = textViewShadow;
			TextView oldSmallTextView = smallTextView;
			TextView oldSmallTextViewShadow = smallTextViewShadow;
			View oldEmptyBanner = emptyBanner;
			setupViews();
			imageView.setImageDrawable(oldImageView.getDrawable());
			copyView(imageView, oldImageView);
			copyTextView(textView, oldTextView);
			copyTextView(textViewShadow, oldTextViewShadow);
			copyTextView(smallTextView, oldSmallTextView);
			copyTextView(smallTextViewShadow, oldSmallTextViewShadow);
			copyView(emptyBanner, oldEmptyBanner);
		}
		view.setOnClickListener(getOnClickListener());
		view.setVisibility(oldContainer.getVisibility());
	}

	protected View.OnClickListener getOnClickListener(){
		return null;
	}

	private void copyTextView(@Nullable TextView newTextView, @Nullable TextView oldTextView) {
		if (newTextView != null && oldTextView != null) {
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
}