package net.osmand.plus.profiles;



import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_BASE_APP_PROFILE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SelectProfileListener;

import org.apache.commons.logging.Log;

public class SettingsProfileFragment extends BaseOsmAndFragment 
		implements ConfigureProfileMenuAdapter.ProfileSelectedListener, AbstractProfileMenuAdapter.ProfilePressedListener{

	private static final Log LOG = PlatformUtil.getLog(SettingsProfileFragment.class);

	public static final String PROFILE_STRING_KEY = "string_key";
	public static final String IS_NEW_PROFILE = "new_profile";
	public static final String IS_USER_PROFILE = "user_profile";


	private ConfigureProfileMenuAdapter adapter;
	private	SelectProfileListener typeListener = null;

	private List<ApplicationMode> allAppModes;
	private Set<ApplicationMode> availableAppModes;
	private List<ProfileDataObject> baseProfiles;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		allAppModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		allAppModes.remove(ApplicationMode.DEFAULT);
		availableAppModes = new LinkedHashSet<>(ApplicationMode.values(getMyApplication()));
		availableAppModes.remove(ApplicationMode.DEFAULT);
		baseProfiles = getBaseProfiles(getMyActivity());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {
		nightMode = !requireSettings().isLightContent();

		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		Context themedContext = new ContextThemeWrapper(getContext(), themeRes);
		View view = inflater.cloneInContext(themedContext).inflate(R.layout.profiles_list_fragment, container, false);

		AppBarLayout appBar = (AppBarLayout) view.findViewById(R.id.appbar);
		if (!(getActivity() instanceof SettingsProfileActivity)) {
			AndroidUtils.addStatusBarPadding21v(getContext(), view);
			ViewCompat.setElevation(appBar, 5.0f);

			TextView toolbarTitle = (TextView) view.findViewById(R.id.toolbar_title);
			toolbarTitle.setText(R.string.application_profiles);

			View closeButton = view.findViewById(R.id.close_button);
			closeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MapActivity mapActivity = (MapActivity) getActivity();
					if (mapActivity != null) {
						mapActivity.onBackPressed();
					}
				}
			});
		} else {
			AndroidUiHelper.updateVisibility(appBar, false);
		}

		RecyclerView recyclerView = view.findViewById(R.id.profiles_list);
		LinearLayout addNewProfileBtn = view.findViewById(R.id.add_profile_btn);

		addNewProfileBtn.setOnClickListener(new OnClickListener() {


			@Override
			public void onClick(View v) {
				final SelectProfileBottomSheetDialogFragment dialog = new SelectProfileBottomSheetDialogFragment();
				Bundle bundle = new Bundle();
				bundle.putString(DIALOG_TYPE, TYPE_BASE_APP_PROFILE);
				dialog.setArguments(bundle);
				if (getActivity() != null) {
					getActivity().getSupportFragmentManager().beginTransaction()
						.add(dialog, "select_base_type").commitAllowingStateLoss();
				}
			}
		});

		adapter = new ConfigureProfileMenuAdapter(allAppModes, availableAppModes, getMyApplication(), null, !getMyApplication().getSettings().isLightContent());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.setProfilePressedListener(this);
		adapter.setProfileSelectedListener(this);
		getBaseProfileListener();

		allAppModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		allAppModes.remove(ApplicationMode.DEFAULT);
		adapter.updateItemsList(allAppModes, new LinkedHashSet<>(ApplicationMode.values(getMyApplication())));
	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	SelectProfileListener getBaseProfileListener() {
		if (typeListener == null) {
			typeListener = new SelectProfileListener() {
				@Override
				public void onSelectedType(int pos, String stringRes) {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						if (activity instanceof SettingsProfileActivity) {
							Intent intent = new Intent(getActivity(), EditProfileActivity.class);
							intent.putExtra(IS_NEW_PROFILE, true);
							intent.putExtra(IS_USER_PROFILE, true);
							intent.putExtra(PROFILE_STRING_KEY, baseProfiles.get(pos).getStringKey());
							activity.startActivity(intent);
						} else {
							FragmentManager fragmentManager = activity.getSupportFragmentManager();
							if (fragmentManager != null) {
								String profileKey = baseProfiles.get(pos).getStringKey();
								EditProfileFragment.showInstance(fragmentManager, true, true, profileKey);
							}
						}
					}
				}
			};
		}
		return typeListener;
	}

	static List<ProfileDataObject> getBaseProfiles(Context ctx) {
		List<ProfileDataObject> profiles = new ArrayList<>();
		for (ApplicationMode mode : ApplicationMode.getDefaultValues()) {
			if (mode != ApplicationMode.DEFAULT) {
				profiles.add(new ProfileDataObject(mode.toHumanString(ctx), mode.getDescription(ctx),
					mode.getStringKey(), mode.getIconRes(), false, mode.getIconColorInfo()));
			}
		}
		return profiles;
	}

	@Override
	public void onProfilePressed(ApplicationMode item) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (activity instanceof SettingsProfileActivity) {
				Intent intent = new Intent(getActivity(), EditProfileActivity.class);
				intent.putExtra(PROFILE_STRING_KEY, item.getStringKey());
				if (item.isCustomProfile()) {
					intent.putExtra(IS_USER_PROFILE, true);
				}
				activity.startActivity(intent);
			} else {
				FragmentManager fragmentManager = activity.getSupportFragmentManager();
				if (fragmentManager != null) {
					String profileKey = item.getStringKey();
					EditProfileFragment.showInstance(fragmentManager, false, item.isCustomProfile(), profileKey);
				}
			}
		}
	}

	@Override
	public void onProfileSelected(ApplicationMode item, boolean isChecked) {
		if (isChecked) {
			availableAppModes.add(item);
		} else {
			availableAppModes.remove(item);
		}
		ApplicationMode.changeProfileAvailability(item, isChecked, getMyApplication());
	}
}
