package net.osmand.plus.track.cards;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXUtilities.Metadata;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;

public abstract class BaseMetadataCard extends MapBaseCard {

	protected final Metadata metadata;

	private ViewGroup itemsContainer;

	public BaseMetadataCard(@NonNull MapActivity mapActivity, @NonNull Metadata metadata) {
		super(mapActivity);
		this.metadata = metadata;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_info_card;
	}

	@StringRes
	abstract protected int getTitleId();

	@Override
	public void updateContent() {
		TextView header = view.findViewById(R.id.header);
		header.setText(getTitleId());

		itemsContainer = view.findViewById(R.id.items_container);
		itemsContainer.removeAllViews();
	}

	@NonNull
	protected View createItemRow(@NonNull String title, @NonNull String description, @Nullable Drawable icon) {
		View view = themedInflater.inflate(R.layout.item_with_title_desc, itemsContainer, false);
		itemsContainer.addView(view);

		ImageView iconIv = view.findViewById(R.id.icon);
		TextView titleTv = view.findViewById(R.id.title);
		TextView descriptionTv = view.findViewById(R.id.description);

		titleTv.setText(title);
		descriptionTv.setText(description);
		iconIv.setImageDrawable(icon);
		AndroidUiHelper.updateVisibility(iconIv, icon != null);

		view.setOnLongClickListener(v -> {
			ShareMenu.copyToClipboardWithToast(activity, description, Toast.LENGTH_SHORT);
			return true;
		});

		return view;
	}

	public void createLinkItemRow(@NonNull String title, @Nullable String link, @DrawableRes int iconRes) {
		if (!Algorithms.isEmpty(link)) {
			Drawable icon = getContentIcon(iconRes);
			View view = createItemRow(title, link, icon);

			TextView description = view.findViewById(R.id.description);
			description.setTextColor(ColorUtilities.getActiveColor(app, nightMode));

			view.setOnClickListener(v -> {
				if (app.getAppCustomization().isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(link));
					AndroidUtils.startActivityIfSafe(v.getContext(), intent);
				}
			});
		}
	}

	public void createEmailItemRow(@NonNull String title, @Nullable String email, @DrawableRes int iconRes) {
		if (!Algorithms.isEmpty(email)) {
			Drawable icon = getContentIcon(iconRes);
			View view = createItemRow(title, email, icon);

			TextView description = view.findViewById(R.id.description);
			description.setTextColor(ColorUtilities.getActiveColor(app, nightMode));

			view.setOnClickListener(v -> sendEmail(email));
		}
	}

	private void sendEmail(@NonNull String email) {
		Intent intent = new Intent("android.intent.action.SENDTO", Uri.fromParts("mailto", email, null));
		AndroidUtils.startActivityIfSafe(view.getContext(), intent);
	}
}