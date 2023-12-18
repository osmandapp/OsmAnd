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
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class RouteInfoCard extends MapBaseCard {

	private final RouteKey routeKey;
	private final GPXFile gpxFile;

	public RouteInfoCard(
			@NonNull MapActivity activity,
			@NonNull RouteKey routeKey,
			@NonNull GPXFile gpxFile
	) {
		super(activity);
		this.routeKey = routeKey;
		this.gpxFile = gpxFile;
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
		String tag = routeKey.type.getName();

		String networkTag = routeKey.getNetwork();
		if (!Algorithms.isEmpty(networkTag)) {
			String network = getStringByProperty(app, "poi_route_" + tag + "_" + networkTag + "_poi");
			addInfoRow(container, network != null ? network : networkTag, app.getString(R.string.poi_network));
		}
		String routeType = getActivityTypeStringPropertyName(app, tag, capitalizeFirstLetterAndLowercase(tag));
		addInfoRow(container, routeType, app.getString(R.string.layer_route));
		addInfoRow(container, routeKey.getOperator(), app.getString(R.string.poi_operator));
		addInfoRow(container, routeKey.getSymbol(), app.getString(R.string.shared_string_symbol));
		addInfoRow(container, routeKey.getWebsite(), app.getString(R.string.website), true);
		addWikiInfoRow(container, routeKey.getWikipedia());
	}

	private void addWikiInfoRow(@NonNull ViewGroup container, @NonNull String text) {
		if (Algorithms.isEmpty(text)) {
			return;
		}
		String url = WikiAlgorithms.getWikiUrl(text);
		String description = app.getString(R.string.shared_string_wikipedia);
		View view = addInfoRow(container, url, description);

		OsmAndAppCustomization customization = app.getAppCustomization();
		if (Algorithms.isUrl(url) && view != null && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
			TextView tvContent = view.findViewById(R.id.title);
			tvContent.setTextColor(ColorUtilities.getActiveColor(app, nightMode));
			view.setOnClickListener(v -> {
				WikiArticleHelper.askShowArticle(activity, nightMode, collectTrackPoints(), url);
			});
		}
	}

	@Nullable
	private View addInfoRow(@NonNull ViewGroup container, @NonNull String text, @NonNull String description) {
		return addInfoRow(container, text, description, false);
	}

	@Nullable
	private View addInfoRow(@NonNull ViewGroup container, @NonNull String text, @NonNull String description, boolean needLinks) {
		if (Algorithms.isEmpty(text)) {
			return null;
		}
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);
		View view = inflater.inflate(R.layout.list_item_with_descr, container, false);

		TextView tvLabel = view.findViewById(R.id.description);
		TextView tvContent = view.findViewById(R.id.title);

		tvContent.setText(text);
		tvLabel.setText(description);

		OsmAndAppCustomization customization = app.getAppCustomization();
		if (needLinks && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID) && Linkify.addLinks(tvContent, Linkify.ALL)) {
			tvContent.setMovementMethod(null);
			tvContent.setLinkTextColor(ColorUtilities.getActiveColor(app, nightMode));
			tvContent.setOnTouchListener(new ClickableSpanTouchListener());
			AndroidUtils.removeLinkUnderline(tvContent);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), container.getChildCount() > 0);
		container.addView(view);
		return view;
	}

	private List<LatLon> collectTrackPoints() {
		List<LatLon> points = new ArrayList<>();
		if (gpxFile != null) {
			for (WptPt wptPt : gpxFile.getAllPoints()) {
				points.add(new LatLon(wptPt.lat, wptPt.lon));
			}
		}
		return points;
	}
}
