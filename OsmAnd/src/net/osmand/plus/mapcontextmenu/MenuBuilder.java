package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;
import net.osmand.plus.mapcontextmenu.builders.cards.CardsRowBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask;
import net.osmand.plus.mapcontextmenu.builders.cards.NoImagesCard;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController.TransportStopRoute;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.*;

public class MenuBuilder {

	public static final float SHADOW_HEIGHT_TOP_DP = 17f;
	public static final int TITLE_LIMIT = 60;

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
	private List<TransportStopRoute> routes = new ArrayList<>();
	private CardsRowBuilder onlinePhotoCardsRow;
	private List<AbstractCard> onlinePhotoCards;

	private String preferredMapLang;
	private String preferredMapAppLang;
	private boolean transliterateNames;

	public class PlainMenuItem {
		private int iconId;
		private String text;
		private boolean needLinks;
		private boolean url;
		private boolean collapsable;
		private CollapsableView collapsableView;
		private OnClickListener onClickListener;

		public PlainMenuItem(int iconId, String text, boolean needLinks, boolean url,
							 boolean collapsable, CollapsableView collapsableView,
							 OnClickListener onClickListener) {
			this.iconId = iconId;
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
		private OsmandPreference<Boolean> collapsedPref;
		private boolean collapsed;
		private OnCollExpListener onCollExpListener;

		public interface OnCollExpListener {
			void onCollapseExpand(boolean collapsed);
		}

		public CollapsableView(@NonNull View contenView, @NonNull OsmandPreference<Boolean> collapsedPref) {
			this.contenView = contenView;
			this.collapsedPref = collapsedPref;
		}

		public CollapsableView(@NonNull View contenView, boolean collapsed) {
			this.contenView = contenView;
			this.collapsed = collapsed;
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
			if (onCollExpListener != null) {
				onCollExpListener.onCollapseExpand(collapsed);
			}
		}

		public OnCollExpListener getOnCollExpListener() {
			return onCollExpListener;
		}

		public void setOnCollExpListener(OnCollExpListener onCollExpListener) {
			this.onCollExpListener = onCollExpListener;
		}
	}

	public MenuBuilder(MapActivity mapActivity) {
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

	public void setRoutes(List<TransportStopRoute> routes) {
		this.routes = routes;
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
		if (showTitleIfTruncated) {
			buildTitleRow(view);
		}
		if (showTransportRoutes()) {
			buildRow(view, 0, app.getString(R.string.transport_Routes), 0, true, getCollapsableTransportStopRoutesView(view.getContext(), false),
					false, 0, false, null, true);
		}
		buildNearestWikiRow(view);
		if (needBuildPlainMenuItems()) {
			buildPlainMenuItems(view);
		}
		buildInternal(view);
		if (showOnlinePhotos) {
			buildNearestPhotosRow(view);
		}
		buildPluginRows(view);
		buildAfter(view);
	}

	private boolean showTransportRoutes() {
		return routes.size() > 0;
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
			buildRow(view, item.getIconId(), item.getText(), 0, item.collapsable, item.collapsableView,
					item.isNeedLinks(), 0, item.isUrl(), item.getOnClickListener(), false);
		}
	}

