package net.osmand.plus.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import net.osmand.Location;
import net.osmand.TspAnt;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class IntermediatePointsDialog {

	public static void openIntermediatePointsDialog(final Activity activity,
			final OsmandApplication app, final boolean changeOrder){
		TargetPointsHelper targets = app.getTargetPointsHelper();
		final List<TargetPoint> intermediates = targets.getIntermediatePointsWithTarget();
		final TIntArrayList originalPositions = new TIntArrayList(intermediates.size());
		for(int j = 1; j <= intermediates.size(); j++) {
			originalPositions.add(j);
		}
		final boolean[] checkedIntermediates = new boolean[intermediates.size()];
		Arrays.fill(checkedIntermediates, true);
		final ArrayAdapter<TargetPoint> listadapter = getListAdapter(app, activity, changeOrder, intermediates, originalPositions, checkedIntermediates);
		ListView lv = new ListView(activity);
		View contentView = lv;
		final ProgressBar pb = new ProgressBar(activity);
		pb.setVisibility(View.GONE);
		final TextView textInfo = new TextView(activity);
		textInfo.setText(R.string.intermediate_items_sort_return);
		textInfo.setVisibility(View.GONE);
		
		if (changeOrder) {
			LinearLayout ll = new LinearLayout(activity);
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.addView(lv);
			ll.addView(pb);
			ll.addView(textInfo);
			contentView = ll;
			
//			lv.addFooterView(pb);
//			lv.addFooterView(textInfo);
		}
		lv.setAdapter(listadapter);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (activity instanceof MapActivity) {
					// AnimateDraggingMapThread thread = mapActivity.getMapView().getAnimatedDraggingThread();
					TargetPoint pointToNavigate = intermediates.get(position);
					int fZoom = ((MapActivity) activity).getMapView().getZoom() < 15 ? 15 : ((MapActivity) activity).getMapView().getZoom();
					// thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
					((MapActivity) activity).getMapView().setIntZoom(fZoom);
					((MapActivity) activity).getMapView().setLatLon(pointToNavigate.getLatitude(), pointToNavigate.getLongitude());
					listadapter.notifyDataSetInvalidated();
				}
			}
		});
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(contentView);
		builder.setInverseBackgroundForced(true);
		lv.setBackgroundColor(Color.WHITE);
		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(changeOrder) {
					commitChangePointsOrder(app, intermediates);
				} else {
					commitPointsRemoval(app, checkedIntermediates);
				}

			}
		});
		if (!changeOrder && intermediates.size() > 1) {
			builder.setNeutralButton(R.string.intermediate_points_change_order, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					openIntermediatePointsDialog(activity, app, true);
				}
			});
		} else if (intermediates.size() > 1) {
			builder.setNeutralButton(R.string.intermediate_items_sort_by_distance, new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int which) {
					// Do nothing here. We override the onclick
				}
			});
		}
		AlertDialog dlg = builder.create();
		if (changeOrder) {
			applySortTargets(dlg, activity, intermediates, originalPositions, listadapter, pb, textInfo);
		}
		dlg.show();
	}

	private static void applySortTargets(AlertDialog dlg, final Activity activity, final List<TargetPoint> intermediates,
			final TIntArrayList originalPositions, final ArrayAdapter<TargetPoint> listadapter, final ProgressBar pb, final TextView textInfo) {
		dlg.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {

						new AsyncTask<Void, Void, int[]>() {

							protected void onPreExecute() {
								pb.setVisibility(View.VISIBLE);
								textInfo.setVisibility(View.VISIBLE);
							};

							protected int[] doInBackground(Void[] params) {
								OsmandApplication app = (OsmandApplication) activity.getApplication();
								Location cll = app.getLocationProvider().getLastKnownLocation();
								ArrayList<TargetPoint> lt = new ArrayList<TargetPoint>(intermediates);
								TargetPoint start ;
								
								if(cll != null) {
									LatLon ll = new LatLon(cll.getLatitude(), cll.getLongitude());
									start = TargetPoint.create(ll, null);
								} else if(app.getTargetPointsHelper().getPointToStart() != null) {
									TargetPoint ps = app.getTargetPointsHelper().getPointToStart();
									LatLon ll = new LatLon(ps.getLatitude(), ps.getLongitude());
									start = TargetPoint.create(ll, null);
//								} else if(activity instanceof MapActivity) {
//									LatLon ll = new LatLon(((MapActivity) activity).getMapView().getLatitude(), ((MapActivity) activity).getMapView().getLongitude());
//									start = TargetPoint.create(ll, null);
								} else {
									start = lt.get(0);
								}
								TargetPoint end = lt.remove(lt.size() - 1);
								ArrayList<LatLon> al = new ArrayList<LatLon>();
								for(TargetPoint p : lt){
									al.add(p.point);
								}
								return new TspAnt().readGraph(al, start.point, end.point).solve();
							};

							protected void onPostExecute(int[] result) {
								pb.setVisibility(View.GONE);
								List<TargetPoint> alocs = new ArrayList<TargetPoint>();
								TIntArrayList newOriginalPositions = new TIntArrayList();
								for (int i = 0; i < result.length; i++) {
									if (result[i] > 0) {
										TargetPoint loc = intermediates.get(result[i] - 1);
										alocs.add(loc);
										newOriginalPositions.add(originalPositions.get(intermediates.indexOf(loc)));
									}
								}
								intermediates.clear();
								intermediates.addAll(alocs);
								originalPositions.clear();
								originalPositions.addAll(newOriginalPositions);
								listadapter.notifyDataSetChanged();
							};
						}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);

					}
				});

			}
		});
	}
	
	

	private static ArrayAdapter<TargetPoint> getListAdapter(final OsmandApplication app, final Activity activity, final boolean changeOrder,
			final List<TargetPoint> intermediates, final TIntArrayList originalPositions,  final boolean[] checkedIntermediates) {
		final int padding = (int) (12 * activity.getResources().getDisplayMetrics().density + 0.5f);
		final ArrayAdapter<TargetPoint> listadapter = new ArrayAdapter<TargetPoint>(app, 
				changeOrder? R.layout.change_order_item : R.layout.list_menu_item_native, R.id.title,
				intermediates) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				
				// User super class to create the View
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(R.id.title);
				String nm = originalPositions.get(position) + ". ";
				TargetPoint tp = intermediates.get(position);
				String distString = "";
				if(activity instanceof MapActivity) {
					double lat = ((MapActivity) activity).getMapView().getLatitude();
					double lon = ((MapActivity) activity).getMapView().getLongitude();
					double meters = MapUtils.getDistance(tp.point, lat, lon);
					distString = OsmAndFormatter.getFormattedDistance((float) meters, app);
				}
				if(position < intermediates.size() - 1) {
					nm += app.getString(R.string.target_point, distString);
				} else {
					nm += app.getString(R.string.destination_point, distString);
				}
				String descr = tp.getOnlyName();
				if(descr != null && descr.trim().length() > 0) {
					nm += "\n" + descr;
				}
				tv.setText(nm);
				if (changeOrder) {
					((ImageButton) v.findViewById(R.id.up)).setOnClickListener(new View.OnClickListener(){
						@Override
						public void onClick(View v) {
							if(position > 0) {
								TargetPoint old = intermediates.remove(position - 1);
								int oldI = originalPositions.removeAt(position -1 );
								intermediates.add(position, old);
								originalPositions.insert(position, oldI);
								notifyDataSetInvalidated();
							}
						}
					});
					((ImageButton) v.findViewById(R.id.down)).setOnClickListener(new View.OnClickListener(){
						@Override
						public void onClick(View v) {
							if(position < intermediates.size() - 1) {
								TargetPoint old = intermediates.remove(position + 1);
								int oldI = originalPositions.removeAt(position + 1 );
								intermediates.add(position, old);
								originalPositions.insert(position, oldI);
								notifyDataSetInvalidated();
							}
						}
					});
				} else {
					int icon = position == intermediates.size() - 1? R.drawable.ic_action_target:
						R.drawable.ic_action_intermediate;
					tv.setCompoundDrawablesWithIntrinsicBounds(app.getUIUtilities().getThemedIcon(icon), null, null, null);
					tv.setCompoundDrawablePadding(padding);
					final CheckBox ch = ((CheckBox) v.findViewById(R.id.toggle_item));
					ch.setVisibility(View.VISIBLE);
					ch.setOnCheckedChangeListener(null);
					ch.setChecked(checkedIntermediates[position]);
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

	public static void commitPointsRemoval(OsmandApplication app, final boolean[] checkedIntermediates) {
		int cnt = 0;
		for (int i = checkedIntermediates.length - 1; i >= 0; i--) {
			if (!checkedIntermediates[i]) {
				cnt++;
			}
		}
		if (cnt > 0) {
			boolean changeDestinationFlag = !checkedIntermediates[checkedIntermediates.length - 1];
			if (cnt == checkedIntermediates.length) { // there is no alternative destination if all points are to be
														// removed?
				app.getTargetPointsHelper().removeAllWayPoints(true, true);
			} else {
				for (int i = checkedIntermediates.length - 2; i >= 0; i--) { // skip the destination until a retained
																				// waypoint is found
					if (checkedIntermediates[i] && changeDestinationFlag) { // Find a valid replacement for the
																			// destination
						app.getTargetPointsHelper().makeWayPointDestination(cnt == 0, i);
						changeDestinationFlag = false;
					} else if (!checkedIntermediates[i]) {
						cnt--;
						app.getTargetPointsHelper().removeWayPoint(cnt == 0, i);
					}
				}
			}
		}
	}
	
	private static void commitChangePointsOrder(OsmandApplication app,  List<TargetPoint> target) {
		TargetPointsHelper targets = app.getTargetPointsHelper();
		List<TargetPoint> cur = targets.getIntermediatePointsWithTarget();
		boolean eq = true;
		for(int j = 0; j < cur.size() && j < target.size() ; j++) {
			if(cur.get(j) != target.get(j)) {
				eq = false;
				break;
			}
		}
		if(!eq) {
			targets.reorderAllTargetPoints(target, true);
		}
	}
}
