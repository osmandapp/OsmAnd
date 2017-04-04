package net.osmand.plus.srtmplugin;

import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.RouteLayer;
import net.osmand.render.RenderingRuleProperty;

import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_DISABLED_VALUE;

public class ContourLinesMenu {
	private static final String TAG = "ContourLinesMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		createLayersItems(adapter, mapActivity);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
										  final MapActivity mapActivity) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp == null) {
			return;
		}
		final OsmandSettings.CommonPreference<String> pref =
				mapActivity.getMyApplication().getSettings().getCustomRenderProperty(contourLinesProp.getAttrName());

		final boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);
		final int toggleActionStringId = selected ? R.string.shared_string_enabled : R.string.shared_string_disabled;
		final int showZoomLevelStringId = R.string.show_from_zoom_level;
		final int colorSchemeStringId = R.string.srtm_color_scheme;
		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
										  View view, int itemId, int pos) {
				if (itemId == showZoomLevelStringId) {
					if (selected) {
						// todo select zoom level
					}
					return false;
				}
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
											  final int itemId, final int pos, final boolean isChecked) {
				/*
				if (itemId == toggleActionStringId) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							plugin.toggleUnderlayState(mapActivity, type, onMapSelectedCallback);
							ConfigureMapMenu.refreshMapComplete(mapActivity);
						}
					});
				} else if (itemId == colorSchemeStringId) {
					hidePolygonsPref.set(!isChecked);
					refreshMapComplete(mapActivity);
				}
				*/
				return false;
			}
		};
	}

	public static void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		GPXLayer gpx = activity.getMapView().getLayerByClass(GPXLayer.class);
		if (gpx != null) {
			gpx.updateLayerStyle();
		}
		RouteLayer rte = activity.getMapView().getLayerByClass(RouteLayer.class);
		if (rte != null) {
			rte.updateLayerStyle();
		}
		activity.getMapView().refreshMap(true);
	}
}
