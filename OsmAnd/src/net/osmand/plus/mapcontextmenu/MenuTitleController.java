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
	protected Drawable secondLineIcon;
	protected String streetStr = "";
	protected boolean addressUnknown = false;

	public abstract MapActivity getMapActivity();

	public abstract LatLon getLatLon();

	public abstract PointDescription getPointDescription();

	public abstract Object getObject();

	public abstract MenuController getMenuController();

	public String getTitleStr() {
		return nameStr;
	}

	public boolean isAddressUnknown() {
		return addressUnknown;
	}

	public int getLeftIconId() {
		return leftIconId;
	}

	public Drawable getLeftIcon() {
		return leftIcon;
	}

	public Drawable getSecondLineIcon() {
		return secondLineIcon;
	}

	public String getLocationStr() {
		MenuController menuController = getMenuController();
		if (menuController != null && menuController.needTypeStr()) {
			return typeStr;
		} else {
			if (Algorithms.isEmpty(streetStr)) {
				return PointDescription.getLocationName(getMapActivity(),
						getLatLon().getLatitude(), getLatLon().getLongitude(), true).replaceAll("\n", "");
			} else {
				return streetStr;
			}
		}
	}

	protected void initTitle() {
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
		secondLineIcon = null;

		if (menuController != null) {
			leftIconId = menuController.getLeftIconId();
			leftIcon = menuController.getLeftIcon();
			secondLineIcon = menuController.getSecondLineIcon();
		}
	}

	protected void acquireNameAndType() {
		MenuController menuController = getMenuController();
		PointDescription pointDescription = getPointDescription();
		if (menuController != null) {
			nameStr = menuController.getNameStr();
			typeStr = menuController.getTypeStr();
		}

		if (Algorithms.isEmpty(nameStr)) {
			nameStr = pointDescription.getName();
		}
		if (Algorithms.isEmpty(typeStr)) {
			typeStr = pointDescription.getTypeName();
		}

		if (Algorithms.isEmpty(nameStr)) {
			if (!Algorithms.isEmpty(typeStr)) {
				nameStr = typeStr;
				typeStr = "";
			} else {
				nameStr = getMapActivity().getString(R.string.address_unknown);
				addressUnknown = true;
			}
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
							streetStr = RoutingHelper.formatStreetName(object.getName(settings.MAP_PREFERRED_LOCALE.get()),
									object.getRef(), object.getDestinationName(settings.MAP_PREFERRED_LOCALE.get()));

							if (!Algorithms.isEmpty(streetStr)) {
								if (getObject() == null) {
									nameStr = streetStr;
									addressUnknown = false;
									streetStr = "";
								}
								getMapActivity().runOnUiThread(new Runnable() {
									public void run() {
										refreshMenuTitle();
									}
								});
							}
						} else {
							streetStr = "";
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
