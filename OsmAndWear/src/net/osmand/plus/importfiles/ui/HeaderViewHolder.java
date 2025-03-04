package net.osmand.plus.importfiles.ui;


import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.importfiles.ui.ImportTracksAdapter.ImportTracksListener;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

class HeaderViewHolder extends ViewHolder {

	final ImportTracksListener listener;

	final TextView title;
	final View importAsOneTrackButton;

	HeaderViewHolder(@NonNull View itemView, @Nullable ImportTracksListener listener) {
		super(itemView);
		this.listener = listener;

		title = itemView.findViewById(R.id.title);
		importAsOneTrackButton = itemView.findViewById(R.id.import_as_one_track);
	}

	public void bindView(@NonNull OsmandApplication app, @NonNull String fileName, int tracksCount, boolean nightMode) {
		String size = String.valueOf(tracksCount);
		String description = app.getString(R.string.import_tracks_descr, fileName, size);

		int index = description.indexOf(fileName);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);

		SpannableString spannable = new SpannableString(description);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), index, index + fileName.length(), 0);
		spannable.setSpan(new ForegroundColorSpan(activeColor), index, index + fileName.length(), 0);

		index = description.lastIndexOf(size);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), index, index + size.length(), 0);
		title.setText(spannable);

		importAsOneTrackButton.setOnClickListener(v -> {
			if (listener != null) {
				listener.onImportAsOneTrackClicked();
			}
		});
	}
}
