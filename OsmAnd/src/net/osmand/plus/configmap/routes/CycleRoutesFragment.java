package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.BICYCLE;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class CycleRoutesFragment extends MapRoutesFragment {

	public static final String TAG = CycleRoutesFragment.class.getSimpleName();

	@Override
	protected boolean isEnabled() {
		return routeLayersHelper.isCycleRoutesEnabled();
	}

	@Override
	protected void toggleMainPreference(@NonNull View view) {
		routeLayersHelper.toggleCycleRoutes();
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		boolean enabled = isEnabled();
		View container = view.findViewById(R.id.preference_container);
		String propertyAttr = BICYCLE.getRenderingPropertyAttr();

		TextView title = container.findViewById(R.id.title);
		title.setText(AndroidUtils.getRenderingStringPropertyName(app, propertyAttr, propertyAttr));

		TextView description = container.findViewById(R.id.description);
		description.setText(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		int appModeColor = ColorUtilities.getAppModeColor(app, nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.default_icon_color);
		ImageView icon = container.findViewById(R.id.icon);
		icon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_bicycle_dark, enabled ? appModeColor : disabledColor));
	}

	@Override
	protected void createCards(@NonNull View view) {
		super.createCards(view);

		addCard(new CycleRouteTypesCard(getMapActivity()));
		addRenderingClassCard(BICYCLE.getRenderingPropertyAttr());
	}
}