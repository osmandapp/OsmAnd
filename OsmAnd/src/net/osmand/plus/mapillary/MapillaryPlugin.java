package net.osmand.plus.mapillary;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuBuilder.CollapsableView;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;
import net.osmand.plus.mapcontextmenu.builders.cards.CardsRowBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MapillaryPlugin extends OsmandPlugin {
	public static final String ID = "osmand.mapillary";
	private OsmandSettings settings;
	private OsmandApplication app;

	private MapillaryLayer rasterLayer;
	private TextInfoWidget mapillaryControl;
	private MapWidgetRegInfo mapillaryWidgetRegInfo;
	private CardsRowBuilder contextMenuCardsRow;
	private List<AbstractCard> contextMenuCards;

	public MapillaryPlugin(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_mapillary;
	}

	@Override
	public int getAssetResourceName() {
		return R.drawable.online_maps;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.mapillary);
	}

	@Override
	public String getName() {
		return app.getString(R.string.mapillary);
	}

	@Override
	public void registerLayers(MapActivity activity) {
		createLayers();
		registerWidget(activity);
	}

	private void createLayers() {
		rasterLayer = new MapillaryLayer();
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		updateMapLayers(mapView, activity.getMapLayers());
	}

	public void updateMapLayers(OsmandMapTileView mapView, final MapActivityLayers layers) {
		if (rasterLayer == null) {
			createLayers();
		}
		if (isActive()) {
			updateLayer(mapView, rasterLayer, 0.6f);
		} else {
			mapView.removeLayer(rasterLayer);
			rasterLayer.setMap(null);
		}
		layers.updateMapSource(mapView, null);
	}

	public void updateLayer(OsmandMapTileView mapView, MapTileLayer layer, float layerOrder) {
		ITileSource mapillarySource = null;
		if (settings.SHOW_MAPILLARY.get()) {
			mapillarySource = settings.getTileSourceByName(TileSourceManager.getMapillarySource().getName(), false);
		}
		if (!Algorithms.objectEquals(mapillarySource, layer.getMap())) {
			if (mapillarySource == null) {
				mapView.removeLayer(layer);
			} else if (mapView.getMapRenderer() == null) {
				mapView.addLayer(layer, layerOrder);
			}
			layer.setMap(mapillarySource);
			mapView.refreshMap();
		}
	}

	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}

	@Override
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView,
												ContextMenuAdapter adapter,
												final MapActivity mapActivity) {
		ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.OnRowItemClick() {

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter, int itemId, final int pos, boolean isChecked) {
				final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
				if (itemId == R.string.mapillary) {
					OsmandMapTileView mapView = mapActivity.getMapView();
					MapActivityLayers mapLayers = mapActivity.getMapLayers();
					settings.SHOW_MAPILLARY.set(!settings.SHOW_MAPILLARY.get());
					updateMapLayers(mapView, mapLayers);
					ContextMenuItem item = adapter.getItem(pos);
					if (item != null) {
						item.setSelected(settings.SHOW_MAPILLARY.get());
						item.setColorRes(settings.SHOW_MAPILLARY.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						adapter.notifyDataSetChanged();
					}
				}
				return false;
			}
		};

		if (rasterLayer.getMap() == null) {
			settings.SHOW_MAPILLARY.set(false);
		}
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.mapillary, mapActivity)
				.setSelected(settings.SHOW_MAPILLARY.get())
				.setColor(settings.SHOW_MAPILLARY.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_mapillary)
				.setListener(listener)
				.setPosition(11)
				.createItem());
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
		mapillaryControl = createMonitoringControl(activity);
		mapillaryWidgetRegInfo = layer.registerSideWidget(mapillaryControl,
				R.drawable.ic_action_mapillary, R.string.mapillary, "mapillary", false, 19);
		layer.recreateControls();
	}

	private TextInfoWidget createMonitoringControl(final MapActivity map) {
		mapillaryControl = new TextInfoWidget(map);
		mapillaryControl.setText("", map.getString(R.string.mapillary));
		mapillaryControl.setIcons(R.drawable.widget_mapillary_day, R.drawable.widget_mapillary_night);
		mapillaryControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// todo open mapillary app
			}
		});

		return mapillaryControl;
	}

	public void setWidgetVisible(MapActivity mapActivity, boolean visible) {
		if (mapillaryWidgetRegInfo != null) {
			final List<ApplicationMode> allModes = ApplicationMode.allPossibleValues();
			for (ApplicationMode mode : allModes) {
				mapActivity.getMapLayers().getMapWidgetRegistry().setVisibility(mode, mapillaryWidgetRegInfo, visible, false);
			}
			MapInfoLayer mil = mapActivity.getMapLayers().getMapInfoLayer();
			if (mil != null) {
				mil.recreateControls();
			}
			mapActivity.refreshMap();
		}
	}

	@Override
	public void buildContextMenuRows(@NonNull final MenuBuilder menuBuilder, @NonNull View view) {
		if (!menuBuilder.getApplication().getSettings().isInternetConnectionAvailable()) {
			return;
		}

		boolean needUpdateOnly = contextMenuCardsRow != null && contextMenuCardsRow.getMenuBuilder() == menuBuilder;
		contextMenuCardsRow = new CardsRowBuilder(menuBuilder, view);
		contextMenuCardsRow.build();
		CollapsableView collapsableView = new CollapsableView(contextMenuCardsRow.getViewPager(),
				app.getSettings().MAPILLARY_MENU_COLLAPSED);
		collapsableView.setOnCollExpListener(new CollapsableView.OnCollExpListener() {
			@Override
			public void onCollapseExpand(boolean collapsed) {
				if (!collapsed && contextMenuCards == null) {
					startLoadingImages(menuBuilder);
				}
			}
		});
		menuBuilder.buildRow(view, R.drawable.ic_action_photo_dark, "Online photos", 0, true,
				collapsableView, false, 1, false, null);

		if (needUpdateOnly && contextMenuCards != null) {
			contextMenuCardsRow.setCards(contextMenuCards);
		} else if (!collapsableView.isCollapsed()) {
			startLoadingImages(menuBuilder);
		}
	}

	private void startLoadingImages(final MenuBuilder menuBuilder) {
		contextMenuCards = new ArrayList<>();
		contextMenuCardsRow.setProgressCard();
		ImageCard.execute(new GetImageCardsTask<>(
				new MapillaryImageCard.MapillaryImageCardFactory(),
				app, menuBuilder.getLatLon(),
				new GetImageCardsTask.Listener<MapillaryImageCard>() {
					@Override
					public void onFinish(List<MapillaryImageCard> cardList) {
						if (!menuBuilder.isHidden()) {
							List<AbstractCard> cards = new ArrayList<>();
							cards.addAll(cardList);
							cards.add(new AddMapillaryPhotoCard());
							contextMenuCardsRow.setCards(cards);
							contextMenuCards = cards;
						}
					}
				}));
	}

	@Override
	public void clearContextMenuRows() {
		contextMenuCardsRow = null;
		contextMenuCards = null;
	}

	public static class MapillaryFirstDialogFragment extends BottomSheetDialogFragment {
		public static final String TAG = "MapillaryFirstDialogFragment";

		private static final String KEY_SHOW_WIDGET = "key_show_widget";
		private boolean showWidget = true;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			if (savedInstanceState != null) {
				showWidget = savedInstanceState.getBoolean(KEY_SHOW_WIDGET, true);
			}

			View view = inflater.inflate(R.layout.mapillary_first_dialog, container, false);
			final SwitchCompat widgetSwitch = (SwitchCompat) view.findViewById(R.id.widget_switch);
			widgetSwitch.setChecked(showWidget);
			widgetSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					showWidget(isChecked);
				}
			});
			view.findViewById(R.id.actionButton).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showWidget(widgetSwitch.isChecked());
					dismiss();
				}
			});
			return view;
		}

		private void showWidget(boolean show) {
			FragmentActivity activity = getActivity();
			MapillaryPlugin plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
			if (plugin != null && activity instanceof MapActivity) {
				plugin.setWidgetVisible((MapActivity) activity, show);
			}
		}

		@Override
		public void onSaveInstanceState(@NonNull Bundle outState) {
			outState.putBoolean(KEY_SHOW_WIDGET, showWidget);
		}
	}
}
