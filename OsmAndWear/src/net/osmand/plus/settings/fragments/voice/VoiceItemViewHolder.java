package net.osmand.plus.settings.fragments.voice;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.fragments.voice.VoiceItemsAdapter.VoiceItemsListener;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.text.DateFormat;

class VoiceItemViewHolder extends RecyclerView.ViewHolder {

	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.DEFAULT);

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final DownloadIndexesThread downloadThread;

	private final TextView title;
	private final TextView description;
	private final ProgressBar progressBar;
	private final ImageView secondaryIcon;
	private final CompoundButton compoundButton;
	private final View mainContainer;

	VoiceItemViewHolder(@NonNull View itemView) {
		super(itemView);
		this.app = (OsmandApplication) itemView.getContext().getApplicationContext();
		this.uiUtilities = app.getUIUtilities();
		this.downloadThread = app.getDownloadThread();

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		progressBar = itemView.findViewById(R.id.ProgressBar);
		secondaryIcon = itemView.findViewById(R.id.secondary_icon);
		compoundButton = itemView.findViewById(R.id.compound_button);
		mainContainer = itemView.findViewById(R.id.main_container);
	}

	public void bindView(@NonNull IndexItem item, @NonNull VoiceItemsListener listener, boolean nightMode) {
		boolean tts = DownloadActivityType.isVoiceTTS(item);
		boolean defaultTTS = DownloadActivityType.isDefaultVoiceTTS(app, item);

		title.setText(defaultTTS ? app.getString(R.string.use_system_language) : item.getVisibleName(app, app.getRegions(), false));
		description.setText(getVoiceIndexDescription(item));

		itemView.setOnClickListener(v -> listener.onItemClicked(item));

		compoundButton.setChecked(listener.isItemSelected(item));
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), compoundButton);

		int heightId = tts && !defaultTTS ? R.dimen.bottom_sheet_list_item_height : R.dimen.setting_list_item_large_height;
		mainContainer.setMinimumHeight(app.getResources().getDimensionPixelSize(heightId));

		boolean downloaded = item.isDownloaded();
		if (tts || downloaded) {
			AndroidUiHelper.updateVisibility(compoundButton, true);
			AndroidUiHelper.updateVisibility(progressBar, false);
			AndroidUiHelper.updateVisibility(secondaryIcon, false);
			AndroidUiHelper.updateVisibility(description, !tts || defaultTTS);
		} else {
			boolean downloading = downloadThread.isDownloading(item);
			boolean currentDownloading = downloadThread.isCurrentDownloading(item);

			if (currentDownloading) {
				progressBar.setProgress((int) downloadThread.getCurrentDownloadProgress());
			}
			progressBar.setIndeterminate(!currentDownloading);

			int iconId = downloading ? R.drawable.ic_action_remove_dark : R.drawable.ic_action_gsave_dark;
			secondaryIcon.setImageDrawable(uiUtilities.getActiveIcon(iconId, nightMode));

			AndroidUiHelper.updateVisibility(secondaryIcon, true);
			AndroidUiHelper.updateVisibility(compoundButton, false);
			AndroidUiHelper.updateVisibility(progressBar, downloading);
			AndroidUiHelper.updateVisibility(description, !downloading);
		}
	}

	@NonNull
	private String getVoiceIndexDescription(@NonNull DownloadItem item) {
		if (DownloadActivityType.isVoiceTTS(item)) {
			return DownloadActivityType.isDefaultVoiceTTS(app, item) ? item.getVisibleName(app, app.getRegions(), false) : "";
		} else {
			String dateModified = item.getDate(DATE_FORMAT, true);
			String size = item.getSizeToDownloadInMb() == 0.0 ? null : item.getSizeDescription(app);
			return Algorithms.isEmpty(size) ? dateModified : app.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, dateModified);
		}
	}
}
