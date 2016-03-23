package net.osmand.plus.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.dialogs.HelpArticleDialogFragment;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;


public class HelpActivity extends OsmandActionBarActivity {

//	public static final String DIALOG = "dialog";
	@IdRes
	public static final int DIALOG = 5;
	final static HelpMenuCategory[] categories = HelpMenuCategory.values();
	public static final String OSMAND_POLL_HTML = "http://osmand.net/android-poll.html";
	//public static final String OSMAND_MAP_LEGEND = "http://osmand.net/help/map-legend_default.png";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		FrameLayout frame = new FrameLayout(this);
		frame.setId(DIALOG);
		setContentView(frame, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		if (savedInstanceState == null) {
			Fragment newFragment = new HelpScreenDialogFragment();
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(DIALOG, newFragment).commit();
		}
		setTitle(R.string.shared_string_help);
		setupHomeButton();
		// if (getSupportFragmentManager().findFragmentByTag(DIALOG) == null) {
		// new HelpScreenDialogFragment().show(getSupportFragmentManager(), DIALOG);
		// }
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
	public static class HelpScreenDialogFragment extends Fragment implements ExpandableListView.OnChildClickListener {
		private static final Log LOG = PlatformUtil.getLog(HelpScreenDialogFragment.class);
		

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}

		@NonNull
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View view = inflater.inflate(R.layout.fragment_help_screen, container, false);

			HelpMenuCategory.BEGIN_WITH_OSMAND.initItems(createBeginWithOsmandItems());
			HelpMenuCategory.FEATURES.initItems(createFeaturesItems());
			HelpMenuCategory.PLUGINS.initItems(createPluginsItems());
			HelpMenuCategory.OTHER.initItems(createOtherItems());

			ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
			final HelpAdapter listAdapter = new HelpAdapter(getActivity());
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
			if (categories[groupPosition] != HelpMenuCategory.HELP_US_TO_IMPROVE &&
					categories[groupPosition].getItem(childPosition).getOnClickListener() != null) {
				LOG.debug("nice");
				categories[groupPosition].getItem(childPosition).getOnClickListener().onClick(v);
			}
			return false;
		}
		


		private List<HelpMenuItem> createBeginWithOsmandItems() {
			ArrayList<HelpMenuItem> arrayList = new ArrayList<>();
			arrayList.add(new HelpMenuItem(R.string.first_usage_item, R.string.first_usage_item_description, -1,
					"feature_articles/start.html", getActivity()));
			arrayList.add(new HelpMenuItem(R.string.shared_string_navigation, R.string.navigation_item_description, -1,
					"feature_articles/navigation.html", getActivity()));
			arrayList.add(new HelpMenuItem(R.string.faq_item, R.string.faq_item_description, -1,
					"feature_articles/faq.html", getActivity()));
			return arrayList;
		}

		private List<HelpMenuItem> createFeaturesItems() {
			ArrayList<HelpMenuItem> arrayList = new ArrayList<>();
			arrayList.add(new HelpMenuItem(R.string.map_viewing_item, -1, -1,
					"feature_articles/map-viewing.html", getActivity()));
			arrayList.add(new HelpMenuItem(R.string.search_on_the_map_item, -1, -1,
					"feature_articles/find-something-on-map.html", getActivity()));
			arrayList.add(new HelpMenuItem(R.string.planning_trip_item, -1, -1,
					"feature_articles/trip-planning.html", getActivity()));
			//final String legendName = getActivity().getString(R.string.map_legend);
			//View.OnClickListener onClickListener = new View.OnClickListener() {
			//	@Override
			//	public void onClick(View v) {
			//		HelpArticleDialogFragment.instantiateWithUrl(OSMAND_MAP_LEGEND, legendName)
			//				.show(getFragmentManager(), null);
			//	}
			//};
			//arrayList.add(new HelpMenuItem(legendName, getActivity().getString(R.string.map_legend_item_description), onClickListener));
			arrayList.add(new HelpMenuItem(R.string.map_legend, -1, -1,
					"feature_articles/map-legend.html", getActivity()));
			return arrayList;
		}

		private List<HelpMenuItem> createPluginsItems() {
			ArrayList<HelpMenuItem> arrayList = new ArrayList<>();
			for (final OsmandPlugin osmandPlugin : OsmandPlugin.getAvailablePlugins()) {
				final String helpFileName = osmandPlugin.getHelpFileName();
				if (helpFileName != null) {
					arrayList.add(new HelpMenuItem(osmandPlugin.getName(), null, osmandPlugin.getLogoResourceId(), 
							helpFileName, getActivity()));
				}
			}
			return arrayList;
		}

