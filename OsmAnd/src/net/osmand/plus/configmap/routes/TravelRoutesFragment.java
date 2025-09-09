package net.osmand.plus.configmap.routes;

import static net.osmand.IProgress.EMPTY_PROGRESS;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ROUTE_ACTIVITY_TYPE;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.render.TravelRendererHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TravelRoutesFragment extends BaseFullScreenFragment {

	public static final String TAG = TravelRoutesFragment.class.getSimpleName();
	private static final String TRAVEL_TYPE_KEY = "travel_type_key";

	private TravelRendererHelper rendererHelper;

	private List<String> routeTypes;
	private List<String> pointCategories;

	private TravelType travelType = TravelType.ROUTE_TYPES;

	private enum TravelType {
		ROUTE_TYPES(R.string.shared_string_tracks),
		TRAVEL_FILES(R.string.shared_string_travelbooks),
		ROUTE_POINTS(R.string.shared_string_gpx_points);

		@StringRes
		public int titleRes;

		TravelType(int titleRes) {
			this.titleRes = titleRes;
		}
	}

	private enum DescriptionType {
		HIDDEN,
		ENABLED_DISABLED,
		VISIBLE_HIDDEN
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		rendererHelper = app.getTravelRendererHelper();
		updateRouteTypes();
		updatePointCategories();

		if (savedInstanceState != null) {
			travelType = TravelType.valueOf(savedInstanceState.getString(TRAVEL_TYPE_KEY));
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(TRAVEL_TYPE_KEY, travelType.name());
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.travel_routes_fragment, container, false);

		showHideTopShadow(view);
		setupHeader(view);
		setupPrefItems(view);
		setupTypeRadioGroup(view);

		return view;
	}

	private void showHideTopShadow(@NonNull View view) {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);
	}

	private void updateRouteTypes() {
		List<String> routesTypes = app.getResourceManager().searchPoiSubTypesByPrefix(ROUTE_ACTIVITY_TYPE);
		Collections.sort(routesTypes, OsmAndCollator.primaryCollator()::compare);
		this.routeTypes = routesTypes;
	}

	private void updatePointCategories() {
		List<String> routesCategories = app.getResourceManager().searchPoiSubTypesByPrefix(MapPoiTypes.CATEGORY);
		Collections.sort(routesCategories, OsmAndCollator.primaryCollator()::compare);
		this.pointCategories = routesCategories;
	}

	private void setupHeader(@NonNull View view) {
		View itemView = view.findViewById(R.id.header);
		updateTypeItemView(itemView, getString(R.string.travel_routes), settings.SHOW_TRAVEL.get());
		itemView.setOnClickListener(v -> {
			boolean selected = !settings.SHOW_TRAVEL.get();
			settings.SHOW_TRAVEL.set(selected);
			setupHeader(view);
			setupPrefItems(view);
			setupTypeRadioGroup(view);
			app.runInUIThread(() -> {
				app.getOsmandMap().refreshMap(true);
				app.getOsmandMap().getMapLayers().updateLayers((MapActivity) getMyActivity());
			});
		});
	}

	private void updateItemView(View itemView, String name, @DrawableRes int imageId, boolean selected,
	                            DescriptionType descriptionType) {
		TextView title = itemView.findViewById(R.id.title);
		ImageView icon = itemView.findViewById(R.id.icon);
		TextView description = itemView.findViewById(R.id.description);

		ApplicationMode mode = settings.getApplicationMode();
		int selectedColor = mode.getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(itemView.getContext(), R.attr.default_icon_color);

		title.setText(name);
		icon.setImageDrawable(getPaintedIcon(imageId, selected ? selectedColor : disabledColor));
		if (description != null) {
			switch (descriptionType) {
				case HIDDEN:
					description.setVisibility(View.GONE);
					break;
				case ENABLED_DISABLED:
					description.setText(selected ? R.string.shared_string_enabled : R.string.shared_string_disabled);
					break;
				case VISIBLE_HIDDEN:
					description.setText(selected ? R.string.shared_string_visible : R.string.shared_string_hidden);
					break;
			}
		}

		CompoundButton button = itemView.findViewById(R.id.toggle_item);
		button.setClickable(false);
		button.setFocusable(false);
		button.setChecked(selected);
		UiUtilities.setupCompoundButton(nightMode, selectedColor, button);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.divider), false);
	}

	private void updateTypeItemView(View itemView, String name, boolean selected) {
		updateItemView(itemView, name, settings.getApplicationMode().getIconRes(), selected, DescriptionType.ENABLED_DISABLED);
	}

	private void updateCategoryItemView(View itemView, String name, boolean selected) {
		updateItemView(itemView, name, R.drawable.ic_action_categories_search, selected, DescriptionType.HIDDEN);
	}

	private void setupTypeRadioGroup(@NonNull View view) {
		boolean selected = settings.SHOW_TRAVEL.get();
		LinearLayout buttonsContainer = view.findViewById(R.id.custom_radio_buttons);
		if (selected) {
			TextRadioItem routes = createRadioButton(TravelType.ROUTE_TYPES);
			TextRadioItem files = createRadioButton(TravelType.TRAVEL_FILES);
			TextRadioItem points = createRadioButton(TravelType.ROUTE_POINTS);

			TextToggleButton radioGroup = new TextToggleButton(app, buttonsContainer, nightMode, true);
			radioGroup.setItems(routes, files, points);
			switch (travelType) {
				case ROUTE_TYPES:
					radioGroup.setSelectedItem(routes);
					break;
				case TRAVEL_FILES:
					radioGroup.setSelectedItem(files);
					break;
				case ROUTE_POINTS:
					radioGroup.setSelectedItem(points);
					break;
			}
		}
		AndroidUiHelper.updateVisibility(buttonsContainer, selected);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.space), selected);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.header_divider), selected);
	}

	private TextRadioItem createRadioButton(TravelType type) {
		TextRadioItem item = new TextRadioItem(app.getString(type.titleRes));
		item.setOnClickListener((radioItem, v) -> {
			travelType = type;
			View view = getView();
			if (view != null) {
				setupPrefItems(view);
			}
			return true;
		});
		return item;
	}

	private void setupPrefItems(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.items_container);
		container.removeAllViews();

		boolean selected = settings.SHOW_TRAVEL.get();
		if (selected) {
			switch (travelType) {
				case ROUTE_TYPES:
					setupRouteTypes(container);
					break;
				case TRAVEL_FILES:
					setupTravelFiles(container);
					break;
				case ROUTE_POINTS:
					setupRoutePoints(container);
					break;
			}
		}
		AndroidUiHelper.updateVisibility(container, selected);
	}

	private void setupTravelFiles(ViewGroup container) {
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);

		OsmandRegions regions = app.getRegions();
		List<String> travelRepositoryNames = app.getResourceManager().getTravelRepositoryNames();
		Collections.sort(travelRepositoryNames);
		for (String fileName : travelRepositoryNames) {
			CommonPreference<Boolean> pref = rendererHelper.getFileVisibilityProperty(fileName);

			String title = FileNameTranslationHelper.getFileName(app, regions, fileName);
			View itemView = inflater.inflate(R.layout.list_item_icon_and_menu, container, false);
			AndroidUtils.setBackground(itemView, UiUtilities.getSelectableDrawable(app));

			updateTypeItemView(itemView, title, pref.get());
			itemView.setOnClickListener(v -> {
				boolean selected = !pref.get();
				pref.set(selected);
				updateTypeItemView(itemView, title, pref.get());

				rendererHelper.updateFileVisibility(fileName, selected);
				rendererHelper.updateRouteArticlePointsFilter();
				updateRouteTypes();
				updatePointCategories();

				app.getResourceManager().reloadIndexesAsync(EMPTY_PROGRESS, warnings -> {
					app.getOsmandMap().refreshMap(true);
					app.getOsmandMap().getMapLayers().updateLayers((MapActivity) getMyActivity());
				});
			});
			container.addView(itemView);
		}
		if (travelRepositoryNames.isEmpty()) {
			TextViewEx emptyView = createEmptyView(container, R.string.shared_string_empty);
			container.addView(emptyView);
		}
	}

	private void setupRouteTypes(ViewGroup container) {
		MapPoiTypes poiTypes = app.getPoiTypes();
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);

		CommonPreference<Boolean> tracksPref = rendererHelper.getRouteTracksProperty();
		View tracksView = inflater.inflate(R.layout.list_item_icon_and_menu, container, false);
		AndroidUtils.setBackground(tracksView, UiUtilities.getSelectableDrawable(app));
		updateItemView(tracksView, getString(R.string.display_route_tracks), R.drawable.ic_action_track_16,
				tracksPref.get(), DescriptionType.VISIBLE_HIDDEN);
		tracksView.setOnClickListener(v -> {
			boolean selected = !tracksPref.get();
			tracksPref.set(selected);
			updateItemView(tracksView, getString(R.string.display_route_tracks), R.drawable.ic_action_track_16,
					selected, DescriptionType.VISIBLE_HIDDEN);
			app.runInUIThread(() -> {
				rendererHelper.updateRouteTrackFilters();
				rendererHelper.updateRouteTypesVisibility();
				app.getOsmandMap().refreshMap(true);
				app.getOsmandMap().getMapLayers().updateLayers((MapActivity) getMyActivity());
			});
		});
		container.addView(tracksView);

		CommonPreference<Boolean> tracksAsPoiPref = rendererHelper.getRouteTracksAsPoiProperty();
		View tracksAsPoiView = inflater.inflate(R.layout.list_item_icon_and_menu, container, false);
		AndroidUtils.setBackground(tracksAsPoiView, UiUtilities.getSelectableDrawable(app));
		updateItemView(tracksAsPoiView, getString(R.string.display_route_tracks_as_poi), R.drawable.ic_action_info_dark,
				tracksAsPoiPref.get(), DescriptionType.VISIBLE_HIDDEN);
		tracksAsPoiView.setOnClickListener(v -> {
			boolean selected = !tracksAsPoiPref.get();
			tracksAsPoiPref.set(selected);
			updateItemView(tracksAsPoiView, getString(R.string.display_route_tracks_as_poi), R.drawable.ic_action_info_dark,
					selected, DescriptionType.VISIBLE_HIDDEN);
			app.runInUIThread(() -> {
				rendererHelper.updateRouteTrackFilters();
				rendererHelper.updateRouteTypesVisibility();
				app.getOsmandMap().refreshMap(true);
				app.getOsmandMap().getMapLayers().updateLayers((MapActivity) getMyActivity());
			});
		});
		container.addView(tracksAsPoiView);

		container.addView(inflater.inflate(R.layout.divider, container, false));

		for (String type : routeTypes) {
			CommonPreference<Boolean> pref = rendererHelper.getRouteTypeProperty(type);
			View itemView = inflater.inflate(R.layout.list_item_icon_and_menu, container, false);
			AndroidUtils.setBackground(itemView, UiUtilities.getSelectableDrawable(app));
			String name;
			String attrName = type.replace(ROUTE_ACTIVITY_TYPE + "_", "");
			PoiType poiType = poiTypes.getTextPoiAdditionalByKey(type);
			if (poiType != null) {
				name = poiType.getTranslation();
			} else {
				name = Algorithms.capitalizeFirstLetterAndLowercase(attrName);
			}
			updateTypeItemView(itemView, name, pref.get());
			itemView.setOnClickListener(v -> {
				boolean selected = !pref.get();
				pref.set(selected);
				updateTypeItemView(itemView, name, selected);
				app.runInUIThread(() -> {
					RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
					rendererHelper.updateRouteTypeVisibility(storage, attrName, selected);
					app.getOsmandMap().refreshMap(true);
					app.getOsmandMap().getMapLayers().updateLayers((MapActivity) getMyActivity());
				});
			});
			container.addView(itemView);
		}
		if (routeTypes.isEmpty()) {
			TextViewEx emptyView = createEmptyView(container, R.string.shared_string_empty);
			container.addView(emptyView);
		}
	}

	private void setupRoutePoints(ViewGroup container) {
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);
		CommonPreference<Boolean> articlePointsPref = rendererHelper.getRouteArticlePointsProperty();
		View pointsView = inflater.inflate(R.layout.list_item_icon_and_menu, container, false);
		AndroidUtils.setBackground(pointsView, UiUtilities.getSelectableDrawable(app));
		updateItemView(pointsView, getString(R.string.poi), R.drawable.ic_action_info_dark,
				articlePointsPref.get(), DescriptionType.VISIBLE_HIDDEN);
		pointsView.setOnClickListener(v -> {
			boolean selected = !articlePointsPref.get();
			articlePointsPref.set(selected);
			rendererHelper.updateRouteArticlePointsFilter();
			updateItemView(pointsView, getString(R.string.poi), R.drawable.ic_action_info_dark,
					selected, DescriptionType.VISIBLE_HIDDEN);
			app.getOsmandMap().refreshMap(true);
		});
		container.addView(pointsView);

		CommonPreference<Boolean> articlesPref = rendererHelper.getRouteArticlesProperty();
		View articleView = inflater.inflate(R.layout.list_item_icon_and_menu, container, false);
		AndroidUtils.setBackground(articleView, UiUtilities.getSelectableDrawable(app));
		updateItemView(articleView, getString(R.string.shared_string_articles), R.drawable.ic_action_read_article,
				articlesPref.get(), DescriptionType.VISIBLE_HIDDEN);
		articleView.setOnClickListener(v -> {
			boolean selected = !articlesPref.get();
			articlesPref.set(selected);
			rendererHelper.updateRouteArticleFilter();
			updateItemView(articleView, getString(R.string.shared_string_articles), R.drawable.ic_action_read_article,
					selected, DescriptionType.VISIBLE_HIDDEN);
			app.getOsmandMap().refreshMap(true);
		});
		container.addView(articleView);

		if (pointCategories.isEmpty()) {
			return;
		}
		View categoriesHeaderView = inflater.inflate(R.layout.list_item_text_header, container, false);
		TextView title = categoriesHeaderView.findViewById(R.id.title);
		title.setText(R.string.included_categories);
		categoriesHeaderView.findViewById(R.id.divider_top).setVisibility(View.VISIBLE);
		container.addView(categoriesHeaderView);

		MapPoiTypes poiTypes = app.getPoiTypes();
		for (String category : pointCategories) {
			CommonPreference<Boolean> categoryPref = rendererHelper.getRoutePointCategoryProperty(category);
			View itemView = inflater.inflate(R.layout.list_item_icon_and_switch_small, container, false);
			AndroidUtils.setBackground(itemView, UiUtilities.getSelectableDrawable(app));
			String name;
			PoiType poiType = poiTypes.getTextPoiAdditionalByKey(category);
			if (poiType != null) {
				name = poiType.getTranslation();
			} else {
				name = Algorithms.capitalizeFirstLetterAndLowercase(category.replace(MapPoiTypes.CATEGORY + "_", ""));
			}
			updateCategoryItemView(itemView, name, categoryPref.get());
			itemView.setOnClickListener(v -> {
				boolean selected = !categoryPref.get();
				categoryPref.set(selected);
				rendererHelper.updateRouteArticlePointsFilter();
				updateCategoryItemView(itemView, name, selected);
				app.getOsmandMap().refreshMap(true);
			});
			container.addView(itemView);
		}
	}

	private TextViewEx createEmptyView(@NonNull ViewGroup container, @StringRes int titleId) {
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);
		TextViewEx emptyView = (TextViewEx) inflater.inflate(R.layout.bottom_sheet_item_title, container, false);
		emptyView.setGravity(Gravity.CENTER);
		emptyView.setText(titleId);
		return emptyView;
	}

	@Nullable
	public Set<InsetSide> getRootInsetSides() {
		return null;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new TravelRoutesFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}