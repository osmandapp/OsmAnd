package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.plus.settings.DataStorageMenuItem;

import org.apache.commons.logging.Log;

import java.io.File;

import static net.osmand.plus.settings.DataStorageHelper.MANUALLY_SPECIFIED;

public class ChangeDataStorageBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = "ChangeDataStorageBottomSheet";

	private static final Log LOG = PlatformUtil.getLog(ChangeDataStorageBottomSheet.class);

	private final static String CURRENT_DIRECTORY = "current_directory";
	private final static String NEW_DIRECTORY = "new_directory";

	public final static String MOVE_DATA = "move_data";
	public final static String CHOSEN_DIRECTORY = "chosen_storage";

	private DataStorageMenuItem currentDirectory;
	private DataStorageMenuItem newDirectory;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		Context ctx = getContext();

		if (savedInstanceState != null) {
			currentDirectory = savedInstanceState.getParcelable(CURRENT_DIRECTORY);
			newDirectory = savedInstanceState.getParcelable(NEW_DIRECTORY);
		}

		if (ctx == null || currentDirectory == null || newDirectory == null) {
			return;
		}

		items.add(new TitleItem(getString(R.string.change_osmand_data_folder_question)));

		int textColorPrimary = nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
		int activeColor = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		CharSequence desc = null;
		
		File currentStorageFile = new File(currentDirectory.getDirectory());
		if ((!OsmandSettings.isWritable(currentStorageFile))) {
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
		btnDontMoveView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				positiveButtonsClick(false);
			}
		});
		UiUtilities.setupDialogButton(nightMode, btnDontMoveView, UiUtilities.DialogButtonType.SECONDARY, getString(R.string.dont_move_maps), currentDirectory.getIconResId());

		View btnMoveView = mainView.findViewById(R.id.btnMove);
		btnMoveView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				positiveButtonsClick(true);
			}
		});
		UiUtilities.setupDialogButton(nightMode, btnMoveView, UiUtilities.DialogButtonType.PRIMARY, getString(R.string.move_maps_to_new_destination), R.drawable.ic_action_folder_move);

		View btnCloseView = mainView.findViewById(R.id.btnClose);
		btnCloseView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		UiUtilities.setupDialogButton(nightMode, btnCloseView, UiUtilities.DialogButtonType.SECONDARY, getString(R.string.shared_string_cancel), R.drawable.ic_action_undo_dark);

		BaseBottomSheetItem baseItem = new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
		items.add(baseItem);
	}

	public void setCurrentDirectory(DataStorageMenuItem currentDirectory) {
		this.currentDirectory = currentDirectory;
	}

	public void setNewDirectory(DataStorageMenuItem newDirectory) {
		this.newDirectory = newDirectory;
	}

	private void positiveButtonsClick(boolean moveData) {
		Bundle bundle = new Bundle();
		bundle.putBoolean(TAG, true);
		bundle.putParcelable(CHOSEN_DIRECTORY, newDirectory);
		bundle.putBoolean(MOVE_DATA, moveData);
		Fragment fragment = getTargetFragment();
		if (fragment  instanceof BaseSettingsFragment) {
			((BaseSettingsFragment) fragment).onPreferenceChange(getPreference(), bundle);
		}
		dismiss();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(CURRENT_DIRECTORY, currentDirectory);
		outState.putParcelable(NEW_DIRECTORY, newDirectory);
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static boolean showInstance(FragmentManager fm, String prefId, DataStorageMenuItem currentDirectory,
	                                   DataStorageMenuItem newDirectory, Fragment target, boolean usedOnMap) {
		try {
			if (fm.findFragmentByTag(TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);

				ChangeDataStorageBottomSheet fragment = new ChangeDataStorageBottomSheet();
				fragment.setCurrentDirectory(currentDirectory);
				fragment.setNewDirectory(newDirectory);
				fragment.setTargetFragment(target, 0);
				fragment.setUsedOnMap(usedOnMap);
				fragment.show(fm, TAG);
				return true;
			}
		} catch (RuntimeException e) {
			LOG.error(e.getMessage());
		}
		return false;
	}
}