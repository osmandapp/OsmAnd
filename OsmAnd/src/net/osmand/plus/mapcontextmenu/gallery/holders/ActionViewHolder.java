package net.osmand.plus.mapcontextmenu.gallery.holders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.gallery.GalleryItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import org.apache.commons.logging.Log;

public class ActionViewHolder extends RecyclerView.ViewHolder {

	private static final Log LOG = PlatformUtil.getLog(ActionViewHolder.class);

	private final View itemView;

	public ActionViewHolder(@NonNull View itemView) {
		super(itemView);
		this.itemView = itemView;
	}

	public void bindView(boolean nightMode,
	                     @NonNull MapActivity mapActivity,
	                     @NonNull GalleryItem.Action action) {
		itemView.findViewById(R.id.card_background).setVisibility(View.GONE);
		AndroidUtils.setBackgroundColor(mapActivity, itemView, ColorUtilities.getActivityBgColorId(nightMode));
		AndroidUtils.setTextPrimaryColor(mapActivity, itemView.findViewById(R.id.title), nightMode);
		itemView.findViewById(R.id.button).setOnClickListener(v -> handleAction(action));
	}

	private void handleAction(@NonNull GalleryItem.Action action) {
		if (!PluginsHelper.handleGalleryAction(action)) {
			LOG.warn("Unhandled gallery action: " + action.getId());
		}
	}
}