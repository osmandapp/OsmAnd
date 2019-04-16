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

	private List<ApplicationMode> allAppModes;
	private Set<ApplicationMode> availableAppModes;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Context ctx = getMyApplication().getApplicationContext();
		allAppModes = ApplicationMode.allPossibleValues();
		allAppModes.remove(ApplicationMode.DEFAULT);
		allAppModes.remove(ApplicationMode.AIRCRAFT);
		allAppModes.remove(ApplicationMode.HIKING);
		allAppModes.remove(ApplicationMode.TRAIN);
		availableAppModes = new LinkedHashSet<>(ApplicationMode.values(getMyApplication()));
		availableAppModes.remove(ApplicationMode.DEFAULT);
	}

//	private String getNavType(ApplicationMode am, Context ctx) {
//		if (am.getParent() != null) {
//			return getNavType(am.getParent(), ctx);
//		} else {
//			switch(am.getStringKey()) {
//				case "car":
//					return ctx.getResources().getString(R.string.rendering_value_car_name);
//				case "bicycle":
//					return ctx.getResources().getString(R.string.rendering_value_bicycle_name);
//				case "pedestrian":
//					return ctx.getResources().getString(R.string.rendering_value_pedestrian_name);
//				case "public_transport":
//					return ctx.getResources().getString(R.string.app_mode_public_transport);
//				case "boat":
//					return ctx.getResources().getString(R.string.app_mode_boat);
//			}
//		}
//		return "";
//	}

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


			}
		});

		listener = new ProfileListener() {
			@Override
			public void changeProfileStatus(ApplicationMode item, boolean isSelected) {
				LOG.debug(getString(item.getStringResource()) + " - " + isSelected);
				StringBuilder vls = new StringBuilder(ApplicationMode.DEFAULT.getStringKey()+",");
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
				}

				for (ApplicationMode sam : availableAppModes) {
					vls.append(sam.getStringKey()).append(",");
				}
				getSettings().AVAILABLE_APP_MODES.set(vls.toString());

			}

			@Override
			public void editProfile(ApplicationMode item) {
				Intent intent = new Intent(getActivity(), SelectedProfileActivity.class);
				intent.putExtra("stringKey", item.getStringKey());
				startActivity(intent);
			}
		};
		adapter = new ProfileMenuAdapter(allAppModes, availableAppModes, getMyApplication(), listener);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);




		return view;
	}


}
