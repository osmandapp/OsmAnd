package net.osmand.plus.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseInitCallback;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.List;

@SuppressLint("Registered")
public class OsmandInAppPurchaseActivity extends AppCompatActivity implements InAppPurchaseListener {
	private static final Log LOG = PlatformUtil.getLog(OsmandInAppPurchaseActivity.class);

	private InAppPurchaseHelper purchaseHelper;
	private boolean activityDestroyed;

	@Override
	protected void onResume() {
		super.onResume();
		initInAppPurchaseHelper();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		deinitInAppPurchaseHelper();
		activityDestroyed = true;
	}

	private void initInAppPurchaseHelper() {
		deinitInAppPurchaseHelper();
		if (purchaseHelper == null) {
			OsmandApplication app = getMyApplication();
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (app.getSettings().isInternetConnectionAvailable()
					&& isInAppPurchaseAllowed()
					&& isInAppPurchaseSupported()) {
				this.purchaseHelper = purchaseHelper;
			}
		}
		if (purchaseHelper != null) {
			final WeakReference<OsmandInAppPurchaseActivity> activityRef = new WeakReference<>(this);
			purchaseHelper.isInAppPurchaseSupported(this, new InAppPurchaseInitCallback() {
				@Override
				public void onSuccess() {
					OsmandInAppPurchaseActivity activity = activityRef.get();
					if (!activityDestroyed && AndroidUtils.isActivityNotDestroyed(activity)) {
						purchaseHelper.setUiActivity(activity);
						if (purchaseHelper.needRequestInventory()) {
							purchaseHelper.requestInventory();
						}
					}
				}

				@Override
				public void onFail() {
				}
			});
		}
	}

	private void deinitInAppPurchaseHelper() {
		if (purchaseHelper != null) {
			purchaseHelper.resetUiActivity(this);
			purchaseHelper.stop();
		}
	}

	public static void purchaseFullVersion(@NonNull final Activity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		if (app != null && Version.isFreeVersion(app)) {
			if (app.isPlusVersionInApp()) {
				InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
				if (purchaseHelper != null) {
					app.logEvent("in_app_purchase_redirect");
					try {
						purchaseHelper.purchaseFullVersion(activity);
					} catch (UnsupportedOperationException e) {
						LOG.error("purchaseFullVersion is not supported", e);
					}
				}
			} else {
				app.logEvent("paid_version_redirect");
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
				app.logEvent("depth_contours_purchase_redirect");
				try {
					purchaseHelper.purchaseDepthContours(activity);
				} catch (UnsupportedOperationException e) {
					LOG.error("purchaseDepthContours is not supported", e);
				}
			}
		}
	}

	public static void purchaseContourLines(@NonNull final Activity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		if (app != null) {
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (purchaseHelper != null) {
				app.logEvent("contour_lines_purchase_redirect");
				try {
					purchaseHelper.purchaseContourLines(activity);
				} catch (UnsupportedOperationException e) {
					LOG.error("purchaseContourLines is not supported", e);
				}
			}
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
		return Version.isGooglePlayEnabled() || Version.isHuawei();
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {
		onInAppPurchaseError(taskType, error);
		fireInAppPurchaseErrorOnFragments(getSupportFragmentManager(), taskType, error);
	}

	public void fireInAppPurchaseErrorOnFragments(@NonNull FragmentManager fragmentManager,
												  InAppPurchaseTaskType taskType, String error) {
		List<Fragment> fragments = fragmentManager.getFragments();
		for (Fragment f : fragments) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).onError(taskType, error);
			}
		}
	}

	@Override
	public void onGetItems() {
		onInAppPurchaseGetItems();
		fireInAppPurchaseGetItemsOnFragments(getSupportFragmentManager());
	}

	public void fireInAppPurchaseGetItemsOnFragments(@NonNull FragmentManager fragmentManager) {
		List<Fragment> fragments = fragmentManager.getFragments();
		for (Fragment f : fragments) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).onGetItems();
			}
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if (purchaseHelper != null && purchaseHelper.getSubscriptions().containsSku(sku)) {
			getMyApplication().logEvent("live_osm_subscription_purchased");
		}
		onInAppPurchaseItemPurchased(sku);
		fireInAppPurchaseItemPurchasedOnFragments(fragmentManager, sku, active);
		if (purchaseHelper != null && purchaseHelper.getFullVersion().getSku().equals(sku)) {
			if (!(this instanceof MapActivity)) {
				finish();
			}
		}
	}

	public void fireInAppPurchaseItemPurchasedOnFragments(@NonNull FragmentManager fragmentManager,
														  String sku, boolean active) {
		List<Fragment> fragments = fragmentManager.getFragments();
		for (Fragment f : fragments) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).onItemPurchased(sku, active);
			}
		}
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
		showInAppPurchaseProgress(taskType);
		fireInAppPurchaseShowProgressOnFragments(getSupportFragmentManager(), taskType);
	}

	public void fireInAppPurchaseShowProgressOnFragments(@NonNull FragmentManager fragmentManager,
														 InAppPurchaseTaskType taskType) {
		List<Fragment> fragments = fragmentManager.getFragments();
		for (Fragment f : fragments) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).showProgress(taskType);
			}
		}
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
		dismissInAppPurchaseProgress(taskType);
		fireInAppPurchaseDismissProgressOnFragments(getSupportFragmentManager(), taskType);
	}

	public void fireInAppPurchaseDismissProgressOnFragments(@NonNull FragmentManager fragmentManager,
															InAppPurchaseTaskType taskType) {
		List<Fragment> fragments = fragmentManager.getFragments();
		for (Fragment f : fragments) {
			if (f instanceof InAppPurchaseListener && f.isAdded()) {
				((InAppPurchaseListener) f).dismissProgress(taskType);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		boolean handled = false;
		if (purchaseHelper != null) {
			handled = purchaseHelper.onActivityResult(this, requestCode, resultCode, data);
		}
		if (!handled) {
			super.onActivityResult(requestCode, resultCode, data);
		}
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
