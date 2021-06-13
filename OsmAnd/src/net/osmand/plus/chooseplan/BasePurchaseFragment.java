package net.osmand.plus.chooseplan;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;

import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getActiveColorId;

public abstract class BasePurchaseFragment extends BaseOsmAndDialogFragment {

	protected OsmandApplication app;
	protected InAppPurchaseHelper purchaseHelper;
	protected LayoutInflater inflater;
	protected Context themedCtx;
	protected boolean nightMode;

	protected View view;

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

		public boolean isLastItem() {
			OsmAndFeature[] features = OsmAndFeature.values();
			return this == features[features.length - 1];
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
		initData(savedInstanceState);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity ctx = requireActivity();
		int themeId = nightMode ?
				R.style.OsmandDarkTheme_DarkActionbar :
				R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
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
	public View onCreateView(@NonNull LayoutInflater i,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(getLayoutId(), container, false);
		initView();
		return view;
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

	protected void setupButtonBackground(@NonNull View v) {
		setupButtonBackground(v, ContextCompat.getColor(themedCtx, getActiveColorId(nightMode)));
	}

	protected void setupButtonBackground(@NonNull View v, @ColorInt int colorNoAlpha) {
		int colorAlpha = UiUtilities.getColorWithAlpha(colorNoAlpha, 0.1f);
		Drawable normal = createRoundedDrawable(colorAlpha);
		Drawable selectable = createSelectableDrawable(colorAlpha);
		Drawable[] layers = {normal, selectable};
		LayerDrawable layerDrawable = new LayerDrawable(layers);
		AndroidUtils.setBackground(v, layerDrawable);
	}

	protected Drawable createSelectableDrawable(@ColorInt int color) {
		Drawable normal = new ColorDrawable(Color.TRANSPARENT);
		Drawable selectable = createRoundedDrawable(color);
		// todo investigate
		// UiUtilities.getColoredSelectableDrawable(app, colorAlpha, 1.0f)
		return AndroidUtils.createPressedStateListDrawable(normal, selectable);
	}

	protected Drawable getActiveStrokeDrawable() {
		return app.getUIUtilities().getIcon(nightMode ?
				R.drawable.btn_background_stroked_active_dark :
				R.drawable.btn_background_stroked_active_light);
	}

	protected void setupIconBackground(@NonNull View v,
	                                   @ColorInt int color) {
		Drawable background = createRoundedDrawable(color);
		AndroidUtils.setBackground(v, background);
	}

	protected Drawable createRoundedDrawable(@ColorInt int color) {
		return UiUtilities.createTintedDrawable(app, R.drawable.rectangle_rounded, color);
	}

	protected int getAlphaColor(@ColorInt int colorNoAlpha, float ratio) {
		return UiUtilities.getColorWithAlpha(colorNoAlpha, ratio);
	}

	protected void bindFeatureItem(@NonNull View itemView,
	                               @NonNull OsmAndFeature feature,
	                               boolean useHeaderTitle) {
		ImageView ivIcon = itemView.findViewById(R.id.icon);
		ivIcon.setImageResource(feature.getIconId(nightMode));

		int titleId = useHeaderTitle ? feature.getHeaderTitleId() : feature.getInListTitleId();
		TextView tvTitle = itemView.findViewById(R.id.title);
		tvTitle.setText(getString(titleId));
	}

	protected void openUrl(@NonNull String url) {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setData(Uri.parse(url));
		if (AndroidUtils.isIntentSafe(app, i)) {
			app.startActivity(i);
		}
	}

	protected abstract void initData(@Nullable Bundle args);

	protected abstract int getLayoutId();

	protected abstract void initView();

}
