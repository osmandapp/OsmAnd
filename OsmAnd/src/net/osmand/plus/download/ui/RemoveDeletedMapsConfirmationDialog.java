package net.osmand.plus.download.ui;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;

public class RemoveDeletedMapsConfirmationDialog extends BaseAlertDialogFragment {

	private static final String TAG = RemoveDeletedMapsConfirmationDialog.class.getSimpleName();

	private static final String KEY_TITLE = "key_title";
	private static final String KEY_DESCRIPTION = "key_description";
	private static final String KEY_ACTION_ID = "key_action_id";

	protected int actionId;
	private String title;
	private String description;

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		AlertDialog.Builder builder = createDialogBuilder();
		if(savedInstanceState != null) {
			title = savedInstanceState.getString(KEY_TITLE);
			description = savedInstanceState.getString(KEY_DESCRIPTION);
			actionId = savedInstanceState.getInt(KEY_ACTION_ID);
		}
		builder.setTitle(title);
		builder.setMessage(description);
		builder.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
					Fragment target = getTargetFragment();
					if (target instanceof ConfirmationDialogListener) {
						((ConfirmationDialogListener) target).onActionConfirmed(actionId);
					}
				});
		setAppMode(app.getSettings().getApplicationMode());
		return builder.create();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_TITLE, title);
		outState.putString(KEY_DESCRIPTION, description);
		outState.putInt(KEY_ACTION_ID, actionId);
	}

	public static void showInstance(FragmentManager fragmentManager,
	                                @NonNull Fragment target,
	                                @NonNull String title,
	                                @NonNull String description,
	                                int actionId) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			RemoveDeletedMapsConfirmationDialog dialog = new RemoveDeletedMapsConfirmationDialog();
			dialog.actionId = actionId;
			dialog.title = title;
			dialog.description = description;
			dialog.setTargetFragment(target, actionId);
			dialog.show(fragmentManager, TAG);
		}
	}

	public interface ConfirmationDialogListener {
		void onActionConfirmed(int actionId);
	}
}