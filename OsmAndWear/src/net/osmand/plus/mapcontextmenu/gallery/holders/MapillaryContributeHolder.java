package net.osmand.plus.mapcontextmenu.gallery.holders;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class MapillaryContributeHolder extends RecyclerView.ViewHolder {

	private final View itemView;

	public MapillaryContributeHolder(@NonNull View itemView) {
		super(itemView);
		this.itemView = itemView;
	}

	public void bindView(boolean nightMode, MapActivity mapActivity) {
		itemView.findViewById(R.id.card_background).setVisibility(View.GONE);
		AndroidUtils.setBackgroundColor(mapActivity, itemView, ColorUtilities.getActivityBgColorId(nightMode));
		AndroidUtils.setTextPrimaryColor(mapActivity, itemView.findViewById(R.id.title), nightMode);
		itemView.findViewById(R.id.button).setOnClickListener(v -> MapillaryPlugin.openMapillary(mapActivity, null));
	}
}
