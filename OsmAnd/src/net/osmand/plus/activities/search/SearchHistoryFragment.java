package net.osmand.plus.activities.search;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.base.OsmAndListFragment;
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
	private boolean compassRegistered;
	private UpdateLocationViewCache updateLocationViewCache;
	private double lastHeading;	

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
		historyAdapter = new HistoryAdapter(helper.getHistoryEntries(true));
		setListAdapter(historyAdapter);
		setHasOptionsMenu(true);
	}


	@Override
	public void onResume() {
		super.onResume();

		//Hardy: onResume() code is needed so that search origin is properly reflected in tab contents when origin has been changed on one tab, then tab is changed to another one.
		location = null;
		FragmentActivity activity = getActivity();
		updateLocationViewCache = getMyApplication().getUIUtilities().getUpdateLocationViewCache();
		Intent intent = activity.getIntent();
		if (intent != null) {
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if (lat != 0 || lon != 0) {
				updateLocationViewCache.specialFrom = new LatLon(lat, lon);
				
			}
		}
		if (activity instanceof SearchActivity && ((SearchActivity) activity).getSearchPoint() != null) {
			updateLocationViewCache.specialFrom = ((SearchActivity) activity).getSearchPoint();
		}
		historyAdapter.clear();
		for (HistoryEntry entry : helper.getHistoryEntries(true)) {
			historyAdapter.add(entry);
		}
		locationUpdate(location);
		clearButton.setVisibility(historyAdapter.isEmpty() ? View.GONE : View.VISIBLE);
		
	}

	@Override
	public void locationUpdate(LatLon l) {
		if (getActivity() instanceof SearchActivity) {
			if (((SearchActivity) getActivity()).isSearchAroundCurrentLocation() && l != null) {
				if (!compassRegistered) {
					OsmandApplication app = getMyApplication();
					app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
					app.getLocationProvider().addCompassListener(this);
					compassRegistered = true;
				}
				updateLocationViewCache.specialFrom = null;
			} else {
				updateLocationViewCache.specialFrom = ((SearchActivity) getActivity()).getSearchPoint();
			}
		}
		if (historyAdapter != null) {
			historyAdapter.updateLocation();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if(getActivity() instanceof SearchActivity) {
			OsmandApplication app = getMyApplication();
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
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
				getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_delete_dark));
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
		

		public void updateLocation() {
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
			getMyApplication().getUIUtilities().updateLocationView(updateLocationViewCache, 
					direction, distanceText, historyEntry.getLat(),
					historyEntry.getLon());
			ImageButton options = (ImageButton) row.findViewById(R.id.options);
			options.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
			options.setVisibility(View.VISIBLE);
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectModelOptions(historyEntry, v);
				}
			});
			if (getActivity() instanceof SearchActivity)
				ViewCompat.setAccessibilityDelegate(row, ((SearchActivity)getActivity()).getAccessibilityAssistant());
			return row;
		}
	}
	
				
	public static void udpateHistoryItem(final HistoryEntry historyEntry, View row,
			LatLon location, Activity activity, OsmandApplication app) {
		TextView nameText = (TextView) row.findViewById(R.id.name);
		TextView distanceText = (TextView) row.findViewById(R.id.distance);
		ImageView direction = (ImageView) row.findViewById(R.id.direction);
		UiUtilities ic = app.getUIUtilities();
		direction.setImageDrawable(ic.getIcon(R.drawable.ic_direction_arrow, R.color.color_distance));
		String distance = "";
		if (location != null) {
			int dist = (int) (MapUtils.getDistance(location, historyEntry.getLat(), historyEntry.getLon()));
			distance = OsmAndFormatter.getFormattedDistance(dist, (OsmandApplication) activity.getApplication()) + "  ";
		}
		distanceText.setText(distance);
		PointDescription pd = historyEntry.getName();
		nameText.setText(pd.getSimpleName(activity, false), BufferType.SPANNABLE);
		ImageView icon = ((ImageView) row.findViewById(R.id.icon));
		icon.setImageDrawable(ic.getThemedIcon(getItemIcon(historyEntry.getName())));

		String typeName = historyEntry.getName().getTypeName();
		if (typeName != null && !typeName.isEmpty()) {
			ImageView group = (ImageView) row.findViewById(R.id.type_name_icon);
			group.setVisibility(View.VISIBLE);
			group.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_group_name_16));
			((TextView) row.findViewById(R.id.type_name)).setText(typeName);
		} else {
			row.findViewById(R.id.type_name_icon).setVisibility(View.GONE);
			((TextView) row.findViewById(R.id.type_name)).setText("");
		}
	}

	public static int getItemIcon(PointDescription pd) {
		int iconId;
		if (pd.isAddress()) {
			iconId = R.drawable.ic_action_street_name;
		} else if (pd.isFavorite()) {
			iconId = R.drawable.ic_action_favorite;
		} else if (pd.isLocation()) {
			iconId = R.drawable.ic_action_marker_dark;
		} else if (pd.isPoi()) {
			iconId = R.drawable.ic_action_info_dark;
		} else if (pd.isWpt()) {
			iconId = R.drawable.ic_action_flag_stroke;
		} else if (pd.isAudioNote()) {
			iconId = R.drawable.ic_type_audio;
		} else if (pd.isVideoNote()) {
			iconId = R.drawable.ic_type_video;
		} else if (pd.isPhotoNote()) {
			iconId = R.drawable.ic_type_img;
		} else {
			iconId = R.drawable.ic_action_street_name;
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
		FragmentActivity activity = getActivity();
		if (Math.abs(MapUtils.degreesDiff(lastHeading, value)) > 5) {
			lastHeading = value;
			if (activity instanceof SearchActivity) {
				((SearchActivity)activity).getAccessibilityAssistant().lockEvents();
				historyAdapter.notifyDataSetChanged();
				((SearchActivity)activity).getAccessibilityAssistant().unlockEvents();
			} else {
				historyAdapter.notifyDataSetChanged();
			}
		}
		if (activity instanceof SearchActivity) {
			final View selected = ((SearchActivity)activity).getAccessibilityAssistant().getFocusedView();
			if (selected != null) {
				try {
					int position = getListView().getPositionForView(selected);
					if ((position != AdapterView.INVALID_POSITION) && (position >= getListView().getHeaderViewsCount())) {
						HistoryEntry historyEntry = historyAdapter.getItem(position - getListView().getHeaderViewsCount());
						LatLon location = new LatLon(historyEntry.getLat(), historyEntry.getLon());
						((SearchActivity)activity).getNavigationInfo().updateTargetDirection(location, heading.floatValue());
					}
				} catch (Exception e) {
					return;
				}
			}
		}
	}
}
