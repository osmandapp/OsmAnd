package net.osmand.plus.osmedit;

import android.annotation.TargetApi;
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
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.BasePurchaseDialogFragment.ButtonBackground;
import net.osmand.plus.settings.backend.OsmandSettings;
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
import java.util.Locale;
import java.util.Map;

public class MappersFragment extends BaseOsmAndFragment {

	public static final String TAG = MappersFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(MappersFragment.class);

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM", Locale.US);
	private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMMM", Locale.US);
	private static final SimpleDateFormat CONTRIBUTION_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.US);

	private static final String CONTRIBUTIONS_URL = "https://www.openstreetmap.org/user/";
	private static final String USER_CHANGES_URL = "https://osmand.net/changesets/user-changes";

	private static final int VISIBLE_MONTHS_COUNT = 6;
	private static final int CHANGES_FOR_MAPPER_PROMO = 15;
	private static final int DAYS_FOR_MAPPER_PROMO_CHECK = 60;

	private OsmandApplication app;
	private OsmandSettings settings;
	private Map<String, Contribution> changesInfo = new LinkedHashMap<>();

	private View mainView;
	private boolean nightMode;

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		if (!fm.isStateSaved() && fm.findFragmentByTag(TAG) == null) {
			MappersFragment fragment = new MappersFragment();
			fragment.setRetainInstance(true);
			fm.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commit();
		}
	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		nightMode = !app.getSettings().isLightContent();

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
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		mainView = themedInflater.inflate(R.layout.fragment_mappers_osm, container, false);
		AndroidUtils.addStatusBarPadding21v(app, mainView);

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
			downloadChangesInfo(new CallbackWithObject<Map<String, Contribution>>() {
				@Override
				public boolean processResult(Map<String, Contribution> result) {
					changesInfo = result;
					fullUpdate();
					return true;
				}
			});
		}
	}

	private void setupToolbar() {
		Toolbar toolbar = mainView.findViewById(R.id.toolbar);
		int iconId = AndroidUtils.getNavigationIconResId(app);
		toolbar.setNavigationIcon(getPaintedContentIcon(iconId, nightMode
				? getResources().getColor(R.color.active_buttons_and_links_text_dark)
				: getResources().getColor(R.color.active_buttons_and_links_text_light)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> dismiss());
	}

	private void setupRefreshButton() {
		View button = mainView.findViewById(R.id.button_refresh);
		int normal = ContextCompat.getColor(app, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
		int pressed = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_bg_pressed_dark : R.color.active_buttons_and_links_bg_pressed_light);
		setupButtonBackground(button, normal, pressed);
		button.setOnClickListener(v -> refreshContributions());
	}

	private void setupContributionsBtn() {
		View button = mainView.findViewById(R.id.contributions_button);
		button.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				String userName = getUserName();
				String url = CONTRIBUTIONS_URL + userName + "/history";
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				if (AndroidUtils.isIntentSafe(app, intent)) {
					startActivity(intent);
				}
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
		long expireTime = settings.MAPPER_LIVE_UPDATES_EXPIRE_TIME.get();
		boolean isAvailable = expireTime > System.currentTimeMillis();
		if (isAvailable) {
			titleColor = ContextCompat.getColor(app, getActiveColorId());
			String date = OsmAndFormatter.getFormattedDate(app, expireTime);
			title = getString(R.string.available_until, date);
			description = getString(R.string.enough_contributions_descr);
		} else {
			int size = getChangesSize(changesInfo);
			titleColor = ContextCompat.getColor(app, getPrimaryTextColorId());
			title = getString(R.string.map_updates_are_unavailable_yet);
			description = getString(R.string.not_enough_contributions_descr,
					String.valueOf(CHANGES_FOR_MAPPER_PROMO - size),
					String.valueOf(DAYS_FOR_MAPPER_PROMO_CHECK));
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

		Calendar calendar = Calendar.getInstance();
		String currentMonth = MONTH_FORMAT.format(calendar.getTimeInMillis());
		calendar.add(Calendar.MONTH, -1);
		String prevMonth = MONTH_FORMAT.format(calendar.getTimeInMillis());

		tvInterval.setText(getString(R.string.ltr_or_rtl_combine_via_dash, prevMonth, currentMonth));
		tvCount.setText(String.valueOf(getChangesSize(changesInfo)));
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
		Drawable normal = createRoundedDrawable(normalColor, ButtonBackground.ROUNDED);
		Drawable pressed = createRoundedDrawable(pressedColor, ButtonBackground.ROUNDED);
		setupRoundedBackground(button, normal, pressed);
	}

	protected Drawable createRoundedDrawable(@ColorInt int color, ButtonBackground background) {
		return UiUtilities.createTintedDrawable(app, background.drawableId, color);
	}

	protected void setupRoundedBackground(@NonNull View view, @NonNull Drawable normal, @NonNull Drawable selected) {
		Drawable background;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			background = UiUtilities.getLayeredIcon(normal, getRippleDrawable());
		} else {
			background = AndroidUtils.createPressedStateListDrawable(normal, selected);
		}
		AndroidUtils.setBackground(view, background);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	protected Drawable getRippleDrawable() {
		return AppCompatResources.getDrawable(app, nightMode ? R.drawable.purchase_button_ripple_dark : R.drawable.purchase_button_ripple_light);
	}

	@ColorRes
	private int getActiveColorId() {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@ColorRes
	private int getPrimaryTextColorId() {
		return nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
	}

	public void refreshContributions() {
		downloadChangesInfo(result -> {
			changesInfo = result;
			checkLastChanges(result);
			fullUpdate();
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
			settings.MAPPER_LIVE_UPDATES_EXPIRE_TIME.set(calendar.getTimeInMillis());
		} else {
			settings.MAPPER_LIVE_UPDATES_EXPIRE_TIME.resetToDefault();
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

	private String getUserName() {
		boolean validToken = app.getOsmOAuthHelper().isValidToken();
		return validToken ? settings.OSM_USER_DISPLAY_NAME.get() : settings.OSM_USER_NAME.get();
	}

	public void downloadChangesInfo(@NonNull CallbackWithObject<Map<String, Contribution>> callback) {
		String userName = getUserName();
		Map<String, String> params = new HashMap<>();
		params.put("name", userName);
		AndroidNetworkUtils.sendRequestAsync(app, USER_CHANGES_URL, params, "Download object changes list", false, false,
				(resultJson, error) -> {
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
