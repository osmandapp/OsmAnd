package net.osmand.plus.track.cards;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;
import static net.osmand.plus.utils.AndroidUtils.getActivityTypeStringPropertyName;
import static net.osmand.plus.utils.AndroidUtils.getStringByProperty;
import static net.osmand.util.Algorithms.capitalizeFirstLetterAndLowercase;

import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.Algorithms;

public class RouteInfoCard extends MapBaseCard {

	private final RouteKey routeKey;

	public RouteInfoCard(@NonNull MapActivity mapActivity, @NonNull RouteKey routeKey) {
		super(mapActivity);
		this.routeKey = routeKey;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_route_info_card;
	}

	@Override
	public void updateContent() {
		LinearLayout container = view.findViewById(R.id.items_container);
		container.removeAllViews();

		RouteKey routeKey = this.routeKey;
		String tag = routeKey.type.getTag();

		String networkTag = routeKey.getValue("network");
		if (!Algorithms.isEmpty(networkTag)) {
			String network = getStringByProperty(app, "poi_route_" + tag + "_" + networkTag + "_poi");
			addInfoRow(container, network != null ? network : networkTag, app.getString(R.string.poi_network));
		}
		String routeType = getActivityTypeStringPropertyName(app, tag, capitalizeFirstLetterAndLowercase(tag));
		addInfoRow(container, routeType, app.getString(R.string.layer_route));

		String operatorTag = routeKey.getValue("operator");
		if (!Algorithms.isEmpty(operatorTag)) {
			addInfoRow(container, operatorTag, app.getString(R.string.poi_operator));
		}
		String symbolTag = routeKey.getValue("symbol");
		if (!Algorithms.isEmpty(symbolTag)) {
			addInfoRow(container, symbolTag, app.getString(R.string.shared_string_symbol));
		}
		String websiteTag = routeKey.getValue("website");
		if (!Algorithms.isEmpty(websiteTag)) {
			addInfoRow(container, websiteTag, app.getString(R.string.website), true);
		}
		String wikipediaTag = routeKey.getValue("wikipedia");
		if (!Algorithms.isEmpty(wikipediaTag)) {
			addInfoRow(container, wikipediaTag, app.getString(R.string.shared_string_wikipedia));
		}
	}

	private void addInfoRow(@NonNull ViewGroup container, @NonNull String text, @NonNull String description) {
		addInfoRow(container, text, description, false);
	}

	private void addInfoRow(@NonNull ViewGroup container, @NonNull String text, @NonNull String description, boolean needLinks) {
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);
		View view = inflater.inflate(R.layout.list_item_with_descr, container, false);

		TextView titleTv = view.findViewById(R.id.title);
		TextView descriptionTv = view.findViewById(R.id.description);

		titleTv.setText(text);
		descriptionTv.setText(description);

		OsmAndAppCustomization customization = app.getAppCustomization();
		if (needLinks && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID) && Linkify.addLinks(titleTv, Linkify.ALL)) {
			titleTv.setMovementMethod(null);
			titleTv.setLinkTextColor(ColorUtilities.getActiveColor(app, nightMode));
			titleTv.setOnTouchListener(new ClickableSpanTouchListener());
			AndroidUtils.removeLinkUnderline(titleTv);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), container.getChildCount() > 0);
		container.addView(view);
	}
}
