package net.osmand.plus.osmedit;

import static net.osmand.plus.settings.fragments.BaseSettingsListFragment.SETTINGS_LIST_TAG;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MappersFragment extends BaseOsmAndFragment {

	public static final String TAG = MappersFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(MappersFragment.class);

	private static final String USER_CHANGES_URL = "https://osmand.net/changesets/user-changes";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM", Locale.US);
	private static final int CHANGES_FOR_MAPPER_PROMO = 15;

	private OsmandApplication app;
	private OsmandSettings settings;
	private Map<String, Integer> changesInfo = new TreeMap<>();

	private View mainView;
	private boolean nightMode;

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		if (!fm.isStateSaved() && fm.findFragmentByTag(TAG) == null) {
			MappersFragment fragment = new MappersFragment();
			fragment.setRetainInstance(true);
			fm.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(SETTINGS_LIST_TAG)
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
			downloadChangesInfo(new CallbackWithObject<Map<String, Integer>>() {
				@Override
				public boolean processResult(Map<String, Integer> result) {
					changesInfo = result;
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
		button.setOnClickListener(v -> app.showShortToastMessage("go to contributions"));
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
			titleColor = ContextCompat.getColor(app, getPrimaryTextColorId());
			title = getString(R.string.map_updates_are_unavailable_yet);
			description = getString(R.string.not_enough_contributions_descr);
		}

		TextView tvTitle = mainView.findViewById(R.id.header_title);
		tvTitle.setText(title);
		tvTitle.setTextColor(titleColor);

		TextView tvDescr = mainView.findViewById(R.id.header_descr);
		tvDescr.setText(description);
	}

	private void updateLastInterval() {

	}

	private void updateContributionsList() {
		LinearLayout list = mainView.findViewById(R.id.contributions_list);
		list.removeAllViews();
	}

	protected void dismiss() {
		FragmentManager fm = getFragmentManager();
		if (fm != null && !fm.isStateSaved()) {
			getFragmentManager().popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
			return true;
		});
	}

	private void checkLastChanges(@NonNull Map<String, Integer> map) {
		int size = getChangesSize(map);
		if (size >= CHANGES_FOR_MAPPER_PROMO) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, 1);
			calendar.set(Calendar.DAY_OF_MONTH, 16);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			settings.MAPPER_LIVE_UPDATES_EXPIRE_TIME.set(calendar.getTimeInMillis());
		}
	}

	private int getChangesSize(@NonNull Map<String, Integer> map) {
		int changesSize = 0;
		Calendar calendar = Calendar.getInstance();
		String date = DATE_FORMAT.format(calendar.getTimeInMillis());

		Integer changesForMonth = map.get(date);
		changesSize += changesForMonth != null ? changesForMonth : 0;

		calendar.add(Calendar.MONTH, -1);
		date = DATE_FORMAT.format(calendar.getTimeInMillis());

		changesForMonth = map.get(date);
		changesSize += changesForMonth != null ? changesForMonth : 0;

		return changesSize;
	}

	public void downloadChangesInfo(@NonNull CallbackWithObject<Map<String, Integer>> callback) {
		boolean validToken = app.getOsmOAuthHelper().isValidToken();
		String userName = validToken ? settings.OSM_USER_DISPLAY_NAME.get() : settings.OSM_USER_NAME.get();
		Map<String, String> params = new HashMap<>();
		params.put("name", userName);
		AndroidNetworkUtils.sendRequestAsync(app, USER_CHANGES_URL, params, "Download object changes list", false, false,
				(resultJson, error) -> {
					Map<String, Integer> map = new TreeMap<>();
					if (!Algorithms.isEmpty(error)) {
						log.error(error);
						app.showShortToastMessage(error);
					} else if (!Algorithms.isEmpty(resultJson)) {
						try {
							JSONObject res = new JSONObject(resultJson);
							JSONObject objectChanges = res.getJSONObject("objectChanges");
							for (Iterator<String> it = objectChanges.keys(); it.hasNext(); ) {
								String date = it.next();
								int changesCount = objectChanges.optInt(date);
								map.put(date, changesCount);
							}
						} catch (JSONException e) {
							log.error(e);
						}
					}
					callback.processResult(map);
				});
	}
}
