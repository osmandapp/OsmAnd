package net.osmand.plus.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.jos.AppUpdateClient;
import com.huawei.hms.jos.JosApps;
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo;
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack;
import com.huawei.updatesdk.service.otaupdate.UpdateKey;

import net.osmand.PlatformUtil;
import net.osmand.plus.utils.AndroidUtils;

import java.io.Serializable;
import java.lang.ref.WeakReference;

public class ExtendedMapActivity {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ExtendedMapActivity.class);

	private static boolean updateChecked = false;

	private void checkUpdate(@NonNull MapActivity mapActivity) {
		AppUpdateClient client = JosApps.getAppUpdateClient(mapActivity);
		client.checkAppUpdate(mapActivity, new UpdateCallBack(mapActivity));
	}

	private void showUpdateDialog(@NonNull MapActivity mapActivity, @NonNull ApkUpgradeInfo info) {
		AppUpdateClient client = JosApps.getAppUpdateClient(mapActivity);
		client.showUpdateDialog(mapActivity, info, false);
	}

	private class UpdateCallBack implements CheckUpdateCallBack {

		private final WeakReference<MapActivity> mapActivityRef;

		public UpdateCallBack(@NonNull MapActivity mapActivity) {
			this.mapActivityRef = new WeakReference<>(mapActivity);
		}

		@Override
		public void onUpdateInfo(Intent intent) {
			if (intent != null) {
				int status = intent.getIntExtra(UpdateKey.STATUS, -1);
				int rtnCode = intent.getIntExtra(UpdateKey.FAIL_CODE, -1);
				String rtnMessage = intent.getStringExtra(UpdateKey.FAIL_REASON);
				Serializable info = intent.getSerializableExtra(UpdateKey.INFO);
				MapActivity mapActivity = mapActivityRef.get();
				if (AndroidUtils.isActivityNotDestroyed(mapActivity) && info instanceof ApkUpgradeInfo) {
					showUpdateDialog(mapActivity, (ApkUpgradeInfo) info);
				}
				LOG.info("onUpdateInfo status: " + status + ", rtnCode: " + rtnCode + ", rtnMessage: " + rtnMessage);
			}
		}

		@Override
		public void onMarketInstallInfo(Intent intent) {
			LOG.error("Check update failed (onMarketInstallInfo)");
		}

		@Override
		public void onMarketStoreError(int responseCode) {
			LOG.error("Check update failed (onMarketStoreError: " +responseCode + ")");
		}

		@Override
		public void onUpdateStoreError(int responseCode) {
			LOG.error("Check update failed (onUpdateStoreError: " +responseCode + ")");
		}
	}

	void onCreate(@NonNull MapActivity mapActivity, @Nullable Bundle savedInstanceState) {
	}

	void onSaveInstanceState(@NonNull MapActivity mapActivity, @NonNull Bundle outState) {
	}

	void onStart(@NonNull MapActivity mapActivity) {
	}

	void onResume(@NonNull MapActivity mapActivity) {
		if (!updateChecked) {
			checkUpdate(mapActivity);
			updateChecked = true;
		}
	}

	void onPause(@NonNull MapActivity mapActivity) {
	}

	void onStop(@NonNull MapActivity mapActivity) {
	}

	void onDestroy(@NonNull MapActivity mapActivity) {
	}

	void onActivityResult(@NonNull MapActivity mapActivity, int requestCode, int resultCode, Intent data) {
	}
}
