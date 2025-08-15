package net.osmand.plus.plugins.rastermaps;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseBottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.MapLayerType;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class MapLayerBottomSheet extends BaseBottomSheetDialogFragment {

	private static final String TAG = MapLayerBottomSheet.class.getSimpleName();
	private static final String KEY_LAYER_NAMES = "KEY_LAYER_NAMES";
	private static final String KEY_UPDATE_TILES = "KEY_UPDATE_TILES";

	private List<MapLayerType> layerTypes;
	private boolean updateTiles;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		layerTypes = (List<MapLayerType>) AndroidUtils.getSerializable(args, KEY_LAYER_NAMES, ArrayList.class);
		updateTiles = args.getBoolean(KEY_UPDATE_TILES);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.bottom_sheet_track_group_list);

		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.select_layer);
		title.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode));

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getThemedContext()));
		recyclerView.setAdapter(new MapLayersAdapter(layerTypes));

		return view;
	}

	@NonNull
	@Override
	public ThemeUsageContext getThemeUsageContext() {
		return ThemeUsageContext.OVER_MAP;
	}

	public class MapLayersAdapter extends RecyclerView.Adapter<MapLayerViewHolder> {

		private final List<MapLayerType> layerTypes;

		public MapLayersAdapter(@NonNull List<MapLayerType> layerTypes) {
			this.layerTypes = layerTypes;
		}

		@NonNull
		@Override
		public MapLayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			return new MapLayerViewHolder(inflater.inflate(R.layout.list_item_icon_and_menu, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull MapLayerViewHolder holder, int position) {
			MapLayerType layerType = layerTypes.get(position);

			holder.title.setText(layerType.getNameId());
			String mapSource = layerType.getMapSourceNameForLayer(app);
			String mapSourceTitle = settings.getTileSourceTitle(mapSource);
			holder.description.setText(mapSourceTitle);
			holder.layerIcon.setImageResource(layerType.getIconId());
			int iconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
			holder.layerIcon.setImageTintList(ColorStateList.valueOf(iconColor));
			holder.itemView.setOnClickListener(view -> {
				downloadTilesForLayer(app, requireFragmentManager(), layerType, updateTiles);
				dismiss();
			});
			AndroidUiHelper.updateVisibility(holder.divider, true);
			AndroidUiHelper.updateVisibility(holder.togle, false);
		}

		@Override
		public int getItemCount() {
			return layerTypes.size();
		}
	}

	static class MapLayerViewHolder extends RecyclerView.ViewHolder {

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

	private static void downloadTilesForLayer(@NonNull OsmandApplication app,
	                                          @NonNull FragmentManager manager,
	                                          @NonNull MapLayerType layerType,
	                                          boolean updateTiles) {
		if (DownloadTilesFragment.shouldShowDialog(app)) {
			DownloadTilesFragment.showInstance(manager, updateTiles, layerType);
		} else {
			app.showShortToastMessage(R.string.maps_could_not_be_downloaded);
		}
	}

	public static void showInstance(@NonNull OsmandApplication app,
	                                @NonNull FragmentManager manager,
	                                @NonNull List<MapLayerType> layerTypes,
	                                boolean updateTiles) {
		if (layerTypes.size() == 1) {
			downloadTilesForLayer(app, manager, layerTypes.get(0), updateTiles);
		} else if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			MapLayerBottomSheet fragment = new MapLayerBottomSheet();
			Bundle args = new Bundle();
			args.putBoolean(KEY_UPDATE_TILES, updateTiles);
			args.putSerializable(KEY_LAYER_NAMES, new ArrayList<>(layerTypes));
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}
}
