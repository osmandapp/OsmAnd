package net.osmand.plus.card.color;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.color.cstyle.OnSelectColoringStyleListener;
import net.osmand.plus.card.color.palette.main.OnColorsPaletteListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routing.ColoringStyleAlgorithms;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ColoringStyleCardController extends BaseMultiStateCardController {

	protected final OsmandApplication app;
	private final List<ColoringStyle> supportedColoringStyles;

	private ColoringStyle selectedColoringStyle;
	private IColorCardControllerListener controllerListener;
	private boolean nightMode;

	public ColoringStyleCardController(@NonNull OsmandApplication app,
	                                   @NonNull ColoringStyle selectedColoringStyle) {
		this.app = app;
		this.supportedColoringStyles = collectSupportedColoringStyles();
		this.selectedColoringStyle = selectedColoringStyle;
	}

	public void setListener(@NonNull IColorCardControllerListener controllerListener) {
		this.controllerListener = controllerListener;
	}

	@NonNull
	public IColorCardControllerListener getControllerListener() {
		return controllerListener;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_color);
	}

	@NonNull
	@Override
	public String getSelectorTitle() {
		return selectedColoringStyle.toHumanString(app);
	}

	@NonNull
	@Override
	public List<PopUpMenuItem> getPopUpMenuItems() {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (ColoringStyle coloringStyle : getSupportedColoringStyles()) {
			int titleColor = isDataAvailableForColoringStyle(coloringStyle)
					? ColorUtilities.getPrimaryTextColor(app, nightMode)
					: ColorUtilities.getDisabledTextColor(app, nightMode);
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitle(coloringStyle.toHumanString(app))
					.setTitleColor(titleColor)
					.setTag(coloringStyle)
					.create()
			);
		}
		return menuItems;
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode) {
		this.nightMode = nightMode;
		container.removeAllViews();
		BaseCard card = getContentCardForSelectedState(activity);
		View cardView = card.getView() != null ? card.getView() : card.build(activity);
		container.addView(cardView);
	}

	@Override
	public boolean shouldShowCardHeader() {
		return true;
	}

	@Override
	public void onPopUpMenuItemSelected(@NonNull FragmentActivity activity,
	                                    @NonNull View view, @NonNull PopUpMenuItem item) {
		ColoringStyle newColoringStyle = (ColoringStyle) item.getTag();
		if (!isDataAvailableForColoringStyle(newColoringStyle)) {
			showUnavailableColoringStyleSnackbar(activity, newColoringStyle, view);
		} else {
			askSelectColoringStyle(newColoringStyle);
		}
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
	public ColoringStyle getSelectedColoringStyle() {
		return selectedColoringStyle;
	}

	public void askSelectColoringStyle(@NonNull ColoringStyle coloringStyle) {
		if (!Objects.equals(selectedColoringStyle, coloringStyle)) {
			this.selectedColoringStyle = coloringStyle;
			onColoringStyleSelected(coloringStyle);
		}
	}

	protected void onColoringStyleSelected(@NonNull ColoringStyle coloringStyle) {
		cardInstance.updateSelectedCardState();
		controllerListener.onColoringStyleSelected(coloringStyle);
	}

	@NonNull
	public List<ColoringStyle> getSupportedColoringStyles() {
		return supportedColoringStyles;
	}

	protected boolean isAvailableInSubscription(@NonNull ColoringStyle coloringStyle) {
		return ColoringStyleAlgorithms.isAvailableInSubscription(app, coloringStyle);
	}

	@NonNull
	protected List<ColoringStyle> collectSupportedColoringStyles() {
		List<ColoringStyle> coloringStyles = new ArrayList<>();
		for (ColoringType coloringType : getSupportedColoringTypes()) {
			if (!coloringType.isRouteInfoAttribute()) {
				coloringStyles.add(new ColoringStyle(coloringType));
			}
		}
		for (String routeInfoAttribute : collectRouteInfoAttributes()) {
			coloringStyles.add(new ColoringStyle(routeInfoAttribute));
		}
		return coloringStyles;
	}

	protected abstract ColoringType[] getSupportedColoringTypes();

	protected abstract boolean isUsedOnMap();

	@NonNull
	protected abstract BaseCard getContentCardForSelectedState(@NonNull FragmentActivity activity);

	protected abstract boolean isDataAvailableForColoringStyle(@NonNull ColoringStyle coloringStyle);

	public interface IColorCardControllerListener
			extends OnSelectColoringStyleListener, OnColorsPaletteListener { }
}
