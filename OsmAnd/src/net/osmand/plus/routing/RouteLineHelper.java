package net.osmand.plus.routing;

import android.graphics.Color;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;

// it's just a test class, maybe it's better to move methods to other places or change the architecture
public class RouteLineHelper {

	public static void saveRouteLineAppearance(@NonNull OsmandApplication app,
	                                           @NonNull ApplicationMode appMode,
	                                           @NonNull RouteLineDrawInfo drawInfo) {
		// save to settings
	}

	public static RouteLineDrawInfo createDrawInfoForAppMode(@NonNull OsmandApplication app,
	                                                         @NonNull ApplicationMode appMode) {
		Integer color = getColorFromSettings(app, appMode);
		Integer width = getWidthFromSettings(app, appMode);
		return new RouteLineDrawInfo(color, width);
	}

	public static Integer getColorFromSettings(@NonNull OsmandApplication app,
	                                           @NonNull ApplicationMode appMode) {
		return null;
	}

	public static Integer getWidthFromSettings(@NonNull OsmandApplication app,
	                                           @NonNull ApplicationMode appMode) {
		return null;
	}

	public static int getColor(@NonNull RouteLineDrawInfo drawInfo) {
		if (drawInfo.getColor() != null) {
			return drawInfo.getColor();
		}
		return getMapStyleColor();
	}

	public static int getWidth(@NonNull RouteLineDrawInfo drawInfo) {
		if (drawInfo.getWidth() != null) {
			return drawInfo.getWidth();
		}
		return getMapStyleWidth();
	}

	public static int getMapStyleColor() {
		// get color from selected map style
		return Color.BLUE;
	}

	public static int getMapStyleWidth() {
		// get width from selected map style
		return 10;
	}
}
