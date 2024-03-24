package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;
import static net.osmand.util.Algorithms.parseIntSilently;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.base.slider.moded.ModedSliderCard;
import net.osmand.plus.card.color.ISelectedColorProvider;
import net.osmand.plus.card.width.WidthComponentController;
import net.osmand.plus.card.width.WidthMode;
import net.osmand.plus.card.width.data.WidthStyle;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;
import net.osmand.plus.track.fragments.controller.TrackWidthController.OnTrackWidthSelectedListener;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TracksAppearanceWidthController extends BaseMultiStateCardController {

	private final WidthStyle DEFAULT_STYLE = new WidthStyle(null, R.string.shared_string_unchanged);

	private static final int UNCHANGED_STYLE_CARD_ID = 0;
	private static final int WIDTH_COMPONENT_CARD_ID = 1;

	private static final int CUSTOM_WIDTH_MIN = 1;
	private static final int CUSTOM_WIDTH_MAX = 24;

	private final List<WidthStyle> supportedWidthStyles;
	private WidthStyle selectedWidthStyle;

	private ISelectedColorProvider colorProvider;
	private WidthComponentController widthComponentController;
	private OnNeedScrollListener onNeedScrollListener;
	private OnTrackWidthSelectedListener listener;

	public TracksAppearanceWidthController(@NonNull OsmandApplication app,
	                                       @Nullable String widthValue) {
		super(app);
		supportedWidthStyles = collectSupportedWidthStyles();
		selectedWidthStyle = findWidthStyle(widthValue);
	}

	public void setListener(@NonNull OnTrackWidthSelectedListener listener) {
		this.listener = listener;
	}

	public void setOnNeedScrollListener(@NonNull OnNeedScrollListener onNeedScrollListener) {
		this.onNeedScrollListener = onNeedScrollListener;
	}

	public void setColorProvider(@NonNull ISelectedColorProvider colorProvider) {
		this.colorProvider = colorProvider;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_width);
	}

	@NonNull
	@Override
	public String getSelectorTitle() {
		return isDefaultStyle(selectedWidthStyle)
				? selectedWidthStyle.toHumanString(app)
				: getWidthComponentController().getSummary(app);
	}

	@NonNull
	@Override
	public List<PopUpMenuItem> getPopUpMenuItems() {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (WidthStyle widthStyle : supportedWidthStyles) {
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitle(widthStyle.toHumanString(app))
					.setTitleColor(getPrimaryTextColor(app, cardInstance.isNightMode()))
					.setTag(widthStyle)
					.create()
			);
		}
		return menuItems;	}

	@Override
	public void onPopUpMenuItemSelected(@NonNull FragmentActivity activity, @NonNull View view, @NonNull PopUpMenuItem item) {
		WidthStyle widthStyle = (WidthStyle) item.getTag();
		String widthValue = getWidthValueOfStyle(widthStyle);
		onWidthValueSelected(widthValue);
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode) {
		if (isDefaultStyle(selectedWidthStyle)) {
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

		// TODO use appropriate card summary
		String pattern = app.getString(R.string.route_line_use_map_style_width);
		String rendererName = app.getRendererRegistry().getSelectedRendererName();
		String summary = String.format(pattern, rendererName);
		DescriptionCard descriptionCard = new DescriptionCard(activity, summary);
		container.addView(descriptionCard.build(activity));
		container.setTag(UNCHANGED_STYLE_CARD_ID);
	}

	private void bindWidthComponentCardIfNeeded(@NonNull FragmentActivity activity,
	                                            @NonNull ViewGroup container) {
		WidthComponentController controller = getWidthComponentController();
		controller.setOnNeedScrollListener(onNeedScrollListener);
		// We only create and bind "Width Component" card only if it wasn't attached before
		// or if there is other card visible at the moment.
		Integer cardId = (Integer) container.getTag();
		if (cardId == null || cardId == UNCHANGED_STYLE_CARD_ID) {
			container.removeAllViews();
			ModedSliderCard widthComponentCard = new ModedSliderCard(activity, controller);
			container.addView(widthComponentCard.build(activity));
			updateColorItems();
		}
		controller.askSelectWidthMode(getWidthValueOfStyle(selectedWidthStyle));
		container.setTag(WIDTH_COMPONENT_CARD_ID);
	}

	private void onWidthValueSelected(@Nullable String widthValue) {
		selectedWidthStyle = findWidthStyle(widthValue);
		cardInstance.updateSelectedCardState();
		listener.onTrackWidthSelected(widthValue);
	}

	public void updateColorItems() {
		int currentColor = colorProvider.getSelectedColorValue();
		WidthComponentController controller = getWidthComponentController();
		controller.updateColorItems(currentColor);
	}

	@NonNull
	private WidthComponentController getWidthComponentController() {
		if (widthComponentController == null) {
			// TODO
			String selectedWidth = null;
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
		result.add(DEFAULT_STYLE);
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

	private boolean isDefaultStyle(@NonNull WidthStyle widthStyle) {
		return Objects.equals(DEFAULT_STYLE, widthStyle);
	}

	private boolean isCustomValue(@NonNull WidthStyle widthStyle) {
		return Objects.equals(widthStyle.getKey(), WidthMode.CUSTOM.getKey());
	}
}
