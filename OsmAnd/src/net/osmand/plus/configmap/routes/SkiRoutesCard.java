package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.SKI;
import static net.osmand.plus.render.RendererRegistry.WINTER_SKI_RENDER;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.render.RenderingRulesStorage;

public class SkiRoutesCard extends MapBaseCard {

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

		TextView mapStyleTitle = mapStyleButton.findViewById(R.id.title);
		mapStyleTitle.setText(R.string.map_widget_renderer);

		TextView mapStyleDescription = mapStyleButton.findViewById(R.id.description);
		mapStyleDescription.setText(ConfigureMapUtils.getRenderDescr(app));

		ImageView mapStyleIcon = mapStyleButton.findViewById(R.id.icon);
		mapStyleIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_map_style, settings.getApplicationMode().getProfileColor(nightMode)));

		mapStyleButton.setOnClickListener(v -> {
			SelectMapStyleBottomSheetDialogFragment.showInstance(activity.getSupportFragmentManager());
		});

		mapStyleButton.findViewById(R.id.divider).setVisibility(View.GONE);
		mapStyleButton.findViewById(R.id.toggle_item).setVisibility(View.GONE);

		View switchBanner = view.findViewById(R.id.switch_banner);
		if (!hideSwitchBanner) {
			AndroidUiHelper.updateVisibility(switchBanner, true);
			setupSwitchBanner();
		} else {
			AndroidUiHelper.updateVisibility(switchBanner, false);
		}
	}

	private void setupSwitchBanner() {
		LinearLayout bottomButtons = view.findViewById(R.id.buttons_container);
		bottomButtons.setPadding(0, 0, 0, 0);

		View buttonsDivider = bottomButtons.findViewById(R.id.buttons_divider);
		buttonsDivider.setLayoutParams(new LayoutParams(AndroidUtils.dpToPx(app, 12), LayoutParams.MATCH_PARENT));
		buttonsDivider.setVisibility(View.VISIBLE);

		DialogButton switchButton = view.findViewById(R.id.dismiss_button);
		switchButton.setButtonType(DialogButtonType.SECONDARY);
		switchButton.setTitleId(R.string.shared_string_switch);
		switchButton.setOnClickListener(view -> {
			switchToWinterSkiStyle();
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
	}

	private void switchToWinterSkiStyle() {
		OsmandApplication app = getMyApplication();
		RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(WINTER_SKI_RENDER);
		if (loaded != null) {
			OsmandMapTileView view = mapActivity.getMapView();
			OsmandSettings settings = view.getSettings();
			settings.RENDERER.set(WINTER_SKI_RENDER);
			CommonPreference<Boolean> pisteRoutesPref = settings.getCustomRenderBooleanProperty(SKI.getRenderingPropertyAttr());
			if (pisteRoutesPref.get()) {
				pisteRoutesPref.set(settings.RENDERER.get().equals(WINTER_SKI_RENDER));
			}
			app.getRendererRegistry().setCurrentSelectedRender(loaded);
			mapActivity.refreshMapComplete();
			DashboardOnMap dashboard = mapActivity.getDashboard();
			dashboard.refreshContent(false);
		} else {
			app.showShortToastMessage(R.string.renderer_load_exception);
		}
	}
}
