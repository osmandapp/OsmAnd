package net.osmand.plus.profiles;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import org.apache.commons.logging.Log;

public class ProfileBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil.getLog(ProfileBottomSheetDialogFragment.class);

	private List<ProfileDataObject> profiles;
	private ProfileTypeDialogListener listener;
	private ProfileTypeDialogListener listListener;
	private RecyclerView recyclerView;
	private ProfileTypeAdapter adapter;
	private TextView titleTV;
	private TextView fragmentDescriptionTV;

	private String selectedKey = null;

	public final static String DIALOG_TYPE = "dialog_type";
	public final static String TYPE_APP_PROFILE = "base_profiles";
	public final static String TYPE_NAV_PROFILE = "routing_profiles";
	public final static String SELECTED_KEY = "selected_base";
	private String type;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		profiles = new ArrayList<>();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState) {

		Bundle args = getArguments();
		if (args != null) {
			if (args.get(DIALOG_TYPE) != null) {
				type = args.getString(DIALOG_TYPE);
			} else {
				dismiss();
			}

			selectedKey = args.getString(SELECTED_KEY, "");

			if (type.equals(TYPE_NAV_PROFILE) && getMyApplication() != null) {
				profiles.addAll(EditProfileFragment.getRoutingProfiles(getMyApplication()));
			} else if (type.equals(TYPE_APP_PROFILE)) {
				profiles.addAll(SettingsProfileFragment.getBaseProfiles(getActivity()));
			}
		}

		final int themeRes = getMyApplication().getSettings().isLightContent()
			? R.style.OsmandLightTheme
			: R.style.OsmandDarkTheme;
		View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
			R.layout.bottom_sheet_select_type_fragment, null);

		titleTV = view.findViewById(R.id.dialog_title);
		fragmentDescriptionTV = view.findViewById(R.id.dialog_description_text);

		if (type.equals(TYPE_APP_PROFILE)) {
			titleTV.setText(R.string.select_base_profile_dialog_title);
			fragmentDescriptionTV.setVisibility(View.VISIBLE);
			fragmentDescriptionTV.setText(R.string.select_base_profile_dialog_message);
		} else if (type.equals(TYPE_NAV_PROFILE)) {
			titleTV.setText(R.string.select_nav_profile_dialog_title);
		}
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		listListener = new ProfileTypeDialogListener() {
			@Override
			public void onSelectedType(int pos) {
				if (listener == null) {
					getListener();
				}
				listener.onSelectedType(pos);
				dismiss();
			}
		};
		recyclerView = view.findViewById(R.id.menu_list_view);
		adapter = new ProfileTypeAdapter(profiles, isNightMode(getMyApplication()),
			listListener);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);
		Button cancelBtn = view.findViewById(R.id.cancel_selection);
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		return view;
	}

	private static boolean isNightMode(OsmandApplication ctx) {
		return !ctx.getSettings().isLightContent();
	}

	class ProfileTypeAdapter extends RecyclerView.Adapter<ItemViewHolder> {

		private final List<ProfileDataObject> items;
		private final boolean isNightMode;
		private ProfileTypeDialogListener listener;
		private int previousSelection;


		ProfileTypeAdapter(List<ProfileDataObject> items, boolean isNightMode,
			ProfileTypeDialogListener listener) {
			this.items = items;
			this.isNightMode = isNightMode;
			this.listener = listener;
		}

		@NonNull
		@Override
		public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ItemViewHolder(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull final ItemViewHolder holder, final int position) {
			final ProfileDataObject item = items.get(position);
			holder.title.setText(item.getName());


			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
						listener.onSelectedType(position);

					holder.radioButton.setChecked(true);

					if (item instanceof RoutingProfileDataObject) {
						items.get(position).setSelected(true);
						items.get(previousSelection).setSelected(false);
					}
					notifyItemChanged(previousSelection);
					previousSelection = position;
				}
			});

			if(item instanceof RoutingProfileDataObject) {
				if (((RoutingProfileDataObject) item).getFileName() != null) {
					holder.descr
						.setText(String.format("From %s", ((RoutingProfileDataObject) item).getFileName()));
				} else {
					holder.descr
						.setText(item.getDescription());
				}
			} else {
				holder.descr.setText(item.getDescription());
			}

			if (selectedKey != null && selectedKey.equals(item.getStringKey())) {
				holder.radioButton.setChecked(true);
				previousSelection = position;
				holder.icon.setImageDrawable(getIcon(item.getIconRes(), isNightMode
					? R.color.active_buttons_and_links_dark
					: R.color.active_buttons_and_links_light));
			} else {
				holder.radioButton.setChecked(false);
				holder.icon.setImageDrawable(getIcon(item.getIconRes(), R.color.icon_color));
			}
		}

		@Override
		public int getItemCount() {
			return items.size();
		}
	}

	void getListener() {
		if (getActivity() != null && getActivity() instanceof EditProfileActivity) {
			EditProfileFragment f = (EditProfileFragment) getActivity().getSupportFragmentManager()
				.findFragmentByTag(EditProfileActivity.EDIT_PROFILE_FRAGMENT_TAG);
			if (type.equals(TYPE_APP_PROFILE)) {
				listener = f.getBaseProfileListener();
			} else if (type.equals(TYPE_NAV_PROFILE)) {
				listener = f.getNavProfileListener();
			}
		} else if (getActivity() != null && getActivity() instanceof SettingsProfileActivity) {
			SettingsProfileFragment f = (SettingsProfileFragment) getActivity().getSupportFragmentManager()
				.findFragmentByTag(SettingsProfileActivity.SETTINGS_PROFILE_FRAGMENT_TAG);
			listener = f.getBaseProfileListener();
		}
	}

	class ItemViewHolder extends RecyclerView.ViewHolder {
		TextView title, descr;
		RadioButton radioButton;
		ImageView icon;

		public ItemViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			descr = itemView.findViewById(R.id.subtitle);
			radioButton = itemView.findViewById(R.id.compound_button);
			icon = itemView.findViewById(R.id.icon);
		}
	}

	interface ProfileTypeDialogListener {
		void onSelectedType(int pos);
	}
}
