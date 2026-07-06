package net.osmand.plus.routepreparationmenu.cards;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum RouteCalculationCardState {

	LEGACY_MISSING_MAPS(
			R.drawable.ic_action_map_download,
			R.string.missing_maps_header,
			R.string.missing_maps_description,
			RouteCalculationCardAction.DETAILS
	),

	AUTO_DEFAULT(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_calculating_route,
			R.string.route_calculation_using_fast_algorithm
	),
	AUTO_MISSING_MAPS(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_missing_maps,
			R.string.route_calculation_missing_maps_desc,
			RouteCalculationCardAction.DOWNLOAD_MAPS
	),
	AUTO_MIXED_MAPS(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_missing_maps,
			R.string.route_calculation_mixed_maps_desc,
			RouteCalculationCardAction.UPDATE_MAPS
	),
	AUTO_FAILED_WITH_MIXED_MAPS(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_missing_maps,
			R.string.route_calculation_mixed_maps_desc,
			RouteCalculationCardAction.UPDATE_MAPS
	),
	AUTO_FAILED_WITH_MISSING_MAPS(
			R.drawable.ic_action_route_error,
			R.string.route_calculation_missing_maps,
			R.string.route_calculation_missing_maps_estimate_desc,
			RouteCalculationCardAction.REVIEW_MAPS,
			RouteCalculationCardAction.USE_EXISTING_MAPS
	),
	AUTO_ROUTE_NOT_FOUND(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_calculating_route,
			R.string.route_calculation_route_not_found_desc
	),
	AUTO_MISSING_HH_CACHE(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_calculating_fast_route,
			R.string.route_calculation_unavailable_parameters_desc
	),
	AUTO_UNAVAILABLE_ROUTE_PARAMETERS(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_calculating_fast_route,
			R.string.route_calculation_unavailable_parameters_desc
	),

	STANDARD_DEFAULT(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_calculating_route,
			R.string.route_calculation_using_standard_algorithm
	),
	STANDARD_LONG_ROUTE(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_calculating_route,
			R.string.route_calculation_standard_long_route_desc,
			RouteCalculationCardAction.ADD_INTERMEDIATE_DESTINATION
	),
	STANDARD_MISSING_MAPS(
			R.drawable.ic_action_route_error,
			R.string.route_calculation_missing_maps,
			R.string.route_calculation_missing_maps_desc,
			RouteCalculationCardAction.DOWNLOAD_MAPS
	),

	FAST_DEFAULT(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_calculating_route,
			R.string.route_calculation_using_fast_algorithm
	),
	FAST_MISSING_MAPS(
			R.drawable.ic_action_route_error,
			R.string.route_calculation_missing_maps,
			R.string.route_calculation_missing_maps_desc,
			RouteCalculationCardAction.DOWNLOAD_MAPS
	),
	FAST_MISSING_OR_OUTDATED_MAPS(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_missing_maps,
			R.string.route_calculation_fast_missing_or_outdated_maps_desc,
			RouteCalculationCardAction.DOWNLOAD_MAPS
	),
	FAST_ROUTE_NOT_FOUND(
			R.drawable.ic_action_route_error,
			R.string.route_calculation_calculating_fast_route,
			R.string.route_calculation_fast_route_not_found_desc
	),
	FAST_MISSING_HH_CACHE(
			R.drawable.ic_action_route_error,
			R.string.route_calculation_calculating_fast_route,
			R.string.route_calculation_fast_missing_hh_cache_desc,
			RouteCalculationCardAction.REVIEW_MAPS
	),
	FAST_UNAVAILABLE_ROUTE_PARAMETERS(
			R.drawable.ic_action_route_direct,
			R.string.route_calculation_calculating_fast_route,
			R.string.route_calculation_fast_unavailable_parameters_desc
	);

	@DrawableRes
	private final int iconId;
	@StringRes
	private final int titleId;
	@StringRes
	private final int descriptionId;
	@NonNull
	private final RouteCalculationCardAction[] actions;

	RouteCalculationCardState(@DrawableRes int iconId,
	                          @StringRes int titleId,
	                          @StringRes int descriptionId,
	                          @NonNull RouteCalculationCardAction... actions) {
		this.iconId = iconId;
		this.titleId = titleId;
		this.descriptionId = descriptionId;
		this.actions = actions;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@StringRes
	public int getDescriptionId() {
		return descriptionId;
	}

	@NonNull
	public RouteCalculationCardAction[] getActions() {
		return actions;
	}
}
