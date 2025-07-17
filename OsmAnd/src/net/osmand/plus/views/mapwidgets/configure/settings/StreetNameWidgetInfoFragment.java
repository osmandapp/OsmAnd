package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.WidgetType.STREET_NAME;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.WidgetType;

public class StreetNameWidgetInfoFragment extends WidgetInfoBaseFragment {

	private static final String SHOW_NEXT_TURN = "show_next_turn";

	private boolean showNextTurnInfo;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return STREET_NAME;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);

		boolean show = settings.SHOW_NEXT_TURN_INFO.getModeValue(appMode);
		showNextTurnInfo = bundle.getBoolean(SHOW_NEXT_TURN, show);
	}

	@Override
	protected void setupMainContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		View view = themedInflater.inflate(R.layout.widget_preference_with_switch, container);
		setupShowNextTurnInfoPref(view);
	}

	private void setupShowNextTurnInfoPref(@NonNull View view) {
		ImageView icon = view.findViewById(R.id.icon);
		int iconColor = ColorUtilities.getActiveColor(app, nightMode);
		icon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_next_turn, iconColor));
		icon.setVisibility(View.VISIBLE);

		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(R.string.next_turn_information);
		description.setText(R.string.next_turn_information_desc);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(showNextTurnInfo);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> showNextTurnInfo = isChecked);

		view.setOnClickListener(v -> compoundButton.setChecked(!compoundButton.isChecked()));
		view.setBackground(getPressedStateDrawable());
	}

	@Override
	protected void applySettings() {
		settings.SHOW_NEXT_TURN_INFO.setModeValue(appMode, showNextTurnInfo);
		app.getRoutingHelper().onSettingsChanged(appMode);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_NEXT_TURN, showNextTurnInfo);
	}
}
