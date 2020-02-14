package net.osmand.plus.settings;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.map.ITileSource;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.render.RenderingIcons;


import java.io.File;
import java.util.List;

public class DuplicatesSettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 0;
	private static final int ITEM_TYPE = 1;

	private boolean nightMode;
	private OsmandApplication app;
	private List<? super Object> items;

	DuplicatesSettingsAdapter(OsmandApplication app, List<? super Object> items, boolean nightMode) {
		this.app = app;
		this.items = items;
		this.nightMode = nightMode;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(app);
		if (viewType == HEADER_TYPE) {
			View view = inflater.inflate(R.layout.list_item_header_import, parent, false);
			return new HeaderViewHolder(view);
		} else {
			View view = inflater.inflate(R.layout.list_item_import, parent, false);
			return new ItemViewHolder(view);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object currentItem = items.get(position);
		if (holder instanceof HeaderViewHolder) {
			((HeaderViewHolder) holder).title.setText((String) currentItem);
			((HeaderViewHolder) holder).subTitle.setText(String.format(
					app.getString(R.string.listed_exist),
					(String) currentItem));
			((HeaderViewHolder) holder).divider.setVisibility(View.VISIBLE);
		} else if (holder instanceof ItemViewHolder) {
			String title = null;
			String subTitle = null;
			Drawable drawable = null;
			if (currentItem instanceof ApplicationMode) {
				title = ((ApplicationMode) currentItem).toHumanString();
				subTitle = ((ApplicationMode) currentItem).getRoutingProfile();
				drawable = app.getUIUtilities().getIcon(
						((ApplicationMode) currentItem).getIconRes(),
						((ApplicationMode) currentItem).getIconColorInfo().getColor(nightMode)
				);
			} else if (currentItem instanceof QuickAction) {
				title = ((QuickAction) currentItem).getName(app);
				drawable = app.getUIUtilities().getIcon(((QuickAction) currentItem).getIconRes(), nightMode);
			} else if (currentItem instanceof PoiUIFilter) {
				title = ((PoiUIFilter) currentItem).getName();
				int iconRes = RenderingIcons.getBigIconResourceId(((PoiUIFilter) currentItem).getIconId());
				drawable = app.getUIUtilities().getIcon(iconRes != 0 ? iconRes : R.drawable.ic_person, nightMode);
			} else if (currentItem instanceof ITileSource) {
				title = ((ITileSource) currentItem).getName();
				drawable = app.getUIUtilities().getIcon(R.drawable.ic_action_info_dark, nightMode);
			} else if (currentItem instanceof File) {
				title = ((File) currentItem).getName();
				if (((File) currentItem).getName().contains("/rendering/")) {
					drawable = app.getUIUtilities().getIcon(R.drawable.ic_action_map_style, nightMode);
				}
			}
			((ItemViewHolder) holder).title.setText(title != null ? title : "");
			if (subTitle != null) {
				((ItemViewHolder) holder).subTitle.setText(subTitle);
				((ItemViewHolder) holder).subTitle.setVisibility(View.VISIBLE);
			} else {
				((ItemViewHolder) holder).subTitle.setVisibility(View.GONE);
			}
			if (drawable != null) {
				((ItemViewHolder) holder).icon.setImageDrawable(drawable);
				((ItemViewHolder) holder).icon.setImageResource(View.VISIBLE);
			} else {
				((ItemViewHolder) holder).icon.setImageResource(R.drawable.ic_action_info_dark);
				((ItemViewHolder) holder).icon.setVisibility(View.GONE);
			}
			((ItemViewHolder) holder).divider.setVisibility(shouldShowDivider(position) ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		if (items.get(position) instanceof String) {
			return HEADER_TYPE;
		} else {
			return ITEM_TYPE;
		}
	}

	private class HeaderViewHolder extends RecyclerView.ViewHolder {
		TextView title;
		TextView subTitle;
		View divider;

		HeaderViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			subTitle = itemView.findViewById(R.id.sub_title);
			divider = itemView.findViewById(R.id.top_divider);
		}
	}

	private class ItemViewHolder extends RecyclerView.ViewHolder {
		TextView title;
		TextView subTitle;
		ImageView icon;
		View divider;

		ItemViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			subTitle = itemView.findViewById(R.id.sub_title);
			icon = itemView.findViewById(R.id.icon);
			divider = itemView.findViewById(R.id.bottom_divider);
		}
	}

	private boolean shouldShowDivider(int position) {
		boolean isLast = position == items.size() - 1;
		if (isLast) {
			return true;
		} else {
			Object next = items.get(position + 1);
			return next instanceof String;
		}
	}
}
