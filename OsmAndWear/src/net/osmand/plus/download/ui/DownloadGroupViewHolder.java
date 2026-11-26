package net.osmand.plus.download.ui;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.plugins.custom.CustomRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroupType;
import net.osmand.plus.download.IndexItem;

public class DownloadGroupViewHolder {

	private final DownloadActivity ctx;

	private final TextView textView;

	public DownloadGroupViewHolder(DownloadActivity ctx, View v) {
		this.ctx = ctx;
		textView = v.findViewById(R.id.title);
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
				iconStart = cache.getThemedIcon(R.drawable.ic_world_globe_dark);
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
		int ic = getIconColorForOutdatedItems(group);
		if (ic != 0) {
			return ctx.getMyApplication().getUIUtilities().getIcon(iconId, ic);
		}
		return null;
	}

	private int getIconColorForOutdatedItems(DownloadResourceGroup group) {
		int clr = 0;
		if (group.getIndividualResources() != null) {
			for (IndexItem ii : group.getIndividualResources()) {
				if (ii.getType() == DownloadActivityType.NORMAL_FILE
						|| ii.getType() == DownloadActivityType.ROADS_FILE) {
					if (ii.isOutdated()) {
						return R.color.color_distance;
					} else if(ii.isDownloaded()) {
						clr = R.color.color_ok;
					}
				}
			}
		}

		if (group.getGroups() != null) {
			for (DownloadResourceGroup g : group.getGroups()) {
				int d = getIconColorForOutdatedItems(g);
				if (d == R.color.color_distance) {
					return d;
				} else if(d != 0) {
					clr = d;
				}
			}
		}
		return clr;
	}

	public void bindItem(DownloadResourceGroup group) {
		String name = group.getName(ctx);
		textView.setText(name);
		Drawable iconStart = getIconForGroup(group);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(textView, iconStart, null, null, null);
	}
}