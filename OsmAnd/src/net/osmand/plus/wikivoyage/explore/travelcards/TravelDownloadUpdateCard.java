package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.IndexItem;

import java.text.DateFormat;

public class TravelDownloadUpdateCard extends BaseTravelCard {

	public static final int TYPE = 50;

	private boolean download;
	private boolean loadingInProgress;
	private int progress;

	private ClickListener listener;

	@Nullable
	private IndexItem indexItem;

	private DateFormat dateFormat;

	public boolean isDownload() {
		return download;
	}

	public boolean isLoadingInProgress() {
		return loadingInProgress;
	}

	public void setLoadingInProgress(boolean loadingInProgress) {
		this.loadingInProgress = loadingInProgress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setListener(ClickListener listener) {
		this.listener = listener;
	}

	public void setIndexItem(@Nullable IndexItem indexItem) {
		this.indexItem = indexItem;
	}

	public TravelDownloadUpdateCard(OsmandApplication app, boolean nightMode, boolean download) {
		super(app, nightMode);
		this.download = download;
		dateFormat = android.text.format.DateFormat.getMediumDateFormat(app);
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof DownloadUpdateVH) {
			DownloadUpdateVH holder = (DownloadUpdateVH) viewHolder;
			holder.title.setText(getTitle());
			holder.icon.setImageDrawable(getIcon());
			holder.description.setText(getDescription());
			if (indexItem == null) {
				holder.fileDataContainer.setVisibility(View.GONE);
			} else {
				holder.fileDataContainer.setVisibility(View.VISIBLE);
				holder.fileIcon.setImageDrawable(getFileIcon());
				holder.fileTitle.setText(getFileTitle());
				holder.fileDescription.setText(getFileDescription());
				holder.progressBar.setVisibility(loadingInProgress ? View.VISIBLE : View.GONE);
				holder.progressBar.setProgress(progress < 0 ? 0 : progress);
			}
			boolean primaryBtnVisible = updatePrimaryButton(holder);
			boolean secondaryBtnVisible = updateSecondaryButton(holder);
			holder.buttonsDivider.setVisibility(primaryBtnVisible && secondaryBtnVisible ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public int getCardType() {
		return TYPE;
	}

	@NonNull
	private String getTitle() {
		if (loadingInProgress) {
			return app.getString(R.string.shared_string_downloading) + "...";
		}
		return app.getString(download ? R.string.download_file : R.string.update_is_available);
	}

	private Drawable getIcon() {
		int id = download ? R.drawable.travel_card_download_icon : R.drawable.travel_card_update_icon;
		return ContextCompat.getDrawable(app, id);
	}

	@NonNull
	private String getDescription() {
		return app.getString(download ? R.string.travel_card_download_descr : R.string.travel_card_update_descr);
	}

	@NonNull
	private String getFileTitle() {
		return indexItem == null ? "" : indexItem.getBasename().replace("_", " ");
	}

	@NonNull
	private String getFileDescription() {
		StringBuilder sb = new StringBuilder();
		if (indexItem != null) {
			sb.append(app.getString(R.string.file_size_in_mb, indexItem.getArchiveSizeMB()));
			sb.append(" • ");
			sb.append(indexItem.getRemoteDate(dateFormat));
		}
		return sb.toString();
	}

	private Drawable getFileIcon() {
		return getActiveIcon(R.drawable.ic_action_read_article);
	}

	/**
	 * @return true if button is visible, false otherwise.
	 */
	private boolean updateSecondaryButton(DownloadUpdateVH vh) {
		if (loadingInProgress || !download) {
			vh.secondaryBtnContainer.setVisibility(View.VISIBLE);
			vh.secondaryBtn.setText(loadingInProgress ? R.string.shared_string_cancel : R.string.later);
			vh.secondaryBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onSecondaryButtonClick();
					}
				}
			});
			return true;
		}
		vh.secondaryBtnContainer.setVisibility(View.GONE);
		return false;
	}

	/**
	 * @return true if button is visible, false otherwise.
	 */
	private boolean updatePrimaryButton(DownloadUpdateVH vh) {
		if (!loadingInProgress) {
			vh.primaryBtnContainer.setVisibility(View.VISIBLE);
			vh.primaryButton.setText(download ? R.string.shared_string_download : R.string.shared_string_update);
			vh.primaryButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onPrimaryButtonClick();
					}
				}
			});
			return true;
		}
		vh.primaryBtnContainer.setVisibility(View.GONE);
		return false;
	}

	public interface ClickListener {

		void onPrimaryButtonClick();

		void onSecondaryButtonClick();
	}

	public static class DownloadUpdateVH extends RecyclerView.ViewHolder {

		final TextView title;
		final ImageView icon;
		final TextView description;
		final View fileDataContainer;
		final ImageView fileIcon;
		final TextView fileTitle;
		final TextView fileDescription;
		final ProgressBar progressBar;
		final View secondaryBtnContainer;
		final TextView secondaryBtn;
		final View buttonsDivider;
		final View primaryBtnContainer;
		final TextView primaryButton;

		@SuppressWarnings("RedundantCast")
		public DownloadUpdateVH(View itemView) {
			super(itemView);
			title = (TextView) itemView.findViewById(R.id.title);
			icon = (ImageView) itemView.findViewById(R.id.icon);
			description = (TextView) itemView.findViewById(R.id.description);
			fileDataContainer = itemView.findViewById(R.id.file_data_container);
			fileIcon = (ImageView) itemView.findViewById(R.id.file_icon);
			fileTitle = (TextView) itemView.findViewById(R.id.file_title);
			fileDescription = (TextView) itemView.findViewById(R.id.file_description);
			progressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
			secondaryBtnContainer = itemView.findViewById(R.id.secondary_btn_container);
			secondaryBtn = (TextView) itemView.findViewById(R.id.secondary_button);
			buttonsDivider = itemView.findViewById(R.id.buttons_divider);
			primaryBtnContainer = itemView.findViewById(R.id.primary_btn_container);
			primaryButton = (TextView) itemView.findViewById(R.id.primary_button);
		}
	}
}
