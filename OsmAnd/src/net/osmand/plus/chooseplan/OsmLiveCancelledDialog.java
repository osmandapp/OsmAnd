package net.osmand.plus.chooseplan;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment.OsmAndFeature;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import static net.osmand.plus.inapp.InAppPurchaseHelper.SUBSCRIPTION_HOLDING_TIME_MSEC;

public class OsmLiveCancelledDialog extends BaseOsmAndDialogFragment implements InAppPurchaseListener {
	public static final String TAG = OsmLiveCancelledDialog.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(OsmLiveCancelledDialog.class);

	private OsmandApplication app;
	private InAppPurchaseHelper purchaseHelper;

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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		purchaseHelper = app.getInAppPurchaseHelper();
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
				.inflate(R.layout.osmlive_cancelled_dialog_fragment, container, false);

		view.findViewById(R.id.button_close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		TextViewEx infoDescr = (TextViewEx) view.findViewById(R.id.info_description);
		StringBuilder descr = new StringBuilder();
		descr.append(getString(R.string.purchase_cancelled_dialog_descr));
		for (OsmAndFeature feature : osmLiveFeatures) {
			descr.append("\n").append("— ").append(feature.toHumanString(ctx));
		}
		infoDescr.setText(descr);

		osmLiveButton = view.findViewById(R.id.card_button);

		return view;
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

		boolean requestingInventory = purchaseHelper != null && purchaseHelper.getActiveTask() == InAppPurchaseTaskType.REQUEST_INVENTORY;
		setupOsmLiveButton(requestingInventory);

		OsmandPreference<Boolean> firstTimeShown = app.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_FIRST_DLG_SHOWN;
		OsmandPreference<Boolean> secondTimeShown = app.getSettings().LIVE_UPDATES_PURCHASE_CANCELLED_SECOND_DLG_SHOWN;
		if (!firstTimeShown.get()) {
			firstTimeShown.set(true);
		} else if (!secondTimeShown.get()) {
			secondTimeShown.set(true);
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

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			setupOsmLiveButton(false);
		}
	}

	@Override
	public void onGetItems() {
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			setupOsmLiveButton(true);
		}
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			setupOsmLiveButton(false);
		}
	}

	private void setupOsmLiveButton(boolean progress) {
		if (osmLiveButton != null) {
			ProgressBar progressBar = (ProgressBar) osmLiveButton.findViewById(R.id.card_button_progress);
			TextViewEx buttonTitle = (TextViewEx) osmLiveButton.findViewById(R.id.card_button_title);
			TextViewEx buttonSubtitle = (TextViewEx) osmLiveButton.findViewById(R.id.card_button_subtitle);
			buttonTitle.setText(getString(R.string.osm_live_plan_pricing));
			buttonSubtitle.setVisibility(View.GONE);
			if (progress) {
				buttonTitle.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
				osmLiveButton.setOnClickListener(null);
			} else {
				buttonTitle.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				osmLiveButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
						FragmentActivity activity = getActivity();
						if (activity != null) {
							ChoosePlanDialogFragment.showOsmLiveInstance(activity.getSupportFragmentManager());
						}
					}
				});
			}
		}
	}

	public static boolean shouldShowDialog(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		long cancelledTime = settings.LIVE_UPDATES_PURCHASE_CANCELLED_TIME.get();
		boolean firstTimeShown = settings.LIVE_UPDATES_PURCHASE_CANCELLED_FIRST_DLG_SHOWN.get();
		boolean secondTimeShown = settings.LIVE_UPDATES_PURCHASE_CANCELLED_SECOND_DLG_SHOWN.get();
		return cancelledTime > 0
				&& (!firstTimeShown
					|| (System.currentTimeMillis() - cancelledTime > SUBSCRIPTION_HOLDING_TIME_MSEC
						&& !secondTimeShown));
	}

	public static void showInstance(@NonNull FragmentManager fm) {
		try {
			if (fm.findFragmentByTag(OsmLiveCancelledDialog.TAG) == null) {
				OsmLiveCancelledDialog fragment = new OsmLiveCancelledDialog();
				fragment.show(fm, OsmLiveCancelledDialog.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
