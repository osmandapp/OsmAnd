package net.osmand.plus.plugins.mapillary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.plugins.PluginsHelper;

public class MapillaryFirstDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = MapillaryFirstDialogFragment.class.getSimpleName();

	private static final String KEY_SHOW_WIDGET = "key_show_widget";

	private boolean showWidget = true;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			showWidget = savedInstanceState.getBoolean(KEY_SHOW_WIDGET, true);
		}

		View view = inflater.inflate(R.layout.mapillary_first_dialog, container, false);
		SwitchCompat widgetSwitch = view.findViewById(R.id.widget_switch);
		widgetSwitch.setChecked(showWidget);
		widgetSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> showWidget(isChecked));
		view.findViewById(R.id.actionButton).setOnClickListener(v -> {
			showWidget(widgetSwitch.isChecked());
			dismiss();
		});
		showWidget(showWidget);
		return view;
	}

	private void showWidget(boolean show) {
		FragmentActivity activity = getActivity();
		MapillaryPlugin plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
		if (plugin != null && activity instanceof MapActivity) {
			plugin.setWidgetVisible((MapActivity) activity, show);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(KEY_SHOW_WIDGET, showWidget);
	}
}