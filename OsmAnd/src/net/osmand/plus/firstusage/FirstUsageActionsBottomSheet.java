package net.osmand.plus.firstusage;

import static net.osmand.plus.firstusage.FirstUsageAction.RESTORE_FROM_CLOUD;
import static net.osmand.plus.firstusage.FirstUsageAction.RESTORE_FROM_FILE;
import static net.osmand.plus.firstusage.FirstUsageAction.SELECT_STORAGE_FOLDER;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

public class FirstUsageActionsBottomSheet extends BaseFirstUsageBottomSheet {

	private static final String TAG = FirstUsageActionsBottomSheet.class.getSimpleName();

	@Override
	protected String getTitle() {
		return getString(R.string.shared_string_other);
	}

	@Override
	protected void setupItems(@NonNull ViewGroup container) {
		container.addView(createItemView(getString(R.string.restore_from_osmand_cloud), R.drawable.ic_action_restore, view -> {
			processActionClick(RESTORE_FROM_CLOUD);
			dismiss();
		}));
		container.addView(createItemView(getString(R.string.restore_from_file), R.drawable.ic_action_read_from_file, view -> {
			processActionClick(RESTORE_FROM_FILE);
			dismiss();
		}));
		if (wizardType != WizardType.MAP_DOWNLOAD) {
			container.addView(createItemView(getString(R.string.application_dir), R.drawable.ic_action_folder, view -> {
				processActionClick(SELECT_STORAGE_FOLDER);
				dismiss();
			}));
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull Fragment target) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			FirstUsageActionsBottomSheet fragment = new FirstUsageActionsBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}