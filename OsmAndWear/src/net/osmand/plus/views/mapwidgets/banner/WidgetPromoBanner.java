package net.osmand.plus.views.mapwidgets.banner;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.WidgetType;

public class WidgetPromoBanner extends MapBaseCard {

	private final WidgetType widget;

	public WidgetPromoBanner(@NonNull MapActivity mapActivity,
	                         @NonNull WidgetType widget,
	                         boolean usedOnMap) {
		super(mapActivity, usedOnMap);
		this.widget = widget;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_widget_promo_banner;
	}

	@Override
	protected void updateContent() {
		TextView tvTitle = view.findViewById(R.id.widget_title);
		TextView tvDesc = view.findViewById(R.id.description);
		ImageView ivWidgetIcon = view.findViewById(R.id.widget_icon);
		ImageView ivPlanIcon = view.findViewById(R.id.subscription_plan_icon);

		tvTitle.setText(widget.titleId);
		ivWidgetIcon.setImageResource(widget.getIconId(nightMode));

		String subscription = app.getString(R.string.osmand_pro);
		String description = app.getString(R.string.available_as_part_of_subscription_plan, subscription);
		tvDesc.setText(description);
		ivPlanIcon.setImageResource(getProIconId());

		View container = view.findViewById(R.id.banner_container);
		int resId = nightMode ? R.drawable.ripple_solid_dark_6dp : R.drawable.ripple_solid_light_6dp;
		Drawable selected = AppCompatResources.getDrawable(app, resId);
		AndroidUtils.setBackground(container, selected);

		View shield = view.findViewById(R.id.get_button);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int color = ColorUtilities.getColorWithAlpha(activeColor, 0.3f);
		Drawable shieldBg = AppCompatResources.getDrawable(app, R.drawable.btn_background_inactive_light);
		shieldBg = UiUtilities.tintDrawable(shieldBg, color);
		AndroidUtils.setBackground(shield, shieldBg);

		container.setOnClickListener(v -> {
			OsmAndFeature feature = widget.isOBDWidget()
					? OsmAndFeature.VEHICLE_METRICS
					: OsmAndFeature.ADVANCED_WIDGETS;
			ChoosePlanFragment.showInstance(mapActivity, feature);
		});
	}

	@DrawableRes
	private int getProIconId() {
		return nightMode ?
				R.drawable.ic_action_osmand_pro_logo_colored_night :
				R.drawable.ic_action_osmand_pro_logo_colored;
	}
}
