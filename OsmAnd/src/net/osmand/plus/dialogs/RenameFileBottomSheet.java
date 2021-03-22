package net.osmand.plus.dialogs;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.AndroidUtils;
import net.osmand.FileUtils.RenameCallback;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;

import static net.osmand.FileUtils.ILLEGAL_FILE_NAME_CHARACTERS;
import static net.osmand.FileUtils.renameFile;
import static net.osmand.FileUtils.renameGpxFile;
import static net.osmand.FileUtils.renameSQLiteFile;

public class RenameFileBottomSheet extends MenuBottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil.getLog(RenameFileBottomSheet.class);
	private static final String TAG = RenameFileBottomSheet.class.getName();
	private static final String SOURCE_FILE_NAME_KEY = "source_file_name_key";
	private static final String SELECTED_FILE_NAME_KEY = "selected_file_name_key";

	private OsmandApplication app;

	private TextInputEditText editText;
	private TextInputLayout nameTextBox;

	private File file;
	private String selectedFileName;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		if (savedInstanceState != null) {
			String path = savedInstanceState.getString(SOURCE_FILE_NAME_KEY);
			if (!Algorithms.isEmpty(path)) {
				file = new File(path);
			}
			selectedFileName = savedInstanceState.getString(SELECTED_FILE_NAME_KEY);
		} else {
			selectedFileName = Algorithms.getFileNameWithoutExtension(file);
		}
		items.add(new TitleItem(getString(R.string.shared_string_rename)));

		View view = UiUtilities.getInflater(app, nightMode).inflate(R.layout.track_name_edit_text, null);
		nameTextBox = view.findViewById(R.id.name_text_box);
		nameTextBox.setBoxBackgroundColorResource(nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light);
		nameTextBox.setHint(AndroidUtils.addColon(app, R.string.shared_string_name));
		ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat
				.getColor(app, nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light));
		nameTextBox.setDefaultHintTextColor(colorStateList);

		editText = view.findViewById(R.id.name_edit_text);
		editText.setText(selectedFileName);
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				updateFileName(s.toString());
			}
		});

		BaseBottomSheetItem editFolderName = new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(editFolderName);
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
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(SOURCE_FILE_NAME_KEY, file.getAbsolutePath());
		outState.putString(SELECTED_FILE_NAME_KEY, selectedFileName);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRightBottomButtonClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AndroidUtils.hideSoftKeyboard(activity, editText);
		}
		File dest;
		int index = file.getName().lastIndexOf('.');
		String ext = index == -1 ? "" : file.getName().substring(index);
		String newName = selectedFileName;
		if (selectedFileName.endsWith(ext)) {
			newName = selectedFileName.substring(0, selectedFileName.lastIndexOf(ext));
		}
		if (SQLiteTileSource.EXT.equals(ext)) {
			dest = renameSQLiteFile(app, file, newName + ext, null);
		} else if (IndexConstants.GPX_FILE_EXT.equals(ext)) {
			dest = renameGpxFile(app, file, newName + ext, false, null);
		} else {
			dest = renameFile(app, file, newName + ext, false, null);
		}
		if (dest != null) {
			Fragment fragment = getTargetFragment();
			if (fragment instanceof RenameCallback) {
				RenameCallback listener = (RenameCallback) fragment;
				listener.renamedTo(dest);
			}
			dismiss();
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

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target,
									@NonNull File file, boolean usedOnMap) {
		if (file.exists() && !fragmentManager.isStateSaved()
				&& fragmentManager.findFragmentByTag(RenameFileBottomSheet.TAG) == null) {
			RenameFileBottomSheet fragment = new RenameFileBottomSheet();
			fragment.file = file;
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, RenameFileBottomSheet.TAG);
		}
	}
}