package net.osmand.plus.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import org.apache.commons.logging.Log;

@SuppressLint("Registered")
public class OsmandInAppPurchaseActivity extends AppCompatActivity implements InAppPurchaseListener {
	private static final Log LOG = PlatformUtil.getLog(OsmandInAppPurchaseActivity.class);

	private InAppPurchaseHelper purchaseHelper;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isInAppPurchaseAllowed() && isInAppPurchaseSupported()) {
			purchaseHelper = getMyApplication().getInAppPurchaseHelper();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		initInAppPurchaseHelper();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		deinitInAppPurchaseHelper();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Pass on the activity result to the helper for handling
		if (purchaseHelper == null || !purchaseHelper.onActivityResultHandled(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void initInAppPurchaseHelper() {
		deinitInAppPurchaseHelper();

		if (purchaseHelper != null) {
			purchaseHelper.addListener(this);
			if (purchaseHelper.needRequestInventory()) {
				purchaseHelper.requestInventory();
			}
		}
	}

	private void deinitInAppPurchaseHelper() {
		if (purchaseHelper != null) {
			purchaseHelper.removeListener(this);
			purchaseHelper.stop();
		}
	}

	public static void purchaseFullVersion(@NonNull final Activity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		if (app != null && Version.isFreeVersion(app)) {
			if (app.isPlusVersionInApp()) {
				InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
				if (purchaseHelper != null) {
					app.logEvent(activity, "in_app_purchase_redirect");
					purchaseHelper.purchaseFullVersion(activity);
				}
			} else {
				app.logEvent(activity, "paid_version_redirect");
				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(Version.getUrlWithUtmRef(app, "net.osmand.plus")));
				try {
					activity.startActivity(intent);
				} catch (ActivityNotFoundException e) {
					LOG.error("ActivityNotFoundException", e);
				}
			}
		}
	}

	public static void purchaseDepthContours(@NonNull final Activity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		if (app != null) {
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (purchaseHelper != null) {
				app.logEvent(activity, "depth_contours_purchase_redirect");
				purchaseHelper.purchaseDepthContours(activity);
			}
		}
	}

	public static void purchaseSrtmPlugin(@NonNull final Activity activity) {
		OsmandPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		if(plugin == null || plugin.getInstallURL() == null) {
			Toast.makeText(activity.getApplicationContext(),
					activity.getString(R.string.activate_srtm_plugin), Toast.LENGTH_LONG).show();
		} else {
			activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Nullable
	public InAppPurchaseHelper getPurchaseHelper() {
		return purchaseHelper;
	}

	public boolean isInAppPurchaseAllowed() {
		return false;
	}

	public boolean isInAppPurchaseSupported() {
		return Version.isGooglePlayEnabled(getMyApplication());
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {
		onInAppPurchaseError(taskType, error);
	}

	@Override
	public void onGetItems() {
		onInAppPurchaseGetItems();
	}

	@Override
	public void onItemPurchased(String sku) {
		onInAppPurchaseItemPurchased(sku);
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
		showInAppPurchaseProgress(taskType);
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
		dismissInAppPurchaseProgress(taskType);
	}

	public void onInAppPurchaseError(InAppPurchaseTaskType taskType, String error) {
		// not implemented
	}

	public void onInAppPurchaseGetItems() {
		// not implemented
	}

	public void onInAppPurchaseItemPurchased(String sku) {
		// not implemented
	}

	public void showInAppPurchaseProgress(InAppPurchaseTaskType taskType) {
		// not implemented
	}

	public void dismissInAppPurchaseProgress(InAppPurchaseTaskType taskType) {
		// not implemented
	}
}
