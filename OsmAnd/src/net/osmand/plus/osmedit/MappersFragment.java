package net.osmand.plus.osmedit;

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

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.BasePurchaseDialogFragment.ButtonBackground;
import net.osmand.plus.settings.backend.OsmandSettings;

import static net.osmand.plus.settings.fragments.BaseSettingsListFragment.SETTINGS_LIST_TAG;

public class MappersFragment extends BaseOsmAndFragment {

	public static final String TAG = MappersFragment.class.getSimpleName();

	protected OsmandApplication app;
	protected OsmandSettings settings;

	private View mainView;
	protected boolean nightMode;

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		if (!fm.isStateSaved() && fm.findFragmentByTag(TAG) == null) {
			MappersFragment fragment = new MappersFragment();
			fragment.setRetainInstance(true);
			fm.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(SETTINGS_LIST_TAG)
					.commitAllowingStateLoss();
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
		button.setOnClickListener(v -> app.showShortToastMessage("refresh"));
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

}
