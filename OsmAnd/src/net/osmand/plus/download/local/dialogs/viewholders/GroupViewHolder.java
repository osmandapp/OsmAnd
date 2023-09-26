package net.osmand.plus.download.local.dialogs.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.dialogs.CategoriesAdapter.LocalTypeListener;
import net.osmand.plus.download.local.dialogs.MemoryInfo;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class GroupViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final boolean nightMode;

	private final ImageView icon;
	private final TextView title;
	private final TextView description;
	private final ProgressBar progressBar;
	private final View bottomShadow;

	public GroupViewHolder(@NonNull View itemView, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();
		this.nightMode = nightMode;

		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.size);
		progressBar = itemView.findViewById(R.id.progress_bar);
		bottomShadow = itemView.findViewById(R.id.bottom_shadow);

		setupSelectableBackground(itemView.findViewById(R.id.selectable_list_item));
	}

	public void bindView(@NonNull LocalGroup group, @NonNull MemoryInfo memoryInfo, @Nullable LocalTypeListener listener, boolean showShadow) {
		LocalItemType itemType = group.getType();
		CategoryType categoryType = itemType.getCategoryType();

		title.setText(itemType.toHumanString(app));
		description.setText(AndroidUtils.formatSize(app, group.getSize()));
		icon.setImageDrawable(uiUtilities.getIcon(itemType.getIconId(), categoryType.getColorId()));

		itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.onGroupSelected(group);
			}
		});
		setupProgressBar(group, categoryType, memoryInfo);
		AndroidUiHelper.updateVisibility(bottomShadow, showShadow);
	}

	private void setupProgressBar(@NonNull LocalGroup group, @NonNull CategoryType type, @NonNull MemoryInfo memoryInfo) {
		long size = group.getSize();
		long maxProgress = memoryInfo.getSize();
		int percentage = maxProgress != 0 ? ProgressHelper.normalizeProgressPercent((int) (size * 100 / maxProgress)) : 0;
		progressBar.setProgress(percentage);

		int progressColor = ContextCompat.getColor(app, type.getColorId());
		int backgroundColor = ColorUtilities.getDividerColor(app, nightMode);
		progressBar.setProgressDrawable(AndroidUtils.createProgressDrawable(backgroundColor, progressColor));
	}

	private void setupSelectableBackground(@NonNull View view) {
		int color = ColorUtilities.getActiveColor(app, nightMode);
		AndroidUtils.setBackground(view, UiUtilities.getColoredSelectableDrawable(app, color, 0.3f));
	}
}
