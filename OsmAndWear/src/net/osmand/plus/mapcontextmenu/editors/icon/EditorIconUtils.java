package net.osmand.plus.mapcontextmenu.editors.icon;

import androidx.annotation.NonNull;

import net.osmand.OnResultCallback;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.primitives.RouteActivity;

public class EditorIconUtils {

	private static final String SAMPLE_ICON_KEY = "ic_sample";

	public static void retrieveIconKey(@NonNull OsmandApplication app,
	                                   @NonNull RouteActivity routeActivity,
	                                   @NonNull OnResultCallback<String> onSuccessCallback) {
		String name = routeActivity.getIconName();
		if (!SAMPLE_ICON_KEY.equalsIgnoreCase(name) && AndroidUtils.hasDrawableId(app, name)) {
			onSuccessCallback.onResult(name);
		}
	}

	public static void retrieveIconKey(@NonNull PoiType poiType,
	                                   @NonNull OnResultCallback<String> onSuccessCallback) {
		String iconKey = poiType.getIconKeyName();
		if (!isIconResourceAvailable(iconKey)) {
			String osmIconKey = poiType.getOsmTag() + "_" + poiType.getOsmValue();
			iconKey = isIconResourceAvailable(osmIconKey) ? osmIconKey : null;
		}
		if (iconKey != null) {
			onSuccessCallback.onResult(iconKey);
		}
	}

	private static boolean isIconResourceAvailable(@NonNull String iconKey) {
		return RenderingIcons.containsBigIcon(iconKey);
	}
}
