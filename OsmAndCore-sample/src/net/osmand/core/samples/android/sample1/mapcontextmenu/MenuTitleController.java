package net.osmand.core.samples.android.sample1.mapcontextmenu;

import android.graphics.drawable.Drawable;

import net.osmand.core.samples.android.sample1.GeocodingLookupService;
import net.osmand.core.samples.android.sample1.GeocodingLookupService.AddressLookupRequest;
import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;

public abstract class MenuTitleController {

	protected int leftIconId;
	protected Drawable leftIcon;
	protected String nameStr = "";
	protected String typeStr = "";
	protected String commonTypeStr = "";
	protected Drawable secondLineTypeIcon;
	protected String streetStr = "";

	private AddressLookupRequest addressLookupRequest;

	protected String searchAddressStr;
	protected String addressNotFoundStr;

	public abstract MainActivity getMainActivity();

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
			getMainActivity().getMyApplication().getGeocodingLookupService().cancel(addressLookupRequest);
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
		searchAddressStr = PointDescription.getSearchAddressStr(getMainActivity().getMyApplication());
		addressNotFoundStr = PointDescription.getAddressNotFoundStr(getMainActivity().getMyApplication());

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
						streetStr = PointDescription.getAddressNotFoundStr(getMainActivity().getMyApplication());
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

		getMainActivity().getMyApplication().getGeocodingLookupService().lookupAddress(addressLookupRequest);
	}

	protected void onSearchAddressDone() {
	}

}
