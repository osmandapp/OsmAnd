package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AProfile extends AidlParams {

	public static final String PROFILE_ID_KEY = "profile_id";
	public static final String USER_PROFILE_NAME_KEY = "user_profile_name";
	public static final String PARENT_KEY = "parent";
	public static final String ICON_NAME_KEY = "icon_name";
	public static final String ICON_COLOR_KEY = "icon_color";
	public static final String ROUTING_PROFILE_KEY = "routing_profile";
	public static final String ROUTE_SERVICE_KEY = "route_service";
	public static final String LOC_ICON_KEY = "loc_icon";
	public static final String NAV_ICON_KEY = "nav_icon";
	public static final String ORDER_KEY = "order";
	public static final String VERSION_KEY = "version";

	private String appModeKey;
	private String userProfileName;
	private String parent;
	private String iconName;
	private String iconColor;
	private String routingProfile;
	private String routeService;
	private String locIcon;
	private String navIcon;
	private int order = -1;
	private int version = -1;

	public AProfile(String appModeKey, String userProfileName, String parent, String iconName, String iconColor,
					String routingProfile, String routeService, String locIcon, String navIcon, int order) {
		this.appModeKey = appModeKey;
		this.userProfileName = userProfileName;
		this.parent = parent;
		this.iconName = iconName;
		this.iconColor = iconColor;
		this.routingProfile = routingProfile;
		this.routeService = routeService;
		this.locIcon = locIcon;
		this.navIcon = navIcon;
		this.order = order;
	}

	public AProfile(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AProfile> CREATOR = new Creator<AProfile>() {
		@Override
		public AProfile createFromParcel(Parcel in) {
			return new AProfile(in);
		}

		@Override
		public AProfile[] newArray(int size) {
			return new AProfile[size];
		}
	};

	public String getStringKey() {
		return appModeKey;
	}

	public String getUserProfileName() {
		return userProfileName;
	}

	public String getParent() {
		return parent;
	}

	public String getIconName() {
		return iconName;
	}

	public String getIconColor() {
		return iconColor;
	}

	public String getRoutingProfile() {
		return routingProfile;
	}

	public String getRouteService() {
		return routeService;
	}

	public String getLocIcon() {
		return locIcon;
	}

	public String getNavIcon() {
		return navIcon;
	}

	public int getOrder() {
		return order;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString(PROFILE_ID_KEY, appModeKey);
		bundle.putString(USER_PROFILE_NAME_KEY, userProfileName);
		bundle.putString(PARENT_KEY, parent);
		bundle.putString(ICON_NAME_KEY, iconName);
		bundle.putString(ICON_COLOR_KEY, iconColor);
		bundle.putString(ROUTING_PROFILE_KEY, routingProfile);
		bundle.putString(ROUTE_SERVICE_KEY, routeService);
		bundle.putString(LOC_ICON_KEY, locIcon);
		bundle.putString(NAV_ICON_KEY, navIcon);
		bundle.putInt(ORDER_KEY, order);
		bundle.putInt(VERSION_KEY, version);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		appModeKey = bundle.getString(PROFILE_ID_KEY);
		userProfileName = bundle.getString(USER_PROFILE_NAME_KEY);
		parent = bundle.getString(PARENT_KEY);
		iconName = bundle.getString(ICON_NAME_KEY);
		iconColor = bundle.getString(ICON_COLOR_KEY);
		routingProfile = bundle.getString(ROUTING_PROFILE_KEY);
		routeService = bundle.getString(ROUTE_SERVICE_KEY);
		locIcon = bundle.getString(LOC_ICON_KEY);
		navIcon = bundle.getString(NAV_ICON_KEY);
		order = bundle.getInt(ORDER_KEY);
		version = bundle.getInt(VERSION_KEY);
	}
}