package net.osmand.plus.plugins.mapillary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class MapillaryFirstDialogFragment extends BottomSheetDialogFragment {

	private static final String TAG = MapillaryFirstDialogFragment.class.getSimpleName();

	private static final String KEY_SHOW_WIDGET = "key_show_widget";

	private boolean showWidget;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		MapActivity activity = getMapActivity();
		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SHOW_WIDGET)) {
			showWidget = savedInstanceState.getBoolean(KEY_SHOW_WIDGET);
		} else {
			showWidget = activity != null && isWidgetVisible(activity);
		}

		View view = inflate(R.layout.mapillary_first_dialog, container, false);
		int activeColor = settings.getApplicationMode().getProfileColor(nightMode);

		SwitchCompat widgetSwitch = view.findViewById(R.id.widget_switch);
		widgetSwitch.setChecked(showWidget);
		widgetSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> showWidget = isChecked);
		UiUtilities.setupCompoundButton(nightMode, activeColor, widgetSwitch);

		View widgetRow = view.findViewById(R.id.widget_row);
		UiUtilities.setupListItemBackground(view.getContext(), widgetRow, activeColor);
		widgetRow.setOnClickListener(v -> widgetSwitch.setChecked(!widgetSwitch.isChecked()));

		View actionButton = view.findViewById(R.id.actionButton);
		UiUtilities.setupListItemBackground(view.getContext(), actionButton, activeColor);
		actionButton.setOnClickListener(v -> {
			applyWidgetVisibility();
			dismiss();
		});
		return view;
	}

	private boolean isWidgetVisible(@NonNull MapActivity activity) {
		MapillaryPlugin plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
		return plugin != null && plugin.isWidgetVisible(activity);
	}

	private void applyWidgetVisibility() {
		MapActivity activity = getMapActivity();
		MapillaryPlugin plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
		if (activity != null && plugin != null) {
			plugin.setWidgetVisible(activity, showWidget);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_SHOW_WIDGET, showWidget);
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		MapillaryPlugin plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
		if (plugin != null && AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			MapillaryFirstDialogFragment fragment = new MapillaryFirstDialogFragment();
			fragment.show(manager, TAG);
			plugin.MAPILLARY_FIRST_DIALOG_SHOWN.set(true);
		}
	}
}