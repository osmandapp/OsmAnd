package net.osmand.plus.myplaces.controller;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.bottomsheets.CustomizableOptionsBottomSheet;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.loadinfo.GpxInfoLoader;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;

public class GPXFolderOptionsController implements IDisplayDataProvider, IDialogItemClicked {

	public static final String PROCESS_ID = "tracks_folder_options";

	private final OsmandApplication app;
	private TrackFolder trackFolder;

	private enum ListOption {
		DETAILS(R.string.shared_string_details, R.drawable.ic_action_info_dark),
		SHOW_ALL_TRACKS(R.string.show_all_tracks_on_the_map, R.drawable.ic_show_on_map),
		EDIT_NAME(R.string.edit_name, R.drawable.ic_action_edit_dark),
		CHANGE_APPEARANCE(R.string.change_default_appearance, R.drawable.ic_action_appearance),
		EXPORT(R.string.shared_string_export, R.drawable.ic_action_upload),
		MOVE(R.string.shared_string_move, R.drawable.ic_action_folder_move),
		DELETE_FOLDER(R.string.shared_string_remove_folder, R.drawable.ic_action_delete_dark);

		@StringRes
		private final int titleId;
		@DrawableRes
		private final int iconId;

		ListOption(@StringRes int titleId, @DrawableRes int iconId) {
			this.titleId = titleId;
			this.iconId = iconId;
		}

		@StringRes
		public int getTitleId() {
			return titleId;
		}

		@DrawableRes
		public int getIconId() {
			return iconId;
		}

		public boolean shouldShowBottomDivider() {
			return Algorithms.equalsToAny(this, SHOW_ALL_TRACKS, CHANGE_APPEARANCE, MOVE);
		}
	}

	public GPXFolderOptionsController(@NonNull OsmandApplication app, @NonNull TrackFolder folder) {
		this.app = app;
		this.trackFolder = folder;
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		UiUtilities iconsCache = app.getUIUtilities();
		DisplayData displayData = new DisplayData();
		int folderColorAlpha = ColorUtilities.getColorWithAlpha(trackFolder.getColor(), 0.3f);
		displayData.putExtra(BACKGROUND_COLOR, folderColorAlpha);

		displayData.addDisplayItem(new DisplayItem()
				.setTitle(trackFolder.getName())
				.setDescription(getFolderDescription())
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_72dp)
				.setIcon(iconsCache.getPaintedIcon(R.drawable.ic_action_folder, trackFolder.getColor()))
				.setShowBottomDivider(true, 0)
				.setClickable(false)
		);

		int dividerPadding = calculateSubtitleDividerPadding();
		for (ListOption listOption : ListOption.values()) {
			displayData.addDisplayItem(new DisplayItem()
					.setTitle(getString(listOption.getTitleId()))
					.setLayoutId(R.layout.bottom_sheet_item_simple_56dp_padding_32dp)
					.setIcon(iconsCache.getThemedIcon(listOption.getIconId()))
					.setShowBottomDivider(listOption.shouldShowBottomDivider(), dividerPadding)
					.setTag(listOption)
			);
		}
		return displayData;
	}

	@Override
	public void onDialogItemClicked(@NonNull String processId, @NonNull DisplayItem item) {
		if (!(item.getTag() instanceof ListOption)) {
			return;
		}
		ListOption option = (ListOption) item.getTag();
		if (option == ListOption.DETAILS) {
			showDetails();
		} else if (option == ListOption.SHOW_ALL_TRACKS) {
			showAllTracksOnMap();
		} else if (option == ListOption.EDIT_NAME) {
			showEditNameDialog();
		} else if (option == ListOption.CHANGE_APPEARANCE) {
			showChangeAppearanceDialog();
		} else if (option == ListOption.EXPORT) {
			showExportDialog();
		} else if (option == ListOption.MOVE) {
			showMoveDialog();
		} else if (option == ListOption.DELETE_FOLDER) {
			showDeleteDialog();
		}
	}

	private void showDetails() {

	}

	private void showAllTracksOnMap() {

	}

	private void showEditNameDialog() {

	}

	private void showChangeAppearanceDialog() {

	}

	private void showExportDialog() {

	}

	private void showMoveDialog() {

	}

	private void showDeleteDialog() {

	}

	private String getFolderDescription() {
		String pattern = getString(R.string.ltr_or_rtl_combine_via_comma);
		return String.format(pattern, formatLastUpdateTime(), formatTracksCount());
	}

	private String formatLastUpdateTime() {
		return OsmAndFormatter.getFormattedDate(app, trackFolder.getLastModified());
	}

	private String formatTracksCount() {
		String pattern = getString(R.string.n_tracks);
		return String.format(pattern, trackFolder.getTotalTracksCount());
	}

	private int calculateSubtitleDividerPadding() {
		int contentPadding = getDimension(R.dimen.content_padding);
		int iconWidth = getDimension(R.dimen.standard_icon_size);
		return contentPadding * 3 + iconWidth;
	}

	public int getDimension(@DimenRes int id) {
		return app.getResources().getDimensionPixelSize(id);
	}

	@NonNull
	private String getString(@StringRes int stringId) {
		return app.getString(stringId);
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull File rootDir) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		GpxInfoLoader.loadGpxInfoForDirectory(app, rootDir, result -> showDialog(activity, result));
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull TrackFolder folder) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		GPXFolderOptionsController controller = new GPXFolderOptionsController(app, folder);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		CustomizableOptionsBottomSheet.showInstance(fragmentManager, PROCESS_ID, false);
	}
}
