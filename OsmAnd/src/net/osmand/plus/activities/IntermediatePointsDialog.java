package net.osmand.plus.activities;

import java.util.ArrayList;
import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class IntermediatePointsDialog {

	
	public static void openIntermediatePointsDialog(MapActivity mapActivity, OsmandApplication app){
		openIntermediatePointsDialog(mapActivity, app, false);
	}
	
	public static void openIntermediatePointsDialog(final MapActivity mapActivity,
			final OsmandApplication app, final boolean changeOrder){
		TargetPointsHelper targets = app.getTargetPointsHelper();
		final List<LatLon> intermediates = targets.getIntermediatePointsWithTarget();
		final List<String> names = targets.getIntermediatePointNamesWithTarget();
		final boolean[] checkedIntermediates = new boolean[intermediates.size()];
		final ArrayAdapter<LatLon> listadapter = new ArrayAdapter<LatLon>(app, 
				changeOrder? R.layout.change_order_item : R.layout.layers_list_activity_item, R.id.title,
				intermediates) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				
				// User super class to create the View
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(R.id.title);
				String nm = (position + 1) + ". ";
				String distString = "";
				if(mapActivity != null) {
					double lat = mapActivity.getMapView().getLatitude();
					double lon = mapActivity.getMapView().getLongitude();
					double meters = MapUtils.getDistance(intermediates.get(position), lat, lon);
					distString = OsmAndFormatter.getFormattedDistance((float) meters, mapActivity);
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
				} else if (position == intermediates.size() - 1) {
					tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.list_view_set_destination, 0, 0, 0);
					final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
					ch.setVisibility(View.GONE);
				} else {
					// list_view_set_intermediate
					tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.list_view_set_intermediate, 0, 0, 0);
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
		ListView lv = new ListView(app);
		lv.setAdapter(listadapter);
		lv.setBackgroundColor(Color.WHITE);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (mapActivity != null) {
					// AnimateDraggingMapThread thread = mapActivity.getMapView().getAnimatedDraggingThread();
					LatLon pointToNavigate = intermediates.get(position);
					float fZoom = mapActivity.getMapView().getFloatZoom() < 15 ? 15 : mapActivity.getMapView().getFloatZoom();
					// thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
					mapActivity.getMapView().setZoom(fZoom);
					mapActivity.getMapView().setLatLon(pointToNavigate.getLatitude(), pointToNavigate.getLongitude());
					listadapter.notifyDataSetInvalidated();
				}
			}
		});
		
		Builder builder = new AccessibleAlertBuilder(app);
		builder.setView(lv);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(changeOrder) {
					commitChangePointsOrder(app, mapActivity, intermediates, names);
				} else {
					commitPointsRemoval(app, mapActivity, checkedIntermediates);
				}

			}
		});
		if (!changeOrder) {
			builder.setNeutralButton("Change order", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					openIntermediatePointsDialog(mapActivity, app, true);
				}
			});
		}
		builder.show();
	}

	private static void commitPointsRemoval(OsmandApplication app, final MapActivity mapActivity, final boolean[] checkedIntermediates) {
		int cnt = 0;
		for (int i = checkedIntermediates.length - 1; i >= 0; i--) {
			if (!checkedIntermediates[i]) {
				cnt++;
			}
		}
		if (cnt > 0) {
			for (int i = checkedIntermediates.length - 1; i >= 0; i--) {
				if (!checkedIntermediates[i]) {
					cnt--;
					app.getTargetPointsHelper().removeWayPoint(mapActivity, cnt == 0, i);
				}
			}
			if(mapActivity != null) {
				mapActivity.getMapLayers().getContextMenuLayer().setLocation(null, "");
			}
		}
	}
	
	private static void commitChangePointsOrder(OsmandApplication app, final MapActivity mapActivity, List<LatLon> target, List<String> names) {
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
			targets.reorderAllTargetPoints(mapActivity, target, names, true);
		}
	}
}
