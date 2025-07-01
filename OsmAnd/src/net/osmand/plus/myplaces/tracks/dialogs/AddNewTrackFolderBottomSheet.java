package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.utils.FileUtils.ILLEGAL_PATH_NAME_CHARACTERS;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import java.io.File;

public class AddNewTrackFolderBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = AddNewTrackFolderBottomSheet.class.getName();

	private static final String FOLDER_NAME_KEY = "folder_name_key";
	private static final String FOLDER_PATH_KEY = "folder_path_key";

	private TextInputEditText editText;
	private TextInputLayout nameTextBox;

	private File parentFolder;
	private String folderName;
	private boolean rightButtonEnabled = true;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			folderName = savedInstanceState.getString(FOLDER_NAME_KEY);
			String path = savedInstanceState.getString(FOLDER_PATH_KEY, null);
			if (!Algorithms.isEmpty(path)) {
				parentFolder = new File(path);
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.add_new_folder)));

		View view = inflate(R.layout.track_name_edit_text);
		nameTextBox = view.findViewById(R.id.name_text_box);
		int textBoxBgColorId = nightMode ? R.color.card_and_list_background_light : R.color.activity_background_color_light;
		int textBoxBgColor = ContextCompat.getColor(app, textBoxBgColorId);
		if (nightMode) {
			textBoxBgColor = ColorUtilities.getColorWithAlpha(textBoxBgColor, 0.1f);
		}
		nameTextBox.setBoxBackgroundColor(textBoxBgColor);
		nameTextBox.setHint(AndroidUtils.addColon(app, R.string.shared_string_name));
		ColorStateList colorStateList = ColorStateList.valueOf(ColorUtilities.getSecondaryTextColor(app, nightMode));
		nameTextBox.setDefaultHintTextColor(colorStateList);
		editText = view.findViewById(R.id.name_edit_text);
		editText.setText(folderName);
		editText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				updateFileNameFromEditText(s.toString().trim());
			}
		});
		editText.requestFocus();
		AndroidUtils.softKeyboardDelayed(requireActivity(), editText);

		BaseBottomSheetItem editFolderName = new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(editFolderName);

		items.add(new DividerSpaceItem(app, dpToPx(12)));
	}

	@Override
	protected boolean isRightBottomButtonEnabled() {
		return rightButtonEnabled;
	}

	private void updateFileNameFromEditText(String name) {
		rightButtonEnabled = false;
		if (Algorithms.isBlank(name)) {
			nameTextBox.setError(getString(R.string.empty_filename));
		} else {
			if (ILLEGAL_PATH_NAME_CHARACTERS.matcher(name).find()) {
				nameTextBox.setError(getString(R.string.file_name_containes_illegal_char));
			} else {
				File parent = parentFolder != null ? parentFolder : app.getAppPath(GPX_INDEX_DIR);
				File destFolder = new File(parent, name);
				if (destFolder.exists()) {
					nameTextBox.setError(getString(R.string.file_with_name_already_exist));
				} else {
					nameTextBox.setError(null);
					nameTextBox.setErrorEnabled(false);
					rightButtonEnabled = true;
				}
			}
		}
		folderName = name;
		updateBottomButtons();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(FOLDER_NAME_KEY, folderName);
		if (parentFolder != null) {
			outState.putString(FOLDER_PATH_KEY, parentFolder.getAbsolutePath());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRightBottomButtonClick() {
		AndroidUtils.hideSoftKeyboard(requireActivity(), editText);
		Fragment fragment = getTargetFragment();
		if (!Algorithms.isBlank(folderName)) {
			if (fragment instanceof OnTrackFolderAddListener listener) {
				listener.onTrackFolderAdd(folderName);
			}
			dismiss();
		} else {
			updateFileNameFromEditText(folderName);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_add;
	}

	public interface OnTrackFolderAddListener {

		void onTrackFolderAdd(String folderName);
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable File parentFolder,
	                                @Nullable String folderName, @Nullable Fragment target, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AddNewTrackFolderBottomSheet fragment = new AddNewTrackFolderBottomSheet();
			fragment.parentFolder = parentFolder;
			fragment.folderName = folderName;
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}