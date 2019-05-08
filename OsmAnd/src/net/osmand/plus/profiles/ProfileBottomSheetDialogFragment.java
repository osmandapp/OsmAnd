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
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;

public class ProfileBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil.getLog(ProfileBottomSheetDialogFragment.class);

	private List<ProfileDataObject> profiles;
	private ProfileTypeDialogListener listener;
	private ProfileTypeDialogListener listListener;
	private RecyclerView recyclerView;
	private ProfileTypeAdapter adapter;


	public final static String TYPE_APP_PROFILE = "base_profiles";
	public final static String TYPE_NAV_PROFILE = "routing_profiles";
	private String type;

	public void setProfileTypeListener(ProfileTypeDialogListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState) {

		Bundle args = getArguments();
		if (args != null) {
			if (args.get(TYPE_NAV_PROFILE) != null) {
				profiles = args.getParcelableArrayList(TYPE_NAV_PROFILE);
				type = TYPE_NAV_PROFILE;
			} else if (args.get(TYPE_APP_PROFILE) != null) {
				profiles = args.getParcelableArrayList(TYPE_APP_PROFILE);
				type = TYPE_APP_PROFILE;
			} else {
				//todo notify on empty list;
				dismiss();
			}

		}

		final int themeRes = getMyApplication().getSettings().isLightContent()
			? R.style.OsmandLightTheme
			: R.style.OsmandDarkTheme;
		View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
			R.layout.bottom_sheet_select_type_fragment, null);
		if (type.equals(TYPE_APP_PROFILE)) {
			TextView fragmentDescription = view.findViewById(R.id.dialog_description_text);
			fragmentDescription.setVisibility(View.VISIBLE);
			fragmentDescription.setText(
				"The new Application Profile should be based on one of the default App Profiles. Selected Profile defines basic settings: setup of Widgets, units of speed and distance. In string below Profile's name, you could learn which Navigation Profiles are suitable for each Application Profile.");
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


		public ProfileTypeAdapter(List<ProfileDataObject> items, boolean isNightMode,
			ProfileTypeDialogListener listener) {
			this.items = items;
			this.isNightMode = isNightMode;
			this.listener = listener;
		}

		public void updateData(List<RoutingProfile> newItems) {
			items.clear();
			items.addAll(newItems);
			notifyDataSetChanged();
		}

		@NonNull
		@Override
		public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ItemViewHolder(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {
			final int pos = holder.getAdapterPosition();
			final ProfileDataObject item = items.get(position);
			holder.title.setText(item.getName());
			if (item.isSelected()) {
			holder.icon.setImageDrawable(getIcon(item.getIconRes(), isNightMode
				? R.color.active_buttons_and_links_dark
				: R.color.active_buttons_and_links_light));
			} else {
				holder.icon.setImageDrawable(getIcon(item.getIconRes(), R.color.icon_color));
			}





			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.onSelectedType(pos);
					holder.radioButton.setChecked(true);
					notifyItemChanged(previousSelection);
					previousSelection = pos;

					if (item instanceof RoutingProfile) {
						((RoutingProfile) items.get(pos)).setSelected(true);
						((RoutingProfile) items.get(previousSelection)).setSelected(false);
					}
				}
			});
			if (item instanceof RoutingProfile) {
				holder.descr.setText(Algorithms
					.capitalizeFirstLetterAndLowercase(item.getDescription()));
				if (((RoutingProfile) item).isSelected()) {
					holder.radioButton.setChecked(true);
					previousSelection = position;
				} else {
					holder.radioButton.setChecked(false);
				}
			} else {
				holder.descr.setText(item.getDescription());
			}
		}

		@Override
		public int getItemCount() {
			return items.size();
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
