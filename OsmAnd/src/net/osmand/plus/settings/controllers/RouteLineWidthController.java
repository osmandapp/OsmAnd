package net.osmand.plus.settings.controllers;

import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;
import static net.osmand.util.Algorithms.parseIntSilently;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnSliderTouchListener;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.base.slider.moded.ModedSliderCard;
import net.osmand.plus.card.width.WidthComponentController;
import net.osmand.plus.card.width.WidthMode;
import net.osmand.plus.card.width.data.WidthStyle;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RouteLineWidthController extends BaseMultiStateCardController implements IDialogController {

	public static final String PROCESS_ID = "select_route_line_width";

	private final WidthStyle DEFAULT_STYLE = new WidthStyle(null, R.string.map_widget_renderer);

	private static final int DEFAULT_STYLE_CARD_ID = 0;
	private static final int WIDTH_COMPONENT_CARD_ID = 1;

	private static final int CUSTOM_WIDTH_MIN = 1;
	private static final int CUSTOM_WIDTH_MAX = 36;

	private final OsmandApplication app;
	private final PreviewRouteLineInfo routeLinePreview;
	private final List<WidthStyle> supportedWidthStyles;
	private WidthStyle selectedWidthStyle;

	private WidthComponentController widthComponentController;
	private OnNeedScrollListener onNeedScrollListener;
	private IRouteLineWidthControllerListener listener;
	private boolean nightMode;

	public RouteLineWidthController(@NonNull OsmandApplication app,
	                                @NonNull PreviewRouteLineInfo routeLinePreview) {
		this.app = app;
		this.routeLinePreview = routeLinePreview;
		this.supportedWidthStyles = collectSupportedWidthStyles();
		this.selectedWidthStyle = findWidthStyle(routeLinePreview.getWidth());
	}

	public void setListener(@NonNull IRouteLineWidthControllerListener listener) {
		this.listener = listener;
	}

	public void setOnNeedScrollListener(@NonNull OnNeedScrollListener onNeedScrollListener) {
		this.onNeedScrollListener = onNeedScrollListener;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_width);
	}

	@NonNull
	@Override
	public String getSelectorTitle() {
		return selectedWidthStyle.toHumanString(app);
	}

	@NonNull
	@Override
	public List<PopUpMenuItem> getPopUpMenuItems() {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (WidthStyle widthStyle : supportedWidthStyles) {
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitle(widthStyle.toHumanString(app))
					.setTitleColor(getPrimaryTextColor(app, nightMode))
					.setTag(widthStyle)
					.create()
			);
		}
		return menuItems;
	}

	@Override
	public void onPopUpMenuItemSelected(@NonNull FragmentActivity activity,
	                                    @NonNull View view, @NonNull PopUpMenuItem item) {
		WidthStyle widthStyle = (WidthStyle) item.getTag();
		String widthValue = getWidthValueOfStyle(widthStyle);
		onWidthValueSelected(widthValue);
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity,
	                              @NonNull ViewGroup container, boolean nightMode) {
		this.nightMode = nightMode;
		if (isMapStyle(selectedWidthStyle)) {
			bindSummaryCard(activity, container, nightMode);
		} else {
			bindWidthComponentCardIfNeeded(activity, container);
		}
	}

	private void bindSummaryCard(@NonNull FragmentActivity activity,
	                             @NonNull ViewGroup container, boolean nightMode) {
		container.removeAllViews();
		LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
		inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);

		String pattern = app.getString(R.string.route_line_use_map_style_width);
		String rendererName = app.getRendererRegistry().getSelectedRendererName();
		String summary = String.format(pattern, rendererName);
		DescriptionCard descriptionCard = new DescriptionCard(activity, summary);
		container.addView(descriptionCard.build(activity));
		container.setTag(DEFAULT_STYLE_CARD_ID);
	}

	private void bindWidthComponentCardIfNeeded(@NonNull FragmentActivity activity, @NonNull ViewGroup container) {
		WidthComponentController controller = getWidthComponentController();
		// We only create and bind "Width Component" card only if it wasn't attached before
		// or if there is other card visible at the moment.
		Integer cardId = (Integer) container.getTag();
		if (cardId == null || cardId == DEFAULT_STYLE_CARD_ID) {
			container.removeAllViews();
			ModedSliderCard widthComponentCard = new ModedSliderCard(activity, controller);
			container.addView(widthComponentCard.build(activity));

			View widthSliderContainer = widthComponentCard.getSliderContainer();
			ScrollUtils.addOnGlobalLayoutListener(widthSliderContainer, () -> {
				if (widthSliderContainer.getVisibility() == View.VISIBLE) {
					onNeedScrollListener.onVerticalScrollNeeded(widthSliderContainer.getBottom());
				}
			});

			// Hide direction arrows in OpenGL while slider is touched
			Slider widthSlider = widthComponentCard.getSlider();
			widthSlider.addOnSliderTouchListener(new OnSliderTouchListener() {

				@Override
				public void onStartTrackingTouch(@NonNull Slider slider) {
					boolean hasMapRenderer = app.getOsmandMap().getMapView().hasMapRenderer();
					routeLinePreview.setShowDirectionArrows(!hasMapRenderer);
				}

				@Override
				public void onStopTrackingTouch(@NonNull Slider slider) {
					routeLinePreview.setShowDirectionArrows(true);
				}
			});
		}
		controller.askSelectWidthMode(getWidthValueOfStyle(selectedWidthStyle));
		container.setTag(WIDTH_COMPONENT_CARD_ID);
	}

	@Override
	public boolean shouldShowCardHeader() {
		return true;
	}

	private void onWidthValueSelected(@Nullable String widthValue) {
		setRouteLineWidth(widthValue);
		selectedWidthStyle = findWidthStyle(widthValue);
		cardInstance.updateSelectedCardState();
		listener.onRouteLineWidthSelected(widthValue);
	}

	private void setRouteLineWidth(String width) {
		routeLinePreview.setWidth(width);
		app.getOsmandMap().getMapView().refreshMap();
	}

	@NonNull
	public WidthComponentController getWidthComponentController() {
		if (widthComponentController == null) {
			String selectedWidth = routeLinePreview.getWidth();
			WidthMode widthMode = WidthMode.valueOfKey(selectedWidth);
			int customValue = parseIntSilently(selectedWidth, CUSTOM_WIDTH_MIN);
			widthComponentController = new WidthComponentController(widthMode, customValue, this::onWidthValueSelected) {
				@NonNull
				@Override
				public Limits getSliderLimits() {
					return new Limits(CUSTOM_WIDTH_MIN, CUSTOM_WIDTH_MAX);
				}
			};
		}
		return widthComponentController;
	}

	@NonNull
	private List<WidthStyle> collectSupportedWidthStyles() {
		List<WidthStyle> result = new ArrayList<>();
		result.add(new WidthStyle(null, R.string.map_widget_renderer));
		for (WidthMode widthMode : WidthMode.values()) {
			result.add(new WidthStyle(widthMode.getKey(), widthMode.getTitleId()));
		}
		return result;
	}

	@NonNull
	private WidthStyle findWidthStyle(@Nullable String width) {
		if (width != null) {
			WidthMode widthMode = WidthMode.valueOfKey(width);
			for (WidthStyle widthStyle : supportedWidthStyles) {
				if (Objects.equals(widthStyle.getKey(), widthMode.getKey())) {
					return widthStyle;
				}
			}
		}
		return DEFAULT_STYLE;
	}

	@Nullable
	private String getWidthValueOfStyle(@NonNull WidthStyle widthStyle) {
		WidthComponentController controller = getWidthComponentController();
		return isCustomValue(widthStyle) ? controller.getSelectedCustomValue() : widthStyle.getKey();
	}

	private boolean isMapStyle(@NonNull WidthStyle widthStyle) {
		return Objects.equals(DEFAULT_STYLE, widthStyle);
	}

	private boolean isCustomValue(@NonNull WidthStyle widthStyle) {
		return Objects.equals(widthStyle.getKey(), WidthMode.CUSTOM.getKey());
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
	}

	public interface IRouteLineWidthControllerListener {
		void onRouteLineWidthSelected(@Nullable String width);
	}

	@NonNull
	public static RouteLineWidthController getInstance(
			@NonNull OsmandApplication app,
			@NonNull PreviewRouteLineInfo routeLinePreview,
			@NonNull OnNeedScrollListener onNeedScrollListener,
			@NonNull IRouteLineWidthControllerListener listener
	) {
		DialogManager dialogManager = app.getDialogManager();
		RouteLineWidthController controller = (RouteLineWidthController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new RouteLineWidthController(app, routeLinePreview);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setOnNeedScrollListener(onNeedScrollListener);
		controller.setListener(listener);
		return controller;
	}
}
