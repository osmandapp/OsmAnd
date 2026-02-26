package net.osmand.plus.download.ui;

import static net.osmand.plus.download.DownloadResourceGroupType.NAUTICAL_DEPTH_HEADER;
import static net.osmand.plus.download.DownloadResourceGroupType.NAUTICAL_POINTS_HEADER;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroupType;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.plugins.custom.CustomIndexItem;

import java.util.ArrayList;
import java.util.List;

public class DownloadResourceGroupAdapter extends OsmandBaseExpandableListAdapter {

	private List<DownloadResourceGroup> data = new ArrayList<DownloadResourceGroup>();
	private final DownloadActivity ctx;
	private DownloadResourceGroup mainGroup;


	public DownloadResourceGroupAdapter(DownloadActivity ctx) {
		this.ctx = ctx;
	}

	public void update(DownloadResourceGroup mainGroup) {
		this.mainGroup = mainGroup;
		data = mainGroup.getGroups();
		notifyDataSetChanged();
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		DownloadResourceGroup drg = data.get(groupPosition);
		if (drg.getType().containsIndexItem()) {
			return drg.getItemByIndex(childPosition);
		}
		return drg.getGroupByIndex(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return groupPosition * 10000 + childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
	                         View convertView, ViewGroup parent) {
		Object child = getChild(groupPosition, childPosition);
		if (child instanceof DownloadItem item) {

			if (item instanceof SrtmDownloadItem srtmDownloadItem) {
				updateSRTMMetricSystem(srtmDownloadItem);
			}
			DownloadResourceGroup group = getGroupObj(groupPosition);
			ItemViewHolder viewHolder;
			if (convertView != null && convertView.getTag() instanceof ItemViewHolder) {
				viewHolder = (ItemViewHolder) convertView.getTag();
			} else {
				convertView = LayoutInflater.from(parent.getContext()).inflate(
						R.layout.two_line_with_images_list_item, parent, false);
				viewHolder = new ItemViewHolder(convertView, ctx);
				viewHolder.setShowRemoteDate(true);
				convertView.setTag(viewHolder);
			}
			if (mainGroup.getType() == DownloadResourceGroupType.NAUTICAL_MAPS) {
				// Use short names for nautical depth contours
				// and depth points maps on Nautical maps screen
				DownloadResourceGroup relatedGroup = item.getRelatedGroup();
				DownloadResourceGroupType type = relatedGroup.getType();
				boolean useShortName = type == NAUTICAL_DEPTH_HEADER || type == NAUTICAL_POINTS_HEADER;
				viewHolder.setUseShortName(useShortName);
			}
			if (mainGroup.getType() == DownloadResourceGroupType.REGION &&
					group != null && group.getType() == DownloadResourceGroupType.REGION_MAPS
					&& !(item instanceof CustomIndexItem)) {
				viewHolder.setShowTypeInName(true);
				viewHolder.setShowTypeInDesc(false);
			} else if (group != null && (group.getType() == DownloadResourceGroupType.SRTM_HEADER
					|| group.getType() == DownloadResourceGroupType.HILLSHADE_HEADER)) {
				viewHolder.setShowTypeInName(false);
				viewHolder.setShowTypeInDesc(false);
			} else {
				viewHolder.setShowTypeInDesc(true);
			}
			viewHolder.bindDownloadItem(item);
		} else {
			DownloadResourceGroup group = (DownloadResourceGroup) child;
			DownloadGroupViewHolder viewHolder;
			if (convertView != null && convertView.getTag() instanceof DownloadGroupViewHolder) {
				viewHolder = (DownloadGroupViewHolder) convertView.getTag();
			} else {
				convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_menu_item,
						parent, false);
				viewHolder = new DownloadGroupViewHolder(ctx, convertView);
				convertView.setTag(viewHolder);
			}
			viewHolder.bindItem(group);
		}

		return convertView;
	}

	private void updateSRTMMetricSystem(@NonNull SrtmDownloadItem srtmDownloadItem) {
		srtmDownloadItem.updateMetric(ctx.getMyApplication());
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View v = convertView;
		String section = getGroup(groupPosition);
		if (v == null) {
			LayoutInflater inflater = LayoutInflater.from(ctx);
			v = inflater.inflate(R.layout.download_item_list_section, parent, false);
		}
		TextView nameView = v.findViewById(R.id.title);
		nameView.setText(section);
		v.setOnClickListener(null);
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = ctx.getTheme();
		theme.resolveAttribute(R.attr.activity_background_color, typedValue, true);
		v.setBackgroundColor(typedValue.data);

		return v;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return data.get(groupPosition).size();
	}

	public DownloadResourceGroup getGroupObj(int groupPosition) {
		return data.get(groupPosition);
	}

	@Override
	public String getGroup(int groupPosition) {
		DownloadResourceGroup drg = data.get(groupPosition);
		int rid = drg.getType().getResourceId();
		if (rid != -1) {
			return ctx.getString(rid);
		}
		return "";
	}

	@Override
	public int getGroupCount() {
		return data.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}