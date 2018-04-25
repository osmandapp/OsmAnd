package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class TravelDownloadUpdateCard extends BaseTravelCard {

	public static final int TYPE = 50;

	private boolean download;
	private boolean loadingInProgress;

	public TravelDownloadUpdateCard(OsmandApplication app, boolean nightMode, boolean download) {
		super(app, nightMode);
		this.download = download;
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof DownloadUpdateVH) {
			DownloadUpdateVH holder = (DownloadUpdateVH) viewHolder;
			holder.title.setText(getTitle());
			holder.icon.setImageDrawable(getIcon());
			holder.description.setText(getDescription());
			holder.fileIcon.setImageDrawable(getFileIcon());
			holder.fileTitle.setText(getFileTitle());
			holder.fileDescription.setText(getFileDescription());
			boolean primaryBtnVisible = updatePrimaryButton(holder.primaryButton);
			boolean secondaryBtnVisible = updateSecondaryButton(holder.secondaryBtn);
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
		return "Some file"; // TODO
	}

	@NonNull
	private String getFileDescription() {
		return "Some description"; // TODO
	}

	private Drawable getFileIcon() {
		return getActiveIcon(R.drawable.ic_action_read_article);
	}

	/**
	 * @return true if button is visible, false otherwise.
	 */
	private boolean updateSecondaryButton(TextView btn) {
		if (loadingInProgress || !download) {
			btn.setText(loadingInProgress ? R.string.shared_string_cancel : R.string.later);
			btn.setVisibility(View.VISIBLE);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onSecondaryBtnClick();
				}
			});
			return true;
		}
		btn.setVisibility(View.GONE);
		return false;
	}

	/**
	 * @return true if button is visible, false otherwise.
	 */
	private boolean updatePrimaryButton(TextView btn) {
		if (!loadingInProgress) {
			btn.setText(download ? R.string.shared_string_download : R.string.shared_string_update);
			btn.setVisibility(View.VISIBLE);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onPrimaryBtnClick();
				}
			});
			return true;
		}
		btn.setVisibility(View.GONE);
		return false;
	}

	private void onSecondaryBtnClick() {
		Toast.makeText(app, "Secondary button", Toast.LENGTH_SHORT).show();
	}

	private void onPrimaryBtnClick() {
		Toast.makeText(app, "Primary button", Toast.LENGTH_SHORT).show();
	}

	public static class DownloadUpdateVH extends RecyclerView.ViewHolder {

		final TextView title;
		final ImageView icon;
		final TextView description;
		final ImageView fileIcon;
		final TextView fileTitle;
		final TextView fileDescription;
		final ProgressBar progressBar;
		final TextView secondaryBtn;
		final View buttonsDivider;
		final TextView primaryButton;

		@SuppressWarnings("RedundantCast")
		public DownloadUpdateVH(View itemView) {
			super(itemView);
			title = (TextView) itemView.findViewById(R.id.title);
			icon = (ImageView) itemView.findViewById(R.id.icon);
			description = (TextView) itemView.findViewById(R.id.description);
			fileIcon = (ImageView) itemView.findViewById(R.id.file_icon);
			fileTitle = (TextView) itemView.findViewById(R.id.file_title);
			fileDescription = (TextView) itemView.findViewById(R.id.file_description);
			progressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
			secondaryBtn = (TextView) itemView.findViewById(R.id.secondary_button);
			buttonsDivider = itemView.findViewById(R.id.buttons_divider);
			primaryButton = (TextView) itemView.findViewById(R.id.primary_button);
		}
	}
}
