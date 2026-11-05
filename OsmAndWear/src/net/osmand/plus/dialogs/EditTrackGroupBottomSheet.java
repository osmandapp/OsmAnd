package net.osmand.plus.dialogs;

import static net.osmand.plus.utils.FileUtils.ILLEGAL_FILE_NAME_CHARACTERS;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public abstract class EditTrackGroupBottomSheet extends MenuBottomSheetDialogFragment {

	protected GpxDisplayGroup group;
	protected OsmandApplication app;
	protected TextInputLayout nameTextBox;
	protected TextInputEditText editText;
	protected String groupName;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		groupName = Algorithms.isEmpty(group.getName()) ? app.getString(R.string.shared_string_gpx_points) : group.getName();

		View mainView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.track_name_edit_text, null);
		setupTextBox(mainView);
		setupEditText(mainView);

		BaseBottomSheetItem editFolderName = new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
		items.add(editFolderName);
	}

	protected void setupTextBox(View mainView) {
		nameTextBox = mainView.findViewById(R.id.name_text_box);
		int backgroundId = nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light;
		nameTextBox.setBoxBackgroundColorResource(backgroundId);
		nameTextBox.setHint(AndroidUtils.addColon(app, R.string.shared_string_name));
		ColorStateList colorStateList = ColorStateList.valueOf(ColorUtilities.getSecondaryTextColor(app, nightMode));
		nameTextBox.setDefaultHintTextColor(colorStateList);
	}

	protected void setupEditText(View mainView) {
		editText = mainView.findViewById(R.id.name_edit_text);
		editText.setText(groupName);
		editText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				updateGroupName(s.toString());
			}
		});
	}

	protected void updateGroupName(String name) {
		if (Algorithms.isBlank(name)) {
			nameTextBox.setError(getString(R.string.empty_filename));
		} else if (ILLEGAL_FILE_NAME_CHARACTERS.matcher(name).find()) {
			nameTextBox.setError(getString(R.string.file_name_containes_illegal_char));
		} else {
			groupName = name;
			nameTextBox.setError(null);
		}
		updateBottomButtons();
	}

	@Override
	protected boolean isRightBottomButtonEnabled() {
		return nameTextBox.getError() == null;
	}

	public interface OnGroupNameChangeListener {
		void onTrackGroupChanged();
	}
}