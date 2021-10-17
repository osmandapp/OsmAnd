package net.osmand.plus.srtmplugin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class TerrainAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(30,
			"terrain.showhide", TerrainAction.class).nameActionRes(R.string.quick_action_show_hide_title).
			nameRes(R.string.shared_string_terrain).iconRes(R.drawable.ic_action_hillshade_dark).nonEditable().
			category(QuickActionType.CONFIGURE_MAP);


	public TerrainAction() {
		super(TYPE);
	}

	public TerrainAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull final MapActivity mapActivity) {
		final SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		if (plugin != null) {
			plugin.toggleTerrain(mapActivity, !plugin.isTerrainLayerEnabled(), () -> {
				if (plugin.isTerrainLayerEnabled()) {
					OsmandPlugin.enablePluginIfNeeded(mapActivity, mapActivity.getMyApplication(), plugin, true);
				}
				plugin.updateLayers(mapActivity, mapActivity);
				mapActivity.refreshMapComplete();
			});
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_terrain_descr);
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		return R.drawable.ic_action_hillshade_dark;
	}

	@Override
	public String getActionText(OsmandApplication application) {
		String nameRes = application.getString(getNameRes());
		String actionName = isActionWithSlash(application) ? application.getString(R.string.shared_string_hide) : application.getString(R.string.shared_string_show);
		return application.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {
		return application.getSettings().TERRAIN.get();
	}
}
