package net.osmand.plus.routepointsnavigation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.*;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import net.osmand.CallbackWithObject;
import net.osmand.plus.*;
import net.osmand.plus.activities.OsmandListActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.*;

/**
 * Created by Bars on 13.06.2014.
 *
 */
public class RoutePointsActivity extends OsmandListActivity {


	private static final String CURRENT_ROUTE_KEY = "CurrentRoute";

	private GPXUtilities.GPXFile gpx;
	private RoutePointsPlugin plugin;
	private OsmandApplication app;

	private List<GPXUtilities.WptPt> sortedPointsList;

	//saves indexed of sorted list
	private List<Integer> pointsIndex;

	private int selectedItemIndex;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.setContentView(R.layout.route_steps_main);
		this.app = (OsmandApplication) getApplication();
		getPlugin();
		getGpx(false);

		if (gpx != null) {
			prepareView();
		}

		super.onCreate(savedInstanceState);
	}


	private void getPlugin() {
		List<OsmandPlugin> plugins = OsmandPlugin.getEnabledPlugins();
		for (OsmandPlugin plugin : plugins) {
			if (plugin instanceof RoutePointsPlugin) {
				this.plugin = (RoutePointsPlugin) plugin;
			}
		}

	}

	private void getGpx(boolean forced) {
		if (plugin.getGpx() != null && !forced) {
			this.gpx = plugin.getGpx();
			return;
		}

		GpxUiHelper.selectGPXFile(this, false, false, new CallbackWithObject<GPXUtilities.GPXFile[]>() {
			@Override
			public boolean processResult(GPXUtilities.GPXFile[] result) {
				gpx = result[0];
				plugin.setGpx(gpx);
				prepareView();
				return false;
			}
		});

	}

	private void prepareView() {
		TextView gpxName = (TextView) findViewById(R.id.gpx_name);
		String fileName = getString(R.string.current_route) + " " + gpx.path.substring(gpx.path.lastIndexOf("/") + 1, gpx.path.lastIndexOf("."));
		gpxName.setText(fileName);
		gpxName.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().startActionMode(mGpxActionModeCallback);
			}
		});

		TextView visited = (TextView) findViewById(R.id.points_count);
		visited.setText("(" + plugin.getVisitedAllString() + ")");

		loadCurrentRoute();
		sortPoints();
		displayListView();
	}

	private void displayListView() {
		ArrayList<PointItem> pointItemsList = new ArrayList<PointItem>();
		for (int i = 0; i < sortedPointsList.size(); i++) {
			String pointName = sortedPointsList.get(i).name;
			long time = plugin.getPointStatus(pointsIndex.get(i));
			if (time != 0) {
				String dateString;
				Date date = new Date(time);
				if (DateFormat.is24HourFormat(app)) {
					dateString = DateFormat.format("MM/dd k:mm", date).toString();
				} else {
					dateString = DateFormat.format("MM/dd h:mm", date).toString() + DateFormat.format("aa", date).toString();
				}

				pointItemsList.add(new PointItem(true, pointName, dateString));
			} else {
				if (i == 0) {
					pointItemsList.add(new PointItem(false, pointName, ""));
					continue;
				}
				GPXUtilities.WptPt first = sortedPointsList.get(i - 1);
				GPXUtilities.WptPt second = sortedPointsList.get(i);

				double d = MapUtils.getDistance(first.lat, first.lon, second.lat, second.lon);
				String distance = OsmAndFormatter.getFormattedDistance((float) d, app);

				pointItemsList.add(new PointItem(false, pointName, distance));
			}
		}

		PointItemAdapter adapter = new PointItemAdapter(this, R.layout.route_point_info, pointItemsList);
		ListView listView = getListView();
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				selectedItemIndex = i;
				view.setSelected(true);

				getActivity().startActionMode(mPointActionModeCallback);

			}
		});
	}

	private void loadCurrentRoute() {
		if (gpx.routes.size() < 1) {
			return;
		}

		Map<String, String> map = gpx.getExtensionsToRead();
		if (map.containsKey(CURRENT_ROUTE_KEY)) {
			String routeName = map.get(CURRENT_ROUTE_KEY);

			for (GPXUtilities.Route route : gpx.routes) {
				if (route.name.equals(routeName)) {
					return;
				}
			}
		}
	}

	private void sortPoints() {
		sortedPointsList = plugin.getPoints();
		List<GPXUtilities.WptPt> listToSort = new ArrayList<GPXUtilities.WptPt>();
		List<Integer> indexItemsAtTheEnd = new ArrayList<Integer>();
		pointsIndex = new ArrayList<Integer>();
		int curPointInd = plugin.getCurrentPointIndex();

		//current item should be first if it's exists
		if (curPointInd != -1) {
			pointsIndex.add(curPointInd);
			listToSort.add(plugin.getCurrentPoint());
		}

		//all not visited points should be at the top
		for (int i = 0; i < sortedPointsList.size(); i++) {
			if (i == curPointInd) {
				continue;
			}
			long status = plugin.getPointStatus(i);
			if (status == 0L) {
				listToSort.add(sortedPointsList.get(i));
				pointsIndex.add(i);
			} else {
				indexItemsAtTheEnd.add(i);
			}
		}

		List<Long> timeOfVisits = new ArrayList<Long>();

		for (Integer anIndexItemsAtTheEnd : indexItemsAtTheEnd) {
			timeOfVisits.add(plugin.getPointStatus(anIndexItemsAtTheEnd));
		}

		//sorting visited point from earliest to latest
		quickSort(timeOfVisits, indexItemsAtTheEnd, 0, indexItemsAtTheEnd.size());
		//reverting items so they will be from latest to earliest
		Collections.reverse(indexItemsAtTheEnd);

		for (int i : indexItemsAtTheEnd) {
			listToSort.add(sortedPointsList.get(i));
			pointsIndex.add(i);
		}

		sortedPointsList = listToSort;
	}

	private class PointItemAdapter extends ArrayAdapter<PointItem> {
		private ArrayList<PointItem> pointsList;

		public PointItemAdapter(Context context, int textViewResourceId, ArrayList<PointItem> pointsList) {
			super(context, textViewResourceId, pointsList);
			this.pointsList = new ArrayList<PointItem>();
			this.pointsList.addAll(pointsList);
		}

		private class ViewHolder {
			ImageView image;
			TextView name;
			TextView dateOrDistance;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			Log.v("ConvertView", String.valueOf(position));
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.route_point_info, null);

				holder = new ViewHolder();
				holder.dateOrDistance = (TextView) convertView.findViewById(R.id.date_or_distance);
				holder.name = (TextView) convertView.findViewById(R.id.name);
				holder.image = (ImageView) convertView.findViewById(R.id.point_icon);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			PointItem point = pointsList.get(position);
			holder.name.setText(point.getName());
			holder.dateOrDistance.setText(String.valueOf(point.getTime()));
			if (point.isSelected()) {
				holder.image.setImageResource(R.drawable.ic_action_ok_dark);
				holder.name.setTextColor(getResources().getColor(R.color.osmbug_closed));
				holder.dateOrDistance.setTextColor(getResources().getColor(R.color.color_unknown));
			} else {
				if (sortedPointsList.get(position).equals(plugin.getCurrentPoint())) {
					holder.image.setImageResource(R.drawable.ic_action_signpost_dark);
				} else {
					holder.image.setImageResource(R.drawable.ic_action_marker_dark);
				}
				holder.name.setTextColor(getResources().getColor(R.color.color_update));
				holder.dateOrDistance.setTextColor(getResources().getColor(R.color.osmbug_not_submitted));
			}
			return convertView;
		}
	}

	//this class needed to represent route point in UI
	private class PointItem {
		private boolean visited;
		private String name;
		private String time;

		public PointItem(boolean visited, String name, String time) {
			this.visited = visited;
			this.name = name;
			this.time = time;
		}

		public String getName() {
			return name;
		}

		public String getTime() {
			return time;
		}

		public boolean isSelected() {
			return visited;
		}

		public void setSelected(boolean selected) {
			this.visited = selected;
		}

	}

	private void revertPointStatusAsync(final GPXUtilities.WptPt point) {
		new AsyncTask<GPXUtilities.WptPt, Void, Void>() {
			private ProgressDialog dlg;

			protected void onPreExecute() {
				dlg = new ProgressDialog(getActivity());
				if (plugin.getPointStatus(point) == 0) {
					dlg.setTitle(R.string.marking_as_visited);
				} else {
					dlg.setTitle(getString(R.string.marking_as_unvisited));
				}
				dlg.setMessage(point.name);
				dlg.show();
			}

			@Override
			protected Void doInBackground(GPXUtilities.WptPt... params) {
				long status = plugin.getPointStatus(point);
				if (status == 0) {
					plugin.setPointStatus(point, true);
				} else {
					plugin.setPointStatus(point, false);
				}
				GPXUtilities.writeGpxFile(new File(gpx.path), gpx, app);
				return null;
			}

			protected void onPostExecute(Void result) {
				//to avoid illegal argument exception when rotating phone during loading
				try {
					dlg.dismiss();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				sortPoints();
				displayListView();

			}
		}.execute(point);
	}

	private ActionMode.Callback mGpxActionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			MenuItem item = menu.add(getString(R.string.select_gpx));
			item.setIcon(R.drawable.ic_action_layers_dark);
			item = menu.add(getString(R.string.start_route));
			item.setIcon(R.drawable.ic_action_map_marker_dark);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			if (menuItem.getTitle().equals(getResources().getString(R.string.select_gpx))) {
				getGpx(true);
			} else if (menuItem.getTitle().equals(getResources().getString(R.string.start_route))) {
				GPXUtilities.WptPt point = plugin.getCurrentPoint();
				if (point == null) {
					if (plugin.getPointStatus(pointsIndex.get(0)) == 0) {
						plugin.setCurrentPoint(sortedPointsList.get(0));
					}
				}
				actionMode.finish();
				finish();
			}
			actionMode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode actionMode) {

		}
	};

	private ActionMode.Callback mPointActionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			MenuItem item = menu.add(getString(R.string.mark_as_current));
			item.setIcon(R.drawable.ic_action_signpost_dark);
			item = menu.add(getString(R.string.mark_as_visited));
			item.setIcon(R.drawable.ic_action_ok_dark);
			item = menu.add(getString(R.string.show_on_map));
			item.setIcon(R.drawable.ic_action_map_marker_dark);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			if (menuItem.getTitle().equals(getResources().getString(R.string.mark_as_current))) {
				plugin.setCurrentPoint(sortedPointsList.get(selectedItemIndex));
				sortPoints();
				displayListView();
			} else if (menuItem.getTitle().equals(getResources().getString(R.string.show_on_map))) {
				GPXUtilities.WptPt point = sortedPointsList.get(selectedItemIndex);
				app.getSettings().setMapLocationToShow(point.lat, point.lon, app.getSettings().getMapZoomToShow());
				finish();
			} else {
				//inverts selection state of item
				revertPointStatusAsync(sortedPointsList.get(selectedItemIndex));
				sortPoints();
				displayListView();
			}
			actionMode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode actionMode) {

		}
	};

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		com.actionbarsherlock.view.MenuItem doneItem = menu.add(getString(R.string.done));
		doneItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		doneItem.setIcon(R.drawable.ic_action_map_marker_dark);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getTitle().equals(getString(R.string.done))) {
			GPXUtilities.WptPt point = plugin.getCurrentPoint();
			app.getSettings().setMapLocationToShow(point.lat, point.lon, app.getSettings().getMapZoomToShow());
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void quickSort(List<Long> valuesList, List<Integer> indexList, int beginIdx, int len) {
		if (len <= 1) {
			return;
		}

		final int endIdx = beginIdx + len - 1;

		// Pivot selection

		final int pivotPos = beginIdx + len / 2;

		final long pivot = valuesList.get(pivotPos);

		swap(valuesList, indexList, pivotPos, endIdx);


		// partitioning
		int p = beginIdx;

		for (int i = beginIdx; i != endIdx; ++i) {
			if (valuesList.get(i) <= pivot) {
				swap(valuesList, indexList, i, p++);
			}
		}

		swap(valuesList, indexList, p, endIdx);

		// recursive call
		quickSort(valuesList, indexList, beginIdx, p - beginIdx);
		quickSort(valuesList, indexList, p + 1, endIdx - p);

	}

	private void swap(List<Long> valuesList, List<Integer> indexList, int piviotPos, int endIndex) {
		long value = valuesList.get(piviotPos);
		valuesList.set(piviotPos, valuesList.get(endIndex));
		valuesList.set(endIndex, value);

		int index = indexList.get(piviotPos);
		indexList.set(piviotPos, indexList.get(endIndex));
		indexList.set(endIndex, index);

	}

	private Activity getActivity() {
		return this;
	}
}
