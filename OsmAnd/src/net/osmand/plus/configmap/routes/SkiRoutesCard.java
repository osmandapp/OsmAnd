package net.osmand.plus.configmap.routes;

import static net.osmand.plus.render.RendererRegistry.WINTER_SKI_RENDER;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment.SelectStyleListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class SkiRoutesCard extends MapBaseCard implements SelectStyleListener {

	private static boolean hideSwitchBanner = false;

	public SkiRoutesCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.ski_route_card;
	}

	@Override
	protected void updateContent() {
		View mapStyleButton = view.findViewById(R.id.map_style_button);
		int profileColor = appMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f);
		AndroidUtils.setBackground(mapStyleButton, background);

		TextView mapStyleTitle = mapStyleButton.findViewById(R.id.title);
		mapStyleTitle.setText(R.string.map_widget_renderer);

		TextView mapStyleDescription = mapStyleButton.findViewById(R.id.description);
		mapStyleDescription.setText(ConfigureMapUtils.getRenderDescr(app));

		ImageView mapStyleIcon = mapStyleButton.findViewById(R.id.icon);
		mapStyleIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_map_style, profileColor));

		mapStyleButton.setOnClickListener(v -> {
			SelectMapStyleBottomSheetDialogFragment.showInstance(activity.getSupportFragmentManager(), this);
		});

		mapStyleButton.findViewById(R.id.divider).setVisibility(View.GONE);
		mapStyleButton.findViewById(R.id.toggle_item).setVisibility(View.GONE);

		setupSwitchBanner();
	}

	private void setupSwitchBanner() {
		View switchBanner = view.findViewById(R.id.switch_banner);

		boolean showBanner = !hideSwitchBanner && !app.getSettings().RENDERER.get().equals(RendererRegistry.WINTER_SKI_RENDER);
		AndroidUiHelper.updateVisibility(switchBanner, showBanner);
		if (!showBanner) {
			return;
		}

		LinearLayout bottomButtons = view.findViewById(R.id.bottom_buttons_container);
		bottomButtons.setPadding(0, 0, 0, 0);

		View buttonsDivider = bottomButtons.findViewById(R.id.buttons_divider);
		buttonsDivider.setLayoutParams(new LayoutParams(AndroidUtils.dpToPx(app, 12), LayoutParams.MATCH_PARENT));
		buttonsDivider.setVisibility(View.VISIBLE);

		DialogButton switchButton = view.findViewById(R.id.dismiss_button);
		switchButton.setButtonType(DialogButtonType.SECONDARY);
		switchButton.setTitleId(R.string.shared_string_switch);
		switchButton.setOnClickListener(view -> {
			switchToWinterSkiStyle();
			notifyCardPressed();
		});
		AndroidUiHelper.updateVisibility(switchButton, true);

		DialogButton laterButton = view.findViewById(R.id.right_bottom_button);
		laterButton.setButtonType(DialogButtonType.STROKED);
		laterButton.setTitleId(R.string.later);
		laterButton.setOnClickListener(view -> {
			hideSwitchBanner = true;
			updateContent();
		});
		AndroidUiHelper.updateVisibility(laterButton, true);

		ImageView bannerIcon = view.findViewById(R.id.banner_icon);
		Drawable icon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_skiing, ColorUtilities.getDefaultIconColor(mapActivity, nightMode));
		bannerIcon.setImageDrawable(icon);
	}

	private void switchToWinterSkiStyle() {
		SelectMapStyleBottomSheetDialogFragment.setStyle(mapActivity, WINTER_SKI_RENDER);
	}

	@Override
	public void onMapStyleSelected() {
		notifyCardPressed();
	}
}
