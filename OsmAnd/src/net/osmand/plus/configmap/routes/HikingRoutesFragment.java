package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.HIKING;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

public class HikingRoutesFragment extends MapRoutesFragment {

	public static final String TAG = HikingRoutesFragment.class.getSimpleName();

	private HikingRoutesCard hikingRoutesCard;

	@Override
	protected boolean isEnabled() {
		return routeLayersHelper.isHikingRoutesEnabled();
	}

	@Override
	protected void toggleMainPreference(@NonNull View view) {
		routeLayersHelper.toggleHikingRoutes();
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		boolean enabled = isEnabled();
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		title.setText(R.string.rendering_attr_hikingRoutesOSMC_name);

		TextView description = container.findViewById(R.id.description);
		description.setText(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.default_icon_color);
		ImageView icon = container.findViewById(R.id.icon);
		icon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_trekking_dark, enabled ? selectedColor : disabledColor));
	}

	@Override
	protected void createCards(@NonNull View view) {
		super.createCards(view);

		addCard(new HikingRoutesCard(getMapActivity()));
		addRenderingClassCard(HIKING.getRenderingPropertyAttr());
	}
}