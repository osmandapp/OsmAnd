package net.osmand.plus.card.color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.color.cstyle.OnSelectColoringStyleListener;
import net.osmand.plus.card.color.palette.main.OnColorsPaletteListener;
import net.osmand.plus.routing.ColoringStyleAlgorithms;
import net.osmand.shared.routing.ColoringType;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteStatisticsHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ColoringStyleCardController extends BaseMultiStateCardController {

	private IColorCardControllerListener externalListener;

	public ColoringStyleCardController(@NonNull OsmandApplication app) {
		super(app);
	}

	public void setListener(@NonNull IColorCardControllerListener externalListener) {
		this.externalListener = externalListener;
	}

	@NonNull
	public IColorCardControllerListener getExternalListener() {
		return externalListener;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_color);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		return selectedState.toHumanString(app);
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		askSelectColoringStyle((ColoringStyle) cardState.getTag());
	}

	@NonNull
	public ColoringStyle requireSelectedColoringStyle() {
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		assert coloringStyle != null;
		return coloringStyle;
	}

	@Nullable
	public ColoringStyle getSelectedColoringStyle() {
		return (ColoringStyle) selectedState.getTag();
	}

	public void askSelectColoringStyle(@Nullable ColoringStyle coloringStyle) {
		ColoringStyle selectedColoringStyle = (ColoringStyle) selectedState.getTag();
		if (!Objects.equals(selectedColoringStyle, coloringStyle)) {
			selectedState = findCardState(coloringStyle);
			onColoringStyleSelected(coloringStyle);
		}
	}

	protected void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
		externalListener.onColoringStyleSelected(coloringStyle);
		card.updateSelectedCardState();
	}

	protected boolean isAvailableInSubscription(@NonNull ColoringStyle coloringStyle) {
		return ColoringStyleAlgorithms.isAvailableInSubscription(app, coloringStyle);
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> result = new ArrayList<>();
		for (ColoringStyle coloringStyle : getSupportedColoringStyles()) {
			result.add(new CardState(coloringStyle.toHumanString(app)).setTag(coloringStyle));
		}
		return result;
	}

	@NonNull
	protected List<ColoringStyle> getSupportedColoringStyles() {
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

	@NonNull
	protected List<String> collectRouteInfoAttributes() {
		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
		return RouteStatisticsHelper.getRouteStatisticAttrsNames(currentRenderer, defaultRenderer, true);
	}

	@NonNull
	protected abstract ColoringType[] getSupportedColoringTypes();

	public interface IColorCardControllerListener
			extends OnSelectColoringStyleListener, OnColorsPaletteListener {}
}
