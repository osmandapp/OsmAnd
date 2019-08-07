package net.osmand.plus.settings.profiles;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.profiles.ProfileDataObject;
import net.osmand.plus.profiles.ProfileMenuAdapter;
import net.osmand.plus.profiles.ProfileMenuAdapter.ProfileMenuAdapterListener;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SettingsProfileFragment extends BaseOsmAndFragment {

	public static final String TAG = "SettingsProfileFragment";

	private static final Log LOG = PlatformUtil.getLog(SettingsProfileFragment.class);

	public static final String PROFILE_STRING_KEY = "string_key";
	public static final String IS_NEW_PROFILE = "new_profile";
	public static final String IS_USER_PROFILE = "user_profile";


	private ProfileMenuAdapter adapter;
	private RecyclerView recyclerView;
	private LinearLayout addNewProfileBtn;

	ProfileMenuAdapterListener listener = null;
	SelectProfileBottomSheetDialogFragment.SelectProfileListener typeListener = null;

	private List<ApplicationMode> allAppModes;
	private Set<ApplicationMode> availableAppModes;
	private List<ProfileDataObject> baseProfiles;

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

		View view = inflater.inflate(R.layout.profiles_list_fragment, container, false);

		AndroidUtils.addStatusBarPadding21v(getContext(), view);

		Toolbar tb = (Toolbar) view.findViewById(R.id.toolbar);
		tb.setTitle(R.string.application_profiles);

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

		recyclerView = view.findViewById(R.id.profiles_list);
		addNewProfileBtn = view.findViewById(R.id.add_profile_btn);

		addNewProfileBtn.setOnClickListener(new OnClickListener() {


			@Override
			public void onClick(View v) {
				final SelectProfileBottomSheetDialogFragment dialog = new SelectProfileBottomSheetDialogFragment();
				Bundle bundle = new Bundle();
				bundle.putString(SelectProfileBottomSheetDialogFragment.DIALOG_TYPE, SelectProfileBottomSheetDialogFragment.TYPE_BASE_APP_PROFILE);
				dialog.setArguments(bundle);
				if (getActivity() != null) {
					getActivity().getSupportFragmentManager().beginTransaction()
							.add(dialog, "select_base_type").commitAllowingStateLoss();
				}
			}
		});

		adapter = new ProfileMenuAdapter(allAppModes, availableAppModes, getMyApplication(), null);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);
		return view;
	}

	private int resolveResourceId(final Activity activity, final int attr) {
		final TypedValue typedvalueattr = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	@Override
	public int getStatusBarColorId() {
		return getSettings().isLightContent() ? R.color.status_bar_color_light : R.color.status_bar_color_dark;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (listener == null) {
			listener = new ProfileMenuAdapterListener() {
				@Override
				public void onProfileSelected(ApplicationMode am, boolean selected) {
					if (selected) {
						availableAppModes.add(am);
					} else {
						availableAppModes.remove(am);
					}
					ApplicationMode.changeProfileAvailability(am, selected, getMyApplication());
				}

				@Override
				public void onProfilePressed(ApplicationMode item) {
					Bundle args = new Bundle();
					args.putString(PROFILE_STRING_KEY, item.getStringKey());
					if (item.isCustomProfile()) {
						args.putBoolean(IS_USER_PROFILE, true);
					}
					EditProfileFragment editProfileFragment = new EditProfileFragment();
					editProfileFragment.setArguments(args);
					getActivity().getSupportFragmentManager()
							.beginTransaction()
							.add(R.id.fragmentContainer, editProfileFragment, EditProfileFragment.TAG)
							.addToBackStack(EditProfileFragment.TAG)
							.commit();
				}

				@Override
				public void onButtonPressed() {
				}
			};
		}
		adapter.setListener(listener);

		getBaseProfileListener();

		allAppModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		allAppModes.remove(ApplicationMode.DEFAULT);
		adapter.updateItemsList(allAppModes, new LinkedHashSet<>(ApplicationMode.values(getMyApplication())));
	}

	SelectProfileBottomSheetDialogFragment.SelectProfileListener getBaseProfileListener() {
		if (typeListener == null) {
			typeListener = new SelectProfileBottomSheetDialogFragment.SelectProfileListener() {
				@Override
				public void onSelectedType(int pos, String stringRes) {
					Bundle args = new Bundle();
					args.putBoolean(IS_NEW_PROFILE, true);
					args.putBoolean(IS_USER_PROFILE, true);
					args.putString(PROFILE_STRING_KEY, baseProfiles.get(pos).getStringKey());

					EditProfileFragment editProfileFragment = new EditProfileFragment();
					editProfileFragment.setArguments(args);
					getActivity().getSupportFragmentManager()
							.beginTransaction()
							.add(R.id.fragmentContainer, editProfileFragment, EditProfileFragment.TAG)
							.addToBackStack(EditProfileFragment.TAG)
							.commit();
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
}