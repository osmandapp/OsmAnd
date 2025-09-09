package net.osmand.plus.plugins.srtm;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.configmap.MapOptionSliderFragment;
import net.osmand.plus.configmap.MapOptionSliderFragment.MapOptionSliderListener;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;

import java.util.Set;

public class Relief3DFragment extends BaseFullScreenFragment implements View.OnClickListener, DownloadEvents, MapOptionSliderListener {

	public static final String TAG = Relief3DFragment.class.getSimpleName();

	private SRTMPlugin plugin;
	private boolean relief3DEnabled;

	private int profileColor;

	private TextView exaggerationValueTv;

	private TextView downloadDescriptionTv;
	private TextView stateTv;
	private SwitchCompat switchCompat;
	private ImageView iconIv;
	private LinearLayout contentContainer;
	private View titleBottomDivider;

	private DownloadMapsCard downloadMapsCard;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		relief3DEnabled = settings.ENABLE_3D_MAPS.get();
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View root = inflate(R.layout.fragment_relief_3d, container, false);
		profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		showHideTopShadow(root);

		exaggerationValueTv = root.findViewById(R.id.exaggeration_value);
		downloadDescriptionTv = root.findViewById(R.id.download_description_tv);
		titleBottomDivider = root.findViewById(R.id.titleBottomDivider);
		contentContainer = root.findViewById(R.id.content_container);
		switchCompat = root.findViewById(R.id.switch_compat);
		stateTv = root.findViewById(R.id.state_tv);
		iconIv = root.findViewById(R.id.icon_iv);
		downloadMapsCard = new DownloadMapsCard(app, plugin, root.findViewById(R.id.download_maps_card), nightMode);

		TextView titleTv = root.findViewById(R.id.title_tv);
		titleTv.setText(R.string.relief_3d);

		switchCompat.setChecked(relief3DEnabled);
		switchCompat.setOnClickListener(this);
		UiUtilities.setupCompoundButton(switchCompat, nightMode, UiUtilities.CompoundButtonType.PROFILE_DEPENDENT);

		setupContentCard(root);
		updateUiMode();
		return root;
	}

	public float getElevationScaleFactor() {
		return plugin.getVerticalExaggerationScale();
	}

	private void setupContentCard(@NonNull View root) {
		downloadDescriptionTv.setText(R.string.relief_3d_download_description);
		View verticalExaggerationBtn = root.findViewById(R.id.vertical_exaggeration_button);
		verticalExaggerationBtn.setOnClickListener(view -> callMapActivity(mapActivity -> {
			mapActivity.getDashboard().hideDashboard();
			Relief3DExaggerationFragment.showInstance(mapActivity.getSupportFragmentManager(), this);
		}));
	}

	private void showHideTopShadow(@NonNull View view) {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		if (id == R.id.switch_compat) {
			onSwitchClick();
		}
	}

	private void updateUiMode() {
		if (relief3DEnabled) {
			iconIv.setImageDrawable(getPaintedIcon(R.drawable.ic_action_3d_relief, profileColor));
			stateTv.setText(R.string.shared_string_on);
			callMapActivity(downloadMapsCard::updateDownloadSection);
		} else {
			iconIv.setImageDrawable(getIcon(R.drawable.ic_action_3d_relief, ColorUtilities.getSecondaryIconColorId(nightMode)));
			stateTv.setText(R.string.shared_string_off);
		}
		exaggerationValueTv.setText(MapOptionSliderFragment.getFormattedValue(app, getElevationScaleFactor()));
		adjustGlobalVisibility();
	}

	private void adjustGlobalVisibility() {
		titleBottomDivider.setVisibility(relief3DEnabled ? View.GONE : View.VISIBLE);
		contentContainer.setVisibility(relief3DEnabled ? View.VISIBLE : View.GONE);
	}

	private void onSwitchClick() {
		relief3DEnabled = !relief3DEnabled;
		switchCompat.setChecked(relief3DEnabled);
		settings.ENABLE_3D_MAPS.set(relief3DEnabled);
		callMapActivity(mapActivity -> {
			app.runInUIThread(() -> app.getOsmandMap().getMapLayers().getMapInfoLayer().recreateAllControls(mapActivity));
		});
		updateUiMode();
	}

	@Override
	public void onUpdatedIndexesList() {
		callMapActivity(downloadMapsCard::updateDownloadSection);
	}

	@Override
	public void downloadInProgress() {
		downloadMapsCard.downloadInProgress();
	}

	@Override
	public void downloadHasFinished() {
		callMapActivity(mapActivity -> {
			downloadMapsCard.updateDownloadSection(mapActivity);
			SRTMPlugin plugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
			if (plugin != null && plugin.isTerrainLayerEnabled()) {
				plugin.registerLayers(mapActivity, mapActivity);
			}
		});
	}

	@Override
	public void onMapOptionChanged(float value) {
		plugin.setVerticalExaggerationScale(value);
		refreshMap();
	}

	protected void refreshMap() {
		callMapActivity(MapActivity::refreshMap);
	}

	@Nullable
	public Set<InsetSide> getRootInsetSides() {
		return null;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new Relief3DFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}