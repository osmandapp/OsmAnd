package net.osmand.plus.plugins.osmedit.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.chooseplan.BasePurchaseDialogFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class MappersPromoFragment extends BasePurchaseDialogFragment {

	public static final String TAG = MappersPromoFragment.class.getSimpleName();

	private OsmOAuthHelper authHelper;
	private LinearLayout listContainer;
	private final List<OsmAndFeature> allFeatures = new ArrayList<>();

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull ApplicationMode appMode, @Nullable Fragment target) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
			MappersPromoFragment fragment = new MappersPromoFragment();
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(activity.getSupportFragmentManager(), TAG);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_mappers_promo;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		authHelper = app.getOsmOAuthHelper();
		allFeatures.add(OsmAndFeature.HOURLY_MAP_UPDATES);
		allFeatures.add(OsmAndFeature.MONTHLY_MAP_UPDATES);
		allFeatures.add(OsmAndFeature.UNLIMITED_MAP_DOWNLOADS);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		listContainer = mainView.findViewById(R.id.list_container);

		if(!InsetsUtils.isEdgeToEdgeSupported()){
			mainView.setFitsSystemWindows(true);
		}

		setupToolbar();
		createFeaturesList();
		setupSignInWithOsmButton();

		return mainView;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	private void setupToolbar() {
		ImageView backBtn = mainView.findViewById(R.id.button_back);
		backBtn.setImageResource(AndroidUtils.getNavigationIconResId(app));
		backBtn.setOnClickListener(v -> dismiss());

		FrameLayout iconBg = mainView.findViewById(R.id.header_icon_background);
		int color = AndroidUtils.getColorFromAttr(mainView.getContext(), R.attr.purchase_sc_header_icon_bg);
		AndroidUtils.setBackground(iconBg, createRoundedDrawable(color, ButtonBackground.ROUNDED_LARGE));
	}

	private void createFeaturesList() {
		listContainer.removeAllViews();
		for (OsmAndFeature feature : allFeatures) {
			View view = createFeatureItemView(feature);
			listContainer.addView(view);
		}
	}

	private View createFeatureItemView(@NonNull OsmAndFeature feature) {
		View view = inflate(R.layout.purchase_dialog_list_item, listContainer, false);
		view.setTag(feature);
		bindFeatureItem(view, feature, false);
		return view;
	}

	@Override
	protected void bindFeatureItem(@NonNull View view, @NonNull OsmAndFeature feature, boolean useHeaderTitle) {
		super.bindFeatureItem(view, feature, useHeaderTitle);

		AndroidUiHelper.setVisibility(View.GONE, view.findViewById(R.id.secondary_icon));
		ImageView ivOsm = view.findViewById(R.id.tertiary_icon);
		ivOsm.setImageResource(R.drawable.ic_action_openstreetmap_logo_colored);

		boolean isLastItem = feature == allFeatures.get(allFeatures.size() - 1);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !isLastItem);
	}

	private void setupSignInWithOsmButton() {
		View button = mainView.findViewById(R.id.sign_in_button);
		int color = ColorUtilities.getActiveColor(app, nightMode);
		setupButtonBackground(button, color);

		TextView tvTitle = button.findViewById(R.id.sign_in_button_title);
		int iconColorId = nightMode ? R.color.text_color_tab_active_dark : R.color.text_color_tab_active_light;
		Drawable icon = getIcon(R.drawable.ic_action_openstreetmap_logo, iconColorId);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(tvTitle, icon, null, null, null);
		tvTitle.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.content_padding_small));

		button.setOnClickListener(v -> {
			Fragment fragment = getTargetFragment();
			if (fragment instanceof OsmAuthorizationListener) {
				authHelper.addListener((OsmAuthorizationListener) fragment);
			}
			authHelper.startOAuth((ViewGroup) v, nightMode);
		});
	}

	private void setupButtonBackground(@NonNull View button, @ColorInt int normalColor) {
		Drawable normal = createRoundedDrawable(normalColor, ButtonBackground.ROUNDED_SMALL);
		Drawable pressed = AppCompatResources.getDrawable(app, ButtonBackground.ROUNDED_SMALL.getRippleId(nightMode));

		AndroidUtils.setBackground(button, UiUtilities.getLayeredIcon(normal, pressed));
	}

	@Override
	protected void updateToolbar(int verticalOffset) {
		float absOffset = Math.abs(verticalOffset);
		float totalScrollRange = appBar.getTotalScrollRange();

		float alpha = ColorUtilities.getProportionalAlpha(totalScrollRange * 0.25f, totalScrollRange * 0.9f, absOffset);
		float inverseAlpha = 1.0f - ColorUtilities.getProportionalAlpha(totalScrollRange * 0.5f, totalScrollRange, absOffset);

		TextView tvTitle = mainView.findViewById(R.id.toolbar_title);
		tvTitle.setAlpha(inverseAlpha);

		mainView.findViewById(R.id.header).setAlpha(alpha);
		mainView.findViewById(R.id.shadowView).setAlpha(inverseAlpha);
	}
}