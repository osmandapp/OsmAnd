package net.osmand.plus.dialogs;

import static net.osmand.plus.utils.FileUtils.ILLEGAL_FILE_NAME_CHARACTERS;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;

public class RenameFileBottomSheet extends MenuBottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil.getLog(RenameFileBottomSheet.class);
	private static final String TAG = RenameFileBottomSheet.class.getName();
	private static final String SOURCE_FILE_NAME_KEY = "source_file_name_key";
	private static final String SELECTED_FILE_NAME_KEY = "selected_file_name_key";

	private OsmandApplication app;

	private TextInputLayout nameTextBox;
	private TextInputEditText editText;

	private File srcFile;
	private String selectedFileName;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		if (savedInstanceState != null) {
			String path = savedInstanceState.getString(SOURCE_FILE_NAME_KEY);
			if (!Algorithms.isEmpty(path)) {
				srcFile = new File(path);
			}
			selectedFileName = savedInstanceState.getString(SELECTED_FILE_NAME_KEY);
		} else {
			selectedFileName = Algorithms.getFileNameWithoutExtension(srcFile);
		}
		items.add(new TitleItem(getString(R.string.shared_string_rename)));

		View mainView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.track_name_edit_text, null);
		nameTextBox = setupTextBox(mainView);
		editText = setupEditText(mainView);
		AndroidUtils.softKeyboardDelayed(getActivity(), editText);

		BaseBottomSheetItem editFolderName = new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
		items.add(editFolderName);
	}

	private TextInputLayout setupTextBox(View mainView) {
		TextInputLayout nameTextBox = mainView.findViewById(R.id.name_text_box);
		int backgroundId = nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light;
		nameTextBox.setBoxBackgroundColorResource(backgroundId);
		nameTextBox.setHint(AndroidUtils.addColon(app, R.string.shared_string_name));
		ColorStateList colorStateList = ColorStateList.valueOf(ColorUtilities.getSecondaryTextColor(app, nightMode));
		nameTextBox.setDefaultHintTextColor(colorStateList);
		return nameTextBox;
	}

	private TextInputEditText setupEditText(View mainView) {
		TextInputEditText editText = mainView.findViewById(R.id.name_edit_text);
		editText.setText(selectedFileName);
		editText.requestFocus();
		editText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				updateFileName(s.toString());
			}
		});
		return editText;
	}

	private void updateFileName(String name) {
		if (Algorithms.isBlank(name)) {
			nameTextBox.setError(getString(R.string.empty_filename));
		} else if (ILLEGAL_FILE_NAME_CHARACTERS.matcher(name).find()) {
			nameTextBox.setError(getString(R.string.file_name_containes_illegal_char));
		} else {
			selectedFileName = name;
			nameTextBox.setError(null);
		}
		updateBottomButtons();
	}

	@Override
	protected boolean isRightBottomButtonEnabled() {
		return nameTextBox.getError() == null;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(SOURCE_FILE_NAME_KEY, srcFile.getAbsolutePath());
		outState.putString(SELECTED_FILE_NAME_KEY, selectedFileName);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRightBottomButtonClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AndroidUtils.hideSoftKeyboard(activity, editText);
		}
		File dest = renameFile();
		if (dest != null) {
			app.getSmartFolderHelper().onTrackRenamed(SharedUtil.kFile(srcFile), SharedUtil.kFile(dest));
			Fragment fragment = getTargetFragment();
			if (fragment instanceof RenameCallback) {
				((RenameCallback) fragment).fileRenamed(srcFile, dest);
			}
			dismiss();
		}
	}

	@Nullable
	private File renameFile() {
		int index = srcFile.getName().lastIndexOf('.');
		String extension = index == -1 ? "" : srcFile.getName().substring(index).trim();
		String newValidName = selectedFileName.trim();
		if (newValidName.endsWith(extension)) {
			newValidName = newValidName.substring(0, newValidName.lastIndexOf(extension)).trim();
		}

		if (SQLiteTileSource.EXT.equals(extension)) {
			return FileUtils.renameSQLiteFile(app, srcFile, newValidName + extension, null);
		} else if (IndexConstants.GPX_FILE_EXT.equals(extension)) {
			return FileUtils.renameGpxFile(app, srcFile, newValidName + extension, false, null);
		} else {
			return FileUtils.renameFile(app, srcFile, newValidName + extension, false, null);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_save;
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target,
	                                @NonNull File srcFile, boolean usedOnMap) {
		if (srcFile.exists() && AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			RenameFileBottomSheet fragment = new RenameFileBottomSheet();
			fragment.srcFile = srcFile;
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}