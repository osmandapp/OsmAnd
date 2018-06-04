package net.osmand.plus.dialogs;


import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.quickaction.QuickActionFactory;
import net.osmand.plus.quickaction.actions.MapStyleAction;
import net.osmand.plus.quickaction.actions.MapSourceAction;
import net.osmand.plus.quickaction.actions.MapOverlayAction;
import net.osmand.plus.quickaction.actions.MapUnderlayAction;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SelectMapViewQuickActionsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectMapViewQuickActionsBottomSheet.class.getSimpleName();

	private static final String SELECTED_ITEM_KEY = "selected_item";
	private static final String LAYER_OSM_VECTOR = "LAYER_OSM_VECTOR";
	private static final String KEY_NO_OVERLAY = "no_overlay";
	private static final String KEY_NO_UNDERLAY = "no_underlay";
	private static final String MAP = "map";

	private LinearLayout stylesContainer;
	private View.OnClickListener onClickListener;
	private ColorStateList rbColorList;
	private OsmandSettings settings;

	private ArrayList<String> stylesList;
	private HashMap<String, String> pairsMap;
	private String selectedItem;
	private int type;

	@Override
	@SuppressWarnings("unchecked")
	public void createMenuItems(Bundle savedInstanceState) {
		final Context context = getContext();
		if (context == null) {
			return;
		}
		if (getArguments() == null) {
			return;
		}
		Bundle args = getArguments();

		type = args.getInt("type");
		if (type == MapStyleAction.TYPE) {
			stylesList = args.getStringArrayList("test");
		} else {
			pairsMap = (HashMap<String, String>) args.getSerializable(MAP);
		}
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		settings = app.getSettings();
		if (Algorithms.isEmpty(stylesList) && Algorithms.isEmpty(pairsMap)) {
			return;
		}
		if (savedInstanceState != null) {
			selectedItem = savedInstanceState.getString(SELECTED_ITEM_KEY);
		} else {
			if (type == MapStyleAction.TYPE) {
				RenderingRulesStorage current = app.getRendererRegistry().getCurrentSelectedRenderer();
				if (current != null) {
					selectedItem = current.getName();
				} else {
					selectedItem = RendererRegistry.DEFAULT_RENDER;
				}
			} else if (type == MapSourceAction.TYPE) {
				Pair<String, String> currentPairItem = settings.MAP_ONLINE_DATA.get()
						? new Pair<>(settings.MAP_TILE_SOURCES.get(), settings.MAP_TILE_SOURCES.get())
						: new Pair<>(LAYER_OSM_VECTOR, getString(R.string.vector_data));
				selectedItem = currentPairItem.first;
			} else if (type == MapUnderlayAction.TYPE) {
				selectedItem = settings.MAP_UNDERLAY.get();
			} else if (type == MapOverlayAction.TYPE) {
				selectedItem = settings.MAP_OVERLAY.get();
			}
		}
		rbColorList = AndroidUtils.createCheckedColorStateList(context, R.color.icon_color, getActiveColorId());

		items.add(new TitleItem(getTitle()));

		NestedScrollView nestedScrollView = new NestedScrollView(context);
		stylesContainer = new LinearLayout(context);
		stylesContainer.setLayoutParams((new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)));
		stylesContainer.setOrientation(LinearLayout.VERTICAL);
		stylesContainer.setPadding(0, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small),
				0, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small));

		int itemsSize = 0;
		if (type == MapSourceAction.TYPE || type == MapUnderlayAction.TYPE || type == MapOverlayAction.TYPE) {
			itemsSize = pairsMap.size();
		} else if (type == MapStyleAction.TYPE) {
			itemsSize = stylesList.size();
		}
		for (int i = 0; i < itemsSize; i++) {
			LayoutInflater.from(new ContextThemeWrapper(context, themeRes))
					.inflate(R.layout.bottom_sheet_item_with_radio_btn, stylesContainer, true);
		}

		nestedScrollView.addView(stylesContainer);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(nestedScrollView).create());

		populateItemsList();
	}

	private String getTitle() {
		switch (type) {
			case MapOverlayAction.TYPE:
				return getString(R.string.map_overlay);
			case MapUnderlayAction.TYPE:
				return getString(R.string.map_underlay);
			case MapSourceAction.TYPE:
				return getString(R.string.map_source);
			case MapStyleAction.TYPE:
				return getString(R.string.map_widget_renderer);
		}
		return "";
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_ITEM_KEY, selectedItem);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected void onRightBottomButtonClick() {
		dismiss();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.quick_action_edit_actions;
	}

	@Override
	protected void onDismissButtonClickAction() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		if (type == MapStyleAction.TYPE) {
			changeMapStyle(mapActivity);
		} else if (type == MapSourceAction.TYPE) {
			changeMapSource(mapActivity);
		} else if (type == MapOverlayAction.TYPE) {
			changeMapOverlay(mapActivity);
		} else if (type == MapUnderlayAction.TYPE) {
			changeMapUnderlay(mapActivity);
		}
	}


	private void changeMapStyle(MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(selectedItem);
		if (loaded != null) {
			OsmandMapTileView view = mapActivity.getMapView();
			view.getSettings().RENDERER.set(selectedItem);
			app.getRendererRegistry().setCurrentSelectedRender(loaded);
			ConfigureMapMenu.refreshMapComplete(mapActivity);
			mapActivity.getDashboard().refreshContent(true);
			Toast.makeText(mapActivity, mapActivity.getString(R.string.quick_action_map_style_switch, selectedItem), Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(mapActivity, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
		}
	}

	private void changeMapSource(MapActivity mapActivity) {
		if (selectedItem.equals(LAYER_OSM_VECTOR)) {
			settings.MAP_ONLINE_DATA.set(false);
			mapActivity.getMapLayers().updateMapSource(mapActivity.getMapView(), null);
		} else {
			settings.MAP_TILE_SOURCES.set(selectedItem);
			settings.MAP_ONLINE_DATA.set(true);
			mapActivity.getMapLayers().updateMapSource(mapActivity.getMapView(), settings.MAP_TILE_SOURCES);
		}
		Toast.makeText(mapActivity, getString(R.string.quick_action_map_source_switch, pairsMap.get(selectedItem)), Toast.LENGTH_SHORT).show();
	}

	private void changeMapOverlay(MapActivity mapActivity) {
		OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
		if (plugin != null) {
			boolean hasOverlay = !selectedItem.equals(KEY_NO_OVERLAY);
			if (hasOverlay) {
				settings.MAP_OVERLAY.set(selectedItem);
				settings.MAP_OVERLAY_PREVIOUS.set(selectedItem);
			} else {
				settings.MAP_OVERLAY.set(null);
				settings.MAP_OVERLAY_PREVIOUS.set(null);
			}
			plugin.updateMapLayers(mapActivity.getMapView(), settings.MAP_OVERLAY, mapActivity.getMapLayers());
			Toast.makeText(mapActivity, getString(R.string.quick_action_map_overlay_switch, pairsMap.get(selectedItem)), Toast.LENGTH_SHORT).show();
		}
	}

	private void changeMapUnderlay(MapActivity mapActivity) {
		OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
		if (plugin != null) {
			boolean hasUnderlay = !selectedItem.equals(KEY_NO_UNDERLAY);
			if (hasUnderlay) {
				settings.MAP_UNDERLAY.set(selectedItem);
				settings.MAP_UNDERLAY_PREVIOUS.set(selectedItem);
			} else {
				settings.MAP_UNDERLAY.set(null);
				settings.MAP_UNDERLAY_PREVIOUS.set(null);
			}
			final OsmandSettings.CommonPreference<Boolean> hidePolygonsPref =
					mapActivity.getMyApplication().getSettings().getCustomRenderBooleanProperty("noPolygons");
			hidePolygonsPref.set(hasUnderlay);

			plugin.updateMapLayers(mapActivity.getMapView(), settings.MAP_UNDERLAY, mapActivity.getMapLayers());
			Toast.makeText(mapActivity, getString(R.string.quick_action_map_underlay_switch, pairsMap.get(selectedItem)), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	private void populateItemsList() {
		Context context = getContext();
		if (context == null) {
			return;
		}
		int counter = 0;
		if (type == MapStyleAction.TYPE) {
			for (String entry : stylesList) {
				boolean selected = entry.equals(selectedItem);
				createItemRow(selected, counter, entry, entry, context);
				counter++;
			}
		} else if (type == MapSourceAction.TYPE || type == MapOverlayAction.TYPE || type == MapUnderlayAction.TYPE) {
			for (Map.Entry<String, String> entry : pairsMap.entrySet()) {
				String tag = entry.getKey();
				boolean selected = tag.equals(selectedItem);
				createItemRow(selected, counter, entry.getValue(), tag, context);
				counter++;
			}
		}
	}

	private void createItemRow(boolean selected, int counter, String text, String tag, Context context) {
		View view = stylesContainer.getChildAt(counter);
		view.setTag(tag);
		view.setOnClickListener(getOnClickListener());

		TextView titleTv = (TextView) view.findViewById(R.id.title);
		titleTv.setText(text);
		titleTv.setTextColor(getStyleTitleColor(selected));

		RadioButton rb = (RadioButton) view.findViewById(R.id.compound_button);
		rb.setChecked(selected);
		CompoundButtonCompat.setButtonTintList(rb, rbColorList);
		ImageView imageView = (ImageView) view.findViewById(R.id.icon);
		imageView.setImageDrawable(((OsmandApplication) context.getApplicationContext())
				.getIconsCache().getThemedIcon(QuickActionFactory.getActionIcon(type)));
	}

	@ColorInt
	private int getStyleTitleColor(boolean selected) {
		int colorId = selected
				? getActiveColorId()
				: nightMode ? R.color.primary_text_dark : R.color.primary_text_light;
		return getResolvedColor(colorId);
	}

	private View.OnClickListener getOnClickListener() {
		if (onClickListener == null) {
			onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Context context = getContext();
					if (context == null) {
						return;
					}
					selectedItem = (String) v.getTag();
					if (type == MapStyleAction.TYPE) {
						Toast.makeText(context, selectedItem, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(context, pairsMap.get(selectedItem), Toast.LENGTH_SHORT).show();
					}
					populateItemsList();
				}
			};
		}
		return onClickListener;
	}
}