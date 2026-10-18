package net.osmand.plus.download.ui;

import static net.osmand.plus.chooseplan.OsmAndFeature.UNLIMITED_MAP_DOWNLOADS;
import static net.osmand.plus.utils.FontCache.FONT_WEIGHT_MEDIUM;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.card.MaterialCardView;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.FreeRegionHelper;
import net.osmand.plus.helpers.DiscountHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.util.Algorithms;

public class FreeVersionBanner {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final DownloadActivity activity;

	private final View freeVersionBanner;
	private final View freeVersionBannerTitle;
	private final View freeVersionCtaContainer;
	private final View freeVersionCtaContentContainer;
	private final TextView freeVersionTitleTextView;
	private final TextView freeVersionSubtitleTextView;
	private final TextView freeVersionDescriptionTextView;
	private final TextView downloadsLeftTextView;
	private final ImageView freeVersionCtaArrow;
	private final TextView freeVersionCtaDiscountBadge;

	private final OnClickListener onBannerClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			app.logEvent("click_free_dialog");
			ChoosePlanFragment.showInstance(activity, UNLIMITED_MAP_DOWNLOADS);
		}
	};

	public FreeVersionBanner(@NonNull View view, @NonNull DownloadActivity activity) {
		this.activity = activity;
		this.app = activity.getApp();
		this.settings = app.getSettings();

		freeVersionBanner = view.findViewById(R.id.freeVersionBanner);
		freeVersionBannerTitle = freeVersionBanner.findViewById(R.id.freeVersionBannerTitle);
		freeVersionCtaContainer = freeVersionBanner.findViewById(R.id.freeVersionCtaContainer);
		freeVersionCtaContentContainer = freeVersionBanner.findViewById(R.id.freeVersionCtaContentContainer);
		freeVersionTitleTextView = freeVersionBanner.findViewById(R.id.freeVersionTitleTextView);
		freeVersionSubtitleTextView = freeVersionBanner.findViewById(R.id.freeVersionSubtitleTextView);
		downloadsLeftTextView = freeVersionBanner.findViewById(R.id.downloadsLeftTextView);
		freeVersionDescriptionTextView = freeVersionBanner.findViewById(R.id.freeVersionDescriptionTextView);
		freeVersionCtaArrow = freeVersionBanner.findViewById(R.id.freeVersionCtaArrow);
		freeVersionCtaDiscountBadge = freeVersionBanner.findViewById(R.id.freeVersionCtaDiscountBadge);

		updateCtaArrowDirection();
		freeVersionBanner.findViewById(R.id.bannerTopLayout).setOnClickListener(onBannerClickListener);
		freeVersionCtaContainer.setOnClickListener(onBannerClickListener);
	}

	private void updateCtaArrowDirection() {
		Drawable arrow = AppCompatResources.getDrawable(activity, R.drawable.ic_arrow_forward);
		if (arrow != null) {
			freeVersionCtaArrow.setImageDrawable(AndroidUtils.getDrawableForDirection(activity, arrow.mutate()));
		}
	}

	public void initFreeVersionBanner() {
		if (!DownloadActivity.shouldShowFreeVersionBanner(app)) {
			freeVersionBanner.setVisibility(View.GONE);
			return;
		}
		freeVersionBanner.setVisibility(View.VISIBLE);
		updateFreeVersionBanner();
	}

	public void updateFreeVersionBanner() {
		if (!DownloadActivity.shouldShowFreeVersionBanner(app)) {
			if (freeVersionBanner.getVisibility() == View.VISIBLE) {
				freeVersionBanner.setVisibility(View.GONE);
			}
			return;
		}
		updateBanner();
	}

	/**
	 * Free version: one free region instead of the 7 downloads counter. The limit is never
	 * "reached" - the free region and updates stay free.
	 */
	private void updateBanner() {
		updateBannerState(1);
		WorldRegion region = new FreeRegionHelper(app).getFreeRegion();
		freeVersionBanner.findViewById(R.id.marksLinearLayout).setVisibility(View.GONE);
		downloadsLeftTextView.setText(region != null ? region.getLocaleName() : "");
		freeVersionSubtitleTextView.setVisibility(View.VISIBLE);
		freeVersionSubtitleTextView.setText(region != null
				? activity.getString(R.string.free_region_banner_descr, region.getLocaleName())
				: activity.getString(R.string.free_region_not_chosen_descr));
	}

	private void updateBannerState(int downloadsLeft) {
		boolean limitReached = downloadsLeft == 0;
		boolean nightMode = app.getDaynightHelper().isNightMode(settings.getApplicationMode(), ThemeUsageContext.APP);
		updateBannerColors();
		freeVersionTitleTextView.setText(limitReached ? R.string.free_download_limit_reached : R.string.free_version_title);
		downloadsLeftTextView.setVisibility(limitReached ? View.GONE : View.VISIBLE);
		freeVersionSubtitleTextView.setVisibility(limitReached ? View.VISIBLE : View.GONE);
		freeVersionDescriptionTextView.setText(R.string.get_unlimited_downloads);
		boolean hasDiscount = updateCtaDiscountBadge(nightMode);
		boolean filledCta = limitReached || hasDiscount;
		int ctaBackgroundId = R.drawable.free_version_banner_cta_neutral_ripple;
		int ctaHorizontalMargin = 0;
		int ctaTopMargin = 0;
		int ctaBottomMargin = 0;
		LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) freeVersionCtaContainer.getLayoutParams();
		if (filledCta) {
			int touchOverhang = (layoutParams.height - freeVersionCtaContentContainer.getLayoutParams().height) / 2;
			ctaHorizontalMargin = (int) app.getResources().getDimension(R.dimen.content_padding);
			ctaTopMargin = (int) app.getResources().getDimension(R.dimen.content_padding_small) - touchOverhang;
			ctaBottomMargin = ctaHorizontalMargin - touchOverhang;
			ctaBackgroundId = nightMode
					? R.drawable.free_version_banner_cta_bg_ripple_dark
					: R.drawable.free_version_banner_cta_bg_ripple;
		}
		layoutParams.leftMargin = ctaHorizontalMargin;
		layoutParams.rightMargin = ctaHorizontalMargin;
		layoutParams.bottomMargin = ctaBottomMargin;
		layoutParams.topMargin = ctaTopMargin;
		freeVersionCtaContentContainer.setBackgroundResource(ctaBackgroundId);
		freeVersionCtaContainer.setVisibility(View.VISIBLE);
	}

	private void updateBannerColors() {
		int backgroundColor = AndroidUtils.getColorFromAttr(activity, R.attr.list_background_color);
		if (freeVersionBanner instanceof MaterialCardView cardView) {
			cardView.setCardBackgroundColor(backgroundColor);
			cardView.setCardElevation(0);
		} else {
			freeVersionBanner.setBackgroundColor(backgroundColor);
		}
		int textColor = AndroidUtils.getColorFromAttr(activity, android.R.attr.textColor);
		int textColorSecondary = AndroidUtils.getColorFromAttr(activity, android.R.attr.textColorSecondary);
		freeVersionTitleTextView.setTextColor(textColor);
		downloadsLeftTextView.setTextColor(textColorSecondary);
		freeVersionSubtitleTextView.setTextColor(textColorSecondary);
	}

	private boolean updateCtaDiscountBadge(boolean nightMode) {
		String discount = DiscountHelper.getCurrentSaleDiscount(app, nightMode, true);
		boolean hasDiscount = !Algorithms.isEmpty(discount);
		freeVersionCtaDiscountBadge.setText(discount);
		freeVersionCtaDiscountBadge.setBackgroundResource(nightMode
				? R.drawable.free_version_banner_cta_discount_bg_dark
				: R.drawable.free_version_banner_cta_discount_bg);
		freeVersionCtaDiscountBadge.setVisibility(hasDiscount ? View.VISIBLE : View.GONE);
		freeVersionCtaArrow.setVisibility(hasDiscount ? View.GONE : View.VISIBLE);
		return hasDiscount;
	}

	protected void updateAvailableDownloads() {
		updateBanner();
	}
}
