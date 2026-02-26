package net.osmand.plus.firstusage;

import static net.osmand.plus.firstusage.FirstUsageAction.RESTORE_FROM_CLOUD;
import static net.osmand.plus.firstusage.FirstUsageAction.RESTORE_FROM_FILE;
import static net.osmand.plus.firstusage.FirstUsageAction.SELECT_STORAGE_FOLDER;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.firstusage.FirstUsageWizardFragment.WizardType;

public class FirstUsageActionsBottomSheet extends BaseFirstUsageBottomSheet {

	@Override
	protected String getTitle() {
		return getString(R.string.shared_string_other);
	}

	@Override
	protected void setupItems(@NonNull ViewGroup container, @NonNull LayoutInflater inflater) {
		container.addView(createItemView(inflater, getString(R.string.restore_from_osmand_cloud), R.drawable.ic_action_restore, view -> {
			processActionClick(RESTORE_FROM_CLOUD);
			dismiss();
		}));
		container.addView(createItemView(inflater, getString(R.string.restore_from_file), R.drawable.ic_action_read_from_file, view -> {
			processActionClick(RESTORE_FROM_FILE);
			dismiss();
		}));
		if (wizardType != WizardType.MAP_DOWNLOAD) {
			container.addView(createItemView(inflater, getString(R.string.application_dir), R.drawable.ic_action_folder, view -> {
				processActionClick(SELECT_STORAGE_FOLDER);
				dismiss();
			}));
		}
	}
}