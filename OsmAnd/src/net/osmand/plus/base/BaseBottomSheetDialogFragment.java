package net.osmand.plus.base;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.IOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment implements IOsmAndFragment {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected ApplicationMode appMode;
	protected UiUtilities iconsCache;
	protected boolean nightMode;

	private LayoutInflater themedInflater;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		settings = app.getSettings();
		iconsCache = app.getUIUtilities();
		appMode = restoreAppMode(app, appMode, savedInstanceState, getArguments());
		updateNightMode();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		if (!AndroidUiHelper.isOrientationPortrait(requireActivity())) {
			dialog.setOnShowListener(dialogInterface -> {
				BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
				FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				if (bottomSheet != null) {
					BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
				}
			});
		}
		return dialog;
	}

	protected void updateNightMode() {
		nightMode = resolveNightMode();
		themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveAppModeToBundle(appMode, outState);
	}

	@Override
	public void setAppMode(@NonNull ApplicationMode appMode) {
		this.appMode = appMode;
	}

	@NonNull
	@Override
	public ApplicationMode getAppMode() {
		return appMode;
	}

	@NonNull
	@Override
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	@Override
	public LayoutInflater getThemedInflater() {
		return themedInflater;
	}

	@NonNull
	@Override
	public UiUtilities getIconsCache() {
		return iconsCache;
	}

	public boolean isNightMode() {
		return nightMode;
	}
}
