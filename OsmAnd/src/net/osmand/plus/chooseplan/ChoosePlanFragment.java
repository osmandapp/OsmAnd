package net.osmand.plus.chooseplan;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.chooseplan.button.PriceButton;
import net.osmand.plus.helpers.AndroidUiHelper;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getActiveColorId;
import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getDefaultIconColorId;

public class ChoosePlanFragment extends BasePurchaseDialogFragment {

	public static final String TAG = ChoosePlanFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(ChoosePlanFragment.class);

	public static final String SELECTED_FEATURE = "selected_feature";

	private LinearLayout listContainer;
	private OsmAndFeature selectedFeature;
	private final List<OsmAndFeature> allFeatures = new ArrayList<>();

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_choose_plan;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		allFeatures.addAll(Arrays.asList(OsmAndFeature.values()));
		allFeatures.remove(OsmAndFeature.COMBINED_WIKI);
		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_FEATURE)) {
			selectedFeature = OsmAndFeature.valueOf(savedInstanceState.getString(SELECTED_FEATURE));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		listContainer = mainView.findViewById(R.id.list_container);

		setupToolbar();
		createFeaturesList();
		setupLaterButton();
		createTroubleshootingCard();

		return mainView;
	}

	@Override
	protected void updateContent(boolean progress) {
		updateHeader();
		updateToolbar();
		updateListSelection();
		updateContinueButtons();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (selectedFeature != null) {
			outState.putString(SELECTED_FEATURE, selectedFeature.name());
		}
	}

	private void createFeaturesList() {
		listContainer.removeAllViews();
		for (OsmAndFeature feature : allFeatures) {
			View view = createFeatureItemView(feature);
			listContainer.addView(view);
		}
	}

	private View createFeatureItemView(@NonNull OsmAndFeature feature) {
		View view = themedInflater.inflate(R.layout.purchase_dialog_list_item, listContainer, false);
		view.setTag(feature);
		view.setOnClickListener(v -> selectFeature(feature));
		bindFeatureItem(view, feature, false);
		return view;
	}

	@Override
	protected void bindFeatureItem(@NonNull View view, @NonNull OsmAndFeature feature, boolean useHeaderTitle) {
		super.bindFeatureItem(view, feature, useHeaderTitle);

		int visibility = feature.isAvailableInMapsPlus() ? View.VISIBLE : View.INVISIBLE;
		AndroidUiHelper.setVisibility(visibility, view.findViewById(R.id.secondary_icon));

		boolean isLastItem = feature == allFeatures.get(allFeatures.size() - 1);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !isLastItem);
	}

	private void setupToolbar() {
		ImageView backBtn = mainView.findViewById(R.id.button_back);
		backBtn.setImageResource(AndroidUtils.getNavigationIconResId(app));
		backBtn.setOnClickListener(v -> dismiss());

		ImageView restoreBtn = mainView.findViewById(R.id.button_reset);
		restoreBtn.setOnClickListener(v -> purchaseHelper.requestInventory());

		FrameLayout iconBg = mainView.findViewById(R.id.header_icon_background);
		int color = AndroidUtils.getColorFromAttr(mainView.getContext(), R.attr.activity_background_color);
		AndroidUtils.setBackground(iconBg, createRoundedDrawable(color));
	}

	@Override
	protected void updateToolbar(int verticalOffset) {
		float absOffset = Math.abs(verticalOffset);
		float totalScrollRange = appBar.getTotalScrollRange();

		float alpha = UiUtilities.getProportionalAlpha(totalScrollRange * 0.25f, totalScrollRange * 0.9f, absOffset);
		float inverseAlpha = 1.0f - UiUtilities.getProportionalAlpha(totalScrollRange * 0.5f, totalScrollRange, absOffset);

		TextView tvTitle = mainView.findViewById(R.id.toolbar_title);
		tvTitle.setText(getString(selectedFeature.getTitleId()));
		tvTitle.setAlpha(inverseAlpha);

		mainView.findViewById(R.id.header).setAlpha(alpha);
		mainView.findViewById(R.id.shadowView).setAlpha(inverseAlpha);
	}

	private void setupLaterButton() {
		View button = mainView.findViewById(R.id.button_later);
		button.setOnClickListener(v -> dismiss());
		setupRoundedBackground(button);
	}

	private void createTroubleshootingCard() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FrameLayout container = mainView.findViewById(R.id.troubleshooting_card);
			container.addView(new TroubleshootingCard(activity, purchaseHelper, true).build(activity));
		}
	}

	private void selectFeature(OsmAndFeature feature) {
		if (selectedFeature != feature) {
			selectedFeature = feature;
			updateContent(isRequestingInventory());
		}
	}

	private void updateHeader() {
		if (selectedFeature != null) {
			Drawable icon = getIcon(selectedFeature.getIconId(nightMode));
			((ImageView) mainView.findViewById(R.id.header_icon)).setImageDrawable(icon);

			String title = getString(selectedFeature.getTitleId());
			((TextView) mainView.findViewById(R.id.header_title)).setText(title);

			String desc = getString(selectedFeature.getDescriptionId());
			((TextView) mainView.findViewById(R.id.primary_description)).setText(desc);

			String mapsPlus = getString(R.string.maps_plus);
			String osmAndPro = getString(R.string.osmand_pro);
			String availablePlans = osmAndPro;
			if (selectedFeature.isAvailableInMapsPlus()) {
				availablePlans = String.format(getString(R.string.ltr_or_rtl_combine_via_or), mapsPlus, osmAndPro);
			}
			String pattern = getString(R.string.you_can_get_feature_as_part_of_pattern);
			String secondaryDesc = String.format(pattern, title, availablePlans);
			SpannableString message = UiUtilities.createSpannableString(secondaryDesc, Typeface.BOLD, mapsPlus, osmAndPro);
			((TextView) mainView.findViewById(R.id.secondary_description)).setText(message);
		}
	}

	private void updateListSelection() {
		for (View view : AndroidUtils.getChildrenViews(listContainer)) {
			OsmAndFeature feature = (OsmAndFeature) view.getTag();
			boolean selected = feature == selectedFeature;
			int activeColor = ContextCompat.getColor(app, getActiveColorId(nightMode));
			int colorWithAlpha = UiUtilities.getColorWithAlpha(activeColor, 0.1f);
			int bgColor = selected ? colorWithAlpha : Color.TRANSPARENT;

			Drawable selectableBg = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.5f);
			Drawable drawable = UiUtilities.getLayeredIcon(new ColorDrawable(bgColor), selectableBg);
			AndroidUtils.setBackground(view, drawable);
		}
	}

	private void updateContinueButtons() {
		List<PriceButton<?>> priceButtons = OsmAndProPlanFragment.collectPriceButtons(app, purchaseHelper);
		PriceButton<?>[] array = new PriceButton[priceButtons.size()];
		priceButtons.toArray(array);

		updateContinueButton(mainView.findViewById(R.id.button_continue_pro),
				R.drawable.ic_action_osmand_pro_logo,
				getString(R.string.osmand_pro),
				ObjectUtils.min(array).getPrice(),
				v -> OsmAndProPlanFragment.showInstance(requireActivity()),
				true);

		priceButtons = MapsPlusPlanFragment.collectPriceButtons(app, purchaseHelper);
		array = new PriceButton[priceButtons.size()];
		priceButtons.toArray(array);

		boolean availableInMapsPlus = selectedFeature.isAvailableInMapsPlus();
		int mapsPlusIconId = availableInMapsPlus ? R.drawable.ic_action_osmand_maps_plus : R.drawable.ic_action_osmand_maps_plus_desaturated;
		updateContinueButton(mainView.findViewById(R.id.button_continue_maps_plus),
				mapsPlusIconId,
				getString(R.string.maps_plus),
				ObjectUtils.min(array).getPrice(),
				v -> MapsPlusPlanFragment.showInstance(requireActivity()),
				availableInMapsPlus);
	}

	private void updateContinueButton(View view, int iconId, String plan, CharSequence price, OnClickListener listener, boolean available) {
		int colorNoAlpha = ContextCompat.getColor(app, available ? getActiveColorId(nightMode) : getDefaultIconColorId(nightMode));

		int pattern = available ? R.string.continue_with : R.string.not_available_with;
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(String.format(getString(pattern), plan));
		tvTitle.setTextColor(colorNoAlpha);

		TextView tvDescription = view.findViewById(R.id.description);
		String pricePattern = getString(R.string.from_with_param);
		String description = String.format(pricePattern, price);
		tvDescription.setText(description);
		tvDescription.setTextColor(UiUtilities.getColorWithAlpha(colorNoAlpha, 0.75f));

		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageResource(iconId);

		setupRoundedBackground(view, colorNoAlpha);
		view.setOnClickListener(listener);
		view.setEnabled(available);
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull OsmAndFeature selectedFeature) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		if (Version.isAmazon()) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.getUrlWithUtmRef(app, "net.osmand.plus")));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (AndroidUtils.isIntentSafe(app, intent)) {
				app.startActivity(intent);
			}
		} else {
			FragmentManager fm = activity.getSupportFragmentManager();
			if (!fm.isStateSaved() && fm.findFragmentByTag(TAG) == null) {
				ChoosePlanFragment fragment = new ChoosePlanFragment();
				fragment.selectedFeature = selectedFeature;
				fragment.show(activity.getSupportFragmentManager(), TAG);
			}
		}
	}
}