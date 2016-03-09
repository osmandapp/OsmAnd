package net.osmand.plus.myplaces;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;


public class SelectedGPXFragment extends OsmAndListFragment {
	public static final String ARG_TO_EXPAND_TRACK_INFO = "ARG_TO_EXPAND_TRACK_INFO";
	public static final String ARG_TO_FILTER_SHORT_TRACKS = "ARG_TO_FILTER_SHORT_TRACKS";
	public static final String ARG_TO_HIDE_CONFIG_BTN = "ARG_TO_HIDE_CONFIG_BTN";
	protected OsmandApplication app;
	protected SelectedGPXAdapter adapter;
	protected TrackActivity activity;
	private boolean updateEnable;


	@Override
	public void onAttach(Activity activity) {
		this.activity = (TrackActivity) activity;
		super.onAttach(activity);

		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		app = (OsmandApplication) activity.getApplication();
	}

	public TrackActivity getMyActivity() {
		return activity;
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
		if(getGpx() != null && getGpx().showCurrentTrack && filterType() == GpxDisplayItemType.TRACK_POINTS) {
			startHandler();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		updateEnable = false;
	}


	protected static List<GpxDisplayGroup> filterGroups(@NonNull GpxDisplayItemType type,
														@NonNull TrackActivity trackActivity,
														@Nullable Bundle args) {
		List<GpxDisplayGroup> result = trackActivity.getResult();
		List<GpxDisplayGroup> groups = new ArrayList<GpxSelectionHelper.GpxDisplayGroup>();
		for (GpxDisplayGroup group : result) {
			boolean add = group.getType() == type || type == null;
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
		adapter = new SelectedGPXAdapter(new ArrayList<GpxSelectionHelper.GpxDisplayItem>());
		updateContent();
		setListAdapter(adapter);
	}

	protected void updateContent() {
		adapter.clear();
		List<GpxSelectionHelper.GpxDisplayGroup> groups = filterGroups(filterType(), getMyActivity(), getArguments());
		adapter.setNotifyOnChange(false);
		for(GpxDisplayItem i: flatten(groups)) {
			adapter.add(i);
		}
		adapter.setNotifyOnChange(true);
		adapter.notifyDataSetChanged();
	}

	protected GpxDisplayItemType filterType() {
		return null;
	}

	protected List<GpxDisplayItem> flatten(List<GpxDisplayGroup> groups) {
		ArrayList<GpxDisplayItem> list = new ArrayList<GpxDisplayItem>();
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
		TextView tv = new TextView(getActivity());
		tv.setText(R.string.none_selected_gpx);
		tv.setTextSize(24);
		listView.setEmptyView(tv);
		setContent();

		return view;
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	protected void saveAsFavorites(final GpxDisplayItemType gpxDisplayItemType) {
		AlertDialog.Builder b = new AlertDialog.Builder(getMyActivity());
		final EditText editText = new EditText(getMyActivity());
		final List<GpxDisplayGroup> gs = filterGroups(gpxDisplayItemType, getMyActivity(), getArguments());
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
		final List<GpxDisplayGroup> gs = filterGroups(gpxDisplayItemType, getMyActivity(), getArguments());
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
				FavouritePoint fp = new FavouritePoint(i.locationStart.lat, i.locationStart.lon, i.name,
						category);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		((TrackActivity) getActivity()).getClearToolbar(false);
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
		return ((TrackActivity)getActivity()).getGpx();
	}

	protected void selectSplitDistance() {
		final List<GpxDisplayGroup> groups = filterGroups(GpxDisplayItemType.TRACK_SEGMENT,
				getMyActivity(), getArguments());

		View view = getMyActivity().getLayoutInflater().inflate(R.layout.selected_track_edit, null);

		final TIntArrayList list = new TIntArrayList();
		final Spinner colorSpinner = (Spinner) view.findViewById(R.id.ColorSpinner);
		ColorDialogs.setupColorSpinner(getActivity(), getGpx().getColor(0), colorSpinner, list);

		final Spinner sp = (Spinner) view.findViewById(R.id.Spinner);
		AlertDialog.Builder bld = new AlertDialog.Builder(getMyActivity());
		final List<Double> distanceSplit = new ArrayList<Double>();
		final TIntArrayList timeSplit = new TIntArrayList();
		if (groups.size() == 0) {
			sp.setVisibility(View.GONE);
			view.findViewById(R.id.GpxSpinnerRow).setVisibility(View.GONE);
		} else {
			sp.setVisibility(View.VISIBLE);

			int[] checkedItem = new int[]{!groups.get(0).isSplitDistance() && !groups.get(0).isSplitTime() ? 0 : -1};
			List<String> options = new ArrayList<String>();


			options.add(app.getString(R.string.none));
			distanceSplit.add(-1d);
			timeSplit.add(-1);
			addOptionSplit(30, true, options, distanceSplit, timeSplit, checkedItem, groups); // 50 feet, 20 yards, 20
			// m
			addOptionSplit(60, true, options, distanceSplit, timeSplit, checkedItem, groups); // 100 feet, 50 yards,
			// 50 m
			addOptionSplit(150, true, options, distanceSplit, timeSplit, checkedItem, groups); // 200 feet, 100 yards,
			// 100 m
			addOptionSplit(300, true, options, distanceSplit, timeSplit, checkedItem, groups); // 500 feet, 200 yards,
			// 200 m
			addOptionSplit(600, true, options, distanceSplit, timeSplit, checkedItem, groups); // 1000 feet, 500 yards,
			// 500 m
			addOptionSplit(1500, true, options, distanceSplit, timeSplit, checkedItem, groups); // 2000 feet, 1000 yards, 1 km
			addOptionSplit(3000, true, options, distanceSplit, timeSplit, checkedItem, groups); // 1 mi, 2 km
			addOptionSplit(6000, true, options, distanceSplit, timeSplit, checkedItem, groups); // 2 mi, 5 km
			addOptionSplit(15000, true, options, distanceSplit, timeSplit, checkedItem, groups); // 5 mi, 10 km

			addOptionSplit(15, false, options, distanceSplit, timeSplit, checkedItem, groups);
			addOptionSplit(30, false, options, distanceSplit, timeSplit, checkedItem, groups);
			addOptionSplit(60, false, options, distanceSplit, timeSplit, checkedItem, groups);
			addOptionSplit(120, false, options, distanceSplit, timeSplit, checkedItem, groups);
			addOptionSplit(150, false, options, distanceSplit, timeSplit, checkedItem, groups);
			addOptionSplit(300, false, options, distanceSplit, timeSplit, checkedItem, groups);
			addOptionSplit(600, false, options, distanceSplit, timeSplit, checkedItem, groups);
			addOptionSplit(900, false, options, distanceSplit, timeSplit, checkedItem, groups);

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(getMyActivity(),
					android.R.layout.simple_spinner_item, options);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sp.setAdapter(adapter);
			if (checkedItem[0] > 0) {
				sp.setSelection(checkedItem[0]);
			}
		}

		final CheckBox vis = (CheckBox) view.findViewById(R.id.Visibility);
		vis.setChecked(app.getSelectedGpxHelper().getSelectedFileByPath(getGpx().path) != null);

		bld.setView(view);
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(getGpx(), vis.isChecked(), false);
				int clr = list.get(colorSpinner.getSelectedItemPosition());
				if (vis.isChecked() && clr != 0 && sf.getModifiableGpxFile() != null) {
					sf.getModifiableGpxFile().setColor(clr);
					sf.processPoints();
				}
				if (groups.size() > 0) {
					updateSplit(groups, distanceSplit, timeSplit, sp.getSelectedItemPosition(), vis.isChecked() ? sf
							: null);
				}
				if (vis.isChecked() && sf.getGpxFile() != null) {
					if (groups.size() > 0 && groups.get(0).getModifiableList().size() > 0) {
						GpxDisplayItem item = groups.get(0).getModifiableList().get(0);
						app.getSettings().setMapLocationToShow(item.locationStart.lat, item.locationStart.lon,
								15,
								new PointDescription(PointDescription.POINT_TYPE_GPX_ITEM, item.group.getGpxName()),
								false,
								item); //$NON-NLS-1$
					} else {
						WptPt wpt = sf.getGpxFile().findPointToShow();
						if (wpt != null) {
							app.getSettings().setMapLocationToShow(wpt.getLatitude(), wpt.getLongitude(),
									15,
									new PointDescription(PointDescription.POINT_TYPE_WPT, wpt.name),
									false,
									wpt); //$NON-NLS-1$
						}
					}
					MapActivity.launchMapActivityMoveToTop(activity);
				}
			}
		});

		bld.show();

	}

