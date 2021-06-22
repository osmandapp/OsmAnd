package net.osmand.plus.mapcontextmenu;

import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.ActivityResultListener.OnActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.ShareDialog;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.mapcontextmenu.UploadPhotosAsyncTask.UploadPhotosListener;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;
import net.osmand.plus.mapcontextmenu.builders.cards.CardsRowBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask;
import net.osmand.plus.mapcontextmenu.builders.cards.NoImagesCard;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.openplacereviews.AddPhotosBottomSheetDialogFragment;
import net.osmand.plus.openplacereviews.OPRConstants;
import net.osmand.plus.openplacereviews.OprStartFragment;
import net.osmand.plus.osmedit.opr.OpenDBAPI;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.search.QuickSearchDialogFragment.QuickSearchToolbarController;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.views.layers.TransportStopsLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;

public class MenuBuilder {

	private static final Log LOG = PlatformUtil.getLog(MenuBuilder.class);
	private static final int PICK_IMAGE = 1231;
	public static final float SHADOW_HEIGHT_TOP_DP = 17f;
	public static final int TITLE_LIMIT = 60;
	protected static final String[] arrowChars = new String[] {"=>", " - "};
	protected final String NEAREST_WIKI_KEY = "nearest_wiki_key";
	protected final String NEAREST_POI_KEY = "nearest_poi_key";

	private static final int NEARBY_MAX_POI_COUNT = 10;
	private static final int NEARBY_POI_MIN_RADIUS = 250;
	private static final int NEARBY_POI_MAX_RADIUS = 1000;
	private static final int NEARBY_POI_SEARCH_FACTOR = 2;

	protected MapActivity mapActivity;
	protected MapContextMenu mapContextMenu;
	protected OsmandApplication app;
	protected LinkedList<PlainMenuItem> plainMenuItems;
	private boolean firstRow;
	protected boolean matchWidthDivider;
	protected boolean light;
	private Amenity amenity;
	private LatLon latLon;
	private boolean hidden;
	private boolean showTitleIfTruncated = true;
	private boolean showNearestWiki = false;
	private boolean showNearestPoi = false;
	private boolean showOnlinePhotos = true;

	private List<OsmandPlugin> menuPlugins = new ArrayList<>();
	@Nullable
	private CardsRowBuilder onlinePhotoCardsRow;
	private List<AbstractCard> onlinePhotoCards;

	private CollapseExpandListener collapseExpandListener;

	private String preferredMapLang;
	private String preferredMapAppLang;
	private boolean transliterateNames;
	private View photoButton;

