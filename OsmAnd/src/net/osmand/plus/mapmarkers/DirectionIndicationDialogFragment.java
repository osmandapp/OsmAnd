package net.osmand.plus.mapmarkers;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MapMarkersMode;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;

public class DirectionIndicationDialogFragment extends BaseOsmAndDialogFragment {

	public final static String TAG = "DirectionIndicationDialogFragment";

	private DirectionIndicationFragmentListener listener;
	private View mainView;

	public void setListener(DirectionIndicationFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final OsmandSettings settings = getSettings();

		mainView = inflater.inflate(R.layout.fragment_direction_indication_dialog, container);

		Toolbar toolbar = mainView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIconsCache().getIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		final CompoundButton distanceIndicationToggle = (CompoundButton) mainView.findViewById(R.id.distance_indication_switch);
		distanceIndicationToggle.setChecked(settings.MARKERS_DISTANCE_INDICATION_ENABLED.get());
		mainView.findViewById(R.id.distance_indication_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateChecked(settings.MARKERS_DISTANCE_INDICATION_ENABLED, distanceIndicationToggle);
				updateSelection(true);
			}
		});

		mainView.findViewById(R.id.top_bar_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				settings.MAP_MARKERS_MODE.set(MapMarkersMode.TOOLBAR);
				updateSelection(true);
			}
		});

		mainView.findViewById(R.id.widget_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				settings.MAP_MARKERS_MODE.set(MapMarkersMode.WIDGETS);
				updateSelection(true);
			}
		});

		updateSelection(false);

		final CompoundButton showArrowsToggle = (CompoundButton) mainView.findViewById(R.id.show_arrows_switch);
		showArrowsToggle.setChecked(settings.SHOW_ARROWS_TO_FIRST_MARKERS.get());
		mainView.findViewById(R.id.show_arrows_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateChecked(settings.SHOW_ARROWS_TO_FIRST_MARKERS, showArrowsToggle);
			}
		});

		final CompoundButton showLinesToggle = (CompoundButton) mainView.findViewById(R.id.show_guide_line_switch);
		showLinesToggle.setChecked(settings.SHOW_LINES_TO_FIRST_MARKERS.get());
		mainView.findViewById(R.id.show_guide_line_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateChecked(settings.SHOW_LINES_TO_FIRST_MARKERS, showLinesToggle);
			}
		});

		return mainView;
	}

	@Override
	protected Drawable getContentIcon(int id) {
		return getIcon(id, getSettings().isLightContent() ? R.color.icon_color : R.color.ctx_menu_info_text_dark);
	}

	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	private void updateChecked(OsmandPreference<Boolean> setting, CompoundButton button) {
		boolean newState = !setting.get();
		setting.set(newState);
		button.setChecked(newState);
		if (getMapActivity() != null) {
			getMapActivity().refreshMap();
		}
	}

	private void notifyListener() {
		if (listener != null) {
			listener.onMapMarkersModeChanged(getSettings().MARKERS_DISTANCE_INDICATION_ENABLED.get());
		}
	}

	private void updateSelection(boolean notifyListener) {
		OsmandSettings settings = getSettings();
		MapMarkersMode mode = settings.MAP_MARKERS_MODE.get();
		boolean distIndEnabled = settings.MARKERS_DISTANCE_INDICATION_ENABLED.get();
		boolean topBar = mode == MapMarkersMode.TOOLBAR;
		boolean widget = mode == MapMarkersMode.WIDGETS;
		updateIcon(R.id.top_bar_icon, R.drawable.ic_action_device_topbar, topBar && distIndEnabled);
		updateIcon(R.id.widget_icon, R.drawable.ic_action_device_widget, widget && distIndEnabled);
		updateMarkerModeRow(R.id.top_bar_row, R.id.top_bar_radio_button, topBar, distIndEnabled);
		updateMarkerModeRow(R.id.widget_row, R.id.widget_radio_button, widget, distIndEnabled);
		if (notifyListener) {
			notifyListener();
		}
	}

	private void updateIcon(int imageViewId, int drawableId, boolean active) {
		ImageView iv = (ImageView) mainView.findViewById(imageViewId);
		iv.setBackgroundDrawable(active
				? getIcon(R.drawable.ic_action_device_top, R.color.dashboard_blue)
				: getContentIcon(R.drawable.ic_action_device_top));
		iv.setImageDrawable(active
				? getIcon(drawableId, R.color.osmand_orange)
				: getContentIcon(drawableId));
	}

	private void updateMarkerModeRow(int rowId, int radioButtonId, boolean checked, boolean active) {
		boolean night = !getSettings().isLightContent();
		RadioButton rb = (RadioButton) mainView.findViewById(radioButtonId);
		rb.setChecked(checked);
		int colorId = active ? night ? R.color.osmand_orange : R.color.dashboard_blue
				: night ? R.color.ctx_menu_info_text_dark : R.color.icon_color;
		CompoundButtonCompat.setButtonTintList(rb, ColorStateList.valueOf(ContextCompat.getColor(getContext(), colorId)));
		mainView.findViewById(rowId).setEnabled(active);
	}

	public interface DirectionIndicationFragmentListener {
		void onMapMarkersModeChanged(boolean showDirectionEnabled);
	}
}