	private void updateSplit(List<GpxDisplayGroup> groups, List<Double> distanceSplit,
							 TIntArrayList timeSplit, int which, SelectedGpxFile sf) {
		new SplitTrackAsyncTask(sf, this, getMyActivity(), groups, distanceSplit, timeSplit, which)
				.execute((Void) null);
	}

	private void addOptionSplit(int value, boolean distance, List<String> options, List<Double> distanceSplit,
								TIntArrayList timeSplit, int[] checkedItem, List<GpxDisplayGroup> model) {
		if (distance) {
			double dvalue = OsmAndFormatter.calculateRoundedDist(value, app);
			options.add(OsmAndFormatter.getFormattedDistance((float) dvalue, app));
			distanceSplit.add(dvalue);
			timeSplit.add(-1);
			if (Math.abs(model.get(0).getSplitDistance() - dvalue) < 1) {
				checkedItem[0] = distanceSplit.size() - 1;
			}
		} else {
			if (value < 60) {
				options.add(value + " " + app.getString(R.string.int_seconds));
			} else if (value % 60 == 0) {
				options.add((value / 60) + " " + app.getString(R.string.int_min));
			} else {
				options.add((value / 60f) + " " + app.getString(R.string.int_min));
			}
			distanceSplit.add(-1d);
			timeSplit.add(value);
			if (model.get(0).getSplitTime() == value) {
				checkedItem[0] = distanceSplit.size() - 1;
			}
		}

	}

