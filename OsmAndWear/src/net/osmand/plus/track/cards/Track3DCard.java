package net.osmand.plus.track.cards;

import static net.osmand.plus.chooseplan.OsmAndFeature.TERRAIN;
import static net.osmand.plus.track.Gpx3DVisualizationType.FIXED_HEIGHT;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_BIKE_POWER;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_CADENCE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_HEART_RATE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_SPEED;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_TEMPERATURE;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.configmap.MapOptionSliderFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.routing.Gpx3DWallColorType;
import net.osmand.shared.util.Localization;

import java.util.ArrayList;
import java.util.List;

public class Track3DCard extends BaseCard {

	public static final int WALL_HEIGHT_BUTTON_INDEX = 0;

	private final TrackDrawInfo drawInfo;
	private final GpxTrackAnalysis analysis;

	public Track3DCard(@NonNull FragmentActivity activity, @NonNull GpxTrackAnalysis analysis,
	                   @NonNull TrackDrawInfo drawInfo) {
		super(activity);
		this.drawInfo = drawInfo;
		this.analysis = analysis;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_3d_appearence_card;
	}

	@Override
	protected void updateContent() {
		boolean visible = drawInfo.getTrackVisualizationType().is3dType();

		setupVisualizedBy(visible);
		setupTrackLine(visible);
		setupWallColor(visible);
		setupVerticalExaggeration(visible);
		setupFreeUserCard();
	}

	private void setupVisualizedBy(boolean visible) {
		View container = view.findViewById(R.id.visualized_by_container);
		TextView title = container.findViewById(R.id.title);
		TextView description = container.findViewById(R.id.description);

		title.setText(R.string.visualized_by);
		description.setText(drawInfo.getTrackVisualizationType().getDisplayNameResId());

		container.findViewById(R.id.button).setOnClickListener(v -> {
			List<PopUpMenuItem> items = new ArrayList<>();

			Gpx3DVisualizationType previous = null;
			for (Gpx3DVisualizationType type : Gpx3DVisualizationType.values()) {
				if (isVisualizationTypeAvailable(type, analysis)) {
					items.add(new PopUpMenuItem.Builder(app)
							.setTitleId(type.getDisplayNameResId())
							.showTopDivider(FIXED_HEIGHT == type || Gpx3DVisualizationType.NONE == previous)
							.setOnClickListener(item -> {
								drawInfo.setTrackVisualizationType(type);
								updateContent();
								notifyCardPressed();
							})
							.create());
					previous = type;
				}
			}
			showOptionsMenu(v, items);
		});
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.divider), visible);
	}

	private boolean isVisualizationTypeAvailable(@NonNull Gpx3DVisualizationType type, @NonNull GpxTrackAnalysis analysis) {
		return switch (type) {
			case ALTITUDE -> analysis.hasElevationData();
			case SPEED -> analysis.hasSpeedData();
			case HEART_RATE -> analysis.hasData(SENSOR_TAG_HEART_RATE);
			case BICYCLE_CADENCE -> analysis.hasData(SENSOR_TAG_CADENCE);
			case BICYCLE_POWER -> analysis.hasData(SENSOR_TAG_BIKE_POWER);
			case TEMPERATURE -> analysis.hasData(SENSOR_TAG_TEMPERATURE);
			case SPEED_SENSOR -> analysis.hasData(SENSOR_TAG_SPEED);
			default -> true;
		};
	}

	private void setupTrackLine(boolean visible) {
		View container = view.findViewById(R.id.track_line_container);
		TextView title = container.findViewById(R.id.title);
		TextView description = container.findViewById(R.id.description);

		title.setText(R.string.track_line);
		description.setText(drawInfo.getTrackLinePositionType().getDisplayNameResId());

		container.findViewById(R.id.button).setOnClickListener(v -> {
			List<PopUpMenuItem> items = new ArrayList<>();
			for (Gpx3DLinePositionType type : Gpx3DLinePositionType.values()) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(type.getDisplayNameResId())
						.setOnClickListener(item -> {
							description.setText(item.getTitle());
							drawInfo.setTrackLinePositionType(type);
							notifyCardPressed();
						})
						.create());
			}
			showOptionsMenu(v, items);
		});
		AndroidUiHelper.updateVisibility(container, visible);
	}

	private void setupWallColor(boolean visible) {
		View container = view.findViewById(R.id.wall_coloring_container);
		TextView title = container.findViewById(R.id.title);
		TextView description = container.findViewById(R.id.description);

		title.setText(R.string.wall_color);
		description.setText(Localization.INSTANCE.getStringId(drawInfo.getTrackWallColorType().getDisplayNameResId()));

		container.findViewById(R.id.button).setOnClickListener(v -> {
			List<PopUpMenuItem> items = new ArrayList<>();

			Gpx3DWallColorType previous = null;
			for (Gpx3DWallColorType type : Gpx3DWallColorType.getEntries()) {
				if (isWallColorAvailable(type, analysis)) {
					items.add(new PopUpMenuItem.Builder(app)
							.setTitleId(Localization.INSTANCE.getStringId(type.getDisplayNameResId()))
							.showTopDivider(Gpx3DWallColorType.NONE == previous || Gpx3DWallColorType.UPWARD_GRADIENT == previous)
							.setOnClickListener(item -> {
								description.setText(item.getTitle());
								drawInfo.setTrackWallColorType(type);
								notifyCardPressed();
							})
							.create());
					previous = type;
				}
			}
			showOptionsMenu(v, items);
		});
		AndroidUiHelper.updateVisibility(container, visible);
	}

	private boolean isWallColorAvailable(@NonNull Gpx3DWallColorType type, @NonNull GpxTrackAnalysis analysis) {
		return switch (type) {
			case ALTITUDE -> analysis.hasElevationData();
			case SPEED, SLOPE -> analysis.hasSpeedData();
			default -> true;
		};
	}

	private void showOptionsMenu(@NonNull View view, @NonNull List<PopUpMenuItem> items) {
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void setupVerticalExaggeration(boolean visible) {
		View wallHeightContainer = view.findViewById(R.id.exaggeration_container);
		wallHeightContainer.setOnClickListener((v) -> notifyButtonPressed(WALL_HEIGHT_BUTTON_INDEX));

		TextView title = wallHeightContainer.findViewById(R.id.title);
		TextView description = wallHeightContainer.findViewById(R.id.value);

		boolean fixedHeight = drawInfo.isFixedHeight();
		if (fixedHeight) {
			float elevation = drawInfo.getElevationMeters();
			description.setText(OsmAndFormatter.getFormattedAlt(elevation, app));
		} else {
			float exaggeration = drawInfo.getAdditionalExaggeration();
			description.setText(MapOptionSliderFragment.getFormattedValue(app, exaggeration));
		}
		title.setText(fixedHeight ? R.string.wall_height : R.string.vertical_exaggeration);
		AndroidUiHelper.updateVisibility(wallHeightContainer, visible);
	}

	private void setupFreeUserCard() {
		View container = view.findViewById(R.id.free_user_card);
		container.findViewById(R.id.get_btn).setOnClickListener((v) -> ChoosePlanFragment.showInstance(activity, TERRAIN));

		boolean paidVersion = Version.isPaidVersion(app);
		AndroidUiHelper.updateVisibility(container, !paidVersion);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.settings_container), paidVersion);
	}
}