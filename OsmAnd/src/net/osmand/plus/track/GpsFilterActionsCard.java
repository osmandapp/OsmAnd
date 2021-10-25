package net.osmand.plus.track;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.AndroidUtils;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;

public class GpsFilterActionsCard extends MapBaseCard {

	private final List<BaseBottomSheetItem> actionButtonsItems;

	public GpsFilterActionsCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		actionButtonsItems = createActionButtons();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gps_filter_actions_card;
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
			resetGpsFiltersToOriginal();
		} else if (actionButton == ActionButton.SAVE_AS_COPY) {
			saveAsCopy();
		} else {
			saveIntoFile();
		}

		CardListener listener = getListener();
		if (listener != null) {
			listener.onCardButtonPressed(this, actionButton.ordinal());
		}
	}

	private void resetGpsFiltersToOriginal() {
		// todo gps
	}

	private void saveAsCopy() {
		// todo gps
	}

	private void saveIntoFile() {
		// todo gps
	}

	@Override
	protected void updateContent() {
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

	public enum ActionButton {

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