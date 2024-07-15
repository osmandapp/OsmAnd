package net.osmand.plus.mapcontextmenu.editors.icon;

import androidx.annotation.NonNull;

import net.osmand.OnResultCallback;
import net.osmand.osm.PoiType;
import net.osmand.plus.render.RenderingIcons;

public class EditorIconUtils {

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
