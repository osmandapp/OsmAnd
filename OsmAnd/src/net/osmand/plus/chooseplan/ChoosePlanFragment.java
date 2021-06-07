package net.osmand.plus.chooseplan;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
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
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.settings.fragments.TroubleshootingOrPurchasingCard;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.Map;

import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getActivePrimaryColorId;
import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getDefaultIconColorId;

public class ChoosePlanFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = ChoosePlanFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(ChoosePlanFragment.class);

	private OsmandApplication app;
	private InAppPurchaseHelper purchaseHelper;
	private LayoutInflater inflater;
	private Context themedCtx;
	private boolean nightMode;

	private View view;
	private LinearLayout listContainer;
	private Map<OsmAndFeature, View> itemViews = new HashMap<>();

	private OsmAndFeature selectedFeature;

	public enum OsmAndFeature {
		OSMAND_CLOUD(R.string.osmand_cloud, R.string.purchases_feature_desc_osmand_cloud, R.drawable.ic_action_cloud_upload_colored_day, R.drawable.ic_action_cloud_upload_colored_day, false),
		ADVANCED_WIDGETS(R.string.advanced_widgets, R.string.purchases_feature_desc_advanced_widgets, R.drawable.ic_action_advanced_widgets_colored, R.drawable.ic_action_advanced_widgets_colored, false),
		HOURLY_MAP_UPDATES(R.string.daily_map_updates, R.string.purchases_feature_desc_hourly_map_updates, R.drawable.ic_action_map_updates_colored_day, R.drawable.ic_action_map_updates_colored_day, false),
		MONTHLY_MAP_UPDATES(R.string.monthly_map_updates, R.string.purchases_feature_desc_monthly_map_updates, R.drawable.ic_action_monthly_map_updates_colored_day, R.drawable.ic_action_monthly_map_updates_colored_day, true),
		UNLIMITED_MAP_DOWNLOADS(R.string.unlimited_map_downloads, R.string.purchases_feature_desc_unlimited_map_download, R.drawable.ic_action_unlimited_downloads_colored_day, R.drawable.ic_action_unlimited_downloads_colored_day, true),
		WIKIPEDIA(R.string.shared_string_wikipedia, R.string.offline_wikipeadia, R.string.purchases_feature_desc_wikipedia, R.drawable.ic_action_wikipedia_download_colored_day, R.drawable.ic_action_wikipedia_download_colored_day, true),
		WIKIVOYAGE(R.string.shared_string_wikivoyage, R.string.offline_wikivoyage, R.string.purchases_feature_desc_wikivoyage, R.drawable.ic_action_backpack_colored_day, R.drawable.ic_action_backpack_colored_day, true),
		TERRAIN(R.string.terrain_maps, R.string.terrain_maps_contour_lines_hillshade_slope, R.string.purchases_feature_desc_terrain, R.drawable.ic_action_srtm_colored_day, R.drawable.ic_action_srtm_colored_day, true),
		NAUTICAL_DEPTH(R.string.nautical_depth, R.string.purchases_feature_desc_nautical, R.drawable.ic_action_nautical_depth_colored_day, R.drawable.ic_action_nautical_depth_colored_day, true);

		OsmAndFeature(int titleId,
		              int descriptionId,
		              int icDayId,
		              int icNightId,
		              boolean availableInMapsPlus) {
			// constructor for features with the same titles in header and list
			this(titleId, titleId, descriptionId, icDayId, icNightId, availableInMapsPlus);
		}

		OsmAndFeature(int headerTitleId,
		              int inListTitleId,
		              int descriptionId,
		              int icDayId,
		              int icNightId,
		              boolean availableInMapsPlus) {
			this.headerTitleId = headerTitleId;
			this.inListTitleId = inListTitleId;
			this.descriptionId = descriptionId;
			this.icDayId = icDayId;
			this.icNightId = icNightId;
			this.availableInMapsPlus = availableInMapsPlus;
		}

		private int headerTitleId;
		private int inListTitleId;
		private int descriptionId;
		private int icDayId;
		private int icNightId;
		public boolean availableInMapsPlus;

		public int getHeaderTitleId() {
			return headerTitleId;
		}

		public int getInListTitleId() {
			return inListTitleId;
		}

		public int getDescriptionId() {
			return descriptionId;
		}

		public boolean isAvailableInMapsPlus() {
			return availableInMapsPlus;
		}

		public int getIconId(boolean nightMode) {
			return nightMode ? icNightId : icDayId;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		purchaseHelper = app.getInAppPurchaseHelper();
		nightMode = isNightMode(getMapActivity() != null);
		inflater = UiUtilities.getInflater(app, nightMode);
		themedCtx = UiUtilities.getThemedContext(app, nightMode);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity ctx = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(ctx, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			if (Build.VERSION.SDK_INT >= 21) {
				window.setStatusBarColor(ContextCompat.getColor(ctx, getStatusBarColorId()));
			}
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_choose_plan, container, false);
		listContainer = view.findViewById(R.id.list_container);
		initView();
		return view;
	}

	private void initView() {
		setupHeaderIconBackground();
		createFeaturesList();
		setupLaterButton();
		createTroubleshootingCard();
		fullUpdateView();
	}

	private void fullUpdateView() {
		updateHeader();
		updateListSelection();
		updateContinueButtons();
	}

	private void selectFeature(OsmAndFeature feature) {
		if (selectedFeature != feature) {
			selectedFeature = feature;
			fullUpdateView();
		}
	}

	private void setupHeaderIconBackground() {
		FrameLayout backgroundView = view.findViewById(R.id.header_icon_background);
		int color = AndroidUtils.getColorFromAttr(themedCtx, R.attr.activity_background_color);
		Drawable bgDrawable = UiUtilities.createTintedDrawable(app, R.drawable.rectangle_rounded, color);
		AndroidUtils.setBackground(backgroundView, bgDrawable);
	}

	private void createFeaturesList() {
		itemViews.clear();
		listContainer.removeAllViews();
		for (OsmAndFeature feature : OsmAndFeature.values()) {
			View view = createFeatureItemView(feature);
			itemViews.put(feature, view);
			listContainer.addView(view);
		}
	}

	private View createFeatureItemView(@NonNull OsmAndFeature feature) {
		View v = inflater.inflate(R.layout.purchase_osmand_feature_item, listContainer, false);

		Drawable icon = getIcon(feature.getIconId(nightMode));
		((ImageView) v.findViewById(R.id.primary_icon)).setImageDrawable(icon);

		String title = getString(feature.getInListTitleId());
		((TextView) v.findViewById(R.id.title)).setText(title);

		int mapsPlusVisibility = feature.isAvailableInMapsPlus() ? View.VISIBLE : View.INVISIBLE;
		v.findViewById(R.id.secondary_icon).setVisibility(mapsPlusVisibility);

		v.setOnClickListener(v1 -> {
			selectFeature(feature);
		});

		View divider = v.findViewById(R.id.bottom_divider);
		divider.setVisibility(isLastItem(feature) ? View.GONE : View.VISIBLE);

		return v;
	}

	private void setupLaterButton() {
		int colorNoAlpha = ContextCompat.getColor(themedCtx, getActivePrimaryColorId(nightMode));
		int colorAlpha = UiUtilities.getColorWithAlpha(colorNoAlpha, 0.15f);

		View button = view.findViewById(R.id.button_later);
		Drawable bgDrawable = UiUtilities.createTintedDrawable(app, R.drawable.rectangle_rounded, colorAlpha);
		Drawable selectableBg = UiUtilities.getColoredSelectableDrawable(app, colorAlpha, 1.0f);
		Drawable[] layers = {bgDrawable, selectableBg};
		LayerDrawable layerDrawable = new LayerDrawable(layers);
		AndroidUtils.setBackground(button, layerDrawable);
	}

	private void createTroubleshootingCard() {
		FragmentActivity activity = getMapActivity();
		if (activity != null) {
			FrameLayout container = view.findViewById(R.id.troubleshooting_card);
			boolean isPaidVersion = Version.isPaidVersion(app);
			container.addView(new TroubleshootingOrPurchasingCard(getMapActivity(), purchaseHelper, isPaidVersion)
					.build(activity));
		}
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
			int activeColor = ContextCompat.getColor(themedCtx, getActivePrimaryColorId(nightMode));
			int transparent = ContextCompat.getColor(themedCtx, R.color.color_transparent);
			int colorWithAlpha = UiUtilities.getColorWithAlpha(activeColor, 0.15f);
			int bgColor = selected ? colorWithAlpha : transparent;

			Drawable selectableBg = UiUtilities.getColoredSelectableDrawable(app, colorWithAlpha, 1.0f);
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
				true);

		boolean availableInMapsPlus = selectedFeature.isAvailableInMapsPlus();
		int mapsPlusIconId = availableInMapsPlus ?
				R.drawable.ic_action_osmand_maps_plus :
				R.drawable.ic_action_osmand_maps_plus_desaturated;
		updateContinueButton(view.findViewById(R.id.button_continue_maps_plus),
				mapsPlusIconId,
				getString(R.string.maps_plus),
				"From € 9,99 / year",
				availableInMapsPlus);
	}

	private void updateContinueButton(View v,
	                                  int iconId,
	                                  String title,
	                                  String description,
	                                  boolean available) {
		int colorNoAlpha = ContextCompat.getColor(themedCtx,
				available ? getActivePrimaryColorId(nightMode) : getDefaultIconColorId(nightMode));
		int colorAlphaLess = UiUtilities.getColorWithAlpha(colorNoAlpha, 0.75f);
		int colorAlphaMany = UiUtilities.getColorWithAlpha(colorNoAlpha, 0.15f);

		int baseTitlePartId = available ? R.string.continue_with : R.string.not_available_with;
		title = String.format(getString(baseTitlePartId), title);
		TextView tvTitle = v.findViewById(R.id.title);
		tvTitle.setText(title);
		tvTitle.setTextColor(colorNoAlpha);

		TextView tvDescription = v.findViewById(R.id.description);
		tvDescription.setText(description);
		tvDescription.setTextColor(colorAlphaLess);

		ImageView ivIcon = v.findViewById(R.id.icon);
		ivIcon.setImageResource(iconId);

		Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(R.drawable.rectangle_rounded, colorAlphaMany);
		Drawable selectableBg = UiUtilities.getColoredSelectableDrawable(app, colorAlphaMany, 1.0f);
		Drawable[] layers = {bgDrawable, selectableBg};
		LayerDrawable layerDrawable = new LayerDrawable(layers);
		AndroidUtils.setBackground(v, layerDrawable);

		v.setOnClickListener(v1 -> app.showShortToastMessage("Toast"));
		v.setEnabled(available);
	}

	private boolean isLastItem(OsmAndFeature feature) {
		OsmAndFeature[] features = OsmAndFeature.values();
		return feature == features[features.length - 1];
	}

	@ColorRes
	protected int getStatusBarColorId() {
		return nightMode ?
				R.color.list_background_color_dark :
				R.color.list_background_color_light;
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
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

}
