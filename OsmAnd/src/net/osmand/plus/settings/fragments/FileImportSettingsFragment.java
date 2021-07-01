package net.osmand.plus.settings.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper;
import net.osmand.plus.settings.backend.backup.ImportFileTask;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.List;

public class FileImportSettingsFragment extends ImportSettingsFragment {

	public static final String TAG = FileImportSettingsFragment.class.getSimpleName();
	public static final Log LOG = PlatformUtil.getLog(FileImportSettingsFragment.class.getSimpleName());

	private File file;
	private FileSettingsHelper settingsHelper;

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		exportMode = false;
		settingsHelper = app.getFileSettingsHelper();

		ImportFileTask importTask = settingsHelper.getImportTask();
		if (importTask != null) {
			if (settingsItems == null) {
				settingsItems = importTask.getItems();
			}
			if (file == null) {
				file = importTask.getFile();
			}
			List<Object> duplicates = importTask.getDuplicates();
			List<SettingsItem> selectedItems = importTask.getSelectedItems();
			if (duplicates == null) {
				importTask.setDuplicatesListener(getDuplicatesListener());
			} else if (duplicates.isEmpty() && selectedItems != null && file != null) {
				settingsHelper.importSettings(file, selectedItems, "", 1, getImportListener());
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		description.setText(R.string.select_data_to_import);

		return view;
	}

	@Override
	protected void onContinueButtonClickAction() {
		if (adapter.getData().isEmpty()) {
			app.showShortToastMessage(getString(R.string.shared_string_nothing_selected));
		} else {
			importItems();
		}
	}

	private void updateUi(int toolbarTitleRes, int descriptionRes) {
		if (file != null) {
			String fileName = file.getName();
			toolbarLayout.setTitle(getString(toolbarTitleRes));
			description.setText(UiUtilities.createSpannableString(
					String.format(getString(descriptionRes), fileName),
					Typeface.BOLD, fileName
			));
			buttonsContainer.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			adapter.clearSettingsList();
		}
	}

	private void importItems() {
		List<SettingsItem> selectedItems = settingsHelper.prepareSettingsItems(adapter.getData(), settingsItems, false);
		if (file != null && settingsItems != null) {
			duplicateStartTime = System.currentTimeMillis();
			settingsHelper.checkDuplicates(file, settingsItems, selectedItems, getDuplicatesListener());
		}
		updateUi(R.string.shared_string_preparing, R.string.checking_for_duplicate_description);
	}

	@Override
	protected void importFinished(boolean succeed, boolean needRestart, List<SettingsItem> items) {
		if (succeed) {
			FragmentManager fm = getFragmentManager();
			if (fm != null && file != null) {
				ImportCompleteFragment.showInstance(fm, items, file.getName(), needRestart);
			}
		}
	}

	protected void processDuplicates(List<Object> duplicates, List<SettingsItem> items) {
		FragmentManager fm = getFragmentManager();
		if (file != null) {
			if (duplicates.isEmpty()) {
				if (isAdded()) {
					updateUi(R.string.shared_string_importing, R.string.importing_from);
				}
				settingsHelper.importSettings(file, items, "", 1, getImportListener());
			} else if (fm != null && !isStateSaved()) {
				FileImportDuplicatesFragment.showInstance(fm, duplicates, items, file, this);
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager fm, @NonNull List<SettingsItem> settingsItems, @NonNull File file) {
		FileImportSettingsFragment fragment = new FileImportSettingsFragment();
		fragment.setSettingsItems(settingsItems);
		fragment.setFile(file);
		fm.beginTransaction().
				replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(SETTINGS_LIST_TAG)
				.commitAllowingStateLoss();
	}
}