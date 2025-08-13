package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.OnResultCallback;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.controllers.ReverseTrackModeDialogController;
import net.osmand.plus.settings.enums.ReverseTrackStrategy;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.data.parameters.LocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.data.parameters.OtherLocalRoutingParameter;

public class ReverseTrackCard extends MapBaseCard {

	private final LocalRoutingParameter parameter;
	private final OnResultCallback<ReverseTrackStrategy> strategyChangeListener;

	public ReverseTrackCard(@NonNull MapActivity mapActivity, boolean isReverse) {
		super(mapActivity);
		int textId = R.string.gpx_option_reverse_route;
		String title = app.getString(textId);
		parameter = new OtherLocalRoutingParameter(textId, title, isReverse);

		strategyChangeListener = strategy -> {
			settings.GPX_REVERSE_STRATEGY.set(strategy);
			onReverseSettingsChanged(isReverseEnabled());
		};
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_reverse_track;
	}

	@Override
	protected void updateContent() {
		setupMainButton();
		setupModeButton();
	}

	private void setupMainButton() {
		View button = view.findViewById(R.id.reverse_button);

		TextView titleTv = button.findViewById(R.id.title);
		titleTv.setText(R.string.gpx_option_reverse_route);

		ImageView icon = button.findViewById(R.id.icon);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_change_navigation_points));

		CompoundButton compoundButton = button.findViewById(R.id.compound_button);
		compoundButton.setChecked(isReverseEnabled());
		UiUtilities.setupCompoundButton(nightMode, getActiveColor(), compoundButton);

		button.setOnClickListener(v -> {
			boolean selected = !isReverseEnabled();
			compoundButton.setChecked(selected);
			onReverseSettingsChanged(selected);
		});
	}

	private void setupModeButton() {
		View button = view.findViewById(R.id.reverse_mode_button);
		TextView tvTitle = button.findViewById(R.id.title);
		tvTitle.setText(R.string.reverse_mode);

		ImageView ivIcon = button.findViewById(R.id.icon);
		ivIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_reverse_mode));

		button.setOnClickListener(v -> {
			ReverseTrackModeDialogController.showDialog(mapActivity, appMode, strategyChangeListener);
		});
		updateModeButton();
		updateStrategyListener();
	}

	private void updateModeButton() {
		View button = view.findViewById(R.id.reverse_mode_button);
		if (!isReverseEnabled()) {
			AndroidUiHelper.updateVisibility(button, false);
			return;
		}
		AndroidUiHelper.updateVisibility(button, true);

		ReverseTrackStrategy selectedStrategy = getReverseStrategy();
		TextView tvTitle = button.findViewById(R.id.selector_title);
		tvTitle.setText(selectedStrategy.getTitleId());
	}

	private void updateStrategyListener() {
		ReverseTrackModeDialogController controller = ReverseTrackModeDialogController.getExistedInstance(app);
		if (controller != null) {
			controller.setOnResultCallback(strategyChangeListener);
		}
	}

	private void onReverseSettingsChanged(boolean selected) {
		app.getRoutingOptionsHelper().applyRoutingParameter(parameter, selected);
		updateModeButton();
		notifyCardPressed();
	}

	private boolean isReverseEnabled() {
		return parameter.isSelected(settings);
	}

	@NonNull
	private ReverseTrackStrategy getReverseStrategy() {
		return settings.GPX_REVERSE_STRATEGY.get();
	}
}