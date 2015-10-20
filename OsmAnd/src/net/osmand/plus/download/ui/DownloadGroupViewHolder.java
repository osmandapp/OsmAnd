package net.osmand.plus.download.ui;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.IndexItem;

public class DownloadGroupViewHolder {
	TextView textView;
	private DownloadActivity ctx;

	public DownloadGroupViewHolder(DownloadActivity ctx, View v) {
		this.ctx = ctx;
		textView = (TextView) v.findViewById(R.id.title);
	}

	private boolean isParentWorld(DownloadResourceGroup group) {
		return group.getParentGroup() == null
				|| group.getParentGroup().getType() == DownloadResourceGroup.DownloadResourceGroupType.WORLD;
	}

	private Drawable getIconForGroup(DownloadResourceGroup group) {
		Drawable iconLeft;
		if (group.getType() == DownloadResourceGroup.DownloadResourceGroupType.VOICE_REC
				|| group.getType() == DownloadResourceGroup.DownloadResourceGroupType.VOICE_TTS) {
			iconLeft = ctx.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_volume_up);
		} else {
			IconsCache cache = ctx.getMyApplication().getIconsCache();
			if (isParentWorld(group) || isParentWorld(group.getParentGroup())) {
				iconLeft = cache.getContentIcon(R.drawable.ic_world_globe_dark);
			} else {
				DownloadResourceGroup ggr = group
						.getSubGroupById(DownloadResourceGroup.DownloadResourceGroupType.REGION_MAPS.getDefaultId());
				iconLeft = cache.getContentIcon(R.drawable.ic_map);
				if (ggr != null && ggr.getIndividualResources() != null) {
					IndexItem item = null;
					for (IndexItem ii : ggr.getIndividualResources()) {
						if (ii.getType() == DownloadActivityType.NORMAL_FILE
								|| ii.getType() == DownloadActivityType.ROADS_FILE) {
							if (ii.isDownloaded() || ii.isOutdated()) {
								item = ii;
								break;
							}
						}
					}
					if (item != null) {
						if (item.isOutdated()) {
							iconLeft = cache.getIcon(R.drawable.ic_map, R.color.color_distance);
						} else {
							iconLeft = cache.getIcon(R.drawable.ic_map, R.color.color_ok);
						}
					}
				}
			}
		}
		return iconLeft;
	}

	public void bindItem(DownloadResourceGroup group) {
		Drawable iconLeft = getIconForGroup(group);
		textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
		String name = group.getName(ctx);
		textView.setText(name);
	}
}
