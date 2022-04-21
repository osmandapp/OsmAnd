package net.osmand.plus.track.cards;

import static net.osmand.plus.mapcontextmenu.controllers.NetworkRouteMenuController.getBackgroundIconIdWithName;
import static net.osmand.plus.mapcontextmenu.controllers.NetworkRouteMenuController.getForegroundIconIdWithName;
import static net.osmand.plus.utils.AndroidUtils.getActivityTypeStringPropertyName;
import static net.osmand.plus.utils.AndroidUtils.getStringByProperty;
import static net.osmand.util.Algorithms.capitalizeFirstLetterAndLowercase;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.router.network.NetworkRouteSelector.RouteType;
import net.osmand.util.Algorithms;

import java.util.List;

public class RouteInfoCard extends MapBaseCard {

	private final RenderedObject renderedObject;

	public RouteInfoCard(@NonNull MapActivity mapActivity, @NonNull RenderedObject renderedObject) {
		super(mapActivity);
		this.renderedObject = renderedObject;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_route_info_card;
	}

	@Override
	public void updateContent() {
		LinearLayout container = view.findViewById(R.id.items_container);
		container.removeAllViews();

		List<RouteKey> routeKeys = RouteType.getRouteStringKeys(renderedObject);
		if (!Algorithms.isEmpty(routeKeys)) {
			RouteKey routeKey = routeKeys.get(0);
			String tag = routeKey.type.getTag();

			String networkTag = renderedObject.getTagValue(tag + "_network");
			if (!Algorithms.isEmpty(networkTag)) {
				String network = getStringByProperty(app, "poi_route_" + tag + "_" + networkTag + "_poi");
				addInfoRow(container, network != null ? network : networkTag, app.getString(R.string.poi_network));
			}
			String routeType = getActivityTypeStringPropertyName(app, tag, capitalizeFirstLetterAndLowercase(tag));
			addInfoRow(container, routeType, app.getString(R.string.layer_route));

			String operatorTag = renderedObject.getTagValue(tag + "_route_operator");
			if (!Algorithms.isEmpty(operatorTag)) {
				addInfoRow(container, operatorTag, app.getString(R.string.poi_operator));
			}
//			addSymbolRow(container, routeKey);
		}
	}

	private void addSymbolRow(@NonNull LinearLayout container, @NonNull RouteKey routeKey) {
		String foregroundName = null;
		String backgroundName = null;

		Pair<String, Integer> foreground = getForegroundIconIdWithName(app, routeKey);
		Pair<String, Integer> background = getBackgroundIconIdWithName(app, routeKey);
		if (foreground != null) {
			foregroundName = foreground.first.replace('_', ' ');
		}
		if (background != null) {
			backgroundName = background.first.replace('_', ' ');
		}

		String symbolName;
		if (backgroundName != null && foregroundName != null) {
			symbolName = app.getString(R.string.on_with_params, foregroundName, backgroundName);
		} else {
			symbolName = foregroundName != null ? foregroundName : backgroundName;
		}
		if (!Algorithms.isEmpty(symbolName)) {
			addInfoRow(container, capitalizeFirstLetterAndLowercase(symbolName), app.getString(R.string.shared_string_symbol));
		}
	}

	private void addInfoRow(@NonNull ViewGroup container, @NonNull String text, @NonNull String description) {
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);
		View view = inflater.inflate(R.layout.list_item_with_descr, container, false);

		TextView titleTv = view.findViewById(R.id.title);
		TextView descriptionTv = view.findViewById(R.id.description);
		titleTv.setText(text);
		descriptionTv.setText(description);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), container.getChildCount() > 0);

		container.addView(view);
	}
}
