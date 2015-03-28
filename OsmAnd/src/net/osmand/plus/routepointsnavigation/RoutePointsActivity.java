package net.osmand.plus.routepointsnavigation;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.view.*;
import android.widget.*;
import net.osmand.CallbackWithObject;
import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandListActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routepointsnavigation.RoutePointsPlugin.RoutePoint;
import net.osmand.plus.routepointsnavigation.RoutePointsPlugin.SelectedRouteGpxFile;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;


/**
 * Created by Bars on 13.06.2014.
 *
 */
public class RoutePointsActivity extends OsmandListActivity {

	private static final int NAVIGATE_DIALOG_ID = 4;
	private static final int OK_ID = 5;
	protected static final int MARK_AS_CURRENT_ID = 6;
	protected static final int AS_VISITED_ID = 7;
	protected static final int POI_ON_MAP_ID = 8;
	private RoutePointsPlugin plugin;
	private OsmandApplication app;

	private RoutePoint selectedItem;
	private PointItemAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.app = (OsmandApplication) getApplication();
		plugin = OsmandPlugin.getEnabledPlugin(RoutePointsPlugin.class);
		super.onCreate(savedInstanceState);
		setSupportProgressBarIndeterminateVisibility(false);
		getSupportActionBar().setTitle(R.string.route_points_activity);
		super.setContentView(R.layout.route_steps_main);
		if (plugin.getCurrentRoute() == null) {
			selectGPX();
		} else {
			prepareView();
		}
	}


	private void selectGPX() {
		GpxUiHelper.selectGPXFile(this, false, false, new CallbackWithObject<GPXUtilities.GPXFile[]>() {
			@Override
			public boolean processResult(GPXUtilities.GPXFile[] result) {
				final GPXFile gpx = result[0];
				app.getSelectedGpxHelper().clearAllGpxFileToShow();
				app.getSelectedGpxHelper().selectGpxFile(gpx, true, true);
				plugin.setCurrentRoute(gpx);
				SelectedRouteGpxFile sgpx = plugin.getCurrentRoute();
				if (!sgpx.getCurrentPoints().isEmpty() && 
						!sgpx.getCurrentPoints().get(0).isNextNavigate){
					sgpx.navigateToNextPoint();
				}
				prepareView();
				return false;
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(plugin.getCurrentRoute() != null) {
			plugin.getCurrentRoute().updateCurrentTargetPoint();
		}
	}


	private void prepareView() {
		TextView gpxName = (TextView) findViewById(R.id.gpx_name);
		TextView visited = (TextView) findViewById(R.id.points_count);
		String visitedString = "(" + plugin.getVisitedAllString() + ")";
		visited.setText(visitedString);

		SelectedRouteGpxFile route = plugin.getCurrentRoute();
		String fileName;
		if(route != null) {
			fileName = getString(R.string.rp_current_route) + " " + route.getName();
		} else {
			fileName = getString(R.string.rp_current_route_not_available);
		}
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		float screenWight = displaymetrics.widthPixels - visited.getPaint().measureText(visitedString) - 15;
		Paint textPaint = gpxName.getPaint();
		String name = fileName;
		int i = fileName.length()-1;
		for(;;){
			float textSize = textPaint.measureText(name);
			if (textSize < screenWight){
				break;
			}
			name = fileName.substring(0, i);
			i--;
		}


		SpannableString content = new SpannableString(name);
		content.setSpan(new ClickableSpan() {

			@Override
			public void onClick(View widget) {
				selectGPX();
			}
		}, 0, content.length(), 0);
		gpxName.setText(content);
		gpxName.setMovementMethod(LinkMovementMethod.getInstance());

		adapter = new PointItemAdapter(this, R.layout.route_point_info,
				route == null ? new ArrayList<RoutePoint>() :
				route.getCurrentPoints());
		ListView listView = getListView();
		listView.setAdapter(adapter);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		RoutePoint rp = adapter.getItem(position);
		getSupportActionBar().startActionMode(getPointActionModeCallback(rp));
		adapter.notifyDataSetChanged();
	}

	private class PointItemAdapter extends ArrayAdapter<RoutePoint> {

		public PointItemAdapter(Context context, int textViewResourceId, List<RoutePoint> pointsList) {
			super(context, textViewResourceId, pointsList);
		}

		private class ViewHolder {
			ImageView image;
			TextView name;
			TextView dateOrDistance;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
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

			RoutePoint point = getItem(position);
			holder.name.setText(point.getName());
			if (selectedItem == point) {
				convertView.setBackgroundColor(getResources().getColor(R.color.row_selection_color));
			} else {
				convertView.setBackgroundColor(Color.TRANSPARENT);
			}

			if (point.isVisited()) {
				holder.image.setImageResource(R.drawable.ic_action_done);
				if (point.isDelivered()){
					holder.name.setTextColor(getResources().getColor(R.color.osmbug_closed));
				} else {
					holder.name.setTextColor(getResources().getColor(R.color.color_invalid));
				}
				holder.dateOrDistance.setTextColor(getResources().getColor(R.color.color_unknown));
				holder.dateOrDistance.setText(point.getTime());
				holder.dateOrDistance.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

			} else {
				if (point.isNextNavigate()) {
					holder.image.setImageResource(R.drawable.ic_action_signpost_dark);
				} else {
					holder.image.setImageResource(R.drawable.ic_action_marker_dark);
				}
				if(position > 0) {
					holder.dateOrDistance.setText(point.getDistance(getItem(position - 1)));
				} else {
					holder.dateOrDistance.setText("");
				}
				holder.name.setTextColor(getResources().getColor(R.color.color_update));
				holder.dateOrDistance.setTextColor(getResources().getColor(R.color.color_distance));
				holder.dateOrDistance.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			}
			return convertView;
		}
	}


	private void saveGPXAsync() {
		new AsyncTask<SelectedRouteGpxFile, Void, Void>() {
			protected void onPreExecute() {
				//getSherlock().setProgressBarIndeterminateVisibility(true);
			}

			@Override
			protected Void doInBackground(SelectedRouteGpxFile... params) {
				if(plugin.getCurrentRoute() != null) {
					plugin.getCurrentRoute().saveFile();
				}
				return null;
			}

			protected void onPostExecute(Void result) {
				//getSherlock().setProgressBarIndeterminateVisibility(false);

			}
		}.execute(plugin.getCurrentRoute());
	}


	
	private ActionMode.Callback getPointActionModeCallback(final RoutePoint rp) {
		return new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
				selectedItem = rp;
				createMenuItem(menu, MARK_AS_CURRENT_ID, R.string.mark_as_current, R.drawable.ic_action_signpost_dark,
						MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				createMenuItem(menu, AS_VISITED_ID, !rp.isVisited() ? 
						R.string.mark_as_visited : R.string.mark_as_not_visited, R.drawable.ic_action_done,
						MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				createMenuItem(menu, POI_ON_MAP_ID, R.string.shared_string_show_on_map, R.drawable.ic_show_on_map,
						MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
				if (menuItem.getItemId() == MARK_AS_CURRENT_ID) {
					plugin.getCurrentRoute().navigateToPoint(rp);
					saveGPXAsync();
					prepareView();
				} else if (menuItem.getItemId() == POI_ON_MAP_ID) {
					LatLon point = rp.getPoint();
					app.getSettings().setMapLocationToShow(point.getLatitude(), point.getLongitude(),
							app.getSettings().getMapZoomToShow());
					finish();
				} else if (menuItem.getItemId() == AS_VISITED_ID) {
					// inverts selection state of item
					if (!rp.isVisited()){
						rp.setDelivered(true);
					} else if (rp.isDelivered()){
						rp.setDelivered(false);
					}
					plugin.getCurrentRoute().markPoint(rp, !rp.isVisited());

					saveGPXAsync();
					prepareView();
				}
				actionMode.finish();
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode actionMode) {
				selectedItem = null;
				adapter.notifyDataSetChanged();
			}
		};
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		createMenuItem(menu, OK_ID, R.string.shared_string_ok, 
				 R.drawable.ic_show_on_map ,
				MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		createMenuItem(menu, NAVIGATE_DIALOG_ID, R.string.navigate_dialog,
			   R.drawable.ic_action_gdirections_dark,
				MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == OK_ID) {
			finish();
			return true;
		} else if (item.getItemId() == NAVIGATE_DIALOG_ID){
			app.getSettings().navigateDialog();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


}
