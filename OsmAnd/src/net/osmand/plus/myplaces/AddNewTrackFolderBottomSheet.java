package net.osmand.plus.myplaces;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;

import static net.osmand.FileUtils.ILLEGAL_PATH_NAME_CHARACTERS;

public class AddNewTrackFolderBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = AddNewTrackFolderBottomSheet.class.getName();
	private static final Log LOG = PlatformUtil.getLog(AddNewTrackFolderBottomSheet.class);
	private static final String FOLDER_NAME_KEY = "folder_name_key";

	private OsmandApplication app;

	private TextInputEditText editText;
	private TextInputLayout nameTextBox;

	private String folderName;
	private boolean rightButtonEnabled = true;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		if (savedInstanceState != null) {
			folderName = savedInstanceState.getString(FOLDER_NAME_KEY);
		}
		items.add(new TitleItem(getString(R.string.add_new_folder)));

		View view = UiUtilities.getInflater(app, nightMode).inflate(R.layout.track_name_edit_text, null);
		nameTextBox = view.findViewById(R.id.name_text_box);
		nameTextBox.setBoxBackgroundColorResource(nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light);
		nameTextBox.setHint(AndroidUtils.addColon(app, R.string.shared_string_name));
		ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat
				.getColor(app, nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light));
		nameTextBox.setDefaultHintTextColor(colorStateList);
		editText = view.findViewById(R.id.name_edit_text);
		editText.setText(folderName);
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				updateFileNameFromEditText(s.toString());
			}
		});
		BaseBottomSheetItem editFolderName = new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(editFolderName);
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
				File destFolder = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), name);
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
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(FOLDER_NAME_KEY, folderName);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRightBottomButtonClick() {
		AndroidUtils.hideSoftKeyboard(requireActivity(), editText);
		Fragment fragment = getTargetFragment();
		if (!Algorithms.isBlank(folderName)) {
			if (fragment instanceof OnTrackFolderAddListener) {
				OnTrackFolderAddListener listener = (OnTrackFolderAddListener) fragment;
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

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target, boolean usedOnMap) {
		try {
			if (!fragmentManager.isStateSaved() && fragmentManager.findFragmentByTag(AddNewTrackFolderBottomSheet.TAG) == null) {
				AddNewTrackFolderBottomSheet fragment = new AddNewTrackFolderBottomSheet();
				fragment.setUsedOnMap(usedOnMap);
				fragment.setTargetFragment(target, 0);
				fragment.show(fragmentManager, AddNewTrackFolderBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}