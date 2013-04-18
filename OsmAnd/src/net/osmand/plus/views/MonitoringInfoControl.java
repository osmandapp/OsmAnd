package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.ImageViewWidget;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MonitoringInfoControl {
	
	
	private List<MonitoringInfoControlServices> monitoringServices = new ArrayList<MonitoringInfoControl.MonitoringInfoControlServices>();
	
	public interface MonitoringInfoControlServices {
		
		public void addMonitorActions(ContextMenuAdapter ca, MonitoringInfoControl li, OsmandMapTileView view);
	}
	
	public void addMonitorActions(MonitoringInfoControlServices la){
		monitoringServices.add(la);
	}
	
	
	public List<MonitoringInfoControlServices> getMonitorActions() {
		return monitoringServices;
	}
	
	public ImageViewWidget createMonitoringWidget(final OsmandMapTileView view, final MapActivity map) {
		final Drawable m = view.getResources().getDrawable(R.drawable.list_activities_monitoring);
		final Drawable mWhite = view.getResources().getDrawable(R.drawable.list_activities_monitoring_white);
		final ImageViewWidget monitoringServices = new ImageViewWidget(view.getContext()) {
			private boolean nightMode;
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
				if(nightMode != this.nightMode) {
					this.nightMode = nightMode;
					setImageDrawable(nightMode ? mWhite : m);
					return true;
				}
				return false;
			}
		};
		monitoringServices.setImageDrawable(m);
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
		final ContextMenuAdapter ca = new ContextMenuAdapter(map);
		for(MonitoringInfoControlServices la : monitoringServices){
			la.addMonitorActions(ca, this, view);
		}
		final QuickAction qa = new QuickAction(lockView);		
		String[] itemNames = ca.getItemNames();
		for(int i = 0; i < ca.length(); i++) {
			final int ij = i;
			ActionItem ai = new ActionItem();
			ai.setTitle(itemNames[ij]);
			if(ca.getImageId(ij) != 0) {
				ai.setIcon(view.getResources().getDrawable(ca.getImageId(ij)));
			}
			ai.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ca.getClickAdapter(ij).onContextMenuClick(ca.getItemId(ij), ij, false, null);
					qa.dismiss();
				}
			});
			qa.addActionItem(ai);
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
