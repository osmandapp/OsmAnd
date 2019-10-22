package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;
import net.osmand.plus.mapcontextmenu.builders.cards.CardsRowBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask;
import net.osmand.plus.mapcontextmenu.builders.cards.NoImagesCard;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;

public class MenuBuilder {

	public static final float SHADOW_HEIGHT_TOP_DP = 17f;
	public static final int TITLE_LIMIT = 60;
	protected static final String[] arrowChars = new String[]{"=>"," - "};

	protected MapActivity mapActivity;
	protected MapContextMenu mapContextMenu;
	protected OsmandApplication app;
	protected LinkedList<PlainMenuItem> plainMenuItems;
	private boolean firstRow;
	protected boolean matchWidthDivider;
	protected boolean light;
	private long objectId;
	private LatLon latLon;
	private boolean hidden;
	private boolean showTitleIfTruncated = true;
	private boolean showNearestWiki = false;
	private boolean showOnlinePhotos = true;
	protected List<Amenity> nearestWiki = new ArrayList<>();
	private List<OsmandPlugin> menuPlugins = new ArrayList<>();
	@Nullable
	private CardsRowBuilder onlinePhotoCardsRow;
	private List<AbstractCard> onlinePhotoCards;

	private CollapseExpandListener collapseExpandListener;

	private String preferredMapLang;
	private String preferredMapAppLang;
	private boolean transliterateNames;

	public interface CollapseExpandListener {
		void onCollapseExpand(boolean collapsed);
	}

	public class PlainMenuItem {
		private int iconId;
		private String buttonText;
		private String text;
		private boolean needLinks;
		private boolean url;
		private boolean collapsable;
		private CollapsableView collapsableView;
		private OnClickListener onClickListener;

		public PlainMenuItem(int iconId, String buttonText, String text, boolean needLinks, boolean url,
							 boolean collapsable, CollapsableView collapsableView,
							 OnClickListener onClickListener) {
			this.iconId = iconId;
			this.buttonText = buttonText;
			this.text = text;
			this.needLinks = needLinks;
			this.url = url;
			this.collapsable = collapsable;
			this.collapsableView = collapsableView;
			this.onClickListener = onClickListener;
		}

		public int getIconId() {
			return iconId;
		}

		public String getButtonText() {
			return buttonText;
		}

		public String getText() {
			return text;
		}

		public boolean isNeedLinks() {
			return needLinks;
		}

		public boolean isUrl() {
			return url;
		}

		public boolean isCollapsable() {
			return collapsable;
		}

		public CollapsableView getCollapsableView() {
			return collapsableView;
		}

		public OnClickListener getOnClickListener() {
			return onClickListener;
		}
	}

	public static class CollapsableView {

		private View contenView;
		private MenuBuilder menuBuilder;
		private OsmandPreference<Boolean> collapsedPref;
		private boolean collapsed;
		private CollapseExpandListener collapseExpandListener;

		public CollapsableView(@NonNull View contenView, MenuBuilder menuBuilder,
							   @NonNull OsmandPreference<Boolean> collapsedPref) {
			this.contenView = contenView;
			this.menuBuilder = menuBuilder;
			this.collapsedPref = collapsedPref;
		}

		public CollapsableView(@NonNull View contenView, MenuBuilder menuBuilder, boolean collapsed) {
			this.contenView = contenView;
			this.collapsed = collapsed;
			this.menuBuilder = menuBuilder;
		}

		public View getContenView() {
			return contenView;
		}

		public boolean isCollapsed() {
			if (collapsedPref != null) {
				return collapsedPref.get();
			} else {
				return collapsed;
			}
		}

		public void setCollapsed(boolean collapsed) {
			if (collapsedPref != null) {
				collapsedPref.set(collapsed);
			} else {
				this.collapsed = collapsed;
			}
			if (collapseExpandListener != null) {
				collapseExpandListener.onCollapseExpand(collapsed);
			}
			if (menuBuilder != null && menuBuilder.collapseExpandListener != null) {
				menuBuilder.collapseExpandListener.onCollapseExpand(collapsed);
			}
		}

