package net.osmand.plus.profiles;

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
import org.apache.commons.logging.Log;

public class SettingsProfileFragment extends BaseOsmAndFragment {

	private static final Log LOG = PlatformUtil.getLog(SettingsProfileFragment.class);

	private ProfileMenuAdapter adapter;
	private RecyclerView recyclerView;
	private LinearLayout addProfileBtn;

	ProfileListener listener = null;

	private List<ApplicationMode> allDefaultModes;
	private Set<ApplicationMode> selectedDefaultModes;
	private List<AppProfile> profilesList;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Context ctx = getMyApplication().getApplicationContext();
		profilesList = new ArrayList<>();
		allDefaultModes = ApplicationMode.allPossibleValues();
		allDefaultModes.remove(ApplicationMode.DEFAULT);
		allDefaultModes.remove(ApplicationMode.AIRCRAFT);
		allDefaultModes.remove(ApplicationMode.HIKING);
		allDefaultModes.remove(ApplicationMode.TRAIN);
		selectedDefaultModes = new LinkedHashSet<>(ApplicationMode.values(getMyApplication()));
		selectedDefaultModes.remove(ApplicationMode.DEFAULT);
		for (ApplicationMode am : allDefaultModes) {

			AppProfile profileItem = new AppProfile(
				am.getSmallIconDark(),
				am.toHumanStringCtx(getMyApplication().getApplicationContext()),
				getNavType(am, ctx));
			if (selectedDefaultModes.contains(am)) {
				profileItem.setSelected(true);
			}
			profilesList.add(profileItem);
		}
	}

	private String getNavType(ApplicationMode am, Context ctx) {
		if (am.getParent() != null) {
			return getNavType(am.getParent(), ctx);
		} else {
			switch(am.getStringKey()) {
				case "car":
					return ctx.getResources().getString(R.string.rendering_value_car_name);
				case "bicycle":
					return ctx.getResources().getString(R.string.rendering_value_bicycle_name);
				case "pedestrian":
					return ctx.getResources().getString(R.string.rendering_value_pedestrian_name);
				case "public_transport":
					return ctx.getResources().getString(R.string.app_mode_public_transport);
				case "boat":
					return ctx.getResources().getString(R.string.app_mode_boat);
			}
		}
		return "";
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.profiles_list_fragment, container, false);
		recyclerView = view.findViewById(R.id.profiles_list);
		addProfileBtn = view.findViewById(R.id.add_profile_btn);
		addProfileBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//todo add new profile;
			}
		});
		listener = new ProfileListener() {
			@Override
			public void changeProfileStatus(AppProfile item, boolean isSelected) {
				LOG.debug(item.getTitle() + " - " + isSelected);
				StringBuilder vls = new StringBuilder(ApplicationMode.DEFAULT.getStringKey()+",");
				ApplicationMode mode = null;
				for (ApplicationMode sam : allDefaultModes) {
					if (sam.toHumanString(getContext()).equals(item.getTitle())) {
						mode = sam;
					}
				}

				if(isSelected && mode != null) {
					selectedDefaultModes.add(mode);
				} else if (mode != null) {
					selectedDefaultModes.remove(mode);
				}

				for (ApplicationMode sam : selectedDefaultModes) {
					vls.append(sam.getStringKey()).append(",");
				}
				getSettings().AVAILABLE_APP_MODES.set(vls.toString());

			}

			@Override
			public void editProfile(AppProfile item) {
				Intent intent = new Intent(getActivity(), SelectedProfileActivity.class);
				intent.putExtra("profile", item);
				startActivity(intent);
			}
		};
		adapter = new ProfileMenuAdapter(profilesList, getMyApplication(), listener);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);


		return view;
	}


}
