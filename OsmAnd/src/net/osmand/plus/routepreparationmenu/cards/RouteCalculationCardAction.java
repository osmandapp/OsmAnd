package net.osmand.plus.routepreparationmenu.cards;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public enum RouteCalculationCardAction {

	DETAILS(R.string.shared_string_details, DialogButtonType.SECONDARY_ACTIVE),
	DOWNLOAD_MAPS(R.string.welmode_download_maps, DialogButtonType.ACCENT_STROKED),
	UPDATE_MAPS(R.string.update_maps, DialogButtonType.ACCENT_STROKED),
	REVIEW_MAPS(R.string.route_calculation_review_maps, DialogButtonType.ACCENT_STROKED),
	USE_EXISTING_MAPS(R.string.route_calculation_use_existing_maps, DialogButtonType.STROKED),
	ADD_INTERMEDIATE_DESTINATION(R.string.add_intermediate, DialogButtonType.ACCENT_STROKED);

	@StringRes
	private final int titleId;
	@NonNull
	private final DialogButtonType buttonType;

	RouteCalculationCardAction(@StringRes int titleId, @NonNull DialogButtonType buttonType) {
		this.titleId = titleId;
		this.buttonType = buttonType;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public DialogButtonType getButtonType() {
		return buttonType;
	}
}
