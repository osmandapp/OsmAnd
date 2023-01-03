package net.osmand.plus.backup.ui.status;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.ui.ChangeItemActionsBottomSheet;
import net.osmand.plus.backup.ui.ChangesTabFragment;
import net.osmand.plus.backup.ui.ChangesTabFragment.CloudChangeItem;
import net.osmand.plus.helpers.AndroidUiHelper;

public class ItemViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;

	private final TextView title;
	private final TextView description;
	private final ProgressBar progressBar;
	private final AppCompatImageView icon;
	private final AppCompatImageView secondIcon;
	private final View divider;

	public ItemViewHolder(@NonNull View itemView) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		progressBar = itemView.findViewById(R.id.progressBar);
		secondIcon = itemView.findViewById(R.id.second_icon);
		description = itemView.findViewById(R.id.description);
		divider = itemView.findViewById(R.id.bottom_divider);
	}

	public void bindView(@NonNull CloudChangeItem item, @Nullable ChangesTabFragment fragment, boolean lastItem) {
		title.setText(item.title);
		description.setText(item.description);

		if (item.iconId != -1) {
			icon.setImageDrawable(getContentIcon(item.iconId));
		}
		secondIcon.setImageDrawable(getContentIcon(R.drawable.ic_overflow_menu_white));

		OnClickListener listener = fragment != null ? view -> {
			FragmentManager manager = fragment.getFragmentManager();
			if (manager != null) {
				ChangeItemActionsBottomSheet.showInstance(manager, item, fragment);
			}
		} : null;
		itemView.setOnClickListener(listener);
		itemView.setEnabled(app.getNetworkSettingsHelper().getSyncTask(item.fileName) == null);
		AndroidUiHelper.updateVisibility(divider, !lastItem);
	}

	@Nullable
	protected Drawable getContentIcon(@DrawableRes int icon) {
		return app.getUIUtilities().getIcon(icon, R.color.icon_color_secondary_light);
	}

}