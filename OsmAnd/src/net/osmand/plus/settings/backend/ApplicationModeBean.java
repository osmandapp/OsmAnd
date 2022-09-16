package net.osmand.plus.settings.backend;

import com.google.gson.annotations.Expose;

import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.profiles.NavigationIcon;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.routing.RouteService;

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
	public LocationIcon locIcon;
	@Expose
	public NavigationIcon navIcon;
	@Expose
	public int order = -1;
	@Expose
	public int version = -1;
}
