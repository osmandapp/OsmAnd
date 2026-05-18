package net.osmand.plus.plugins.osmedit.fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.BasePurchaseDialogFragment.ButtonBackground;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MappersFragment extends BaseOsmAndFragment {

	public static final String TAG = MappersFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(MappersFragment.class);

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-LL");
	private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("LLLL");
	private static final SimpleDateFormat CONTRIBUTION_FORMAT = new SimpleDateFormat("LLLL yyyy");

	private static final String CONTRIBUTIONS_URL = "https://www.openstreetmap.org/user/";
	private static final String USER_CHANGES_URL = AndroidNetworkUtils.getHttpProtocol() + "osmand.net/changesets/user-changes";

	private static final int VISIBLE_MONTHS_COUNT = 6;
	private static final int CHANGES_FOR_MAPPER_PROMO = 30;
	private static final int DAYS_FOR_MAPPER_PROMO_CHECK = 60;

	private OsmEditingPlugin plugin;
	private Map<String, Contribution> changesInfo = new LinkedHashMap<>();

	private View mainView;

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (!manager.isStateSaved() && manager.findFragmentByTag(TAG) == null) {
			MappersFragment fragment = new MappersFragment();
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);

		requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				dismiss();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		mainView = themedInflater.inflate(R.layout.fragment_mappers_osm, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), mainView);

		setupToolbar();
		setupRefreshButton();
		setupContributionsBtn();
		fullUpdate();

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (Algorithms.isEmpty(changesInfo)) {
			refreshContributions();
		}
	}

	private void setupToolbar() {
		Toolbar toolbar = mainView.findViewById(R.id.toolbar);
		int iconId = AndroidUtils.getNavigationIconResId(app);
		int color = ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode);
		toolbar.setNavigationIcon(getPaintedContentIcon(iconId, color));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> dismiss());
	}

	private void setupRefreshButton() {
		View button = mainView.findViewById(R.id.button_refresh);
		int normal = ColorUtilities.getActiveColor(app, nightMode);
		int pressed = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_bg_pressed_dark : R.color.active_buttons_and_links_bg_pressed_light);
		setupButtonBackground(button, normal, pressed);
		button.setOnClickListener(v -> refreshContributions());
	}

	private void setupContributionsBtn() {
		View button = mainView.findViewById(R.id.contributions_button);
		button.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				String userName = plugin.OSM_USER_DISPLAY_NAME.get();
				String url = CONTRIBUTIONS_URL + Uri.encode(userName) + "/history";
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				AndroidUtils.startActivityIfSafe(activity, intent);
			}
		});
	}

	private void fullUpdate() {
		updateHeader();
		updateLastInterval();
		updateContributionsList();
	}

	private void updateHeader() {
		int titleColor;
		String title;
		String description;
		long expireTime = app.getSettings().MAPPER_LIVE_UPDATES_EXPIRE_TIME.get();
		boolean isAvailable = expireTime > System.currentTimeMillis();
		if (isAvailable) {
			titleColor = ColorUtilities.getActiveColor(app, nightMode);
			String date = OsmAndFormatter.getFormattedDate(app, expireTime);
			title = getString(R.string.available_until, date);
			description = getString(R.string.enough_contributions_descr);
		} else {
			titleColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
			title = getString(R.string.map_updates_are_unavailable_yet);
			description = getString(R.string.not_enough_contributions_descr,
					String.valueOf(CHANGES_FOR_MAPPER_PROMO), "(" + getMonthPeriod() + ")");
		}

		TextView tvTitle = mainView.findViewById(R.id.header_title);
		tvTitle.setText(title);
		tvTitle.setTextColor(titleColor);

		TextView tvDescr = mainView.findViewById(R.id.header_descr);
		tvDescr.setText(description);
	}

	private void updateLastInterval() {
		View container = mainView.findViewById(R.id.contributions_header);
		TextView tvInterval = container.findViewById(R.id.interval);
		TextView tvCount = container.findViewById(R.id.total_contributions);

		tvInterval.setText(getMonthPeriod());
		tvCount.setText(String.valueOf(getChangesSize(changesInfo)));
	}

	private String getMonthPeriod() {
		Calendar calendar = Calendar.getInstance();
		String currentMonth = MONTH_FORMAT.format(calendar.getTimeInMillis());
		calendar.add(Calendar.MONTH, -1);
		String prevMonth = MONTH_FORMAT.format(calendar.getTimeInMillis());

		return getString(R.string.ltr_or_rtl_combine_via_dash, prevMonth, currentMonth);
	}

	private void updateContributionsList() {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		LinearLayout list = mainView.findViewById(R.id.contributions_list);
		list.removeAllViews();

		Calendar calendar = Calendar.getInstance();
		for (int i = 0; i < VISIBLE_MONTHS_COUNT; i++) {
			long time = calendar.getTimeInMillis();
			calendar.add(Calendar.MONTH, -1);
			Contribution contribution = changesInfo.get(DATE_FORMAT.format(time));
			int changesSize = contribution != null ? contribution.count : 0;

			View view = inflater.inflate(R.layout.osm_contribution_item, list, false);
			TextView tvTitle = view.findViewById(R.id.title);
			TextView tvCount = view.findViewById(R.id.count);

			tvTitle.setText(CONTRIBUTION_FORMAT.format(time));
			tvCount.setText(String.valueOf(changesSize));
			list.addView(view);
		}
	}

	protected void dismiss() {
		FragmentManager manager = getFragmentManager();
		if (manager != null && !manager.isStateSaved()) {
			manager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	private void setupButtonBackground(@NonNull View button, @ColorInt int normalColor, @ColorInt int pressedColor) {
		Drawable normal = createRoundedDrawable(normalColor, ButtonBackground.ROUNDED_SMALL);

		Drawable background;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Drawable pressed = AppCompatResources.getDrawable(app, ButtonBackground.ROUNDED_SMALL.getRippleId(nightMode));
			background = UiUtilities.getLayeredIcon(normal, pressed);
		} else {
			Drawable pressed = createRoundedDrawable(pressedColor, ButtonBackground.ROUNDED_SMALL);
			background = AndroidUtils.createPressedStateListDrawable(normal, pressed);
		}
		AndroidUtils.setBackground(button, background);
	}

	protected Drawable createRoundedDrawable(@ColorInt int color, ButtonBackground background) {
		return UiUtilities.createTintedDrawable(app, background.drawableId, color);
	}

	public void refreshContributions() {
		downloadChangesInfo(result -> {
			changesInfo = result;
			checkLastChanges(result);
			if (isAdded()) {
				fullUpdate();
			}
			return true;
		});
	}

	private void checkLastChanges(@NonNull Map<String, Contribution> map) {
		int size = getChangesSize(map);
		if (size >= CHANGES_FOR_MAPPER_PROMO) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, 1);
			calendar.set(Calendar.DAY_OF_MONTH, 16);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			app.getSettings().MAPPER_LIVE_UPDATES_EXPIRE_TIME.set(calendar.getTimeInMillis());
		} else {
			app.getSettings().MAPPER_LIVE_UPDATES_EXPIRE_TIME.resetToDefault();
		}
	}

	private int getChangesSize(@NonNull Map<String, Contribution> map) {
		int changesSize = 0;
		Calendar calendar = Calendar.getInstance();
		String date = DATE_FORMAT.format(calendar.getTimeInMillis());

		Contribution contribution = map.get(date);
		changesSize += contribution != null ? contribution.count : 0;

		calendar.add(Calendar.MONTH, -1);
		date = DATE_FORMAT.format(calendar.getTimeInMillis());

		contribution = map.get(date);
		changesSize += contribution != null ? contribution.count : 0;

		return changesSize;
	}

	public void downloadChangesInfo(@NonNull CallbackWithObject<Map<String, Contribution>> callback) {
		String userName = plugin.OSM_USER_DISPLAY_NAME.get();
		Map<String, String> params = new HashMap<>();
		params.put("name", userName);
		AndroidNetworkUtils.sendRequestAsync(app, USER_CHANGES_URL, params, "Download object changes list", false, false,
				(resultJson, error, resultCode) -> {
					Map<String, Contribution> map = new LinkedHashMap<>();
					if (!Algorithms.isEmpty(error)) {
						log.error(error);
						app.showShortToastMessage(error);
					} else if (!Algorithms.isEmpty(resultJson)) {
						try {
							JSONObject res = new JSONObject(resultJson);
							JSONObject objectChanges = res.getJSONObject("objectChanges");
							for (Iterator<String> it = objectChanges.keys(); it.hasNext(); ) {
								String dateStr = it.next();
								Date date = DATE_FORMAT.parse(dateStr);
								int changesCount = objectChanges.optInt(dateStr);
								map.put(dateStr, new Contribution(date, changesCount));
							}
						} catch (JSONException | ParseException e) {
							log.error(e);
						}
					}
					callback.processResult(map);
				});
	}

	private static class Contribution {

		private final Date date;
		private final int count;

		public Contribution(Date date, int count) {
			this.date = date;
			this.count = count;
		}
	}
}
