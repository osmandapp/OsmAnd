package net.osmand.plus.settings.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper;
import net.osmand.plus.settings.backend.backup.ImportFileTask;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.util.List;

import static net.osmand.plus.settings.fragments.BaseSettingsListFragment.SETTINGS_LIST_TAG;

public class FileImportDuplicatesFragment extends ImportDuplicatesFragment {

	public static final String TAG = FileImportDuplicatesFragment.class.getSimpleName();

	private File file;
	private FileSettingsHelper settingsHelper;

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settingsHelper = app.getFileSettingsHelper();
		ImportFileTask importTask = settingsHelper.getImportTask();
		if (importTask != null) {
			if (settingsItems == null) {
				settingsItems = importTask.getSelectedItems();
			}
			if (duplicatesList == null) {
				duplicatesList = importTask.getDuplicates();
			}
			if (file == null) {
				file = importTask.getFile();
			}
			Fragment target = getTargetFragment();
			if (target instanceof ImportSettingsFragment) {
				ImportListener importListener = ((ImportSettingsFragment) target).getImportListener();
				importTask.setImportListener(importListener);
			}
		}
	}

	@Override
	protected ImportType getImportTaskType() {
		return settingsHelper.getImportTaskType();
	}

	@Override
	protected void importItems(boolean shouldReplace) {
		super.importItems(shouldReplace);
		if (settingsItems != null && file != null) {
			Fragment target = getTargetFragment();
			if (target instanceof ImportSettingsFragment) {
				ImportListener importListener = ((ImportSettingsFragment) target).getImportListener();
				settingsHelper.importSettings(file, settingsItems, "", 1, importListener);
			}
		}
	}

	@Override
	protected void setupImportingUi() {
		super.setupImportingUi();
		toolbarLayout.setTitle(getString(R.string.shared_string_importing));
		description.setText(UiUtilities.createSpannableString(
				String.format(getString(R.string.importing_from), file.getName()),
				Typeface.BOLD, file.getName()
		));
	}

	public static void showInstance(@NonNull FragmentManager fm, List<? super Object> duplicatesList,
									List<SettingsItem> settingsItems, File file, Fragment targetFragment) {
		FileImportDuplicatesFragment fragment = new FileImportDuplicatesFragment();
		fragment.setTargetFragment(targetFragment, 0);
		fragment.setDuplicatesList(duplicatesList);
		fragment.setSettingsItems(settingsItems);
		fragment.setFile(file);
		fm.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(SETTINGS_LIST_TAG)
				.commitAllowingStateLoss();
	}
}