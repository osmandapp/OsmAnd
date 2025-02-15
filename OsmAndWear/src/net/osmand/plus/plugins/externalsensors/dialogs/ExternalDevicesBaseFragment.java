package net.osmand.plus.plugins.externalsensors.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public abstract class ExternalDevicesBaseFragment extends BaseOsmAndFragment {

	public static final String TAG = ExternalDevicesBaseFragment.class.getSimpleName();
	protected ExternalSensorsPlugin plugin;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
	}

	@LayoutRes
	protected abstract int getLayoutId();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(getLayoutId(), container, false);
		setupToolbar(view);
		setupUI(view);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		return view;
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_main_dark : R.color.status_bar_main_light;
	}

	public boolean getContentStatusBarNightMode() {
		return true;
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	protected void setupUI(@NonNull View view) {
	}

	protected void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, getElevation());
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setTitleTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode));
		toolbar.setNavigationIcon(AndroidUtils.getNavigationIconResId(app));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			requireActivity().onBackPressed();
		});
	}

	protected float getElevation() {
		return 5.0f;
	}


	@NonNull
	protected MapActivity requireMapActivity() {
		return ((MapActivity) requireActivity());
	}
}