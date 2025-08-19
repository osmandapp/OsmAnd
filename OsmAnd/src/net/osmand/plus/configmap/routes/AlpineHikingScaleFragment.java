package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.ALPINE;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

public class AlpineHikingScaleFragment extends MapRoutesFragment {

	public static final String TAG = AlpineHikingScaleFragment.class.getSimpleName();

	@Override
	protected boolean isEnabled() {
		return routeLayersHelper.isAlpineHikingRoutesEnabled();
	}

	@Override
	protected void toggleMainPreference(@NonNull View view) {
		routeLayersHelper.toggleAlpineHikingRoutes();
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		boolean enabled = isEnabled();
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		title.setText(R.string.rendering_attr_alpineHiking_name);

		TextView description = container.findViewById(R.id.description);
		description.setText(routeLayersHelper.getRoutesTypeDescription(ALPINE.getRenderingPropertyAttr()));
		AndroidUiHelper.updateVisibility(description, enabled);

		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(app, R.attr.default_icon_color);
		ImageView icon = container.findViewById(R.id.icon);
		icon.setImageDrawable(getPaintedIcon(RouteUtils.getIconIdForAttr(
				ALPINE.getRenderingPropertyAttr()), enabled ? selectedColor : disabledColor));
	}

	@Override
	protected void createCards(@NonNull View view) {
		super.createCards(view);

		addCard(new AlpineHikingCard(getMapActivity()));
		addRenderingClassCard(ALPINE.getRenderingPropertyAttr());
	}
}