		private List<HelpMenuItem> createOtherItems() {
			ArrayList<HelpMenuItem> arrayList = new ArrayList<>();
			//String name = getActivity().getString(R.string.instalation_troubleshooting_item);
			arrayList.add(new HelpMenuItem(R.string.instalation_troubleshooting_item, -1, -1, 
					"feature_articles/installation-and-troubleshooting.html", getActivity()));
			arrayList.add(new HelpMenuItem(R.string.techical_articles_item, -1, -1, 
					"feature_articles/technical-articles.html", getActivity()));
			arrayList.add(new HelpMenuItem(R.string.versions_item, -1, -1, 
					"feature_articles/changes.html", getActivity()));

			String releasedate = "";
			if (!this.getString(R.string.app_edition).equals("")) {
				releasedate = ", " + this.getString(R.string.shared_string_release).toLowerCase() + ": " + this.getString(R.string.app_edition);
			}
			String version = Version.getFullVersion(getOsmandApplication()) + releasedate;
			ShowArticleOnTouchListener listener = new ShowArticleOnTouchListener(
					"feature_articles/about.html", getActivity(), version);

			arrayList.add(new HelpMenuItem(getActivity().getString(R.string.shared_string_about), version, listener));
			return arrayList;
		}


	}
	
	public static class HelpAdapter extends OsmandBaseExpandableListAdapter {
		private final OsmandApplication ctx;
		private final FragmentActivity activity;

		public HelpAdapter(FragmentActivity activity) {
			this.ctx = (OsmandApplication) activity.getApplication();
			this.activity = activity;
		}

		@Override
		public HelpMenuItem getChild(int groupPosition, int childPosition) {
			if (categories[groupPosition] != HelpMenuCategory.HELP_US_TO_IMPROVE) {
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
			if (categories[groupPosition] == HelpMenuCategory.HELP_US_TO_IMPROVE) {
				convertView = LayoutInflater.from(parent.getContext()).inflate(
						R.layout.help_to_improve_item, parent, false);
				TextView feedbackButton = (TextView) convertView.findViewById(R.id.feedbackButton);
				Drawable pollIcon = ctx.getIconsCache().getContentIcon(R.drawable.ic_action_message);
				feedbackButton.setCompoundDrawablesWithIntrinsicBounds(null, pollIcon, null, null);
				feedbackButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						HelpArticleDialogFragment.instantiateWithUrl(OSMAND_POLL_HTML, ctx.getString(R.string.feedback))
								.show(activity.getSupportFragmentManager(), null);
					}
				});
				TextView contactUsButton = (TextView) convertView.findViewById(R.id.contactUsButton);
				Drawable contactUsIcon =
						ctx.getIconsCache().getContentIcon(R.drawable.ic_action_message);
				contactUsButton.setCompoundDrawablesWithIntrinsicBounds(null, contactUsIcon, null,
						null);
				final String email = ctx.getString(R.string.support_email);
				contactUsButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(Intent.ACTION_SENDTO);
						intent.setData(Uri.parse("mailto:")); // only email apps should handle this
						intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
						if (intent.resolveActivity(ctx.getPackageManager()) != null) {
							activity.startActivity(intent);
						}
					}
				});
				return convertView;
			} else {
				final HelpMenuItem child = getChild(groupPosition, childPosition);
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
			TextView nameView = ((TextView) v.findViewById(R.id.title));
			nameView.setText(titleId);
			v.setOnClickListener(null);
			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = ctx.getTheme();
			theme.resolveAttribute(R.attr.ctx_menu_info_view_bg, typedValue, true);
			v.setBackgroundColor(typedValue.data);

			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (categories[groupPosition] != HelpMenuCategory.HELP_US_TO_IMPROVE) {
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
			return HelpMenuCategory.values().length;
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

	private static class ShowArticleOnTouchListener implements View.OnClickListener {
		private final String filename;
		private final FragmentActivity ctx;
		private String name;

		private ShowArticleOnTouchListener(String filename, FragmentActivity ctx, String name) {
			this.filename = filename;
			this.name = name;
			this.ctx = ctx;
		}

		@Override
		public void onClick(View v) {
			HelpArticleDialogFragment.instantiateWithAsset(filename, name)
					.show(ctx.getSupportFragmentManager(), "DIALOG_HELP_ARTICLE");
		}
	}

	private enum HelpMenuCategory {
		BEGIN_WITH_OSMAND(R.string.begin_with_osmand_menu_group),
		FEATURES(R.string.features_menu_group),
		PLUGINS(R.string.plugins_menu_group),
		HELP_US_TO_IMPROVE(R.string.help_us_to_improve_menu_group),
		OTHER(R.string.other_menu_group);

		private List<HelpMenuItem> items;
		@StringRes
		private final int title;

		HelpMenuCategory(int title) {
			this.title = title;
		}

		public int getTitle() {
			return title;
		}

		public int getChildrenCount() {
			return items.size();
		}

		public HelpMenuItem getItem(int position) {
			return items.get(position);
		}

		public void initItems(List<HelpMenuItem> items) {
			this.items = items;
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

		public void bindMenuItem(HelpMenuItem menuItem) {
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
	
	public static class HelpMenuItem {
		private final String title;
		private final String description;
		@DrawableRes
		private final int icon;
		private final OnClickListener onClickListener;


		public HelpMenuItem(String name, String description, int icon, String helpFileName,
				FragmentActivity activity) {
			this.icon = icon;
			this.title = name;
			this.description = description;
			this.onClickListener = new ShowArticleOnTouchListener(helpFileName, activity, title);
		}
		
		public HelpMenuItem(int name, int description, int icon, String helpFileName,
				FragmentActivity activity) {
			this.title = activity.getString(name);
			this.description = description == -1 ? null : activity.getString(description);
			this.icon = icon;
			this.onClickListener = new ShowArticleOnTouchListener(helpFileName,  activity, title);
		}

		public HelpMenuItem(String name, String description, OnClickListener onClickListener) {
			this.title = name;
			this.description = description;
			this.icon = -1;
			this.onClickListener = onClickListener;
		}

		public String getTitle() {
			return title;
		}

		public String getDesription() {
			return description;
		}

		public int getIcon() {
			return icon;
		}

		public OnClickListener getOnClickListener() {
			return onClickListener;
		}

	}
}
