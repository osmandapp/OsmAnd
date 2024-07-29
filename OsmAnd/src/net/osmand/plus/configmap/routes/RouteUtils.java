package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.ALPINE;
import static net.osmand.osm.OsmRouteType.BICYCLE;
import static net.osmand.osm.OsmRouteType.HIKING;
import static net.osmand.osm.OsmRouteType.HORSE;
import static net.osmand.osm.OsmRouteType.MTB;
import static net.osmand.osm.OsmRouteType.SKI;
import static net.osmand.osm.OsmRouteType.WATER;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_ROUTES;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RouteUtils {

	public static final String CYCLE_NODE_NETWORK_ROUTES_ATTR = "showCycleNodeNetworkRoutes";
	public static final String SHOW_MTB_SCALE_IMBA_TRAILS = "showMtbScaleIMBATrails";
	public static final String SHOW_MTB_SCALE = "showMtbScale";
	public static final String SHOW_MTB_SCALE_UPHILL = "showMtbScaleUphill";

	public static void showRendererSnackbarForAttr(@NonNull MapActivity activity,
	                                               @NonNull String attrName, boolean nightMode,
	                                               @Nullable CommonPreference<Boolean> pref) {
		OsmandApplication app = activity.getMyApplication();
		String renderer = getRendererForAttr(attrName);
		if (renderer != null) {
			String rendererName = RendererRegistry.getRendererName(app, renderer);
			String text = app.getString(R.string.setting_supported_by_style, rendererName);
			Snackbar snackbar = Snackbar.make(activity.getLayout(), text, Snackbar.LENGTH_LONG)
					.setAction(R.string.shared_string_change, view -> {
						RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(renderer);
						if (loaded != null) {
							app.getSettings().RENDERER.set(renderer);
							if (pref != null) {
								pref.set(!pref.get());
							}
							app.getRendererRegistry().setCurrentSelectedRender(loaded);
							activity.refreshMapComplete();
							activity.getDashboard().refreshContent(false);
						} else {
							app.showShortToastMessage(R.string.renderer_load_exception);
						}
					});
			UiUtilities.setupSnackbar(snackbar, nightMode);
			snackbar.show();
		}
	}

	public static Set<String> getRoutesAttrsNames(@NonNull List<RenderingRuleProperty> customRules) {
		Set<String> routeAttrNames = new LinkedHashSet<>(getRoutesDefaultAttrs().keySet());
		for (RenderingRuleProperty property : customRules) {
			String attrName = property.getAttrName();
			if (Algorithms.stringsEqual(property.getCategory(), UI_CATEGORY_ROUTES)
					&& !Algorithms.stringsEqual(attrName, CYCLE_NODE_NETWORK_ROUTES_ATTR)
					&& !Algorithms.stringsEqual(attrName, SHOW_MTB_SCALE)
					&& !Algorithms.stringsEqual(attrName, SHOW_MTB_SCALE_UPHILL)
					&& !Algorithms.stringsEqual(attrName, SHOW_MTB_SCALE_IMBA_TRAILS)) {
				routeAttrNames.add(attrName);
			}
		}
		return routeAttrNames;
	}

	@Nullable
	public static String getRendererForAttr(@NonNull String attrName) {
		return getRoutesDefaultAttrs().get(attrName);
	}

	public static Map<String, String> getRoutesDefaultAttrs() {
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put(BICYCLE.getRenderingPropertyAttr(), RendererRegistry.DEFAULT_RENDER);
		attrs.put(MTB.getRenderingPropertyAttr(), RendererRegistry.DEFAULT_RENDER);
		attrs.put(HIKING.getRenderingPropertyAttr(), RendererRegistry.DEFAULT_RENDER);
		attrs.put(ALPINE.getRenderingPropertyAttr(), RendererRegistry.DEFAULT_RENDER);
		attrs.put(SKI.getRenderingPropertyAttr(), RendererRegistry.WINTER_SKI_RENDER);
		attrs.put(HORSE.getRenderingPropertyAttr(), RendererRegistry.DEFAULT_RENDER);
		attrs.put(WATER.getRenderingPropertyAttr(), RendererRegistry.DEFAULT_RENDER);
		return attrs;
	}

}
