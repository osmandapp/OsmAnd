package net.osmand.plus.profiles;

import static net.osmand.plus.profiles.ProfileBottomSheetDialogFragment.TYPE_APP_PROFILE;

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
import net.osmand.plus.profiles.ProfileBottomSheetDialogFragment.ProfileTypeDialogListener;
import net.osmand.plus.profiles.ProfileMenuAdapter.ProfileListener;
import org.apache.commons.logging.Log;

public class SettingsProfileFragment extends BaseOsmAndFragment {

	private static final Log LOG = PlatformUtil.getLog(SettingsProfileFragment.class);

	private ProfileMenuAdapter adapter;
	private RecyclerView recyclerView;
	private LinearLayout addNewProfileBtn;

	ProfileListener listener = null;
	ProfileTypeDialogListener typeListener = null;

	private List<ApplicationMode> allAppModes;
	private Set<ApplicationMode> availableAppModes;
	private ArrayList<BaseProfile> baseProfiles;


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
			public void changeProfileStatus(ApplicationMode item, boolean isSelected) {
				StringBuilder vls = new StringBuilder(ApplicationMode.DEFAULT.getStringKey() + ",");
				ApplicationMode mode = null;
				for (ApplicationMode sam : allAppModes) {
					if (sam.getStringKey().equals(item.getStringKey())) {
						mode = sam;
					}
				}

				if(isSelected && mode != null) {
					availableAppModes.add(mode);
				} else if (mode != null) {
					availableAppModes.remove(mode);
					if (getSettings() != null && getSettings().APPLICATION_MODE.get() == mode) {
						getSettings().APPLICATION_MODE.set(ApplicationMode.DEFAULT);
					}
				}

				for (ApplicationMode sam : availableAppModes) {
					vls.append(sam.getStringKey()).append(",");
				}
				if (getSettings() != null) {
					getSettings().AVAILABLE_APP_MODES.set(vls.toString());
				}
			}

			@Override
			public void editProfile(ApplicationMode item) {
				Intent intent = new Intent(getActivity(), EditProfileActivity.class);
				intent.putExtra("stringKey", item.getStringKey());
				intent.putExtra("isNew", false);
				if (item.getUserProfileName() != null && !item.getUserProfileName().isEmpty()) {
					intent.putExtra("isUserProfile", true);
				}
				startActivity(intent);
			}
		};

		typeListener = new ProfileTypeDialogListener() {
			@Override
			public void onSelectedType(int pos) {
				LOG.debug("Base profile: " + baseProfiles.get(pos).getName());
				Intent intent = new Intent(getActivity(), EditProfileActivity.class);
				intent.putExtra("isNew", true);
				intent.putExtra("isUserProfile", true);
				intent.putExtra("stringKey", baseProfiles.get(pos).getStringKey());
				startActivity(intent);
			}
		};

		View view = inflater.inflate(R.layout.profiles_list_fragment, container, false);
		recyclerView = view.findViewById(R.id.profiles_list);
		addNewProfileBtn = view.findViewById(R.id.add_profile_btn);

		addNewProfileBtn.setOnClickListener(new OnClickListener() {


			@Override
			public void onClick(View v) {
				final ProfileBottomSheetDialogFragment dialog = new ProfileBottomSheetDialogFragment();
				dialog.setProfileTypeListener(typeListener);
				Bundle bundle = new Bundle();
				bundle.putParcelableArrayList(TYPE_APP_PROFILE, baseProfiles);
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

		typeListener = new ProfileTypeDialogListener() {
			@Override
			public void onSelectedType(int pos) {
				LOG.debug("Base profile: " + baseProfiles.get(pos).getName());
				Intent intent = new Intent(getActivity(), EditProfileActivity.class);
				intent.putExtra("isNew", true);
				intent.putExtra("isUserProfile", true);
				intent.putExtra("stringKey", baseProfiles.get(pos).getStringKey());
				startActivity(intent);
			}
		};

		allAppModes = ApplicationMode.allPossibleValues();
		allAppModes.remove(ApplicationMode.DEFAULT);

		adapter.updateItemsList(allAppModes, new LinkedHashSet<>(ApplicationMode.values(getMyApplication())));
	}

	ProfileTypeDialogListener getBaseProfileListener() {
		if (typeListener == null) {
			typeListener = new ProfileTypeDialogListener() {
				@Override
				public void onSelectedType(int pos) {
					LOG.debug("Base profile: " + baseProfiles.get(pos).getName());
					Intent intent = new Intent(getActivity(), EditProfileActivity.class);
					intent.putExtra("isNew", true);
					intent.putExtra("isUserProfile", true);
					intent.putExtra("stringKey", baseProfiles.get(pos).getStringKey());
					startActivity(intent);
				}
			};
		}
		return typeListener;
	}

	static ArrayList<BaseProfile> getBaseProfiles(Context ctx) {
		ArrayList<BaseProfile> profiles = new ArrayList<>();
		for (ApplicationMode mode : ApplicationMode.getDefaultValues()) {
			if (mode != ApplicationMode.DEFAULT) {
				String descr = "";
				switch (mode.getStringKey()) {
					case "car":
						descr = "Car, Truck, Motorcycle";
						break;
					case "bicycle":
						descr = "MBT, Moped, Skiing, Horse";
						break;
					case "pedestrian":
						descr = "Walking, Hiking, Running";
						break;
					case "public_transport":
						descr = "All PT types";
						break;
					case "boat":
						descr = "Ship, Rowing, Sailing";
						break;
					case "aircraft":
						descr = "Airplane, Gliding";
						break;
				}
				profiles.add(new BaseProfile(mode.getStringKey(), mode.toHumanString(ctx), descr,
					mode.getSmallIconDark(), false));
			}
		}
		return profiles;
	}

}
