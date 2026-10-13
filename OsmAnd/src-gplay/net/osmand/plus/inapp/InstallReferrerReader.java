package net.osmand.plus.inapp;

import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse;
import com.android.installreferrer.api.InstallReferrerStateListener;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

public class InstallReferrerReader {

	private static final Log LOG = PlatformUtil.getLog(InstallReferrerReader.class);

	public static void readOnce(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (settings.INSTALL_REFERRER_CHECKED.get()) {
			return;
		}
		try {
			InstallReferrerClient client = InstallReferrerClient.newBuilder(app).build();
			client.startConnection(new InstallReferrerStateListener() {
				@Override
				public void onInstallReferrerSetupFinished(int responseCode) {
					try {
						if (responseCode == InstallReferrerResponse.OK) {
							String referrer = client.getInstallReferrer().getInstallReferrer();
							settings.INSTALL_REFERRER.set(InstallReferrer.filter(referrer));
							settings.INSTALL_REFERRER_CHECKED.set(true);
						} else if (responseCode == InstallReferrerResponse.FEATURE_NOT_SUPPORTED) {
							settings.INSTALL_REFERRER_CHECKED.set(true);
						}
						// SERVICE_UNAVAILABLE and the like: try again on the next start
					} catch (RemoteException | RuntimeException e) {
						LOG.error("Install referrer error", e);
					} finally {
						client.endConnection();
					}
				}

				@Override
				public void onInstallReferrerServiceDisconnected() {
				}
			});
		} catch (RuntimeException e) {
			LOG.error("Install referrer error", e);
		}
	}
}
