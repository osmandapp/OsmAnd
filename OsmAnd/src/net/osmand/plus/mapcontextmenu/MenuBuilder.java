package net.osmand.plus.mapcontextmenu;

import static android.graphics.Typeface.DEFAULT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_ONLINE_PHOTOS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_PHONE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_SEARCH_MORE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_SHOW_ON_MAP_ID;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.DIVIDER_ROW_KEY;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_POI_KEY;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_WIKI_KEY;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.WITHIN_POLYGONS_ROW_KEY;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;
import net.osmand.plus.mapcontextmenu.builders.cards.CardsRowBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.NoImagesCard;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.mapcontextmenu.gallery.GalleryController;
import net.osmand.plus.mapcontextmenu.gallery.ImageCardsHolder;
import net.osmand.plus.mapcontextmenu.gallery.tasks.GetImageCardsTask;
import net.osmand.plus.mapcontextmenu.gallery.tasks.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.mapcontextmenu.other.MenuObject;
import net.osmand.plus.mapcontextmenu.other.MenuObjectUtils;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.dialogs.QuickSearchToolbarController;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.views.layers.TransportStopsLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.*;

public class MenuBuilder {

	private static final Log LOG = PlatformUtil.getLog(MenuBuilder.class);
	public static final float SHADOW_HEIGHT_TOP_DP = 17f;
	public static final int TITLE_LIMIT = 60;

	protected static final String[] arrowChars = {"=>", " - "};

	private static final int NEARBY_MAX_POI_COUNT = 10;
	private static final int NEARBY_POI_MIN_RADIUS = 250;
	private static final int NEARBY_POI_MAX_RADIUS = 1000;
	private static final int NEARBY_POI_SEARCH_FACTOR = 2;

	protected OsmandApplication app;
	protected MapActivity mapActivity;
	protected MapContextMenu mapContextMenu;
	protected OsmAndAppCustomization customization;
	protected MenuRowBuilder menuRowBuilder;

	protected LinkedList<PlainMenuItem> plainMenuItems;
	protected boolean firstRow;
	protected boolean matchWidthDivider;
	private Amenity amenity;
	private LatLon latLon;
	private boolean hidden;
	private boolean showTitleIfTruncated = true;
	private boolean showNearestWiki;
	private boolean showNearestPoi;
	private boolean showOnlinePhotos = true;

	private final List<OsmandPlugin> menuPlugins = new ArrayList<>();

	private GalleryController galleryController;
	@Nullable
	private CardsRowBuilder onlinePhotoCardsRow;
	private List<AbstractCard> onlinePhotoCards;

	private CollapseExpandListener collapseExpandListener;

	private final String preferredMapLang;
	private String preferredMapAppLang;
	private final boolean transliterateNames;
	private final GetImageCardsListener imageCardListener = new GetImageCardsListener() {
		@Override
		public void onTaskStarted() {
			if (!isHidden()) {
				onLoadingImages(true);
				PluginsHelper.onGetImageCardsStart();
			}
		}

		@Override
		public void onFinish(ImageCardsHolder cardsHolder) {
			if (!isHidden()) {
				onLoadingImages(false);
				if (galleryController != null) {
					galleryController.setCurrentCardsHolder(cardsHolder);
				}
				setOnlinePhotosCards(cardsHolder.getOrderedCards());
				PluginsHelper.onGetImageCardsFinished(cardsHolder);
			}
		}
	};

	private void setOnlinePhotosCards(List<ImageCard> onlinePhotosCards) {
		List<AbstractCard> cards = new ArrayList<>(onlinePhotosCards);
		if (onlinePhotosCards.isEmpty() && mapActivity != null) {
			cards.add(new NoImagesCard(mapActivity));
		}
		if (onlinePhotoCardsRow != null) {
			onlinePhotoCardsRow.setCards(cards);
		}
		onlinePhotoCards = cards;
	}

	private void onLoadingImages(boolean loading) {
		if (onlinePhotoCardsRow != null) {
			onlinePhotoCardsRow.onLoadingImage(loading);
		}
	}

	public interface CollapseExpandListener {
		void onCollapseExpand(boolean collapsed);
	}

	public MenuBuilder(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.customization = app.getAppCustomization();
		this.menuRowBuilder = new MenuRowBuilder(mapActivity);
		this.plainMenuItems = new LinkedList<>();
		this.galleryController = (GalleryController) app.getDialogManager().findController(GalleryController.PROCESS_ID);

		preferredMapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		preferredMapAppLang = preferredMapLang;
		if (Algorithms.isEmpty(preferredMapAppLang)) {
			preferredMapAppLang = app.getLanguage();
		}
		transliterateNames = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
	}

	public void notifyCollapseExpand(boolean collapsed) {
		if (collapseExpandListener != null) {
			collapseExpandListener.onCollapseExpand(collapsed);
		}
	}

	public CollapseExpandListener getCollapseExpandListener() {
		return collapseExpandListener;
	}

	public void setCollapseExpandListener(CollapseExpandListener collapseExpandListener) {
		this.collapseExpandListener = collapseExpandListener;
	}

	public String getPreferredMapLang() {
		return preferredMapLang;
	}

	public String getPreferredMapAppLang() {
		return preferredMapAppLang;
	}

