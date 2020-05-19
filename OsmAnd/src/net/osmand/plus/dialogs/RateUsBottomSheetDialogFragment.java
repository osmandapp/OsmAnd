package net.osmand.plus.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;

import org.apache.commons.logging.Log;

public class RateUsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {
	public static final String TAG = "RateUsBottomSheetDialogFragment";
	private static final Log LOG = PlatformUtil.getLog(SendAnalyticsBottomSheetDialogFragment.class);
	private static final long SIXTY_DAYS = 60 * 24 * 60 * 60 * 1000L;

	private RateUsState newRateUsState = RateUsState.IGNORED;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) {
			return;
		}

		final View titleView = View.inflate(new ContextThemeWrapper(context, themeRes), R.layout.rate_us_title, null);
		final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_no;
	}

	@Override
	protected void onDismissButtonClickAction() {
		FragmentManager fm = getFragmentManager();
		if (fm != null) {
			newRateUsState = null;
			DislikeOsmAndBottomSheetDialogFragment.showInstance(fm);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.button_rate;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			newRateUsState = RateUsState.LIKED;
			Uri uri = Uri.parse(Version.getUrlWithUtmRef(app, app.getPackageName()));
			try {
				Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(goToMarket);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			dismiss();
		}
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		FragmentActivity activity = getActivity();
		if (newRateUsState != null && activity != null && !activity.isChangingConfigurations()) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			OsmandSettings settings = app.getSettings();
			RateUsState newState = RateUsState.getNewState(app, newRateUsState);
			settings.RATE_US_STATE.set(newState);
			if (newState != RateUsState.LIKED) {
				settings.NUMBER_OF_APP_STARTS_ON_DISLIKE_MOMENT.set(app.getAppInitializer().getNumberOfStarts());
			}
			settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
		}
	}

	public static void showInstance(@NonNull FragmentManager fm) {
		try {
			if (fm.findFragmentByTag(RateUsBottomSheetDialogFragment.TAG) == null) {
				RateUsBottomSheetDialogFragment fragment = new RateUsBottomSheetDialogFragment();
				fragment.show(fm, RateUsBottomSheetDialogFragment.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public static boolean shouldShow(OsmandApplication app) {
		long firstInstalledDays = app.getAppInitializer().getFirstInstalledDays();
		//Do not show dialog if not google play version or more than 350 days left from the first start
		if (!Version.isGooglePlayEnabled(app) || firstInstalledDays > 350) {
			return false;
		}
		OsmandSettings settings = app.getSettings();
		int numberOfStarts = app.getAppInitializer().getNumberOfStarts();
		RateUsState state = settings.RATE_US_STATE.get();
		switch (state) {
			//Do not show anymore if liked
			case LIKED:
			case DISLIKED_OR_IGNORED_AGAIN:
				return false;
			//First dialog after 15 days from the first start or 100 starts
			case INITIAL_STATE:
				return firstInstalledDays > 15 || numberOfStarts > 100;
			//Second dialog after 60 days or 50 starts from the first appearance (if ignored or disliked)
			case IGNORED:
			case DISLIKED_WITH_MESSAGE:
			case DISLIKED_WITHOUT_MESSAGE:
				int startsOnDislikeMoment = settings.NUMBER_OF_APP_STARTS_ON_DISLIKE_MOMENT.get();
				long lastDisplayTimeInMillis = settings.LAST_DISPLAY_TIME.get();
				long currentTime = System.currentTimeMillis();
				return currentTime - lastDisplayTimeInMillis > SIXTY_DAYS || numberOfStarts - startsOnDislikeMoment > 50;
		}
		return false;
	}

	public enum RateUsState {
		INITIAL_STATE,
		IGNORED,
		LIKED,
		DISLIKED_WITH_MESSAGE,
		DISLIKED_WITHOUT_MESSAGE,
		DISLIKED_OR_IGNORED_AGAIN;

		public static RateUsState getNewState(OsmandApplication app, RateUsState requiredState) {
			RateUsState currentState = app.getSettings().RATE_US_STATE.get();
			switch (requiredState) {
				case INITIAL_STATE:
				case LIKED:
				case DISLIKED_OR_IGNORED_AGAIN:
					return requiredState;
				case IGNORED:
				case DISLIKED_WITH_MESSAGE:
				case DISLIKED_WITHOUT_MESSAGE:
					return currentState == INITIAL_STATE ? requiredState : RateUsState.DISLIKED_OR_IGNORED_AGAIN;
			}
			return requiredState;
		}
	}
}
