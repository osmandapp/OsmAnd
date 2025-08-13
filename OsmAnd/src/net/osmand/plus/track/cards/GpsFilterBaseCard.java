package net.osmand.plus.track.cards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment;
import net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment.SaveAsNewTrackFragmentListener;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.helpers.FilteredSelectedGpxFile;
import net.osmand.plus.track.helpers.GpsFilterHelper;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class GpsFilterBaseCard extends MapBaseCard {

	public static final int RESET_FILTERS_BUTTON_INDEX = 0;

	protected final GpsFilterHelper gpsFilterHelper;
	protected final FilteredSelectedGpxFile filteredSelectedGpxFile;

	private final Fragment target;
	private final List<BaseBottomSheetItem> actionButtonsItems;

	public GpsFilterBaseCard(@NonNull MapActivity mapActivity,
	                         @NonNull Fragment target,
	                         @NonNull FilteredSelectedGpxFile filteredSelectedGpxFile) {
		super(mapActivity);
		this.gpsFilterHelper = app.getGpsFilterHelper();
		this.target = target;
		this.filteredSelectedGpxFile = filteredSelectedGpxFile;
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

			boolean actionAvailable = actionButton != ActionButton.SAVE_AS_COPY || isSourceFileSaved();
			int colorId = actionAvailable
					? ColorUtilities.getActiveColorId(nightMode)
					: ColorUtilities.getDefaultIconColorId(nightMode);
			Drawable icon = getColoredIcon(actionButton.iconId, colorId);

			BaseBottomSheetItem actionButtonItem = new SimpleBottomSheetItem.Builder()
					.setIcon(AndroidUtils.getDrawableForDirection(app, icon))
					.setTitle(app.getString(actionButton.titleId))
					.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
					.setOnClickListener(v -> onActionButtonClick(actionButton))
					.setTag(actionButton.ordinal())
					.create();

			actionButtons.add(actionButtonItem);
		}
		return actionButtons;
	}

	private void onActionButtonClick(@NonNull ActionButton actionButton) {
		if (actionButton == ActionButton.RESET_TO_ORIGINAL) {
			filteredSelectedGpxFile.resetFilters(app);
			notifyButtonPressed(RESET_FILTERS_BUTTON_INDEX);
		} else if (actionButton == ActionButton.SAVE_AS_COPY) {
			saveAsCopy();
		} else if (actionButton == ActionButton.SAVE_INTO_FILE) {
			saveIntoFile();
		}
	}

	private void saveAsCopy() {
		String sourceFilePath = filteredSelectedGpxFile.getGpxFile().getPath();
		String sourceFileNameWithExtension = Algorithms.getFileWithoutDirs(sourceFilePath);
		String sourceFileName = Algorithms.getFileNameWithoutExtension(sourceFileNameWithExtension);
		String destFileName = sourceFileName + "-copy";
		if (target instanceof SaveAsNewTrackFragmentListener) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			SaveAsNewTrackBottomSheetDialogFragment.showInstance(manager, destFileName, target, false, true);
		}
	}

	private void saveIntoFile() {
		GpxFile newGpxFile = filteredSelectedGpxFile.getGpxFile();
		filteredSelectedGpxFile.getSourceSelectedGpxFile().setGpxFile(newGpxFile, app);

		File outFile = new File(newGpxFile.getPath());

		SaveGpxHelper.saveGpx(outFile, newGpxFile, errorMessage -> {
			String toastMessage = errorMessage == null
					? MessageFormat.format(app.getString(R.string.gpx_saved_sucessfully), newGpxFile.getPath())
					: errorMessage.getMessage();
			app.showToastMessage(toastMessage);
			if (target instanceof SaveIntoFileListener) {
				((SaveIntoFileListener) target).onSavedIntoFile(newGpxFile.getPath());
			}
		});

		updateMainContent();
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

	@NonNull
	protected View inflateMainContent() {
		return themedInflater.inflate(getMainContentLayoutId(), view.findViewById(R.id.main_content));
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
			actionButton.inflate(mapActivity, actionButtonsContainer, nightMode);
			int dp20 = getDimen(R.dimen.title_padding);
			AndroidUtils.setPadding(actionButton.getView(), dp20, 0, 0, 0);

			boolean disableSaveAsCopy = !isSourceFileSaved()
					&& Algorithms.objectEquals(actionButton.getTag(), ActionButton.SAVE_AS_COPY.ordinal());
			if (disableSaveAsCopy) {
				actionButton.getView().setEnabled(false);
			}
		}
	}

	private boolean isSourceFileSaved() {
		return new File(filteredSelectedGpxFile.getGpxFile().getPath()).exists();
	}

	public abstract void onFinishFiltering();

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

	public interface SaveIntoFileListener {

		void onSavedIntoFile(@NonNull String filePath);
	}
}