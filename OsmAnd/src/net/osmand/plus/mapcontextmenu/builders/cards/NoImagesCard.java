package net.osmand.plus.mapcontextmenu.builders.cards;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
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
			IconsCache ic = getMyApplication().getIconsCache();
			boolean night = getMyApplication().getDaynightHelper().isNightModeForMapControls();
			MapActivity ctx = getMapActivity();
			AndroidUtils.setBackgroundColor(ctx, view, night, R.color.bg_color_light, R.color.bg_color_dark);
			((ImageView) view.findViewById(R.id.icon_sadface)).setImageDrawable(ic.getIcon(R.drawable.ic_action_sadface,
					night ? R.color.color_white : R.color.icon_color));
			AndroidUtils.setTextPrimaryColor(ctx, (TextView) view.findViewById(R.id.title), night);
			AndroidUtils.setBackgroundColor(ctx, view.findViewById(R.id.button_background), night,
					R.color.ctx_menu_card_btn_light, R.color.ctx_menu_card_btn_dark);
			((ImageView) view.findViewById(R.id.icon_add_photos)).setImageDrawable(ic.getIcon(R.drawable.ic_action_add_photos,
					night ? R.color.color_dialog_buttons_dark : R.color.color_dialog_buttons_light));
			((TextView) view.findViewById(R.id.app_photos_text_view)).setTextColor(ContextCompat.getColor(ctx,
					night ? R.color.color_dialog_buttons_dark : R.color.color_dialog_buttons_light));
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
