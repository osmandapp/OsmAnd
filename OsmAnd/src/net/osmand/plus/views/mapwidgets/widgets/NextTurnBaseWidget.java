package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget.MAX_SHIELDS_QUANTITY;
import static net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget.setShieldImage;
import static java.lang.Math.min;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
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

import com.github.ksoichiro.android.observablescrollview.ScrollUtils;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.RoadShield;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatterParams;
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
	public static final int SHIELD_HEIGHT_DP = 40;

	protected boolean horizontalMini;
	protected int deviatedPath;
	protected int nextTurnDistance;
	private final TextPaint textPaint = new TextPaint();
	private TurnDrawable turnDrawable;

	private ImageView topImageView;
	private TextView topTextView;
	private ViewGroup bottomLayout;

	private TurnType turnType;
	private OutlinedTextContainer distanceView;
	private OutlinedTextContainer distanceSubView;
	private OutlinedTextContainer streetView;
	private TextView exitView;
	private ImageView arrowView;
	private LinearLayout shieldImagesContainer;
	private LinearLayout bg;

	private List<RoadShield> cachedRoadShields;
	private final ResizableWidgetState widgetState;
	protected TextState textState;
	private boolean isFullRow;
	protected boolean verticalWidget;

	public NextTurnBaseWidget(@NonNull MapActivity mapActivity, @Nullable String customId,
			@NonNull WidgetType widgetType, @Nullable WidgetsPanel panel,
			boolean horizontalMini) {
		super(mapActivity, widgetType, customId, panel);
		this.horizontalMini = horizontalMini;
		widgetState = new ResizableWidgetState(app, customId, widgetType, WidgetSize.MEDIUM);

		WidgetsPanel selectedPanel = panel != null ? panel : widgetType.getPanel(customId != null ? customId : widgetType.id, settings);
		setVerticalWidget(selectedPanel);
		setupViews();

		turnDrawable = new TurnDrawable(mapActivity, !verticalWidget && horizontalMini);
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
		setOnLongClickListener(v -> {
			WidgetsContextMenu.showMenu(view, mapActivity, widgetType, customId, null, panel, nightMode, true);
			return true;
		});
		setOnClickListener(getOnClickListener());
	}

	public void updateWidgetView() {
		if (verticalWidget) {
			app.getOsmandMap().getMapLayers().getMapInfoLayer().updateRow(this);
			checkShieldOverflow();
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
			smallTextViewShadow = view.findViewById(R.id.widget_text_small_shadow);
			smallTextView = view.findViewById(R.id.widget_text_small);
			bottomDivider = view.findViewById(R.id.bottom_divider);
		}
	}

	public void setTurnType(TurnType turnType) {
		this.turnType = turnType;
		boolean visibilityUpdated = !verticalWidget && updateVisibility(turnType != null);
		if (turnDrawable.setTurnType(turnType) || visibilityUpdated) {
			turnDrawable.updateColors(isNightMode());
			if (verticalWidget) {
				setVerticalImage(turnDrawable);
			} else {
				turnDrawable.updateTextPaint(textPaint, nightMode);
				if (horizontalMini) {
					setImageDrawable(turnDrawable, false);
				} else {
					setTopImageDrawable(turnDrawable, "");
				}
			}
		}
	}

	private void setRoadShield(@NonNull List<RoadShield> shields) {
		boolean isShieldSet = false;
		if (!Algorithms.isEmpty(shields)) {
			shieldImagesContainer.removeAllViews();
			int maxShields = min(shields.size(), MAX_SHIELDS_QUANTITY);
			for (int i = 0; i < maxShields; i++) {
				RoadShield shield = shields.get(i);
				isShieldSet |= setShieldImage(shield, mapActivity, shieldImagesContainer, isNightMode());
			}
		}
		AndroidUiHelper.updateVisibility(shieldImagesContainer, isShieldSet);
		checkShieldOverflow();
	}

	public void setStreetName(@Nullable CurrentStreetName streetName) {
		if (streetName == null || !verticalWidget) {
			AndroidUiHelper.updateVisibility(exitView, false);
			AndroidUiHelper.updateVisibility(shieldImagesContainer, false);
			return;
		}
		if (!Algorithms.isEmpty(streetName.text)) {
			streetName.text = removeSymbol(streetName.text);
		}
		List<RoadShield> shields = streetName.shields;
		if (!shields.isEmpty() && app.getRendererRegistry().getCurrentSelectedRenderer() != null) {
			if (!shields.equals(cachedRoadShields) || (shields.equals(cachedRoadShields) && shieldImagesContainer.getChildCount() == 0)) {
				setRoadShield(shields);
			} else {
				AndroidUiHelper.updateVisibility(shieldImagesContainer, shields.equals(cachedRoadShields));
			}
			cachedRoadShields = shields;
		} else if (shields.isEmpty()) {
			AndroidUiHelper.updateVisibility(shieldImagesContainer, false);
			cachedRoadShields = null;
		}

		setExit(streetName);

		if (Algorithms.isEmpty(streetName.text)) {
			streetView.setText("");
		} else if (!streetName.text.equals(streetView.getText().toString())) {
			streetView.setText(streetName.text);
		}
	}

	private void checkShieldOverflow() {
		if (verticalWidget && WidgetSize.SMALL == getWidgetSizePref().get()) {
			ScrollUtils.addOnGlobalLayoutListener(shieldImagesContainer, () -> {
				int containerWidth = shieldImagesContainer.getWidth();
				int usedWidth = 0;
				int addedCount = 0;

				for (int i = 0; i < shieldImagesContainer.getChildCount(); i++) {
					View view = shieldImagesContainer.getChildAt(i);

					if (!(view instanceof ImageView image)) continue;

					Drawable drawable = image.getDrawable();
					int shieldWidth = drawable.getIntrinsicWidth();
					ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) image.getLayoutParams();
					int margins = params.getMarginStart() + params.getMarginEnd();
					int totalWidth = shieldWidth + margins;

					if (usedWidth + totalWidth <= containerWidth) {
						image.setVisibility(View.VISIBLE);
						usedWidth += totalWidth;
						addedCount++;
					} else {
						image.setVisibility(View.GONE);
					}
				}

				AndroidUiHelper.updateVisibility(shieldImagesContainer, addedCount != 0);
			});
		}
	}

	private void setExit(@NonNull CurrentStreetName streetName) {
		String exitNumber = null;
		if (turnType != null && turnType.getExitOut() > 0) {
			exitNumber = String.valueOf(turnType.getExitOut());
		} else if (!Algorithms.isEmpty(streetName.exitRef)) {
			exitNumber = streetName.exitRef;
		}

		if (!Algorithms.isEmpty(exitNumber)) {
			String exit = app.getString(R.string.shared_string_road_exit);
			String exitViewText = app.getString(R.string.ltr_or_rtl_combine_via_space, exit, exitNumber);
			exitView.setText(exitViewText);
			AndroidUiHelper.updateVisibility(exitView, true);
		} else {
			AndroidUiHelper.updateVisibility(exitView, false);
		}
	}

	public static String removeSymbol(String input) {
		if (input.startsWith("» ")) {
			return input.replace("» ", "");
		}
		return input;
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
		String distance = OsmAndFormatter.getFormattedDistance(deviatePath, app, OsmAndFormatterParams.USE_LOWER_BOUNDS);

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
			if (subText == null) {
				subText = "";
			}
			distanceSubView.setText(subText);
			formatSubText();
		} else {
			setTextNoUpdateVisibility(text, subText);
		}

		TurnType turnType = getTurnType();
		if (turnType != null) {
			setContentDescription(distance + " " + RouteCalculationResult.toString(turnType, app, true));
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
			updateTextColor(topTextView, null, textState.textColor,
					textState.textShadowColor, textState.textBold, textState.textShadowRadius);

			textPaint.set(topTextView.getPaint());
			textPaint.setColor(textState.textColor);
			turnDrawable.updateTextPaint(textPaint, isNightMode());
			turnDrawable.updateColors(isNightMode());
		}
		turnDrawable.invalidateSelf();
	}

	protected void updateVerticalWidgetColors(@NonNull TextState textState) {
		int typefaceStyle = textState.textBold ? Typeface.BOLD : Typeface.NORMAL;

		nightMode = textState.night;
		int exitRefTextColorId = isNightMode()
				? R.color.text_color_primary_dark
				: R.color.widgettext_day;
		exitView.setTextColor(ContextCompat.getColor(app, exitRefTextColorId));

		int streetNameColor = ColorUtilities.getColor(app, nightMode
				? R.color.text_color_tertiary_light
				: R.color.icon_color_secondary_dark);
		streetView.setTextColor(streetNameColor);

		distanceView.setTextColor(ContextCompat.getColor(app, exitRefTextColorId));
		distanceSubView.setTextColor(ColorUtilities.getSecondaryTextColor(mapActivity, nightMode));

		distanceView.setTypeface(Typeface.DEFAULT, typefaceStyle);
		distanceSubView.setTypeface(Typeface.DEFAULT, typefaceStyle);

		turnDrawable.updateColors(isNightMode());
		bg.setBackgroundResource(textState.widgetBackgroundId);

		updateTextOutline(distanceView, textState);
		updateTextOutline(distanceSubView, textState);
		updateTextOutline(streetView, textState);
	}

	@Override
	public void updateFullRowState(int widgetsCount) {
		boolean fullRow = widgetsCount <= 1;
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

		boolean shouldHide = shouldHide();
		boolean typeAllowed = widgetType != null && widgetType.isAllowed();
		boolean hasInfoToDisplay = (turnDrawable.getTurnType() != null || turnType != null || nextTurnDistance != 0);
		boolean visible = typeAllowed && !shouldHide && hasInfoToDisplay;
		updateVisibility(visible);
		if (typeAllowed && !shouldHide) {
			updateNavigationInfo(drawSettings);
		}
	}

	protected boolean shouldHide() {
		return visibilityHelper.shouldHideVerticalWidgets()
				|| panel == BOTTOM && visibilityHelper.shouldHideBottomWidgets();
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
	public boolean allowResize() {
		return verticalWidget;
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
			OutlinedTextContainer oldDistanceView = distanceView;
			OutlinedTextContainer oldDistanceSubView = distanceSubView;
			OutlinedTextContainer oldStreetView = streetView;
			TextView oldExitView = exitView;
			ImageView oldArrowView = arrowView;
			View oldShieldContainer = shieldImagesContainer;
			TurnType type = turnDrawable.getTurnType();
			int turnImminent = turnDrawable.getTurnImminent();
			boolean deviatedFromRoute = turnDrawable.isDeviatedFromRoute();

			setupViews();

			turnDrawable = new TurnDrawable(mapActivity, !verticalWidget && horizontalMini);
			turnDrawable.setTurnType(type);
			turnDrawable.setTurnImminent(turnImminent, deviatedFromRoute);
			setVerticalImage(turnDrawable);
			copyView(shieldImagesContainer, oldShieldContainer);
			copyView(arrowView, oldArrowView);
			copyTextView(distanceView, oldDistanceView);
			copyTextView(distanceSubView, oldDistanceSubView);
			copyTextView(streetView, oldStreetView);
			copyTextView(exitView, oldExitView);

			formatSubText();
		} else {
			ImageView oldImageView = imageView;
			OutlinedTextContainer oldTextView = textView;
			OutlinedTextContainer oldSmallTextView = smallTextView;
			TextView oldSmallTextViewShadow = smallTextViewShadow;
			View oldEmptyBanner = emptyBanner;

			setupViews();

			imageView.setImageDrawable(oldImageView.getDrawable());
			copyView(imageView, oldImageView);
			copyTextView(textView, oldTextView);
			copyTextView(smallTextView, oldSmallTextView);
			copyTextView(smallTextViewShadow, oldSmallTextViewShadow);
			copyView(emptyBanner, oldEmptyBanner);
		}
		view.setVisibility(oldContainer.getVisibility());
	}

	private void formatSubText() {
		if (distanceSubView == null || Algorithms.isEmpty(distanceSubView.getText().toString())) {
			return;
		}

		WidgetSize currentWidgetSize = widgetState.getWidgetSizePref().get();
		String subText = distanceSubView.getText().toString();
		String formattedSubText = null;
		switch (currentWidgetSize) {
			case SMALL -> {
				boolean shouldShowComma = !Algorithms.isEmpty(streetView.getText()) || !Algorithms.isEmpty(exitView.getText());
				if (shouldShowComma && !subText.endsWith(",")) {
					formattedSubText = subText + ",";
				}
			}
			case MEDIUM, LARGE -> {
				if (subText.endsWith(",")) {
					formattedSubText = subText.substring(0, subText.length() - 1);
				}
			}
		}

		if (formattedSubText != null) {
			distanceSubView.setText(formattedSubText);
		}
	}

	@Nullable
	protected View.OnClickListener getOnClickListener() {
		return null;
	}

	private void copyTextView(@Nullable TextView newTextView, @Nullable TextView oldTextView) {
		if (newTextView != null && oldTextView != null) {
			newTextView.setText(oldTextView.getText());
			copyView(newTextView, oldTextView);
		}
	}

	private void copyTextView(@Nullable OutlinedTextContainer newTextView, @Nullable OutlinedTextContainer oldTextView) {
		if (newTextView != null && oldTextView != null) {
			newTextView.copyFromTextContainer(oldTextView);
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