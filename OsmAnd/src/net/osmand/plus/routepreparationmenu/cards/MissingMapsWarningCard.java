package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.AddPointBottomSheetDialog;
import net.osmand.plus.routepreparationmenu.RequiredMapsController;
import net.osmand.plus.routepreparationmenu.data.PointType;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.ArrayList;
import java.util.List;

public class MissingMapsWarningCard extends MapBaseCard {

	@NonNull
	private final RouteCalculationCardState state;

	public MissingMapsWarningCard(@NonNull MapActivity mapActivity) {
		this(mapActivity, RouteCalculationCardState.LEGACY_MISSING_MAPS);
	}

	public MissingMapsWarningCard(@NonNull MapActivity mapActivity,
	                              @NonNull RouteCalculationCardState state) {
		super(mapActivity);
		this.state = state;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_missing_maps_warning;
	}

	@Override
	protected void updateContent() {
		ImageView icon = view.findViewById(R.id.warning_icon);
		TextView title = view.findViewById(R.id.warning_title);
		TextView description = view.findViewById(R.id.warning_description);

		icon.setImageDrawable(getContentIcon(state.getIconId()));
		title.setText(state.getTitleId());
		description.setText(state.getDescriptionId());

		List<RouteCalculationCardAction> actions = getAvailableActions();
		setupActionButton(view.findViewById(R.id.primary_button), actions.size() > 0 ? actions.get(0) : null);
		setupActionButton(view.findViewById(R.id.secondary_button), actions.size() > 1 ? actions.get(1) : null);
	}

	@NonNull
	private List<RouteCalculationCardAction> getAvailableActions() {
		List<RouteCalculationCardAction> result = new ArrayList<>();
		for (RouteCalculationCardAction action : state.getActions()) {
			if (shouldShowAction(action)) {
				result.add(action);
			}
		}
		return result;
	}

	private boolean shouldShowAction(@NonNull RouteCalculationCardAction action) {
		return action != RouteCalculationCardAction.REVIEW_MAPS || RequiredMapsController.hasMapsToDisplay(app);
	}

	private void setupActionButton(@NonNull DialogButton button, RouteCalculationCardAction action) {
		if (action == null) {
			button.setVisibility(View.GONE);
			button.setOnClickListener(null);
			return;
		}
		button.setVisibility(View.VISIBLE);
		button.setTitleId(action.getTitleId());
		button.setButtonType(action.getButtonType());
		button.setOnClickListener(v -> handleAction(action));
	}

	private void handleAction(@NonNull RouteCalculationCardAction action) {
		switch (action) {
			case USE_EXISTING_MAPS:
				app.getSettings().setStopOnMissingMaps(false);
				app.getSettings().IGNORE_MISSING_MAPS = true;
				app.getRoutingHelper().onSettingsChanged(true);
				break;
			case ADD_INTERMEDIATE_DESTINATION:
				AddPointBottomSheetDialog.showInstance(mapActivity, PointType.INTERMEDIATE);
				break;
			case DETAILS:
			case DOWNLOAD_MAPS:
			case UPDATE_MAPS:
			case REVIEW_MAPS:
				showMissingMapsDialog();
				break;
		}
	}

	private void showMissingMapsDialog() {
		app.getSettings().setStopOnMissingMaps(true);
		app.getRoutingHelper().stopCalculationImmediately();
		RequiredMapsController.showDialog(getMapActivity());
	}
}
