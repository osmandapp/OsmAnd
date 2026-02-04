package net.osmand.plus.myplaces.favorites.dialogs;

import static android.view.View.GONE;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;

public class SortFavoriteViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final SortFavoriteListener listener;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final ImageView imageView;

	public SortFavoriteViewHolder(@NonNull View view, @Nullable SortFavoriteListener listener, boolean nightMode) {
		super(view);
		this.app = (OsmandApplication) view.getContext().getApplicationContext();
		this.listener = listener;
		this.nightMode = nightMode;

		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		imageView = view.findViewById(R.id.icon);
		View filterButton = view.findViewById(R.id.filter_button);
		filterButton.setVisibility(GONE);
	}

	public void bindView(boolean enabled) {
		FavoriteListSortMode sortMode = listener != null ? listener.getTracksSortMode() : FavoriteListSortMode.getDefaultSortMode();
		int textColorId = enabled ? ColorUtilities.getActiveColorId(nightMode) : ColorUtilities.getSecondaryTextColorId(nightMode);
		int iconColorId = enabled ? ColorUtilities.getActiveIconColorId(nightMode) : ColorUtilities.getSecondaryIconColorId(nightMode);

		title.setTextColor(ColorUtilities.getColor(app, textColorId));
		description.setText(sortMode.getNameId());
		imageView.setImageDrawable(app.getUIUtilities().getIcon(sortMode.getIconId(), iconColorId));
		itemView.setEnabled(enabled);

		itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.showSortByDialog();
			}
		});
	}

	public interface SortFavoriteListener {

		void showSortByDialog();

		default void showFiltersDialog() {
		}

		@NonNull
		FavoriteListSortMode getTracksSortMode();

		void setTracksSortMode(@NonNull FavoriteListSortMode sortMode, boolean sortSubFolders);
	}
}