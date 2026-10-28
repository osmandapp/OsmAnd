package net.osmand.plus.feedback;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_SEND_ANALYTICS_ID;

import android.content.Context;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitmeListDividerItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import org.apache.commons.logging.Log;

public class SendAnalyticsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SendAnalyticsBottomSheetDialogFragment";
	private static final Log LOG = PlatformUtil.getLog(SendAnalyticsBottomSheetDialogFragment.class);

	private boolean sendAnonymousMapDownloadsData;
	private boolean sendAnonymousAppUsageData;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		Context context = getContext();
		if (context == null || app == null) {
			return;
		}

		View titleView = View.inflate(new ContextThemeWrapper(context, themeRes), R.layout.make_better_title, null);
		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);

		items.add(new SubtitleDividerItem(context));

		if (app.getSettings().SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.isSet()) {
			sendAnonymousMapDownloadsData = app.getSettings().SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.get();
		} else {
			sendAnonymousMapDownloadsData = true;
		}
		if (app.getSettings().SEND_ANONYMOUS_APP_USAGE_DATA.isSet()) {
			sendAnonymousAppUsageData = app.getSettings().SEND_ANONYMOUS_APP_USAGE_DATA.get();
		} else {
			sendAnonymousAppUsageData = true;
		}
		BottomSheetItemWithCompoundButton[] downloadedMapsItem = new BottomSheetItemWithCompoundButton[1];
		downloadedMapsItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(sendAnonymousMapDownloadsData)
				.setTitle(getString(R.string.downloaded_maps))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
				.setOnClickListener(v -> {
					boolean checked = !downloadedMapsItem[0].isChecked();
					downloadedMapsItem[0].setChecked(checked);
					sendAnonymousMapDownloadsData = checked;
					updateBottomButtons();
				})
				.setTag("downloaded_maps")
				.create();
		items.add(downloadedMapsItem[0]);

		items.add(new LongDescriptionItem(getString(R.string.downloaded_maps_collect_descr)));

		items.add(new SubtitmeListDividerItem(context));

		BottomSheetItemWithCompoundButton[] visitedScreensItem = new BottomSheetItemWithCompoundButton[1];
		visitedScreensItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(sendAnonymousAppUsageData)
				.setTitle(getString(R.string.visited_screens))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
				.setOnClickListener(v -> {
					boolean checked = !visitedScreensItem[0].isChecked();
					visitedScreensItem[0].setChecked(checked);
					sendAnonymousAppUsageData = checked;
					updateBottomButtons();
				})
				.setTag("visited_screens")
				.create();
		items.add(visitedScreensItem[0]);

		items.add(new LongDescriptionItem(getString(R.string.visited_screens_collect_descr)));

		items.add(new DividerItem(context));

		String privacyPolicyText = getString(R.string.shared_string_privacy_policy);
		String text = getString(R.string.privacy_and_security_change_descr, privacyPolicyText);

		SpannableString spannable = new SpannableString(text);
		int start = text.indexOf(privacyPolicyText);
		int end = start + privacyPolicyText.length();
		String url = getString(R.string.osmand_privacy_policy);
		spannable.setSpan(new URLSpan(url) {
			@Override
			public void updateDrawState(@NonNull TextPaint ds) {
				super.updateDrawState(ds);
				ds.setUnderlineText(false);
			}
		}, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		int linkTextColor = ContextCompat.getColor(context, !nightMode ? R.color.active_color_primary_light : R.color.active_color_primary_dark);
		spannable.setSpan(new ForegroundColorSpan(linkTextColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		LongDescriptionItem descriptionItem = new LongDescriptionItem(spannable);
		descriptionItem.setDescriptionLinksClickable(true);
		items.add(descriptionItem);
	}

	@Override
	protected boolean isRightBottomButtonEnabled() {
		return sendAnonymousMapDownloadsData || sendAnonymousAppUsageData;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_no_thank_you;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_allow;
	}

	@Override
	protected void onDismissButtonClickAction() {
		OsmandApplication app = requiredMyApplication();
		OsmandSettings settings = app.getSettings();
		settings.SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.set(false);
		settings.SEND_ANONYMOUS_APP_USAGE_DATA.set(false);
		settings.SEND_ANONYMOUS_DATA_REQUEST_PROCESSED.set(true);
		informAnalyticsPrefsUpdate();
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = requiredMyApplication();
		OsmandSettings settings = app.getSettings();
		settings.SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.set(sendAnonymousMapDownloadsData);
		settings.SEND_ANONYMOUS_APP_USAGE_DATA.set(sendAnonymousAppUsageData);
		settings.SEND_ANONYMOUS_DATA_REQUEST_PROCESSED.set(true);
		informAnalyticsPrefsUpdate();
		dismiss();
	}

	private void informAnalyticsPrefsUpdate() {
		Fragment target = getTargetFragment();
		if (target instanceof OnSendAnalyticsPrefsUpdate) {
			((OnSendAnalyticsPrefsUpdate) target).onAnalyticsPrefsUpdate();
		}
	}

	public static boolean shouldShowDialog(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (app.getAppCustomization().isFeatureEnabled(FRAGMENT_SEND_ANALYTICS_ID)) {
			int requestsCount = settings.SEND_ANONYMOUS_DATA_REQUESTS_COUNT.get();
			long firstInstalledDays = app.getAppInitializer().getFirstInstalledDays();
			boolean requestProcessed = settings.SEND_ANONYMOUS_DATA_REQUEST_PROCESSED.get();
			if (!requestProcessed && firstInstalledDays >= 5 && firstInstalledDays <= 30 && requestsCount < 3) {
				if (requestsCount == 0) {
					return true;
				} else {
					int numberOfStarts = app.getAppInitializer().getNumberOfStarts();
					int lastRequestNS = settings.SEND_ANONYMOUS_DATA_LAST_REQUEST_NS.get();
					return numberOfStarts - lastRequestNS > 2;
				}
			}
		}
		return false;
	}

	public static void showInstance(@NonNull OsmandApplication app, @NonNull FragmentManager fm, @Nullable Fragment target) {
		try {
			if (fm.findFragmentByTag(TAG) == null) {
				SendAnalyticsBottomSheetDialogFragment fragment = new SendAnalyticsBottomSheetDialogFragment();
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, TAG);

				OsmandSettings settings = app.getSettings();
				int numberOfStarts = app.getAppInitializer().getNumberOfStarts();
				OsmandPreference<Integer> lastRequestNS = settings.SEND_ANONYMOUS_DATA_LAST_REQUEST_NS;
				if (numberOfStarts != lastRequestNS.get()) {
					OsmandPreference<Integer> counter = settings.SEND_ANONYMOUS_DATA_REQUESTS_COUNT;
					counter.set(counter.get() + 1);
					lastRequestNS.set(numberOfStarts);
				}
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public interface OnSendAnalyticsPrefsUpdate {

		void onAnalyticsPrefsUpdate();

	}
}
