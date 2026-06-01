package net.osmand.plus.gallery.ui.holders;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.gallery.model.GalleryAction;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.ColorUtilities;

import org.apache.commons.logging.Log;

public class NoMediaHolder extends RecyclerView.ViewHolder {

	private static final Log LOG = PlatformUtil.getLog(NoMediaHolder.class);

	private final OsmandApplication app;
	private final ImageView imageView;
	private final TextView titleView;
	private final TextView descriptionView;
	private final View actionButton;

	public NoMediaHolder(@NonNull View itemView, @NonNull OsmandApplication app) {
		super(itemView);
		this.imageView = itemView.findViewById(R.id.icon);
		this.titleView = itemView.findViewById(R.id.title);
		this.descriptionView = itemView.findViewById(R.id.description);
		this.actionButton = itemView.findViewById(R.id.no_media_action_button);
		this.app = app;
	}

	public void bindView(boolean nightMode, @NonNull GalleryItem.NoMedia item) {
		Drawable icon = app.getUIUtilities().getPaintedIcon(
				item.getIconResId(),
				ColorUtilities.getDefaultIconColor(app, nightMode)
		);
		imageView.setImageDrawable(icon);
		titleView.setText(item.getTitleResId());
		descriptionView.setText(item.getDescriptionResId());
		bindAction(item.getAction());
	}

	private void bindAction(@Nullable GalleryAction action) {
		AndroidUiHelper.updateVisibility(actionButton, action != null);

		if (action != null) {
			actionButton.setOnClickListener(v -> handleAction(action));
		} else {
			actionButton.setOnClickListener(null);
		}
	}

	private void handleAction(@NonNull GalleryAction action) {
		if (!PluginsHelper.handleGalleryAction(action)) {
			LOG.warn("Unhandled gallery action: " + action.getId());
		}
	}
}