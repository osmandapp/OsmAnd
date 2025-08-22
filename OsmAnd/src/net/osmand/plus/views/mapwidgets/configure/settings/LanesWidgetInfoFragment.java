package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.WidgetType.LANES;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.WidgetType;

public class LanesWidgetInfoFragment extends WidgetInfoBaseFragment {

	private static final String SHOW_MINOR_TURNS = "show_minor_turns";

	private boolean showMinorTurns;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return LANES;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);

		boolean show = settings.SHOW_MINOR_TURNS.getModeValue(appMode);
		showMinorTurns = bundle.getBoolean(SHOW_MINOR_TURNS, show);
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.widget_preference_with_switch, container);
		setupShowMinorTurnsPref(view);
	}

	private void setupShowMinorTurnsPref(@NonNull View view) {
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(R.string.show_minor_turns);
		description.setText(R.string.show_minor_turns_descr);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(showMinorTurns);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> showMinorTurns = isChecked);

		view.setOnClickListener(v -> compoundButton.setChecked(!compoundButton.isChecked()));
		view.setBackground(getPressedStateDrawable());
	}

	@Override
	protected void applySettings() {
		settings.SHOW_MINOR_TURNS.setModeValue(appMode, showMinorTurns);

		// Required for routing recalculation — omit if routing isn't affected
		app.getRoutingHelper().onSettingsChanged(appMode);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_MINOR_TURNS, showMinorTurns);
	}
}