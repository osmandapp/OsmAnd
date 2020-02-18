package net.osmand.plus.settings;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.map.ITileSource;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;


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
			if (currentItem instanceof ApplicationMode.ApplicationModeBean) {
				String profileName = ((ApplicationMode.ApplicationModeBean) currentItem).userProfileName;
				if (Algorithms.isEmpty(profileName)) {
					ApplicationMode appMode = ApplicationMode.valueOfStringKey(((ApplicationMode.ApplicationModeBean) currentItem).stringKey, null);
					profileName = app.getString(appMode.getNameKeyResource());
				}
				((ItemViewHolder) holder).title.setText(profileName);
				String routingProfile = (((ApplicationMode.ApplicationModeBean) currentItem).routingProfile);
				if (Algorithms.isEmpty(routingProfile)) {
					((ItemViewHolder) holder).subTitle.setVisibility(View.GONE);
				} else {
					((ItemViewHolder) holder).subTitle.setText(String.format(
							app.getString(R.string.ltr_or_rtl_combine_via_colon),
							app.getString(R.string.nav_type_hint),
							routingProfile));
					((ItemViewHolder) holder).subTitle.setVisibility(View.VISIBLE);
				}
				int profileIconRes = AndroidUtils.getDrawableId(app, ((ApplicationMode.ApplicationModeBean) currentItem).iconName);
				ProfileIconColors iconColor = ((ApplicationMode.ApplicationModeBean) currentItem).iconColor;
				((ItemViewHolder) holder).icon.setImageDrawable(app.getUIUtilities().getIcon(profileIconRes, iconColor.getColor(nightMode)));
				((ItemViewHolder) holder).icon.setVisibility(View.VISIBLE);
			} else if (currentItem instanceof QuickAction) {
				((ItemViewHolder) holder).title.setText(((QuickAction) currentItem).getName(app.getApplicationContext()));
				((ItemViewHolder) holder).icon.setImageDrawable(app.getUIUtilities().getIcon(((QuickAction) currentItem).getIconRes(), nightMode));
				((ItemViewHolder) holder).subTitle.setVisibility(View.GONE);
				((ItemViewHolder) holder).icon.setVisibility(View.VISIBLE);
			} else if (currentItem instanceof PoiUIFilter) {
				((ItemViewHolder) holder).title.setText(((PoiUIFilter) currentItem).getName());
				int iconRes = RenderingIcons.getBigIconResourceId(((PoiUIFilter) currentItem).getIconId());
				((ItemViewHolder) holder).icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes != 0 ? iconRes : R.drawable.ic_person, nightMode));
				((ItemViewHolder) holder).subTitle.setVisibility(View.GONE);
				((ItemViewHolder) holder).icon.setVisibility(View.VISIBLE);
			} else if (currentItem instanceof ITileSource) {
				((ItemViewHolder) holder).title.setText(((ITileSource) currentItem).getName());
				((ItemViewHolder) holder).icon.setImageResource(R.drawable.ic_action_info_dark);
				((ItemViewHolder) holder).subTitle.setVisibility(View.GONE);
				((ItemViewHolder) holder).icon.setVisibility(View.INVISIBLE);
			} else if (currentItem instanceof File) {
				((ItemViewHolder) holder).title.setText(((File) currentItem).getName());
				if (((File) currentItem).getName().contains("/rendering/")) {
					((ItemViewHolder) holder).icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_map_style, nightMode));
					((ItemViewHolder) holder).icon.setVisibility(View.VISIBLE);
				} else {
					((ItemViewHolder) holder).icon.setImageResource(R.drawable.ic_action_info_dark);
					((ItemViewHolder) holder).icon.setVisibility(View.INVISIBLE);
				}
				((ItemViewHolder) holder).subTitle.setVisibility(View.GONE);
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
