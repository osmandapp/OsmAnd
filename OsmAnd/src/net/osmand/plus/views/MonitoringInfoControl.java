package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.londatiga.android.QuickAction;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MonitoringInfoControl {
	
	
	private List<MonitoringInfoControlServices> monitoringServices = new ArrayList<MonitoringInfoControl.MonitoringInfoControlServices>();
	
	public interface MonitoringInfoControlServices {
		
		public void addMonitorActions(QuickAction qa, MonitoringInfoControl li, OsmandMapTileView view);
	}
	
	public void addMonitorActions(MonitoringInfoControlServices la){
		monitoringServices.add(la);
	}
	
	
	public List<MonitoringInfoControlServices> getMonitorActions() {
		return monitoringServices;
	}
	
	public ImageView createMonitoringWidget(final OsmandMapTileView view, final MapActivity map) {
		final ImageView monitoringServices = new ImageView(view.getContext());
		monitoringServices.setImageDrawable(view.getResources().getDrawable(R.drawable.monitoring));
		monitoringServices.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(MonitoringInfoControl.this.monitoringServices.isEmpty()) {
					Toast.makeText(view.getContext(), R.string.enable_plugin_monitoring_services, Toast.LENGTH_LONG).show();
				} else {
					showBgServiceQAction(monitoringServices, view, map);
				}
			}
		});
		return monitoringServices;
	}

	private void showBgServiceQAction(final ImageView lockView, final OsmandMapTileView view, final MapActivity map) {	
		final QuickAction qa = new QuickAction(lockView);
		for(MonitoringInfoControlServices la : monitoringServices){
			la.addMonitorActions(qa, this, view);
		}
		qa.show();
		
	}
	
	public static class ValueHolder<T> {
		public T value;
	}
	
	public void showIntervalChooseDialog(final OsmandMapTileView view, final String patternMsg,
			String title, final int[] seconds, final int[] minutes, final ValueHolder<Integer> v, OnClickListener onclick){
		final Context ctx = view.getContext();
		Builder dlg = new AlertDialog.Builder(view.getContext());
		dlg.setTitle(title);
		LinearLayout ll = new LinearLayout(view.getContext());
		final TextView tv = new TextView(view.getContext());
		tv.setPadding(7, 3, 7, 0);
		tv.setText(String.format(patternMsg, ctx.getString(R.string.int_continuosly)));
		SeekBar sp = new SeekBar(view.getContext());
		sp.setPadding(7, 5, 7, 0);
		final int secondsLength = seconds.length;
    	final int minutesLength = minutes.length;
    	sp.setMax(secondsLength + minutesLength - 1);
		sp.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				String s;
				if(progress == 0) {
					s = ctx.getString(R.string.int_continuosly);
					v.value = 0;
				} else {
					if(progress < secondsLength) {
						s = seconds[progress] + " " + ctx.getString(R.string.int_seconds);
						v.value = seconds[progress] * 1000;
					} else {
						s = minutes[progress - secondsLength] + " " + ctx.getString(R.string.int_min);
						v.value = minutes[progress - secondsLength] * 60 * 1000;
					}
				}
				tv.setText(String.format(patternMsg, s));
				
			}
		});
		
		for (int i = 0; i < secondsLength + minutesLength - 1; i++) {
			if (i < secondsLength) {
				if (v.value <= seconds[i] * 1000) {
					sp.setProgress(i);
					break;
				}
			} else {
				if (v.value <= minutes[i - secondsLength] * 1000 * 60) {
					sp.setProgress(i);
					break;
				}
			}
		}
		
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.addView(tv);
		ll.addView(sp);
		dlg.setView(ll);
		dlg.setPositiveButton(R.string.default_buttons_ok, onclick);
		dlg.setNegativeButton(R.string.default_buttons_cancel, null);
		dlg.show();
	}
	
	
}
