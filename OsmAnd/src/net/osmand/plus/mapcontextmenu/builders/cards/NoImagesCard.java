package net.osmand.plus.mapcontextmenu.builders.cards;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapillary.MapillaryPlugin;

public class NoImagesCard extends AbstractCard {

	public NoImagesCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.context_menu_card_no_images;
	}

	@Override
	public void update() {
		if (view != null) {
			UiUtilities ic = getMyApplication().getUIUtilities();
			boolean night = getMyApplication().getDaynightHelper().isNightModeForMapControls();
			MapActivity ctx = getMapActivity();
			AndroidUtils.setBackgroundColor(ctx, view, night, R.color.list_background_color_light, R.color.list_background_color_dark);
			((ImageView) view.findViewById(R.id.icon_sadface)).setImageDrawable(ic.getIcon(R.drawable.ic_action_sadface,
					night ? R.color.color_white : R.color.icon_color_default_light));
			AndroidUtils.setTextPrimaryColor(ctx, (TextView) view.findViewById(R.id.title), night);
			AndroidUtils.setBackgroundColor(ctx, view.findViewById(R.id.button_background), night,
					R.color.inactive_buttons_and_links_bg_light, R.color.inactive_buttons_and_links_bg_dark);
			((ImageView) view.findViewById(R.id.icon_add_photos)).setImageDrawable(ic.getIcon(R.drawable.ic_action_add_photos,
					night ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
			((TextView) view.findViewById(R.id.app_photos_text_view)).setTextColor(ContextCompat.getColor(ctx,
					night ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
			AndroidUtils.setBackground(ctx, view.findViewById(R.id.card_background), night,
					R.drawable.context_menu_card_light, R.drawable.context_menu_card_dark);
			view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapillaryPlugin.openMapillary(getMapActivity(), null);
				}
			});
		}
	}
}
