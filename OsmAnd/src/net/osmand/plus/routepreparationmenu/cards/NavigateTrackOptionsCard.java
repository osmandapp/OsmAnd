package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;

import static net.osmand.plus.UiUtilities.CustomRadioButtonType;
import static net.osmand.plus.UiUtilities.CustomRadioButtonType.START;
import static net.osmand.plus.UiUtilities.CustomRadioButtonType.END;

public class NavigateTrackOptionsCard extends MapBaseCard {

	private LocalRoutingParameter passWholeRoute;
	private LocalRoutingParameter navigationType;

	public NavigateTrackOptionsCard(@NonNull MapActivity mapActivity,
	                                @NonNull LocalRoutingParameter passWholeRoute,
	                                @NonNull LocalRoutingParameter navigationType) {
		super(mapActivity);
		this.passWholeRoute = passWholeRoute;
		this.navigationType = navigationType;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.navigate_track_options_card;
	}

	@Override
	protected void updateContent() {
		setupPassWholeRoute(view.findViewById(R.id.pass_whole_route_container));
		setupNavigationType(view.findViewById(R.id.navigation_type_container));
	}

	private void setupPassWholeRoute(final View parameterView) {
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

	private void setupNavigationType(final View parameterView) {
		View buttonsView = parameterView.findViewById(R.id.custom_radio_buttons);
		TextView description = parameterView.findViewById(R.id.description);
		TextView leftButton = parameterView.findViewById(R.id.left_button);
		TextView rightButton = parameterView.findViewById(R.id.right_button);

		description.setText(R.string.nav_type_hint);
		leftButton.setText(R.string.routing_profile_straightline);
		rightButton.setText(app.getRoutingHelper().getAppMode().toHumanString());

		boolean enabled = navigationType.isSelected(app.getSettings());
		CustomRadioButtonType buttonType = enabled ? END : START;
		UiUtilities.updateCustomRadioButtons(app, buttonsView, nightMode, buttonType);

		leftButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (navigationType.isSelected(app.getSettings())) {
					applyParameter(parameterView, navigationType, START, false);
				}
			}
		});
		rightButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!navigationType.isSelected(app.getSettings())) {
					applyParameter(parameterView, navigationType, END, true);
				}
			}
		});
	}

	private void applyParameter(View parameterView, LocalRoutingParameter parameter, CustomRadioButtonType buttonType, boolean isChecked) {
		updateModeButtons(parameterView, buttonType);
		app.getRoutingOptionsHelper().applyRoutingParameter(parameter, isChecked);

		CardListener listener = getListener();
		if (listener != null) {
			listener.onCardPressed(NavigateTrackOptionsCard.this);
		}
	}

	private void updateModeButtons(View customRadioButton, CustomRadioButtonType buttonType) {
		UiUtilities.updateCustomRadioButtons(app, customRadioButton, nightMode, buttonType);
	}
}