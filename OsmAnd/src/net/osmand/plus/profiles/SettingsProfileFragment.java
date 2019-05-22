package net.osmand.plus.profiles;



import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_APP_PROFILE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.profiles.ProfileMenuAdapter.ProfileListener;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SelectProfileListener;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;

public class SettingsProfileFragment extends BaseOsmAndFragment {

	private static final Log LOG = PlatformUtil.getLog(SettingsProfileFragment.class);

	public static final String PROFILE_STRING_KEY = "string_key";
	public static final String IS_NEW_PROFILE = "new_profile";
	public static final String IS_USER_PROFILE = "user_profile";


	private ProfileMenuAdapter adapter;
	private RecyclerView recyclerView;
	private LinearLayout addNewProfileBtn;

	ProfileListener listener = null;
	SelectProfileListener typeListener = null;

	private List<ApplicationMode> allAppModes;
	private Set<ApplicationMode> availableAppModes;
	private List<ProfileDataObject> baseProfiles;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		allAppModes = ApplicationMode.allPossibleValues();
		allAppModes.remove(ApplicationMode.DEFAULT);
		availableAppModes = new LinkedHashSet<>(ApplicationMode.values(getMyApplication()));
		availableAppModes.remove(ApplicationMode.DEFAULT);
		baseProfiles = getBaseProfiles(getMyActivity());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {

		listener = new ProfileListener() {
			@Override
			public void changeProfileStatus(ApplicationMode am, boolean isSelected) {
				if(isSelected) {
					availableAppModes.add(am);
				} else {
					availableAppModes.remove(am);
				}
				ApplicationMode.changeProfileStatus(am, isSelected, getMyApplication());
			}

			@Override
			public void editProfile(ApplicationMode item) {
				Intent intent = new Intent(getActivity(), EditProfileActivity.class);
				intent.putExtra(PROFILE_STRING_KEY, item.getStringKey());
				if (!Algorithms.isEmpty(item.getUserProfileName())) {
					intent.putExtra(IS_USER_PROFILE, true);
				}
				startActivity(intent);
			}
		};

		View view = inflater.inflate(R.layout.profiles_list_fragment, container, false);
		recyclerView = view.findViewById(R.id.profiles_list);
		addNewProfileBtn = view.findViewById(R.id.add_profile_btn);

		addNewProfileBtn.setOnClickListener(new OnClickListener() {


			@Override
			public void onClick(View v) {
				final SelectProfileBottomSheetDialogFragment dialog = new SelectProfileBottomSheetDialogFragment();
				Bundle bundle = new Bundle();
				bundle.putString(DIALOG_TYPE, TYPE_APP_PROFILE);
				dialog.setArguments(bundle);
				if (getActivity() != null) {
					getActivity().getSupportFragmentManager().beginTransaction()
						.add(dialog, "select_base_type").commitAllowingStateLoss();
				}
			}
		});

		adapter = new ProfileMenuAdapter(allAppModes, availableAppModes, getMyApplication(), listener);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		getBaseProfileListener();

		allAppModes = ApplicationMode.allPossibleValues();
		allAppModes.remove(ApplicationMode.DEFAULT);
		adapter.updateItemsList(allAppModes, new LinkedHashSet<>(ApplicationMode.values(getMyApplication())));
	}

	SelectProfileListener getBaseProfileListener() {
		if (typeListener == null) {
			typeListener = new SelectProfileListener() {
				@Override
				public void onSelectedType(int pos) {
					Intent intent = new Intent(getActivity(), EditProfileActivity.class);
					intent.putExtra(IS_NEW_PROFILE, true);
					intent.putExtra(IS_USER_PROFILE, true);
					intent.putExtra(PROFILE_STRING_KEY, baseProfiles.get(pos).getStringKey());
					startActivity(intent);
				}
			};
		}
		return typeListener;
	}

	static List<ProfileDataObject> getBaseProfiles(Context ctx) {
		List<ProfileDataObject> profiles = new ArrayList<>();
		for (ApplicationMode mode : ApplicationMode.getDefaultValues()) {
			if (mode != ApplicationMode.DEFAULT) {
				profiles.add(new ProfileDataObject( mode.toHumanString(ctx),
					ctx.getString(BaseProfilesDescr.valueOf(mode.getStringKey().toUpperCase()).getDescrRes()),
					mode.getStringKey(), mode.getSmallIconDark(), false));
			}
		}
		return profiles;
	}

	public enum BaseProfilesDescr {
		CAR(R.string.base_profile_descr_car),
		BICYCLE(R.string.base_profile_descr_bicycle),
		PEDESTRIAN(R.string.base_profile_descr_pedestrian),
		PUBLIC_TRANSPORT(R.string.base_profile_descr_public_transport),
		BOAT(R.string.base_profile_descr_boat),
		AIRCRAFT(R.string.base_profile_descr_aircraft);

		private int descrRes;

		BaseProfilesDescr(int descrRes) {
			this.descrRes = descrRes;
		}

		public int getDescrRes() {
			return descrRes;
		}
	}



}
