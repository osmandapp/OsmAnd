package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.QUICK_SEARCH_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.SMALL_SIZE_DP;
import static net.osmand.shared.grid.ButtonPositionSize.POS_LEFT;
import static net.osmand.shared.grid.ButtonPositionSize.POS_TOP;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.shared.grid.ButtonPositionSize;

public class QuickSearchButtonState extends MapButtonState {

	private final CommonPreference<Boolean> visibilityPref;

	public QuickSearchButtonState(@NonNull OsmandApplication app) {
		super(app, QUICK_SEARCH_HUD_ID);
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", true)).makeProfile();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.map_widget_search);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.search_action_descr);
	}

	@Override
	public boolean isEnabled() {
		return visibilityPref.get();
	}

	@NonNull
	@Override
	public CommonPreference<Boolean> getVisibilityPref() {
		return visibilityPref;
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.map_search_button;
	}

	@Override
	public int getDefaultSize() {
		return SMALL_SIZE_DP;
	}

	@NonNull
	@Override
	public String getDefaultIconName() {
		return "ic_action_search_dark";
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_LEFT, POS_TOP, true, false);
	}
}