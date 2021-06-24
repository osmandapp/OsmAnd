package net.osmand.plus.liveupdates;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import static net.osmand.AndroidUtils.getPrimaryTextColorId;
import static net.osmand.AndroidUtils.getSecondaryTextColorId;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastCheck;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLatestUpdateAvailable;

public class LiveUpdatesClearBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = LiveUpdatesClearBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesClearBottomSheet.class);
	private static final String LOCAL_INDEX_FILE_NAME = "local_index_file_name";

	private OsmandApplication app;
	private OsmandSettings settings;

	private String fileName;

	public static void showInstance(@NonNull FragmentManager fragmentManager, Fragment target, String fileName) {
		if (!fragmentManager.isStateSaved()) {
			LiveUpdatesClearBottomSheet fragment = new LiveUpdatesClearBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.fileName = fileName;
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = getMyApplication();
		settings = app.getSettings();

		if (savedInstanceState != null && savedInstanceState.containsKey(LOCAL_INDEX_FILE_NAME)) {
			fileName = savedInstanceState.getString(LOCAL_INDEX_FILE_NAME);
		}

		items.add(new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.delete_updates))
				.setTitleColorId(getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_title)
				.create());

		String nameToDisplay = getNameToDisplay(fileName, app);
		String text = getString(R.string.live_update_delete_updates_msg, nameToDisplay);
		SpannableString message = UiUtilities.createSpannableString(text, Typeface.BOLD, nameToDisplay);

		items.add(new LongDescriptionItem.Builder()
				.setDescription(message)
				.setDescriptionColorId(getSecondaryTextColorId(nightMode))
				.setDescriptionMaxLines(5)
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create());

		items.add(new DividerSpaceItem(app, getResources().getDimensionPixelSize(R.dimen.content_padding_small)));
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(LOCAL_INDEX_FILE_NAME, fileName);
	}

	private void deleteUpdates() {
		IncrementalChangesManager changesManager = app.getResourceManager().getChangesManager();
		String fileNameWithoutExt = Algorithms.getFileNameWithoutExtension(fileName);
		changesManager.deleteUpdates(fileNameWithoutExt);
		preferenceLastCheck(fileName, settings).resetToDefault();
		preferenceLatestUpdateAvailable(fileName, settings).resetToDefault();
	}

	@Override
	protected void onRightBottomButtonClick() {
		deleteUpdates();

		Fragment fragment = getTargetFragment();
		if (fragment instanceof RefreshLiveUpdates) {
			((RefreshLiveUpdates) fragment).onUpdateStates(app);
		}

		dismiss();
	}

	public interface RefreshLiveUpdates {
		void onUpdateStates(Context context);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_delete;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY_HARMFUL;
	}

}
