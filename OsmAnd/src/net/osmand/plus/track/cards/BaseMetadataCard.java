package net.osmand.plus.track.cards;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseMetadataCard extends MapBaseCard {
	public static final int NO_ICON = -1;
	protected LayoutInflater inflater;
	protected ViewGroup container;

	public BaseMetadataCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		inflater = UiUtilities.getInflater(mapActivity, nightMode);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.metadata_card;
	}

	@Override
	public void updateContent() {
		TextView titleView = view.findViewById(R.id.card_title);
		titleView.setText(getCardTitle());
		container = view.findViewById(R.id.item_container);

		updateCard();
	}

	abstract void updateCard();

	abstract protected int getCardTitle();

	protected void addNewItem(int titleId, String descriptionText, boolean isUrl, boolean copyOnLongClick) {
		addNewItem(app.getString(titleId), descriptionText, isUrl, copyOnLongClick);
	}

	protected void addNewItem(String title, String descriptionText, boolean isUrl, boolean copyOnLongClick) {
		LinearLayout nameView = (LinearLayout) inflater.inflate(R.layout.item_with_title_desc, null);
		container.addView(nameView);
		fillCardItems(nameView, NO_ICON, title, descriptionText, isUrl, copyOnLongClick);
	}

	private void fillCardItems(LinearLayout container, int iconId, String titleText, String descriptionText, boolean isUrl, boolean copyOnLongClick) {
		ImageView icon = container.findViewById(R.id.icon);
		TextView title = container.findViewById(R.id.title);
		TextView description = container.findViewById(R.id.description);

		title.setText(titleText);
		if (iconId == NO_ICON) {
			icon.setVisibility(View.GONE);
		} else {
			icon.setImageResource(iconId);
		}
		description.setText(descriptionText);

		if (isUrl) {
			container.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
			int linkTextColor = ContextCompat.getColor(view.getContext(), !nightMode ? R.color.active_color_primary_light : R.color.active_color_primary_dark);
			description.setTextColor(linkTextColor);
			container.setOnClickListener(v -> {
				if (app.getAppCustomization().isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(descriptionText));
					AndroidUtils.startActivityIfSafe(v.getContext(), intent);
				}
			});
		}
		if (copyOnLongClick) {
			container.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
			container.setOnLongClickListener(v -> {
				copyToClipboard(descriptionText, view.getContext());
				return true;
			});
		}
	}

	protected static void copyToClipboard(String text, Context ctx) {
		ShareMenu.copyToClipboardWithToast(ctx, text, Toast.LENGTH_SHORT);
	}
}
