package net.osmand.plus.plugins.openseamaps;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_NAUTICAL;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.SHOW_DEPTH_CONTOURS;
import static net.osmand.plus.download.local.LocalItemType.DEPTH_DATA;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.DashboardType;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class NauticalMapsPlugin extends OsmandPlugin {

	public static final String COMPONENT = "net.osmand.nauticalPlugin";
	public static final String DEPTH_CONTOURS = "depthContours";

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
	public CharSequence getDescription(boolean linksEnabled) {
		String docsUrl = app.getString(R.string.docs_plugin_nautical);
		String description = app.getString(R.string.plugin_nautical_descr, docsUrl);
		return linksEnabled ? UiUtilities.createUrlSpannable(app, description, docsUrl) : description;
	}

	@Override
	public String getName() {
		return app.getString(net.osmand.plus.R.string.plugin_nautical_name);
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

	public void createAdapterItem(@NonNull String id,
	                              @NonNull ContextMenuAdapter adapter,
	                              @NonNull MapActivity mapActivity,
	                              @NonNull List<RenderingRuleProperty> customRules) {
		if ((isEnabled() || hasDepthMaps())) {
			createNauticalItem(id, adapter, mapActivity, customRules);
		}
	}

	@Override
	public void registerLayerContextMenuActions(@NonNull ContextMenuAdapter menuAdapter,
	                                            @NonNull MapActivity mapActivity,
	                                            @NonNull List<RenderingRuleProperty> customRules) {
		if (!PluginsHelper.isEnabled(SRTMPlugin.class)) {
			createAdapterItem(SHOW_DEPTH_CONTOURS, menuAdapter, mapActivity, customRules);
		}
	}

	private boolean hasDepthMaps() {
		boolean readFiles = !app.getResourceManager().isIndexesLoadedOnStart();
		LocalIndexHelper helper = new LocalIndexHelper(app);
		List<LocalItem> depthIndexData = helper.getLocalIndexItems(readFiles, false, null, DEPTH_DATA);
		return !Algorithms.isEmpty(depthIndexData);
	}

	private void createNauticalItem(@NonNull String id,
	                                @NonNull ContextMenuAdapter adapter,
	                                @NonNull MapActivity mapActivity,
	                                @NonNull List<RenderingRuleProperty> customRules) {
		OsmandSettings settings = app.getSettings();
		Iterator<RenderingRuleProperty> iterator = customRules.iterator();
		while (iterator.hasNext()) {
			RenderingRuleProperty property = iterator.next();
			String attrName = property.getAttrName();
			if (DEPTH_CONTOURS.equals(attrName)) {
				CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(attrName);
				ItemClickListener listener = getPropertyItemClickListener(pref, mapActivity);

				adapter.addItem(new ContextMenuItem(id)
						.setTitleId(R.string.nautical_depth, mapActivity)
						.setSecondaryIcon(R.drawable.ic_action_additional_option)
						.setSecondaryDescription(pref.get() ? app.getString(R.string.shared_string_on) : app.getString(R.string.shared_string_off))
						.setSelected(pref.get())
						.setColor(app, pref.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
						.setIcon(R.drawable.ic_action_nautical_depth)
						.setListener(listener));

				iterator.remove();
			}
		}
	}

	public ItemClickListener getPropertyItemClickListener(@NonNull CommonPreference<Boolean> pref,
	                                                      @NonNull MapActivity mapActivity) {
		return new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
			                              @NonNull View view, @NonNull ContextMenuItem item) {
				DashboardOnMap dashboard = mapActivity.getDashboard();
				dashboard.setDashboardVisibility(true, DashboardType.NAUTICAL_DEPTH);
				return false;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
			                                  @Nullable View view, @NotNull ContextMenuItem item,
			                                  boolean isChecked) {
				pref.set(isChecked);
				item.setSelected(pref.get());
				item.setColor(app, pref.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				item.setDescription(app.getString(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled));
				if (uiAdapter != null) {
					uiAdapter.onDataSetChanged();
				}
				mapActivity.refreshMapComplete();
				return true;
			}
		};
	}

	@Override
	public boolean disablePreferences() {
		return false;
	}

	@Nullable
	@Override
	protected String getRenderPropertyPrefix() {
		return "depthContour";
	}

	@Override
	protected CommonPreference<String> registerRenderingPreference(@NonNull RenderingRuleProperty property) {
		String attrName = property.getAttrName();
		String defValue = Algorithms.isEmpty(property.getPossibleValues()) ? "" : property.getPossibleValues()[0];
		return registerRenderingPreference(attrName, defValue);
	}
}