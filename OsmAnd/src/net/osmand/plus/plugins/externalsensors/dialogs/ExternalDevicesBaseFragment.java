package net.osmand.plus.plugins.externalsensors.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public abstract class ExternalDevicesBaseFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = ExternalDevicesBaseFragment.class.getSimpleName();
	protected ExternalSensorsPlugin plugin;
	protected boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
	}

	@LayoutRes
	protected abstract int getLayoutId();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		MapActivity activity = requireMapActivity();
		View view = UiUtilities.getInflater(activity, nightMode).inflate(getLayoutId(), container, false);
		setupToolbar(view);
		setupUI(view);
		return view;
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
			dismiss();
		});
	}

	protected float getElevation() {
		return 5.0f;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity activity = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(activity, themeId) {
			@Override
			public void onBackPressed() {
				dismiss();
			}
		};
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			window.setStatusBarColor(ContextCompat.getColor(app, getStatusBarColorId()));
		}
		return dialog;
	}

	@ColorRes
	private int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return ((MapActivity) requireActivity());
	}
}