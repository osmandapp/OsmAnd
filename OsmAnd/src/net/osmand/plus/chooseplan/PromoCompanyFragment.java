package net.osmand.plus.chooseplan;

import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.HUGEROCK_PROMO;
import static net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin.TRIPLTEK_PROMO;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.List;

public abstract class PromoCompanyFragment extends BaseFullScreenDialogFragment {

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
	}

	@ColorRes
	protected int getStatusBarColorId() {
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	protected boolean isUsedOnMap() {
		return getActivity() instanceof MapActivity;
	}

	@NonNull
	protected abstract List<OsmAndFeature> getFeatures();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflate(R.layout.fragment_company_promo, container, false);

		setupToolbar(view);
		setupContent(view);
		setupButtons(view);
		setupShadows(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(R.drawable.ic_action_close, ColorUtilities.getPrimaryIconColorId(nightMode)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> dismiss());
	}

	protected void setupContent(@NonNull View view) {
		LinearLayout container = view.findViewById(R.id.items_container);
		container.removeAllViews();

		for (OsmAndFeature feature : getFeatures()) {
			container.addView(createFeatureView(feature, container));
		}
	}

	@NonNull
	private View createFeatureView(@NonNull OsmAndFeature feature, @NonNull ViewGroup container) {
		View view = inflate(R.layout.purchase_dialog_list_item, container, false);

		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageResource(feature.getIconId(nightMode));

		TextView title = view.findViewById(R.id.title);
		title.setText(getString(feature.getListTitleId()));

		ImageView checkmarkIcon = view.findViewById(R.id.tertiary_icon);
		checkmarkIcon.setImageDrawable(getIcon(nightMode ? R.drawable.ic_action_checkmark_colored_night : R.drawable.ic_action_checkmark_colored_day));

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.secondary_icon), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), false);

		return view;
	}

	private void setupButtons(@NonNull View view) {
		View container = view.findViewById(R.id.bottom_buttons_container);
		container.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		DialogButton button = container.findViewById(R.id.dismiss_button);
		button.setTitleId(R.string.see_all_plans);
		button.setButtonType(DialogButtonType.SECONDARY);
		button.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				ChoosePlanFragment.showDefaultInstance(activity);
			}
			dismiss();
		});
	}

	private void setupShadows(@NonNull View view) {
		View appBar = view.findViewById(R.id.appbar);
		View buttonsShadow = view.findViewById(R.id.buttons_shadow);
		ScrollView scrollView = view.findViewById(R.id.scroll_view);
		scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
			boolean scrollToTopAvailable = scrollView.canScrollVertically(-1);
			boolean scrollToBottomAvailable = scrollView.canScrollVertically(1);

			updateShadowVisibility(buttonsShadow, scrollToBottomAvailable);
			ViewCompat.setElevation(appBar, scrollToTopAvailable ? 5.0f : 0f);
		});
	}

	private void updateShadowVisibility(@NonNull View view, boolean visible) {
		view.animate().alpha(visible ? 0.8f : 0f).setDuration(200);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull PurchaseOrigin origin) {
		if (origin == TRIPLTEK_PROMO) {
			TripltekPromoFragment.showInstance(manager);
		} else if (origin == HUGEROCK_PROMO) {
			HugerockPromoFragment.showInstance(manager);
		}
	}
}