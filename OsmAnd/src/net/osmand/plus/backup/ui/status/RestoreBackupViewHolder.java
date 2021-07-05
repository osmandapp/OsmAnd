package net.osmand.plus.backup.ui.status;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.ui.RestoreSettingsFragment;

public class RestoreBackupViewHolder extends RecyclerView.ViewHolder {

	final TextView title;
	final ImageView icon;
	final View restoreButton;

	public RestoreBackupViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(android.R.id.title);
		icon = itemView.findViewById(android.R.id.icon);
		restoreButton = itemView.findViewById(R.id.selectable_list_item);
	}

	public void bindView(@NonNull MapActivity mapActivity, boolean nightMode) {
		title.setText(R.string.backup_restore_data);

		int colorId = getActiveColorId(nightMode);
		icon.setImageDrawable(getApplication().getUIUtilities().getIcon(R.drawable.ic_action_restore, colorId));

		restoreButton.setOnClickListener(v -> {
			if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
				RestoreSettingsFragment.showInstance(mapActivity.getSupportFragmentManager());
			}
		});
		int color = ContextCompat.getColor(itemView.getContext(), colorId);
		Drawable drawableBg = UiUtilities.getColoredSelectableDrawable(itemView.getContext(), color, 0.3f);
		AndroidUtils.setBackground(restoreButton, drawableBg);
	}

	@NonNull
	private OsmandApplication getApplication() {
		return (OsmandApplication) itemView.getContext().getApplicationContext();
	}

	@ColorRes
	private int getActiveColorId(boolean nightMode) {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}
}