		public CollapseExpandListener getCollapseExpandListener() {
			return collapseExpandListener;
		}

		public void setCollapseExpandListener(CollapseExpandListener collapseExpandListener) {
			this.collapseExpandListener = collapseExpandListener;
		}
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

	public void setShowNearestWiki(boolean showNearestWiki) {
		this.showNearestWiki = showNearestWiki;
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

	public void setShowNearestWiki(boolean showNearestWiki, long objectId) {
		this.objectId = objectId;
		this.showNearestWiki = showNearestWiki;
	}

	public void addMenuPlugin(OsmandPlugin plugin) {
		menuPlugins.add(plugin);
	}

	public void setLight(boolean light) {
		this.light = light;
	}

	public void build(View view) {
		firstRow = true;
		hidden = false;
		buildTopInternal(view);
		if (showTitleIfTruncated) {
			buildTitleRow(view);
		}
		buildNearestWikiRow(view);
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
			buildRow(view, item.getIconId(), item.getButtonText(), item.getText(), 0, item.collapsable, item.collapsableView,
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

	protected void buildNearestWikiRow(View view) {
		if (processNearstWiki() && nearestWiki.size() > 0) {
			buildRow(view, R.drawable.ic_action_wikipedia, null, app.getString(R.string.wiki_around) + " (" + nearestWiki.size()+")", 0,
					true, getCollapsableWikiView(view.getContext(), true),
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
		CollapsableView collapsableView = new CollapsableView(onlinePhotoCardsRow.getContentView(), this,
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

	private void buildCoordinatesRow(View view) {
		Map<Integer, String> locationData = PointDescription.getLocationData(mapActivity, latLon.getLatitude(), latLon.getLongitude(), true);
		String title = locationData.get(PointDescription.LOCATION_LIST_HEADER);
		locationData.remove(PointDescription.LOCATION_LIST_HEADER);
		CollapsableView cv = getLocationCollapsableView(locationData);
		buildRow(view, R.drawable.ic_action_get_my_location, null, title, 0, true, cv, false, 1,
			false, null, false);
	}
	
	private void startLoadingImages() {
		if (onlinePhotoCardsRow == null) {
			return;
		}
		onlinePhotoCards = new ArrayList<>();
		onlinePhotoCardsRow.setProgressCard();
		execute(new GetImageCardsTask(mapActivity, getLatLon(), getAdditionalCardParams(),
				new GetImageCardsListener() {
					@Override
					public void onPostProcess(List<ImageCard> cardList) {
						processOnlinePhotosCards(cardList);
					}

					@Override
					public void onFinish(List<ImageCard> cardList) {
						if (!isHidden()) {
							List<AbstractCard> cards = new ArrayList<>();
							cards.addAll(cardList);
							if (cardList.size() == 0) {
								cards.add(new NoImagesCard(mapActivity));
							}
							if (onlinePhotoCardsRow != null) {
								onlinePhotoCardsRow.setCards(cards);
							}
							onlinePhotoCards = cards;
						}
					}
				}));
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

	protected void buildDescription(View view){
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
			llIconParams.setMargins(dpToPx(16f), dpToPx(12f), dpToPx(24f), dpToPx(12f));
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
		llTextViewParams.setMargins(0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		ll.addView(llText);

		TextViewEx textPrefixView = null;
		if (!Algorithms.isEmpty(textPrefix)) {
			textPrefixView = new TextViewEx(view.getContext());
			LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			llTextParams.setMargins(icon == null ? dpToPx(16f) : 0, dpToPx(8f), 0, 0);
			textPrefixView.setLayoutParams(llTextParams);
			textPrefixView.setTypeface(FontCache.getRobotoRegular(view.getContext()));
			textPrefixView.setTextSize(12);
			textPrefixView.setTextColor(app.getResources().getColor(light ? R.color.text_color_secondary_light: R.color.text_color_secondary_dark));
			textPrefixView.setMinLines(1);
			textPrefixView.setMaxLines(1);
			textPrefixView.setText(textPrefix);
			llText.addView(textPrefixView);
		}

		// Primary text
		TextViewEx textView = new TextViewEx(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(icon != null ? 0 : dpToPx(16f), dpToPx(textPrefixView != null ? 2f : (secondaryText != null ? 10f : 8f)), 0, dpToPx(secondaryText != null ? 6f : 8f));
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
			llTextSecondaryParams.setMargins(icon != null ? 0 : dpToPx(16f), 0, 0, dpToPx(6f));
			textViewSecondary.setLayoutParams(llTextSecondaryParams);
			textViewSecondary.setTypeface(FontCache.getRobotoRegular(view.getContext()));
			textViewSecondary.setTextSize(14);
			textViewSecondary.setTextColor(app.getResources().getColor(light ? R.color.text_color_secondary_light: R.color.text_color_secondary_dark));
			textViewSecondary.setText(secondaryText);
			llText.addView(textViewSecondary);
		}

		//Button
		if (!TextUtils.isEmpty(buttonText)) {
			TextViewEx buttonTextView = new TextViewEx(view.getContext());
			LinearLayout.LayoutParams buttonTextViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			buttonTextViewParams.gravity = Gravity.CENTER_VERTICAL;
			buttonTextViewParams.setMargins(dpToPx(8), 0, dpToPx(8), 0);
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
			llIconCollapseParams.setMargins(0, dpToPx(12f), dpToPx(24f), dpToPx(12f));
			llIconCollapseParams.gravity = Gravity.CENTER_VERTICAL;
			iconViewCollapse.setLayoutParams(llIconCollapseParams);
			iconViewCollapse.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconViewCollapse.setImageDrawable(getCollapseIcon(collapsableView.getContenView().getVisibility() == View.GONE));
			llIconCollapse.addView(iconViewCollapse);
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (collapsableView.getContenView().getVisibility() == View.VISIBLE) {
						collapsableView.getContenView().setVisibility(View.GONE);
						iconViewCollapse.setImageDrawable(getCollapseIcon(true));
						collapsableView.setCollapsed(true);
					} else {
						collapsableView.getContenView().setVisibility(View.VISIBLE);
						iconViewCollapse.setImageDrawable(getCollapseIcon(false));
						collapsableView.setCollapsed(false);
					}
				}
			});
			if (collapsableView.isCollapsed()) {
				collapsableView.getContenView().setVisibility(View.GONE);
				iconViewCollapse.setImageDrawable(getCollapseIcon(true));
			}
			if (collapsableView.getContenView().getParent() != null) {
				((ViewGroup) collapsableView.getContenView().getParent())
					.removeView(collapsableView.getContenView());
			}
			baseView.addView(collapsableView.getContenView());
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

	public View buildDescriptionRow(final View view, final String textPrefix, final String description, int textColor,
	                                int textLinesLimit, boolean matchWidthDivider) {
		OnClickListener clickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				POIMapLayer.showDescriptionDialog(view.getContext(), app, description, textPrefix);
			}
		};

		return buildRow(view, null, null, textPrefix, description, textColor,
				null, false, null, true, textLinesLimit,
				false, false, false, clickListener, matchWidthDivider);
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
		((ClipboardManager) app.getSystemService(Activity.CLIPBOARD_SERVICE)).setText(text);
		Toast.makeText(ctx,
				ctx.getResources().getString(R.string.copied_to_clipboard) + ":\n" + text,
				Toast.LENGTH_SHORT).show();
	}

	protected CollapsableView getLocationCollapsableView(Map<Integer, String> locationData) {
		LinearLayout llv = buildCollapsableContentView(mapActivity, true, true);
		for (final Map.Entry<Integer, String> line : locationData.entrySet()) {
			final TextViewEx button = buildButtonInCollapsableView(mapActivity, false, false);
			if (line.getKey() == OsmAndFormatter.UTM_FORMAT || line.getKey() == OsmAndFormatter.OLC_FORMAT) {
				SpannableStringBuilder ssb = new SpannableStringBuilder();
				ssb.append(line.getKey() == OsmAndFormatter.UTM_FORMAT ? "UTM: " : "OLC: ");
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

	protected void buildButtonRow(final View view, Drawable buttonIcon, String text, OnClickListener onClickListener) {
		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));

		// Empty
		LinearLayout llIcon = new LinearLayout(view.getContext());
		llIcon.setOrientation(LinearLayout.HORIZONTAL);
		llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(62f), dpToPx(58f)));
		llIcon.setGravity(Gravity.CENTER_VERTICAL);
		ll.addView(llIcon);


		// Button
		LinearLayout llButton = new LinearLayout(view.getContext());
		llButton.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llButton);

		Button buttonView = new Button(view.getContext());
		LinearLayout.LayoutParams llBtnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		buttonView.setLayoutParams(llBtnParams);
		buttonView.setPadding(dpToPx(10f), 0, dpToPx(10f), 0);
		buttonView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		//buttonView.setTextSize(view.getResources().getDimension(resolveAttribute(view.getContext(), R.dimen.default_desc_text_size)));
		buttonView.setTextColor(view.getResources().getColor(AndroidUtils.resolveAttribute(view.getContext(), R.attr.contextMenuButtonColor)));
		buttonView.setText(text);

		if (buttonIcon != null) {
			buttonView.setCompoundDrawablesWithIntrinsicBounds(buttonIcon, null, null, null);
			buttonView.setCompoundDrawablePadding(dpToPx(8f));
		}
		llButton.addView(buttonView);

		((LinearLayout) view).addView(ll);

		ll.setOnClickListener(onClickListener);

		rowBuilt();
	}

	public void buildRowDivider(View view) {
		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		if (!matchWidthDivider) {
			llHorLineParams.setMargins(dpToPx(64f), 0, 0, 0);
		}
		horizontalLine.setLayoutParams(llHorLineParams);
		horizontalLine.setBackgroundColor(app.getResources().getColor(light ? R.color.ctx_menu_bottom_view_divider_light : R.color.ctx_menu_bottom_view_divider_dark));
		((LinearLayout) view).addView(horizontalLine);
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
		baseView.setPadding(dpToPx(16), 0, dpToPx(16), dpToPx(12));
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));

		TextViewEx transportRect = new TextViewEx(view.getContext());
		LinearLayout.LayoutParams trParams = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(18));
		trParams.setMargins(0, dpToPx(16), 0, 0);
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
		infoViewLayoutParams.setMargins(dpToPx(16), dpToPx(12), dpToPx(16), 0);
		infoView.setLayoutParams(infoViewLayoutParams);
		baseView.addView(infoView);

		TextView titleView = new TextView(view.getContext());
		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		titleView.setLayoutParams(titleParams);
		titleView.setTextSize(16);
		titleView.setTextColor(app.getResources().getColor(light ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
		String desc = route.getDescription(getMapActivity().getMyApplication(), true);
		Drawable arrow = app.getUIUtilities().getIcon(R.drawable.ic_arrow_right_16, light ? R.color.ctx_menu_route_icon_color_light : R.color.ctx_menu_route_icon_color_dark);
		arrow.setBounds(0, 0, arrow.getIntrinsicWidth(), arrow.getIntrinsicHeight());

		titleView.setText(AndroidUtils.replaceCharsWithIcon(desc, arrow, arrowChars));
		infoView.addView(titleView);

		LinearLayout typeView = new LinearLayout(view.getContext());
		typeView.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams typeViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		typeViewParams.setMargins(0, dpToPx(8), 0, 0);
		typeView.setGravity(Gravity.CENTER);
		typeView.setLayoutParams(typeViewParams);
		infoView.addView(typeView);

		ImageView typeImageView = new ImageView(view.getContext());
		LinearLayout.LayoutParams typeImageParams = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
		typeImageParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
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
		llTextDescParams.setMargins(dpToPx(64f), 0, dpToPx(40f), dpToPx(13f));
		textView.setLayoutParams(llTextDescParams);
		textView.setTypeface(FontCache.getRobotoRegular(context));
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
		textView.setText(text);
		return new CollapsableView(textView, this, collapsed);
	}

	protected CollapsableView getCollapsableWikiView(Context context, boolean collapsed) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, collapsed, true);

		for (final Amenity wiki : nearestWiki) {
			TextViewEx button = buildButtonInCollapsableView(context, false, false);
			String name = wiki.getName(preferredMapAppLang, transliterateNames);
			button.setText(name);

			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					LatLon latLon = new LatLon(wiki.getLocation().getLatitude(), wiki.getLocation().getLongitude());
					PointDescription pointDescription = mapActivity.getMapLayers().getPoiMapLayer().getObjectName(wiki);
					mapActivity.getContextMenu().show(latLon, pointDescription, wiki);
				}
			});
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}

