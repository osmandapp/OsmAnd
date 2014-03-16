package net.osmand.plus.views.controls;

import java.util.HashSet;
import java.util.Set;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.NavigateAction;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Canvas;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;

public class MapAppModeControl extends MapControls {
	private ImageButton settingsAppModeButton;
	private OsmandSettings settings;
	private int cachedId;
	private Dialog dialog;
	
	
	public MapAppModeControl(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
		settings = mapActivity.getMyApplication().getSettings();
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		settingsAppModeButton = addImageButton(parent, R.string.routing_preferences_descr, R.drawable.map_btn_plain);
		cachedId = 0;
		settingsAppModeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(dialog != null) {
					dialog.hide();
					dialog = null;
					settingsAppModeButton.setBackgroundResource(R.drawable.map_btn_plain);
				} else {
					dialog = showDialog();
					dialog.show();
					settingsAppModeButton.setBackgroundResource(R.drawable.map_btn_plain_p);
					dialog.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dlg) {
							settingsAppModeButton.setBackgroundResource(R.drawable.map_btn_plain);
							dialog = null;
						}
					});
				}
			}
		});
	}
	
	private Dialog showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);        
        View ll = createLayout();
        builder.setView(ll);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        lp.y = (int) (settingsAppModeButton.getBottom() - settingsAppModeButton.getTop() + scaleCoefficient * 5); 
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setAttributes(lp);
        return dialog;
	}

	private View createLayout() {
		View settingsDlg = View.inflate(mapActivity, R.layout.plan_route_settings, null);
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		ApplicationMode am = settings.APPLICATION_MODE.get();
		final ListView lv = (ListView) settingsDlg.findViewById(android.R.id.list);
		final Set<ApplicationMode> selected = new HashSet<ApplicationMode>();
		selected.add(am);
		NavigateAction.prepareAppModeView(mapActivity, selected, false, 
				(ViewGroup) settingsDlg.findViewById(R.id.TopBar), true, 
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(selected.size() > 0) {
					settings.APPLICATION_MODE.set(selected.iterator().next());
					mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
					//listAdapter.notifyDataSetChanged();
				}
			}
		});
		return settingsDlg;
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, settingsAppModeButton);
		layout.removeView(settingsAppModeButton);
		mapActivity.accessibleContent.remove(settingsAppModeButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		int id = settings.getApplicationMode().getSmallIcon(settingsAppModeButton.isPressed() || dialog != null);
		if(cachedId != id && settingsAppModeButton.getLeft() > 0) {
			cachedId = id;
			settingsAppModeButton.setImageResource(id);
		}
	}
}
