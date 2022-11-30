package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.base.OsmandBaseExpandableListAdapter.adjustIndicator;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.ui.ChangesFragment;
import net.osmand.plus.backup.ui.ChangesFragment.ChangesTabType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

public class CloudSyncCardViewHolder extends RecyclerView.ViewHolder {
	private final View itemView;
	private final OsmandApplication app;
	private BackupInfo info;
	private boolean changesVisible = true;

	public CloudSyncCardViewHolder(@NonNull View itemView) {
		super(itemView);
		this.itemView = itemView;
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
	}

	public void bindView(boolean nightMode, FragmentManager fragmentManager, BackupInfo info) {
		this.info = info;
		setupSyncHeader(nightMode);
		updateButtonsVisibility(nightMode);
		setupItems(nightMode, fragmentManager);
	}

	private void setupSyncButton(boolean nightMode) {

	}

	private void setupItems(boolean nightMode, androidx.fragment.app.FragmentManager fragmentManager) {
		int localChangesCount = 0;
		setupItem(nightMode, fragmentManager, R.id.local_changes_icon, R.id.local_changes_count, R.id.local_changes, localChangesCount, ChangesTabType.LOCAL_CHANGES);

		int cloudChangesCount = 0;
		setupItem(nightMode, fragmentManager, R.id.cloud_changes_icon, R.id.cloud_changes_count, R.id.cloud_changes, cloudChangesCount, ChangesTabType.CLOUD_CHANGES);

		int conflictsCount = 0;
		setupItem(nightMode, fragmentManager, R.id.conflicts_icon, R.id.conflicts_count, R.id.conflict_button, conflictsCount, ChangesTabType.CONFLICTS);
	}

	private void setupItem(boolean nightMode,
	                       androidx.fragment.app.FragmentManager fragmentManager,
	                       @IdRes int iconId,
	                       @IdRes int textViewId,
	                       @IdRes int buttonId,
	                       int count,
	                       ChangesTabType type) {
		AppCompatImageView icon = itemView.findViewById(iconId);
		setupItemIcon(icon, count, nightMode);
		TextViewEx countTextView = itemView.findViewById(textViewId);
		countTextView.setText(String.valueOf(count));
		View button = itemView.findViewById(buttonId);
		setupSelectableBackground(button);
		button.setOnClickListener(view -> ChangesFragment.showInstance(fragmentManager, type, info));
	}

	private void setupItemIcon(AppCompatImageView imageView, int changes, boolean nightMode) {
		int colorDefault = ContextCompat.getColor(app, changes > 0 ? ColorUtilities.getActiveColorId(nightMode) : ColorUtilities.getSecondaryIconColorId(nightMode));
		imageView.getDrawable().setTint(colorDefault);
	}

	private void setupSyncHeader(boolean nightMode) {
		TextViewEx title = itemView.findViewById(R.id.sync_title);
		TextViewEx lastSync = itemView.findViewById(R.id.last_sync);
		TextViewEx changes = itemView.findViewById(R.id.changes);
		AppCompatImageView headerIcon = itemView.findViewById(R.id.sync_header_icon);
		ProgressBar progressBar = itemView.findViewById(R.id.progress_bar);

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

		View headerContainer = itemView.findViewById(R.id.header_container);
		setupSelectableBackground(headerContainer);
		headerContainer.setOnClickListener(view -> {
			changesVisible = !changesVisible;
			updateButtonsVisibility(nightMode);
		});
	}

	private void updateButtonsVisibility(boolean nightMode) {
		View headerContainer = itemView.findViewById(R.id.header_container);
		View localChangesContainer = itemView.findViewById(R.id.local_changes);
		View cloudChangesContainer = itemView.findViewById(R.id.cloud_changes);
		View conflictsContainer = itemView.findViewById(R.id.conflict_button);

		adjustIndicator(getApplication(), headerContainer, changesVisible, nightMode);
		AndroidUiHelper.updateVisibility(localChangesContainer, changesVisible);
		AndroidUiHelper.updateVisibility(cloudChangesContainer, changesVisible);
		AndroidUiHelper.updateVisibility(conflictsContainer, changesVisible);
	}

	private void setupSelectableBackground(@NonNull View view) {
		int color = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.active_color_basic);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(view.getContext(), color, 0.3f);
		AndroidUtils.setBackground(view, drawable);
	}

	@NonNull
	private OsmandApplication getApplication() {
		return (OsmandApplication) itemView.getContext().getApplicationContext();
	}
}
