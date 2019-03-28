package net.osmand.plus.profiles;

import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.LinearLayout;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class SelectedProfileFragment extends BaseOsmAndFragment {

	AppProfile profile = null;
	OsmandApplication app;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		if (getArguments() != null) {
			profile = getArguments().getParcelable("profile");
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		boolean isNightMode = !app.getSettings().isLightContent();

		View view =  inflater.inflate(R.layout.fragment_selected_profile, container, false);

		ImageView profileIcon = view.findViewById(R.id.select_icon_btn_img);
		LinearLayout profileIconBtn = view.findViewById(R.id.profile_icon_layout);
		ExtendedEditText profileNameEt = view.findViewById(R.id.profile_name_et);
		OsmandTextFieldBoxes profileNameTextBox = view.findViewById(R.id.profile_name_otfb);
		ExtendedEditText navTypeEt = view.findViewById(R.id.navigation_type_et);
		OsmandTextFieldBoxes navTypeTextBox= view.findViewById(R.id.navigation_type_otfb);


		profileIconBtn.setBackgroundResource(R.drawable.rounded_background_3dp);
		GradientDrawable selectIconBtnBackground = (GradientDrawable) profileIconBtn.getBackground();

		profileIcon.setImageDrawable(app.getUIUtilities().getIcon(profile.getIconRes(),
			isNightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));

		if (isNightMode) {
			profileNameTextBox.setPrimaryColor(ContextCompat.getColor(app, R.color.color_dialog_buttons_dark));
			navTypeTextBox.setPrimaryColor(ContextCompat.getColor(app, R.color.color_dialog_buttons_dark));
			selectIconBtnBackground.setColor(app.getResources().getColor(R.color.text_field_box_dark));
		} else {
			selectIconBtnBackground.setColor(app.getResources().getColor(R.color.text_field_box_light));
		}

//((OsmandTextFieldBoxes) textBox).setPrimaryColor(ContextCompat.getColor(app, R.color.color_dialog_buttons_dark));

		return view;

	}
}
