package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

import static net.osmand.plus.views.mapwidgets.WidgetType.ROUTE_INFO;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget.DISTANCE_CHANGE_THRESHOLD;
import static net.osmand.plus.views.mapwidgets.widgets.TimeToNavigationPointWidget.UPDATE_INTERVAL_SECONDS;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.PaintedText;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatterParams;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
import net.osmand.plus.views.mapwidgets.WidgetsContextMenu;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportMultiRow;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportVerticalPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.routeinfo.RouteInfoCalculator.DestinationInfo;
import net.osmand.plus.views.mapwidgets.widgetstates.RouteInfoWidgetState;
import net.osmand.plus.widgets.MultiTextViewEx;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RouteInfoWidget extends MapWidget implements ISupportVerticalPanel, ISupportWidgetResizing, ISupportMultiRow {

	private final RouteInfoWidgetState widgetState;

	private TextState textState;
	private final RouteInfoCalculator calculator;
	private List<DestinationInfo> cachedRouteInfo;
	private DisplayValue cachedDefaultView;
	private DisplayPriority cachedDisplayPriority;
	private Integer cachedMetricSystem;
	private boolean forceUpdate = false;
	private boolean hasEnoughWidth;
	private boolean hasSecondaryData;
	private int widgetWidth;

	// views
	private OutlinedTextContainer tvPrimaryLine1;
	private OutlinedTextContainer tvSecondaryLine1;
	private OutlinedTextContainer tvPrimaryLine2;
	private OutlinedTextContainer tvSecondaryLine2;

	public RouteInfoWidget(@NonNull MapActivity mapActivity, @Nullable String customId,
	                       @Nullable WidgetsPanel panel) {
		super(mapActivity, ROUTE_INFO, customId, panel);
		widgetState = new RouteInfoWidgetState(app, customId);
		calculator = new RouteInfoCalculator(mapActivity);

		setupViews();
		updateVisibility(false);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.simple_widget_vertical_content_container;
	}

	@LayoutRes
	protected int getContentLayoutId() {
		return switch (getWidgetSize()) {
			case SMALL -> R.layout.widget_route_information_small;
			case MEDIUM -> R.layout.widget_route_information_medium;
			case LARGE -> R.layout.widget_route_information_large;
		};
	}

	private void setupViews() {
		LinearLayout container = (LinearLayout) view;
		container.removeAllViews();
		LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		inflater.inflate(getContentLayoutId(), container);
		collectViews();
		if (textState != null) {
			view.setBackgroundResource(textState.widgetBackgroundId);
			int color = ColorUtilities.getSecondaryActiveColor(app, nightMode);
			Drawable normal = UiUtilities.createTintedDrawable(app, R.drawable.rectangle_rounded_small, color);

			int rippleDrawableId = nightMode ? R.drawable.ripple_solid_dark_3dp : R.drawable.ripple_solid_light_3dp;
			Drawable selected = AppCompatResources.getDrawable(app, rippleDrawableId);

			Drawable drawable = UiUtilities.getLayeredIcon(normal, selected);
			AndroidUtils.setBackground(view.findViewById(R.id.button_body), drawable);
		}
		updateWidgetRowView();

		View buttonTappableArea = view.findViewById(R.id.button_tappable_area);
		buttonTappableArea.setOnClickListener(v -> mapActivity.getMapActions().doRoute());

		view.setOnLongClickListener(v -> {
			WidgetsContextMenu.showMenu(view, mapActivity, widgetType, customId, null, panel, nightMode, true);
			return true;
		});

		WidgetSize size = getWidgetSize();
		boolean useSingleLine = (hasEnoughWidth && !hasSecondaryData) || size == WidgetSize.SMALL;
		tvPrimaryLine1.setGravity(Gravity.START | (useSingleLine ? Gravity.CENTER_VERTICAL : Gravity.TOP));

		AndroidUiHelper.setVisibility(!useSingleLine, tvSecondaryLine1);

		boolean secondaryBlockVisible = hasSecondaryData && hasEnoughWidth;
		View blocksDivider = view.findViewById(R.id.blocks_divider);
		View secondaryBlock = view.findViewById(R.id.secondary_block);
		AndroidUiHelper.setVisibility(secondaryBlockVisible, blocksDivider, secondaryBlock);
	}

	private void collectViews() {
		tvPrimaryLine1 = view.findViewById(R.id.primary_line_1);
		tvSecondaryLine1 = view.findViewById(R.id.secondary_line_1);
		tvPrimaryLine2 = view.findViewById(R.id.primary_line_2);
		tvSecondaryLine2 = view.findViewById(R.id.secondary_line_2);
	}

	@Override
	public void updateFullRowState(int widgetsCount) {
		if (widgetsCount == 0) return;
		int screenWidth = AndroidUtils.getScreenWidth(mapActivity);
		widgetWidth = screenWidth / widgetsCount;
		boolean hasEnoughWidth = widgetWidth >= AndroidUtils.dpToPx(app, 240);
		if (this.hasEnoughWidth != hasEnoughWidth) {
			this.hasEnoughWidth = hasEnoughWidth;
			recreateView();
			if (textState != null) {
				updateColors(textState);
			}
		}
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		updateInfoInternal();
	}

	private void updateInfoInternal() {
		boolean shouldHide = shouldHide();
		boolean typeAllowed = widgetType != null && widgetType.isAllowed();
		if (typeAllowed && !shouldHide) {
			updateRouteInformation();
		} else {
			updateVisibility(false);
		}
	}

	protected boolean shouldHide() {
		return visibilityHelper.shouldHideVerticalWidgets()
				|| panel == BOTTOM && visibilityHelper.shouldHideBottomWidgets();
	}

	private void updateRouteInformation() {
		ApplicationMode appMode = settings.getApplicationMode();
		DisplayPriority priority = getDisplayPriority(appMode);

		List<DestinationInfo> routeInfo = calculator.calculateRouteInformation(priority);
		if (Algorithms.isEmpty(routeInfo)) {
			updateVisibility(false);
			return;
		}
		boolean visibilityChanged = updateVisibility(true);

		if (!forceUpdate && !visibilityChanged && !isUpdateNeeded(routeInfo)) {
			return;
		}
		cachedRouteInfo = routeInfo;

		boolean hasSecondaryData = hasSecondaryData();
		if (this.hasSecondaryData != hasSecondaryData) {
			this.hasSecondaryData = hasSecondaryData;
			// Call recreate view to trigger layout changes
			recreateView();
			return;
		}

		DisplayValue defaultView = getDefaultView(appMode);
		DisplayValue[] orderedDisplayValues = DisplayValue.values(defaultView);

		updatePrimaryBlock(cachedRouteInfo.get(0), orderedDisplayValues);
		updateSecondaryBlock(cachedRouteInfo.size() > 1 ? cachedRouteInfo.get(1) : null, orderedDisplayValues);
		forceUpdate = false;
		view.post(this::applySuitableTextSize);
	}

	private void updatePrimaryBlock(@NonNull DestinationInfo destinationInfo,
	                                @NonNull DisplayValue[] displayValues) {
		WidgetSize size = getWidgetSize();
		int primaryColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		int secondaryColor = ColorUtilities.getSecondaryTextColor(app, nightMode);

		Map<DisplayValue, String> data = prepareDisplayData(destinationInfo);
		String value1 = Objects.requireNonNull(data.get(displayValues[0]));
		String value2 = Objects.requireNonNull(data.get(displayValues[1]));
		String value3 = Objects.requireNonNull(data.get(displayValues[2]));

		List<PaintedText> primaryLineText = new ArrayList<>();
		List<PaintedText> secondaryLineText = new ArrayList<>();
		primaryLineText.add(new PaintedText(value1, primaryColor));
		if (!hasEnoughWidth) {
			if (size == WidgetSize.SMALL) {
				primaryLineText.add(new PaintedText(value2, primaryColor));
				primaryLineText.add(new PaintedText(value3, secondaryColor));
			} else {
				secondaryLineText.add(new PaintedText(value2, primaryColor));
				secondaryLineText.add(new PaintedText(value3, secondaryColor));
			}
		} else if (hasSecondaryData) {
			primaryLineText.add(new PaintedText(value2, primaryColor));
			if (size == WidgetSize.SMALL) {
				primaryLineText.add(new PaintedText(value3, secondaryColor));
			} else {
				secondaryLineText.add(new PaintedText(value3, secondaryColor));
			}
		} else {
			primaryLineText.add(new PaintedText(value2, primaryColor));
			primaryLineText.add(new PaintedText(value3, secondaryColor));
		}
		tvPrimaryLine1.setMultiText(primaryLineText);
		if (tvSecondaryLine1 != null) {
			tvSecondaryLine1.setMultiText(secondaryLineText);
		}
	}

	private void updateSecondaryBlock(@Nullable DestinationInfo destinationInfo,
	                                  @NonNull DisplayValue[] displayValues) {
		if (destinationInfo != null) {
			Map<DisplayValue, String> data = prepareDisplayData(destinationInfo);
			tvPrimaryLine2.setText(data.get(displayValues[0]));
			tvSecondaryLine2.setText(data.get(displayValues[1]));
		} else {
			tvPrimaryLine2.setText("");
			tvSecondaryLine2.setText("");
		}
	}

	private void applySuitableTextSize() {
		WidgetSize widgetSize = getWidgetSize();
		TextSizeMode textSize = TextSizeMode.valueOf(widgetSize);
		boolean useSingleLine = widgetSize == WidgetSize.SMALL || (hasEnoughWidth && !hasSecondaryData);
		boolean hasSecondaryBlock = hasEnoughWidth && hasSecondaryData;

		// Calculate total space occupied by other (non-text) UI elements and space available for text
		int buttonWidthPx = view.findViewById(R.id.button_tappable_area).getMeasuredWidth();
		MarginLayoutParams params = (MarginLayoutParams) view.findViewById(R.id.primary_block).getLayoutParams();
		int primaryBlockMarginPx = params.getMarginEnd() + params.getMarginStart();

		int dividerWidth = view.findViewById(R.id.blocks_divider).getMeasuredWidth();
		params = (MarginLayoutParams) view.findViewById(R.id.secondary_block).getLayoutParams();
		int secondaryBlockMarginPx = params.getMarginEnd() + params.getMarginStart();
		int secondaryWidthPx = hasSecondaryBlock ? dividerWidth + secondaryBlockMarginPx : 0;

		int totalNotTextWidthPx = buttonWidthPx + primaryBlockMarginPx + secondaryWidthPx;
		int availableTextWidthPx = widgetWidth - totalNotTextWidthPx;

		int preferredTextSizePx = spToPx(useSingleLine ? textSize.getSingleLineTextSizeSp() : textSize.getMaxTextSizeSp());
		int minTextSizePx = spToPx(textSize.getMinTextSizeSp());

		if (widgetSize == WidgetSize.SMALL) {
			int secondaryBlockTextSizeSp = textSize.getMinTextSizeSp();
			int secondaryBlockTextSizePx = spToPx(secondaryBlockTextSizeSp);
			String tertiaryText1 = tvPrimaryLine2.getText().toString();
			String tertiaryText2 = tvSecondaryLine2.getText().toString();
			int secondaryBlockMaxTextWidth = Math.max(
					AndroidUtils.getTextWidth(tvPrimaryLine2.getTypeface(), secondaryBlockTextSizePx, tertiaryText1),
					AndroidUtils.getTextWidth(tvSecondaryLine2.getTypeface(), secondaryBlockTextSizePx, tertiaryText2)
			);
			availableTextWidthPx = availableTextWidthPx - secondaryBlockMaxTextWidth;
			String fullText = tvPrimaryLine1.getText().toString();
			int textSizePx = AndroidUtils.getMaxPossibleTextSize(fullText, tvPrimaryLine1.getTypeface(), availableTextWidthPx, minTextSizePx, preferredTextSizePx);
			tvPrimaryLine1.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
			tvPrimaryLine2.setTextSize(secondaryBlockTextSizeSp);
			tvSecondaryLine2.setTextSize(secondaryBlockTextSizeSp);
		} else {
			// Calculate possible text sizes for top and bottom lines
			String fullText = "" + tvPrimaryLine1.getText() + (hasSecondaryBlock ? tvPrimaryLine2.getText() : "");
			int textSizePx = AndroidUtils.getMaxPossibleTextSize(fullText, tvPrimaryLine1.getTypeface(), availableTextWidthPx, minTextSizePx, preferredTextSizePx);
			tvPrimaryLine1.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
			tvPrimaryLine2.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);

			preferredTextSizePx = spToPx(textSize.getSecondaryMaxTextSizeSp());
			minTextSizePx = spToPx(textSize.getMinTextSizeSp());
			fullText = "";
			if (tvSecondaryLine1 != null) {
				fullText += tvSecondaryLine1.getText();
			}
			if (hasSecondaryBlock) {
				fullText += tvSecondaryLine2.getText();
			}
			textSizePx = AndroidUtils.getMaxPossibleTextSize(fullText, tvSecondaryLine2.getTypeface(), availableTextWidthPx, minTextSizePx, preferredTextSizePx);
			if (tvSecondaryLine1 != null) {
				tvSecondaryLine1.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
			}
			tvSecondaryLine2.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
		}
	}

	@NonNull
	private Map<DisplayValue, String> prepareDisplayData(@NonNull DestinationInfo info) {
		Map<DisplayValue, String> displayData = new HashMap<>();
		displayData.put(DisplayValue.ARRIVAL_TIME, formatArrivalTime(app, info.arrivalTime()));
		displayData.put(DisplayValue.TIME_TO_GO, formatDuration(app, info.timeToGo()));
		displayData.put(DisplayValue.DISTANCE, formatDistance(app, info.distance()));
		return displayData;
	}

	private boolean isUpdateNeeded(@NonNull List<DestinationInfo> routeInfo) {
		int metricSystem = settings.METRIC_SYSTEM.get().ordinal();
		boolean metricSystemChanged = cachedMetricSystem == null || cachedMetricSystem != metricSystem;
		cachedMetricSystem = metricSystem;
		if (metricSystemChanged) {
			return true;
		}
		DisplayValue defaultView = widgetState.getDefaultView();
		if (cachedDefaultView != defaultView) {
			cachedDefaultView = defaultView;
			return true;
		}
		DisplayPriority displayPriority = widgetState.getDisplayPriority();
		if (cachedDisplayPriority != displayPriority) {
			cachedDisplayPriority = displayPriority;
			return true;
		}
		if (Algorithms.isEmpty(cachedRouteInfo) || cachedRouteInfo.size() != routeInfo.size()) {
			return true;
		}
		for (int i = 0; i < routeInfo.size(); i++) {
			if (isDataChanged(cachedRouteInfo.get(i), routeInfo.get(i))) {
				return true;
			}
		}
		return false;
	}

	private boolean isDataChanged(@NonNull DestinationInfo i1, @NonNull DestinationInfo i2) {
		int distanceDif = Math.abs(i1.distance() - i2.distance());
		long timeToGoDif = Math.abs(i1.timeToGo() - i2.timeToGo());
		return distanceDif > DISTANCE_CHANGE_THRESHOLD || timeToGoDif > UPDATE_INTERVAL_SECONDS * 1000L;
	}

	@Override
	public void updateColors(@NonNull TextState textState) {
		this.textState = textState;
		this.nightMode = textState.night;
		recreateView();
		updateTextOutline(tvPrimaryLine1, textState);
		updateTextOutline(tvSecondaryLine1, textState);
		updateTextOutline(tvPrimaryLine2, textState);
		updateTextOutline(tvSecondaryLine2, textState);
		forceUpdate = true;
		updateInfoInternal();
	}

	@Override
	public boolean updateVisibility(boolean visible) {
		if (super.updateVisibility(visible)) {
			updateWidgetRowView();
			return true;
		}
		return false;
	}

	public void updateWidgetRowView() {
		app.getOsmandMap().getMapLayers().getMapInfoLayer().updateRow(this);
	}

	@Override
	public boolean allowResize() {
		return true;
	}

	@NonNull
	public WidgetSize getWidgetSize() {
		return getWidgetSizePref().get();
	}

	@NonNull
	@Override
	public OsmandPreference<WidgetSize> getWidgetSizePref() {
		return widgetState.getWidgetSizePref();
	}

	@Override
	public void recreateView() {
		forceUpdate = true;
		setupViews();
		updateInfoInternal();
	}

	private boolean hasSecondaryData() {
		return cachedRouteInfo != null && cachedRouteInfo.size() > 1;
	}

	@NonNull
	public DisplayValue getDefaultView(@NonNull ApplicationMode appMode) {
		return widgetState.getDefaultView(appMode);
	}

	public void setDefaultView(@NonNull ApplicationMode appMode,
	                           @NonNull DisplayValue defaultView) {
		widgetState.setDefaultView(appMode, defaultView);
	}

	@NonNull
	public DisplayPriority getDisplayPriority(@NonNull ApplicationMode appMode) {
		return widgetState.getDisplayPriority(appMode);
	}

	public void setDisplayPriority(@NonNull ApplicationMode appMode,
	                               @NonNull DisplayPriority displayPriority) {
		widgetState.setDisplayPriority(appMode, displayPriority);
	}

	private int spToPx(int sp) {
		return AndroidUtils.spToPx(app, sp);
	}

	@NonNull
	public static String formatArrivalTime(@NonNull Context ctx, long time) {
		Pair<String, String> formattedTime = OsmAndFormatter.getFormattedTime(ctx, time);
		if (formattedTime.second != null) {
			String pattern = ctx.getString(R.string.ltr_or_rtl_combine_via_space);
			return String.format(pattern, formattedTime.first, formattedTime.second);
		}
		return formattedTime.first;
	}

	@NonNull
	public static String formatDuration(@NonNull OsmandApplication app, long timeLeft) {
		long diffInMinutes = TimeUnit.SECONDS.convert(timeLeft, TimeUnit.MILLISECONDS);
		return OsmAndFormatter.getFormattedDuration(diffInMinutes, app);
	}

	@NonNull
	public static String formatDistance(@NonNull OsmandApplication ctx, float meters) {
		return OsmAndFormatter.getFormattedDistance(meters, ctx, OsmAndFormatterParams.USE_LOWER_BOUNDS);
	}
}
