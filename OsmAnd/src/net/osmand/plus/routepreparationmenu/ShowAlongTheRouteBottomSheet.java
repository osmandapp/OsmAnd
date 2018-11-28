package net.osmand.plus.routepreparationmenu;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ShowAlongTheRouteBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ShowAlongTheRouteBottomSheet";


	public static final int REQUEST_CODE = 2;
	public static final int SHOW_CONTENT_ITEM_REQUEST_CODE = 3;

	private OsmandApplication app;

	private MapActivity mapActivity;
	private WaypointHelper waypointHelper;

	private ExpandableListView expListView;
	private ExpandableListAdapter adapter;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		Bundle args = getArguments();
		if (ctx == null || args == null) {
			return;
		}
		app = getMyApplication();
		mapActivity = (MapActivity) getActivity();
		waypointHelper = app.getWaypointHelper();

		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View titleView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.bottom_sheet_item_toolbar_title, null);
		TextView textView = (TextView) titleView.findViewById(R.id.title);
		textView.setText(R.string.show_along_the_route);

		Toolbar toolbar = (Toolbar) titleView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getContentIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);

		if (waypointHelper.isRouteCalculated()) {
			final ContentItem contentItem = getAdapterContentItems();

			items.add(new SimpleDividerItem(app));

			Drawable transparent = ContextCompat.getDrawable(ctx, R.color.color_transparent);
			adapter = new ExpandableListAdapter(ctx, contentItem);
			expListView = new ExpandableListView(ctx);
			expListView.setAdapter(adapter);
			expListView.setDivider(transparent);
			expListView.setGroupIndicator(transparent);
			expListView.setSelector(transparent);
			expListView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
			LinearLayout container = new LinearLayout(ctx);
			container.addView(expListView);

			items.add(new SimpleBottomSheetItem.Builder().setCustomView(container).create());
		}
	}

	protected String getCategotyTitle(int type, Context ctx) {
		String str = ctx.getString(R.string.shared_string_waypoints);
		switch (type) {
			case WaypointHelper.TARGETS:
				str = ctx.getString(R.string.shared_string_target_points);
				break;
			case WaypointHelper.ALARMS:
				str = ctx.getString(R.string.way_alarms);
				break;
			case WaypointHelper.FAVORITES:
				str = ctx.getString(R.string.shared_string_my_favorites);
				break;
			case WaypointHelper.WAYPOINTS:
				str = ctx.getString(R.string.shared_string_waypoints);
				break;
			case WaypointHelper.POI:
				str = ctx.getString(R.string.poi);
				break;
		}
		return str;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getBgColorId() {
		return nightMode ? R.color.wikivoyage_bottom_bar_bg_dark : R.color.bg_color_light;
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	private ContentItem getAdapterContentItems() {
		final ContentItem contentItem = new ContentItem("items", null);
		if (waypointHelper.isRouteCalculated()) {
			for (int i = 2; i < WaypointHelper.MAX; i++) {
				List<WaypointHelper.LocationPointWrapper> tp = waypointHelper.getWaypoints(i);
				ContentItem headerItem = new ContentItem(getCategotyTitle(i, app), contentItem);
				contentItem.subItems.add(headerItem);
				headerItem.type = i;

				if ((i == WaypointHelper.POI || i == WaypointHelper.FAVORITES) && waypointHelper.isTypeEnabled(i)) {
					ContentItem radiusItem = new ContentItem("radius", contentItem);
					headerItem.subItems.add(radiusItem);
				}

				if (tp != null && tp.size() > 0) {
					for (int j = 0; j < tp.size(); j++) {
						WaypointHelper.LocationPointWrapper pointWrapper = tp.get(j);
						String title = pointWrapper.getPoint().getPointDescription(app).getName();
						ContentItem subheaderItem = new ContentItem(title, headerItem);

						headerItem.subItems.add(subheaderItem);
						subheaderItem.point = pointWrapper;
					}
				}
			}
		}
		return contentItem;
	}

	private void updateAdapter() {
		if (adapter != null) {
			adapter.contentItem = getAdapterContentItems();
			adapter.notifyDataSetChanged();
		}
	}

	class ExpandableListAdapter extends OsmandBaseExpandableListAdapter {

		private Context context;

		private ContentItem contentItem;

		ExpandableListAdapter(Context context, ContentItem contentItem) {
			this.context = context;
			this.contentItem = contentItem;
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			return contentItem.getSubItems().get(groupPosition).getSubItems().get(childPosititon).getName();
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, final int childPosition,
		                         boolean isLastChild, View convertView, ViewGroup parent) {
			ContentItem group = contentItem.getSubItems().get(groupPosition);
			final ContentItem child = group.getSubItems().get(childPosition);

			if (child.name.equals("radius")) {
				convertView = createItemForRadiusProximity(group.type, nightMode);
			} else {
				convertView = LayoutInflater.from(context)
						.inflate(R.layout.bottom_sheet_item_show_along_the_route, parent, false);
				updatePointInfoView(app, mapActivity, convertView, group, child);

				convertView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						WaypointDialogHelper.showOnMap(app, mapActivity, child.point.getPoint(), false);
						Fragment fragment = getTargetFragment();
						if (fragment != null) {
							fragment.onActivityResult(getTargetRequestCode(), SHOW_CONTENT_ITEM_REQUEST_CODE, null);
						}
						dismiss();
					}
				});
			}

			return convertView;
		}

		private void updatePointInfoView(final OsmandApplication app, final Activity activity,
		                                 View localView, final ContentItem group, final ContentItem item) {
			WaypointHelper wh = app.getWaypointHelper();
			final WaypointHelper.LocationPointWrapper ps = item.point;
			final LocationPoint point = ps.getPoint();
			TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
			AndroidUtils.setTextPrimaryColor(activity, text, nightMode);
			TextView textShadow = (TextView) localView.findViewById(R.id.waypoint_text_shadow);

			final ImageButton remove = (ImageButton) localView.findViewById(R.id.info_close);
			remove.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
			remove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getWaypointHelper().removeVisibleLocationPoint(ps);
					group.subItems.remove(item);
					adapter.notifyDataSetChanged();
				}
			});

			TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
			((ImageView) localView.findViewById(R.id.waypoint_icon)).setImageDrawable(ps.getDrawable(activity, app, nightMode));
			int dist = -1;
			boolean startPoint = ps.type == WaypointHelper.TARGETS && ((TargetPointsHelper.TargetPoint) ps.point).start;
			if (!startPoint) {
				if (!wh.isRouteCalculated()) {
					if (activity instanceof MapActivity) {
						dist = (int) MapUtils.getDistance(((MapActivity) activity).getMapView().getLatitude(), ((MapActivity) activity)
								.getMapView().getLongitude(), point.getLatitude(), point.getLongitude());
					}
				} else {
					dist = wh.getRouteDistance(ps);
				}
			}

			if (dist > 0) {
				textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));
			} else {
				textDist.setText("");
			}

			TextView textDeviation = (TextView) localView.findViewById(R.id.waypoint_deviation);
			if (textDeviation != null) {
				if (dist > 0 && ps.deviationDistance > 0) {
					String devStr = "+" + OsmAndFormatter.getFormattedDistance(ps.deviationDistance, app);
					textDeviation.setText(devStr);
					int colorId = nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light;
					AndroidUtils.setTextSecondaryColor(activity, textDeviation, nightMode);
					if (ps.deviationDirectionRight) {
						textDeviation.setCompoundDrawablesWithIntrinsicBounds(
								app.getUIUtilities().getIcon(R.drawable.ic_small_turn_right, colorId),
								null, null, null);
					} else {
						textDeviation.setCompoundDrawablesWithIntrinsicBounds(
								app.getUIUtilities().getIcon(R.drawable.ic_small_turn_left, colorId),
								null, null, null);
					}

					textDeviation.setVisibility(View.VISIBLE);
				} else {
					textDeviation.setText("");
					textDeviation.setVisibility(View.GONE);
				}
			}

			String descr;
			PointDescription pd = point.getPointDescription(app);
			if (Algorithms.isEmpty(pd.getName())) {
				descr = pd.getTypeName();
			} else {
				descr = pd.getName();
			}

			if (textShadow != null) {
				textShadow.setText(descr);
			}
			text.setText(descr);

			String pointDescription = "";
			TextView descText = (TextView) localView.findViewById(R.id.waypoint_desc_text);
			if (descText != null) {
				AndroidUtils.setTextSecondaryColor(activity, descText, nightMode);
				if (ps.type == WaypointHelper.FAVORITES) {
					FavouritePoint favPoint = (FavouritePoint) ps.point;
					pointDescription = Algorithms.isEmpty(favPoint.getCategory()) ? activity.getResources().getString(R.string.shared_string_favorites) : favPoint.getCategory();
				}
			}
			if (Algorithms.objectEquals(descr, pointDescription)) {
				pointDescription = "";
			}
			if (dist > 0 && !Algorithms.isEmpty(pointDescription)) {
				pointDescription = "  •  " + pointDescription;
			}
			if (descText != null) {
				descText.setText(pointDescription);
			}
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return contentItem.getSubItems().get(groupPosition).getSubItems().size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return contentItem.getSubItems().get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return contentItem.getSubItems().size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(final int groupPosition, final boolean isExpanded,
		                         View convertView, ViewGroup parent) {
			final ContentItem group = contentItem.getSubItems().get(groupPosition);
			final int type = group.type;
			final boolean enabled = waypointHelper.isTypeEnabled(type);

			if (convertView == null) {
				convertView = LayoutInflater.from(context)
						.inflate(R.layout.along_the_route_category_item, parent, false);
			}
			TextView lblListHeader = (TextView) convertView.findViewById(R.id.title);
			lblListHeader.setText(group.name);
			lblListHeader.setTextColor(ContextCompat.getColor(context, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));

			adjustIndicator(app, groupPosition, isExpanded, convertView, !nightMode);

			final CompoundButton compoundButton = (CompoundButton) convertView.findViewById(R.id.compound_button);
			compoundButton.setChecked(enabled);
			compoundButton.setEnabled(true);
			compoundButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean isChecked = compoundButton.isChecked();
					if (type == WaypointHelper.POI && isChecked) {
						selectPoi(type, isChecked);
					} else {
						enableType(type, isChecked);
					}
				}
			});

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (enabled) {
						if (isExpanded) {
							expListView.collapseGroup(groupPosition);
						} else {
							expListView.expandGroup(groupPosition);
						}
					}
				}
			});

			View topDivider = convertView.findViewById(R.id.top_divider);
			View bottomDivider = convertView.findViewById(R.id.bottom_divider);

			AndroidUtils.setBackground(app, topDivider, nightMode,
					R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
			AndroidUtils.setBackground(app, bottomDivider, nightMode,
					R.color.dashboard_divider_light, R.color.dashboard_divider_dark);

			bottomDivider.setVisibility(isExpanded ? View.GONE : View.VISIBLE);

			return convertView;
		}

		private View createItemForRadiusProximity(final int type, boolean nightMode) {
			View v;
			if (type == WaypointHelper.POI) {
				v = mapActivity.getLayoutInflater().inflate(R.layout.along_the_route_radius_poi, null);
				AndroidUtils.setTextSecondaryColor(mapActivity, (TextView) v.findViewById(R.id.titleEx), nightMode);
				String descEx = !app.getPoiFilters().isShowingAnyPoi() ? getString(R.string.poi) : app.getPoiFilters().getSelectedPoiFiltersName();
				((TextView) v.findViewById(R.id.title)).setText(getString(R.string.search_radius_proximity) + ":");
				((TextView) v.findViewById(R.id.titleEx)).setText(getString(R.string.shared_string_type) + ":");
				final TextView radiusEx = (TextView) v.findViewById(R.id.descriptionEx);
				radiusEx.setText(descEx);
				v.findViewById(R.id.secondCellContainer).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						mapActivity.getMapLayers().showSingleChoicePoiFilterDialog(mapActivity.getMapView(), new MapActivityLayers.DismissListener() {

							@Override
							public void dismiss() {
								enableType(type, true);
							}
						});
					}
				});
				AndroidUtils.setTextSecondaryColor(mapActivity, (TextView) v.findViewById(R.id.title), nightMode);
				final TextView radius = (TextView) v.findViewById(R.id.description);
				radius.setText(OsmAndFormatter.getFormattedDistance(waypointHelper.getSearchDeviationRadius(type), app));
				v.findViewById(R.id.firstCellContainer).setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						selectDifferentRadius(type);
					}
				});
				AndroidUtils.setBackground(app, v.findViewById(R.id.top_divider), nightMode,
						R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
				AndroidUtils.setBackground(app, v.findViewById(R.id.bottom_divider), nightMode,
						R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
			} else {
				v = mapActivity.getLayoutInflater().inflate(R.layout.along_the_route_radius_simple, null);
				((TextView) v.findViewById(R.id.title)).setText(getString(R.string.search_radius_proximity));
				AndroidUtils.setTextPrimaryColor(mapActivity, (TextView) v.findViewById(R.id.title), nightMode);
				final TextView radius = (TextView) v.findViewById(R.id.description);
				radius.setText(OsmAndFormatter.getFormattedDistance(waypointHelper.getSearchDeviationRadius(type), app));
				radius.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						selectDifferentRadius(type);
					}

				});
			}
			return v;
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

	private void selectPoi(final int type, final boolean enable) {
		if (!app.getPoiFilters().isPoiFilterSelected(PoiUIFilter.CUSTOM_FILTER_ID)) {
			mapActivity.getMapLayers().showSingleChoicePoiFilterDialog(mapActivity.getMapView(),
					new MapActivityLayers.DismissListener() {
						@Override
						public void dismiss() {
							if (app.getPoiFilters().isShowingAnyPoi()) {
								enableType(type, enable);
							}
						}
					});
		} else {
			enableType(type, enable);
		}
	}

	protected void selectDifferentRadius(final int type) {
		int length = WaypointHelper.SEARCH_RADIUS_VALUES.length;
		String[] names = new String[length];
		int selected = 0;
		for (int i = 0; i < length; i++) {
			names[i] = OsmAndFormatter.getFormattedDistance(WaypointHelper.SEARCH_RADIUS_VALUES[i], app);
			if (WaypointHelper.SEARCH_RADIUS_VALUES[i] == waypointHelper.getSearchDeviationRadius(type)) {
				selected = i;
			}
		}
		new AlertDialog.Builder(mapActivity)
				.setSingleChoiceItems(names, selected, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						int value = WaypointHelper.SEARCH_RADIUS_VALUES[i];
						if (waypointHelper.getSearchDeviationRadius(type) != value) {
							waypointHelper.setSearchDeviationRadius(type, value);
							recalculatePoints(type);
							dialogInterface.dismiss();
							updateAdapter();
						}
					}
				}).setTitle(app.getString(R.string.search_radius_proximity))
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	private void enableType(final int type,
	                        final boolean enable) {
		new EnableWaypointsTypeTask(this, type, enable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void recalculatePoints(final int type) {
		new RecalculatePointsTask(this, type).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private static class RecalculatePointsTask extends AsyncTask<Void, Void, Void> {

		private OsmandApplication app;
		private WeakReference<ShowAlongTheRouteBottomSheet> fragmentRef;
		private int type;

		RecalculatePointsTask(ShowAlongTheRouteBottomSheet fragment, int type) {
			this.app = fragment.getMyApplication();
			this.fragmentRef = new WeakReference<>(fragment);
			this.type = type;
		}

		@Override
		protected Void doInBackground(Void... params) {
			app.getWaypointHelper().recalculatePoints(type);
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			ShowAlongTheRouteBottomSheet fragment = fragmentRef.get();
			if (fragment != null) {
				fragment.updateAdapter();
			}
		}
	}

	private static class EnableWaypointsTypeTask extends AsyncTask<Void, Void, Void> {

		private OsmandApplication app;
		private WeakReference<ShowAlongTheRouteBottomSheet> fragmentRef;
		private int type;
		private boolean enable;

		EnableWaypointsTypeTask(ShowAlongTheRouteBottomSheet fragment, int type, boolean enable) {
			this.app = fragment.getMyApplication();
			this.fragmentRef = new WeakReference<>(fragment);
			this.type = type;
			this.enable = enable;
		}

		@Override
		protected Void doInBackground(Void... params) {
			app.getWaypointHelper().enableWaypointType(type, enable);
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			ShowAlongTheRouteBottomSheet fragment = fragmentRef.get();
			if (fragment != null) {
				fragment.updateAdapter();
			}
		}
	}

	public static class ContentItem {

		private int type;
		private String name;
		private ArrayList<ContentItem> subItems = new ArrayList<>();
		private ContentItem parent;
		private WaypointHelper.LocationPointWrapper point;

		private ContentItem(String name, ContentItem parent) {
			this.parent = parent;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public ContentItem getParent() {
			return parent;
		}

		public ArrayList<ContentItem> getSubItems() {
			return subItems;
		}
	}
}