package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.settings.enums.MediaStorageType.MANUALLY_SPECIFIED;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.settings.enums.MediaStorageType;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.util.Algorithms;

public class ChangeMediaStorageBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = ChangeMediaStorageBottomSheet.class.getSimpleName();

	private static final String CURRENT_STORAGE_TYPE = "current_storage_type";
	private static final String NEW_STORAGE_TYPE = "new_storage_type";
	private static final String NEW_MANUAL_URI = "new_manual_uri";

	public static final String CHOSEN_STORAGE_TYPE = "chosen_storage_type";
	public static final String CHOSEN_MANUAL_URI = "chosen_manual_uri";
	public static final String MOVE_MEDIA = "move_media";

	private MediaStorageType currentStorageType;
	private MediaStorageType newStorageType;
	@Nullable
	private String newManualUri;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}
		if (currentStorageType == null || newStorageType == null) {
			return;
		}

		Context ctx = UiUtilities.getThemedContext(requireContext(), nightMode);
		items.add(new TitleItem(getString(R.string.change_media_storage_question)));

		int textColorPrimary = ColorUtilities.getPrimaryTextColorId(nightMode);

		BottomSheetItemWithDescription description = (BottomSheetItemWithDescription) new BottomSheetItemWithDescription.Builder()
				.setDescription(getDescription(ctx))
				.setDescriptionColorId(textColorPrimary)
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create();
		items.add(description);

		View mainView = inflate(R.layout.bottom_sheet_change_data_storage);
		View btnDontMoveView = mainView.findViewById(R.id.btnDontMove);
		btnDontMoveView.setOnClickListener(v -> positiveButtonsClick(false));
		UiUtilities.setupDialogButton(nightMode, btnDontMoveView, DialogButtonType.SECONDARY,
				getString(R.string.dont_move_maps), R.drawable.ic_action_folder);

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

	@NonNull
	private SpannableString getDescription(@NonNull Context ctx) {
		int activeColor = ColorUtilities.getActiveColor(ctx, nightMode);
		String from = getStorageName(ctx, currentStorageType, settings.MEDIA_STORAGE_MANUAL_URI.get());
		String to = getStorageName(ctx, newStorageType, newManualUri);
		String fullDescription = getString(R.string.change_media_storage_full_description, from, to);
		SpannableString description = UiUtilities.createColorSpannable(fullDescription, activeColor, from);
		UiUtilities.setSpan(description, new ForegroundColorSpan(activeColor), fullDescription, to, false);
		return description;
	}

	@NonNull
	private String getStorageName(@NonNull Context ctx, @NonNull MediaStorageType type, @Nullable String manualUri) {
		if (type == MANUALLY_SPECIFIED) {
			String displayDirectory = new MediaStorageHelper(app).getStorageDisplayDirectory(type, manualUri);
			return Algorithms.isEmpty(displayDirectory) ? type.toHumanString(ctx) : displayDirectory;
		}
		return type.toHumanString(ctx);
	}

	private void restoreState(@NonNull Bundle bundle) {
		String currentName = bundle.getString(CURRENT_STORAGE_TYPE);
		if (currentName != null) {
			currentStorageType = MediaStorageType.valueOf(currentName);
		}
		String newName = bundle.getString(NEW_STORAGE_TYPE);
		if (newName != null) {
			newStorageType = MediaStorageType.valueOf(newName);
		}
		newManualUri = bundle.getString(NEW_MANUAL_URI);
	}

	private void positiveButtonsClick(boolean moveMedia) {
		Bundle bundle = new Bundle();
		bundle.putBoolean(TAG, true);
		bundle.putString(CHOSEN_STORAGE_TYPE, newStorageType.name());
		bundle.putString(CHOSEN_MANUAL_URI, newManualUri);
		bundle.putBoolean(MOVE_MEDIA, moveMedia);

		if (getTargetFragment() instanceof BaseSettingsFragment settingsFragment) {
			settingsFragment.onPreferenceChange(getPreference(), bundle);
		}
		dismiss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (currentStorageType != null) {
			outState.putString(CURRENT_STORAGE_TYPE, currentStorageType.name());
		}
		if (newStorageType != null) {
			outState.putString(NEW_STORAGE_TYPE, newStorageType.name());
		}
		outState.putString(NEW_MANUAL_URI, newManualUri);
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String prefId,
			@NonNull MediaStorageType currentStorageType, @NonNull MediaStorageType newStorageType,
			@Nullable String newManualUri, @NonNull Fragment target, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefId);

			ChangeMediaStorageBottomSheet fragment = new ChangeMediaStorageBottomSheet();
			fragment.currentStorageType = currentStorageType;
			fragment.newStorageType = newStorageType;
			fragment.newManualUri = newManualUri;
			fragment.setTargetFragment(target, 0);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}
}