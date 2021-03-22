package net.osmand.plus.chooseplan;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment.ChoosePlanDialogType;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment.OsmAndFeature;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public abstract class OsmLiveGoneDialog extends BaseOsmAndDialogFragment {
	public static final String TAG = OsmLiveGoneDialog.class.getName();
	private static final Log LOG = PlatformUtil.getLog(OsmLiveGoneDialog.class);

	private static final long TIME_BETWEEN_DIALOGS_MSEC = 1000 * 60 * 60 * 24 * 3; // 3 days

	private OsmandApplication app;
	private boolean nightMode;
	private View osmLiveButton;

	private final OsmAndFeature[] osmLiveFeatures = {
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.UNLOCK_ALL_FEATURES,
	};

	public static class OsmLiveOnHoldDialog extends OsmLiveGoneDialog {
		public static final String TAG = OsmLiveOnHoldDialog.class.getSimpleName();

		@Override
		protected OsmLiveButtonType getOsmLiveButtonType() {
			return OsmLiveButtonType.MANAGE_SUBSCRIPTION;
		}

		@Override
		protected String getTitle() {
			return getString(R.string.subscription_on_hold_title);
		}

		@Override
		protected String getSubscriptionDescr() {
			return getString(R.string.subscription_payment_issue_title);
		}
	}

	public static class OsmLivePausedDialog extends OsmLiveGoneDialog {
		public static final String TAG = OsmLivePausedDialog.class.getSimpleName();

		@Override
		protected OsmLiveButtonType getOsmLiveButtonType() {
			return OsmLiveButtonType.MANAGE_SUBSCRIPTION;
		}

		@Override
		protected String getTitle() {
			return getString(R.string.subscription_paused_title);
		}
	}

	public static class OsmLiveExpiredDialog extends OsmLiveGoneDialog {
		public static final String TAG = OsmLiveExpiredDialog.class.getSimpleName();

		@Override
		protected String getTitle() {
			return getString(R.string.subscription_expired_title);
		}
	}

	protected enum OsmLiveButtonType {
		PURCHASE_SUBSCRIPTION,
		MANAGE_SUBSCRIPTION
	}

	protected OsmLiveButtonType getOsmLiveButtonType() {
		return OsmLiveButtonType.PURCHASE_SUBSCRIPTION;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		nightMode = isNightMode(getMapActivity() != null);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity ctx = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(ctx, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			if (Build.VERSION.SDK_INT >= 21) {
				window.setStatusBarColor(ContextCompat.getColor(ctx, getStatusBarColor()));
			}
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return null;
		}
		int themeRes = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		View view = LayoutInflater.from(new ContextThemeWrapper(getContext(), themeRes))
				.inflate(R.layout.osmlive_gone_dialog_fragment, container, false);

		view.findViewById(R.id.button_close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		TextViewEx title = (TextViewEx) view.findViewById(R.id.title);
		title.setText(getTitle());
		TextViewEx infoDescr = (TextViewEx) view.findViewById(R.id.info_description);
		StringBuilder descr = new StringBuilder();
		String subscriptionDescr = getSubscriptionDescr();
		if (!Algorithms.isEmpty(subscriptionDescr)) {
			descr.append(subscriptionDescr).append("\n\n");
		}
		descr.append(getString(R.string.purchase_cancelled_dialog_descr));
		for (OsmAndFeature feature : osmLiveFeatures) {
			descr.append("\n").append("— ").append(feature.toHumanString(ctx));
		}
		infoDescr.setText(descr);

		osmLiveButton = view.findViewById(R.id.card_button);

		return view;
	}

	protected abstract String getTitle();

	protected String getSubscriptionDescr() {
		return null;
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}

		setupOsmLiveButton();

		OsmandPreference<Long> firstTimeShownTime = app.getSettings().LIVE_UPDATES_EXPIRED_FIRST_DLG_SHOWN_TIME;
		OsmandPreference<Long> secondTimeShownTime = app.getSettings().LIVE_UPDATES_EXPIRED_SECOND_DLG_SHOWN_TIME;
		if (firstTimeShownTime.get() == 0) {
			firstTimeShownTime.set(System.currentTimeMillis());
		} else if (secondTimeShownTime.get() == 0) {
			secondTimeShownTime.set(System.currentTimeMillis());
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@ColorRes
	protected int getStatusBarColor() {
		return nightMode ? R.color.status_bar_wikivoyage_dark : R.color.status_bar_wikivoyage_light;
	}

	private void setupOsmLiveButton() {
		if (osmLiveButton != null) {
			TextViewEx buttonTitle = (TextViewEx) osmLiveButton.findViewById(R.id.card_button_title);
			TextViewEx buttonSubtitle = (TextViewEx) osmLiveButton.findViewById(R.id.card_button_subtitle);
			switch (getOsmLiveButtonType()) {
				case PURCHASE_SUBSCRIPTION:
					buttonTitle.setText(getString(R.string.osm_live_plan_pricing));
					osmLiveButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dismiss();
							FragmentActivity activity = getActivity();
							if (activity != null) {
								ChoosePlanDialogFragment.showDialogInstance(app, activity.getSupportFragmentManager(), ChoosePlanDialogType.OSM_LIVE);
							}
						}
					});
					break;
				case MANAGE_SUBSCRIPTION:
					buttonTitle.setText(getString(R.string.manage_subscription));
					osmLiveButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dismiss();
							FragmentActivity activity = getActivity();
							if (activity != null) {
								InAppSubscription expiredSubscription = getExpiredSubscription((OsmandApplication) activity.getApplication());
								if (expiredSubscription != null) {
									manageSubscription(expiredSubscription.getSku());
								}
							}
						}
					});
					break;
			}
			buttonSubtitle.setVisibility(View.GONE);
			buttonTitle.setVisibility(View.VISIBLE);
			osmLiveButton.findViewById(R.id.card_button_progress).setVisibility(View.GONE);
		}
	}

	private void manageSubscription(@Nullable String sku) {
		Context ctx = getContext();
		if (ctx != null) {
			String url = "https://play.google.com/store/account/subscriptions?package=" + ctx.getPackageName();
			if (!Algorithms.isEmpty(sku)) {
				url += "&sku=" + sku;
			}
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(intent);
		}
	}

	@Nullable
	private static InAppSubscription getExpiredSubscription(@NonNull OsmandApplication app) {
		if (!app.getSettings().LIVE_UPDATES_PURCHASED.get()) {
			InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
			return purchaseHelper.getLiveUpdates().getTopExpiredSubscription();
		}
		return null;
	}

	public static boolean shouldShowDialog(@NonNull OsmandApplication app) {
		InAppSubscription expiredSubscription = getExpiredSubscription(app);
		if (expiredSubscription == null) {
			return false;
		}
		OsmandSettings settings = app.getSettings();
		long firstTimeShownTime = settings.LIVE_UPDATES_EXPIRED_FIRST_DLG_SHOWN_TIME.get();
		long secondTimeShownTime = settings.LIVE_UPDATES_EXPIRED_SECOND_DLG_SHOWN_TIME.get();
		return firstTimeShownTime == 0
				|| (System.currentTimeMillis() - firstTimeShownTime > TIME_BETWEEN_DIALOGS_MSEC && secondTimeShownTime == 0);
	}

	public static void showInstance(@NonNull OsmandApplication app, @NonNull FragmentManager fm) {
		try {
			InAppSubscription expiredSubscription = getExpiredSubscription(app);
			if (expiredSubscription == null) {
				return;
			}
			String tag = null;
			DialogFragment fragment = null;
			switch (expiredSubscription.getState()) {
				case ON_HOLD:
					tag = OsmLiveOnHoldDialog.TAG;
					if (fm.findFragmentByTag(tag) == null) {
						fragment = new OsmLiveOnHoldDialog();
					}
					break;
				case PAUSED:
					tag = OsmLivePausedDialog.TAG;
					if (fm.findFragmentByTag(tag) == null) {
						fragment = new OsmLivePausedDialog();
					}
					break;
				case EXPIRED:
					tag = OsmLiveExpiredDialog.TAG;
					if (fm.findFragmentByTag(tag) == null) {
						fragment = new OsmLiveExpiredDialog();
					}
					break;
			}
			if (fragment != null) {
				fragment.show(fm, tag);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}