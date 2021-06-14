package net.osmand.plus.chooseplan;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.Map;

import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getActiveColorId;
import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getDefaultIconColorId;

public class ChoosePlanFragment extends BasePurchaseFragment {

	public static final String TAG = SelectedPlanFragment.class.getSimpleName();
	protected static final Log LOG = PlatformUtil.getLog(SelectedPlanFragment.class);

	private LinearLayout listContainer;
	private Map<OsmAndFeature, View> itemViews = new HashMap<>();

	private OsmAndFeature selectedFeature;

	public static void showInstance(@NonNull FragmentActivity activity) {
		showInstance(activity, OsmAndFeature.OSMAND_CLOUD);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull OsmAndFeature selectedFeature) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		if (Version.isAmazon()) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.getUrlWithUtmRef(app, "net.osmand.plus")));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (AndroidUtils.isIntentSafe(app, intent)) {
				app.startActivity(intent);
			}
		} else {
			try {
				ChoosePlanFragment fragment = new ChoosePlanFragment();
				fragment.selectedFeature = selectedFeature;
				fragment.show(activity.getSupportFragmentManager(), TAG);
			} catch (RuntimeException e) {
				LOG.error(selectedFeature.name(), e);
			}
		}
	}

	@Override
	protected void initData(@Nullable Bundle args) {
		features.remove(OsmAndFeature.COMBINED_WIKI);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_choose_plan;
	}

	@Override
	protected void initView() {
		listContainer = view.findViewById(R.id.list_container);
		setupToolbar();
		setupHeaderIconBackground();
		createFeaturesList();
		setupLaterButton();
		createTroubleshootingCard();
		fullUpdateView();
	}

	private void setupToolbar() {
		ImageView backBtn = view.findViewById(R.id.button_back);
		backBtn.setImageResource(AndroidUtils.getNavigationIconResId(app));
		backBtn.setOnClickListener(v -> dismiss());

		ImageView restoreBtn = view.findViewById(R.id.button_reset);
		restoreBtn.setOnClickListener(v -> purchaseHelper.requestInventory());

		scrollView.getViewTreeObserver().addOnScrollChangedListener(this::updateToolbar);
	}

	private void updateToolbar() {
		View shadow = view.findViewById(R.id.toolbar_shadow);
		View header = view.findViewById(R.id.header);
		TextView tvTitle = view.findViewById(R.id.toolbar_title);
		if (scrollView.getScrollY() > header.getBottom()) {
			shadow.setVisibility(View.VISIBLE);
			tvTitle.setText(getString(selectedFeature.getHeaderTitleId()));

		} else {
			shadow.setVisibility(View.GONE);
			tvTitle.setText("");
		}
	}

	private void setupHeaderIconBackground() {
		FrameLayout v = view.findViewById(R.id.header_icon_background);
		int color = AndroidUtils.getColorFromAttr(themedCtx, R.attr.activity_background_color);
		setupIconBackground(v, color);
	}

	private void createFeaturesList() {
		itemViews.clear();
		listContainer.removeAllViews();
		for (OsmAndFeature feature : features) {
			View view = createFeatureItemView(feature);
			itemViews.put(feature, view);
			listContainer.addView(view);
		}
	}

	private View createFeatureItemView(@NonNull OsmAndFeature feature) {
		View v = inflater.inflate(R.layout.purchase_dialog_list_item, listContainer, false);
		v.setOnClickListener(v1 -> selectFeature(feature));
		bindFeatureItem(v, feature);
		return v;
	}

	protected void bindFeatureItem(@NonNull View v,
	                               @NonNull OsmAndFeature feature) {
		bindFeatureItem(v, feature, false);

		int mapsPlusVisibility = feature.isAvailableInMapsPlus() ? View.VISIBLE : View.INVISIBLE;
		v.findViewById(R.id.secondary_icon).setVisibility(mapsPlusVisibility);

		View divider = v.findViewById(R.id.bottom_divider);
		boolean isLastItem = features.indexOf(feature) == features.size() - 1;
		divider.setVisibility(isLastItem ? View.GONE : View.VISIBLE);
	}

	private void setupLaterButton() {
		View button = view.findViewById(R.id.button_later);
		button.setOnClickListener(v -> dismiss());
		setupRoundedBackground(button);
	}

	private void createTroubleshootingCard() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FrameLayout container = view.findViewById(R.id.troubleshooting_card);
			boolean isPaidVersion = Version.isPaidVersion(app);
			container.addView(new TroubleshootingOrPurchasingCard(activity, purchaseHelper, isPaidVersion, true)
					.build(activity));
		}
	}

	private void selectFeature(OsmAndFeature feature) {
		if (selectedFeature != feature) {
			selectedFeature = feature;
			fullUpdateView();
		}
	}

	private void fullUpdateView() {
		updateHeader();
		updateToolbar();
		updateListSelection();
		updateContinueButtons();
	}

	private void updateHeader() {
		if (selectedFeature == null) return;

		Drawable icon = getIcon(selectedFeature.getIconId(nightMode));
		((ImageView) view.findViewById(R.id.header_icon)).setImageDrawable(icon);

		String title = getString(selectedFeature.getHeaderTitleId());
		((TextView) view.findViewById(R.id.header_title)).setText(title);

		String desc = getString(selectedFeature.getDescriptionId());
		((TextView) view.findViewById(R.id.primary_description)).setText(desc);

		String mapsPlus = getString(R.string.maps_plus);
		String osmAndPro = getString(R.string.osmand_pro);
		String pattern = getString(R.string.you_can_get_feature_as_part_of_pattern);
		String availablePlans = osmAndPro;
		if (selectedFeature.isAvailableInMapsPlus()) {
			availablePlans = String.format(
					getString(R.string.ltr_or_rtl_combine_via_or),
					mapsPlus, osmAndPro);
		}
		String secondaryDesc = String.format(pattern, title, availablePlans);
		SpannableString message = UiUtilities.createSpannableString(secondaryDesc, Typeface.BOLD, mapsPlus, osmAndPro);
		((TextView) view.findViewById(R.id.secondary_description)).setText(message);
	}

	private void updateListSelection() {
		for (OsmAndFeature feature : itemViews.keySet()) {
			View v = itemViews.get(feature);

			boolean selected = feature == selectedFeature;
			int activeColor = ContextCompat.getColor(themedCtx, getActiveColorId(nightMode));
			int colorWithAlpha = getAlphaColor(activeColor, 0.1f);
			int bgColor = selected ? colorWithAlpha : Color.TRANSPARENT;

			Drawable selectableBg = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.5f);
			Drawable[] layers = {new ColorDrawable(bgColor), selectableBg};
			LayerDrawable layerDrawable = new LayerDrawable(layers);
			AndroidUtils.setBackground(v, layerDrawable);
		}
	}

	private void updateContinueButtons() {
		updateContinueButton(view.findViewById(R.id.button_continue_pro),
				R.drawable.ic_action_osmand_pro_logo,
				getString(R.string.osmand_pro),
				"From € 3 / month",
				v -> OsmAndProPlanFragment.showInstance(requireActivity()),
				true);

		boolean availableInMapsPlus = selectedFeature.isAvailableInMapsPlus();
		int mapsPlusIconId = availableInMapsPlus ?
				R.drawable.ic_action_osmand_maps_plus :
				R.drawable.ic_action_osmand_maps_plus_desaturated;
		updateContinueButton(view.findViewById(R.id.button_continue_maps_plus),
				mapsPlusIconId,
				getString(R.string.maps_plus),
				"From € 9,99 / year",
				v -> MapsPlusPlanFragment.showInstance(requireActivity()),
				availableInMapsPlus);
	}

	private void updateContinueButton(View v,
	                                  int iconId,
	                                  String title,
	                                  String description,
	                                  OnClickListener l,
	                                  boolean available) {
		int colorNoAlpha = ContextCompat.getColor(themedCtx,
				available ? getActiveColorId(nightMode) : getDefaultIconColorId(nightMode));

		int baseTitlePartId = available ? R.string.continue_with : R.string.not_available_with;
		title = String.format(getString(baseTitlePartId), title);
		TextView tvTitle = v.findViewById(R.id.title);
		tvTitle.setText(title);
		tvTitle.setTextColor(colorNoAlpha);

		TextView tvDescription = v.findViewById(R.id.description);
		tvDescription.setText(description);
		tvDescription.setTextColor(getAlphaColor(colorNoAlpha, 0.75f));

		ImageView ivIcon = v.findViewById(R.id.icon);
		ivIcon.setImageResource(iconId);

		setupRoundedBackground(v, colorNoAlpha);
		v.setOnClickListener(l);
		v.setEnabled(available);
	}

}
