package net.osmand.plus.search.listitems;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

class DownloadItemsAdapter extends RecyclerView.Adapter<DownloadItemsAdapter.ViewHolder> {
	public static final Integer DOWNLOADING_WIKI_MAPS_TYPE = 0;

	public interface OnItemClickListener {
		void onItemClick(IndexItem item);
	}

	private final OsmandApplication app;
	private final OnItemClickListener listener;
	private List<Object> items = new ArrayList<>();
	private boolean nightMode;

	DownloadItemsAdapter(@NonNull OsmandApplication app, @NonNull OnItemClickListener listener, boolean nightMode) {
		this.app = app;
		this.listener = listener;
		this.nightMode = nightMode;
	}

	public void setItems(List<Object> items) {
		this.items = items;
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		View view = inflater.inflate(R.layout.list_item_icon_and_download, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		Object item = items.get(position);
		if (item == DOWNLOADING_WIKI_MAPS_TYPE) {
			holder.bindLoading();
		} else {
			holder.bind((IndexItem) item, position == getItemCount() - 1);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	class ViewHolder extends RecyclerView.ViewHolder {
		private final TextView title;
		private final TextView description;
		private final ImageView icon;
		private final ProgressBar progressBar;
		private final ImageView secondaryIcon;
		private final View divider;

		public ViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			icon = itemView.findViewById(R.id.icon);
			progressBar = itemView.findViewById(R.id.ProgressBar);
			secondaryIcon = itemView.findViewById(R.id.secondary_icon);
			divider = itemView.findViewById(R.id.divider);
		}

		public void bind(IndexItem item, boolean isLastItem) {
			title.setText(item.getVisibleName(app, app.getRegions(), false));
			description.setText(DownloadActivityType.WIKIPEDIA_FILE.getString(app) + " • " + item.getSizeDescription(app));
			progressBar.setIndeterminate(false);
			icon.setImageDrawable(getIconDrawable(DownloadActivityType.WIKIPEDIA_FILE.getIconResource()));
			AndroidUiHelper.updateVisibility(icon, true);
			AndroidUiHelper.updateVisibility(secondaryIcon, true);
			AndroidUiHelper.updateVisibility(divider, !isLastItem);
			DownloadIndexesThread downloadThread = app.getDownloadThread();
			if (downloadThread.isDownloading(item)) {
				progressBar.setProgress((int) downloadThread.getCurrentDownloadProgress());
				AndroidUiHelper.updateVisibility(progressBar, true);
				secondaryIcon.setImageDrawable(getIconDrawable(R.drawable.ic_action_remove_dark));
			} else {
				AndroidUiHelper.updateVisibility(progressBar, false);
				secondaryIcon.setImageDrawable(getIconDrawable(R.drawable.ic_action_import));
			}
			itemView.setOnClickListener(v -> listener.onItemClick(item));
		}

		public void bindLoading() {
			title.setText(R.string.downloading_list_indexes);
			progressBar.setIndeterminate(true);
			description.setText("");
			AndroidUiHelper.updateVisibility(progressBar, true);
			AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.icon), false);
			AndroidUiHelper.updateVisibility(divider, false);
			progressBar.setVisibility(View.VISIBLE);
			secondaryIcon.setVisibility(View.GONE);
		}
	}

	private Drawable getIconDrawable(@DrawableRes int resId) {
		return app.getUIUtilities().getIcon(resId, nightMode);
	}
}