package net.osmand.plus.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.map.ITileSource;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.profiles.AdditionalDataWrapper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.render.RenderingIcons;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ExportImportSettingsAdapter extends OsmandBaseExpandableListAdapter {

	private OsmandApplication app;

	private List<? super Object> settingsToOperate;

	private List<AdditionalDataWrapper> settingsList;

	private boolean nightMode;

	private boolean importState;

	ExportImportSettingsAdapter(OsmandApplication app, List<AdditionalDataWrapper> settingsList, boolean nightMode, boolean importState) {
		this.app = app;
		this.settingsList = settingsList;
		this.nightMode = nightMode;
		this.importState = importState;
		this.settingsToOperate = new ArrayList<>();
	}

	public void updateSettingsList(List<AdditionalDataWrapper> settingsList) {
		this.settingsList = settingsList;
		notifyDataSetChanged();
	}

	public List<? super Object> getSettingsToOperate() {
		return this.settingsToOperate;
	}

	public void selectAll(boolean selectAll) {
		settingsToOperate.clear();
		if (selectAll) {
			for (AdditionalDataWrapper item : settingsList) {
				settingsToOperate.addAll(item.getItems());
			}
		}
		notifyDataSetChanged();
	}

	@Override
	public int getGroupCount() {
		return settingsList.size();
	}

	@Override
	public int getChildrenCount(int i) {
		return settingsList.get(i).getItems().size();
	}

	@Override
	public Object getGroup(int i) {
		return settingsList.get(i);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return settingsList.get(groupPosition).getItems().get(childPosition);
	}

	@Override
	public long getGroupId(int i) {
		return i;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return groupPosition * 10000 + childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View group = convertView;
		if (group == null) {
//			LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
			LayoutInflater inflater = LayoutInflater.from(app);
			group = inflater.inflate(R.layout.profile_data_list_item_group, parent, false);
		}

		boolean isLastGroup = groupPosition == getGroupCount() - 1;
		final AdditionalDataWrapper.Type type = settingsList.get(groupPosition).getType();

		TextView titleTv = group.findViewById(R.id.title_tv);
		TextView subTextTv = group.findViewById(R.id.sub_text_tv);
		final CheckBox checkBox = group.findViewById(R.id.check_box);
		ImageView expandIv = group.findViewById(R.id.explist_indicator);
		View divider = group.findViewById(R.id.divider);
		View cardTopDivider = group.findViewById(R.id.card_top_divider);
		View cardBottomDivider = group.findViewById(R.id.card_bottom_divider);

		titleTv.setText(getGroupTitle(type));
		cardTopDivider.setVisibility(importState ? View.VISIBLE : View.GONE);
		if (importState) {
			cardBottomDivider.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
			divider.setVisibility(View.GONE);
		} else {
			cardBottomDivider.setVisibility(View.GONE);
			divider.setVisibility(isExpanded || isLastGroup ? View.GONE : View.VISIBLE);
		}
//		CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, profileColor)));

		final List<?> listItems = settingsList.get(groupPosition).getItems();
		subTextTv.setText(String.valueOf(listItems.size()));

		checkBox.setChecked(settingsToOperate.containsAll(listItems));
		checkBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				if (checkBox.isChecked()) {
					for (Object object : listItems) {
						if (!settingsToOperate.contains(object)) {
							settingsToOperate.add(object);
						}
					}
				} else {
					settingsToOperate.removeAll(listItems);
				}
				notifyDataSetInvalidated();
			}
		});

		adjustIndicator(app, groupPosition, isExpanded, group, true);

		return group;
	}

	@Override
	public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View child = convertView;
		if (child == null) {
//			LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
			LayoutInflater inflater = LayoutInflater.from(app);
			child = inflater.inflate(R.layout.profile_data_list_item_child, parent, false);
		}
		final Object currentItem = settingsList.get(groupPosition).getItems().get(childPosition);


		boolean isLastGroup = groupPosition == getGroupCount() - 1;
		final AdditionalDataWrapper.Type type = settingsList.get(groupPosition).getType();

		TextView title = child.findViewById(R.id.title_tv);
//		TextView subText = child.findViewById(R.id.sub_text_tv);
		final CheckBox checkBox = child.findViewById(R.id.check_box);
		ImageView icon = child.findViewById(R.id.icon);
		View divider = child.findViewById(R.id.divider);
		View cardBottomDivider = child.findViewById(R.id.card_bottom_divider);

		divider.setVisibility(isLastChild && !isLastGroup && !importState ? View.VISIBLE : View.GONE);
		cardBottomDivider.setVisibility(isLastChild && importState ? View.VISIBLE : View.GONE);
//		CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, profileColor)));

		checkBox.setChecked(settingsToOperate.contains(currentItem));
		checkBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (checkBox.isChecked()) {
					settingsToOperate.add(currentItem);
				} else {
					settingsToOperate.remove(currentItem);
				}
				notifyDataSetInvalidated();
			}
		});

		switch (type) {
			case PROFILE:
				title.setText(((ApplicationMode) currentItem).toHumanString());
//				subText.setText(((ApplicationMode) currentItem));
				icon.setVisibility(View.INVISIBLE);
				icon.setImageResource(((ApplicationMode) currentItem).getIconRes());
				break;
			case QUICK_ACTIONS:
				title.setText(((QuickAction) currentItem).getName(app.getApplicationContext()));
				icon.setVisibility(View.INVISIBLE);
				icon.setImageResource(R.drawable.ic_action_info_dark);
				break;
			case POI_TYPES:
				title.setText(((PoiUIFilter) currentItem).getName());
				icon.setVisibility(View.VISIBLE);
				int iconRes = RenderingIcons.getBigIconResourceId(((PoiUIFilter) currentItem).getIconId());
//				icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes != 0 ? iconRes : R.drawable.ic_person, profileColor));
				break;
			case MAP_SOURCES:
				title.setText(((ITileSource) currentItem).getName());
				icon.setVisibility(View.INVISIBLE);
				icon.setImageResource(R.drawable.ic_action_info_dark);
				break;
			case CUSTOM_RENDER_STYLE:
				String renderName = ((File) currentItem).getName();
				renderName = renderName.replace('_', ' ').replaceAll(".render.xml", "");
				title.setText(renderName);
				icon.setVisibility(View.INVISIBLE);
				icon.setImageResource(R.drawable.ic_action_info_dark);
				break;
			case CUSTOM_ROUTING:
				String routingName = ((File) currentItem).getName();
				routingName = routingName.replace('_', ' ').replaceAll(".xml", "");
				title.setText(routingName);
				icon.setVisibility(View.INVISIBLE);
				icon.setImageResource(R.drawable.ic_action_info_dark);
				break;
			default:
				return child;
		}
		return child;
	}

	@Override
	public boolean isChildSelectable(int i, int i1) {
		return false;
	}

	private int getGroupTitle(AdditionalDataWrapper.Type type) {
		switch (type) {
			case PROFILE:
				return R.string.shared_sting_profiles;
			case QUICK_ACTIONS:
				return R.string.configure_screen_quick_action;
			case POI_TYPES:
				return R.string.poi_dialog_poi_type;
			case MAP_SOURCES:
				return R.string.quick_action_map_source_title;
			case CUSTOM_RENDER_STYLE:
				return R.string.shared_string_custom_rendering_style;
			case CUSTOM_ROUTING:
				return R.string.shared_string_routing;
			default:
				return R.string.access_empty_list;
		}
	}
}

