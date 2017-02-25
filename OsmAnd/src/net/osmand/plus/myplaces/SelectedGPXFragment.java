package net.osmand.plus.myplaces;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GPXDatabase;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.OsmAndListFragment;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SelectedGPXFragment extends OsmAndListFragment {
	public static final String ARG_TO_FILTER_SHORT_TRACKS = "ARG_TO_FILTER_SHORT_TRACKS";
	protected OsmandApplication app;
	protected ArrayAdapter<GpxDisplayItem> adapter;
	private boolean updateEnable;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		app = (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(getResources().getColor(
				getMyApplication().getSettings().isLightContent() ? R.color.ctx_menu_info_view_bg_light
						: R.color.ctx_menu_info_view_bg_dark));
	}

	public TrackActivity getMyActivity() {
		return (TrackActivity) getActivity();
	}

	@Override
	public ArrayAdapter<?> getAdapter() {
		return adapter;
	}


	private void startHandler() {
		Handler updateCurrentRecordingTrack = new Handler();
		updateCurrentRecordingTrack.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (updateEnable) {
					updateContent();
					adapter.notifyDataSetChanged();
					startHandler();
				}
			}
		}, 2000);
	}

	public static boolean isArgumentTrue(@Nullable Bundle args, @NonNull String arg) {
		return args != null && args.getBoolean(arg);
	}


	@Override
	public void onResume() {
		super.onResume();
		updateContent();
		updateEnable = true;
		if (getGpx() != null && getGpx().showCurrentTrack && hasFilterType(GpxDisplayItemType.TRACK_POINTS)) {
			startHandler();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		updateEnable = false;
	}


	protected static List<GpxDisplayGroup> filterGroups(GpxDisplayItemType[] types,
														@NonNull TrackActivity trackActivity,
														@Nullable Bundle args) {
		List<GpxDisplayGroup> result = trackActivity.getGpxFile();
		List<GpxDisplayGroup> groups = new ArrayList<>();
		for (GpxDisplayGroup group : result) {
			boolean add = types == null || hasFilterType(types, group.getType());
			if (isArgumentTrue(args, ARG_TO_FILTER_SHORT_TRACKS)) {
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
		adapter = createSelectedGPXAdapter();
		updateContent();
		setupListView(listView);
		setListAdapter(adapter);
	}

	protected void setupListView(ListView listView) {
		if (adapter.getCount() > 0) {
			listView.addFooterView(getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false));
		}
	}

	protected void updateContent() {
		adapter.clear();
		List<GpxDisplayGroup> groups = filterGroups(filterTypes(), getMyActivity(), getArguments());
		adapter.setNotifyOnChange(false);
		for (GpxDisplayItem i : flatten(groups)) {
			adapter.add(i);
		}
		adapter.setNotifyOnChange(true);
		adapter.notifyDataSetChanged();
	}

	protected GpxDisplayItemType[] filterTypes() {
		return null;
	}

	protected boolean hasFilterType(GpxDisplayItemType filterType) {
		for (GpxDisplayItemType type : filterTypes()) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	protected static boolean hasFilterType(GpxDisplayItemType[] filterTypes, GpxDisplayItemType filterType) {
		for (GpxDisplayItemType type : filterTypes) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	protected List<GpxDisplayItem> flatten(List<GpxDisplayGroup> groups) {
		ArrayList<GpxDisplayItem> list = new ArrayList<>();
		for(GpxDisplayGroup g : groups) {
			list.addAll(g.getModifiableList());
		}
		return list;
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



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		getMyActivity().getClearToolbar(false);
		if (getGpx() != null && getGpx().path != null && !getGpx().showCurrentTrack) {
			MenuItem item = menu.add(R.string.shared_string_share).setIcon(R.drawable.ic_action_gshare_dark)
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							final Uri fileUri = Uri.fromFile(new File(getGpx().path));
							final Intent sendIntent = new Intent(Intent.ACTION_SEND);
							sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
							sendIntent.setType("application/gpx+xml");
							startActivity(sendIntent);
							return true;
						}
					});
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}

	protected GPXFile getGpx() {
		return getMyActivity().getGpx();
	}

	protected GpxDataItem getGpxDataItem() {
		return getMyActivity().getGpxDataItem();
	}

	public ArrayAdapter<GpxDisplayItem> createSelectedGPXAdapter() {
		return null;
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

			if (child.group.getType() == GpxDisplayItemType.TRACK_POINTS) {
				settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
						settings.getLastKnownMapZoom(),
						new PointDescription(PointDescription.POINT_TYPE_WPT, child.locationStart.name),
						false,
						child.locationStart);
			} else if (child.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
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
/*
//		if(child.group.getType() == GpxDisplayItemType.TRACK_POINTS ||
//				child.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
			ContextMenuAdapter qa = new ContextMenuAdapter(v.getContext());
			qa.setAnchor(v);
			PointDescription name = new PointDescription(PointDescription.POINT_TYPE_FAVORITE, Html.fromHtml(child.name).toString());
			LatLon location = new LatLon(child.locationStart.lat, child.locationStart.lon);
			OsmandSettings settings = app.getSettings();
			final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
			DirectionsDialogs.createDirectionActionsPopUpMenu(optionsMenu, location, child.locationStart, name, settings.getLastKnownMapZoom(),
					getActivity(), false, false);
			optionsMenu.show();
//		} else {
//			child.expanded = !child.expanded;
//			adapter.notifyDataSetInvalidated();
//		}
*/
	}
}
