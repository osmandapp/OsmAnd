package net.osmand.plus.dialogs;

import static net.osmand.plus.wikivoyage.data.TravelGpx.ACTIVITY_TYPE;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.ReloadIndexesTask;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.render.TravelRendererHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.render.RenderingRulesStorage;

import java.util.List;

public class TravelRoutesFragment extends BaseOsmAndFragment {

	public static final String TAG = TravelRoutesFragment.class.getSimpleName();
	private static final String TRAVEL_TYPE_KEY = "travel_type_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private TravelRendererHelper rendererHelper;

	private List<PoiSubType> routesTypes;

	private TravelType travelType = TravelType.ROUTE_TYPES;
	private boolean nightMode;

	private enum TravelType {
		ROUTE_TYPES(R.string.travel_route_types),
		TRAVEL_FILES(R.string.shared_string_files);

		@StringRes
		public int titleRes;

		TravelType(int titleRes) {
			this.titleRes = titleRes;
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		rendererHelper = app.getTravelRendererHelper();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		routesTypes = app.getResourceManager().searchPoiSubTypesByPrefix(ACTIVITY_TYPE);

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
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.travel_routes_fragment, container, false);

		setupHeader(view);
		setupPrefItems(view);
		setupTypeRadioGroup(view);
		setupBottomEmptySpace(view);

		return view;
	}

	private void setupHeader(@NonNull View view) {
		View itemView = view.findViewById(R.id.header);
		updateItemView(itemView, getString(R.string.travel_routes), settings.SHOW_TRAVEL.get());
		itemView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean selected = !settings.SHOW_TRAVEL.get();
				settings.SHOW_TRAVEL.set(selected);

				setupHeader(view);
				setupPrefItems(view);
				setupTypeRadioGroup(view);

				MapActivity mapActivity = (MapActivity) getMyActivity();
				if (mapActivity != null) {
					mapActivity.refreshMapComplete();
					mapActivity.getMapLayers().updateLayers(mapActivity);
				}
			}
		});
	}

	private void updateItemView(View itemView, String name, boolean selected) {
		TextView title = itemView.findViewById(R.id.title);
		ImageView icon = itemView.findViewById(R.id.icon);
		TextView description = itemView.findViewById(R.id.description);

		ApplicationMode mode = settings.getApplicationMode();
		int selectedColor = mode.getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(itemView.getContext(), R.attr.default_icon_color);

		title.setText(name);
		icon.setImageDrawable(getPaintedContentIcon(mode.getIconRes(), selected ? selectedColor : disabledColor));
		description.setText(selected ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		CompoundButton button = itemView.findViewById(R.id.toggle_item);
		button.setClickable(false);
		button.setFocusable(false);
		button.setChecked(selected);
		UiUtilities.setupCompoundButton(nightMode, selectedColor, button);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.divider), false);
	}

	private void setupTypeRadioGroup(@NonNull View view) {
		boolean selected = settings.SHOW_TRAVEL.get();
		LinearLayout buttonsContainer = view.findViewById(R.id.custom_radio_buttons);
		if (selected) {
			TextRadioItem routes = createRadioButton(TravelType.ROUTE_TYPES);
			TextRadioItem files = createRadioButton(TravelType.TRAVEL_FILES);

			TextToggleButton radioGroup = new TextToggleButton(app, buttonsContainer, nightMode);
			radioGroup.setItems(routes, files);
			radioGroup.setSelectedItem(travelType == TravelType.ROUTE_TYPES ? routes : files);
		}
		AndroidUiHelper.updateVisibility(buttonsContainer, selected);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.space), selected);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.header_divider), selected);
	}

	private TextRadioItem createRadioButton(final TravelType type) {
		TextRadioItem item = new TextRadioItem(app.getString(travelType.titleRes));
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
			if (travelType == TravelType.TRAVEL_FILES) {
				setupTravelFiles(container);
			} else if (travelType == TravelType.ROUTE_TYPES) {
				setupRouteTypes(container);
			}
		}
		AndroidUiHelper.updateVisibility(container, selected);
	}

	private void setupTravelFiles(ViewGroup container) {
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);

		OsmandRegions regions = app.getRegions();
		for (BinaryMapIndexReader reader : app.getResourceManager().getTravelMapRepositories()) {
			String fileName = reader.getFile().getName();
			CommonPreference<Boolean> pref = rendererHelper.getFileProperty(fileName);

			String title = FileNameTranslationHelper.getFileName(app, regions, fileName);
			View itemView = inflater.inflate(R.layout.list_item_icon_and_menu, null, false);
			AndroidUtils.setBackground(itemView, UiUtilities.getSelectableDrawable(app));

			updateItemView(itemView, title, pref.get());
			itemView.setOnClickListener(v -> {
				boolean selected = !pref.get();
				pref.set(selected);
				updateItemView(itemView, title, pref.get());

				rendererHelper.updateFileVisibility(fileName, selected);
				new ReloadIndexesTask(app, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

				MapActivity mapActivity = (MapActivity) getMyActivity();
				if (mapActivity != null) {
					mapActivity.refreshMapComplete();
					mapActivity.getMapLayers().updateLayers(mapActivity);
				}
			});
			container.addView(itemView);
		}
	}

	private void setupRouteTypes(ViewGroup container) {
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);
		for (PoiSubType type : routesTypes) {
			CommonPreference<Boolean> pref = rendererHelper.getRouteTypeProperty(type.name);
			View itemView = inflater.inflate(R.layout.list_item_icon_and_menu, null, false);
			AndroidUtils.setBackground(itemView, UiUtilities.getSelectableDrawable(app));
			String attrName = type.name.replace(ACTIVITY_TYPE + "_", "");

			updateItemView(itemView, attrName, pref.get());
			itemView.setOnClickListener(v -> {
				boolean selected = !pref.get();
				pref.set(selected);
				updateItemView(itemView, attrName, pref.get());

				RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
				rendererHelper.updateRouteTypeVisibility(storage, attrName, selected);

				MapActivity mapActivity = (MapActivity) getMyActivity();
				if (mapActivity != null) {
					mapActivity.refreshMapComplete();
					mapActivity.getMapLayers().updateLayers(mapActivity);
				}
			});
			container.addView(itemView);
		}
	}

	private void setupBottomEmptySpace(@NonNull View view) {
		View bottomView = view.findViewById(R.id.bottom_empty_space);
		int height = AndroidUtils.getScreenHeight(requireActivity()) - getResources().getDimensionPixelSize(R.dimen.dashboard_map_top_padding);
		ViewGroup.LayoutParams params = bottomView.getLayoutParams();
		params.height = height;
		bottomView.setLayoutParams(params);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new TravelRoutesFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}