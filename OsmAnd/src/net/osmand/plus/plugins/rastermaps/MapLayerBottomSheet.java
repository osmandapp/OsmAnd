package net.osmand.plus.plugins.rastermaps;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseBottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class MapLayerBottomSheet extends BaseBottomSheetDialogFragment {

	private static final String TAG = MapLayerBottomSheet.class.getSimpleName();
	private static final String KEY_LAYER_NAMES = "KEY_LAYER_NAMES";
	private static final String KEY_UPDATE_TILES = "KEY_UPDATE_TILES";

	private ArrayList<Integer> layerNameIds;
	private boolean nightMode;
	private boolean updateTiles;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = isNightMode(true);
		Bundle args = getArguments();
		layerNameIds = args.getIntegerArrayList(KEY_LAYER_NAMES);
		updateTiles = args.getBoolean(KEY_UPDATE_TILES);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		Context context = requireContext();
		inflater = UiUtilities.getInflater(context, nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_track_group_list, null);

		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.select_layer);
		title.setTextColor(ColorUtilities.getSecondaryTextColor(context, nightMode));

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));
		recyclerView.setAdapter(new MapLayersAdapter(layerNameIds));

		return view;
	}

	public class MapLayersAdapter extends RecyclerView.Adapter<MapLayersAdapter.MapLayerViewHolder> {

		private final List<Integer> layerNameIds;

		public MapLayersAdapter(@NonNull List<Integer> layerNameIds) {
			this.layerNameIds = layerNameIds;
		}

		@NonNull
		@Override
		public MapLayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			return new MapLayerViewHolder(inflater.inflate(R.layout.list_item_icon_and_menu, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull MapLayerViewHolder holder, int position) {
			Integer layerNameId = layerNameIds.get(position);
			holder.title.setText(app.getString(layerNameId));
			holder.description.setText(getMapSourceNameForLayer(layerNameId));
			holder.layerIcon.setImageResource(getLayerIcon(layerNameId));
			int iconColor = ColorUtilities.getColor(app, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
			holder.layerIcon.setImageTintList(ColorStateList.valueOf(iconColor));
			holder.itemView.setOnClickListener(view -> {
				downloadTilesForLayer(app, requireFragmentManager(), layerNameId, updateTiles);
				dismiss();
			});
			AndroidUiHelper.updateVisibility(holder.divider, true);
			AndroidUiHelper.updateVisibility(holder.togle, false);
		}

		private String getMapSourceNameForLayer(@StringRes int layerNameId) {
			if (layerNameId == R.string.map_source) {
				return app.getSettings().MAP_TILE_SOURCES.get();
			} else if (layerNameId == R.string.map_overlay) {
				return app.getSettings().MAP_OVERLAY.get();
			} else {
				return app.getSettings().MAP_UNDERLAY.get();
			}
		}

		private int getLayerIcon(@StringRes int layerNameId) {
			if (layerNameId == R.string.map_source) {
				return R.drawable.ic_world_globe_dark;
			} else if (layerNameId == R.string.map_overlay) {
				return R.drawable.ic_layer_top;
			} else {
				return R.drawable.ic_layer_bottom;
			}
		}

		@Override
		public int getItemCount() {
			return layerNameIds.size();
		}

		class MapLayerViewHolder extends RecyclerView.ViewHolder {

			private final TextView title;
			private final TextView description;
			private final ImageView layerIcon;
			private final SwitchCompat togle;
			private final View divider;

			public MapLayerViewHolder(@NonNull View itemView) {
				super(itemView);
				togle = itemView.findViewById(R.id.toggle_item);
				title = itemView.findViewById(R.id.title);
				description = itemView.findViewById(R.id.description);
				layerIcon = itemView.findViewById(R.id.icon);
				divider = itemView.findViewById(R.id.divider);
			}
		}
	}

	private static void downloadTilesForLayer(@NonNull OsmandApplication app,
	                                          @NonNull FragmentManager manager,
	                                          @StringRes int layerNameId,
	                                          boolean updateTiles) {
		if (DownloadTilesFragment.shouldShowDialog(app)) {
			DownloadTilesFragment.showInstance(manager, updateTiles, layerNameId);
		} else {
			app.showShortToastMessage(R.string.maps_could_not_be_downloaded);
		}
	}

	public static void showInstance(@NonNull OsmandApplication app,
	                                @NonNull FragmentManager manager,
	                                @NonNull ArrayList<Integer> layerNameIds,
	                                boolean updateTiles) {
		if (layerNameIds.size() == 1) {
			downloadTilesForLayer(app, manager, layerNameIds.get(0), updateTiles);
		} else {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				MapLayerBottomSheet fragment = new MapLayerBottomSheet();
				Bundle args = new Bundle();
				args.putBoolean(KEY_UPDATE_TILES, updateTiles);
				args.putIntegerArrayList(KEY_LAYER_NAMES, layerNameIds);
				fragment.setArguments(args);
				fragment.show(manager, TAG);
			}
		}
	}
}
