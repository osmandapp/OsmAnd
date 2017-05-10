package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.AppCompatButton;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
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
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;
import net.osmand.plus.mapcontextmenu.builders.cards.CardsRowBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.mapcontextmenu.builders.cards.NoImagesCard;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class MenuBuilder {

	public static final float SHADOW_HEIGHT_TOP_DP = 17f;

	protected MapActivity mapActivity;
	protected OsmandApplication app;
	protected LinkedList<PlainMenuItem> plainMenuItems;
	private boolean firstRow;
	protected boolean light;
	private long objectId;
	private LatLon latLon;
	private boolean hidden;
	private boolean showNearestWiki = false;
	protected List<Amenity> nearestWiki = new ArrayList<>();
	private List<OsmandPlugin> menuPlugins = new ArrayList<>();
	private CardsRowBuilder onlinePhotoCardsRow;
	private List<AbstractCard> onlinePhotoCards;

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

	public boolean isShowNearestWiki() {
		return showNearestWiki;
	}

	public void setShowNearestWiki(boolean showNearestWiki) {
		this.showNearestWiki = showNearestWiki;
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
		buildNearestWikiRow(view);
		if (needBuildPlainMenuItems()) {
			buildPlainMenuItems(view);
		}
		buildInternal(view);
		buildNearestPhotosRow(view);
		buildPluginRows(view);
		buildAfter(view);
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
					item.isNeedLinks(), 0, item.isUrl(), item.getOnClickListener());
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

	protected void buildNearestWikiRow(View view) {
		if (processNearstWiki() && nearestWiki.size() > 0) {
			buildRow(view, R.drawable.ic_action_wikipedia, app.getString(R.string.wiki_around) + " (" + nearestWiki.size()+")", 0,
					true, getCollapsableWikiView(view.getContext(), true),
					false, 0, false, null);
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
				app.getSettings().MAPILLARY_MENU_COLLAPSED);
		collapsableView.setOnCollExpListener(new CollapsableView.OnCollExpListener() {
			@Override
			public void onCollapseExpand(boolean collapsed) {
				if (!collapsed && onlinePhotoCards == null) {
					startLoadingImages(MenuBuilder.this);
				}
			}
		});
		buildRow(view, R.drawable.ic_action_photo_dark, app.getString(R.string.online_photos), 0, true,
				collapsableView, false, 1, false, null);

		if (needUpdateOnly && onlinePhotoCards != null) {
			onlinePhotoCardsRow.setCards(onlinePhotoCards);
		} else if (!collapsableView.isCollapsed()) {
			startLoadingImages(this);
		}
	}

	private void startLoadingImages(final MenuBuilder menuBuilder) {
		onlinePhotoCards = new ArrayList<>();
		onlinePhotoCardsRow.setProgressCard();
		execute(new GetImageCardsTask(mapActivity, menuBuilder.getLatLon(),
				new GetImageCardsListener() {
					@Override
					public void onFinish(List<ImageCard> cardList) {
						if (!menuBuilder.isHidden()) {
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

	protected void buildInternal(View view) {
	}

	protected void buildAfter(View view) {
		buildRowDivider(view, false);
	}

	public boolean isFirstRow() {
		return firstRow;
	}

	public void rowBuilt() {
		firstRow = false;
	}

	public View buildRow(View view, int iconId, String text, int textColor,
							boolean collapsable, final CollapsableView collapsableView,
							boolean needLinks, int textLinesLimit, boolean isUrl, OnClickListener onClickListener) {
		return buildRow(view, getRowIcon(iconId), text, textColor, collapsable, collapsableView,
				needLinks, textLinesLimit, isUrl, onClickListener);
	}

	public View buildRow(final View view, Drawable icon, final String text, int textColor,
							boolean collapsable, final CollapsableView collapsableView, boolean needLinks,
							int textLinesLimit, boolean isUrl, OnClickListener onClickListener) {

		if (!isFirstRow()) {
			buildRowDivider(view, false);
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
		LinearLayout llIcon = new LinearLayout(view.getContext());
		llIcon.setOrientation(LinearLayout.HORIZONTAL);
		llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(72f), dpToPx(48f)));
		llIcon.setGravity(Gravity.CENTER_VERTICAL);
		ll.addView(llIcon);

		ImageView iconView = new ImageView(view.getContext());
		LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
		llIconParams.setMargins(dpToPx(16f), dpToPx(12f), dpToPx(32f), dpToPx(12f));
		llIconParams.gravity = Gravity.CENTER_VERTICAL;
		iconView.setLayoutParams(llIconParams);
		iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		iconView.setImageDrawable(icon);
		llIcon.addView(iconView);

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textView = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(0, dpToPx(8f), 0, dpToPx(8f));
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_info_text_light : R.color.ctx_menu_info_text_dark));

		if (isUrl) {
			textView.setTextColor(textView.getLinkTextColors());
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

		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.weight = 1f;
		llTextViewParams.setMargins(0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		llText.addView(textView);

		final ImageView iconViewCollapse = new ImageView(view.getContext());
		if (collapsable && collapsableView != null) {
			// Icon
			LinearLayout llIconCollapse = new LinearLayout(view.getContext());
			llIconCollapse.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40f), dpToPx(48f)));
			llIconCollapse.setOrientation(LinearLayout.HORIZONTAL);
			llIconCollapse.setGravity(Gravity.CENTER_VERTICAL);
			ll.addView(llIconCollapse);

			LinearLayout.LayoutParams llIconCollapseParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
			llIconCollapseParams.setMargins(0, dpToPx(12f), dpToPx(32f), dpToPx(12f));
			llIconCollapseParams.gravity = Gravity.CENTER_VERTICAL;
			iconViewCollapse.setLayoutParams(llIconCollapseParams);
			iconViewCollapse.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconViewCollapse.setImageDrawable(app.getIconsCache().getThemedIcon(collapsableView.getContenView().getVisibility() == View.GONE ?
					R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up));
			llIconCollapse.addView(iconViewCollapse);
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (collapsableView.getContenView().getVisibility() == View.VISIBLE) {
						collapsableView.setCollapsed(true);
						collapsableView.getContenView().setVisibility(View.GONE);
						iconViewCollapse.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_arrow_down));
					} else {
						collapsableView.setCollapsed(false);
						collapsableView.getContenView().setVisibility(View.VISIBLE);
						iconViewCollapse.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_arrow_up));
					}
				}
			});
			if (collapsableView.isCollapsed()) {
				collapsableView.getContenView().setVisibility(View.GONE);
				iconViewCollapse.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_arrow_down));
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

		return ll;
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

	public void buildRowDivider(View view, boolean matchWidth) {
		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		if (!matchWidth) {
			llHorLineParams.setMargins(dpToPx(72f), 0, 0, 0);
		}
		horizontalLine.setLayoutParams(llHorLineParams);
		horizontalLine.setBackgroundColor(app.getResources().getColor(light ? R.color.ctx_menu_info_divider_light : R.color.ctx_menu_info_divider_dark));
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
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color : R.color.icon_color_light);
	}

	public Drawable getRowIcon(Context ctx, String fileName) {
		Drawable d = RenderingIcons.getBigIcon(ctx, fileName);
		if (d != null) {
			d.setColorFilter(app.getResources()
					.getColor(light ? R.color.icon_color : R.color.icon_color_light), PorterDuff.Mode.SRC_IN);
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

	protected CollapsableView getCollapsableTextView(Context context, boolean collapsed, String text) {
		final TextView textView = new TextView(context);
		textView.setVisibility(collapsed ? View.GONE : View.VISIBLE);
		LinearLayout.LayoutParams llTextDescParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextDescParams.setMargins(dpToPx(72f), 0, dpToPx(40f), dpToPx(13f));
		textView.setLayoutParams(llTextDescParams);
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_info_text_light : R.color.ctx_menu_info_text_dark));
		textView.setText(text);
		return new CollapsableView(textView, collapsed);
	}

	protected CollapsableView getCollapsableWikiView(Context context, boolean collapsed) {
		final LinearLayout view = new LinearLayout(context);
		view.setOrientation(LinearLayout.VERTICAL);
		view.setVisibility(collapsed ? View.GONE : View.VISIBLE);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llParams.setMargins(dpToPx(68f), 0, dpToPx(12f), dpToPx(13f));
		view.setLayoutParams(llParams);

		for (final Amenity wiki : nearestWiki) {
			AppCompatButton wikiButton = new AppCompatButton(
					new ContextThemeWrapper(view.getContext(), light ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme));
			LinearLayout.LayoutParams llWikiButtonParams =
					new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			wikiButton.setLayoutParams(llWikiButtonParams);
			wikiButton.setPadding(dpToPx(14f), 0, dpToPx(14f), 0);
			wikiButton.setTextColor(app.getResources()
					.getColor(light ? R.color.color_dialog_buttons_light : R.color.color_dialog_buttons_dark));
			wikiButton.setText(wiki.getName());

			wikiButton.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			wikiButton.setSingleLine(true);
			wikiButton.setEllipsize(TextUtils.TruncateAt.END);
			wikiButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					PointDescription pointDescription = mapActivity.getMapLayers().getPoiMapLayer().getObjectName(wiki);
					mapActivity.getContextMenu().show(
							new LatLon(wiki.getLocation().getLatitude(), wiki.getLocation().getLongitude()),
							pointDescription, wiki);
				}
			});
			view.addView(wikiButton);
		}

		return new CollapsableView(view, collapsed);
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
			if (id != 0) {
				for (Amenity wiki : nearestWiki) {
					if (wiki.getId().equals(id)) {
						nearestWiki.remove(wiki);
						break;
					}
				}
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static <P> void execute(AsyncTask<P, ?, ?> task, P... requests) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requests);
		} else {
			task.execute(requests);
		}
	}
}
