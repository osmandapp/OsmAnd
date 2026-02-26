package net.osmand.plus.base;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected UiUtilities uiUtilities;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
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

	protected boolean isNightMode(boolean usedOnMap) {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}
}
