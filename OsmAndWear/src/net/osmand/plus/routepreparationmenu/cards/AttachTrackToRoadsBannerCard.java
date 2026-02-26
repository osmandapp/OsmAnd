package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;

public class AttachTrackToRoadsBannerCard extends MapBaseCard {

	public AttachTrackToRoadsBannerCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_attach_to_roads_banner;
	}

	@Override
	protected void updateContent() {
		View button = view.findViewById(R.id.button_container);

		int resId = nightMode ? R.drawable.ripple_solid_dark_6dp : R.drawable.ripple_solid_light_6dp;
		Drawable selected = AppCompatResources.getDrawable(app, resId);
		AndroidUtils.setBackground(button, selected);

		button.setOnClickListener(v -> {
			notifyButtonPressed(0);
		});
	}

}
