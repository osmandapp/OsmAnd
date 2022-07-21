package net.osmand.plus.routepreparationmenu.cards;

import static net.osmand.plus.utils.UiUtilities.CustomRadioButtonType;
import static net.osmand.plus.utils.UiUtilities.CustomRadioButtonType.END;
import static net.osmand.plus.utils.UiUtilities.CustomRadioButtonType.START;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.settings.backend.ApplicationMode;

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

		View buttonsView = parameterView.findViewById(R.id.custom_radio_buttons);
		TextView leftButton = parameterView.findViewById(R.id.left_button);
		TextView rightButton = parameterView.findViewById(R.id.right_button);
		TextView description = parameterView.findViewById(R.id.description);

		boolean enabled = passWholeRoute.isSelected(app.getSettings());
		CustomRadioButtonType buttonType = enabled ? START : END;
		UiUtilities.updateCustomRadioButtons(app, buttonsView, nightMode, buttonType);

		leftButton.setText(R.string.start_of_the_track);
		rightButton.setText(R.string.nearest_point);
		description.setText(R.string.pass_whole_track_descr);

		leftButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!passWholeRoute.isSelected(app.getSettings())) {
					applyParameter(parameterView, passWholeRoute, START, true);
				}
			}
		});
		rightButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (passWholeRoute.isSelected(app.getSettings())) {
					applyParameter(parameterView, passWholeRoute, END, false);
				}
			}
		});
	}

	private void setupConnectTrackPoints(View parameterView, LocalRoutingParameter parameter) {
		setupParameterView(parameterView, parameter);

		TextView description = parameterView.findViewById(R.id.description);
		TextView leftButton = parameterView.findViewById(R.id.left_button);
		TextView rightButton = parameterView.findViewById(R.id.right_button);

		ApplicationMode appMode = app.getRoutingHelper().getAppMode();
		leftButton.setText(appMode.toHumanString());
		rightButton.setText(R.string.routing_profile_straightline);
		description.setText(R.string.connect_track_points_as);
	}

	private void setupNavigationType(View parameterView, LocalRoutingParameter parameter) {
		setupParameterView(parameterView, parameter);

		TextView description = parameterView.findViewById(R.id.description);
		TextView leftButton = parameterView.findViewById(R.id.left_button);
		TextView rightButton = parameterView.findViewById(R.id.right_button);

		ApplicationMode appMode = app.getRoutingHelper().getAppMode();
		description.setText(R.string.nav_type_hint);
		leftButton.setText(R.string.routing_profile_straightline);
		rightButton.setText(appMode.toHumanString());
	}

	private void setupParameterView(View parameterView, LocalRoutingParameter parameter) {
		AndroidUiHelper.updateVisibility(parameterView, true);

		View buttonsView = parameterView.findViewById(R.id.custom_radio_buttons);
		TextView leftButton = parameterView.findViewById(R.id.left_button);
		TextView rightButton = parameterView.findViewById(R.id.right_button);

		boolean enabled = parameter.isSelected(app.getSettings());
		CustomRadioButtonType buttonType = enabled ? END : START;
		UiUtilities.updateCustomRadioButtons(app, buttonsView, nightMode, buttonType);

		leftButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (parameter.isSelected(app.getSettings())) {
					applyParameter(parameterView, parameter, START, false);
				}
			}
		});
		rightButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!parameter.isSelected(app.getSettings())) {
					applyParameter(parameterView, parameter, END, true);
				}
			}
		});
	}

	private void applyParameter(View parameterView, LocalRoutingParameter parameter, CustomRadioButtonType buttonType, boolean isChecked) {
		updateModeButtons(parameterView, buttonType);
		app.getRoutingOptionsHelper().applyRoutingParameter(parameter, isChecked);
		notifyCardPressed();
	}

	private void updateModeButtons(View customRadioButton, CustomRadioButtonType buttonType) {
		UiUtilities.updateCustomRadioButtons(app, customRadioButton, nightMode, buttonType);
	}
}