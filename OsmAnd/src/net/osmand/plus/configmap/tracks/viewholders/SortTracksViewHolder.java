package net.osmand.plus.configmap.tracks.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.utils.ColorUtilities;

public class SortTracksViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final SortTracksListener listener;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final ImageView imageView;

	public SortTracksViewHolder(@NonNull View view, @Nullable SortTracksListener listener, boolean nightMode) {
		super(view);
		this.app = (OsmandApplication) view.getContext().getApplicationContext();
		this.listener = listener;
		this.nightMode = nightMode;

		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		imageView = view.findViewById(R.id.icon);
		itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.showSortByDialog();
			}
		});
	}

	public void bindView(boolean enabled) {
		TracksSortMode sortMode = listener != null ? listener.getTracksSortMode() : TracksSortMode.getDefaultSortMode();
		int textColorId = enabled ? ColorUtilities.getActiveColorId(nightMode) : ColorUtilities.getSecondaryTextColorId(nightMode);
		int iconColorId = enabled ? ColorUtilities.getActiveIconColorId(nightMode) : ColorUtilities.getSecondaryIconColorId(nightMode);

		title.setTextColor(ColorUtilities.getColor(app, textColorId));
		description.setText(sortMode.getNameId());
		imageView.setImageDrawable(app.getUIUtilities().getIcon(sortMode.getIconId(), iconColorId));
		itemView.setEnabled(enabled);
	}

	public interface SortTracksListener {

		void showSortByDialog();

		@NonNull
		TracksSortMode getTracksSortMode();

		void setTracksSortMode(@NonNull TracksSortMode sortMode);
	}
}

