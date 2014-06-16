package net.osmand.plus.routesteps;

import alice.tuprolog.Int;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.views.ContextMenuLayer;

import java.io.File;
import java.util.*;

/**
 * Created by Bars on 13.06.2014.
 */
public class RouteStepsActivity extends SherlockFragmentActivity {

	private static final String VISITED_KEY = "IsVisited";
	private static final String POINT_KEY = "Point";
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

	private RouteStepsPlugin plugin;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.setContentView(R.layout.route_steps_main);
		this.app = (OsmandApplication) getApplication();
		getPlugin();
		getGpx();

		if (gpx != null){
			preparePoints();
		}

		Button done = (Button) findViewById(R.id.done);
		done.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				saveStatus();
			}
		});

		super.onCreate(savedInstanceState);
	}


	private void getPlugin(){
		List<OsmandPlugin> plugins = OsmandPlugin.getEnabledPlugins();
		for (OsmandPlugin plugin: plugins){
			if (plugin instanceof RouteStepsPlugin){
				this.plugin = (RouteStepsPlugin) plugin;
			}
		}

	}

	private void getGpx(){
		if (plugin.getGpx() != null){
			this.gpx = plugin.getGpx();
		} else {
			GpxUiHelper.selectGPXFile(this,false,false, new CallbackWithObject<GPXUtilities.GPXFile[]>() {
				@Override
				public boolean processResult(GPXUtilities.GPXFile[] result) {
					gpx = result[0];
					preparePoints();
					plugin.setGpx(gpx);
					return false;
				}
			});
		}
	}

	private void preparePoints(){
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
				String dateString= DateFormat.format("MM/dd/yyyy hh:mm:ss", new Date(pointsStatus.get(i))).toString();
				pointItemsList.add(new PointItem(true, pointName, dateString));
			} else {
				pointItemsList.add(new PointItem(false, pointName, ""));
			}
		}

		PointItemAdapter adapter = new PointItemAdapter(this, R.layout.route_point_info, pointItemsList);
		final ListView listView = (ListView) findViewById(R.id.pointsListView);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				final PopupMenu menu = new PopupMenu(RouteStepsActivity.this, view);

				menu.getMenuInflater().inflate(R.menu.route_step_menu, menu.getMenu());

				menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem menuItem) {
						if (menuItem.getTitle().equals("Mark as next")){

						} else {
							//AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
							///int position = info.position;
							//GPXUtilities.WptPt point = pointsList.get(position);
							//app.getSettings().setMapLocationToShow();
						}
						return true;
					}
				});

				menu.show();
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
			pointsStatus.add(getPointStatus(pointsIndex.get(i)));
		}

		return pointsStatus;
	}

	private void saveStatus(){
		for (int i = 0; i < pointsChangedState.size(); i++) {
			boolean newValue = pointsChangedState.get(i);
			//if values is the same - there's no need to save data
			if (newValue != pointsStartState.get(i)) {
				int indexToWrite = pointsIndex.get(i);
				setPointStatus(indexToWrite, newValue);
			}
		}

		saveGPXFile();
		finish();
	}

	private void sortPoints(){
		List<GPXUtilities.WptPt> listToSort = new ArrayList<GPXUtilities.WptPt>();
		List<Integer> indexItemsAtTheEnd = new ArrayList<Integer>();
		pointsIndex = new ArrayList<Integer>();


		for (int i =0; i< pointsList.size(); i++){
			long status = getPointStatus(i);
			if (status == 0L){
				listToSort.add(pointsList.get(i));
				pointsIndex.add(i);
			} else{
				indexItemsAtTheEnd.add(i);
			}
		}

		for (int i : indexItemsAtTheEnd){
			listToSort.add(pointsList.get(i));
			pointsIndex.add(i);
		}

		pointsList = listToSort;
	}

	private void saveGPXFile() {
		GPXUtilities.writeGpxFile(new File(gpx.path), gpx, app);
	}

	private long getPointStatus(int numberOfPoint) {
		Map<String, String> map = currentRoute.getExtensionsToRead();

		String mapKey = POINT_KEY + numberOfPoint + VISITED_KEY;
		if (map.containsKey(mapKey)) {
			String value = map.get(mapKey);
			return (Long.valueOf(value));
		}

		return 0L;
	}

	//saves point status value to gpx extention file
	private void setPointStatus(int numberOfPoint, boolean status) {
		Map<String, String> map = currentRoute.getExtensionsToWrite();

		String mapKey = POINT_KEY + numberOfPoint + VISITED_KEY;
		if (status) {
			//value is current time
			Calendar c = Calendar.getInstance();
			long number = c.getTimeInMillis();
			map.put(mapKey, String.valueOf(number));
		} else if (map.containsKey(mapKey)) {
			map.remove(mapKey);
		}

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
		private RouteStepsActivity ctx;
		private ArrayList<PointItem> pointsList;

		public PointItemAdapter(Context context, int textViewResourceId, ArrayList<PointItem> pointsList) {
			super(context, textViewResourceId, pointsList);
			ctx = (RouteStepsActivity) context;
			this.pointsList = new ArrayList<PointItem>();
			this.pointsList.addAll(pointsList);
		}

		private class ViewHolder {
			TextView index;
			TextView name;
			TextView date;
			CheckBox visited;

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
				holder.visited = (CheckBox) convertView.findViewById(R.id.checkBox1);
				convertView.setTag(holder);

				holder.visited.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						CheckBox ch = (CheckBox) view;
						RelativeLayout parent = (RelativeLayout) ch.getParent();
						TextView text = (TextView) parent.getChildAt(0);
						pointsChangedState.set(Integer.parseInt(text.getText().toString()), ch.isChecked());
					}
				});
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			PointItem point = pointsList.get(position);
			holder.index.setText(String.valueOf(position));
			holder.visited.setChecked(point.isSelected());
			String pointName = point.getName();
			int pos = pointName.indexOf(":");
			holder.name.setText(pointName.substring(0, pos));
			if (point.isSelected()){
				holder.date.setText(String.valueOf(point.getTime()));
			} else{
				holder.date.setText("");
			}

			return convertView;
		}

	}

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
