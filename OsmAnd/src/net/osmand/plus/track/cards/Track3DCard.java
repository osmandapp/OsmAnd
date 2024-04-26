package net.osmand.plus.track.cards;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.Gpx3DWallColorType;
import net.osmand.plus.track.TrackDrawInfo;

import java.util.ArrayList;
import java.util.List;

public class Track3DCard extends BaseCard {

	private final TrackDrawInfo trackDrawInfo;
	private Spinner visualizedBy;
	private Spinner wallColor;
	private Spinner trackLine;

	private View wallColorContainer;
	private View trackLineContainer;
	private View wallColorDivider;
	private View visualizedByDivider;
	private View getButton;
	private View freeUserCard;
	private View settingsContainer;

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
		visualizedBy = cardView.findViewById(R.id.spinner_visualized_by);
		wallColor = cardView.findViewById(R.id.spinner_wall_color);
		trackLine = cardView.findViewById(R.id.spinner_track_line);
		wallColorContainer = cardView.findViewById(R.id.wall_coloring_container);
		trackLineContainer = cardView.findViewById(R.id.track_line_container);
		wallColorDivider = cardView.findViewById(R.id.wall_coloring_divider);
		visualizedByDivider = cardView.findViewById(R.id.visualized_by_divider);
		freeUserCard = cardView.findViewById(R.id.free_user_card);
		settingsContainer = cardView.findViewById(R.id.settings_container);
		getButton = cardView.findViewById(R.id.get_btn);
		getButton.setOnClickListener((v) -> openChoosePlan());

		List<String> visualizedByItems = new ArrayList<>();
		for (Gpx3DVisualizationType item : Gpx3DVisualizationType.values()) {
			visualizedByItems.add(ctx.getString(item.getDisplayNameResId()));
		}
		List<String> wallColorItems = new ArrayList<>();
		for (Gpx3DWallColorType item : Gpx3DWallColorType.values()) {
			wallColorItems.add(ctx.getString(item.getDisplayNameResId()));
		}
		List<String> trackLineItems = new ArrayList<>();
		for (Gpx3DLinePositionType item : Gpx3DLinePositionType.values()) {
			trackLineItems.add(ctx.getString(item.getDisplayNameResId()));
		}
		initSpinner(ctx, visualizedBy, visualizedByItems, new Track3dSettingSelectListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				visualizedBy.setSelection(position);
				trackDrawInfo.setTrackVisualizationType(Gpx3DVisualizationType.values()[position]);
				updateContent();
				notifyCardPressed();
			}
		});
		initSpinner(ctx, wallColor, wallColorItems, new Track3dSettingSelectListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				wallColor.setSelection(position);
				trackDrawInfo.setTrackWallColorType(Gpx3DWallColorType.values()[position]);
				notifyCardPressed();
			}
		});
		initSpinner(ctx, trackLine, trackLineItems, new Track3dSettingSelectListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				trackLine.setSelection(position);
				trackDrawInfo.setTrackLinePositionType(Gpx3DLinePositionType.values()[position]);
				notifyCardPressed();
			}

		});
		update();
		return cardView;
	}

	private void initSpinner(@NonNull Context ctx,
	                         @NonNull Spinner spinner,
	                         @NonNull List<String> items,
	                         @NonNull AdapterView.OnItemSelectedListener onItemSelectedListener) {
		ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(1);
		spinner.setOnItemSelectedListener(onItemSelectedListener);
	}

	@Override
	protected void updateContent() {
		if (visualizedBy != null) {
			Gpx3DVisualizationType visualizationType = trackDrawInfo.getTrackVisualizationType();
			visualizedBy.setSelection(visualizationType.ordinal());
			wallColor.setSelection(trackDrawInfo.getTrackWallColorType().ordinal());
			trackLine.setSelection(trackDrawInfo.getTrackLinePositionType().ordinal());
			AndroidUiHelper.updateVisibility(wallColor, visualizationType != Gpx3DVisualizationType.NONE);
			AndroidUiHelper.updateVisibility(trackLine, visualizationType != Gpx3DVisualizationType.NONE);
			AndroidUiHelper.updateVisibility(wallColorContainer, visualizationType != Gpx3DVisualizationType.NONE);
			AndroidUiHelper.updateVisibility(trackLineContainer, visualizationType != Gpx3DVisualizationType.NONE);
			AndroidUiHelper.updateVisibility(wallColorDivider, visualizationType != Gpx3DVisualizationType.NONE);
			AndroidUiHelper.updateVisibility(visualizedByDivider, visualizationType != Gpx3DVisualizationType.NONE);
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
			ChoosePlanFragment.showInstance(activity, OsmAndFeature.RELIEF_3D);
		}
	}

	private static class Track3dSettingSelectListener implements AdapterView.OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}
	}
}