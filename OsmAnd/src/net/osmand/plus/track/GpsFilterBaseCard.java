package net.osmand.plus.track;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpsFilterHelper;
import net.osmand.plus.helpers.GpsFilterHelper.GpsFilterListener;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment;
import net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment.SaveAsNewTrackFragmentListener;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;

public abstract class GpsFilterBaseCard extends MapBaseCard implements GpsFilterListener {

	protected final GpsFilterHelper gpsFilterHelper;

	private final Fragment target;
	private final List<BaseBottomSheetItem> actionButtonsItems;

	public GpsFilterBaseCard(@NonNull MapActivity mapActivity, @NonNull Fragment target) {
		super(mapActivity);
		this.gpsFilterHelper = app.getGpsFilterHelper();
		this.gpsFilterHelper.addListener(this);
		this.target = target;
		this.actionButtonsItems = createActionButtons();
	}

	@Override
	public final int getCardLayoutId() {
		return R.layout.gps_filter_base_card;
	}

	public void softScrollToActionCard() {
		((ScrollView) view).smoothScrollTo(0, ((int) view.findViewById(R.id.header).getY()));
	}

	private List<BaseBottomSheetItem> createActionButtons() {
		List<BaseBottomSheetItem> actionButtons = new ArrayList<>();
		for (ActionButton actionButton : ActionButton.values()) {

			Drawable icon = getActiveIcon(actionButton.iconId);
			BaseBottomSheetItem actionButtonItem = new SimpleBottomSheetItem.Builder()
					.setIcon(AndroidUtils.getDrawableForDirection(app, icon))
					.setTitle(app.getString(actionButton.titleId))
					.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
					.setOnClickListener(v -> onActionButtonClick(actionButton))
					.create();

			actionButtons.add(actionButtonItem);
		}
		return actionButtons;
	}

	private void onActionButtonClick(@NonNull ActionButton actionButton) {
		if (actionButton == ActionButton.RESET_TO_ORIGINAL) {
			gpsFilterHelper.resetFilters();
		} else if (actionButton == ActionButton.SAVE_AS_COPY) {
			saveAsCopy();
		} else if (actionButton == ActionButton.SAVE_INTO_FILE) {
			saveIntoFile();
		}
	}

	private void saveAsCopy() {
		String sourceFilePath = gpsFilterHelper.getFilteredSelectedGpxFile().getGpxFile().path;
		String sourceFileName = Algorithms.getFileNameWithoutExtension(Algorithms.getFileWithoutDirs(sourceFilePath));
		String finalFileName = Algorithms.isEmpty(sourceFileName)
				? MeasurementToolFragment.getSuggestedFileName(app, null)
				: sourceFileName;
		String destFileName = finalFileName + "-copy";
		if (target instanceof SaveAsNewTrackFragmentListener) {
			SaveAsNewTrackBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
					target, null, finalFileName, destFileName, false, true);
		}
	}

	private void saveIntoFile() {
		SelectedGpxFile sourceSelectedGpxFile = gpsFilterHelper.getSourceSelectedGpxFile();
		GPXFile newGpxFile = gpsFilterHelper.getFilteredSelectedGpxFile().getGpxFile();
		if (sourceSelectedGpxFile != null) {
			sourceSelectedGpxFile.setGpxFile(newGpxFile, app);
			gpsFilterHelper.setSelectedGpxFile(sourceSelectedGpxFile);

			String path = Algorithms.isEmpty(newGpxFile.path)
					? app.getAppPath(IndexConstants.GPX_INDEX_DIR) + MeasurementToolFragment.getSuggestedFileName(app, null)
					: newGpxFile.path;
			File outFile = new File(path);
			new SaveGpxAsyncTask(outFile, newGpxFile, new SaveGpxListener() {

				@Override
				public void gpxSavingStarted() {
				}

				@Override
				public void gpxSavingFinished(Exception errorMessage) {
					if (app != null) {
						String toastMessage = errorMessage == null
								? MessageFormat.format(app.getString(R.string.gpx_saved_sucessfully), newGpxFile.path)
								: errorMessage.getMessage();
						app.showToastMessage(toastMessage);
					}
					gpsFilterHelper.onSavedFile(newGpxFile.path);
				}
			}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

			updateMainContent();
		}
	}

	protected void disallowScroll() {
		((ScrollView) view).requestDisallowInterceptTouchEvent(true);
	}

	@Override
	protected final void updateContent() {
		updateMainContent();
		updateActionsButtons();
	}

	@LayoutRes
	protected abstract int getMainContentLayoutId();

	protected View inflateMainContent() {
		return UiUtilities.getInflater(mapActivity, nightMode)
				.inflate(getMainContentLayoutId(), view.findViewById(R.id.main_content));
	}

	protected abstract void updateMainContent();

	private void updateActionsButtons() {
		View header = view.findViewById(R.id.header);
		View content = view.findViewById(R.id.content);
		AppCompatImageView upDownButton = view.findViewById(R.id.up_down_button);

		header.setOnClickListener(v -> {
			boolean expanded = content.getVisibility() == View.VISIBLE;

			int arrowIconId = expanded ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up;
			int arrowIconColorId = ColorUtilities.getDefaultIconColorId(nightMode);
			upDownButton.setImageDrawable(getColoredIcon(arrowIconId, arrowIconColorId));

			AndroidUiHelper.updateVisibility(content, !expanded);
		});

		ViewGroup actionButtonsContainer = view.findViewById(R.id.action_buttons_container);
		actionButtonsContainer.removeAllViews();
		for (BaseBottomSheetItem actionButton : actionButtonsItems) {
			actionButton.inflate(mapActivity, (ViewGroup) actionButtonsContainer, nightMode);
			int dp20 = view.getResources().getDimensionPixelSize(R.dimen.title_padding);
			AndroidUtils.setPadding(actionButton.getView(), dp20, 0, 0, 0);
		}
	}

	@Override
	public void onFiltersReset() {
		updateMainContent();
	}

	private enum ActionButton {

		RESET_TO_ORIGINAL(R.drawable.ic_action_reset_to_default_dark, R.string.reset_to_original),
		SAVE_AS_COPY(R.drawable.ic_action_save_as_copy, R.string.save_as_copy),
		SAVE_INTO_FILE(R.drawable.ic_action_save_to_file, R.string.save_changes_into_file);

		@DrawableRes
		public final int iconId;
		@StringRes
		public final int titleId;

		ActionButton(@DrawableRes int iconId, @StringRes int titleId) {
			this.iconId = iconId;
			this.titleId = titleId;
		}
	}
}