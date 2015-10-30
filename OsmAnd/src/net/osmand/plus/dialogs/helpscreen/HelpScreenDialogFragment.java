package net.osmand.plus.dialogs.helpscreen;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class HelpScreenDialogFragment extends DialogFragment implements ExpandableListView.OnChildClickListener {
	private static final Log LOG = PlatformUtil.getLog(HelpScreenDialogFragment.class);
	final static MenuCategory[] categories = MenuCategory.values();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = (getOsmandApplication())
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@NonNull
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_help_screen, container, false);

		MenuCategory.BEGIN_WITH_OSMAND.initItems(createBeginWithOsmandItems());
		MenuCategory.FEATURES.initItems(createFeaturesItems());
		MenuCategory.PLUGINS.initItems(createPluginsItems());
		MenuCategory.OTHER.initItems(createOtherItems());

		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		final HelpAdapter listAdapter = new HelpAdapter(getOsmandApplication());
		listView.setAdapter(listAdapter);
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
		listView.setOnChildClickListener(this);
		return view;
	}

	private OsmandApplication getOsmandApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
								int childPosition, long id) {
		if (categories[groupPosition] != MenuCategory.HELP_US_TO_IMPROVE &&
				categories[groupPosition].getItem(childPosition).getOnClickListener() != null) {
			categories[groupPosition].getItem(childPosition).getOnClickListener().onClick(v);
		}
		return false;
	}

	public static class HelpAdapter extends OsmandBaseExpandableListAdapter {
		private OsmandApplication ctx;

		public HelpAdapter(OsmandApplication ctx) {
			this.ctx = ctx;
		}

		@Override
		public MyMenuItem getChild(int groupPosition, int childPosition) {
			if (categories[groupPosition] != MenuCategory.HELP_US_TO_IMPROVE) {
				return categories[groupPosition].getItem(childPosition);
			} else {
				return null;
			}
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild,
								 View convertView, ViewGroup parent) {
			LOG.debug("categories[groupPosition]=" + categories[groupPosition]);
			if(categories[groupPosition] == MenuCategory.HELP_US_TO_IMPROVE) {
				convertView = LayoutInflater.from(parent.getContext()).inflate(
						R.layout.help_to_improve_item, parent, false);
				TextView pollButton = (TextView) convertView.findViewById(R.id.pollButton);
				Drawable pollIcon = ctx.getIconsCache().getContentIcon(R.drawable.ic_action_message);
				pollButton.setCompoundDrawablesWithIntrinsicBounds(null, pollIcon, null, null);
				TextView contactUsButton = (TextView) convertView.findViewById(R.id.contactUsButton);
				Drawable contactUsIcon =
						ctx.getIconsCache().getContentIcon(R.drawable.ic_action_message);
				contactUsButton.setCompoundDrawablesWithIntrinsicBounds(null, contactUsIcon, null,
						null);
				return convertView;
			} else {
				final MyMenuItem child = getChild(groupPosition, childPosition);
				MenuItemViewHolder viewHolder;
				if (convertView == null || convertView.getTag() == null) {
					convertView = LayoutInflater.from(parent.getContext()).inflate(
							R.layout.two_line_with_images_list_item, parent, false);
					viewHolder = new MenuItemViewHolder(convertView, ctx);
					convertView.setTag(viewHolder);
				} else {
					viewHolder = (MenuItemViewHolder) convertView.getTag();
				}
				viewHolder.bindMenuItem(child);

				return convertView;
			}
		}


		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, final View convertView, final ViewGroup parent) {
			View v = convertView;
			int titleId = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = LayoutInflater.from(ctx);
				v = inflater.inflate(R.layout.download_item_list_section, parent, false);
			}
			TextView nameView = ((TextView) v.findViewById(R.id.section_name));
			nameView.setText(titleId);
			v.setOnClickListener(null);
			TypedValue typedValue = new TypedValue();
			// TODO optimize
			Resources.Theme theme = ctx.getTheme();
			theme.resolveAttribute(R.attr.ctx_menu_info_view_bg, typedValue, true);
			v.setBackgroundColor(typedValue.data);

			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (categories[groupPosition] != MenuCategory.HELP_US_TO_IMPROVE) {
				return categories[groupPosition].getChildrenCount();
			} else {
				return 1;
			}
		}

		@Override
		public Integer getGroup(int groupPosition) {
			return categories[groupPosition].getTitle();
		}

		@Override
		public int getGroupCount() {
			return MenuCategory.values().length;
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

	private static class MenuItemViewHolder {
		private final TextView nameTextView;
		private final TextView descrTextView;
		private final ImageView leftImageView;
		private final OsmandApplication context;

		public MenuItemViewHolder(View view, OsmandApplication context) {
			this.context = context;
			leftImageView = (ImageView) view.findViewById(R.id.leftImageView);
			descrTextView = (TextView) view.findViewById(R.id.description);
			nameTextView = (TextView) view.findViewById(R.id.name);
		}

		public void bindMenuItem(MyMenuItem menuItem) {
			nameTextView.setText(menuItem.getTitle());
			if (menuItem.getDesription() != null) {
				descrTextView.setVisibility(View.VISIBLE);
				descrTextView.setText(menuItem.getDesription());
			} else {
				descrTextView.setVisibility(View.GONE);
			}
			if (menuItem.getIcon() != -1) {
				leftImageView.setVisibility(View.VISIBLE);
				leftImageView.setImageDrawable(context.getIconsCache()
						.getContentIcon(menuItem.getIcon()));
			} else {
				leftImageView.setVisibility(View.GONE);
			}
		}
	}

	private enum MenuCategory {
		BEGIN_WITH_OSMAND(R.string.begin_with_osmand_menu_group),
		FEATURES(R.string.features_menu_group),
		PLUGINS(R.string.plugins_menu_group),
		HELP_US_TO_IMPROVE(R.string.help_us_to_improve_menu_group),
		OTHER(R.string.other_menu_group);

		private List<MyMenuItem> items;
		@StringRes
		private final int title;

		MenuCategory(int title) {
			this.title = title;
		}

		public int getTitle() {
			return title;
		}

		public int getChildrenCount() {
			return items.size();
		}

		public MyMenuItem getItem(int position) {
			return items.get(position);
		}

		public void initItems(List<MyMenuItem> items) {
			this.items = items;
		}
	}


	private List<MyMenuItem> createBeginWithOsmandItems(){
		ArrayList<MyMenuItem> arrayList = new ArrayList<>();
		MyMenuItem.Builder builder = new MyMenuItem.Builder()
				.setTitle(R.string.first_usage_item)
				.setDescription(R.string.first_usage_item_description, getActivity());
		arrayList.add(builder.create());
		builder = new MyMenuItem.Builder()
				.setTitle(R.string.shared_string_navigation)
				.setDescription(R.string.navigation_item_description, getActivity());
		arrayList.add(builder.create());
		builder = new MyMenuItem.Builder()
				.setTitle(R.string.faq_item)
				.setDescription(R.string.faq_item_description, getActivity());
		arrayList.add(builder.create());
		return arrayList;
	}

	private static List<MyMenuItem> createFeaturesItems() {
		ArrayList<MyMenuItem> arrayList = new ArrayList<>();
		arrayList.add(new MyMenuItem(R.string.map_viewing_item));
		arrayList.add(new MyMenuItem(R.string.search_on_the_map_item));
		arrayList.add(new MyMenuItem(R.string.planning_trip_item));
		return arrayList;
	}

	private static List<MyMenuItem> createPluginsItems() {
		ArrayList<MyMenuItem> arrayList = new ArrayList<>();
		MyMenuItem.Builder builder = new MyMenuItem.Builder()
				.setTitle(R.string.shared_string_online_maps)
				.setIcon(R.drawable.ic_world_globe_dark);
		arrayList.add(builder.create());
		builder.reset()
				.setTitle(R.string.contour_lines_and_hillshade_maps_item)
				.setIcon(R.drawable.ic_plugin_srtm);
		arrayList.add(builder.create());
		builder.reset()
				.setTitle(R.string.trip_recording_tool_item)
				.setIcon(R.drawable.ic_action_polygom_dark);
		arrayList.add(builder.create());
		builder.reset()
				.setTitle(R.string.osmand_ski_maps_item)
				.setIcon(R.drawable.ic_plugin_skimaps);
		arrayList.add(builder.create());
		builder.reset()
				.setTitle(R.string.nautical_charts_item)
				.setIcon(R.drawable.ic_action_sail_boat_dark);
		arrayList.add(builder.create());
		builder.reset()
				.setTitle(R.string.audio_video_note_item)
				.setIcon(R.drawable.ic_action_micro_dark);
		arrayList.add(builder.create());
		builder.reset()
				.setTitle(R.string.osm_editing_item)
				.setIcon(R.drawable.ic_action_bug_dark);
		arrayList.add(builder.create());
		builder.reset()
				.setTitle(R.string.osmand_distance_planning_plugin_name)
				.setIcon(R.drawable.ic_action_ruler);
		arrayList.add(builder.create());
		builder.reset()
				.setTitle(R.string.poi_filter_parking)
				.setIcon(R.drawable.ic_action_parking_dark);
		arrayList.add(builder.create());
		builder.reset()
				.setTitle(R.string.debugging_and_development)
				.setIcon(R.drawable.ic_plugin_developer);
		arrayList.add(builder.create());
		return arrayList;
	}

	private List<MyMenuItem> createOtherItems() {
		ArrayList<MyMenuItem> arrayList = new ArrayList<>();
		arrayList.add(new MyMenuItem(R.string.instalation_troubleshooting_item));
		arrayList.add(new MyMenuItem(R.string.techical_articles_item));
		arrayList.add(new MyMenuItem(R.string.versions_item));
		String version = Version.getBuildAppEdition(getOsmandApplication());
		if (TextUtils.isEmpty(version)) {
			version = Version.getAppVersion(getOsmandApplication());
		}
		MyMenuItem.Builder builder = new MyMenuItem.Builder()
				.setTitle(R.string.shared_string_about)
				.setDescription(version);
		arrayList.add(builder.create());
		return arrayList;
	}
}
