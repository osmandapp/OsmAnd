package net.osmand.plus.configmap.tracks.viewholders;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.TracksSearchFilter;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.utils.ColorUtilities;

public class SortTracksViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final SortTracksListener listener;
	private final boolean nightMode;

	private final TextView title;
	private final TextView filtersCount;
	private final TextView description;
	private final ImageView imageView;
	private final ImageView filtersImageView;
	private final View sortButton;

	public SortTracksViewHolder(@NonNull View view, @Nullable SortTracksListener listener, boolean nightMode) {
		super(view);
		this.app = (OsmandApplication) view.getContext().getApplicationContext();
		this.listener = listener;
		this.nightMode = nightMode;

		title = view.findViewById(R.id.title);
		filtersCount = view.findViewById(R.id.filter_count);
		description = view.findViewById(R.id.description);
		imageView = view.findViewById(R.id.icon);
		sortButton = view.findViewById(R.id.sort_button);
		filtersImageView = view.findViewById(R.id.filter_icon);
		View filterButton = view.findViewById(R.id.filter_button);
		filterButton.setOnClickListener(v -> {
			if (listener != null) {
				listener.showFiltersDialog();
			}
		});
	}

	public void bindView(boolean enabled, @Nullable TracksSearchFilter filters) {
		TracksSortMode sortMode = listener != null ? listener.getTracksSortMode() : TracksSortMode.getDefaultSortMode();
		int textColorId = enabled ? ColorUtilities.getActiveColorId(nightMode) : ColorUtilities.getSecondaryTextColorId(nightMode);
		int iconColorId = enabled ? ColorUtilities.getActiveIconColorId(nightMode) : ColorUtilities.getSecondaryIconColorId(nightMode);

		title.setTextColor(ColorUtilities.getColor(app, textColorId));
		description.setText(sortMode.getNameId());
		imageView.setImageDrawable(app.getUIUtilities().getIcon(sortMode.getIconId(), iconColorId));
		itemView.setEnabled(enabled);
		AndroidUiHelper.updateVisibility(filtersCount, filters != null);
		AndroidUiHelper.updateVisibility(filtersImageView, filters != null);
		View sortClickableView = itemView;
		if (filters != null) {
			sortClickableView = sortButton;
			itemView.setOnClickListener(null);
			int filtersCountColor = ColorUtilities.getActiveColorId(nightMode);
			int filtersIconColor = ColorUtilities.getActiveIconColorId(nightMode);
			filtersCount.setTextColor(ColorUtilities.getColor(app, filtersCountColor));
			filtersCount.setText(String.format(app.getString(R.string.filter_tracks_count), filters.getAppliedFiltersCount()));
			filtersImageView.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_filter_dark, filtersIconColor));
		}
		sortClickableView.setOnClickListener(v -> {
			if (listener != null) {
				listener.showSortByDialog();
			}
		});
	}

	public interface SortTracksListener {

		void showSortByDialog();

		default void showFiltersDialog() {

		}

		@NonNull
		TracksSortMode getTracksSortMode();

		void setTracksSortMode(@NonNull TracksSortMode sortMode, boolean sortSubFolders);
	}
}

