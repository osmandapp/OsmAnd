package net.osmand.plus.plugins.openseamaps;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DEPTH_CONTOURS;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_NAUTICAL;
import static net.osmand.plus.ContextMenuAdapter.makeDeleteAction;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import java.util.Collections;
import java.util.List;


public class NauticalMapsPlugin extends OsmandPlugin {

	public static final String COMPONENT = "net.osmand.nauticalPlugin";
	public final OsmandPreference<Boolean> SHOW_DEPTH_CONTOURS;

	public NauticalMapsPlugin(OsmandApplication app) {
		super(app);
		SHOW_DEPTH_CONTOURS = registerBooleanPreference(app, "show_depth_contours", true);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_nautical_map;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.nautical_map);
	}

	@Override
	public boolean isMarketPlugin() {
		return true;
	}

	@Override
	public String getComponentId1() {
		return COMPONENT;
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(net.osmand.plus.R.string.plugin_nautical_descr);
	}

	@Override
	public String getName() {
		return app.getString(net.osmand.plus.R.string.plugin_nautical_name);
	}

	@Override
	public String getHelpFileName() {
		return "feature_articles/nautical-charts.html";
	}

	@Override
	public List<ApplicationMode> getAddedAppModes() {
		return Collections.singletonList(ApplicationMode.BOAT);
	}

	@Override
	public List<String> getRendererNames() {
		return Collections.singletonList(RendererRegistry.NAUTICAL_RENDER);
	}

	@Override
	public List<String> getRouterNames() {
		return Collections.singletonList("boat");
	}

	@Override
	public String getId() {
		return PLUGIN_NAUTICAL;
	}

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity) {
		ItemClickListener listener = (adptr, itemId, pos, isChecked, viewCoordinates) -> {
			if (itemId == R.string.index_item_depth_contours_osmand_ext) {
				SHOW_DEPTH_CONTOURS.set(!SHOW_DEPTH_CONTOURS.get());
				Log.i("tag",String.valueOf(SHOW_DEPTH_CONTOURS.get()));
				adptr.getItem(pos).setColor(app, SHOW_DEPTH_CONTOURS.get() ?
						R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				adptr.getItem(pos).setDescription(app.getString(SHOW_DEPTH_CONTOURS.get() ? R.string.shared_string_enabled : R.string.shared_string_disabled));
				adptr.notifyDataSetChanged();
				NauticalMapsPlugin.this.updateLayers(mapActivity, mapActivity);
			}
			return true;
		};

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.index_item_depth_contours_osmand_ext, app)
				.setId(DEPTH_CONTOURS)
				.setSelected(SHOW_DEPTH_CONTOURS.get())
				.setIcon(R.drawable.ic_action_nautical_depth)
				.setColor(mapActivity, SHOW_DEPTH_CONTOURS.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setItemDeleteAction(makeDeleteAction(SHOW_DEPTH_CONTOURS))
				.setListener(listener)
				.setDescription(app.getString(SHOW_DEPTH_CONTOURS.get() ? R.string.shared_string_enabled : R.string.shared_string_disabled))
				.createItem());
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		super.updateLayers(context, mapActivity);
	}
}