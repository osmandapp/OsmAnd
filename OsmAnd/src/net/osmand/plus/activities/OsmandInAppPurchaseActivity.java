package net.osmand.plus.activities;

import static net.osmand.plus.Version.FULL_VERSION_NAME;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseInitCallback;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.TalkbackUtils;
import net.osmand.plus.utils.TalkbackUtils.TalkbackHandler;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.List;

public class OsmandInAppPurchaseActivity extends AppCompatActivity implements InAppPurchaseListener, TalkbackHandler {

	private static final Log LOG = PlatformUtil.getLog(OsmandInAppPurchaseActivity.class);

	protected OsmandApplication app;
	protected OsmandSettings settings;

	private InAppPurchaseHelper purchaseHelper;
	private boolean activityDestroyed;
	private boolean activityHiddenForTalkback = false;
	private FragmentLifecycleCallbacks lifecycleCallbacks = TalkbackUtils.getLifecycleCallbacks(this);

	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(newBase);

		this.app = (OsmandApplication) newBase.getApplicationContext();
		this.settings = app.getSettings();
	}

	@Override
	protected void onResume() {
		super.onResume();
		initInAppPurchaseHelper();
		getSupportFragmentManager().registerFragmentLifecycleCallbacks(lifecycleCallbacks, false);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(lifecycleCallbacks);
	}

	@Override
	public void setActivityAccessibility(boolean hideActivity) {
		List<View> views = getHidingViews();
		if (views == null) {
			View view = getWindow().getDecorView();
			TalkbackUtils.setActivityViewsAccessibility(view, hideActivity, this);
		} else {
			for (View hidingView : views) {
				hidingView.setImportantForAccessibility(hideActivity ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : View.IMPORTANT_FOR_ACCESSIBILITY_YES);
			}
		}
	}

	@Nullable
	protected List<View> getHidingViews() {
		return null;
	}

	@Override
	public List<Fragment> getActiveTalkbackFragments() {
		return getSupportFragmentManager().getFragments();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopInAppPurchaseHelper();
		activityDestroyed = true;
	}

	private void initInAppPurchaseHelper() {
		stopInAppPurchaseHelper();
		if (purchaseHelper == null) {
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (settings.isInternetConnectionAvailable()
					&& isInAppPurchaseAllowed()
					&& Version.isInAppPurchaseSupported()) {
				this.purchaseHelper = purchaseHelper;
			}
		}
		if (purchaseHelper != null) {
			WeakReference<OsmandInAppPurchaseActivity> activityRef = new WeakReference<>(this);
			purchaseHelper.isInAppPurchaseSupported(this, new InAppPurchaseInitCallback() {
				@Override
				public void onSuccess() {
					OsmandInAppPurchaseActivity activity = activityRef.get();
					if (!activityDestroyed && AndroidUtils.isActivityNotDestroyed(activity)) {
						purchaseHelper.setUiActivity(activity);
						if (purchaseHelper.needRequestInventory()) {
							purchaseHelper.requestInventory(false);
						}
					}
				}

				@Override
				public void onFail() {
				}
			});
		} else if (isInAppPurchaseAllowed() && settings.isInternetConnectionAvailable()) {
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			if (purchaseHelper != null && purchaseHelper.needRequestPromo()) {
				purchaseHelper.checkPromoAsync(null);
			}
		}
	}

	private void stopInAppPurchaseHelper() {
		if (purchaseHelper != null) {
			purchaseHelper.resetUiActivity(this);
			purchaseHelper.stop();
		}
	}

	public static void purchaseFullVersion(@NonNull Activity activity) {
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
						Uri.parse(Version.getUrlWithUtmRef(app, FULL_VERSION_NAME)));
				AndroidUtils.startActivityIfSafe(activity, intent);
			}
		}
	}

	public static void purchaseDepthContours(@NonNull Activity activity) {
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

	public static void purchaseContourLines(@NonNull Activity activity) {
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

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public OsmandSettings getSettings() {
		return settings;
	}

	@Nullable
	public InAppPurchaseHelper getPurchaseHelper() {
		return purchaseHelper;
	}

	public boolean isInAppPurchaseAllowed() {
		return false;
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
			app.logEvent("live_osm_subscription_purchased");
		}
		onInAppPurchaseItemPurchased(sku);
		fireInAppPurchaseItemPurchasedOnFragments(fragmentManager, sku, active);
		InAppPurchase fullVersion = purchaseHelper != null ? purchaseHelper.getFullVersion() : null;
		if (fullVersion != null && fullVersion.getSku().equals(sku)) {
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

	public void fireInAppPurchaseDismissProgressOnFragments(
			@NonNull FragmentManager fragmentManager,
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

	@Override
	public boolean isActivityHiddenForTalkback() {
		return activityHiddenForTalkback;
	}

	@Override
	public void setActivityHiddenForTalkback(boolean activityHiddenForTalkback) {
		this.activityHiddenForTalkback = activityHiddenForTalkback;
	}
}
