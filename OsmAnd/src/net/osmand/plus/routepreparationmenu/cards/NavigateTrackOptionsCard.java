package net.osmand.plus.routepreparationmenu.cards;

import static net.osmand.plus.widgets.multistatetoggle.RadioItem.*;
import static net.osmand.plus.widgets.multistatetoggle.TextToggleButton.*;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;

public class NavigateTrackOptionsCard extends MapBaseCard {

	private final LocalRoutingParameter passWholeRoute;
	private final LocalRoutingParameter navigationType;
	private final LocalRoutingParameter connectPointsStraightly;

	private final boolean userIntermediateRtePoints;

	public NavigateTrackOptionsCard(@NonNull MapActivity mapActivity,
									@NonNull LocalRoutingParameter passWholeRoute,
									@NonNull LocalRoutingParameter navigationType,
									@NonNull LocalRoutingParameter connectPointsStraightly,
									boolean useIntermediateRtePoints) {
		super(mapActivity);
		this.passWholeRoute = passWholeRoute;
		this.navigationType = navigationType;
		this.connectPointsStraightly = connectPointsStraightly;
		this.userIntermediateRtePoints = useIntermediateRtePoints;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.navigate_track_options_card;
	}

	@Override
	protected void updateContent() {
		setupPassWholeRoute(view.findViewById(R.id.pass_whole_route_container));
		View navTypeContainer = view.findViewById(R.id.navigation_type_container);
		if (userIntermediateRtePoints) {
			setupConnectTrackPoints(navTypeContainer, connectPointsStraightly);
		} else {
			setupNavigationType(navTypeContainer, navigationType);
		}
	}

	private void setupPassWholeRoute(View parameterView) {
		AndroidUiHelper.updateVisibility(parameterView, true);

		LinearLayout buttonsView = parameterView.findViewById(R.id.custom_radio_buttons);
		TextView description = parameterView.findViewById(R.id.description);
		description.setText(R.string.pass_whole_track_descr);

		boolean enabled = passWholeRoute.isSelected(app.getSettings());
		TextToggleButton radioGroup = new TextToggleButton(app, buttonsView, nightMode);

		TextRadioItem leftButton = createRadioButton(getString(R.string.start_of_the_track), (radioItem, view) -> {
			if (!passWholeRoute.isSelected(app.getSettings())) {
				applyParameter(passWholeRoute, true);
				return true;
			}
			return false;
		});

		TextRadioItem rightButton = createRadioButton(getString(R.string.nearest_point), (radioItem, view) -> {
			if (passWholeRoute.isSelected(app.getSettings())) {
				applyParameter(passWholeRoute, false);
				return true;
			}
			return false;
		});

		radioGroup.setItems(leftButton, rightButton);
		radioGroup.setSelectedItem(enabled ? leftButton : rightButton);
	}

	@NonNull
	private TextRadioItem createRadioButton(@NonNull String title, @NonNull OnRadioItemClickListener listener) {
		TextRadioItem item = new TextRadioItem(title);
		item.setOnClickListener(listener);
		return item;
	}

	private void setupConnectTrackPoints(View parameterView, LocalRoutingParameter parameter) {
		ApplicationMode appMode = app.getRoutingHelper().getAppMode();
		setupParameterView(parameterView, parameter, appMode.toHumanString(), getString(R.string.routing_profile_straightline));

		TextView description = parameterView.findViewById(R.id.description);
		description.setText(R.string.connect_track_points_as);
	}

	private void setupNavigationType(View parameterView, LocalRoutingParameter parameter) {
		ApplicationMode appMode = app.getRoutingHelper().getAppMode();
		setupParameterView(parameterView, parameter, getString(R.string.routing_profile_straightline), appMode.toHumanString());

		TextView description = parameterView.findViewById(R.id.description);
		description.setText(R.string.nav_type_hint);
	}

	private void setupParameterView(View parameterView, LocalRoutingParameter parameter, @NonNull String leftButtonTitle, @NonNull String rightButtonTitle) {
		AndroidUiHelper.updateVisibility(parameterView, true);
		LinearLayout buttonsView = parameterView.findViewById(R.id.custom_radio_buttons);

		boolean enabled = parameter.isSelected(app.getSettings());
		TextToggleButton radioGroup = new TextToggleButton(app, buttonsView, nightMode);

		TextRadioItem leftButton = createRadioButton(leftButtonTitle, (radioItem, view) -> {
			if (parameter.isSelected(app.getSettings())) {
				applyParameter(parameter, false);
				return true;
			}
			return false;
		});

		TextRadioItem rightButton = createRadioButton(rightButtonTitle, (radioItem, view) -> {
			if (!parameter.isSelected(app.getSettings())) {
				applyParameter(parameter, true);
				return true;
			}
			return false;
		});

		radioGroup.setItems(leftButton, rightButton);
		radioGroup.setSelectedItem(enabled ? rightButton : leftButton);
	}

	private void applyParameter(LocalRoutingParameter parameter, boolean isChecked) {
		app.getRoutingOptionsHelper().applyRoutingParameter(parameter, isChecked);
		notifyCardPressed();
	}
}