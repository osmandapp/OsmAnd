package net.osmand.plus.download.ui;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import net.osmand.AndroidUtils;
import net.osmand.plus.CustomRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType;
import net.osmand.plus.download.IndexItem;

public class DownloadGroupViewHolder {

	private DownloadActivity ctx;

	private TextView textView;

	public DownloadGroupViewHolder(DownloadActivity ctx, View v) {
		this.ctx = ctx;
		textView = (TextView) v.findViewById(R.id.title);
	}

	private boolean isParentWorld(DownloadResourceGroup group) {
		return group.getParentGroup() == null
				|| group.getParentGroup().getType() == DownloadResourceGroupType.WORLD;
	}

	private Drawable getIconForGroup(DownloadResourceGroup group) {
		Drawable iconStart;
		OsmandApplication app = ctx.getMyApplication();
		UiUtilities cache = app.getUIUtilities();
		if (group.getType() == DownloadResourceGroupType.VOICE_REC
				|| group.getType() == DownloadResourceGroupType.VOICE_TTS) {
			iconStart = cache.getThemedIcon(R.drawable.ic_action_volume_up);
		} else if (group.getType() == DownloadResourceGroupType.FONTS) {
			iconStart = cache.getThemedIcon(R.drawable.ic_action_map_language);
		} else {
			if (group.getRegion() instanceof CustomRegion) {
				String iconName = ((CustomRegion) group.getRegion()).getIconName(ctx);
				int iconId = AndroidUtils.getDrawableId(app, iconName);
				if (iconId != 0) {
					iconStart = getIconForDownloadedItems(group, iconId);
					return iconStart != null ? iconStart : cache.getThemedIcon(iconId);
				}
			}
			if (isParentWorld(group) || isParentWorld(group.getParentGroup())) {
				iconStart = getIconForDownloadedItems(group, R.drawable.ic_world_globe_dark);
				if (iconStart == null) {
					iconStart = cache.getThemedIcon(R.drawable.ic_world_globe_dark);
				}
			} else {
				iconStart = getIconForDownloadedItems(group, R.drawable.ic_map);
				if (iconStart == null) {
					iconStart = cache.getThemedIcon(R.drawable.ic_map);
				}
			}
		}
		return iconStart;
	}

	private Drawable getIconForDownloadedItems(DownloadResourceGroup group, @DrawableRes int iconId) {
		DownloadResourceGroup ggr = group.getSubGroupById(DownloadResourceGroupType.REGION_MAPS.getDefaultId());
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
				int color = item.isOutdated() ? R.color.color_distance : R.color.color_ok;
				return ctx.getMyApplication().getUIUtilities().getIcon(iconId, color);
			}
		}
		return getIconForOutdatedItems(group, iconId);
	}

	private Drawable getIconForOutdatedItems(DownloadResourceGroup group, @DrawableRes int iconId) {
		if (group.getIndividualResources() != null) {
			for (IndexItem ii : group.getIndividualResources()) {
				if (ii.getType() == DownloadActivityType.NORMAL_FILE
						|| ii.getType() == DownloadActivityType.ROADS_FILE) {
					if (ii.isOutdated()) {
						return ctx.getMyApplication().getUIUtilities().getIcon(iconId, R.color.color_distance);
					}
				}
			}
		}
		if (group.getGroups() != null) {
			for (DownloadResourceGroup g : group.getGroups()) {
				Drawable d = getIconForOutdatedItems(g, iconId);
				if (d != null) {
					return d;
				}
			}
		}
		return null;
	}

	public void bindItem(DownloadResourceGroup group) {
		String name = group.getName(ctx);
		textView.setText(name);
		Drawable iconStart = getIconForGroup(group);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(textView, iconStart, null, null, null);
	}
}