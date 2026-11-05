package net.osmand.plus.settings.backend;

import static net.osmand.plus.settings.backend.ApplicationMode.CAR;
import static net.osmand.plus.settings.backend.ApplicationMode.CUSTOM_MODE_KEY_SEPARATOR;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

public class ApplicationModeBean {
	@Expose
	public String stringKey;
	@Expose
	public String userProfileName;
	@Expose
	public String parent;
	@Expose
	public String iconName = "ic_world_globe_dark";
	@Expose
	public ProfileIconColors iconColor = ProfileIconColors.DEFAULT;
	@Expose
	public Integer customIconColor;
	@Expose
	public String routingProfile;
	@Expose
	public RouteService routeService = RouteService.OSMAND;
	@Expose
	public String locIcon;
	@Expose
	public String navIcon;
	@Expose
	public int order = -1;
	@Expose
	public int version = -1;


	public static void checkAndReplaceInvalidValues(@NonNull OsmandApplication app, @NonNull ApplicationModeBean modeBean) {
		if (ApplicationMode.isCustomProfile(modeBean.stringKey)) {
			checkAndReplaceInvalidParent(modeBean);
		}
		checkAndReplaceInvalidIconName(app, modeBean);
	}

	private static void checkAndReplaceInvalidParent(@NonNull ApplicationModeBean modeBean) {
		ApplicationMode parent = ApplicationMode.valueOfStringKey(modeBean.parent, null);
		if (parent == null) {
			if (!Algorithms.isEmpty(modeBean.parent)) {
				int index = modeBean.parent.indexOf(CUSTOM_MODE_KEY_SEPARATOR);
				if (index > 0) {
					String parentKey = modeBean.parent.substring(0, index);
					parent = ApplicationMode.valueOfStringKey(parentKey, null);
				}
			}
			if (parent == null) {
				parent = CAR;
			}
			modeBean.parent = parent.getStringKey();
		}
	}

	private static void checkAndReplaceInvalidIconName(@NonNull OsmandApplication app, @NonNull ApplicationModeBean modeBean) {
		if (AndroidUtils.getDrawableId(app, modeBean.iconName) == 0) {
			ApplicationMode appMode = ApplicationMode.valueOfStringKey(modeBean.stringKey, null);
			if (appMode == null) {
				appMode = ApplicationMode.valueOfStringKey(modeBean.parent, null);
			}
			if (appMode != null) {
				modeBean.iconName = appMode.getIconName();
			}
		}
	}
}
