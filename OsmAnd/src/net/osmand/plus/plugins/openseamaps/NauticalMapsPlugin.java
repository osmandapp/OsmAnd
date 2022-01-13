package net.osmand.plus.plugins.openseamaps;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_NAUTICAL;
import static net.osmand.plus.ContextMenuAdapter.makeDeleteAction;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

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
import net.osmand.plus.views.OsmandMapTileView;

import java.util.Collections;
import java.util.List;


public class NauticalMapsPlugin extends OsmandPlugin {

	public static final String COMPONENT = "net.osmand.nauticalPlugin";
	public static final String DEPTH_CONTOURS = "depthContours";
	public OsmandPreference<Boolean> SHOW_DEPTH_CONTOURS;

	public NauticalMapsPlugin(OsmandApplication app) {
		super(app);
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
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter menuAdapter, @NonNull MapActivity mapActivity) {
		SHOW_DEPTH_CONTOURS = registerBooleanPreference(app, "nrenderer_depthContours", false);
		ItemClickListener listener = (adapter, itemId, pos, isChecked, viewCoordinates) -> {
			if (itemId == R.string.index_item_depth_contours_osmand_ext) {
				boolean checked = !SHOW_DEPTH_CONTOURS.get();
				SHOW_DEPTH_CONTOURS.set(checked);
				adapter.getItem(pos).setColor(app, SHOW_DEPTH_CONTOURS.get() ?
						R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				adapter.getItem(pos).setDescription(app.getString(SHOW_DEPTH_CONTOURS.get() ? R.string.shared_string_enabled : R.string.shared_string_disabled));
				adapter.notifyDataSetChanged();
				refreshLayer();
			}
			return true;
		};

		menuAdapter.addItem(new ContextMenuItem.ItemBuilder()
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

	private void refreshLayer() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		mapView.refreshMap(true);
	}
}