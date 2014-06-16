package net.osmand.plus.routepointsnavigation;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import net.osmand.CallbackWithObject;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandExpandableListActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import org.w3c.dom.Text;

import java.io.File;
import java.util.*;

/**
 * Created by Bars on 13.06.2014.
 */
public class RoutePointsActivity extends SherlockFragmentActivity {


	private static final String CURRENT_ROUTE_KEY = "CurrentRoute";

	private File file;
	private GPXUtilities.GPXFile gpx;
	private OsmandApplication app;

	private GPXUtilities.Route currentRoute;
	private List<GPXUtilities.WptPt> pointsList;

	private List<Long> pointsStatus;

	//saves indexed of sorted list
	private List<Integer> pointsIndex;

	//needed to save user selection
	private List<Boolean> pointsChangedState;
	private List<Boolean> pointsStartState;

	private RoutePointsPlugin plugin;

	private int selectedItemIndex;

	private ListView listView;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.setContentView(R.layout.route_steps_main);
		this.app = (OsmandApplication) getApplication();
		getPlugin();
		getGpx();

		if (gpx != null) {
			prepareView();
		}

		Button done = (Button) findViewById(R.id.done);
		done.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				saveStatus();
				GPXUtilities.WptPt point = plugin.getCurrentPoint();
				app.getSettings().setMapLocationToShow(point.lat, point.lon, app.getSettings().getMapZoomToShow());
				finish();
			}
		});

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

	private void getGpx() {
		if (plugin.getGpx() != null) {
			this.gpx = plugin.getGpx();
		} else {
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
	}

	private void prepareView() {
		TextView gpxName = (TextView) findViewById(R.id.gpx_name);
		String fileName = gpx.path.substring(gpx.path.lastIndexOf("/") + 1,gpx.path.lastIndexOf("."));
		gpxName.setText(fileName);

		TextView visited = (TextView) findViewById(R.id.points_count);
		visited.setText(plugin.getVisitedAllString());

		loadCurrentRoute();
		pointsList = currentRoute.points;
		sortPoints();
		pointsStatus = getAllPointsStatus();
		pointsStartState = getPointsState();
		pointsChangedState = new ArrayList<Boolean>(pointsStartState);
		displayListView();
	}

	private void displayListView() {
		ArrayList<PointItem> pointItemsList = new ArrayList<PointItem>();
		for (int i = 0; i < pointsList.size(); i++) {
			String pointName = pointsList.get(i).name;
			if (pointsStatus.get(i) != 0) {
				String dateString = DateFormat.format("MM/dd/yyyy hh:mm:ss", new Date(pointsStatus.get(i))).toString();
				pointItemsList.add(new PointItem(true, pointName, dateString));
			} else {
				pointItemsList.add(new PointItem(false, pointName, ""));
			}
		}

		PointItemAdapter adapter = new PointItemAdapter(this, R.layout.route_point_info, pointItemsList);
		listView = (ListView) findViewById(R.id.pointsListView);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				selectedItemIndex = i;
				final PopupMenu popup = new PopupMenu(RoutePointsActivity.this, view);
				final Menu menu = popup.getMenu();
				menu.add(getString(R.string.mark_as_current));
				menu.add(getString(R.string.mark_as_visited));
				menu.add(getString(R.string.show_on_map));


				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem menuItem) {
						if (menuItem.getTitle().equals(getResources().getString(R.string.mark_as_current))) {
							plugin.setCurrentPoint(pointsList.get(selectedItemIndex));

						} else if (menuItem.getTitle().equals(getResources().getString(R.string.show_on_map))) {
							GPXUtilities.WptPt point = pointsList.get(selectedItemIndex);
							app.getSettings().setMapLocationToShow(point.lat, point.lon, app.getSettings().getMapZoomToShow());
							finish();
						} else {
							//inverts selection state of item
							boolean state = pointsChangedState.get(selectedItemIndex);
							pointsChangedState.set(selectedItemIndex,!state);
						}
						return true;
					}
				});

				popup.show();
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
					currentRoute = route;
					return;
				}
			}
		}
		currentRoute = gpx.routes.get(0);
	}

	private List<Long> getAllPointsStatus() {
		List<Long> pointsStatus = new ArrayList<Long>();
		for (int i = 0; i < pointsList.size(); i++) {
			pointsStatus.add(plugin.getPointStatus(pointsIndex.get(i)));
		}

		return pointsStatus;
	}

	private void saveStatus() {
		for (int i = 0; i < pointsChangedState.size(); i++) {
			boolean newValue = pointsChangedState.get(i);
			//if values is the same - there's no need to save data
			if (newValue != pointsStartState.get(i)) {
				int indexToWrite = pointsIndex.get(i);
				plugin.setPointStatus(indexToWrite, newValue);
			}
		}

		saveGPXFile();
	}

	private void sortPoints() {
		List<GPXUtilities.WptPt> listToSort = new ArrayList<GPXUtilities.WptPt>();
		List<Integer> indexItemsAtTheEnd = new ArrayList<Integer>();
		pointsIndex = new ArrayList<Integer>();


		for (int i = 0; i < pointsList.size(); i++) {
			long status = plugin.getPointStatus(i);
			if (status == 0L) {
				listToSort.add(pointsList.get(i));
				pointsIndex.add(i);
			} else {
				indexItemsAtTheEnd.add(i);
			}
		}

		for (int i : indexItemsAtTheEnd) {
			listToSort.add(pointsList.get(i));
			pointsIndex.add(i);
		}

		pointsList = listToSort;
	}

	private void saveGPXFile() {
		GPXUtilities.writeGpxFile(new File(gpx.path), gpx, app);
	}

	public List<Boolean> getPointsState() {
		List<Boolean> status = new ArrayList<Boolean>();
		for (int i = 0; i < pointsStatus.size(); i++) {
			if (pointsStatus.get(i) == 0) {
				status.add(false);
			} else {
				status.add(true);
			}
		}
		return status;
	}

	private boolean[] toPrimitiveArray(final List<Boolean> booleanList) {
		final boolean[] primitives = new boolean[booleanList.size()];
		int index = 0;
		for (Boolean object : booleanList) {
			primitives[index++] = object;
		}
		return primitives;
	}

	private class PointItemAdapter extends ArrayAdapter<PointItem> {
		private RoutePointsActivity ctx;
		private ArrayList<PointItem> pointsList;

		public PointItemAdapter(Context context, int textViewResourceId, ArrayList<PointItem> pointsList) {
			super(context, textViewResourceId, pointsList);
			ctx = (RoutePointsActivity) context;
			this.pointsList = new ArrayList<PointItem>();
			this.pointsList.addAll(pointsList);
		}

		private class ViewHolder {
			TextView index;
			TextView name;
			TextView date;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			Log.v("ConvertView", String.valueOf(position));
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.route_point_info, null);

				holder = new ViewHolder();
				holder.index = (TextView) convertView.findViewById(R.id.index);
				holder.date = (TextView) convertView.findViewById(R.id.date);
				holder.name = (TextView) convertView.findViewById(R.id.name);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			PointItem point = pointsList.get(position);
			holder.index.setText(String.valueOf(position));
			String pointName = point.getName();
			int pos = pointName.indexOf(":");
			holder.name.setText(pointName.substring(0, pos));
			if (point.isSelected()) {
				holder.name.setTextColor(getResources().getColor(R.color.osmbug_closed));
				holder.date.setText(String.valueOf(point.getTime()));
			} else {
				holder.name.setTextColor(getResources().getColor(R.color.color_update));
				holder.date.setText("");
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
}
