package net.osmand.plus.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hms.jos.AppUpdateClient;
import com.huawei.hms.jos.JosApps;
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo;
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.io.Serializable;

public class OsmandBaseActivity extends AppCompatActivity {

	private static final Log LOG = PlatformUtil.getLog(OsmandBaseActivity.class);

	private static boolean hasInit = false;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!hasInit) {
			hasInit = true;
			checkUpdate();
		}
	}

	public void checkUpdate() {
		AppUpdateClient client = JosApps.getAppUpdateClient(this);
		client.checkAppUpdate(this, new UpdateCallBack(this));
	}

	private static class UpdateCallBack implements CheckUpdateCallBack {
		private final OsmandBaseActivity activity;

		private UpdateCallBack(OsmandBaseActivity activity) {
			this.activity = activity;
		}

		/**
		 * Get update info from appmarket
		 * *
		 * @param intent see detail:
		 *        https://developer.huawei.com/consumer/cn/doc/development/HMS-References/appupdateclient#intent
		 */
		@Override
		public void onUpdateInfo(Intent intent) {
			if (intent != null) {
				Serializable info = intent.getSerializableExtra("updatesdk_update_info");
				if (info instanceof ApkUpgradeInfo) {
					LOG.info("Check update success");
					AppUpdateClient client = JosApps.getAppUpdateClient(activity);
					// show update dialog
					client.showUpdateDialog(activity, (ApkUpgradeInfo) info, false);
				} else {
					LOG.warn("Check update failed");
				}
			}
		}

		// ignored
		@Override
		public void onMarketInstallInfo(Intent intent) {
			LOG.warn("Check update failed");
		}

		// ignored
		@Override
		public void onMarketStoreError(int responseCode) {
			LOG.warn("Check update failed: " + responseCode);
		}

		// ignored
		@Override
		public void onUpdateStoreError(int responseCode) {
			LOG.warn("Check update failed: " + responseCode);
		}
	}

}
