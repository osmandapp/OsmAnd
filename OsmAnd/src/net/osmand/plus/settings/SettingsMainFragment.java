package net.osmand.plus.settings;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.profiles.SettingsProfileFragment;
import net.osmand.util.Algorithms;

public class SettingsMainFragment extends BaseOsmAndFragment {

	public static final String TAG = "SettingsMainFragment";

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_settings_main, null);

		AndroidUtils.addStatusBarPadding21v(getContext(), view);

		Toolbar tb = (Toolbar) view.findViewById(R.id.toolbar);

		tb.setTitle(R.string.shared_string_settings);
		tb.setClickable(true);
		tb.setNavigationIcon(getIcon(R.drawable.ic_arrow_back));
		tb.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		tb.setBackgroundColor(getResources().getColor(resolveResourceId(getActivity(), R.attr.pstsTabBackground)));
		tb.setTitleTextColor(getResources().getColor(resolveResourceId(getActivity(), R.attr.pstsTextColor)));
		tb.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				getActivity().getSupportFragmentManager().popBackStack();
			}
		});

		View personal_account_container = view.findViewById(R.id.personal_account_container);
		personal_account_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getMyApplication().showShortToastMessage("personal_account_container");
			}
		});
		View global_settings_container = view.findViewById(R.id.global_settings_container);
		global_settings_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getMyApplication().showShortToastMessage("global_settings_container");
			}
		});
		View browse_map_container = view.findViewById(R.id.browse_map_container);
		browse_map_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getMyApplication().showShortToastMessage("browse_map_container");
			}
		});

		final ApplicationMode selectedMode = getSettings().APPLICATION_MODE.get();
		View configure_profile_container = view.findViewById(R.id.configure_profile_container);

		int iconRes = selectedMode.getIconRes();
		int iconColor = selectedMode.getIconColorInfo().getColor(!getSettings().isLightContent());
		String title = selectedMode.isCustomProfile() ? selectedMode.getCustomProfileName() : getResources().getString(selectedMode.getNameKeyResource());

		TextView profileTitle = (TextView) view.findViewById(R.id.configure_profile_title);
		profileTitle.setText(title);

		String profileType = null;
		if (selectedMode.isCustomProfile()) {
			profileType = String.format(getString(R.string.profile_type_descr_string), Algorithms.capitalizeFirstLetterAndLowercase(selectedMode.getParent().toHumanString(getContext())));
		} else {
			profileType = getString(R.string.profile_type_base_string);
		}

		TextView profileDescription = (TextView) view.findViewById(R.id.configure_profile_description);
		profileDescription.setText(profileType);

		ImageView profileIcon = (ImageView) view.findViewById(R.id.configure_profile_icon);
		profileIcon.setImageDrawable(getIcon(iconRes, iconColor));

		configure_profile_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ConfigureProfileFragment.showInstance(getActivity().getSupportFragmentManager(), selectedMode);
			}
		});
		View manage_profiles_container = view.findViewById(R.id.manage_profiles_container);
		manage_profiles_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SettingsProfileFragment profileFragment = new SettingsProfileFragment();
				getActivity().getSupportFragmentManager()
						.beginTransaction()
						.replace(R.id.fragmentContainer, profileFragment, SettingsProfileFragment.TAG)
						.addToBackStack(SettingsProfileFragment.TAG)
						.commit();
			}
		});

		return view;
	}

	@Override
	public int getStatusBarColorId() {
		return getSettings().isLightContent() ? R.color.status_bar_color_light : R.color.status_bar_color_dark;
	}

	private int resolveResourceId(final Activity activity, final int attr) {
		final TypedValue typedvalueattr = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			SettingsMainFragment settingsMainFragment = new SettingsMainFragment();
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, settingsMainFragment, SettingsMainFragment.TAG)
					.addToBackStack(SettingsMainFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}