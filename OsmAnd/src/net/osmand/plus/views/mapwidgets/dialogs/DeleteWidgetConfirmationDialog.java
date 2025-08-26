package net.osmand.plus.views.mapwidgets.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;

public class DeleteWidgetConfirmationDialog extends BaseAlertDialogFragment {

	private static final String TAG = DeleteWidgetConfirmationDialog.class.getSimpleName();

	private static final String USED_ON_MAP_KEY = "used_on_map";

	private DeleteWidgetConfirmationController controller;
	private boolean usedOnMap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = DeleteWidgetConfirmationController.getExistedInstance(app);
		if (controller == null) {
			dismiss();
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
		}
		updateNightMode();
		AlertDialog.Builder builder = createDialogBuilder();
		builder.setTitle(getString(R.string.delete_widget));
		builder.setMessage(R.string.delete_widget_description);
		builder.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
					callMapActivity(activity -> {
						if (controller != null) {
							controller.onDeleteActionConfirmed(activity);
						}
					});
				});
		return builder.create();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (controller != null) {
			controller.finishProcessIfNeeded(getActivity());
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(USED_ON_MAP_KEY, usedOnMap);
	}

	@Override
	public boolean isUsedOnMap() {
		return usedOnMap;
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull ApplicationMode appMode, boolean usedOnMap) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			DeleteWidgetConfirmationDialog dialog = new DeleteWidgetConfirmationDialog();
			dialog.usedOnMap = usedOnMap;
			dialog.setAppMode(appMode);
			dialog.show(fragmentManager, TAG);
		}
	}
}
