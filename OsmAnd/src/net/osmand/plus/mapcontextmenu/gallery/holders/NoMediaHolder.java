package net.osmand.plus.mapcontextmenu.gallery.holders;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.gallery.GalleryAction;
import net.osmand.plus.gallery.GalleryItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.ColorUtilities;

import org.apache.commons.logging.Log;

public class NoMediaHolder extends RecyclerView.ViewHolder {

	private static final Log LOG = PlatformUtil.getLog(NoMediaHolder.class);

	private final OsmandApplication app;
	private final ImageView imageView;
	private final View actionButton;

	public NoMediaHolder(@NonNull View itemView, @NonNull OsmandApplication app) {
		super(itemView);
		this.imageView = itemView.findViewById(R.id.icon);
		this.actionButton = itemView.findViewById(R.id.no_media_action_button);
		this.app = app;
	}

	public void bindView(boolean nightMode, @NonNull GalleryItem.NoMedia item) {
		Drawable icon = app.getUIUtilities().getPaintedIcon(
				R.drawable.ic_action_desert,
				ColorUtilities.getDefaultIconColor(app, nightMode)
		);
		imageView.setImageDrawable(icon);
		bindAction(item.getAction());
	}

	private void bindAction(@Nullable GalleryAction action) {
		AndroidUiHelper.updateVisibility(actionButton, action != null);

		if (action != null) {
			actionButton.setOnClickListener(v -> {
				if (!PluginsHelper.handleGalleryAction(action)) {
					LOG.warn("Unhandled gallery action: " + action.getId());
				}
			});
		} else {
			actionButton.setOnClickListener(null);
		}
	}
}