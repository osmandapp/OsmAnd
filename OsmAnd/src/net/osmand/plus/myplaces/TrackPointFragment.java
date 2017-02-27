package net.osmand.plus.myplaces;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TrackPointFragment extends OsmAndListFragment {

	public static final String ARG_TO_FILTER_SHORT_TRACKS = "ARG_TO_FILTER_SHORT_TRACKS";

	private OsmandApplication app;
	private ArrayAdapter<GpxDisplayItem> adapter;
	private GpxDisplayItemType[] filterTypes = { GpxDisplayItemType.TRACK_POINTS, GpxDisplayItemType.TRACK_ROUTE_POINTS };

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = getMyApplication();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(getResources().getColor(
				getMyApplication().getSettings().isLightContent() ? R.color.ctx_menu_info_view_bg_light
						: R.color.ctx_menu_info_view_bg_dark));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.header_layout).setVisibility(View.GONE);
		ListView listView = (ListView) view.findViewById(android.R.id.list);
		listView.setDivider(null);
		listView.setDividerHeight(0);
		TextView tv = new TextView(getActivity());
		tv.setText(R.string.none_selected_gpx);
		tv.setTextSize(24);
		listView.setEmptyView(tv);
		setContent(listView);
		return view;
	}

	public TrackActivity getMyActivity() {
		return (TrackActivity) getActivity();
	}

	public ArrayAdapter<?> getAdapter() {
		return adapter;
	}

	private GPXUtilities.GPXFile getGpx() {
		return getMyActivity().getGpx();
	}

	private GPXDatabase.GpxDataItem getGpxDataItem() {
		return getMyActivity().getGpxDataItem();
	}

	private void setupListView(ListView listView) {
		if (adapter.getCount() > 0) {
			listView.addHeaderView(getActivity().getLayoutInflater().inflate(R.layout.list_shadow_header, null, false));
			listView.addFooterView(getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false));
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		MenuItem item = menu.add(R.string.shared_string_add_to_favorites)
				.setIcon(R.drawable.ic_action_fav_dark)
				.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				//saveAsFavorites(filterType());
				return true;
			}
		});
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

		if (app.getSettings().USE_MAP_MARKERS.get()) {
			item = menu.add(R.string.shared_string_add_to_map_markers)
					.setIcon(R.drawable.ic_action_flag_dark)
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							//saveAsMapMarkers(GpxSelectionHelper.GpxDisplayItemType.TRACK_POINTS);
							return true;
						}
					});
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}

	private boolean isArgumentTrue(@NonNull String arg) {
		return getArguments() != null && getArguments().getBoolean(arg);
	}

	private boolean hasFilterType(GpxDisplayItemType[] filterTypes, GpxDisplayItemType filterType) {
		for (GpxDisplayItemType type : filterTypes) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	private List<GpxDisplayGroup> filterGroups(GpxDisplayItemType[] types) {
		List<GpxDisplayGroup> result = getMyActivity().getGpxFile(false);
		List<GpxDisplayGroup> groups = new ArrayList<>();
		for (GpxDisplayGroup group : result) {
			boolean add = types == null || hasFilterType(types, group.getType());
			if (isArgumentTrue(ARG_TO_FILTER_SHORT_TRACKS)) {
				Iterator<GpxDisplayItem> item = group.getModifiableList().iterator();
				while (item.hasNext()) {
					GpxDisplayItem it2 = item.next();
					if (it2.analysis != null && it2.analysis.totalDistance < 100) {
						item.remove();
					}
				}
				if (group.getModifiableList().isEmpty()) {
					add = false;
				}
			}
			if (add) {
				groups.add(group);
			}

		}
		return groups;
	}

	public void setContent() {
		setContent(getListView());
	}

	public void setContent(ListView listView) {
		adapter = new PointGPXAdapter(new ArrayList<GpxDisplayItem>());
		updateContent();
		setupListView(listView);
		setListAdapter(adapter);
	}

	protected void updateContent() {
		adapter.clear();
		List<GpxDisplayGroup> groups = filterGroups(filterTypes);
		adapter.setNotifyOnChange(false);
		for (GpxDisplayItem i : flatten(groups)) {
			adapter.add(i);
		}
		adapter.setNotifyOnChange(true);
		adapter.notifyDataSetChanged();
	}

	protected List<GpxDisplayItem> flatten(List<GpxDisplayGroup> groups) {
		ArrayList<GpxDisplayItem> list = new ArrayList<>();
		for(GpxDisplayGroup g : groups) {
			list.addAll(g.getModifiableList());
		}
		return list;
	}

	protected void saveAsFavorites(final GpxDisplayItemType gpxDisplayItemType) {
		AlertDialog.Builder b = new AlertDialog.Builder(getMyActivity());
		final EditText editText = new EditText(getMyActivity());
		final List<GpxDisplayGroup> gs = filterGroups(new GpxDisplayItemType[] { gpxDisplayItemType });
		if (gs.size() == 0) {
			return;
		}
		String name = gs.get(0).getName();
		if(name.indexOf('\n') > 0) {
			name = name.substring(0, name.indexOf('\n'));
		}
		editText.setText(name);
		editText.setPadding(7, 3, 7, 3);
		b.setTitle(R.string.save_as_favorites_points);
		b.setView(editText);
		b.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				saveFavoritesImpl(flatten(gs), editText.getText().toString());
			}
		});
		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.show();
	}

	protected void saveAsMapMarkers(final GpxDisplayItemType gpxDisplayItemType) {
		AlertDialog.Builder b = new AlertDialog.Builder(getMyActivity());
		final List<GpxDisplayGroup> gs = filterGroups(new GpxDisplayItemType[] { gpxDisplayItemType });
		if (gs.size() == 0) {
			return;
		}
		b.setMessage(R.string.add_points_to_map_markers_q);
		b.setPositiveButton(R.string.shared_string_add, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				saveMapMarkersImpl(flatten(gs));
			}
		});
		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.show();
	}

	protected void saveFavoritesImpl(List<GpxDisplayItem> modifiableList, String category) {
		FavouritesDbHelper fdb = app.getFavorites();
		for(GpxDisplayItem i : modifiableList) {
			if (i.locationStart != null) {
				FavouritePoint fp = new FavouritePoint(i.locationStart.lat, i.locationStart.lon, i.name, category);
				if (!Algorithms.isEmpty(i.description)) {
					fp.setDescription(i.description);
				}
				fdb.addFavourite(fp, false);
			}
		}
		fdb.saveCurrentPointsIntoFile();
	}

	protected void saveMapMarkersImpl(List<GpxDisplayItem> modifiableList) {
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<LatLon> points = new ArrayList<>();
		List<PointDescription> names = new ArrayList<>();
		for(GpxDisplayItem i : modifiableList) {
			if (i.locationStart != null) {
				points.add(new LatLon(i.locationStart.lat, i.locationStart.lon));
				names.add(new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, i.name));
			}
		}
		markersHelper.addMapMarkers(points, names);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		GpxDisplayItem child = adapter.getItem(position);
		if (child != null) {
			if (child.group.getGpx() != null) {
				app.getSelectedGpxHelper().setGpxFileToDisplay(child.group.getGpx());
			}

			final OsmandSettings settings = app.getSettings();
			LatLon location = new LatLon(child.locationStart.lat, child.locationStart.lon);

			if (child.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
				settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
						settings.getLastKnownMapZoom(),
						new PointDescription(PointDescription.POINT_TYPE_WPT, child.name),
						false,
						child.locationStart);
			} else {
				settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
						settings.getLastKnownMapZoom(),
						new PointDescription(PointDescription.POINT_TYPE_GPX_ITEM, child.group.getGpxName()),
						false,
						child);
			}
			MapActivity.launchMapActivityMoveToTop(getActivity());
		}
	}

	class PointGPXAdapter extends ArrayAdapter<GpxDisplayItem> {

		Map<GpxDisplayItemType, List<GpxDisplayItem>> itemTypes = new LinkedHashMap<>();
		List<GpxDisplayItemType> types = new ArrayList<>();

		PointGPXAdapter(List<GpxDisplayItem> items) {
			super(getActivity(), R.layout.gpx_list_item_tab_content, items);
		}

		@Override
		public void add(GpxDisplayItem item) {
			super.add(item);
			GpxDisplayItemType type = item.group.getType();
			List<GpxDisplayItem> items = itemTypes.get(type);
			if (items == null) {
				items = new ArrayList<>();
				itemTypes.put(type, items);
			}
			items.add(item);
			if (!types.contains(type)) {
				types.add(type);
			}
		}

		@Override
		public void clear() {
			super.clear();
			types.clear();
			itemTypes.clear();
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			GpxDisplayItem child = adapter.getItem(position);

			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getMyActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.gpx_item_list_item, parent, false);
			}

			TextView label = (TextView) row.findViewById(R.id.name);
			TextView description = (TextView) row.findViewById(R.id.description);
			TextView additional = (TextView) row.findViewById(R.id.additional);
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			if (child.splitMetric >= 0 && child.splitName != null) {
				additional.setVisibility(View.VISIBLE);
				icon.setVisibility(View.INVISIBLE);
				additional.setText(child.splitName);
			} else {
				icon.setVisibility(View.VISIBLE);
				additional.setVisibility(View.INVISIBLE);
				if (child.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
					icon.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_markers_dark));
				} else {
					int groupColor = child.group.getColor();
					if (child.locationStart != null) {
						groupColor = child.locationStart.getColor(groupColor);
					}
					if (groupColor == 0) {
						groupColor = getMyActivity().getResources().getColor(R.color.gpx_track);
					}
					icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getMyActivity(), groupColor, false));
				}
			}
			row.setTag(child);

			label.setText(Html.fromHtml(child.name.replace("\n", "<br/>")));
			boolean expand = true; //child.expanded || isArgumentTrue(ARG_TO_EXPAND_TRACK_INFO)
			if (expand && !Algorithms.isEmpty(child.description)) {
				String d = child.description;
				description.setText(Html.fromHtml(d));
				description.setVisibility(View.VISIBLE);
			} else {
				description.setVisibility(View.GONE);
			}

			return row;
		}
	}
}
