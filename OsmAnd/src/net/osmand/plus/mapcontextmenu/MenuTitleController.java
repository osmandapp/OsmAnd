package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.Algorithms;

public abstract class MenuTitleController {

	protected int leftIconId;
	protected Drawable leftIcon;
	protected String nameStr = "";
	protected String typeStr = "";
	protected String commonTypeStr = "";
	protected Drawable secondLineTypeIcon;
	protected String streetStr = "";
	protected String addressNotKnownStr;

	public abstract MapActivity getMapActivity();

	public abstract LatLon getLatLon();

	public abstract PointDescription getPointDescription();

	public abstract Object getObject();

	public abstract MenuController getMenuController();

	public String getTitleStr() {
		return nameStr;
	}

	public int getLeftIconId() {
		return leftIconId;
	}

	public Drawable getLeftIcon() {
		return leftIcon;
	}

	public Drawable getTypeIcon() {
		return secondLineTypeIcon;
	}

	public String getTypeStr() {
		MenuController menuController = getMenuController();
		if (menuController != null && menuController.needTypeStr()) {
			return typeStr;
		} else {
			return "";
		}
	}

	public String getCommonTypeStr() {
		return commonTypeStr;
	}

	public String getStreetStr() {
		MenuController menuController = getMenuController();
		if (menuController != null && menuController.needStreetName()) {
			return streetStr;
		} else {
			return "";
		}
	}

	protected void initTitle() {
		addressNotKnownStr = getMapActivity().getString(R.string.address_unknown);
		acquireIcons();
		acquireNameAndType();
		if (needStreetName()) {
			acquireStreetName();
		}
	}

	protected boolean needStreetName() {
		MenuController menuController = getMenuController();
		boolean res = getObject() != null || Algorithms.isEmpty(getPointDescription().getName());
		if (res && menuController != null) {
			res = menuController.needStreetName();
		}
		return res;
	}

	protected void acquireIcons() {
		MenuController menuController = getMenuController();

		leftIconId = 0;
		leftIcon = null;
		secondLineTypeIcon = null;

		if (menuController != null) {
			leftIconId = menuController.getLeftIconId();
			leftIcon = menuController.getLeftIcon();
			secondLineTypeIcon = menuController.getSecondLineTypeIcon();
		}
	}

	protected void acquireNameAndType() {
		nameStr = "";
		typeStr = "";
		commonTypeStr = "";
		streetStr = "";

		MenuController menuController = getMenuController();
		if (menuController != null) {
			nameStr = menuController.getNameStr();
			typeStr = menuController.getTypeStr();
			commonTypeStr = menuController.getCommonTypeStr();
		}

		if (Algorithms.isEmpty(nameStr)) {
			if (!Algorithms.isEmpty(typeStr)) {
				nameStr = typeStr;
				typeStr = commonTypeStr;
			} else {
				nameStr = addressNotKnownStr;
				typeStr = commonTypeStr;
			}
		} else if (Algorithms.isEmpty(typeStr)) {
			typeStr = commonTypeStr;
		}
	}

	protected void acquireStreetName() {
		Location ll = new Location("");
		ll.setLatitude(getLatLon().getLatitude());
		ll.setLongitude(getLatLon().getLongitude());
		getMapActivity().getMyApplication().getLocationProvider()
				.getRouteSegment(ll, new ResultMatcher<RouteDataObject>() {

					@Override	
					public boolean publish(RouteDataObject object) {
						if (object != null) {
							OsmandSettings settings = getMapActivity().getMyApplication().getSettings();
							String streetName = object.getName(settings.MAP_PREFERRED_LOCALE.get());
							String ref = object.getRef();
							if(Algorithms.isEmpty(streetName)) {
								streetName = "";
							}
							if(!Algorithms.isEmpty(ref)) {
								if(!Algorithms.isEmpty(streetName)) {
									streetName += ", ";
								}
								streetName += ref;
							}
							streetStr = streetName;
							if (!Algorithms.isEmpty(streetStr)) {
								MenuController menuController = getMenuController();
								if (menuController == null || menuController.displayStreetNameInTitle()) {
									nameStr = streetStr;
									getPointDescription().setName(nameStr);
								}
								getMapActivity().runOnUiThread(new Runnable() {
									public void run() {
										refreshMenuTitle();
									}
								});
							}
						}
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}

				});
	}

	protected abstract void refreshMenuTitle();

}
