package net.osmand.plus.mapcontextmenu.gallery.holders;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.utils.ColorUtilities;

public class NoImagesHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final ImageView imageView;
	private final View mapillaryAddPhotos;

	public NoImagesHolder(@NonNull View itemView, @NonNull OsmandApplication app) {
		super(itemView);
		this.imageView = itemView.findViewById(R.id.icon);
		this.mapillaryAddPhotos = itemView.findViewById(R.id.mapillary_add_photos);
		this.app = app;
	}

	public void bindView(boolean nightMode, @NonNull MapActivity mapActivity, boolean isOnlinePhotos) {
		Drawable icon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_desert, ColorUtilities.getDefaultIconColor(app, nightMode));
		imageView.setImageDrawable(icon);

		AndroidUiHelper.updateVisibility(mapillaryAddPhotos, !isOnlinePhotos);
		mapillaryAddPhotos.setOnClickListener(v -> {
			MapillaryPlugin.openMapillary(mapActivity, null);
		});
	}
}