	public boolean isTransliterateNames() {
		return transliterateNames;
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public OsmandApplication getApplication() {
		return app;
	}

	public MapContextMenu getMapContextMenu() {
		return mapContextMenu;
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public void setLatLon(LatLon objectLocation) {
		this.latLon = objectLocation;
	}

	public void setMapContextMenu(MapContextMenu mapContextMenu) {
		this.mapContextMenu = mapContextMenu;
	}

	public boolean isShowNearestWiki() {
		return showNearestWiki;
	}

	public boolean isShowNearestPoi() {
		return showNearestPoi;
	}

	public void setShowNearestWiki(boolean showNearestWiki) {
		this.showNearestWiki = showNearestWiki;
	}

	public void setShowNearestPoi(boolean showNearestPoi) {
		this.showNearestPoi = showNearestPoi;
	}

	public void setShowTitleIfTruncated(boolean showTitleIfTruncated) {
		this.showTitleIfTruncated = showTitleIfTruncated;
	}

	public boolean isShowOnlinePhotos() {
		return showOnlinePhotos;
	}

	public void setShowOnlinePhotos(boolean showOnlinePhotos) {
		this.showOnlinePhotos = showOnlinePhotos;
	}

	public void setAmenity(Amenity amenity) {
		this.amenity = amenity;
	}

	public void addMenuPlugin(@NonNull OsmandPlugin plugin) {
		if (!menuPlugins.contains(plugin)) {
			menuPlugins.add(plugin);
		}
	}

	public void setLight(boolean light) {
		menuRowBuilder.setLightContent(light);
	}

	public void build(@NonNull ViewGroup view, @Nullable Object object) {
		firstRow = true;
		hidden = false;
		buildTopInternal(view);
		if (showTitleIfTruncated) {
			buildTitleRow(view);
		}
		buildWithinRow(view);
		buildNearestWikiRow(view);
		buildNearestPoiRow(view);
		if (needBuildPlainMenuItems()) {
			buildPlainMenuItems(view);
		}
		buildInternal(view);
		buildPluginRows(view, object);

		if (needBuildCoordinatesRow()) {
			buildCoordinatesRow(view);
		}
		galleryController = (GalleryController) app.getDialogManager().findController(GalleryController.PROCESS_ID);
		if (customization.isFeatureEnabled(CONTEXT_MENU_ONLINE_PHOTOS_ID) && showOnlinePhotos && galleryController != null) {
			buildNearestPhotosRow(view);
			buildPluginGalleryRows(view, object);
		}

		startLoadingImages();
	}

	private boolean showTransportRoutes() {
		return showLocalTransportRoutes() || showNearbyTransportRoutes();
	}

	private boolean showLocalTransportRoutes() {
		List<TransportStopRoute> localTransportRoutes = mapContextMenu.getLocalTransportStopRoutes();
		return localTransportRoutes != null && localTransportRoutes.size() > 0;
	}

	private boolean showNearbyTransportRoutes() {
		List<TransportStopRoute> nearbyTransportRoutes = mapContextMenu.getNearbyTransportStopRoutes();
		return nearbyTransportRoutes != null && nearbyTransportRoutes.size() > 0;
	}

	void onHide() {
		hidden = true;
	}

	void onClose() {
		onlinePhotoCardsRow = null;
		onlinePhotoCards = null;
		if (galleryController != null) {
			galleryController.clearHolder();
		}
		clearPluginRows();
	}

	public boolean isHidden() {
		return hidden;
	}

	protected void buildPlainMenuItems(View view) {
		for (PlainMenuItem item : plainMenuItems) {
			buildRow(view, item.getIconId(), item.getButtonText(), item.getText(), 0, item.isCollapsable(), item.getCollapsableView(),
					item.isNeedLinks(), 0, item.isUrl(), item.getOnClickListener(), false);
		}
	}

	protected boolean needBuildPlainMenuItems() {
		return true;
	}

	protected boolean needBuildCoordinatesRow() {
		return true;
	}

	protected void buildPluginRows(@NonNull View view, @Nullable Object object) {
		for (OsmandPlugin plugin : menuPlugins) {
			plugin.buildContextMenuRows(this, view, object);
		}
	}

	protected void buildPluginGalleryRows(@NonNull View view, @Nullable Object object) {
		for (OsmandPlugin plugin : menuPlugins) {
			plugin.buildContextMenuGalleryRows(this, view, object);
		}
	}

	protected void clearPluginRows() {
		for (OsmandPlugin plugin : menuPlugins) {
			plugin.clearContextMenuRows();
		}
	}

	public void buildTitleRow(View view) {
		if (mapContextMenu != null) {
			String title = mapContextMenu.getTitleStr();
			if (title.length() > TITLE_LIMIT) {
				buildRow(view, R.drawable.ic_action_note_dark, null, title, 0, false, null, false, 0, false, null, false);
			}
		}
	}

	protected void buildWithinRow(ViewGroup viewGroup) {
		MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
		MapRendererView rendererView = app.getOsmandMap().getMapView().getMapRenderer();
		if (mapContext != null && rendererView != null) {
			ZoomLevel zoom = rendererView.getZoomLevel();
			PointI pointI = NativeUtilities.getPoint31FromLatLon(getLatLon());
			List<RenderedObject> polygons = mapContext.retrievePolygonsAroundMapObject(pointI, mapContextMenu.getObject(), zoom);
			if (!Algorithms.isEmpty(polygons)) {
				List<MenuObject> menuObjects = MenuObjectUtils.createMenuObjectsList(mapActivity, polygons, getLatLon());
				Context context = viewGroup.getContext();
				View rowContainer = createRowContainer(context, WITHIN_POLYGONS_ROW_KEY);
				buildDetailsRow(rowContainer, getRowIcon(R.drawable.ic_action_pin_location),
						app.getString(R.string.transport_nearby_routes), null,
						MenuObjectUtils.getMenuObjectsNamesByComma(menuObjects),
						getWithinCollapsableView(menuObjects), true, null);
				viewGroup.addView(rowContainer);
			}
		}
	}

	@NonNull
	private CollapsableView getWithinCollapsableView(@NonNull List<MenuObject> menuObjects) {
		LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
		for (int i = 0; i < menuObjects.size(); i++) {
			MenuObject menuObject = menuObjects.get(i);
			View container = createRowContainer(app, null);
			String title = menuObject.getTitleStr();
			String textPrefix = MenuObjectUtils.getSecondLineText(menuObject);
			OnClickListener onClickListener = v -> {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					IContextMenuProvider contextObject = mapActivity.getMapLayers().getPoiMapLayer();
					ContextMenuLayer contextMenuLayer = mapActivity.getMapLayers().getContextMenuLayer();
					contextMenuLayer.showContextMenu(menuObject.getLatLon(), menuObject.getPointDescription(), menuObject.getObject(), contextObject);
				}
			};
			buildDetailsRow(container, null, title, textPrefix, null, null, false, onClickListener);
			llv.addView(container);
		}
		return new CollapsableView(llv, this, true);
	}

	protected void buildDetailsRow(@NonNull View view, @Nullable Drawable icon, @Nullable String text,
	                               @Nullable String textPrefix, @Nullable String textSuffix,
	                               @Nullable CollapsableView collapsableView, boolean parentRow,
	                               @Nullable OnClickListener onClickListener) {
		menuRowBuilder.buildDetailsRow(view, icon, text, textPrefix, textSuffix,
				collapsableView, isFirstRow(), parentRow, onClickListener);
		rowBuilt();
	}

	protected void buildNearestWikiRow(ViewGroup viewGroup) {
		int position = viewGroup.getChildCount();
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		buildNearestWikiRow(viewGroup, new SearchAmenitiesListener() {
			@Override
			public void onFinish(List<Amenity> amenities) {
				ViewGroup viewGroup = viewGroupRef.get();
				if (viewGroup == null || Algorithms.isEmpty(amenities)) {
					return;
				}
				View amenitiesRow = createRowContainer(viewGroup.getContext(), NEAREST_WIKI_KEY);

				int insertIndex = position == 0 ? 0 : position + 1;

				firstRow = insertIndex == 0 || isDividerAtPosition(viewGroup, insertIndex - 1);
				String text = app.getString(R.string.wiki_around);
				buildNearestRow(amenitiesRow, amenities, R.drawable.ic_action_wikipedia, text, NEAREST_WIKI_KEY);
				viewGroup.addView(amenitiesRow, insertIndex);

				buildNearestRowDividerIfMissing(viewGroup, insertIndex);
			}
		});
	}

	protected void buildNearestPoiRow(ViewGroup viewGroup) {
		if (amenity != null) {
			int position = viewGroup.getChildCount();
			WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
			buildNearestPoiRow(new SearchAmenitiesListener() {
				@Override
				public void onFinish(List<Amenity> amenities) {
					ViewGroup viewGroup = viewGroupRef.get();
					if (viewGroup == null) {
						return;
					}
					String title = app.getString(R.string.speak_poi);
					String type = "\"" + AmenityMenuController.getTypeStr(amenity) + "\"";
					String count = "(" + amenities.size() + ")";
					String text = app.getString(R.string.ltr_or_rtl_triple_combine_via_space, title, type, count);

					View wikiRow = viewGroup.findViewWithTag(NEAREST_WIKI_KEY);
					int insertIndex = wikiRow != null
							? viewGroup.indexOfChild(wikiRow) + 1
							: position == 0 ? 0 : position + 1;

					View amenitiesRow = createRowContainer(viewGroup.getContext(), NEAREST_POI_KEY);
					firstRow = insertIndex == 0 || isDividerAtPosition(viewGroup, insertIndex - 1);
					buildNearestRow(amenitiesRow, amenities, AmenityMenuController.getRightIconId(amenity), text, NEAREST_POI_KEY);
					viewGroup.addView(amenitiesRow, insertIndex);

					buildNearestRowDividerIfMissing(viewGroup, insertIndex);
				}
			});
		}
	}

	protected View createRowContainer(Context context, String tag) {
		LinearLayout view = new LinearLayout(context);
		view.setTag(tag);
		view.setOrientation(LinearLayout.VERTICAL);
		view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return view;
	}

	protected void buildNearestRow(View view, List<Amenity> nearestAmenities, int iconId, String text, String amenityKey) {
		if (nearestAmenities.size() > 0) {
			String count = "(" + nearestAmenities.size() + ")";
			text = app.getString(R.string.ltr_or_rtl_combine_via_space, text, count);
			CollapsableView collapsableView = getCollapsableView(view.getContext(), true, nearestAmenities, amenityKey);
			buildRow(view, iconId, null, text, 0, true, collapsableView,
					false, 0, false, null, false);
		}
	}

	protected void buildNearestRowDividerIfMissing(@NonNull ViewGroup viewGroup, int nearestRowPosition) {
		if (!isDividerAtPosition(viewGroup, nearestRowPosition + 1)) {
			buildRowDivider(viewGroup, nearestRowPosition + 1);
		}
	}

	protected void buildNearestPhotosRow(View view) {
		boolean needUpdateOnly = onlinePhotoCardsRow != null && onlinePhotoCardsRow.getMenuBuilder() == this;
		onlinePhotoCardsRow = new CardsRowBuilder(this);
		onlinePhotoCardsRow.build(galleryController, true, getApplication().getDaynightHelper().isNightModeForMapControls());

		LinearLayout parent = new LinearLayout(view.getContext());
		parent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		parent.setOrientation(LinearLayout.VERTICAL);
		parent.addView(onlinePhotoCardsRow.getGalleryView());
		CollapsableView collapsableView = new CollapsableView(parent, this, app.getSettings().ONLINE_PHOTOS_ROW_COLLAPSED);
		collapsableView.setCollapseExpandListener(collapsed -> {
			if (!collapsed && onlinePhotoCards == null) {
				startLoadingImages();
			}
		});
		buildRow(view, R.drawable.ic_action_photo, null, app.getString(R.string.online_photos), 0, true,
				collapsableView, false, 1, false, null, false);

		if (needUpdateOnly && onlinePhotoCards != null) {
			onlinePhotoCardsRow.setCards(onlinePhotoCards);
		}
	}

	private void buildCoordinatesRow(View view) {
		Map<Integer, String> locationData = PointDescription.getLocationData(mapActivity, latLon.getLatitude(), latLon.getLongitude(), true);
		String title = locationData.get(PointDescription.LOCATION_LIST_HEADER);
		locationData.remove(PointDescription.LOCATION_LIST_HEADER);
		CollapsableView cv = getLocationCollapsableView(locationData);
		buildRow(view, R.drawable.ic_action_get_my_location, null, title, 0, true, cv, false, 1,
				false, null, false);
	}

	public void startLoadingImages() {
		startLoadingImagesTask();
	}

	private void startLoadingImagesTask() {
		if (!app.getSettings().isInternetConnectionAvailable() || galleryController == null) {
			return;
		}
		onlinePhotoCards = new ArrayList<>();
		LatLon latLon = getLatLon();
		Map<String, String> params = getAdditionalCardParams();
		if (galleryController.isCurrentHolderEquals(latLon, params)) {
			imageCardListener.onFinish(galleryController.getCurrentCardsHolder());
		}
		execute(new GetImageCardsTask(mapActivity, getLatLon(), getAdditionalCardParams(), imageCardListener));
	}

	protected Map<String, String> getAdditionalCardParams() {
		return null;
	}

	protected void buildInternal(View view) {
	}

	protected void buildTopInternal(View view) {
		buildDescription(view);
		if (showLocalTransportRoutes()) {
			buildRow(view, 0, null, app.getString(R.string.transport_Routes), 0, true, getCollapsableTransportStopRoutesView(view.getContext(), false, false),
					false, 0, false, null, true);
		}
		if (showNearbyTransportRoutes()) {
			CollapsableView collapsableView = getCollapsableTransportStopRoutesView(view.getContext(), false, true);
			String routesWithingDistance = app.getString(R.string.transport_nearby_routes_within) + " " + OsmAndFormatter.getFormattedDistance(TransportStopController.SHOW_STOPS_RADIUS_METERS_UI, app);
			buildRow(view, 0, null, routesWithingDistance, 0, true, collapsableView,
					false, 0, false, null, true);
		}
	}

	protected void buildDescription(View view) {
	}

	protected void showDescriptionDialog(@NonNull Context ctx, @NonNull String description, @NonNull String title) {
		if (Algorithms.isHtmlText(description)) {
			POIMapLayer.showHtmlDescriptionDialog(ctx, app, description, title);
		} else {
			POIMapLayer.showPlainDescriptionDialog(ctx, app, description, title);
		}
	}

	public boolean isFirstRow() {
		return firstRow;
	}

	public void rowBuilt() {
		firstRow = false;
	}

	public View buildRow(View view, int iconId, String buttonText, String text, int textColor,
	                     boolean collapsable, CollapsableView collapsableView,
	                     boolean needLinks, int textLinesLimit, boolean isUrl, OnClickListener onClickListener, boolean matchWidthDivider) {
		return buildRow(view, iconId == 0 ? null : getRowIcon(iconId), buttonText, text, textColor, null, collapsable, collapsableView,
				needLinks, textLinesLimit, isUrl, onClickListener, matchWidthDivider);
	}

	public View buildRow(View view, Drawable icon, String buttonText, String text, int textColor, String secondaryText,
	                     boolean collapsable, CollapsableView collapsableView, boolean needLinks,
	                     int textLinesLimit, boolean isUrl, OnClickListener onClickListener, boolean matchWidthDivider) {
		return buildRow(view, icon, buttonText, null, text, textColor, secondaryText, collapsable, collapsableView,
				needLinks, textLinesLimit, isUrl, false, false, onClickListener, matchWidthDivider);
	}

	public View buildRow(View view, int iconId, String buttonText, String text, int textColor,
	                     boolean collapsable, CollapsableView collapsableView,
	                     boolean needLinks, int textLinesLimit, boolean isUrl, boolean isNumber, boolean isEmail, OnClickListener onClickListener, boolean matchWidthDivider) {
		return buildRow(view, iconId == 0 ? null : getRowIcon(iconId), buttonText, null, text, textColor, null, collapsable, collapsableView,
				needLinks, textLinesLimit, isUrl, isNumber, isEmail, onClickListener, matchWidthDivider);
	}

	public View buildRow(View view, Drawable icon, String buttonText, String textPrefix, String text,
	                     int textColor, String secondaryText, boolean collapsable, CollapsableView collapsableView, boolean needLinks,
	                     int textLinesLimit, boolean isUrl, boolean isNumber, boolean isEmail, OnClickListener onClickListener, boolean matchWidthDivider) {
		boolean light = isLightContent();

		if (!isFirstRow()) {
			buildRowDivider(view);
		}

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llBaseViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		baseView.setLayoutParams(llBaseViewParams);

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		ll.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				String textToCopy = Algorithms.isEmpty(textPrefix) ? text : textPrefix + ": " + text;
				copyToClipboard(textToCopy, view.getContext());
				return true;
			}
		});

		baseView.addView(ll);

		// Icon
		if (icon != null) {
			LinearLayout llIcon = new LinearLayout(view.getContext());
			llIcon.setOrientation(LinearLayout.HORIZONTAL);
			llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(64f), dpToPx(48f)));
			llIcon.setGravity(Gravity.CENTER_VERTICAL);
			ll.addView(llIcon);

			ImageView iconView = new ImageView(view.getContext());
			LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
			AndroidUtils.setMargins(llIconParams, dpToPx(16f), dpToPx(12f), dpToPx(24f), dpToPx(12f));
			llIconParams.gravity = Gravity.CENTER_VERTICAL;
			iconView.setLayoutParams(llIconParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconView.setImageDrawable(icon);
			llIcon.addView(iconView);
		}

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.weight = 1f;
		AndroidUtils.setMargins(llTextViewParams, 0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		ll.addView(llText);

		TextViewEx textPrefixView = null;
		if (!Algorithms.isEmpty(textPrefix)) {
			textPrefixView = new TextViewEx(view.getContext());
			LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			AndroidUtils.setMargins(llTextParams, icon == null ? dpToPx(16f) : 0, dpToPx(8f), 0, 0);
			textPrefixView.setLayoutParams(llTextParams);
			textPrefixView.setTypeface(DEFAULT);
			textPrefixView.setTextSize(12);
			textPrefixView.setTextColor(ColorUtilities.getSecondaryTextColor(app, !light));
			textPrefixView.setMinLines(1);
			textPrefixView.setMaxLines(1);
			textPrefixView.setText(textPrefix);
			llText.addView(textPrefixView);
		}

		// Primary text
		TextViewEx textView = new TextViewEx(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llTextParams,
				icon != null ? 0 : dpToPx(16f), dpToPx(textPrefixView != null ? 2f : (secondaryText != null ? 10f : 8f)), 0, dpToPx(secondaryText != null ? 6f : 8f));
		textView.setLayoutParams(llTextParams);
		textView.setTypeface(DEFAULT);
		textView.setTextSize(16);
		textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, !light));
		textView.setText(text);

		int linkTextColor = ContextCompat.getColor(view.getContext(), light ? R.color.active_color_primary_light : R.color.active_color_primary_dark);

		if (isUrl || isNumber || isEmail) {
			textView.setTextColor(linkTextColor);
		} else if (needLinks && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID) && Linkify.addLinks(textView, Linkify.ALL)) {
			textView.setMovementMethod(null);
			textView.setLinkTextColor(linkTextColor);
			textView.setOnTouchListener(new ClickableSpanTouchListener());
			AndroidUtils.removeLinkUnderline(textView);
		}
		if (textLinesLimit > 0) {
			textView.setMinLines(1);
			textView.setMaxLines(textLinesLimit);
			textView.setEllipsize(TextUtils.TruncateAt.END);
		}
		if (textColor > 0) {
			textView.setTextColor(getColor(textColor));
		}
		llText.addView(textView);

		// Secondary text
		if (!TextUtils.isEmpty(secondaryText)) {
			TextViewEx textViewSecondary = new TextViewEx(view.getContext());
			LinearLayout.LayoutParams llTextSecondaryParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			AndroidUtils.setMargins(llTextSecondaryParams, icon != null ? 0 : dpToPx(16f), 0, 0, dpToPx(6f));
			textViewSecondary.setLayoutParams(llTextSecondaryParams);
			textViewSecondary.setTypeface(DEFAULT);
			textViewSecondary.setTextSize(14);
			textViewSecondary.setTextColor(ColorUtilities.getSecondaryTextColor(app, !light));
			textViewSecondary.setText(secondaryText);
			llText.addView(textViewSecondary);
		}

		//Button
		if (!TextUtils.isEmpty(buttonText)) {
			TextViewEx buttonTextView = new TextViewEx(view.getContext());
			LinearLayout.LayoutParams buttonTextViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			buttonTextViewParams.gravity = Gravity.CENTER_VERTICAL;
			AndroidUtils.setMargins(buttonTextViewParams, dpToPx(8), 0, dpToPx(8), 0);
			buttonTextView.setLayoutParams(buttonTextViewParams);
			buttonTextView.setTypeface(FontCache.getMediumFont());
			buttonTextView.setAllCaps(true);
			buttonTextView.setTextColor(ContextCompat.getColor(view.getContext(), !light ? R.color.ctx_menu_controller_button_text_color_dark_n : R.color.ctx_menu_controller_button_text_color_light_n));
			buttonTextView.setText(buttonText);
			ll.addView(buttonTextView);
		}

		ImageView iconViewCollapse = new ImageView(view.getContext());
		if (collapsable && collapsableView != null) {
			// Icon
			LinearLayout llIconCollapse = new LinearLayout(view.getContext());
			llIconCollapse.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40f), dpToPx(48f)));
			llIconCollapse.setOrientation(LinearLayout.HORIZONTAL);
			llIconCollapse.setGravity(Gravity.CENTER_VERTICAL);
			ll.addView(llIconCollapse);

			LinearLayout.LayoutParams llIconCollapseParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
			AndroidUtils.setMargins(llIconCollapseParams, 0, dpToPx(12f), dpToPx(24f), dpToPx(12f));
			llIconCollapseParams.gravity = Gravity.CENTER_VERTICAL;
			iconViewCollapse.setLayoutParams(llIconCollapseParams);
			iconViewCollapse.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconViewCollapse.setImageDrawable(getCollapseIcon(collapsableView.getContentView().getVisibility() == View.GONE));
			llIconCollapse.addView(iconViewCollapse);
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (collapsableView.getContentView().getVisibility() == View.VISIBLE) {
						collapsableView.getContentView().setVisibility(View.GONE);
						iconViewCollapse.setImageDrawable(getCollapseIcon(true));
						collapsableView.setCollapsed(true);
					} else {
						collapsableView.getContentView().setVisibility(View.VISIBLE);
						iconViewCollapse.setImageDrawable(getCollapseIcon(false));
						collapsableView.setCollapsed(false);
					}
				}
			});
			if (collapsableView.isCollapsed()) {
				collapsableView.getContentView().setVisibility(View.GONE);
				iconViewCollapse.setImageDrawable(getCollapseIcon(true));
			}
			if (collapsableView.getContentView().getParent() != null) {
				((ViewGroup) collapsableView.getContentView().getParent())
						.removeView(collapsableView.getContentView());
			}
			baseView.addView(collapsableView.getContentView());
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		} else if (isUrl) {
			ll.setOnClickListener(v -> {
				if (customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(text));
					AndroidUtils.startActivityIfSafe(v.getContext(), intent);
				}
			});
		} else if (isNumber) {
			ll.setOnClickListener(v -> {
				if (customization.isFeatureEnabled(CONTEXT_MENU_PHONE_ID)) {
					showDialog(text, Intent.ACTION_DIAL, "tel:", v);
				}
			});
		} else if (isEmail) {
			ll.setOnClickListener(v -> {
				if (customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
					Intent intent = new Intent(Intent.ACTION_SENDTO);
					intent.setData(Uri.parse("mailto:" + text));
					AndroidUtils.startActivityIfSafe(v.getContext(), intent);
				}
			});
		}

		((LinearLayout) view).addView(baseView);

		rowBuilt();

		setDividerWidth(matchWidthDivider);

		return ll;
	}

	public View buildDescriptionRow(View view, String description) {
		String descriptionLabel = app.getString(R.string.shared_string_description);
		View.OnClickListener onClickListener = v -> {
			showDescriptionDialog(view.getContext(), description, descriptionLabel);
		};
		boolean light = isLightContent();

		if (!isFirstRow()) {
			buildRowDivider(view);
		}

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llBaseViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		baseView.setLayoutParams(llBaseViewParams);

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		ll.setOnLongClickListener(v -> {
			copyToClipboard(description, view.getContext());
			return true;
		});

		baseView.addView(ll);

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.weight = 1f;
		AndroidUtils.setMargins(llTextViewParams, 0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		ll.addView(llText);

		// Description label
		TextViewEx textPrefixView = new TextViewEx(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llTextParams, dpToPx(16f), dpToPx(8f), 0, 0);
		textPrefixView.setLayoutParams(llTextParams);
		textPrefixView.setTypeface(DEFAULT);
		textPrefixView.setTextSize(12);
		textPrefixView.setTextColor(ColorUtilities.getSecondaryTextColor(app, !light));
		textPrefixView.setMinLines(1);
		textPrefixView.setMaxLines(1);
		textPrefixView.setText(descriptionLabel);
		llText.addView(textPrefixView);

		// Description
		TextViewEx textView = new TextViewEx(view.getContext());
		LinearLayout.LayoutParams llDescriptionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llDescriptionParams, dpToPx(16f), dpToPx(2f), 0, dpToPx(8f));
		textView.setLayoutParams(llDescriptionParams);
		textView.setTypeface(DEFAULT);
		textView.setTextSize(16);
		textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, !light));
		textView.setText(WikiArticleHelper.getPartialContent(description));

		if (customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID) && Linkify.addLinks(textView, Linkify.ALL)) {
			textView.setMovementMethod(null);
			int linkTextColor = ContextCompat.getColor(view.getContext(), light ?
					R.color.active_color_primary_light : R.color.active_color_primary_dark);
			textView.setLinkTextColor(linkTextColor);
			textView.setOnTouchListener(new ClickableSpanTouchListener());
			AndroidUtils.removeLinkUnderline(textView);
		}
		textView.setMinLines(1);
		textView.setMaxLines(10);
		textView.setEllipsize(TextUtils.TruncateAt.END);
		llText.addView(textView);

		// Read Full button
		buildReadFullButton(llText, app.getString(R.string.context_menu_read_full), onClickListener);

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}
		((LinearLayout) view).addView(baseView);

		rowBuilt();
		setDividerWidth(true);

		return ll;
	}

	protected void showDialog(String text, String actionType, String dataPrefix, View v) {
		Context context = v.getContext();
		String[] items = text.split("[,;]");
		Intent intent = new Intent(actionType);
		if (items.length > 1) {
			for (int i = 0; i < items.length; i++) {
				items[i] = items[i].trim();
			}
			AlertDialog.Builder dlg = new AlertDialog.Builder(context);
			dlg.setNegativeButton(R.string.shared_string_cancel, null);
			dlg.setItems(items, (dialog, which) -> {
				intent.setData(Uri.parse(dataPrefix + items[which]));
				AndroidUtils.startActivityIfSafe(context, intent);
			});
			dlg.show();
		} else {
			intent.setData(Uri.parse(dataPrefix + text));
			AndroidUtils.startActivityIfSafe(context, intent);
		}
	}

	protected void setDividerWidth(boolean matchWidthDivider) {
		this.matchWidthDivider = matchWidthDivider;
	}

	protected void copyToClipboard(String text, Context ctx) {
		menuRowBuilder.copyToClipboard(text, ctx);
	}

	protected CollapsableView getLocationCollapsableView(Map<Integer, String> locationData) {
		LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
		for (Map.Entry<Integer, String> line : locationData.entrySet()) {
			TextViewEx button = buildButtonInCollapsableView(mapActivity, false, false);
			SpannableStringBuilder ssb = new SpannableStringBuilder();
			if (line.getKey() == OsmAndFormatter.UTM_FORMAT) {
				ssb.append("UTM: ");
				ssb.setSpan(new ForegroundColorSpan(getColor(R.color.text_color_secondary_light)), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if (line.getKey() == OsmAndFormatter.MGRS_FORMAT) {
				ssb.append("MGRS: ");
				ssb.setSpan(new ForegroundColorSpan(getColor(R.color.text_color_secondary_light)), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if (line.getKey() == OsmAndFormatter.OLC_FORMAT) {
				ssb.append("OLC: ");
				ssb.setSpan(new ForegroundColorSpan(getColor(R.color.text_color_secondary_light)), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if (line.getKey() == OsmAndFormatter.SWISS_GRID_FORMAT) {
				ssb.append("CH1903: ");
				ssb.setSpan(new ForegroundColorSpan(getColor(R.color.text_color_secondary_light)), 0, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if (line.getKey() == OsmAndFormatter.SWISS_GRID_PLUS_FORMAT) {
				ssb.append("CH1903+: ");
				ssb.setSpan(new ForegroundColorSpan(getColor(R.color.text_color_secondary_light)), 0, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			ssb.append(line.getValue());
			button.setText(ssb);
			button.setOnClickListener(v -> copyToClipboard(line.getValue(), mapActivity));
			llv.addView(button);
		}
		return new CollapsableView(llv, this, true);

	}

	protected CollapsableView getDistanceCollapsableView(Set<String> distanceData) {
		LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
		for (String distance : distanceData) {
			TextView button = buildButtonInCollapsableView(mapActivity, false, false);
			button.setText(distance);
			button.setOnClickListener(v -> copyToClipboard(distance, mapActivity));
			llv.addView(button);
		}
		return new CollapsableView(llv, this, true);
	}

	public void buildRowDivider(@NonNull View view) {
		buildRowDivider(view, -1);
	}

	public void buildRowDivider(@NonNull View view, int index) {
		boolean light = isLightContent();
		View horizontalLine = new View(view.getContext());
		horizontalLine.setTag(DIVIDER_ROW_KEY);
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		if (!matchWidthDivider) {
			AndroidUtils.setMargins(llHorLineParams, dpToPx(64f), 0, 0, 0);
		}
		horizontalLine.setLayoutParams(llHorLineParams);
		horizontalLine.setBackgroundColor(getColor(light ? R.color.divider_color_light : R.color.divider_color_dark));
		((LinearLayout) view).addView(horizontalLine, index);
	}

	protected boolean isDividerAtPosition(@NonNull ViewGroup viewGroup, int index) {
		View child = viewGroup.getChildAt(index);
		return child != null && DIVIDER_ROW_KEY.equals(child.getTag());
	}

	protected void buildReadFullButton(LinearLayout container, String btnText, View.OnClickListener onClickListener) {
		Context ctx = container.getContext();
		boolean light = isLightContent();

		TextViewEx button = new TextViewEx(new ContextThemeWrapper(ctx, light ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme));
		LinearLayout.LayoutParams llButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(36f));
		AndroidUtils.setMargins(llButtonParams, dpToPx(16f), 0, 0, dpToPx(16f));
		button.setLayoutParams(llButtonParams);
		button.setTypeface(FontCache.getMediumFont());
		button.setBackgroundResource(light ? R.drawable.context_menu_controller_bg_light : R.drawable.context_menu_controller_bg_dark);
		button.setTextSize(14);
		int paddingSides = dpToPx(10f);
		button.setPadding(paddingSides, 0, paddingSides, 0);
		ColorStateList buttonColorStateList = AndroidUtils.createPressedColorStateList(ctx, !light,
				R.color.ctx_menu_controller_button_text_color_light_n, R.color.ctx_menu_controller_button_text_color_light_p,
				R.color.ctx_menu_controller_button_text_color_dark_n, R.color.ctx_menu_controller_button_text_color_dark_p);
		button.setTextColor(buttonColorStateList);
		button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		button.setSingleLine(true);
		button.setEllipsize(TextUtils.TruncateAt.END);
		button.setOnClickListener(onClickListener);
		button.setAllCaps(true);
		button.setText(btnText);
		Drawable normal = app.getUIUtilities().getIcon(R.drawable.ic_action_read_text,
				light ? R.color.ctx_menu_controller_button_text_color_light_n : R.color.ctx_menu_controller_button_text_color_dark_n);
		Drawable pressed = app.getUIUtilities().getIcon(R.drawable.ic_action_read_text,
				light ? R.color.ctx_menu_controller_button_text_color_light_p : R.color.ctx_menu_controller_button_text_color_dark_p);

		Drawable drawable = AndroidUtils.createPressedStateListDrawable(normal, pressed);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(button, drawable, null, null, null);
		button.setCompoundDrawablePadding(dpToPx(8f));
		container.addView(button);
	}

	protected void buildDateRow(@NonNull View view, long timestamp) {
		if (timestamp > 0) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(view.getContext());
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(view.getContext());
			Date date = new Date(timestamp);
			buildRow(view, R.drawable.ic_action_data, null, dateFormat.format(date) + " — " + timeFormat.format(date),
					0, false, null, false, 0, false, null, false);
		}
	}

	protected void buildCommentRow(@NonNull View view, @Nullable String comment) {
		if (!Algorithms.isEmpty(comment)) {
			View row = buildRow(view, R.drawable.ic_action_note_dark, null, comment, 0,
					false, null, true, 10, false, null, false);
			row.setOnClickListener(v -> POIMapLayer.showPlainDescriptionDialog(row.getContext(),
					app, comment, row.getResources().getString(R.string.poi_dialog_comment)));
		}
	}

	public boolean hasCustomAddressLine() {
		return false;
	}

	public void buildCustomAddressLine(LinearLayout ll) {
	}

	public void addPlainMenuItem(int iconId, String text, boolean needLinks, boolean isUrl, OnClickListener onClickListener) {
		plainMenuItems.add(new PlainMenuItem(iconId, null, text, needLinks, isUrl, false, null, onClickListener));
	}

	public void addPlainMenuItem(int iconId, String buttonText, String text, boolean needLinks, boolean isUrl, OnClickListener onClickListener) {
		plainMenuItems.add(new PlainMenuItem(iconId, buttonText, text, needLinks, isUrl, false, null, onClickListener));
	}

	public void addPlainMenuItem(int iconId, String text, boolean needLinks, boolean isUrl,
	                             boolean collapsable, CollapsableView collapsableView,
	                             OnClickListener onClickListener) {
		plainMenuItems.add(new PlainMenuItem(iconId, null, text, needLinks, isUrl, collapsable, collapsableView, onClickListener));
	}

	public void clearPlainMenuItems() {
		plainMenuItems.clear();
	}

	private void buildTransportRowItem(View view, TransportStopRoute route, OnClickListener listener) {
		boolean light = isLightContent();
		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llBaseViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		baseView.setLayoutParams(llBaseViewParams);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		AndroidUtils.setPadding(baseView, dpToPx(16), 0, dpToPx(16), dpToPx(12));

		TextViewEx transportRect = new TextViewEx(view.getContext());
		LinearLayout.LayoutParams trParams = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(18));
		AndroidUtils.setMargins(trParams, 0, dpToPx(16), 0, 0);
		transportRect.setLayoutParams(trParams);
		transportRect.setGravity(Gravity.CENTER);
		transportRect.setAllCaps(true);
		transportRect.setTypeface(FontCache.getMediumFont());
		transportRect.setTextColor(Color.WHITE);
		transportRect.setTextSize(10);
		transportRect.setMaxLines(1);

		GradientDrawable shape = new GradientDrawable();
		shape.setShape(GradientDrawable.RECTANGLE);
		shape.setCornerRadius(dpToPx(3));
		int bgColor = route.getColor(app, !light);
		shape.setColor(bgColor);
		transportRect.setTextColor(ColorUtilities.getContrastColor(app, bgColor, true));

		transportRect.setBackground(shape);
		transportRect.setText(route.route.getAdjustedRouteRef(true));
		baseView.addView(transportRect);

		LinearLayout infoView = new LinearLayout(view.getContext());
		infoView.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams infoViewLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(infoViewLayoutParams, dpToPx(16), dpToPx(12), dpToPx(16), 0);
		infoView.setLayoutParams(infoViewLayoutParams);
		baseView.addView(infoView);

		TextView titleView = new TextView(view.getContext());
		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		titleView.setLayoutParams(titleParams);
		titleView.setTextSize(16);
		int textColor = ColorUtilities.getPrimaryTextColor(app, !light);
		titleView.setTextColor(textColor);
		String desc = route.getDescription(getMapActivity().getMyApplication(), true);
		Drawable arrow = app.getUIUtilities().getIcon(R.drawable.ic_arrow_right_16, light ? R.color.icon_color_secondary_light : R.color.icon_color_secondary_dark);
		arrow.setBounds(0, 0, arrow.getIntrinsicWidth(), arrow.getIntrinsicHeight());

		titleView.setText(AndroidUtils.replaceCharsWithIcon(desc, arrow, arrowChars));
		infoView.addView(titleView);
		if (route.route.hasInterval()) {
			infoView.addView(createIntervalView(view.getContext(), route, titleParams, textColor));
		}
		LinearLayout typeView = new LinearLayout(view.getContext());
		typeView.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams typeViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(typeViewParams, 0, dpToPx(8), 0, 0);
		typeView.setGravity(Gravity.CENTER);
		typeView.setLayoutParams(typeViewParams);
		infoView.addView(typeView);

		ImageView typeImageView = new ImageView(view.getContext());
		LinearLayout.LayoutParams typeImageParams = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
		AndroidUtils.setMargins(typeImageParams, dpToPx(4), 0, dpToPx(4), 0);
		typeImageView.setLayoutParams(typeImageParams);
		int drawableResId = route.type == null ? R.drawable.ic_action_polygom_dark : route.type.getResourceId();
		typeImageView.setImageDrawable(getRowIcon(drawableResId));
		typeView.addView(typeImageView);

		TextView typeTextView = new TextView(view.getContext());
		LinearLayout.LayoutParams typeTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		typeTextView.setLayoutParams(typeTextParams);
		typeTextView.setText(route.getTypeStrRes());
		AndroidUtils.setTextSecondaryColor(getMapActivity(), typeTextView, getApplication().getDaynightHelper().isNightModeForMapControls());
		typeView.addView(typeTextView);

		baseView.setOnClickListener(listener);

		((ViewGroup) view).addView(baseView);
	}

	private View createIntervalView(Context ctx, TransportStopRoute route, LinearLayout.LayoutParams titleParams,
	                                int textColor) {
		TextView intervalView;
		intervalView = new TextView(ctx);
		intervalView.setLayoutParams(titleParams);
		intervalView.setTextSize(16);
		intervalView.setTextColor(textColor);
		intervalView.setText(ctx.getString(R.string.ltr_or_rtl_combine_via_colon,
				ctx.getString(R.string.shared_string_interval), route.route.getInterval()));
		return intervalView;
	}

	private void buildTransportRouteRow(ViewGroup parent, TransportStopRoute r, OnClickListener listener, boolean showDivider) {
		buildTransportRowItem(parent, r, listener);

		if (showDivider) {
			buildRowDivider(parent);
		}
	}

	private CollapsableView getCollapsableTransportStopRoutesView(Context context, boolean collapsed, boolean isNearbyRoutes) {
		LinearLayout view = buildCollapsableContentView(context, collapsed, false);
		List<TransportStopRoute> localTransportStopRoutes = mapContextMenu.getLocalTransportStopRoutes();
		List<TransportStopRoute> nearbyTransportStopRoutes = mapContextMenu.getNearbyTransportStopRoutes();
		if (!isNearbyRoutes) {
			buildTransportRouteRows(view, localTransportStopRoutes);
		} else {
			buildTransportRouteRows(view, nearbyTransportStopRoutes);
		}
		return new CollapsableView(view, this, collapsed);
	}

	private void buildTransportRouteRows(LinearLayout view, List<TransportStopRoute> routes) {
		for (int i = 0; i < routes.size(); i++) {
			TransportStopRoute r = routes.get(i);
			boolean showDivider = i < routes.size() - 1;
			buildTransportRouteRow(view, r, createTransportRoutesViewClickListener(r), showDivider);
		}
	}

	private View.OnClickListener createTransportRoutesViewClickListener(TransportStopRoute r) {
		return v -> {
			OsmandMapTileView mapView = getMapActivity().getMapView();
			MapContextMenu mm = getMapActivity().getContextMenu();
			PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_ROUTE,
					r.getDescription(getMapActivity().getMyApplication(), false));
			mm.show(latLon, pd, r);
			TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
			stopsLayer.setRoute(r);
			int zoom = r.calculateZoom(0, mapView.getCurrentRotatedTileBox());
			mapView.setIntZoom(zoom);
		};
	}

	protected CollapsableView getCollapsableTextView(Context context, boolean collapsed, String text) {
		TextViewEx textView = new TextViewEx(context);
		textView.setVisibility(collapsed ? View.GONE : View.VISIBLE);
		LinearLayout.LayoutParams llTextDescParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llTextDescParams, dpToPx(64f), 0, dpToPx(40f), dpToPx(13f));
		textView.setLayoutParams(llTextDescParams);
		textView.setTypeface(DEFAULT);
		textView.setTextSize(16);
		textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, !isLightContent()));
		textView.setText(text);
		return new CollapsableView(textView, this, collapsed);
	}

	protected CollapsableView getCollapsableView(Context context, boolean collapsed, List<Amenity> nearestAmenities, String nearestPoiType) {
		LinearLayout view = buildCollapsableContentView(context, collapsed, true);

		for (Amenity poi : nearestAmenities) {
			TextViewEx button = buildButtonInCollapsableView(context, false, false);
			PointDescription pointDescription = mapActivity.getMapLayers().getPoiMapLayer().getObjectName(poi);
			String name = pointDescription.getName();
			if (Algorithms.isBlank(name)) {
				name = AmenityMenuController.getTypeStr(poi);
			}
			float dist = (float) MapUtils.getDistance(latLon, poi.getLocation());
			name += " (" + OsmAndFormatter.getFormattedDistance(dist, app) + ")";
			button.setText(name);

			button.setOnClickListener(v -> {
				LatLon latLon = new LatLon(poi.getLocation().getLatitude(), poi.getLocation().getLongitude());
				mapActivity.getContextMenu().show(latLon, pointDescription, poi);
				app.getOsmandMap().setMapLocation(latLon.getLatitude(), latLon.getLongitude());
			});
			view.addView(button);
		}
		PoiUIFilter filter = getPoiFilterForType(nearestPoiType);
		if (filter != null) {
			if (customization.isFeatureEnabled(CONTEXT_MENU_SHOW_ON_MAP_ID)
					&& nearestAmenities.size() >= NEARBY_MAX_POI_COUNT) {
				view.addView(createShowOnMap(context, filter));
			}
			if (customization.isFeatureEnabled(CONTEXT_MENU_SEARCH_MORE_ID)) {
				view.addView(createSearchMoreButton(context, filter));
			}
		}
		return new CollapsableView(view, this, collapsed);
	}

	private View createSearchMoreButton(Context context, PoiUIFilter filter) {
		TextViewEx buttonShowAll = buildButtonInCollapsableView(context, false, false);
		buttonShowAll.setText(app.getString(R.string.search_more));
		buttonShowAll.setOnClickListener(v ->
				mapActivity.getFragmentsHelper().showQuickSearch(filter, latLon));
		return buttonShowAll;
	}

	private View createShowOnMap(Context context, PoiUIFilter filter) {
		TextViewEx buttonShowAll = buildButtonInCollapsableView(context, false, false);
		buttonShowAll.setText(app.getString(R.string.shared_string_show_on_map));
		buttonShowAll.setOnClickListener(v -> {
			PoiFiltersHelper poiFiltersHelper = app.getPoiFilters();
			poiFiltersHelper.clearGeneralSelectedPoiFilters();
			poiFiltersHelper.addSelectedPoiFilter(filter);
			QuickSearchToolbarController controller = new QuickSearchToolbarController();
			controller.setTitle(filter.getName());

			controller.setOnBackButtonClickListener(v1 -> {
				if (mapContextMenu != null) {
					mapContextMenu.show();
				}
			});
			controller.setOnTitleClickListener(v2 ->
					mapActivity.getFragmentsHelper().showQuickSearch(filter));

			controller.setOnCloseButtonClickListener(v3 -> {
				poiFiltersHelper.clearGeneralSelectedPoiFilters();
				mapActivity.hideTopToolbar(controller);
				mapActivity.refreshMap();
			});
			mapContextMenu.hideMenus();
			mapActivity.showTopToolbar(controller);
			mapActivity.refreshMap();
		});
		return buttonShowAll;
	}

	protected LinearLayout buildCollapsableContentView(Context context, boolean collapsed, boolean needMargin) {
		LinearLayout view = new LinearLayout(context);
		view.setOrientation(LinearLayout.VERTICAL);
		view.setVisibility(collapsed ? View.GONE : View.VISIBLE);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		if (needMargin) {
			AndroidUtils.setMargins(llParams, dpToPx(64f), 0, dpToPx(12f), 0);
		}
		view.setLayoutParams(llParams);
		return view;
	}

	protected TextViewEx buildButtonInCollapsableView(Context context, boolean selected, boolean showAll) {
		return buildButtonInCollapsableView(context, selected, showAll, true);
	}

	protected TextViewEx buildButtonInCollapsableView(Context context, boolean selected, boolean showAll, boolean singleLine) {
		boolean light = isLightContent();
		TextViewEx button = new TextViewEx(new ContextThemeWrapper(context, light ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme));
		LinearLayout.LayoutParams llWikiButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llWikiButtonParams, 0, 0, 0, dpToPx(8f));
		//button.setMinimumHeight(dpToPx(36f));
		button.setLayoutParams(llWikiButtonParams);
		button.setTypeface(DEFAULT);
		int bg;
		if (selected) {
			bg = light ? R.drawable.context_menu_controller_bg_light_selected : R.drawable.context_menu_controller_bg_dark_selected;
		} else if (showAll) {
			bg = light ? R.drawable.context_menu_controller_bg_light_show_all : R.drawable.context_menu_controller_bg_dark_show_all;
		} else {
			bg = light ? R.drawable.context_menu_controller_bg_light : R.drawable.context_menu_controller_bg_dark;
		}
		button.setBackgroundResource(bg);
		button.setTextSize(14);
		int paddingSides = dpToPx(10f);
		AndroidUtils.setPadding(button, paddingSides, paddingSides, paddingSides, paddingSides);
		if (!selected) {
			ColorStateList buttonColorStateList = AndroidUtils.createPressedColorStateList(context, !light,
					R.color.ctx_menu_controller_button_text_color_light_n, R.color.ctx_menu_controller_button_text_color_light_p,
					R.color.ctx_menu_controller_button_text_color_dark_n, R.color.ctx_menu_controller_button_text_color_dark_p);
			button.setTextColor(buttonColorStateList);
		} else {
			button.setTextColor(ColorUtilities.getPrimaryTextColor(context, !light));
		}
		button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		button.setSingleLine(singleLine);
		button.setEllipsize(TextUtils.TruncateAt.END);
		return button;
	}

	protected void buildNearestWikiRow(ViewGroup viewGroup, SearchAmenitiesListener listener) {
		WikipediaPlugin plugin = PluginsHelper.getEnabledPlugin(WikipediaPlugin.class);
		if (plugin != null) {
			if (plugin.isLocked()) {
				buildGetWikipediaBanner(viewGroup);
			} else if (showNearestWiki && latLon != null) {
				PoiUIFilter filter = app.getPoiFilters().getTopWikiPoiFilter();
				if (filter != null) {
					searchSortedAmenities(filter, latLon, listener);
				}
			}
		}
	}

	private void buildGetWikipediaBanner(ViewGroup viewGroup) {
		boolean light = isLightContent();
		OsmAndFeature feature = OsmAndFeature.WIKIPEDIA;
		LinearLayout view = buildCollapsableContentView(app, false, true);

		View banner = UiUtilities.getInflater(app, !light)
				.inflate(R.layout.get_wikipedia_context_menu_banner, view, false);

		ImageView ivIcon = banner.findViewById(R.id.icon);
		ivIcon.setImageResource(feature.getIconId(!light));

		View btnGet = banner.findViewById(R.id.button_get);
		UiUtilities.setupDialogButton(!light, btnGet, DialogButtonType.PRIMARY, R.string.shared_string_get);
		btnGet.setOnClickListener(v -> {
			if (mapActivity != null) {
				ChoosePlanFragment.showInstance(mapActivity, feature);
			}
		});

		View row = createRowContainer(app, NEAREST_WIKI_KEY);
		view.addView(banner);
		String text = app.getString(R.string.wiki_around);
		CollapsableView collapsableView = new CollapsableView(view, this, false);
		buildRow(row, R.drawable.ic_action_wikipedia, null, text, 0, true, collapsableView,
				false, 0, false, null, false);
		viewGroup.addView(row);
	}

	protected void buildNearestPoiRow(SearchAmenitiesListener listener) {
		if (showNearestPoi && latLon != null && amenity != null) {
			PoiUIFilter filter = getPoiFilterForAmenity(amenity);
			if (filter != null) {
				searchSortedAmenities(filter, latLon, listener);
			}
		}
	}

	private PoiUIFilter getPoiFilterForType(String nearestPoiType) {
		if (NEAREST_POI_KEY.equals(nearestPoiType)) {
			return getPoiFilterForAmenity(amenity);
		} else if (NEAREST_WIKI_KEY.equals(nearestPoiType)) {
			return app.getPoiFilters().getTopWikiPoiFilter();
		}
		return null;
	}

	private PoiUIFilter getPoiFilterForAmenity(Amenity amenity) {
		if (amenity != null) {
			PoiCategory category = amenity.getType();
			PoiType poiType = category.getPoiTypeByKeyName(amenity.getSubType());
			if (poiType != null) {
				return app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + poiType.getKeyName());
			}
		}
		return null;
	}

	private void searchSortedAmenities(PoiUIFilter filter, LatLon latLon, SearchAmenitiesListener listener) {
		execute(new SearchAmenitiesTask(filter, latLon, listener));
	}

	@ColorInt
	protected int getColor(@ColorRes int resId) {
		return menuRowBuilder.getColor(resId);
	}

	public Drawable getRowIcon(int iconId) {
		return menuRowBuilder.getRowIcon(iconId);
	}

	public Drawable getThemedIcon(int iconId) {
		return menuRowBuilder.getThemedIcon(iconId);
	}

	public Drawable getRowIcon(Context ctx, String fileName) {
		return menuRowBuilder.getRowIcon(ctx, fileName);
	}

	public int dpToPx(float dp) {
		return menuRowBuilder.dpToPx(dp);
	}

	public Drawable getCollapseIcon(boolean collapsed) {
		return menuRowBuilder.getCollapseIcon(collapsed);
	}

	protected boolean isLightContent() {
		return menuRowBuilder.isLightContent();
	}

	private class SearchAmenitiesTask extends AsyncTask<Void, Void, List<Amenity>> {

		private final LatLon latLon;
		private final PoiUIFilter filter;
		private final SearchAmenitiesListener listener;

		private SearchAmenitiesTask(PoiUIFilter filter, LatLon latLon, SearchAmenitiesListener listener) {
			this.filter = filter;
			this.latLon = latLon;
			this.listener = listener;
		}

		@Override
		protected List<Amenity> doInBackground(Void... params) {
			int radius = NEARBY_POI_MIN_RADIUS;
			List<Amenity> amenities = Collections.emptyList();
			while (amenities.size() < NEARBY_MAX_POI_COUNT && radius <= NEARBY_POI_MAX_RADIUS) {
				QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), radius);
				amenities = getAmenities(rect, filter);
				amenities.remove(amenity);
				radius *= NEARBY_POI_SEARCH_FACTOR;
			}
			MapUtils.sortListOfMapObject(amenities, latLon.getLatitude(), latLon.getLongitude());
			return amenities.subList(0, Math.min(NEARBY_MAX_POI_COUNT, amenities.size()));
		}

		@Override
		protected void onPostExecute(List<Amenity> amenities) {
			if (listener != null) {
				listener.onFinish(amenities);
			}
		}

		private List<Amenity> getAmenities(QuadRect rect, PoiUIFilter filter) {
			return filter.searchAmenities(rect.top, rect.left,
					rect.bottom, rect.right, -1, null);
		}
	}

	public interface SearchAmenitiesListener {
		void onFinish(List<Amenity> amenities);
	}

	@SuppressWarnings("unchecked")
	public static <P> void execute(AsyncTask<P, ?, ?> task, P... requests) {
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requests);
	}
}
