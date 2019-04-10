package net.osmand.plus.profiles;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.util.ArrayList;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.profiles.NavTypeBottomSheetDialogFragment.NavTypeDialogListener;
import net.osmand.plus.profiles.NavTypeBottomSheetDialogFragment.IconIdListener;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import org.apache.commons.logging.Log;
import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class SelectedProfileFragment extends BaseOsmAndFragment {

	private static final Log LOG = PlatformUtil.getLog(SelectedProfileFragment.class);

	ApplicationMode profile = null;
	ArrayList<RoutingProfile> routingProfiles;
	OsmandApplication app;

	boolean isDataChanged = false;

	private NavTypeDialogListener navTypeDialogListener = null;
	private IconIdListener iconIdListener = null;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		if (getArguments() != null) {
			String modeName = getArguments().getString("stringKey");
			profile = ApplicationMode.valueOfStringKey(modeName, ApplicationMode.CAR);
		}
		routingProfiles = getRoutingProfiles();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {
		boolean isNightMode = !app.getSettings().isLightContent();

		View view = inflater.inflate(R.layout.fragment_selected_profile, container, false);

		final ImageView profileIcon = view.findViewById(R.id.select_icon_btn_img);
		final LinearLayout profileIconBtn = view.findViewById(R.id.profile_icon_layout);
		final ExtendedEditText profileNameEt = view.findViewById(R.id.profile_name_et);
		final OsmandTextFieldBoxes profileNameTextBox = view.findViewById(R.id.profile_name_otfb);
		final ExtendedEditText navTypeEt = view.findViewById(R.id.navigation_type_et);
		final OsmandTextFieldBoxes navTypeTextBox = view.findViewById(R.id.navigation_type_otfb);
		final FrameLayout select_nav_type_btn = view.findViewById(R.id.select_nav_type_btn);

		profileIconBtn.setBackgroundResource(R.drawable.rounded_background_3dp);
		GradientDrawable selectIconBtnBackground = (GradientDrawable) profileIconBtn
			.getBackground();

		profileIcon.setImageDrawable(app.getUIUtilities().getIcon(profile.getSmallIconDark(),
			isNightMode ? R.color.active_buttons_and_links_dark
				: R.color.active_buttons_and_links_light));

		if (isNightMode) {
			profileNameTextBox
				.setPrimaryColor(ContextCompat.getColor(app, R.color.color_dialog_buttons_dark));
			navTypeTextBox
				.setPrimaryColor(ContextCompat.getColor(app, R.color.color_dialog_buttons_dark));
			selectIconBtnBackground
				.setColor(app.getResources().getColor(R.color.text_field_box_dark));
		} else {
			selectIconBtnBackground
				.setColor(app.getResources().getColor(R.color.text_field_box_light));
		}

		navTypeDialogListener = new NavTypeDialogListener() {
			@Override
			public void selectedNavType(int pos) {
				navTypeEt.setText(routingProfiles.get(pos).getName());
			}
		};

		select_nav_type_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final NavTypeBottomSheetDialogFragment fragment = new NavTypeBottomSheetDialogFragment();
				fragment.setNavTypeListener(navTypeDialogListener);
				Bundle bundle = new Bundle();
				bundle.putParcelableArrayList("routing_profiles", routingProfiles);
				fragment.setArguments(bundle);
				getActivity().getSupportFragmentManager().beginTransaction().add(fragment, "select_nav_type")
					.commitAllowingStateLoss();


//				navTypeEt.setText("Car");
//				navTypeEt.setCursorVisible(false);
//				navTypeEt.setTextIsSelectable(false);
//				navTypeEt.clearFocus();
//				navTypeEt.requestFocus(EditText.FOCUS_UP);
//				LOG.debug("click on text");

			}
		});
		return view;
	}

	/**
	 * For now there are only default nav profiles todo: add profiles from custom routing xml-s
	 */
	private ArrayList<RoutingProfile> getRoutingProfiles() {
		ArrayList<RoutingProfile> routingProfiles = new ArrayList<>();
		for (GeneralRouterProfile navProfileName : GeneralRouterProfile.values()) {
			String name = "";
			int iconRes = -1;
			switch (navProfileName) {
				case CAR:
					iconRes = R.drawable.ic_action_car_dark;
					name = getString(R.string.rendering_value_car_name);
					break;
				case PEDESTRIAN:
					iconRes = R.drawable.map_action_pedestrian_dark;
					name = getString(R.string.rendering_value_pedestrian_name);
					break;
				case BICYCLE:
					iconRes = R.drawable.map_action_bicycle_dark;
					name = getString(R.string.rendering_value_bicycle_name);
					break;
				case PUBLIC_TRANSPORT:
					iconRes = R.drawable.map_action_bus_dark;
					name = getString(R.string.app_mode_public_transport);
					break;
				case BOAT:
					iconRes = R.drawable.map_action_sail_boat_dark;
					name = getString(R.string.app_mode_boat);
					break;
			}
			routingProfiles
				.add(new RoutingProfile(name, getString(R.string.osmand_default_routing), iconRes, false));
		}
		return routingProfiles;
	}
}
