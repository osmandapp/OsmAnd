package net.osmand.plus.inapp.util;

import androidx.annotation.Nullable;

public class IapManager {

	private UserIapData userIapData;

	public IapManager() {
	}

	/**
	 * Method to set the app's amazon user id and marketplace from IAP SDK
	 * responses.
	 *
	 * @param newAmazonUserId      - new amazon user id
	 * @param newAmazonMarketplace - new amazon market place
	 */
	public UserIapData setAmazonUserId(final @Nullable String newAmazonUserId, final @Nullable String newAmazonMarketplace) {
		// Reload everything if the Amazon user has changed.
		if (newAmazonUserId == null) {
			// A null user id typically means there is no registered Amazon
			// account.
			if (userIapData != null) {
				userIapData = null;
			}
		} else if (userIapData == null || !newAmazonUserId.equals(userIapData.getAmazonUserId())) {
			// If there was no existing Amazon user then either no customer was
			// previously registered or the application has just started.

			// If the user id does not match then another Amazon user has
			// registered.
			userIapData = new UserIapData(newAmazonUserId, newAmazonMarketplace);
		}
		return userIapData;
	}

	public UserIapData getUserIapData() {
		return this.userIapData;
	}
}