	class SelectedGPXAdapter extends ArrayAdapter<GpxDisplayItem> {

		public SelectedGPXAdapter(List<GpxDisplayItem> items) {
			super(getActivity(), R.layout.gpx_item_list_item, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getMyActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.gpx_item_list_item, parent, false);
			}
			GpxDisplayItem child = getItem(position);
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
				if (child.group.getType() == GpxDisplayItemType.TRACK_SEGMENT) {
					icon.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_action_polygom_dark));
				} else if (child.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
					icon.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_action_markers_dark));
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

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		GpxDisplayItem child = adapter.getItem(position);

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
		} else {
			settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
					settings.getLastKnownMapZoom(),
					new PointDescription(PointDescription.POINT_TYPE_GPX_ITEM, child.group.getGpxName()),
					false,
					child);
		}
		MapActivity.launchMapActivityMoveToTop(getActivity());
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

	public static class SplitTrackAsyncTask extends AsyncTask<Void, Void, Void> {
		@Nullable private final SelectedGpxFile mSelectedGpxFile;
		@NonNull private final SelectedGPXFragment mFragment;
		@NonNull private final TrackActivity mActivity;

		private final List<GpxDisplayGroup> groups;
		private final List<Double> distanceSplit;
		private final TIntArrayList timeSplit;
		private final int which;

		public SplitTrackAsyncTask(@Nullable SelectedGpxFile selectedGpxFile,
								   SelectedGPXFragment fragment,
								   TrackActivity activity,
								   List<GpxDisplayGroup> groups,
								   List<Double> distanceSplit,
								   TIntArrayList timeSplit,
								   int which) {
			mSelectedGpxFile = selectedGpxFile;
			mFragment = fragment;
			mActivity = activity;
			this.groups = groups;
			this.distanceSplit = distanceSplit;
			this.timeSplit = timeSplit;
			this.which = which;
		}

		protected void onPostExecute(Void result) {
			if (mSelectedGpxFile != null) {
				mSelectedGpxFile.setDisplayGroups(filterGroups(null, mActivity, mFragment.getArguments()));
			}
			if (mFragment.isVisible()) {
				mFragment.updateContent();
			}
			if (!mActivity.isFinishing()) {
				mActivity.setProgressBarIndeterminateVisibility(false);
			}
		}

		protected void onPreExecute() {
			mActivity.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (GpxDisplayGroup model : groups) {
				OsmandApplication application = mActivity.getMyApplication();
				if (which == 0) {
					model.noSplit(application);
				} else if (distanceSplit.get(which) > 0) {
					model.splitByDistance(application, distanceSplit.get(which));
				} else if (timeSplit.get(which) > 0) {
					model.splitByTime(application, timeSplit.get(which));
				}
			}

			return null;
		}
	}
}
