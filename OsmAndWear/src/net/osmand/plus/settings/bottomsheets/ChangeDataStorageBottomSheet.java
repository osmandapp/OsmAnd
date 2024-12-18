package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.settings.datastorage.DataStorageHelper.MANUALLY_SPECIFIED;

import android.content.Context;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import org.apache.commons.logging.Log;

import java.io.File;

public class ChangeDataStorageBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = "ChangeDataStorageBottomSheet";

	private static final Log LOG = PlatformUtil.getLog(ChangeDataStorageBottomSheet.class);

	private static final String CURRENT_DIRECTORY = "current_directory";
	private static final String NEW_DIRECTORY = "new_directory";

	public static final String MOVE_DATA = "move_data";
	public static final String CHOSEN_DIRECTORY = "chosen_storage";

	private StorageItem currentDirectory;
	private StorageItem newDirectory;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			currentDirectory = savedInstanceState.getParcelable(CURRENT_DIRECTORY);
			newDirectory = savedInstanceState.getParcelable(NEW_DIRECTORY);
		}
		if (currentDirectory == null || newDirectory == null) {
			return;
		}

		Context ctx = UiUtilities.getThemedContext(requireContext(), nightMode);
		items.add(new TitleItem(getString(R.string.change_osmand_data_folder_question)));

		int textColorPrimary = ColorUtilities.getPrimaryTextColorId(nightMode);
		int activeColor = ColorUtilities.getActiveColorId(nightMode);
		CharSequence desc = null;

		File currentStorageFile = new File(currentDirectory.getDirectory());
		if ((!FileUtils.isWritable(currentStorageFile))) {
			desc = String.format(getString(R.string.android_19_location_disabled), currentStorageFile.getAbsoluteFile());
		} else {
			String from = currentDirectory.getKey().equals(MANUALLY_SPECIFIED) ? currentDirectory.getDirectory() : currentDirectory.getTitle();
			String to = newDirectory.getKey().equals(MANUALLY_SPECIFIED) ? newDirectory.getDirectory() : newDirectory.getTitle();
			String fullDescription = String.format(getString(R.string.change_data_storage_full_description), from, to);
			SpannableStringBuilder coloredDescription = new SpannableStringBuilder(
					fullDescription);
			int startIndexFrom = fullDescription.indexOf(from);
			int endIndexFrom = startIndexFrom + from.length();
			int startIndexTo = fullDescription.indexOf(to);
			int endIndexTo = startIndexTo + to.length();
			coloredDescription.setSpan(new ForegroundColorSpan(
							ContextCompat.getColor(ctx, activeColor)),
					startIndexFrom, endIndexFrom, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			coloredDescription.setSpan(new ForegroundColorSpan(
							ContextCompat.getColor(ctx, activeColor)),
					startIndexTo, endIndexTo, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			desc = coloredDescription;
		}
		BottomSheetItemWithDescription description = (BottomSheetItemWithDescription) new BottomSheetItemWithDescription.Builder()
				.setDescription(desc)
				.setDescriptionColorId(textColorPrimary)
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create();
		items.add(description);

		//buttons
		View mainView = View.inflate(ctx, R.layout.bottom_sheet_change_data_storage, null);

		View btnDontMoveView = mainView.findViewById(R.id.btnDontMove);
		btnDontMoveView.setOnClickListener(v -> positiveButtonsClick(false));
		UiUtilities.setupDialogButton(nightMode, btnDontMoveView, DialogButtonType.SECONDARY,
				getString(R.string.dont_move_maps), currentDirectory.getSelectedIconResId());

		View btnMoveView = mainView.findViewById(R.id.btnMove);
		btnMoveView.setOnClickListener(v -> positiveButtonsClick(true));
		UiUtilities.setupDialogButton(nightMode, btnMoveView, DialogButtonType.PRIMARY,
				getString(R.string.move_maps_to_new_destination), R.drawable.ic_action_folder_move);

		View btnCloseView = mainView.findViewById(R.id.btnClose);
		btnCloseView.setOnClickListener(v -> dismiss());
		UiUtilities.setupDialogButton(nightMode, btnCloseView, DialogButtonType.SECONDARY,
				getString(R.string.shared_string_cancel), R.drawable.ic_action_undo_dark);

		BaseBottomSheetItem baseItem = new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
		items.add(baseItem);
	}

	public void setCurrentDirectory(StorageItem currentDirectory) {
		this.currentDirectory = currentDirectory;
	}

	public void setNewDirectory(StorageItem newDirectory) {
		this.newDirectory = newDirectory;
	}

	private void positiveButtonsClick(boolean moveData) {
		Bundle bundle = new Bundle();
		bundle.putBoolean(TAG, true);
		bundle.putParcelable(CHOSEN_DIRECTORY, newDirectory);
		bundle.putBoolean(MOVE_DATA, moveData);
		Fragment fragment = getTargetFragment();
		if (fragment instanceof BaseSettingsFragment) {
			((BaseSettingsFragment) fragment).onPreferenceChange(getPreference(), bundle);
		}
		dismiss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(CURRENT_DIRECTORY, currentDirectory);
		outState.putParcelable(NEW_DIRECTORY, newDirectory);
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String prefId,
	                                StorageItem currentDirectory, StorageItem newDirectory,
	                                Fragment target, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefId);

			ChangeDataStorageBottomSheet fragment = new ChangeDataStorageBottomSheet();
			fragment.setCurrentDirectory(currentDirectory);
			fragment.setNewDirectory(newDirectory);
			fragment.setTargetFragment(target, 0);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}
}