	private final OpenDBAPI openDBAPI = new OpenDBAPI();
	private String[] placeId = new String[0];
	private GetImageCardsListener imageCardListener = new GetImageCardsListener() {
		@Override
		public void onPostProcess(List<ImageCard> cardList) {
			processOnlinePhotosCards(cardList);
		}

		@Override
		public void onPlaceIdAcquired(final String[] placeId) {
			MenuBuilder.this.placeId = placeId;
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					AndroidUiHelper.updateVisibility(photoButton, placeId.length >= 2);
				}
			});
		}

		@Override
		public void onFinish(List<ImageCard> cardList) {
			if (!isHidden()) {
				List<AbstractCard> cards = new ArrayList<AbstractCard>(cardList);
				if (cardList.size() == 0) {
					cards.add(new NoImagesCard(mapActivity));
				}
				if (onlinePhotoCardsRow != null) {
					onlinePhotoCardsRow.setCards(cards);
				}
				onlinePhotoCards = cards;
			}
		}
	};

	public void addImageCard(ImageCard card) {
		if (onlinePhotoCards.size() == 1 && onlinePhotoCards.get(0) instanceof NoImagesCard) {
			onlinePhotoCards.clear();
		}
		onlinePhotoCards.add(0, card);
		if (onlinePhotoCardsRow != null) {
			onlinePhotoCardsRow.setCards(onlinePhotoCards);
		}
	}

	public interface CollapseExpandListener {
		void onCollapseExpand(boolean collapsed);
	}

	public MenuBuilder(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.plainMenuItems = new LinkedList<>();

		preferredMapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		preferredMapAppLang = preferredMapLang;
		if (Algorithms.isEmpty(preferredMapAppLang)) {
			preferredMapAppLang = app.getLanguage();
		}
		transliterateNames = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
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

	public void addMenuPlugin(OsmandPlugin plugin) {
		menuPlugins.add(plugin);
	}

	public void setLight(boolean light) {
		this.light = light;
	}

	public void build(ViewGroup view) {
		firstRow = true;
		hidden = false;
		buildTopInternal(view);
		if (showTitleIfTruncated) {
			buildTitleRow(view);
		}
		buildNearestWikiRow(view);
		buildNearestPoiRow(view);
		if (needBuildPlainMenuItems()) {
			buildPlainMenuItems(view);
		}
		buildInternal(view);
		if (needBuildCoordinatesRow()) {
			buildCoordinatesRow(view);
		}
		if (showOnlinePhotos) {
			buildNearestPhotosRow(view);
		}
		buildPluginRows(view);
//		buildAfter(view);
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

	protected void buildPluginRows(View view) {
		for (OsmandPlugin plugin : menuPlugins) {
			plugin.buildContextMenuRows(this, view);
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

	protected void buildNearestWikiRow(ViewGroup viewGroup) {
		final int position = viewGroup.getChildCount();
		final WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		buildNearestWikiRow(new SearchAmenitiesListener() {
			@Override
			public void onFinish(List<Amenity> amenities) {
				ViewGroup viewGroup = viewGroupRef.get();
				if (viewGroup == null || Algorithms.isEmpty(amenities)) {
					return;
				}
				View amenitiesRow = createRowContainer(viewGroup.getContext(), NEAREST_WIKI_KEY);

				buildNearestRow(amenitiesRow, amenities, R.drawable.ic_action_wikipedia, app.getString(R.string.wiki_around), NEAREST_WIKI_KEY);
				viewGroup.addView(amenitiesRow, position);
			}
		});
	}

	protected void buildNearestPoiRow(ViewGroup viewGroup) {
		if (amenity != null) {
			final int position = viewGroup.getChildCount();
			final WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
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

					View amenitiesRow = createRowContainer(viewGroup.getContext(), NEAREST_POI_KEY);
					buildNearestRow(amenitiesRow, amenities, AmenityMenuController.getRightIconId(amenity), text, NEAREST_POI_KEY);

					View wikiRow = viewGroup.findViewWithTag(NEAREST_WIKI_KEY);
					if (wikiRow != null) {
						int index = viewGroup.indexOfChild(wikiRow);
						viewGroup.addView(amenitiesRow, index + 1);
					} else {
						viewGroup.addView(amenitiesRow, position);
					}
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

	protected void buildNearestPhotosRow(View view) {
		if (!app.getSettings().isInternetConnectionAvailable()) {
			return;
		}

		boolean needUpdateOnly = onlinePhotoCardsRow != null && onlinePhotoCardsRow.getMenuBuilder() == this;
		onlinePhotoCardsRow = new CardsRowBuilder(this, view, false);
		onlinePhotoCardsRow.build();
		LinearLayout parent = new LinearLayout(view.getContext());
		parent.setLayoutParams(
				new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT));
		parent.setOrientation(LinearLayout.VERTICAL);
		parent.addView(onlinePhotoCardsRow.getContentView());
		parent.addView(createAddPhotoButton(view.getContext()));
		CollapsableView collapsableView = new CollapsableView(parent, this,
				app.getSettings().ONLINE_PHOTOS_ROW_COLLAPSED);
		collapsableView.setCollapseExpandListener(new CollapseExpandListener() {
			@Override
			public void onCollapseExpand(boolean collapsed) {
				if (!collapsed && onlinePhotoCards == null) {
					startLoadingImages();
				}
			}
		});
		buildRow(view, R.drawable.ic_action_photo_dark, null, app.getString(R.string.online_photos), 0, true,
				collapsableView, false, 1, false, null, false);

		if (needUpdateOnly && onlinePhotoCards != null) {
			onlinePhotoCardsRow.setCards(onlinePhotoCards);
		} else if (!collapsableView.isCollapsed()) {
			startLoadingImages();
		}
	}

	private View createAddPhotoButton(Context ctx) {
		View view = UiUtilities.getInflater(ctx, !light).inflate(R.layout.dialog_button_with_icon, null);
		int dp6 = ctx.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_title_padding_bottom);
		View button = view.findViewById(R.id.button);
		UiUtilities.setupDialogButton(!light, button, UiUtilities.DialogButtonType.STROKED,
				ctx.getString(R.string.shared_string_add_photo), R.drawable.ic_sample);
		TextView textView = view.findViewById(R.id.button_text);
		textView.setCompoundDrawablePadding(dp6);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (false) {
					AddPhotosBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager());
				} else {
					registerResultListener();
					final String baseUrl = OPRConstants.getBaseUrl(app);
					final String name = app.getSettings().OPR_USERNAME.get();
					final String privateKey = app.getSettings().OPR_ACCESS_TOKEN.get();
					if (Algorithms.isBlank(privateKey) || Algorithms.isBlank(name)) {
						OprStartFragment.showInstance(mapActivity.getSupportFragmentManager());
						return;
					}
					new Thread(new Runnable() {
						@Override
						public void run() {
							if (openDBAPI.checkPrivateKeyValid(app, baseUrl, name, privateKey)) {
								app.runInUIThread(new Runnable() {
									@Override
									public void run() {
										Intent intent = new Intent();
										intent.setType("image/*");
										intent.setAction(Intent.ACTION_GET_CONTENT);
										if (Build.VERSION.SDK_INT > 18) {
											intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
										}
										mapActivity.startActivityForResult(Intent.createChooser(intent,
												mapActivity.getString(R.string.select_picture)), PICK_IMAGE);
									}
								});
							} else {
								OprStartFragment.showInstance(mapActivity.getSupportFragmentManager());
							}
						}
					}).start();
				}
			}
		});
		AndroidUiHelper.updateVisibility(view, false);
		photoButton = view;
		return view;
	}

	private void buildCoordinatesRow(View view) {
		Map<Integer, String> locationData = PointDescription.getLocationData(mapActivity, latLon.getLatitude(), latLon.getLongitude(), true);
		String title = locationData.get(PointDescription.LOCATION_LIST_HEADER);
		locationData.remove(PointDescription.LOCATION_LIST_HEADER);
		CollapsableView cv = getLocationCollapsableView(locationData);
		buildRow(view, R.drawable.ic_action_get_my_location, null, title, 0, true, cv, false, 1,
				false, null, false);
	}

	private void registerResultListener() {
		mapActivity.registerActivityResultListener(new ActivityResultListener(PICK_IMAGE, new OnActivityResultListener() {
			@Override
			public void onResult(int resultCode, Intent resultData) {
				if (resultData != null) {
					List<Uri> imagesUri = new ArrayList<>();
					Uri data = resultData.getData();
					if (data != null) {
						imagesUri.add(data);
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						ClipData clipData = resultData.getClipData();
						if (clipData != null) {
							for (int i = 0; i < clipData.getItemCount(); i++) {
								Uri uri = resultData.getClipData().getItemAt(i).getUri();
								if (uri != null) {
									imagesUri.add(uri);
								}
							}
						}
					}
					UploadPhotosListener listener = new UploadPhotosListener() {
						@Override
						public void uploadPhotosSuccess(final String response) {
							app.runInUIThread(new Runnable() {
								@Override
								public void run() {
									if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
										try {
											ImageCard imageCard = OsmandPlugin.createImageCardForJson(new JSONObject(response));
											if (imageCard != null) {
												addImageCard(imageCard);
											}
										} catch (JSONException e) {
											LOG.error(e);
										}
									}
								}
							});
						}
					};
					execute(new UploadPhotosAsyncTask(mapActivity, imagesUri, placeId, listener));
				}
			}
		}));
	}

	private void startLoadingImages() {
		if (onlinePhotoCardsRow == null) {
			return;
		}
		startLoadingImagesTask();
	}

	private void startLoadingImagesTask() {
		onlinePhotoCards = new ArrayList<>();
		onlinePhotoCardsRow.setProgressCard();
		execute(new GetImageCardsTask(mapActivity, getLatLon(), getAdditionalCardParams(), imageCardListener));
	}

	protected Map<String, String> getAdditionalCardParams() {
		return null;
	}

	protected void processOnlinePhotosCards(List<ImageCard> cardList) {
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
			String routesWithingDistance = app.getString(R.string.transport_nearby_routes_within) + " " + OsmAndFormatter.getFormattedDistance(TransportStopController.SHOW_STOPS_RADIUS_METERS, app);
			buildRow(view, 0, null, routesWithingDistance, 0, true, collapsableView,
					false, 0, false, null, true);
		}
	}

	protected void buildDescription(View view) {
	}

	protected void buildAfter(View view) {
		buildRowDivider(view);
	}

	public boolean isFirstRow() {
		return firstRow;
	}

	public void rowBuilt() {
		firstRow = false;
	}

	public View buildRow(View view, int iconId, String buttonText, String text, int textColor,
	                     boolean collapsable, final CollapsableView collapsableView,
	                     boolean needLinks, int textLinesLimit, boolean isUrl, OnClickListener onClickListener, boolean matchWidthDivider) {
		return buildRow(view, iconId == 0 ? null : getRowIcon(iconId), buttonText, text, textColor, null, collapsable, collapsableView,
				needLinks, textLinesLimit, isUrl, onClickListener, matchWidthDivider);
	}

	public View buildRow(final View view, Drawable icon, final String buttonText, final String text, int textColor, String secondaryText,
	                     boolean collapsable, final CollapsableView collapsableView, boolean needLinks,
	                     int textLinesLimit, boolean isUrl, OnClickListener onClickListener, boolean matchWidthDivider) {
		return buildRow(view, icon, buttonText, null, text, textColor, secondaryText, collapsable, collapsableView,
				needLinks, textLinesLimit, isUrl, false, false, onClickListener, matchWidthDivider);
	}

	public View buildRow(View view, int iconId, String buttonText, String text, int textColor,
	                     boolean collapsable, final CollapsableView collapsableView,
	                     boolean needLinks, int textLinesLimit, boolean isUrl, boolean isNumber, boolean isEmail, OnClickListener onClickListener, boolean matchWidthDivider) {
		return buildRow(view, iconId == 0 ? null : getRowIcon(iconId), buttonText, null, text, textColor, null, collapsable, collapsableView,
				needLinks, textLinesLimit, isUrl, isNumber, isEmail, onClickListener, matchWidthDivider);
	}

	public View buildRow(final View view, Drawable icon, final String buttonText, final String textPrefix, final String text,
	                     int textColor, String secondaryText, boolean collapsable, final CollapsableView collapsableView, boolean needLinks,
	                     int textLinesLimit, boolean isUrl, boolean isNumber, boolean isEmail, OnClickListener onClickListener, boolean matchWidthDivider) {

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
			textPrefixView.setTypeface(FontCache.getRobotoRegular(view.getContext()));
			textPrefixView.setTextSize(12);
			textPrefixView.setTextColor(app.getResources().getColor(light ? R.color.text_color_secondary_light : R.color.text_color_secondary_dark));
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
		textView.setTypeface(FontCache.getRobotoRegular(view.getContext()));
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
		textView.setText(text);

		int linkTextColor = ContextCompat.getColor(view.getContext(), light ? R.color.ctx_menu_bottom_view_url_color_light : R.color.ctx_menu_bottom_view_url_color_dark);

		if (isUrl || isNumber || isEmail) {
			textView.setTextColor(linkTextColor);
		} else if (needLinks && Linkify.addLinks(textView, Linkify.ALL)) {
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
			textView.setTextColor(view.getResources().getColor(textColor));
		}
		llText.addView(textView);

		// Secondary text
		if (!TextUtils.isEmpty(secondaryText)) {
			TextViewEx textViewSecondary = new TextViewEx(view.getContext());
			LinearLayout.LayoutParams llTextSecondaryParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			AndroidUtils.setMargins(llTextSecondaryParams, icon != null ? 0 : dpToPx(16f), 0, 0, dpToPx(6f));
			textViewSecondary.setLayoutParams(llTextSecondaryParams);
			textViewSecondary.setTypeface(FontCache.getRobotoRegular(view.getContext()));
			textViewSecondary.setTextSize(14);
			textViewSecondary.setTextColor(app.getResources().getColor(light ? R.color.text_color_secondary_light : R.color.text_color_secondary_dark));
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
			buttonTextView.setTypeface(FontCache.getRobotoMedium(view.getContext()));
			buttonTextView.setAllCaps(true);
			buttonTextView.setTextColor(ContextCompat.getColor(view.getContext(), !light ? R.color.ctx_menu_controller_button_text_color_dark_n : R.color.ctx_menu_controller_button_text_color_light_n));
			buttonTextView.setText(buttonText);
			ll.addView(buttonTextView);
		}

		final ImageView iconViewCollapse = new ImageView(view.getContext());
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
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(text));
					v.getContext().startActivity(intent);
				}
			});
		} else if (isNumber) {
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					showDialog(text, Intent.ACTION_DIAL, "tel:", v);
				}
			});
		} else if (isEmail) {
			ll.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_SENDTO);
					intent.setData(Uri.parse("mailto:" + text));
					v.getContext().startActivity(intent);
				}
			});
		}

		((LinearLayout) view).addView(baseView);

		rowBuilt();

		setDividerWidth(matchWidthDivider);

		return ll;
	}

	public View buildDescriptionRow(final View view, final String description) {

		final String descriptionLabel = app.getString(R.string.shared_string_description);
		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (description.contains("</")) {
					POIMapLayer.showHtmlDescriptionDialog(view.getContext(), app, description, descriptionLabel);
				} else {
					POIMapLayer.showPlainDescriptionDialog(view.getContext(), app, description, descriptionLabel);
				}
			}
		};

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
				copyToClipboard(description, view.getContext());
				return true;
			}
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
		textPrefixView.setTypeface(FontCache.getRobotoRegular(view.getContext()));
		textPrefixView.setTextSize(12);
		textPrefixView.setTextColor(app.getResources().getColor(light ? R.color.text_color_secondary_light : R.color.text_color_secondary_dark));
		textPrefixView.setMinLines(1);
		textPrefixView.setMaxLines(1);
		textPrefixView.setText(descriptionLabel);
		llText.addView(textPrefixView);

		// Description
		TextViewEx textView = new TextViewEx(view.getContext());
		LinearLayout.LayoutParams llDescriptionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llDescriptionParams, dpToPx(16f), dpToPx(2f), 0, dpToPx(8f));
		textView.setLayoutParams(llDescriptionParams);
		textView.setTypeface(FontCache.getRobotoRegular(view.getContext()));
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
		textView.setText(WikiArticleHelper.getPartialContent(description));

		if (Linkify.addLinks(textView, Linkify.ALL)) {
			textView.setMovementMethod(null);
			int linkTextColor = ContextCompat.getColor(view.getContext(), light ?
					R.color.ctx_menu_bottom_view_url_color_light : R.color.ctx_menu_bottom_view_url_color_dark);
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

	protected void showDialog(String text, final String actionType, final String dataPrefix, final View v) {
		final String[] items = text.split("[,;]");
		final Intent intent = new Intent(actionType);
		if (items.length > 1) {
			for (int i = 0; i < items.length; i++) {
				items[i] = items[i].trim();
			}
			AlertDialog.Builder dlg = new AlertDialog.Builder(v.getContext());
			dlg.setNegativeButton(R.string.shared_string_cancel, null);
			dlg.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					intent.setData(Uri.parse(dataPrefix + items[which]));
					v.getContext().startActivity(intent);
				}
			});
			dlg.show();
		} else {
			intent.setData(Uri.parse(dataPrefix + text));
			v.getContext().startActivity(intent);
		}
	}

	protected void setDividerWidth(boolean matchWidthDivider) {
		this.matchWidthDivider = matchWidthDivider;
	}

	protected void copyToClipboard(String text, Context ctx) {
		ShareDialog.copyToClipboardWithToast(ctx, text, Toast.LENGTH_SHORT);
	}

	protected CollapsableView getLocationCollapsableView(Map<Integer, String> locationData) {
		LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
		for (final Map.Entry<Integer, String> line : locationData.entrySet()) {
			final TextViewEx button = buildButtonInCollapsableView(mapActivity, false, false);
			if (line.getKey() == OsmAndFormatter.UTM_FORMAT || line.getKey() == OsmAndFormatter.OLC_FORMAT || line.getKey() == OsmAndFormatter.MGRS_FORMAT) {
				SpannableStringBuilder ssb = new SpannableStringBuilder();
				if (line.getKey() == OsmAndFormatter.UTM_FORMAT) {
					ssb.append("UTM: ");
				} else if (line.getKey() == OsmAndFormatter.MGRS_FORMAT) {
					ssb.append("MGRS: ");
				} else if (line.getKey() == OsmAndFormatter.OLC_FORMAT) {
					ssb.append("OLC: ");
				}
				ssb.setSpan(new ForegroundColorSpan(app.getResources().getColor(R.color.text_color_secondary_light)), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				ssb.append(line.getValue());
				button.setText(ssb);
			} else {
				button.setText(line.getValue());
			}
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					copyToClipboard(line.getValue(), mapActivity);
				}
			});
			llv.addView(button);
		}
		return new CollapsableView(llv, this, true);

	}

	protected CollapsableView getDistanceCollapsableView(Set<String> distanceData) {
		LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
		for (final String distance : distanceData) {
			TextView button = buildButtonInCollapsableView(mapActivity, false, false);
			button.setText(distance);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					copyToClipboard(distance, mapActivity);
				}
			});
			llv.addView(button);
		}
		return new CollapsableView(llv, this, true);
	}

	public void buildRowDivider(View view) {
		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		if (!matchWidthDivider) {
			AndroidUtils.setMargins(llHorLineParams, dpToPx(64f), 0, 0, 0);
		}
		horizontalLine.setLayoutParams(llHorLineParams);
		horizontalLine.setBackgroundColor(app.getResources().getColor(light ? R.color.ctx_menu_bottom_view_divider_light : R.color.ctx_menu_bottom_view_divider_dark));
		((LinearLayout) view).addView(horizontalLine);
	}

	protected void buildReadFullButton(LinearLayout container, String btnText, View.OnClickListener onClickListener) {
		Context ctx = container.getContext();

		TextViewEx button = new TextViewEx(new ContextThemeWrapper(ctx, light ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme));
		LinearLayout.LayoutParams llButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(36f));
		AndroidUtils.setMargins(llButtonParams, dpToPx(16f), 0, 0, dpToPx(16f));
		button.setLayoutParams(llButtonParams);
		button.setTypeface(FontCache.getRobotoMedium(app));
		button.setBackgroundResource(light ? R.drawable.context_menu_controller_bg_light : R.drawable.context_menu_controller_bg_dark);
		button.setTextSize(14);
		int paddingSides = dpToPx(10f);
		button.setPadding(paddingSides, 0, paddingSides, 0);
		ColorStateList buttonColorStateList = AndroidUtils.createPressedColorStateList(ctx, !light,
				R.color.ctx_menu_controller_button_text_color_light_n, R.color.ctx_menu_controller_button_text_color_light_p,
				R.color.ctx_menu_controller_button_text_color_dark_n, R.color.ctx_menu_controller_button_text_color_dark_p);
		button.setTextColor(buttonColorStateList);
		button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		button.setSingleLine(true);
		button.setEllipsize(TextUtils.TruncateAt.END);
		button.setOnClickListener(onClickListener);
		button.setAllCaps(true);
		button.setText(btnText);
		Drawable normal = app.getUIUtilities().getIcon(R.drawable.ic_action_read_text,
				light ? R.color.ctx_menu_controller_button_text_color_light_n : R.color.ctx_menu_controller_button_text_color_dark_n);
		Drawable pressed = app.getUIUtilities().getIcon(R.drawable.ic_action_read_text,
				light ? R.color.ctx_menu_controller_button_text_color_light_p : R.color.ctx_menu_controller_button_text_color_dark_p);
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(button, Build.VERSION.SDK_INT >= 21
				? AndroidUtils.createPressedStateListDrawable(normal, pressed) : normal, null, null, null);
		button.setCompoundDrawablePadding(dpToPx(8f));
		container.addView(button);
	}

	protected void buildDateRow(View view, long timestamp) {
		DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(view.getContext());
		DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(view.getContext());
		Date date = new Date(timestamp);
		buildRow(view, R.drawable.ic_action_data, null, dateFormat.format(date) + " — " + timeFormat.format(date),
				0, false, null, false, 0, false, null, false);
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

	public Drawable getRowIcon(int iconId) {
		UiUtilities iconsCache = app.getUIUtilities();
		return iconsCache.getIcon(iconId, light ? R.color.ctx_menu_bottom_view_icon_light : R.color.ctx_menu_bottom_view_icon_dark);
	}

	public Drawable getThemedIcon(int iconId) {
		return app.getUIUtilities().getThemedIcon(iconId);
	}

	public Drawable getRowIcon(Context ctx, String fileName) {
		Drawable d = RenderingIcons.getBigIcon(ctx, fileName);
		if (d != null) {
			d = DrawableCompat.wrap(d);
			d.mutate();
			d.setColorFilter(app.getResources().getColor(light
					? R.color.ctx_menu_bottom_view_icon_light : R.color.ctx_menu_bottom_view_icon_dark), PorterDuff.Mode.SRC_IN);
			return d;
		} else {
			return null;
		}
	}

	public int dpToPx(float dp) {
		return AndroidUtils.dpToPx(app, dp);
	}

	public Drawable getCollapseIcon(boolean collapsed) {
		return app.getUIUtilities().getIcon(collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up,
				light ? R.color.ctx_menu_collapse_icon_color_light : R.color.ctx_menu_collapse_icon_color_dark);
	}

	private View buildTransportRowItem(View view, TransportStopRoute route, OnClickListener listener) {
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
		transportRect.setTypeface(FontCache.getRobotoMedium(view.getContext()));
		transportRect.setTextColor(Color.WHITE);
		transportRect.setTextSize(10);
		transportRect.setMaxLines(1);

		GradientDrawable shape = new GradientDrawable();
		shape.setShape(GradientDrawable.RECTANGLE);
		shape.setCornerRadius(dpToPx(3));
		int bgColor = route.getColor(app, !light);
		shape.setColor(bgColor);
		transportRect.setTextColor(UiUtilities.getContrastColor(app, bgColor, true));

		transportRect.setBackgroundDrawable(shape);
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
		int textColor = app.getResources().getColor(light ? R.color.text_color_primary_light : R.color.text_color_primary_dark);
		titleView.setTextColor(textColor);
		String desc = route.getDescription(getMapActivity().getMyApplication(), true);
		Drawable arrow = app.getUIUtilities().getIcon(R.drawable.ic_arrow_right_16, light ? R.color.ctx_menu_route_icon_color_light : R.color.ctx_menu_route_icon_color_dark);
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

		return baseView;
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

	private CollapsableView getCollapsableTransportStopRoutesView(final Context context, boolean collapsed, boolean isNearbyRoutes) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, collapsed, false);
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
			final TransportStopRoute r = routes.get(i);
			boolean showDivider = i < routes.size() - 1;
			buildTransportRouteRow(view, r, createTransportRoutesViewClickListener(r), showDivider);
		}
	}

	private View.OnClickListener createTransportRoutesViewClickListener(final TransportStopRoute r) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				MapContextMenu mm = getMapActivity().getContextMenu();
				PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_ROUTE,
						r.getDescription(getMapActivity().getMyApplication(), false));
				mm.show(latLon, pd, r);
				TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
				stopsLayer.setRoute(r);
				int cz = r.calculateZoom(0, getMapActivity().getMapView().getCurrentRotatedTileBox());
				getMapActivity().changeZoom(cz - getMapActivity().getMapView().getZoom());
			}
		};
	}

	protected CollapsableView getCollapsableTextView(Context context, boolean collapsed, String text) {
		final TextViewEx textView = new TextViewEx(context);
		textView.setVisibility(collapsed ? View.GONE : View.VISIBLE);
		LinearLayout.LayoutParams llTextDescParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llTextDescParams, dpToPx(64f), 0, dpToPx(40f), dpToPx(13f));
		textView.setLayoutParams(llTextDescParams);
		textView.setTypeface(FontCache.getRobotoRegular(context));
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
		textView.setText(text);
		return new CollapsableView(textView, this, collapsed);
	}

	protected CollapsableView getCollapsableView(Context context, boolean collapsed, List<Amenity> nearestAmenities, String nearestPoiType) {
		LinearLayout view = buildCollapsableContentView(context, collapsed, true);

		for (final Amenity poi : nearestAmenities) {
			TextViewEx button = buildButtonInCollapsableView(context, false, false);
			final PointDescription pointDescription = mapActivity.getMapLayers().getPoiMapLayer().getObjectName(poi);
			String name = pointDescription.getName();
			if (Algorithms.isBlank(name)) {
				name = AmenityMenuController.getTypeStr(poi);
			}
			float dist = (float) MapUtils.getDistance(latLon, poi.getLocation());
			name += " (" + OsmAndFormatter.getFormattedDistance(dist, app) + ")";
			button.setText(name);

			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					LatLon latLon = new LatLon(poi.getLocation().getLatitude(), poi.getLocation().getLongitude());
					mapActivity.getContextMenu().show(latLon, pointDescription, poi);
					mapActivity.setMapLocation(poi.getLocation().getLatitude(), poi.getLocation().getLongitude());
				}
			});
			view.addView(button);
		}
		PoiUIFilter filter = getPoiFilterForType(nearestPoiType);
		if (filter != null) {
			if (nearestAmenities.size() >= NEARBY_MAX_POI_COUNT) {
				view.addView(createShowOnMap(context, filter));
			}
			view.addView(createSearchMoreButton(context, filter));
		}
		return new CollapsableView(view, this, collapsed);
	}

	private View createSearchMoreButton(Context context, final PoiUIFilter filter) {
		TextViewEx buttonShowAll = buildButtonInCollapsableView(context, false, false);
		buttonShowAll.setText(app.getString(R.string.search_more));
		buttonShowAll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.showQuickSearch(filter, latLon);
			}
		});
		return buttonShowAll;
	}

	private View createShowOnMap(Context context, final PoiUIFilter filter) {
		TextViewEx buttonShowAll = buildButtonInCollapsableView(context, false, false);
		buttonShowAll.setText(app.getString(R.string.shared_string_show_on_map));
		buttonShowAll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final PoiFiltersHelper poiFiltersHelper = app.getPoiFilters();
				poiFiltersHelper.clearSelectedPoiFilters();
				poiFiltersHelper.addSelectedPoiFilter(filter);
				final QuickSearchToolbarController controller = new QuickSearchToolbarController();
				controller.setTitle(filter.getName());
				controller.setOnBackButtonClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mapContextMenu != null) {
							mapContextMenu.show();
						}
					}
				});
				controller.setOnTitleClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mapActivity.showQuickSearch(filter);
					}
				});
				controller.setOnCloseButtonClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						poiFiltersHelper.clearSelectedPoiFilters();
						mapActivity.hideTopToolbar(controller);
						mapActivity.refreshMap();
					}
				});
				mapContextMenu.hideMenues();
				mapActivity.showTopToolbar(controller);
				mapActivity.refreshMap();
			}
		});
		return buttonShowAll;
	}

	protected LinearLayout buildCollapsableContentView(Context context, boolean collapsed, boolean needMargin) {
		final LinearLayout view = new LinearLayout(context);
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
		TextViewEx button = new TextViewEx(new ContextThemeWrapper(context, light ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme));
		LinearLayout.LayoutParams llWikiButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(llWikiButtonParams, 0, 0, 0, dpToPx(8f));
		//button.setMinimumHeight(dpToPx(36f));
		button.setLayoutParams(llWikiButtonParams);
		button.setTypeface(FontCache.getRobotoRegular(context));
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
			button.setTextColor(ContextCompat.getColor(context, light ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
		}
		button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		button.setSingleLine(singleLine);
		button.setEllipsize(TextUtils.TruncateAt.END);

		return button;
	}

	protected void buildNearestWikiRow(SearchAmenitiesListener listener) {
		if (showNearestWiki && latLon != null && amenity != null) {
			PoiUIFilter filter = app.getPoiFilters().getTopWikiPoiFilter();
			if (filter != null) {
				searchSortedAmenities(filter, latLon, listener);
			}
		}
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