	protected LinearLayout buildCollapsableContentView(Context context, boolean collapsed, boolean needMargin) {
		final LinearLayout view = new LinearLayout(context);
		view.setOrientation(LinearLayout.VERTICAL);
		view.setVisibility(collapsed ? View.GONE : View.VISIBLE);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		if (needMargin) {
			llParams.setMargins(dpToPx(64f), 0, dpToPx(12f), 0);
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
		llWikiButtonParams.setMargins(0, 0, 0, dpToPx(8f));
		//button.setMinimumHeight(dpToPx(36f));
		button.setLayoutParams(llWikiButtonParams);
		button.setTypeface(FontCache.getRobotoRegular(context));
		int bg;
		if (selected) {
			bg = light ? R.drawable.context_menu_controller_bg_light_selected: R.drawable.context_menu_controller_bg_dark_selected;
		} else if (showAll) {
			bg = light ? R.drawable.context_menu_controller_bg_light_show_all : R.drawable.context_menu_controller_bg_dark_show_all;
		} else {
			bg = light ? R.drawable.context_menu_controller_bg_light : R.drawable.context_menu_controller_bg_dark;
		}
		button.setBackgroundResource(bg);
		button.setTextSize(14);
		int paddingSides = dpToPx(10f);
		button.setPadding(paddingSides, paddingSides, paddingSides, paddingSides);
		if (!selected) {
			ColorStateList buttonColorStateList = AndroidUtils.createPressedColorStateList(context, !light,
					R.color.ctx_menu_controller_button_text_color_light_n, R.color.ctx_menu_controller_button_text_color_light_p,
					R.color.ctx_menu_controller_button_text_color_dark_n, R.color.ctx_menu_controller_button_text_color_dark_p);
			button.setTextColor(buttonColorStateList);
		} else {
			button.setTextColor(ContextCompat.getColor(context, light ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
		}
		button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		button.setSingleLine(singleLine);
		button.setEllipsize(TextUtils.TruncateAt.END);

		return button;
	}

	protected boolean processNearstWiki() {
		if (showNearestWiki && latLon != null) {
			QuadRect rect = MapUtils.calculateLatLonBbox(
					latLon.getLatitude(), latLon.getLongitude(), 250);
			nearestWiki = app.getResourceManager().searchAmenities(
					new BinaryMapIndexReader.SearchPoiTypeFilter() {
						@Override
						public boolean accept(PoiCategory type, String subcategory) {
							return type != null && type.isWiki();
						}

						@Override
						public boolean isEmpty() {
							return false;
						}
					}, rect.top, rect.left, rect.bottom, rect.right, -1, null);
			Collections.sort(nearestWiki, new Comparator<Amenity>() {

				@Override
				public int compare(Amenity o1, Amenity o2) {
					double d1 = MapUtils.getDistance(latLon, o1.getLocation());
					double d2 = MapUtils.getDistance(latLon, o2.getLocation());
					return Double.compare(d1, d2);
				}
			});
			Long id = objectId;
			List<Amenity> wikiList = new ArrayList<>();
			for (Amenity wiki : nearestWiki) {
				String lng = wiki.getContentLanguage("content", preferredMapAppLang, "en");
				if (wiki.getId().equals(id) || (!lng.equals("en") && !lng.equals(preferredMapAppLang))) {
					wikiList.add(wiki);
				}
			}
			nearestWiki.removeAll(wikiList);
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static <P> void execute(AsyncTask<P, ?, ?> task, P... requests) {
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requests);
	}
}
