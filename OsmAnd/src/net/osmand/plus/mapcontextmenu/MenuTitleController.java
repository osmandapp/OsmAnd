package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public abstract class MenuTitleController {

	protected int rightIconId;
	protected Drawable rightIcon;
	protected String nameStr = "";
	protected String typeStr = "";
	protected String commonTypeStr = "";
	protected Drawable secondLineTypeIcon;
	protected String streetStr = "";

	private AddressLookupRequest addressLookupRequest;

	protected String searchAddressStr;
	protected String addressNotFoundStr;

	public abstract MapActivity getMapActivity();

	public abstract LatLon getLatLon();

	public abstract PointDescription getPointDescription();

	public abstract Object getObject();

	public abstract MenuController getMenuController();

	public String getTitleStr() {
		if (displayStreetNameInTitle() && searchingAddress()) {
			return searchAddressStr;
		} else {
			return nameStr;
		}
	}

	public boolean searchingAddress() {
		return addressLookupRequest != null;
	}

	public void cancelSearchAddress() {
		if (addressLookupRequest != null) {
			getMapActivity().getMyApplication().getGeocodingLookupService().cancel(addressLookupRequest);
			addressLookupRequest = null;
			onSearchAddressDone();
		}
	}

	public boolean displayStreetNameInTitle() {
		MenuController menuController = getMenuController();
		return menuController != null && menuController.displayStreetNameInTitle();
	}

	// Has title which does not equal to "Looking up address" and "No address determined"
	public boolean hasValidTitle() {
		String title = getTitleStr();
		return !addressNotFoundStr.equals(title) && !searchAddressStr.equals(title);
	}

	public int getRightIconId() {
		return rightIconId;
	}

	public Drawable getRightIcon() {
		return rightIcon;
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

	public String getStreetStr() {
		if (needStreetName()) {
			if (searchingAddress()) {
				return searchAddressStr;
			} else {
				return streetStr;
			}
		} else {
			return "";
		}
	}

	protected void initTitle() {
		searchAddressStr = PointDescription.getSearchAddressStr(getMapActivity());
		addressNotFoundStr = PointDescription.getAddressNotFoundStr(getMapActivity());

		if (searchingAddress()) {
			cancelSearchAddress();
		}

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

		rightIconId = 0;
		rightIcon = null;
		secondLineTypeIcon = null;

		if (menuController != null) {
			rightIconId = menuController.getRightIconId();
			rightIcon = menuController.getRightIcon();
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
			nameStr = typeStr;
			typeStr = commonTypeStr;
		} else if (Algorithms.isEmpty(typeStr)) {
			typeStr = commonTypeStr;
		}
	}

	protected void acquireStreetName() {
		addressLookupRequest = new AddressLookupRequest(getLatLon(), new GeocodingLookupService.OnAddressLookupResult() {
			@Override
			public void geocodingDone(String address) {
				if (addressLookupRequest != null) {
					addressLookupRequest = null;
					if (Algorithms.isEmpty(address)) {
						streetStr = PointDescription.getAddressNotFoundStr(getMapActivity());
					} else {
						streetStr = address;
					}

					if (displayStreetNameInTitle()) {
						nameStr = streetStr;
						getPointDescription().setName(nameStr);
					}
					onSearchAddressDone();
				}
			}
		}, new GeocodingLookupService.OnAddressLookupProgress() {
			@Override
			public void geocodingInProgress() {
				// animate three dots
			}
		});

		getMapActivity().getMyApplication().getGeocodingLookupService().lookupAddress(addressLookupRequest);
	}

	protected void onSearchAddressDone() {
	}

}
