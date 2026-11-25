package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;

public class StreetNameWidgetInfoFragment extends WidgetInfoBaseFragment {

	private static final String SHOW_NEXT_TURN = "show_next_turn";
	private static final String PRIORITIZE_DESTINATION = "prioritize_destination";

	private boolean showNextTurn;
	private boolean prioritizeDestination;
	private StreetNameWidget widget;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.STREET_NAME;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null && widgetInfo.widget instanceof StreetNameWidget) {
			widget = (StreetNameWidget) widgetInfo.widget;
			boolean defaultValue = widget.isShowNextTurnEnabled(appMode);
			showNextTurn = bundle.getBoolean(SHOW_NEXT_TURN, defaultValue);
			boolean defaultPrioritizeDest = widget.getWidgetState().isPrioritizeDestinationEnabled(appMode);
			prioritizeDestination = bundle.getBoolean(PRIORITIZE_DESTINATION, defaultPrioritizeDest);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View showNextTurnView = inflate(R.layout.widget_preference_with_switch, container);
		setupShowNextTurnInfoPref(showNextTurnView);

		View prioritizeDestView = inflate(R.layout.widget_preference_with_switch, container);
		setupPrioritizeDestinationPref(prioritizeDestView);
	}

	private void setupShowNextTurnInfoPref(@NonNull View view) {
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(R.string.next_turn_information);
		description.setText(R.string.next_turn_information_desc);
		updateShowNextTurnInfoPrefIcon(view);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(showNextTurn);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			showNextTurn = isChecked;
			updateShowNextTurnInfoPrefIcon(view);
		});

		view.setOnClickListener(v -> compoundButton.setChecked(!compoundButton.isChecked()));
		view.setBackground(getPressedStateDrawable());

	}

	private void updateShowNextTurnInfoPrefIcon(@NonNull View view) {
		ImageView icon = view.findViewById(R.id.icon);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = showNextTurn ? activeColor : defaultColor;
		icon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_next_turn, iconColor));
		icon.setVisibility(View.VISIBLE);
	}

	private void setupPrioritizeDestinationPref(@NonNull View view) {
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(R.string.prioritize_destination);
		description.setText(R.string.prioritize_destination_desc);
		updatePrioritizeDestinationPrefIcon(view);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(prioritizeDestination);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			prioritizeDestination = isChecked;
			updatePrioritizeDestinationPrefIcon(view);
		});

		view.setOnClickListener(v -> compoundButton.setChecked(!compoundButton.isChecked()));
		view.setBackground(getPressedStateDrawable());
	}

	private void updatePrioritizeDestinationPrefIcon(@NonNull View view) {
		ImageView icon = view.findViewById(R.id.icon);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = prioritizeDestination ? activeColor : defaultColor;
		icon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_target, iconColor));
		icon.setVisibility(View.VISIBLE);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_NEXT_TURN, showNextTurn);
		outState.putBoolean(PRIORITIZE_DESTINATION, prioritizeDestination);
	}

	@Override
	protected void applySettings() {
		widget.setShowNextTurnEnabled(appMode, showNextTurn);
		widget.getWidgetState().setPrioritizeDestinationEnabled(appMode, prioritizeDestination);
	}

	@Override
	public String getAnalyticsScreen() {
		return "street_name_widget_info";
	}
}
