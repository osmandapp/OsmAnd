package net.osmand.plus.routepreparationmenu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import net.osmand.plus.helpers.LocationPointWrapper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.data.ValueHolder;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.IRoutingDataUpdateListener;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ShowAlongTheRouteBottomSheet extends MenuBottomSheetDialogFragment implements IRouteInformationListener, IRoutingDataUpdateListener {

	public static final String TAG = "ShowAlongTheRouteBottomSheet";


	public static final int REQUEST_CODE = 2;
	public static final int SHOW_CONTENT_ITEM_REQUEST_CODE = 3;
	public static final String EXPAND_TYPE_KEY = "expand_type_key";

	private OsmandApplication app;

	private MapActivity mapActivity;
	private WaypointHelper waypointHelper;

	private ExpandableListView expListView;
	private ExpandableListAdapter adapter;
	private int expandIndex = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		mapActivity = (MapActivity) getActivity();
		waypointHelper = app.getWaypointHelper();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		Bundle args = getArguments();
		if (ctx == null || args == null) {
			return;
		}
		int expandType = args.getInt(EXPAND_TYPE_KEY, -1);

		View titleView = UiUtilities.getInflater(ctx, nightMode)
				.inflate(R.layout.bottom_sheet_item_toolbar_title, null);
		TextView textView = titleView.findViewById(R.id.title);
		textView.setText(R.string.show_along_the_route);

		Toolbar toolbar = titleView.findViewById(R.id.toolbar);
		Drawable icBack = getContentIcon(AndroidUtils.getNavigationIconResId(ctx));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);

		ContentItem contentItem = getAdapterContentItems();
		if (expandType != -1) {
			ArrayList<ContentItem> subItems = contentItem.getSubItems();
			for (int i = 0; i < subItems.size(); i++) {
				ContentItem item = subItems.get(i);
				if (expandType == item.type) {
					expandIndex = i;
					break;
				}
			}
		}

		items.add(new SimpleDividerItem(app));

		Drawable transparent = AppCompatResources.getDrawable(ctx, R.color.color_transparent);
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

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	private ContentItem getAdapterContentItems() {
		ContentItem contentItem = new ContentItem();
		for (int i = 2; i < WaypointHelper.MAX; i++) {
			List<LocationPointWrapper> tp = waypointHelper.getWaypoints(i);
			ContentItem headerItem = new PointItem(i);
			contentItem.subItems.add(headerItem);
			headerItem.type = i;

			if (waypointHelper.isRouteCalculated()) {
				if ((i == WaypointHelper.POI || i == WaypointHelper.FAVORITES) && waypointHelper.isTypeEnabled(i)) {
					ContentItem radiusItem = new RadiusItem(i);
					headerItem.subItems.add(radiusItem);
				}
				if (tp != null && tp.size() > 0) {
					for (int j = 0; j < tp.size(); j++) {
						LocationPointWrapper pointWrapper = tp.get(j);
						if (!waypointHelper.isPointPassed(pointWrapper)) {
							PointItem subheaderItem = new PointItem(pointWrapper.type);
							subheaderItem.point = pointWrapper;
							headerItem.subItems.add(subheaderItem);
						}
					}
				}
			} else {
				ContentItem infoItem = new InfoItem(i);
				headerItem.subItems.add(infoItem);
			}
		}
		return contentItem;
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		updateAdapter();
	}

	@Override
	public void routeWasCancelled() {

	}

	@Override
	public void routeWasFinished() {

	}

	@Override
	public void onPause() {
		super.onPause();
		app.getRoutingHelper().removeListener(this);
		app.getRoutingHelper().removeRouteDataListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getRoutingHelper().addListener(this);
		app.getRoutingHelper().addRouteDataListener(this);
		if (expandIndex != -1) {
			expListView.expandGroup(expandIndex);
			setupHeightAndBackground(getView());
		}
	}


	private void updateAdapter() {
		if (adapter != null) {
			adapter.contentItem = getAdapterContentItems();
			adapter.notifyDataSetChanged();
			setupHeightAndBackground(getView());
		}
	}

	@Override
	public void onRoutingDataUpdate() {
		updateAdapter();
	}

	class ExpandableListAdapter extends OsmandBaseExpandableListAdapter {

		private final Context context;

		private ContentItem contentItem;

		ExpandableListAdapter(Context context, ContentItem contentItem) {
			this.context = context;
			this.contentItem = contentItem;
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			return contentItem.getSubItems().get(groupPosition).getSubItems().get(childPosititon);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
		                         boolean isLastChild, View convertView, ViewGroup parent) {
			LayoutInflater themedInflater = UiUtilities.getInflater(mapActivity, nightMode);

			ContentItem group = contentItem.getSubItems().get(groupPosition);
			ContentItem child = group.getSubItems().get(childPosition);

			if (child instanceof RadiusItem) {
				convertView = createItemForRadiusProximity(themedInflater, group.type);
			} else if (child instanceof InfoItem) {
				convertView = createInfoItem(themedInflater);
			} else if (child instanceof PointItem) {
				PointItem item = (PointItem) child;
				convertView = themedInflater.inflate(R.layout.along_the_route_point_item, parent, false);
				WaypointDialogHelper.updatePointInfoView(app, mapActivity, convertView, item.point, true, nightMode, true, false);

				convertView.findViewById(R.id.waypoint_container).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment fragment = getTargetFragment();
						if (fragment != null) {
							fragment.onActivityResult(getTargetRequestCode(), SHOW_CONTENT_ITEM_REQUEST_CODE, null);
						}
						dismiss();
						WaypointDialogHelper.showOnMap(app, mapActivity, item.point.getPoint(), false);
					}
				});

				ImageButton remove = convertView.findViewById(R.id.info_close);
				remove.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
				remove.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						app.getWaypointHelper().removeVisibleLocationPoint(item.point);
						group.subItems.remove(item);
						adapter.notifyDataSetChanged();
					}
				});
			}

			View bottomDivider = convertView.findViewById(R.id.bottom_divider);
			if (bottomDivider != null) {
				bottomDivider.setVisibility(isLastChild ? View.VISIBLE : View.GONE);
			}

			if (child instanceof RadiusItem && group.type == WaypointHelper.POI) {
				convertView.findViewById(R.id.divider).setVisibility(isLastChild ? View.GONE : View.VISIBLE);
			}

			return convertView;
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
		public View getGroupView(int groupPosition, boolean isExpanded,
		                         View convertView, ViewGroup parent) {
			ContentItem group = contentItem.getSubItems().get(groupPosition);
			int type = group.type;
			boolean enabled = waypointHelper.isTypeEnabled(type);
			boolean nightMode = isNightMode(app);

			if (convertView == null) {
				convertView = UiUtilities.getInflater(context, nightMode).inflate(R.layout.along_the_route_category_item, parent, false);
			}
			TextView lblListHeader = convertView.findViewById(R.id.title);
			lblListHeader.setText(getHeader(group.type, mapActivity));

			adjustIndicator(app, groupPosition, isExpanded, convertView, !nightMode);

			CompoundButton compoundButton = convertView.findViewById(R.id.compound_button);
			compoundButton.setChecked(enabled);
			compoundButton.setEnabled(true);
			compoundButton.setOnClickListener(v -> {
				boolean isChecked = compoundButton.isChecked();
				if (type == WaypointHelper.POI && isChecked) {
					selectPoi(type, isChecked);
				} else {
					enableType(type, isChecked);
				}
				if (isChecked) {
					expListView.expandGroup(groupPosition);
					setupHeightAndBackground(getView());
				}
			});
			int profileColor = ColorUtilities.getAppModeColor(app, nightMode);
			UiUtilities.setupCompoundButton(nightMode, profileColor, compoundButton);

			convertView.setOnClickListener(v -> {
				if (enabled) {
					if (isExpanded) {
						expListView.collapseGroup(groupPosition);
					} else {
						expListView.expandGroup(groupPosition);
					}
					setupHeightAndBackground(getView());
				}
			});

			convertView.findViewById(R.id.bottom_divider).setVisibility(isExpanded ? View.GONE : View.VISIBLE);

			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		protected void adjustIndicator(OsmandApplication app, int groupPosition, boolean expanded, View row, boolean light) {
			adjustIndicator(app, row, expanded, !light);
		}

		private String getHeader(int type, Context ctx) {
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
					str = ctx.getString(R.string.points_of_interests);
					break;
			}
			return str;
		}

		private View createInfoItem(LayoutInflater themedInflater) {
			View view = themedInflater.inflate(R.layout.show_along_the_route_info_item, null);
			TextView titleTv = view.findViewById(R.id.title);
			titleTv.setText(getText(R.string.waiting_for_route_calculation));

			return view;
		}

		private View createItemForRadiusProximity(LayoutInflater themedInflater, int type) {
			View v;
			if (type == WaypointHelper.POI) {
				v = themedInflater.inflate(R.layout.along_the_route_radius_poi, null);
				String descEx = !app.getPoiFilters().isShowingAnyPoi() ?
						getString(R.string.poi) : app.getPoiFilters().getSelectedPoiFiltersName();
				((TextView) v.findViewById(R.id.title)).setText(getString(R.string.search_radius_proximity) + ":");
				((TextView) v.findViewById(R.id.titleEx)).setText(getString(R.string.shared_string_type) + ":");
				TextView radiusEx = v.findViewById(R.id.descriptionEx);
				radiusEx.setText(descEx);
				v.findViewById(R.id.secondCellContainer).setOnClickListener(view -> mapActivity.getMapLayers()
						.showSingleChoicePoiFilterDialog(mapActivity, () -> enableType(WaypointHelper.POI, true)));
				TextView radius = v.findViewById(R.id.description);
				radius.setText(OsmAndFormatter.getFormattedDistance(waypointHelper.getSearchDeviationRadius(type), app));
				v.findViewById(R.id.firstCellContainer).setOnClickListener(view -> selectDifferentRadius(type));
			} else {
				v = themedInflater.inflate(R.layout.along_the_route_radius_simple, null);
				((TextView) v.findViewById(R.id.title)).setText(getString(R.string.search_radius_proximity));
				TextView radius = v.findViewById(R.id.description);
				radius.setText(OsmAndFormatter.getFormattedDistance(waypointHelper.getSearchDeviationRadius(type), app));
				v.setOnClickListener(view -> selectDifferentRadius(type));
			}
			return v;
		}
	}

	private void selectPoi(int type, boolean enable) {
		if (!app.getPoiFilters().isPoiFilterSelected(PoiUIFilter.CUSTOM_FILTER_ID)) {
			mapActivity.getMapLayers().showSingleChoicePoiFilterDialog(mapActivity,
					() -> {
						if (app.getPoiFilters().isShowingAnyPoi()) {
							enableType(type, enable);
						}
					});
		} else {
			enableType(type, enable);
		}
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private void updateMenu() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().updateMenu();
		}
	}

	protected void selectDifferentRadius(int type) {
		int length = WaypointHelper.SEARCH_RADIUS_VALUES.length;
		String[] names = new String[length];
		int selected = 0;
		for (int i = 0; i < length; i++) {
			names[i] = OsmAndFormatter.getFormattedDistance(WaypointHelper.SEARCH_RADIUS_VALUES[i], app);
			if (WaypointHelper.SEARCH_RADIUS_VALUES[i] == waypointHelper.getSearchDeviationRadius(type)) {
				selected = i;
			}
		}

		AlertDialogData dialogData = new AlertDialogData(mapActivity, nightMode)
				.setTitle(app.getString(R.string.search_radius_proximity))
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode))
				.setNegativeButton(R.string.shared_string_cancel, null);

		CustomAlert.showSingleSelection(dialogData, names, selected, v -> {
			int which = (int) v.getTag();
			int value = WaypointHelper.SEARCH_RADIUS_VALUES[which];
			if (waypointHelper.getSearchDeviationRadius(type) != value) {
				waypointHelper.setSearchDeviationRadius(type, value);
				recalculatePoints(type);
				updateAdapter();
			}
		});
	}

	private void enableType(int type,
	                        boolean enable) {
		new EnableWaypointsTypeTask(this, type, enable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void recalculatePoints(int type) {
		new RecalculatePointsTask(this, type).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private static class RecalculatePointsTask extends AsyncTask<Void, Void, Void> {

		private final OsmandApplication app;
		private final WeakReference<ShowAlongTheRouteBottomSheet> fragmentRef;
		private final int type;

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

		private final OsmandApplication app;
		private final WeakReference<ShowAlongTheRouteBottomSheet> fragmentRef;
		private final int type;
		private final boolean enable;

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
			if (fragment != null && fragment.isAdded()) {
				fragment.updateAdapter();
				fragment.updateMenu();
			}
		}
	}

	private static class ContentItem {

		private int type;
		private final ArrayList<ContentItem> subItems = new ArrayList<>();

		private ContentItem(int type) {
			this.type = type;
		}

		private ContentItem() {
		}

		private ArrayList<ContentItem> getSubItems() {
			return subItems;
		}
	}

	private static class RadiusItem extends ContentItem {

		private RadiusItem(int type) {
			super(type);
		}
	}

	private static class InfoItem extends ContentItem {

		private InfoItem(int type) {
			super(type);
		}
	}

	private static class PointItem extends ContentItem {

		private LocationPointWrapper point;

		private PointItem(int type) {
			super(type);
		}
	}
}