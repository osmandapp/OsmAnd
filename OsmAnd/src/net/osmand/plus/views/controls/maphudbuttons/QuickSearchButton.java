package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.QUICK_SEARCH_HUD_ID;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.search.ShowQuickSearchMode;

public class QuickSearchButton extends MapButton {

	public QuickSearchButton(@NonNull MapActivity mapActivity) {
		super(mapActivity, mapActivity.findViewById(R.id.map_search_button), QUICK_SEARCH_HUD_ID, false);
		setIconId(R.drawable.ic_action_search_dark);
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setBackground(R.drawable.btn_inset_circle_trans, R.drawable.btn_inset_circle_night);
		setOnClickListener(v -> {
			mapActivity.getFragmentsHelper().dismissCardDialog();
			mapActivity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
		});
	}

	@Override
	protected boolean shouldShow() {
		return !isRouteDialogOpened() && visibilityHelper.shouldShowTopButtons();
	}


	@Override
	public void refresh() {
		updateVisibility(shouldShow());
	}
}