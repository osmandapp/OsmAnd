package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.quickaction.QuickActionIds.TERRAIN_ACTION_ID;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public class TerrainAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(TERRAIN_ACTION_ID,
			"terrain.showhide", TerrainAction.class)
			.nameRes(R.string.shared_string_terrain).iconRes(R.drawable.ic_action_hillshade_dark).nonEditable().
			category(QuickActionType.CONFIGURE_MAP).nameActionRes(R.string.quick_action_verb_show_hide);

	public TerrainAction() {
		super(TYPE);
	}

	public TerrainAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (plugin != null) {
			plugin.toggleTerrain(!plugin.isTerrainLayerEnabled(), () -> {
				if (plugin.isTerrainLayerEnabled()) {
					PluginsHelper.enablePluginIfNeeded(mapActivity, mapActivity.getApp(), plugin, true);
				}
				plugin.updateLayers(mapActivity, mapActivity);
				mapActivity.refreshMapComplete();
			});
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_terrain_descr);
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		return R.drawable.ic_action_hillshade_dark;
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName = isActionWithSlash(app) ? app.getString(R.string.shared_string_hide) : app.getString(R.string.shared_string_show);
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		return plugin != null && plugin.TERRAIN.get();
	}
}
