package net.osmand.plus.activities;

import java.util.ArrayList;
import java.util.List;

import net.osmand.TspAnt;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

public class IntermediatePointsDialog {

	
	public static void openIntermediatePointsDialog(MapActivity mapActivity){
		openIntermediatePointsDialog(mapActivity, (OsmandApplication) mapActivity.getApplication(), false);
	}
	
	public static void openIntermediatePointsDialog(final MapActivity activity,
			final OsmandApplication app, final boolean changeOrder){
		TargetPointsHelper targets = app.getTargetPointsHelper();
		final List<LatLon> intermediates = targets.getIntermediatePointsWithTarget();
		final List<String> names = targets.getIntermediatePointNamesWithTarget();
		final boolean[] checkedIntermediates = new boolean[intermediates.size()];
		final ArrayAdapter<LatLon> listadapter = getListAdapter(app, activity, changeOrder, intermediates, names, checkedIntermediates);
		ListView lv = new ListView(activity);
		final ProgressBar pb = new ProgressBar(activity);
		pb.setVisibility(View.GONE);
		final TextView textInfo = new TextView(activity);
		textInfo.setText(R.string.intermediate_items_sort_return);
		textInfo.setVisibility(View.GONE);
		if (changeOrder) {
			Button btn = new Button(activity);
			btn.setText(R.string.intermediate_items_sort_by_distance);
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					new AsyncTask<Void, Void, int[]>() {

						protected void onPreExecute() {
							pb.setVisibility(View.VISIBLE);
							textInfo.setVisibility(View.VISIBLE);
						};

						protected int[] doInBackground(Void[] params) {
							ArrayList<LatLon> lt = new ArrayList<LatLon>(intermediates);
							LatLon start = new LatLon(activity.getMapView().getLatitude(), activity.getMapView().getLongitude());
							LatLon end = lt.remove(lt.size() - 1);
							return new TspAnt().readGraph(lt, start, end).solve();
						};

						protected void onPostExecute(int[] result) {
							pb.setVisibility(View.GONE);
							List<LatLon> alocs = new ArrayList<LatLon>();
							List<String> anames = new ArrayList<String>();
							for (int i = 0; i < result.length; i++) {
								if(result[i] > 0) {
									alocs.add(intermediates.get(result[i] - 1));
									anames.add(names.get(result[i] - 1));
								}
							}
							intermediates.clear();
							intermediates.addAll(alocs);
							names.clear();
							names.addAll(anames);
							listadapter.notifyDataSetChanged();
						};
					}.execute(new Void[0]);
				}
			});
			lv.addFooterView(pb);
			lv.addFooterView(textInfo);
			lv.addFooterView(btn);
		}
		lv.setAdapter(listadapter);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (activity instanceof MapActivity) {
					// AnimateDraggingMapThread thread = mapActivity.getMapView().getAnimatedDraggingThread();
					LatLon pointToNavigate = intermediates.get(position);
					float fZoom = ((MapActivity) activity).getMapView().getFloatZoom() < 15 ? 15 : ((MapActivity) activity).getMapView().getFloatZoom();
					// thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
					((MapActivity) activity).getMapView().setZoom(fZoom);
					((MapActivity) activity).getMapView().setLatLon(pointToNavigate.getLatitude(), pointToNavigate.getLongitude());
					listadapter.notifyDataSetInvalidated();
				}
			}
		});
		
		Builder builder = new AccessibleAlertBuilder(activity);
		builder.setView(lv);
		builder.setInverseBackgroundForced(true);
		lv.setBackgroundColor(Color.WHITE);
		builder.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(changeOrder) {
					commitChangePointsOrder(app, intermediates, names);
				} else {
					commitPointsRemoval(app, checkedIntermediates);
				}

			}
		});
		if (!changeOrder) {
			builder.setNeutralButton(R.string.intermediate_points_change_order, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					openIntermediatePointsDialog(activity, app, true);
				}
			});
		}
		builder.show();
	}
	

	private static ArrayAdapter<LatLon> getListAdapter(final OsmandApplication app, final Activity activity, final boolean changeOrder,
			final List<LatLon> intermediates, final List<String> names, final boolean[] checkedIntermediates) {
		final int padding = (int) (12 * activity.getResources().getDisplayMetrics().density + 0.5f);
		final ArrayAdapter<LatLon> listadapter = new ArrayAdapter<LatLon>(app, 
				changeOrder? R.layout.change_order_item : R.layout.list_menu_item, R.id.title,
				intermediates) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				
				// User super class to create the View
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(R.id.title);
				String nm = (position + 1) + ". ";
				String distString = "";
				if(activity instanceof MapActivity) {
					double lat = ((MapActivity) activity).getMapView().getLatitude();
					double lon = ((MapActivity) activity).getMapView().getLongitude();
					double meters = MapUtils.getDistance(intermediates.get(position), lat, lon);
					distString = OsmAndFormatter.getFormattedDistance((float) meters, app);
				}
				
				nm += app.getString(R.string.target_point, distString);
				String descr = names.get(position);
				if(descr != null && descr.trim().length() > 0) {
					nm += "\n" + descr;
				}
				tv.setText(nm);
				checkedIntermediates[position] = true;
				if (changeOrder) {
					((ImageButton) v.findViewById(R.id.up)).setOnClickListener(new View.OnClickListener(){
						@Override
						public void onClick(View v) {
							if(position > 0) {
								LatLon old = intermediates.remove(position - 1);
								String oldN = names.remove(position - 1);
								names.add(position, oldN);
								intermediates.add(position, old);
								notifyDataSetInvalidated();
							}
						}
					});
					((ImageButton) v.findViewById(R.id.down)).setOnClickListener(new View.OnClickListener(){
						@Override
						public void onClick(View v) {
							if(position < intermediates.size() - 1) {
								LatLon old = intermediates.remove(position + 1);
								String oldN = names.remove(position + 1);
								names.add(position, oldN);
								intermediates.add(position, old);
								notifyDataSetInvalidated();
							}
						}
					});
				} else {
					tv.setCompoundDrawablesWithIntrinsicBounds(
							position == intermediates.size() - 1? R.drawable.list_activities_set_destination:
								R.drawable.list_activities_set_intermediate, 0, 0, 0);
					tv.setCompoundDrawablePadding(padding);
					final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
					ch.setVisibility(View.VISIBLE);
					ch.setChecked(true);
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							checkedIntermediates[position] = isChecked;
						}
					});
				}
				return v;
			}
		};
		return listadapter;
	}

	private static void commitPointsRemoval(OsmandApplication app, final boolean[] checkedIntermediates) {
		int cnt = 0;
		for (int i = checkedIntermediates.length - 1; i >= 0; i--) {
			if (!checkedIntermediates[i]) {
				cnt++;
			}
		}
		if (cnt > 0) {
			boolean changeDestinationFlag =!checkedIntermediates [checkedIntermediates.length - 1];
			if(cnt == checkedIntermediates.length){	//there is no alternative destination if all points are to be removed?
				app.getTargetPointsHelper().removeAllWayPoints(true);	
			}else{					
				for (int i = checkedIntermediates.length - 2; i >= 0; i--) {	//skip the destination until a retained waypoint is found
					if (checkedIntermediates[i] && changeDestinationFlag) {	//Find a valid replacement for the destination
						app.getTargetPointsHelper().makeWayPointDestination(cnt == 0, i);				
						changeDestinationFlag = false;
					}else if(!checkedIntermediates[i]){
						cnt--;
						app.getTargetPointsHelper().removeWayPoint(cnt == 0, i);
					}
				}
				// FIXME
//				if(mapActivity instanceof MapActivity) {
//					((MapActivity) mapActivity).getMapLayers().getContextMenuLayer().setLocation(null, "");
//				}
			}
		}
	}
	
	private static void commitChangePointsOrder(OsmandApplication app,  List<LatLon> target, List<String> names) {
		TargetPointsHelper targets = app.getTargetPointsHelper();
		List<LatLon> cur = targets.getIntermediatePointsWithTarget();
		boolean eq = true;
		for(int j = 0; j < cur.size() && j < target.size() ; j++) {
			if(cur.get(j) != target.get(j)) {
				eq = false;
				break;
			}
		}
		if(!eq) {
			targets.reorderAllTargetPoints(target, names, true);
		}
	}
}
