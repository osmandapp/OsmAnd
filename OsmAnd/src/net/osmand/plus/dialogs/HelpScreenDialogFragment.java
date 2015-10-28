package net.osmand.plus.dialogs;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;

import java.util.ArrayList;
import java.util.List;

public class HelpScreenDialogFragment extends DialogFragment {

	private static final List<MenuCategory> menu = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = ((OsmandApplication) getActivity().getApplication())
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);


		final MenuCategory beginWithOsmand = new MenuCategory(R.string.begin_with_osmand_menu_group);
		MyMenuItem.Builder builder = new MyMenuItem.Builder();
		builder.setTitle(R.string.first_usage_menu_item)
				.setDesription(R.string.first_usage_menu_item_description);
		beginWithOsmand.addItem(builder.create());
		builder = new MyMenuItem.Builder();
		builder.setTitle(R.string.shared_string_navigation)
				.setDesription(R.string.navigation_menu_item_description);
		beginWithOsmand.addItem(builder.create());
		builder = new MyMenuItem.Builder();
		builder.setTitle(R.string.faq_menu_item)
				.setDesription(R.string.faq_menu_item_description);
		beginWithOsmand.addItem(builder.create());

		menu.add(beginWithOsmand);
		menu.add(new MenuCategory(R.string.features_menu_group));
		menu.add(new MenuCategory(R.string.plugins_menu_group));
		menu.add(new MenuCategory(R.string.help_us_to_improve_menu_group));
		menu.add(new MenuCategory(R.string.other_menu_group));
	}

	@NonNull
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_help_screen, container, false);
		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		final OsmandApplication application = (OsmandApplication) getActivity().getApplication();
		final MenuAdapter listAdapter = new MenuAdapter(application, menu);
		listView.setAdapter(listAdapter);
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
		return view;
	}

	public static class MenuAdapter extends OsmandBaseExpandableListAdapter {
		private OsmandApplication ctx;
		private final List<MenuCategory> menu;

		public MenuAdapter(OsmandApplication ctx, List<MenuCategory> menu) {
			this.ctx = ctx;
			this.menu = menu;
		}

		@Override
		public MyMenuItem getChild(int groupPosition, int childPosition) {
			return menu.get(groupPosition).getItem(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild,
								 View convertView, ViewGroup parent) {
			final MyMenuItem child = getChild(groupPosition, childPosition);
			MenuItemViewHolder viewHolder;
			if (convertView == null) {
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
			return menu.get(groupPosition).getChildrenCount();
		}

		@Override
		public Integer getGroup(int groupPosition) {
			return menu.get(groupPosition).getTitle();
		}

		@Override
		public int getGroupCount() {
			return menu.size();
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
			nameTextView.setText(menuItem.title);
			if (menuItem.desription != -1) {
				descrTextView.setVisibility(View.VISIBLE);
				descrTextView.setText(menuItem.desription);
			} else {
				descrTextView.setVisibility(View.GONE);
			}
			if (menuItem.icon != -1) {
				leftImageView.setVisibility(View.VISIBLE);
				leftImageView.setImageDrawable(context.getIconsCache().getContentIcon(menuItem.icon));
			} else {
				leftImageView.setVisibility(View.GONE);
			}
		}
	}

	private static class MenuCategory {
		private final List<MyMenuItem> items = new ArrayList<>();
		@StringRes
		private final int title;

		private MenuCategory(@StringRes int title) {
			this.title = title;
		}

		public int getTitle() {
			return title;
		}

		public int getChildrenCount() {
			return items.size();
		}

		public MenuCategory addItem(MyMenuItem item) {
			items.add(item);
			return this;
		}

		public MyMenuItem getItem(int position) {
			return items.get(position);
		}
	}


	public static class MyMenuItem {
		private final int title;
		private final int desription;
		@DrawableRes
		private final int icon;
		private final boolean isSquare;

//		MyMenuItem(int title) {
//			this.title = title;
//			desription = null;
//			icon = -1;
//			isSquare = false;
//		}

		private MyMenuItem(int title, int desription, int icon, boolean isSquare) {
			this.title = title;
			this.desription = desription;
			this.icon = icon;
			this.isSquare = isSquare;
		}

		public static class Builder {
			@StringRes
			private int title = -1;
			private int desription = -1;
			private int icon = -1;
			private boolean isSquare = false;

			public Builder setTitle(@StringRes int title) {
				this.title = title;
				return this;
			}

			public Builder setDesription(int desription) {
				this.desription = desription;
				return this;
			}

			public Builder setIcon(int icon) {
				this.icon = icon;
				return this;
			}

			public Builder setIsSquare(boolean isSquare) {
				this.isSquare = isSquare;
				return this;
			}

			public HelpScreenDialogFragment.MyMenuItem create() {
				return new HelpScreenDialogFragment.MyMenuItem(title, desription, icon, isSquare);
			}
		}
	}
}
