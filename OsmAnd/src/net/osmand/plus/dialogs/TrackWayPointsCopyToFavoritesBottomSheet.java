package net.osmand.plus.dialogs;

import static net.osmand.plus.utils.FileUtils.ILLEGAL_FILE_NAME_CHARACTERS;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.myplaces.FavouritesDbHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class TrackWayPointsCopyToFavoritesBottomSheet extends MenuBottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil.getLog(TrackWayPointsCopyToFavoritesBottomSheet.class);
	private static final String TAG = TrackWayPointsCopyToFavoritesBottomSheet.class.getName();
	private GpxDisplayGroup group;
	private OsmandApplication app;
	private TextInputLayout nameTextBox;
	private String groupName;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		groupName = Algorithms.isEmpty(group.getName()) ? app.getString(R.string.shared_string_gpx_points) : group.getName();
		View view = UiUtilities.getInflater(app, nightMode).inflate(R.layout.title_with_desc, null);
		BaseBottomSheetItem titleWithDescr = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.please_provide_group_name_message))
				.setTitle(getString(R.string.copy_to_map_favorites))
				.setCustomView(view)
				.create();

		items.add(titleWithDescr);
		View mainView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.track_name_edit_text, null);
		nameTextBox = setupTextBox(mainView);
		setupEditText(mainView);

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

	private void setupEditText(View mainView) {
		TextInputEditText editText = mainView.findViewById(R.id.name_edit_text);
		editText.setText(groupName);
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				updateGroupName(s.toString());
			}
		});
	}

	private void updateGroupName(String name) {
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

	@Override
	public void onRightBottomButtonClick() {
		copyToFavorites();
	}

	private void copyToFavorites() {
		FavouritesDbHelper favouritesDbHelper = app.getFavorites();
		for (GpxDisplayItem item : group.getModifiableList()) {
			if (item.locationStart != null) {
				FavouritePoint fp = FavouritePoint.fromWpt(item.locationStart, app, groupName);
				if (!Algorithms.isEmpty(item.description)) {
					fp.setDescription(item.description);
				}
				favouritesDbHelper.addFavourite(fp, false);
			}
		}
		favouritesDbHelper.saveCurrentPointsIntoFile();
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_copy;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment target,
	                                @NonNull GpxDisplayGroup group) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			TrackWayPointsCopyToFavoritesBottomSheet fragment = new TrackWayPointsCopyToFavoritesBottomSheet();
			fragment.group = group;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TrackWayPointsCopyToFavoritesBottomSheet.TAG);
		}
	}
}