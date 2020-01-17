package net.osmand.plus.dialogs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.view.ContextThemeWrapper;
import android.view.View;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;

import org.apache.commons.logging.Log;

import java.util.Calendar;

public class RateUsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {
	public static final String TAG = "RateUsBottomSheetDialogFragment";
	private static final Log LOG = PlatformUtil.getLog(SendAnalyticsBottomSheetDialogFragment.class);
	private static final long SIXTY_DAYS = 60 * 24 * 60 * 60 * 1000L;

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
			app.getSettings().RATE_US_STATE.set(RateUsState.LIKED);
			Uri uri = Uri.parse(Version.getUrlWithUtmRef(app, app.getPackageName()));
			Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(goToMarket);
			dismiss();
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

	public static boolean shouldShow(OsmandApplication application) {
		long firstInstalledDays = application.getAppInitializer().getFirstInstalledDays();
		if (!Version.isGooglePlayEnabled(application) || firstInstalledDays > 350) {
			return false;
		}
		OsmandSettings settings = application.getSettings();
		if (!settings.LAST_DISPLAY_TIME.isSet()) {
			settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
		}
		int numberOfStarts = application.getAppInitializer().getNumberOfStarts();

		RateUsState state = settings.RATE_US_STATE.get();
		switch (state) {
			case LIKED:
				return false;
			case INITIAL_STATE:
				return firstInstalledDays > 15 && numberOfStarts > 100;
			case IGNORED:
			case DISLIKED_WITH_MESSAGE:
			case DISLIKED_WITHOUT_MESSAGE:
				int startsOnDislikeMoment = settings.NUMBER_OF_APP_STARTS_ON_DISLIKE_MOMENT.get();
				long lastDisplayTimeInMillis = settings.LAST_DISPLAY_TIME.get();
				long currentTime = System.currentTimeMillis();
				return currentTime - lastDisplayTimeInMillis > SIXTY_DAYS
						&& numberOfStarts - startsOnDislikeMoment > 50;
		}
		return false;
	}

	public enum RateUsState {
		INITIAL_STATE,
		IGNORED,
		LIKED,
		DISLIKED_WITH_MESSAGE,
		DISLIKED_WITHOUT_MESSAGE
	}
}
