package net.osmand.plus.card.color.coloringstyle;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.IMultiStateCardController;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routing.ColoringStyleAlgorithms;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

public abstract class ColoringStyleCardController implements IMultiStateCardController {

	protected final OsmandApplication app;
	private final List<ColoringStyle> supportedColoringStyles;
	private ColoringStyle selectedColoringStyle;
	protected boolean nightMode;

	public ColoringStyleCardController(@NonNull OsmandApplication app,
	                                   @NonNull ColoringStyle selectedColoringStyle) {
		this.app = app;
		this.supportedColoringStyles = collectSupportedColoringStyles();
		this.selectedColoringStyle = selectedColoringStyle;
	}

	@NonNull
	@Override
	public String getMultiStateCardTitle() {
		return app.getString(R.string.shared_string_color);
	}

	@NonNull
	@Override
	public String getMultiStateSelectorTitle() {
		return selectedColoringStyle.toHumanString(app);
	}

	@Override
	public int getMultiStateSelectorAccentColor(boolean nightMode) {
		return ColorUtilities.getActiveColor(app, nightMode);
	}

	@NonNull
	@Override
	public abstract List<PopUpMenuItem> getMultiSateMenuItems();

	@Override
	public void onBindMultiStateCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode) {
		this.nightMode = nightMode;
		container.removeAllViews();
		BaseCard card = getContentCardForSelectedState(activity);
		View cardView = card.getView() != null ? card.getView() : card.build(activity);
		container.addView(cardView);
	}

	@Override
	public boolean onMultiStateMenuItemSelected(@NonNull FragmentActivity activity,
	                                            @NonNull View view, @NonNull PopUpMenuItem item) {
		ColoringStyle newColoringStyle = (ColoringStyle) item.getTag();
		if (!isAvailableColoringStyle(newColoringStyle)) {
			showUnavailableColoringStyleSnackbar(activity, newColoringStyle, view);
		} else if (!Objects.equals(selectedColoringStyle, newColoringStyle)) {
			selectColoringStyle(newColoringStyle);
			return true;
		}
		return false;
	}

	private void showUnavailableColoringStyleSnackbar(@NonNull FragmentActivity activity,
	                                                  @NonNull ColoringStyle coloringStyle,
	                                                  @NonNull View view) {
		ColoringType coloringType = coloringStyle.getType();
		String text = "";
		if (coloringType == ColoringType.SPEED) {
			text = app.getString(R.string.track_has_no_speed);
		} else if (CollectionUtils.equalsToAny(coloringType, ColoringType.ALTITUDE, ColoringType.SLOPE)) {
			text = app.getString(R.string.track_has_no_altitude);
		} else if (coloringType.isRouteInfoAttribute()) {
			text = app.getString(R.string.track_has_no_needed_data);
		}
		text += " " + app.getString(R.string.select_another_colorization);
		Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
				.setAnchorView(activity.findViewById(R.id.dismiss_button));
		UiUtilities.setupSnackbar(snackbar, nightMode);
		snackbar.show();
	}

	@NonNull
	protected List<String> collectRouteInfoAttributes() {
		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
		return RouteStatisticsHelper.getRouteStatisticAttrsNames(currentRenderer, defaultRenderer, true);
	}

	@NonNull
	protected ColoringStyle getSelectedColoringStyle() {
		return selectedColoringStyle;
	}

	protected void selectColoringStyle(@NonNull ColoringStyle coloringStyle) {
		this.selectedColoringStyle = coloringStyle;
		onColoringStyleSelected(coloringStyle);
	}

	protected abstract void onColoringStyleSelected(@NonNull ColoringStyle coloringStyle);

	@NonNull
	public List<ColoringStyle> getSupportedColoringStyles() {
		return supportedColoringStyles;
	}

	protected boolean isAvailableInSubscription(@NonNull ColoringStyle coloringStyle) {
		return ColoringStyleAlgorithms.isAvailableInSubscription(app, coloringStyle);
	}

	protected abstract boolean isUsedOnMap();

	@NonNull
	protected abstract BaseCard getContentCardForSelectedState(@NonNull FragmentActivity activity);

	protected abstract boolean isAvailableColoringStyle(@NonNull ColoringStyle coloringStyle);

	@NonNull
	protected abstract List<ColoringStyle> collectSupportedColoringStyles();
}
