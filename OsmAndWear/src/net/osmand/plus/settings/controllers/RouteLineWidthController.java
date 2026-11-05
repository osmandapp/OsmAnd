package net.osmand.plus.settings.controllers;

import static net.osmand.util.Algorithms.parseIntSilently;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnSliderTouchListener;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.base.slider.moded.ModedSliderCard;
import net.osmand.plus.card.width.WidthComponentController;
import net.osmand.plus.card.width.WidthMode;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class RouteLineWidthController extends BaseMultiStateCardController implements IDialogController {

	public static final String PROCESS_ID = "select_route_line_width";

	private static final int DEFAULT_STYLE_CARD_ID = 0;
	private static final int WIDTH_COMPONENT_CARD_ID = 1;

	private static final int CUSTOM_WIDTH_MIN = 1;
	private static final int CUSTOM_WIDTH_MAX = 36;

	private PreviewRouteLineInfo routeLinePreview;

	private WidthComponentController widthComponentController;
	private OnNeedScrollListener onNeedScrollListener;
	private IRouteLineWidthControllerListener listener;

	public RouteLineWidthController(@NonNull OsmandApplication app, @Nullable String widthValue) {
		super(app);
		this.selectedState = findCardState(widthValue);
	}

	public void setListener(@NonNull IRouteLineWidthControllerListener listener) {
		this.listener = listener;
	}

	public void setOnNeedScrollListener(@NonNull OnNeedScrollListener onNeedScrollListener) {
		this.onNeedScrollListener = onNeedScrollListener;
	}

	public void setRouteLinePreview(@NonNull PreviewRouteLineInfo routeLinePreview) {
		this.routeLinePreview = routeLinePreview;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_width);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		return selectedState.getTag() == null
				? selectedState.toHumanString(app)
				: getWidthComponentController().getSummary(app);
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		String widthValue = getWidthValue(cardState);
		onWidthValueSelected(widthValue);
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity,
	                              @NonNull ViewGroup container, boolean nightMode, boolean usedOnMap) {
		if (selectedState.getTag() == null) {
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

	private void bindWidthComponentCardIfNeeded(@NonNull FragmentActivity activity,
	                                            @NonNull ViewGroup container) {
		WidthComponentController controller = getWidthComponentController();
		controller.setOnNeedScrollListener(onNeedScrollListener);
		// We only create and bind "Width Component" card only if it wasn't attached before
		// or if there is other card visible at the moment.
		Integer cardId = (Integer) container.getTag();
		if (cardId == null || cardId == DEFAULT_STYLE_CARD_ID) {
			container.removeAllViews();
			ModedSliderCard widthComponentCard = new ModedSliderCard(activity, controller);
			container.addView(widthComponentCard.build(activity));

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
		controller.askSelectWidthMode(getWidthValue(selectedState));
		container.setTag(WIDTH_COMPONENT_CARD_ID);
	}

	private void onWidthValueSelected(@Nullable String widthValue) {
		setRouteLineWidth(widthValue);
		selectedState = findCardStateByWidthValue(widthValue);
		card.updateSelectedCardState();
		listener.onRouteLineWidthSelected(widthValue);
	}

	private void setRouteLineWidth(String width) {
		routeLinePreview.setWidth(width);
		app.getOsmandMap().getMapView().refreshMap();
	}

	@NonNull
	private WidthComponentController getWidthComponentController() {
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
	@Override
	protected CardState findCardState(@Nullable Object tag) {
		if (tag instanceof String) {
			return findCardStateByWidthValue((String) tag);
		}
		return super.findCardState(tag);
	}

	@NonNull
	private CardState findCardStateByWidthValue(@Nullable String width) {
		return findCardState(width != null ? WidthMode.valueOfKey(width) : null);
	}

	@Nullable
	private String getWidthValue(@NonNull CardState cardState) {
		if (isCustomValue(cardState)) {
			WidthComponentController controller = getWidthComponentController();
			return controller.getSelectedCustomValue();
		}
		if (cardState.getTag() instanceof WidthMode) {
			return ((WidthMode) cardState.getTag()).getKey();
		}
		return null;
	}

	private boolean isCustomValue(@NonNull CardState cardState) {
		return cardState.getTag() == WidthMode.CUSTOM;
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> result = new ArrayList<>();
		result.add(new CardState(R.string.map_widget_renderer));
		for (WidthMode widthMode : WidthMode.values()) {
			result.add(new CardState(widthMode.getTitleId())
					.setShowTopDivider(widthMode.ordinal() == 0)
					.setTag(widthMode)
			);
		}
		return result;
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
			controller = new RouteLineWidthController(app, routeLinePreview.getWidth());
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setRouteLinePreview(routeLinePreview);
		controller.setOnNeedScrollListener(onNeedScrollListener);
		controller.setListener(listener);
		return controller;
	}
}
