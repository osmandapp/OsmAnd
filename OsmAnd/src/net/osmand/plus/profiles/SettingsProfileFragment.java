package net.osmand.plus.profiles;

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
import net.sf.junidecode.App;
import org.apache.commons.logging.Log;

public class SettingsProfileFragment extends BaseOsmAndFragment {

	private static final Log LOG = PlatformUtil.getLog(SettingsProfileFragment.class);

	private ProfileMenuAdapter adapter;
	private RecyclerView recyclerView;
	private LinearLayout btn;

	ProfileListener listener = null;

	private List<ApplicationMode> allDefaultModes;
	private Set<ApplicationMode> selectedDefaultModes;
	private List<ProfileItem> profilesList;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		profilesList = new ArrayList<>();
		allDefaultModes = ApplicationMode.allPossibleValues();
		allDefaultModes.remove(ApplicationMode.DEFAULT);
		selectedDefaultModes = new LinkedHashSet<>(ApplicationMode.values(getMyApplication()));
		selectedDefaultModes.remove(ApplicationMode.DEFAULT);
		for (ApplicationMode am : allDefaultModes) {
			ProfileItem profileItem = new ProfileItem(
				am.getSmallIconDark(),
				am.toHumanStringCtx(getMyApplication().getApplicationContext()),
				am.toHumanStringCtx(getMyApplication().getApplicationContext()),
				true);
			if (selectedDefaultModes.contains(am)) {
				profileItem.setSelected(true);
			}
			profilesList.add(profileItem);
		}


	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.profiles_list_fragment, container, false);
		recyclerView = view.findViewById(R.id.profiles_list);
		btn = view.findViewById(R.id.add_profile_btn);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//todo add new profile;
			}
		});
		listener = new ProfileListener() {
			@Override
			public void changeProfileStatus(ProfileItem item, boolean isSelected) {
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
			public void editProfile(ProfileItem item) {

			}
		};
		adapter = new ProfileMenuAdapter(profilesList, getMyApplication(), listener);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);


		return view;
	}

	public class ProfileItem {
		private int iconRes;
		private String title;
		private String descr;
		private boolean isSelected;
		private boolean isAppDefault;

		public ProfileItem(int iconRes, String title, String descr, boolean isAppDefault) {
			this.iconRes = iconRes;
			this.title = title;
			this.descr = descr;
			this.isAppDefault = isAppDefault;
		}

		public int getIconRes() {
			return iconRes;
		}

		public void setIconRes(int iconRes) {
			this.iconRes = iconRes;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescr() {
			return descr;
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		public boolean isSelected() {
			return isSelected;
		}

		public void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}

		public boolean isAppDefault() {
			return isAppDefault;
		}

		public void setAppDefault(boolean appDefault) {
			isAppDefault = appDefault;
		}
	}
}
