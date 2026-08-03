package net.osmand.plus.mapcontextmenu.gallery.holders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;

public class ImagesCountHolder extends RecyclerView.ViewHolder {
	private final OsmandApplication app;
	private final TextView countView;

	public ImagesCountHolder(@NonNull View itemView, @NonNull OsmandApplication app) {
		super(itemView);
		this.countView = itemView.findViewById(R.id.images_count);
		this.app = app;
	}

	public void bindView(int imagesCount, boolean nightMode) {
		String count = String.valueOf(imagesCount );
		String sharedItems = app.getString(R.string.shared_string_items);
		countView.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, count, sharedItems));
		countView.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode));
	}
}
