package net.osmand.plus.mapcontextmenu.builders.cards;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;

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
			AndroidUtils.setBackgroundColor(ctx, view, ColorUtilities.getListBgColorId(night));
			Drawable smileDrawableSrc = getMyApplication().getDrawable(R.drawable.ic_action_sadface);
			if(smileDrawableSrc != null) {
			Bitmap srcBitmap = AndroidUtils.getBitmapFromDrawable(smileDrawableSrc);
				float scale = getMyApplication().getOsmandMap().getTextScale();
				Bitmap smileBitmap = AndroidUtils.scaleBitmap(srcBitmap,
						(int) (srcBitmap.getWidth() * scale), (int) (srcBitmap.getHeight() * scale), false);
				((ImageView) view.findViewById(R.id.icon_sadface)).setImageDrawable(ic.wrapDrawableWithColor(new BitmapDrawable(getMyApplication().getResources(), smileBitmap), R.drawable.ic_action_sadface,
						night ? R.color.card_and_list_background_light : R.color.icon_color_default_light));
			}
			AndroidUtils.setTextPrimaryColor(ctx, view.findViewById(R.id.title), night);
			AndroidUtils.setBackgroundColor(ctx, view.findViewById(R.id.button_background), night,
					R.color.inactive_buttons_and_links_bg_light, R.color.inactive_buttons_and_links_bg_dark);
			((ImageView) view.findViewById(R.id.icon_add_photos)).setImageDrawable(
					ic.getIcon(R.drawable.ic_action_add_photos, ColorUtilities.getActiveColorId(night)));
			((TextView) view.findViewById(R.id.app_photos_text_view))
					.setTextColor(ColorUtilities.getActiveColor(ctx, night));
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
