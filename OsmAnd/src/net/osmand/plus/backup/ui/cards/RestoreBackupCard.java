package net.osmand.plus.backup.ui.cards;

import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.RestoreSettingsFragment;
import net.osmand.plus.routepreparationmenu.cards.MapActivityCard;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class RestoreBackupCard extends MapActivityCard {

	private BackupInfo info;

	public RestoreBackupCard(@NonNull MapActivity mapActivity, BackupInfo info) {
		super(mapActivity, false);
		this.info = info;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.restore_backup_card;
	}

	@Override
	protected void updateContent() {
		setupRestoreButton();
	}

	private void setupRestoreButton() {
		TextView title = view.findViewById(android.R.id.title);
		title.setText(R.string.backup_restore_data);

		ImageView icon = view.findViewById(android.R.id.icon);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_restore));

		view.findViewById(R.id.restore).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					List<SettingsItem> items = new ArrayList<>();
					for (RemoteFile remoteFile : info.filesToDownload) {
						if (remoteFile.item != null) {
							items.add(remoteFile.item);
						}
					}
					for (Pair<LocalFile, RemoteFile> pair : info.filesToMerge) {
						if (pair.second.item != null) {
							items.add(pair.second.item);
						}
					}
					if (!Algorithms.isEmpty(items)) {
						RestoreSettingsFragment.showInstance(mapActivity.getSupportFragmentManager(), items);
					}
				}
			}
		});
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f);
		AndroidUtils.setBackground(view.findViewById(R.id.selectable_list_item), drawable);
	}
}
