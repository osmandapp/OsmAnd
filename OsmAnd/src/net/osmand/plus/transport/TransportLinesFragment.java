package net.osmand.plus.transport;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRuleProperty;

import java.util.List;

import static net.osmand.plus.transport.TransportLinesMenu.getTransportRules;

public class TransportLinesFragment extends BaseOsmAndFragment {

	public static final String TAG = TransportLinesFragment.class.getSimpleName();

	private OsmandApplication app;
	private MapActivity mapActivity;
	private OsmandSettings settings;
	private ApplicationMode appMode;
	private TransportLinesMenu transportMenu;

	private View view;
	private LayoutInflater themedInflater;
	private boolean isShowAnyTransport;
	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		mapActivity = (MapActivity) requireMyActivity();
		transportMenu = new TransportLinesMenu(app);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		appMode = settings.getApplicationMode();
		isShowAnyTransport = transportMenu.isShowAnyTransport();

		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		view = themedInflater.inflate(R.layout.fragment_transport_lines, container, false);

		setupMainToggle();
		setupTransportStopsToggle();
		setupRoutesToggles();

		updateScreenMode(isShowAnyTransport);
		return view;
	}

	private void setupMainToggle() {
		setupButton(
				view.findViewById(R.id.main_toggle),
				R.drawable.ic_action_transport_bus,
				getString(R.string.rendering_category_transport),
				isShowAnyTransport,
				false,
				v -> {
					isShowAnyTransport = !isShowAnyTransport;
					transportMenu.toggleTransportLines(mapActivity, isShowAnyTransport);
					updateScreenMode(isShowAnyTransport);
				});
	}

	private void setupTransportStopsToggle() {
		TransportType type = TransportType.TRANSPORT_STOPS;
		setupButton(
				view.findViewById(R.id.transport_stops_toggle),
				type.getIconId(),
				transportMenu.getTransportName(type.getAttrName()),
				transportMenu.isTransportEnabled(type.getAttrName()),
				false,
				v -> {
					boolean enabled = !transportMenu.isTransportEnabled(type.getAttrName());
					transportMenu.toggleTransportType(mapActivity, type.getAttrName(), enabled);
				}
		);
	}

	private void setupRoutesToggles() {
		ViewGroup list = view.findViewById(R.id.transport_toggles_list);
		List<RenderingRuleProperty> rules = getTransportRules(app);
		for (int i = 0; i < rules.size(); i++) {
			RenderingRuleProperty property = rules.get(i);
			String attrName = property.getAttrName();
			if (!TransportType.TRANSPORT_STOPS.getAttrName().equals(attrName)) {
				View view = themedInflater.inflate(R.layout.bottom_sheet_item_with_switch, list, false);
				boolean showDivider = i < rules.size() - 1;
				setupButton(
						view,
						transportMenu.getTransportIcon(attrName),
						transportMenu.getTransportName(attrName, property.getName()),
						transportMenu.isTransportEnabled(attrName),
						showDivider,
						v -> {
							boolean enabled = !transportMenu.isTransportEnabled(attrName);
							transportMenu.toggleTransportType(mapActivity, attrName, enabled);
						}
				);
				list.addView(view);
			}
		}
	}

	private void updateScreenMode(boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	private void setupButton(@NonNull View view, int iconId, @NonNull String title, boolean enabled,
	                         boolean showDivider, @Nullable OnClickListener listener) {
		int activeColor = appMode.getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = enabled ? activeColor : defColor;

		Drawable icon = getPaintedContentIcon(iconId, iconColor);
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);
		ivIcon.setColorFilter(enabled ? activeColor : defColor);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		CompoundButton cb = view.findViewById(R.id.compound_button);
		cb.setChecked(enabled);
		cb.setVisibility(View.VISIBLE);
		UiUtilities.setupCompoundButton(nightMode, activeColor, cb);

		cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
			ivIcon.setColorFilter(isChecked ? activeColor : defColor);
			if (listener != null) {
				listener.onClick(buttonView);
			}
		});

		view.setOnClickListener(v -> {
			boolean newState = !cb.isChecked();
			cb.setChecked(newState);
		});

		View divider = view.findViewById(R.id.bottom_divider);
		if (divider != null) {
			AndroidUiHelper.updateVisibility(divider, showDivider);
		}

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new TransportLinesFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}