	protected boolean needBuildPlainMenuItems() {
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
				buildRow(view, R.drawable.ic_action_note_dark, title, 0, false, null, false, 0, false, null, false);
			}
		}
	}

	protected void buildNearestWikiRow(View view) {
		if (processNearstWiki() && nearestWiki.size() > 0) {
			buildRow(view, R.drawable.ic_action_wikipedia, app.getString(R.string.wiki_around) + " (" + nearestWiki.size()+")", 0,
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
		CollapsableView collapsableView = new CollapsableView(onlinePhotoCardsRow.getContentView(),
				app.getSettings().ONLINE_PHOTOS_ROW_COLLAPSED);
		collapsableView.setOnCollExpListener(new CollapsableView.OnCollExpListener() {
			@Override
			public void onCollapseExpand(boolean collapsed) {
				if (!collapsed && onlinePhotoCards == null) {
					startLoadingImages();
				}
			}
		});
		buildRow(view, R.drawable.ic_action_photo_dark, app.getString(R.string.online_photos), 0, true,
				collapsableView, false, 1, false, null, false);

		if (needUpdateOnly && onlinePhotoCards != null) {
			onlinePhotoCardsRow.setCards(onlinePhotoCards);
		} else if (!collapsableView.isCollapsed()) {
			startLoadingImages();
		}
	}

	private void startLoadingImages() {
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
							onlinePhotoCardsRow.setCards(cards);
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

	protected void buildAfter(View view) {
		buildRowDivider(view);
	}

	public boolean isFirstRow() {
		return firstRow;
	}

	public void rowBuilt() {
		firstRow = false;
	}

	public View buildRow(View view, int iconId, String text, int textColor,
							boolean collapsable, final CollapsableView collapsableView,
							boolean needLinks, int textLinesLimit, boolean isUrl, OnClickListener onClickListener, boolean matchWidthDivider) {
		return buildRow(view, iconId == 0 ? null : getRowIcon(iconId), text, textColor, null, collapsable, collapsableView,
				needLinks, textLinesLimit, isUrl, onClickListener, matchWidthDivider);
	}

	public View buildRow(final View view, Drawable icon, final String text, int textColor, String secondaryText,
							boolean collapsable, final CollapsableView collapsableView, boolean needLinks,
							int textLinesLimit, boolean isUrl, OnClickListener onClickListener, boolean matchWidthDivider) {

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
		ll.setBackgroundResource(resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		ll.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(text, view.getContext());
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

		// Primary text
		TextViewEx textView = new TextViewEx(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(icon != null ? 0 : dpToPx(16f), dpToPx(secondaryText != null ? 10f : 8f), 0, dpToPx(secondaryText != null ? 6f : 8f));
		textView.setLayoutParams(llTextParams);
		textView.setTypeface(FontCache.getRobotoRegular(view.getContext()));
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_bottom_view_text_color_light : R.color.ctx_menu_bottom_view_text_color_dark));

		if (isUrl) {
			textView.setTextColor(ContextCompat.getColor(view.getContext(), light ? R.color.ctx_menu_bottom_view_url_color_light : R.color.ctx_menu_bottom_view_url_color_dark));
		} else if (needLinks) {
			textView.setAutoLinkMask(Linkify.ALL);
			textView.setLinksClickable(true);
		}
		if (textLinesLimit > 0) {
			textView.setMinLines(1);
			textView.setMaxLines(textLinesLimit);
		}
		textView.setText(text);
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
			textViewSecondary.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_bottom_view_secondary_text_color_light: R.color.ctx_menu_bottom_view_secondary_text_color_dark));
			textViewSecondary.setText(secondaryText);
			llText.addView(textViewSecondary);
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
			iconViewCollapse.setImageDrawable(app.getIconsCache().getIcon(collapsableView.getContenView().getVisibility() == View.GONE ?
					R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up, light ? R.color.ctx_menu_bottom_view_icon_light : R.color.ctx_menu_bottom_view_icon_dark));
			llIconCollapse.addView(iconViewCollapse);
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (collapsableView.getContenView().getVisibility() == View.VISIBLE) {
						collapsableView.setCollapsed(true);
						collapsableView.getContenView().setVisibility(View.GONE);
						iconViewCollapse.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_arrow_down, light ? R.color.ctx_menu_bottom_view_icon_light : R.color.ctx_menu_bottom_view_icon_dark));
					} else {
						collapsableView.setCollapsed(false);
						collapsableView.getContenView().setVisibility(View.VISIBLE);
						iconViewCollapse.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_arrow_up, light ? R.color.ctx_menu_bottom_view_icon_light : R.color.ctx_menu_bottom_view_icon_dark));
					}
				}
			});
			if (collapsableView.isCollapsed()) {
				collapsableView.getContenView().setVisibility(View.GONE);
				iconViewCollapse.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_arrow_down, light ? R.color.ctx_menu_bottom_view_icon_light : R.color.ctx_menu_bottom_view_icon_dark));
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
		}

		((LinearLayout) view).addView(baseView);

		rowBuilt();

		setDividerWidth(matchWidthDivider);

		return ll;
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

	protected void buildButtonRow(final View view, Drawable buttonIcon, String text, OnClickListener onClickListener) {
		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));

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
		buttonView.setTextColor(view.getResources().getColor(resolveAttribute(view.getContext(), R.attr.contextMenuButtonColor)));
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
		plainMenuItems.add(new PlainMenuItem(iconId, text, needLinks, isUrl, false, null, onClickListener));
	}

	public void addPlainMenuItem(int iconId, String text, boolean needLinks, boolean isUrl,
								 boolean collapsable, CollapsableView collapsableView,
								 OnClickListener onClickListener) {
		plainMenuItems.add(new PlainMenuItem(iconId, text, needLinks, isUrl, collapsable, collapsableView, onClickListener));
	}

	public void clearPlainMenuItems() {
		plainMenuItems.clear();
	}

	public Drawable getRowIcon(int iconId) {
		IconsCache iconsCache = app.getIconsCache();
		return iconsCache.getIcon(iconId, light ? R.color.ctx_menu_bottom_view_icon_light : R.color.ctx_menu_bottom_view_icon_dark);
	}

	public Drawable getRowIcon(Context ctx, String fileName) {
		Drawable d = RenderingIcons.getBigIcon(ctx, fileName);
		if (d != null) {
			d.setColorFilter(app.getResources().getColor(light ? R.color.ctx_menu_bottom_view_icon_light : R.color.ctx_menu_bottom_view_icon_dark), PorterDuff.Mode.SRC_IN);
			return d;
		} else {
			return null;
		}
	}

	public int resolveAttribute(Context ctx, int attribute) {
		TypedValue outValue = new TypedValue();
		ctx.getTheme().resolveAttribute(attribute, outValue, true);
		return outValue.resourceId;
	}

	public int dpToPx(float dp) {
		Resources r = app.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	private void buildTransportRouteRow(ViewGroup parent, TransportStopRoute r, OnClickListener listener) {
		if (!isFirstRow()) {
			buildRowDivider(parent);
		}

		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ctx_menu_transport_route_layout, parent, false);
		TextView routeDesc = (TextView) view.findViewById(R.id.route_desc);
		routeDesc.setText(r.getDescription(getMapActivity().getMyApplication(), true));
		routeDesc.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_bottom_view_text_color_light : R.color.ctx_menu_bottom_view_text_color_dark));
		int drawableResId = r.type == null ? R.drawable.ic_action_polygom_dark : r.type.getResourceId();
		((ImageView) view.findViewById(R.id.route_type_icon)).setImageDrawable(getRowIcon(drawableResId));
		((TextView) view.findViewById(R.id.route_ref)).setText(r.route.getRef());
		view.setOnClickListener(listener);
		int typeResId;
		switch (r.type) {
			case BUS:
				typeResId = R.string.poi_route_bus_ref;
				break;
			case TRAM:
				typeResId = R.string.poi_route_tram_ref;
				break;
			case FERRY:
				typeResId = R.string.poi_route_ferry_ref;
				break;
			case TRAIN:
				typeResId = R.string.poi_route_train_ref;
				break;
			case SHARE_TAXI:
				typeResId = R.string.poi_route_share_taxi_ref;
				break;
			case FUNICULAR:
				typeResId = R.string.poi_route_funicular_ref;
				break;
			case LIGHT_RAIL:
				typeResId = R.string.poi_route_light_rail_ref;
				break;
			case MONORAIL:
				typeResId = R.string.poi_route_monorail_ref;
				break;
			case TROLLEYBUS:
				typeResId = R.string.poi_route_trolleybus_ref;
				break;
			case RAILWAY:
				typeResId = R.string.poi_route_railway_ref;
				break;
			case SUBWAY:
				typeResId = R.string.poi_route_subway_ref;
				break;
			default:
				typeResId = R.string.poi_filter_public_transport;
				break;
		}
		((TextView) view.findViewById(R.id.route_type_text)).setText(typeResId);

		parent.addView(view);

		rowBuilt();
	}

	private CollapsableView getCollapsableTransportStopRoutesView(final Context context, boolean collapsed) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, collapsed, false);

		for (final TransportStopRoute r : routes) {
			View.OnClickListener listener = new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					MapContextMenu mm = getMapActivity().getContextMenu();
					PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_ROUTE,
							r.getDescription(getMapActivity().getMyApplication(), false));
					mm.show(latLon, pd, r);
					TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
					stopsLayer.setRoute(r.route);
					int cz = r.calculateZoom(0, getMapActivity().getMapView().getCurrentRotatedTileBox());
					getMapActivity().changeZoom(cz - getMapActivity().getMapView().getZoom());
				}
			};
			buildTransportRouteRow(view, r, listener);
		}

		return new CollapsableView(view, collapsed);
	}

	protected CollapsableView getCollapsableTextView(Context context, boolean collapsed, String text) {
		final TextViewEx textView = new TextViewEx(context);
		textView.setVisibility(collapsed ? View.GONE : View.VISIBLE);
		LinearLayout.LayoutParams llTextDescParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextDescParams.setMargins(dpToPx(64f), 0, dpToPx(40f), dpToPx(13f));
		textView.setLayoutParams(llTextDescParams);
		textView.setTypeface(FontCache.getRobotoRegular(context));
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_bottom_view_text_color_light : R.color.ctx_menu_bottom_view_text_color_dark));
		textView.setText(text);
		return new CollapsableView(textView, collapsed);
	}

	protected CollapsableView getCollapsableFavouritesView(final Context context, boolean collapsed, @NonNull final FavoriteGroup group, FavouritePoint selectedPoint) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, collapsed, true);

		List<FavouritePoint> points = group.points;
		for (int i = 0; i < points.size() && i < 10; i++) {
			final FavouritePoint point = points.get(i);
			boolean selected = selectedPoint != null && selectedPoint.equals(point);
			TextViewEx button = buildButtonInCollapsableView(context, selected, false);
			String name = point.getName();
			button.setText(name);

			if (!selected) {
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getName());
						mapActivity.getContextMenu().show(latLon, pointDescription, point);
					}
				});
			}
			view.addView(button);
		}

		if (points.size() > 10) {
			TextViewEx button = buildButtonInCollapsableView(context, false, true);
			button.setText(context.getString(R.string.shared_string_show_all));
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					OsmAndAppCustomization appCustomization = app.getAppCustomization();
					final Intent intent = new Intent(context, appCustomization.getFavoritesActivity());
					intent.putExtra(FavoritesActivity.OPEN_FAVOURITES_TAB, true);
					intent.putExtra(FavoritesActivity.GROUP_NAME_TO_SHOW, group.name);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					context.startActivity(intent);
				}
			});
			view.addView(button);
		}

		return new CollapsableView(view, collapsed);
	}

	protected CollapsableView getCollapsableWaypointsView(final Context context, boolean collapsed, @NonNull final GPXFile gpxFile, WptPt selectedPoint) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, collapsed, true);

		List<WptPt> points = gpxFile.getPoints();
		for (int i = 0; i < points.size() && i < 10; i++) {
			final WptPt point = points.get(i);
			boolean selected = selectedPoint != null && selectedPoint.equals(point);
			TextViewEx button = buildButtonInCollapsableView(context, selected, false);
			button.setText(point.name);

			if (!selected) {
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_WPT, point.name);
						mapActivity.getContextMenu().show(latLon, pointDescription, point);
					}
				});
			}
			view.addView(button);
		}

		if (points.size() > 10) {
			TextViewEx button = buildButtonInCollapsableView(context, false, true);
			button.setText(context.getString(R.string.shared_string_show_all));
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					OsmAndAppCustomization appCustomization = app.getAppCustomization();
					final Intent intent = new Intent(context, appCustomization.getTrackActivity());
					intent.putExtra(TrackActivity.TRACK_FILE_NAME, gpxFile.path);
					intent.putExtra(TrackActivity.OPEN_POINTS_TAB, true);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					context.startActivity(intent);
				}
			});
			view.addView(button);
		}

		return new CollapsableView(view, collapsed);
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

		return new CollapsableView(view, collapsed);
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
		TextViewEx button = new TextViewEx(new ContextThemeWrapper(context, light ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme));
		LinearLayout.LayoutParams llWikiButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(36f));
		llWikiButtonParams.setMargins(0, 0, 0, dpToPx(8f));
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
		button.setPadding(paddingSides, 0, paddingSides, 0);
		if (!selected) {
			ColorStateList buttonColorStateList = new ColorStateList(
					new int[][] {
							new int[]{android.R.attr.state_pressed},
							new int[]{}
					},
					new int[] {
							context.getResources().getColor(light ? R.color.ctx_menu_controller_button_text_color_light_p : R.color.ctx_menu_controller_button_text_color_dark_p),
							context.getResources().getColor(light ? R.color.ctx_menu_controller_button_text_color_light_n : R.color.ctx_menu_controller_button_text_color_dark_n)
					}
			);
			button.setTextColor(buttonColorStateList);
		} else {
			button.setTextColor(ContextCompat.getColor(context, light ? R.color.ctx_menu_bottom_view_text_color_light : R.color.ctx_menu_bottom_view_text_color_dark));
		}
		button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		button.setSingleLine(true);
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
							return type.isWiki();
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
