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
		final Drawable m = view.getResources().getDrawable(R.drawable.map_monitoring);
		final Drawable mWhite = view.getResources().getDrawable(R.drawable.map_monitoring_white);
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
			if(ca.getImageId(ij, false) != 0) {
				ai.setIcon(view.getResources().getDrawable(ca.getImageId(ij , false)));
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
	
	
	
	
}
