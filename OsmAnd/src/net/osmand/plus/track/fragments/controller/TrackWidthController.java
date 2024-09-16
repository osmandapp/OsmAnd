package net.osmand.plus.track.fragments.controller;

import static net.osmand.util.Algorithms.parseIntSilently;

import android.view.View;

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
import net.osmand.plus.card.base.headed.IHeadedCardController;
import net.osmand.plus.card.base.headed.IHeadedContentCard;
import net.osmand.plus.card.base.slider.moded.ModedSliderCard;
import net.osmand.plus.card.width.WidthComponentController;
import net.osmand.plus.card.width.WidthMode;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;

public class TrackWidthController implements IHeadedCardController, IDialogController {

	private static final String PROCESS_ID = "select_track_width";

	private static final int CUSTOM_WIDTH_MIN = 1;
	private static final int CUSTOM_WIDTH_MAX = 24;

	private final OsmandApplication app;
	private final TrackDrawInfo drawInfo;

	private IHeadedContentCard cardInstance;
	private WidthComponentController widthComponentController;
	private OnNeedScrollListener onNeedScrollListener;
	private ITrackWidthSelectedListener listener;

	private TrackWidthController(@NonNull OsmandApplication app,
	                             @NonNull TrackDrawInfo drawInfo) {
		this.app = app;
		this.drawInfo = drawInfo;
	}

	@Override
	public void bindComponent(@NonNull IHeadedContentCard cardInstance) {
		this.cardInstance = cardInstance;
	}

	public void setOnNeedScrollListener(@NonNull OnNeedScrollListener onNeedScrollListener) {
		this.onNeedScrollListener = onNeedScrollListener;
	}

	public void setListener(@NonNull ITrackWidthSelectedListener listener) {
		this.listener = listener;
	}

	private void onWidthValueSelected(@Nullable String widthValue) {
		if (widthValue != null) {
			setGpxWidth(widthValue);
			cardInstance.updateCardSummary();
			listener.onTrackWidthSelected(widthValue);
		}
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_width);
	}

	@NonNull
	@Override
	public String getCardSummary() {
		WidthComponentController controller = getWidthComponentController();
		return controller.getSummary(app);
	}

	@NonNull
	@Override
	public View getCardContentView(@NonNull FragmentActivity activity, boolean nightMode) {
		WidthComponentController controller = getWidthComponentController();
		controller.setOnNeedScrollListener(onNeedScrollListener);

		ModedSliderCard widthComponentCard = new ModedSliderCard(activity, controller);
		View view = widthComponentCard.build(activity);

		// Disable arrows in OpenGL while touching slider, to prevent arrows blinking
		Slider widthSlider = widthComponentCard.getSlider();
		widthSlider.addOnSliderTouchListener(new OnSliderTouchListener() {

			boolean prevShowArrows;

			@Override
			public void onStartTrackingTouch(@NonNull Slider slider) {
				if (hasMapRenderer()) {
					if (drawInfo.isShowArrows()) {
						prevShowArrows = true;
						setShowArrows(false);
					}
				}
			}

			@Override
			public void onStopTrackingTouch(@NonNull Slider slider) {
				if (hasMapRenderer()) {
					if (prevShowArrows) {
						setShowArrows(true);
					}
				}
			}

			private boolean hasMapRenderer() {
				return app.getOsmandMap().getMapView().getMapRenderer() != null;
			}

			private void setShowArrows(boolean showArrows) {
				drawInfo.setShowArrows(showArrows);
				app.getOsmandMap().getMapView().refreshMap();
			}
		});

		return view;
	}

	@NonNull
	public WidthComponentController getWidthComponentController() {
		if (widthComponentController == null) {
			String selectedWidth = drawInfo.getWidth();
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

	private void setGpxWidth(@NonNull String width) {
		drawInfo.setWidth(width);
		app.getOsmandMap().getMapView().refreshMap();
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
	}

	public interface ITrackWidthSelectedListener {
		void onTrackWidthSelected(@Nullable String width);
	}

	@NonNull
	public static TrackWidthController getInstance(
			@NonNull OsmandApplication app, @NonNull TrackDrawInfo drawInfo,
			@NonNull OnNeedScrollListener onNeedScrollListener, @NonNull ITrackWidthSelectedListener listener
	) {
		DialogManager dialogManager = app.getDialogManager();
		TrackWidthController controller = (TrackWidthController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new TrackWidthController(app, drawInfo);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setOnNeedScrollListener(onNeedScrollListener);
		controller.setListener(listener);
		return controller;
	}
}
