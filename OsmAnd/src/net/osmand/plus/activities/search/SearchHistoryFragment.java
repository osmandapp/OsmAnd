package net.osmand.plus.activities.search;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.util.MapUtils;

import java.util.List;


public class SearchHistoryFragment extends OsmAndListFragment implements SearchActivityChild, OsmAndCompassListener  {
	private LatLon location;
	private SearchHistoryHelper helper;
	private Button clearButton;
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;
	private HistoryAdapter historyAdapter;
	private Float heading;
	private boolean searchAroundLocation;
	private boolean compassRegistered;
	private int screenOrientation;	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.search_history, container, false);
		clearButton = (Button) view.findViewById(R.id.clearAll);
		clearButton.setText(R.string.shared_string_clear_all);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clearWithConfirmation();
			}
		});
		((ListView)view.findViewById(android.R.id.list)).setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				onListItemClick((ListView) parent, view, position, id);
			}
		});
		
		return view;
	}
	
	private void clearWithConfirmation() {
		AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
		bld.setMessage(R.string.confirmation_to_clear_history);
		bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				clearWithoutConfirmation();
			}
		});
		bld.setNegativeButton(R.string.shared_string_no, null);
		bld.show();
	}
	
	@Override
	public ArrayAdapter<?> getAdapter() {
		return historyAdapter;
	}

	private void clearWithoutConfirmation() {
		helper.removeAll();
		historyAdapter.clear();
		clearButton.setVisibility(View.GONE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper = SearchHistoryHelper.getInstance((OsmandApplication) getActivity().getApplicationContext());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		historyAdapter = new HistoryAdapter(helper.getHistoryEntries());
		setListAdapter(historyAdapter);
		setHasOptionsMenu(true);
	}


	@Override
	public void onResume() {
		super.onResume();

		//Hardy: onResume() code is needed so that search origin is properly reflected in tab contents when origin has been changed on one tab, then tab is changed to another one.
		location = null;
		FragmentActivity activity = getActivity();
		Intent intent = activity.getIntent();
		if (intent != null) {
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if (lat != 0 || lon != 0) {
				historyAdapter.location = new LatLon(lat, lon);
			}
		}
		if (location == null && activity instanceof SearchActivity) {
			location = ((SearchActivity) activity).getSearchPoint();
		}
		if (location == null) {
			location = ((OsmandApplication) activity.getApplication()).getSettings().getLastKnownMapLocation();
		}
		historyAdapter.clear();
		for (HistoryEntry entry : helper.getHistoryEntries()) {
			historyAdapter.add(entry);
		}
		locationUpdate(location);
		clearButton.setVisibility(historyAdapter.isEmpty() ? View.GONE : View.VISIBLE);
		screenOrientation = DashLocationFragment.getScreenOrientation(getActivity());
	}

	@Override
	public void locationUpdate(LatLon l) {
		//location = l;
		if (getActivity() instanceof SearchActivity) {
			if (((SearchActivity) getActivity()).isSearchAroundCurrentLocation() && l != null) {
				if (!compassRegistered) {
					((OsmandApplication) getActivity().getApplication()).getLocationProvider().addCompassListener(this);
					compassRegistered = true;
				}
				searchAroundLocation = true;
			} else {
				searchAroundLocation = false;
			}
		}
		if (historyAdapter != null) {
			historyAdapter.updateLocation(l);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if(getActivity() instanceof SearchActivity) {
			((OsmandApplication) getActivity().getApplication()).getLocationProvider().removeCompassListener(this);
			compassRegistered = false;
		}
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		HistoryEntry model = ((HistoryAdapter) getListAdapter()).getItem(position);
		selectModel(model);
	}

	private void selectModel(final HistoryEntry model) {
		PointDescription name = model.getName();
		OsmandSettings settings = ((OsmandApplication) getActivity().getApplication()).getSettings();

		LatLon location = new LatLon(model.getLat(), model.getLon());

		settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
				settings.getLastKnownMapZoom(),
				name,
				true,
				model); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	private void selectModelOptions(final HistoryEntry model, View v) {
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		MenuItem item = optionsMenu.getMenu().add(
				R.string.shared_string_remove).setIcon(
				getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				helper.remove(model);
				historyAdapter.remove(model);
				return true;
			}
		});
		optionsMenu.show();
	}

	class HistoryAdapter extends ArrayAdapter<HistoryEntry> {
		private LatLon location;
		

		public void updateLocation(LatLon l) {
			location = l;
			notifyDataSetChanged();
		}

		public HistoryAdapter(List<HistoryEntry> list) {
			super(getActivity(), R.layout.search_history_list_item, list);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.search_history_list_item, parent, false);
			}
			final HistoryEntry historyEntry = getItem(position);
			udpateHistoryItem(historyEntry, row, location, getActivity(), getMyApplication());
			TextView distanceText = (TextView) row.findViewById(R.id.distance);
			ImageView direction = (ImageView) row.findViewById(R.id.direction);
			DashLocationFragment.updateLocationView(!searchAroundLocation, location, heading, direction, distanceText, 
					historyEntry.getLat(), historyEntry.getLon(), screenOrientation, getMyApplication(), getActivity()); 
			ImageButton options = (ImageButton) row.findViewById(R.id.options);
			options.setImageDrawable(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_overflow_menu_white));
			options.setVisibility(View.VISIBLE);
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectModelOptions(historyEntry, v);
				}
			});
			return row;
		}
	}
	
				
	public static void udpateHistoryItem(final HistoryEntry historyEntry, View row,
			LatLon location, Activity activity, OsmandApplication app) {
		TextView nameText = (TextView) row.findViewById(R.id.name);
		TextView distanceText = (TextView) row.findViewById(R.id.distance);
		ImageView direction = (ImageView) row.findViewById(R.id.direction);
		IconsCache ic = app.getIconsCache();
		direction.setImageDrawable(ic.getIcon(R.drawable.ic_destination_arrow_white, R.color.color_distance));
		String distance = "";
		if (location != null) {
			int dist = (int) (MapUtils.getDistance(location, historyEntry.getLat(), historyEntry.getLon()));
			distance = OsmAndFormatter.getFormattedDistance(dist, (OsmandApplication) activity.getApplication()) + "  ";
		}
		distanceText.setText(distance);
		PointDescription pd = historyEntry.getName();
		nameText.setText(pd.getSimpleName(activity, false), BufferType.SPANNABLE);
		ImageView icon = ((ImageView) row.findViewById(R.id.icon));
		icon.setImageDrawable(ic.getContentIcon(getItemIcon(historyEntry.getName())));

		String typeName = historyEntry.getName().getTypeName();
		if (typeName != null && !typeName.isEmpty()) {
			ImageView group = (ImageView) row.findViewById(R.id.type_name_icon);
			group.setVisibility(View.VISIBLE);
			group.setImageDrawable(ic.getContentIcon(R.drawable.ic_small_group));
			((TextView) row.findViewById(R.id.type_name)).setText(typeName);
		} else {
			row.findViewById(R.id.type_name_icon).setVisibility(View.GONE);
			((TextView) row.findViewById(R.id.type_name)).setText("");
		}
	}

	public static int getItemIcon(PointDescription pd) {
		int iconId;
		if (pd.isAddress()) {
			iconId = R.drawable.ic_type_address;
		} else if (pd.isFavorite()) {
			iconId = R.drawable.ic_type_favorites;
		} else if (pd.isLocation()) {
			iconId = R.drawable.ic_type_coordinates;
		} else if (pd.isPoi()) {
			iconId = R.drawable.ic_type_info;
		} else if (pd.isWpt()) {
			iconId = R.drawable.ic_type_waypoint;
		} else if (pd.isAudioNote()) {
			iconId = R.drawable.ic_type_audio;
		} else if (pd.isVideoNote()) {
			iconId = R.drawable.ic_type_video;
		}else if (pd.isPhotoNote()) {
			iconId = R.drawable.ic_type_img;
		}  else {
			iconId = R.drawable.ic_type_address;
		}
		return iconId;
	}

	@Override
	public void onCreateOptionsMenu(Menu onCreate, MenuInflater inflater) {
		if (getActivity() instanceof SearchActivity) {
			((SearchActivity) getActivity()).getClearToolbar(false);
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}
	
	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initalize arrows (with reference vs. fixed-north direction) on non-compass
		// devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (heading != null && Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			 historyAdapter.notifyDataSetChanged();
		} else {
			heading = lastHeading;
		}
	}
}
