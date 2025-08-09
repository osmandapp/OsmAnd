package net.osmand.plus.configmap.routes;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

public class CustomRoutesFragment extends MapRoutesFragment {

	public static final String TAG = CustomRoutesFragment.class.getSimpleName();

	private String attrName;

	@Override
	protected boolean isEnabled() {
		return routeLayersHelper.isRoutesTypeEnabled(attrName);
	}

	@Override
	protected void toggleMainPreference(@NonNull View view) {
		routeLayersHelper.toggleRoutesType(attrName);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		attrName = app.getRouteLayersHelper().getSelectedAttrName();
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		boolean enabled = isEnabled();
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		title.setText(routeLayersHelper.getRoutesTypeName(routeLayersHelper.getSelectedAttrName()));

		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(app, R.attr.default_icon_color);
		ImageView icon = container.findViewById(R.id.icon);
		int iconId = RouteUtils.getIconIdForAttr(attrName);
		if (iconId > 0) {
			icon.setImageDrawable(getPaintedIcon(iconId, enabled ? selectedColor : disabledColor));
		}

		AndroidUiHelper.updateVisibility(container.findViewById(R.id.description), false);
	}

	@Override
	protected void createCards(@NonNull View view) {
		super.createCards(view);

		addRenderingClassCard(attrName);
	}
}
