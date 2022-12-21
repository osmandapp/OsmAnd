package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.base.OsmandBaseExpandableListAdapter.adjustIndicator;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

public class CloudSyncCard extends BaseCard {

	public static final int SYNC_BUTTON_INDEX = 0;
	public static final int LOCAL_CHANGES_BUTTON_INDEX = 1;
	public static final int CLOUD_CHANGES_BUTTON_INDEX = 2;
	public static final int CONFLICTS_BUTTON_INDEX = 3;

	private boolean actionsVisible;

	public CloudSyncCard(@NonNull FragmentActivity activity) {
		super(activity, false);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.cloud_sync_card;
	}

	@Override
	protected void updateContent() {
		setupHeader();
		setupButtons();
		updateButtonsVisibility();
	}

	private void setupHeader() {
		TextViewEx title = view.findViewById(R.id.sync_title);
		TextViewEx lastSync = view.findViewById(R.id.last_sync);
		TextViewEx changes = view.findViewById(R.id.changes);
		AppCompatImageView headerIcon = view.findViewById(R.id.sync_header_icon);
		ProgressBar progressBar = view.findViewById(R.id.progress_bar);

		if (true) {
			String backupTime = "";
			if (Algorithms.isEmpty(backupTime)) {
				lastSync.setText(R.string.shared_string_never);
			} else {
				lastSync.setText(backupTime);
			}

			int changesCount = 0;
			String changesCountString = app.getString(R.string.changes, String.valueOf(changesCount));
			if (changesCount > 0) {
				changes.setText(changesCountString);
				headerIcon.setImageResource(R.drawable.ic_action_cloud_alert);
			} else {
				changes.setText(R.string.no_new_changes);
				headerIcon.setImageResource(R.drawable.ic_action_cloud_done);
			}

			AndroidUiHelper.updateVisibility(progressBar, false);
		} else {
			AndroidUiHelper.updateVisibility(progressBar, true);
		}

		View headerContainer = view.findViewById(R.id.header_container);
		headerContainer.setOnClickListener(view -> {
			actionsVisible = !actionsVisible;
			updateButtonsVisibility();
		});
		setupSelectableBackground(headerContainer);
	}

	private void setupButtons() {
		setupLocalChangesButton();
		setupCloudChangesButton();
		setupConflictsButton();
		setupSyncButton();
	}

	private void setupSyncButton() {
		String title = app.getString(R.string.sync_now);
		Drawable icon = getActiveIcon(R.drawable.ic_action_update);
		setupButton(view.findViewById(R.id.sync_button), title, icon, SYNC_BUTTON_INDEX);
	}

	private void setupLocalChangesButton() {
		String title = app.getString(R.string.local_changes);
		Drawable icon = getActiveIcon(R.drawable.ic_action_phone_filled);
		setupButton(view.findViewById(R.id.local_changes_button), title, icon, LOCAL_CHANGES_BUTTON_INDEX);
	}

	private void setupCloudChangesButton() {
		String title = app.getString(R.string.cloud_changes);
		Drawable icon = getActiveIcon(R.drawable.ic_action_cloud);
		setupButton(view.findViewById(R.id.cloud_changes_button), title, icon, CLOUD_CHANGES_BUTTON_INDEX);
	}

	private void setupConflictsButton() {
		String title = app.getString(R.string.backup_conflicts);
		Drawable icon = getActiveIcon(R.drawable.ic_small_warning);
		setupButton(view.findViewById(R.id.conflicts_button), title, icon, CONFLICTS_BUTTON_INDEX);
	}

	private void setupButton(@NonNull View button, @Nullable String text, @Nullable Drawable icon, int buttonIndex) {
		TextView textView = button.findViewById(android.R.id.title);
		ImageView imageView = button.findViewById(android.R.id.icon);

		textView.setText(text);
		imageView.setImageDrawable(icon);
		setupSelectableBackground(button);
		button.setOnClickListener(v -> notifyButtonPressed(buttonIndex));
	}

	private void updateButtonsVisibility() {
		adjustIndicator(app, view.findViewById(R.id.header_container), actionsVisible, nightMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.actions_container), actionsVisible);
	}

	private void setupSelectableBackground(@NonNull View view) {
		Context ctx = view.getContext();
		int color = ColorUtilities.getActiveColor(ctx, nightMode);
		AndroidUtils.setBackground(view, UiUtilities.getColoredSelectableDrawable(ctx, color, 0.3f));
	}
}

