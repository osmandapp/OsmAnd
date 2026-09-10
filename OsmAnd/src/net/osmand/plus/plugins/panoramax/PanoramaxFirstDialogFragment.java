package net.osmand.plus.plugins.panoramax;

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

public class PanoramaxFirstDialogFragment extends BottomSheetDialogFragment {

	private static final String TAG = PanoramaxFirstDialogFragment.class.getSimpleName();

	private static final String KEY_SHOW_WIDGET = "key_show_widget";

	private boolean showWidget = true;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		if (savedInstanceState != null) {
			showWidget = savedInstanceState.getBoolean(KEY_SHOW_WIDGET, true);
		}

		View view = inflate(R.layout.panoramax_first_dialog, container, false);
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
		MapActivity activity = getMapActivity();
		PanoramaxPlugin plugin = PluginsHelper.getPlugin(PanoramaxPlugin.class);
		if (activity != null && plugin != null) {
			plugin.setWidgetVisible(activity, show);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(KEY_SHOW_WIDGET, showWidget);
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		PanoramaxPlugin plugin = PluginsHelper.getPlugin(PanoramaxPlugin.class);
		if (plugin != null && AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			PanoramaxFirstDialogFragment fragment = new PanoramaxFirstDialogFragment();
			fragment.show(manager, TAG);
			plugin.PANORAMAX_FIRST_DIALOG_SHOWN.set(true);
		}
	}
}