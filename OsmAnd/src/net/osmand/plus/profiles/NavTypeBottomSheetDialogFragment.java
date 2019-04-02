package net.osmand.plus.profiles;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import java.util.EnumSet;
import java.util.List;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import org.apache.commons.logging.Log;

public class NavTypeBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil.getLog(NavTypeBottomSheetDialogFragment.class);

	List<RoutingProfile> routingProfiles;
	NavTypeDialogListener listener;
	RecyclerView recyclerView;
	NavTypeAdapter adapter;

	public void setListener(NavTypeDialogListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Bundle args = getArguments();
		if (args != null) {
			routingProfiles = args.getParcelableArrayList("routing_profiles");
		}

		final int themeRes = getMyApplication().getSettings().isLightContent() ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.bottom_sheet_nav_type_fragment, null);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		listener = new NavTypeDialogListener() {
			@Override
			public void selectedNavType(int pos) {
				for (int i = 0; i < routingProfiles.size(); i++) {
					routingProfiles.get(i).setSelected(false);
				}
				routingProfiles.get(pos).setSelected(true);

			}
		};
		recyclerView = view.findViewById(R.id.menu_list_view);
		adapter = new NavTypeAdapter(getContext(), routingProfiles, isNightMode(getMyApplication()), listener);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);

		return view;
	}

	private static boolean isNightMode(OsmandApplication ctx) {
		return !ctx.getSettings().isLightContent();
	}

	class NavTypeAdapter extends RecyclerView.Adapter<ItemViewHolder> {
		private final Context context;
		private final List<RoutingProfile> items;
		private final boolean isNightMode;
		private NavTypeDialogListener listener;

		public NavTypeAdapter(@NonNull Context context, @NonNull List<RoutingProfile> objects,
			@NonNull boolean isNightMode, NavTypeDialogListener listener) {
			this.context = context;
			this.items = objects;
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
			return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {
			final int pos = position;
			final RoutingProfile item = items.get(position);
			holder.title.setText(item.getName());
			holder.descr.setText(item.getOrigin());
			holder.icon.setImageDrawable(getIcon(item.getIconRes(), isNightMode
					? R.color.active_buttons_and_links_dark
					: R.color.active_buttons_and_links_light));
			if(item.isSelected()) {
				holder.radioButton.setEnabled(true);
			}
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.selectedNavType(pos);
					for (int i = 0; i < routingProfiles.size(); i++) {
						if (!routingProfiles.get(i).equals(item)) {
							routingProfiles.get(i).setSelected(true);
						} else {
							routingProfiles.get(i).setSelected(false);
						}
						notifyItemChanged(i);
					}
				}
			});
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

	interface NavTypeDialogListener {
		void selectedNavType(int pos);
	}

}
