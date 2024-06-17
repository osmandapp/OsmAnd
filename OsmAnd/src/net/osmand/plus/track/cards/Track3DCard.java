package net.osmand.plus.track.cards;

import static net.osmand.plus.chooseplan.OsmAndFeature.TERRAIN;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.configmap.MapOptionSliderFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.Gpx3DWallColorType;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.utils.OsmAndFormatter;

import java.util.ArrayList;
import java.util.List;

public class Track3DCard extends BaseCard {

	public static final int WALL_HEIGHT_BUTTON_INDEX = 0;

	private final TrackDrawInfo trackDrawInfo;
	private Spinner visualizedBy;
	private Spinner trackLine;
	private Spinner wallColor;

	private View trackLineContainer;
	private View wallColorContainer;
	private View visualizedByDivider;
	private View freeUserCard;
	private View settingsContainer;
	private View wallHeightContainer;
	private TextView wallHeightValue;
	private TextView wallHeightTitle;

	public Track3DCard(@NonNull FragmentActivity activity, @NonNull TrackDrawInfo trackDrawInfo) {
		super(activity);
		this.trackDrawInfo = trackDrawInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_3d_appearence_card;
	}

	@NonNull
	@Override
	public View build(@NonNull Context ctx) {
		View cardView = super.build(ctx);

		settingsContainer = cardView.findViewById(R.id.settings_container);
		freeUserCard = cardView.findViewById(R.id.free_user_card);
		cardView.findViewById(R.id.get_btn).setOnClickListener((v) -> openChoosePlan());

		setupVisualizedBy(cardView);
		setupTrackLine(cardView);
		setupWallColor(cardView);
		setupVerticalExaggeration(cardView);

		update();

		return cardView;
	}

	private void setupVisualizedBy(@NonNull View view) {
		List<String> items = new ArrayList<>();
		for (Gpx3DVisualizationType item : Gpx3DVisualizationType.values()) {
			items.add(getString(item.getDisplayNameResId()));
		}
		View container = view.findViewById(R.id.visualized_by_container);
		TextView title = container.findViewById(R.id.title);
		title.setText(R.string.visualized_by);

		visualizedBy = container.findViewById(R.id.spinner);
		initSpinner(view.getContext(), visualizedBy, items, new ItemListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				visualizedBy.setSelection(position);
				trackDrawInfo.setTrackVisualizationType(Gpx3DVisualizationType.values()[position]);
				updateContent();
				notifyCardPressed();
			}
		});
		visualizedByDivider = container.findViewById(R.id.divider);
	}

	private void setupTrackLine(@NonNull View view) {
		List<String> items = new ArrayList<>();
		for (Gpx3DLinePositionType item : Gpx3DLinePositionType.values()) {
			items.add(getString(item.getDisplayNameResId()));
		}
		trackLineContainer = view.findViewById(R.id.track_line_container);
		trackLine = trackLineContainer.findViewById(R.id.spinner);

		TextView title = trackLineContainer.findViewById(R.id.title);
		title.setText(R.string.track_line);

		initSpinner(view.getContext(), trackLine, items, new ItemListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				trackLine.setSelection(position);
				trackDrawInfo.setTrackLinePositionType(Gpx3DLinePositionType.values()[position]);
				notifyCardPressed();
			}
		});
	}

	private void setupWallColor(@NonNull View view) {
		List<String> items = new ArrayList<>();
		for (Gpx3DWallColorType item : Gpx3DWallColorType.values()) {
			items.add(getString(item.getDisplayNameResId()));
		}
		wallColorContainer = view.findViewById(R.id.wall_coloring_container);
		wallColor = wallColorContainer.findViewById(R.id.spinner);

		TextView title = wallColorContainer.findViewById(R.id.title);
		title.setText(R.string.wall_color);

		initSpinner(view.getContext(), wallColor, items, new ItemListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				wallColor.setSelection(position);
				trackDrawInfo.setTrackWallColorType(Gpx3DWallColorType.values()[position]);
				notifyCardPressed();
			}
		});
	}

	private void initSpinner(@NonNull Context ctx, @NonNull Spinner spinner,
	                         @NonNull List<String> items, @NonNull OnItemSelectedListener listener) {
		ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(listener);
	}

	private void setupVerticalExaggeration(@NonNull View view) {
		wallHeightContainer = view.findViewById(R.id.exaggeration_container);
		wallHeightContainer.setOnClickListener((v) -> notifyButtonPressed(WALL_HEIGHT_BUTTON_INDEX));

		wallHeightValue = wallHeightContainer.findViewById(R.id.value);
		wallHeightTitle = wallHeightContainer.findViewById(R.id.title);
	}

	@Override
	protected void updateContent() {
		if (visualizedBy != null) {
			Gpx3DVisualizationType type = trackDrawInfo.getTrackVisualizationType();
			visualizedBy.setSelection(type.ordinal());
			wallColor.setSelection(trackDrawInfo.getTrackWallColorType().ordinal());
			trackLine.setSelection(trackDrawInfo.getTrackLinePositionType().ordinal());

			boolean fixedHeight = trackDrawInfo.isFixedHeight();
			if (fixedHeight) {
				float elevation = trackDrawInfo.getElevationMeters();
				wallHeightValue.setText(OsmAndFormatter.getFormattedAlt(elevation, app));
			} else {
				float exaggeration = trackDrawInfo.getAdditionalExaggeration();
				wallHeightValue.setText(MapOptionSliderFragment.getFormattedValue(app, exaggeration));
			}
			wallHeightTitle.setText(fixedHeight ? R.string.wall_height : R.string.vertical_exaggeration);

			boolean paramsVisible = type.is3dType();
			AndroidUiHelper.updateVisibility(wallColor, paramsVisible);
			AndroidUiHelper.updateVisibility(trackLine, paramsVisible);
			AndroidUiHelper.updateVisibility(wallColorContainer, paramsVisible);
			AndroidUiHelper.updateVisibility(trackLineContainer, paramsVisible);
			AndroidUiHelper.updateVisibility(wallHeightContainer, paramsVisible);
			AndroidUiHelper.updateVisibility(visualizedByDivider, paramsVisible);
			AndroidUiHelper.updateVisibility(freeUserCard, isGetBtnVisible());
			AndroidUiHelper.updateVisibility(settingsContainer, !isGetBtnVisible());
		}
	}

	private boolean isGetBtnVisible() {
		boolean isFullVersion = !Version.isFreeVersion(app) || InAppPurchaseUtils.isFullVersionAvailable(app, false);
		return !isFullVersion && !InAppPurchaseUtils.isSubscribedToAny(app, false);
	}

	private void openChoosePlan() {
		if (activity != null) {
			ChoosePlanFragment.showInstance(activity, TERRAIN);
		}
	}

	private static abstract class ItemListener implements OnItemSelectedListener {

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
	}
}