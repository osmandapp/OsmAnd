package net.osmand.plus.myplaces.tracks.dialogs;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.DiscountHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.util.Algorithms;

public class TracksFreeBackupCard extends BaseCard {

	public static final int GET_OSMAND_PRO_BUTTON_INDEX = 0;
	private static final String DISCOUNT_BADGE_TAG = "tracks_backup_discount_badge";

	public TracksFreeBackupCard(@NonNull FragmentActivity activity) {
		super(activity, false);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.free_backup_card;
	}

	@Override
	protected void updateContent() {
		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);
		icon.setImageDrawable(getIcon(nightMode
				? R.drawable.ic_action_cloud_upload_colored_night
				: R.drawable.ic_action_cloud_upload_colored_day));
		title.setText(R.string.tracks_backup_promo_title);
		description.setText(R.string.tracks_backup_promo_description);
		ImageView closeButton = view.findViewById(R.id.btn_close);
		closeButton.setImageDrawable(getContentIcon(R.drawable.ic_action_cancel));
		closeButton.setOnClickListener(v -> dismiss());
		DialogButton actionButton = view.findViewById(R.id.dismiss_button_container);
		actionButton.setTitle(getString(R.string.tracks_backup_promo_button));
		actionButton.setOnClickListener(v -> notifyButtonPressed(GET_OSMAND_PRO_BUTTON_INDEX));
		updateDiscountBadge(actionButton);
	}

	private void updateDiscountBadge(@NonNull DialogButton actionButton) {
		String discount = getSubscriptionDiscount();
		FrameLayout buttonBody = actionButton.getButtonView().findViewById(R.id.button_body);
		TextViewEx buttonText = buttonBody.findViewById(R.id.button_text);
		View currentBadge = buttonBody.findViewWithTag(DISCOUNT_BADGE_TAG);
		if (Algorithms.isEmpty(discount)) {
			updateButtonTextAlignment(buttonText, false);
			if (currentBadge != null) {
				buttonBody.removeView(currentBadge);
			}
			return;
		}

		TextView discountBadge = currentBadge instanceof TextView ? (TextView) currentBadge : null;
		if (discountBadge == null) {
			if (currentBadge != null) {
				buttonBody.removeView(currentBadge);
			}
			discountBadge = new TextView(actionButton.getContext());
			discountBadge.setTag(DISCOUNT_BADGE_TAG);
			discountBadge.setGravity(Gravity.CENTER);
			discountBadge.setPadding(
					getDimen(R.dimen.content_padding_half),
					0,
					getDimen(R.dimen.content_padding_half),
					0);
			discountBadge.setTextColor(app.getColor(R.color.active_buttons_and_links_text_light));
			discountBadge.setTextSize(14);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.WRAP_CONTENT,
					FrameLayout.LayoutParams.MATCH_PARENT,
					Gravity.END | Gravity.CENTER_VERTICAL);
			int margin = (int) app.getResources().getDimension(R.dimen.content_padding_small_half);
			params.setMarginEnd(margin);
			params.topMargin = margin;
			params.bottomMargin = margin;
			buttonBody.addView(discountBadge, params);
		}
		discountBadge.setBackgroundResource(nightMode
				? R.drawable.free_version_banner_cta_discount_bg_dark
				: R.drawable.free_version_banner_cta_discount_bg);
		discountBadge.setText(discount);
		updateButtonTextAlignment(buttonText, true);
	}

	private void updateButtonTextAlignment(@NonNull TextViewEx buttonText, boolean hasDiscount) {
		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) buttonText.getLayoutParams();
		params.width = hasDiscount ? FrameLayout.LayoutParams.MATCH_PARENT : FrameLayout.LayoutParams.WRAP_CONTENT;
		params.gravity = hasDiscount ? Gravity.START | Gravity.CENTER_VERTICAL : Gravity.CENTER;
		buttonText.setLayoutParams(params);
		buttonText.setGravity(hasDiscount ? Gravity.START | Gravity.CENTER_VERTICAL : Gravity.CENTER);
	}

	private String getSubscriptionDiscount() {
		boolean nightMode = app.getDaynightHelper().isNightMode(settings.getApplicationMode(), ThemeUsageContext.APP);
		return DiscountHelper.getCurrentSaleDiscount(getMyApplication(), nightMode, false);
	}

	private void dismiss() {
		settings.TRACKS_FREE_ACCOUNT_CARD_DISMISSED.set(true);
		setLayoutNeeded();
	}

	public static boolean shouldShow(@NonNull OsmandApplication app, @NonNull TrackFolder rootFolder) {
		boolean hasTracks = !Algorithms.isEmpty(rootFolder.getFlattenedTrackItems());
		boolean backupAvailable = InAppPurchaseUtils.isBackupAvailable(app);
		boolean dismissed = app.getSettings().TRACKS_FREE_ACCOUNT_CARD_DISMISSED.get();
		return hasTracks && !backupAvailable && !dismissed;
	}
}