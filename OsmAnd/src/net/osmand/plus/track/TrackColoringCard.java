package net.osmand.plus.track;

import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.GpxAppearanceAdapter.AppearanceListItem;
import net.osmand.plus.dialogs.GpxAppearanceAdapter.GpxAppearanceAdapterType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.FlowLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.osmand.plus.dialogs.GpxAppearanceAdapter.getAppearanceItems;

public class TrackColoringCard extends BaseCard {

	private SelectedGpxFile selectedGpxFile;

	private GradientScaleType selectedScaleType;

	@ColorInt
	private int selectedColor;

	public TrackColoringCard(MapActivity mapActivity, GpxSelectionHelper.SelectedGpxFile selectedGpxFile) {
		super(mapActivity);
		this.selectedGpxFile = selectedGpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_coloring_card;
	}

	@Override
	protected void updateContent() {
		updateHeader();
		updateCustomWidthSlider();
		createColorSelector();

		RecyclerView groupRecyclerView = view.findViewById(R.id.recycler_view);
		groupRecyclerView.setAdapter(new GpxWidthAdapter(Arrays.asList(GradientScaleType.values())));
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
	}

	private void createColorSelector() {
		FlowLayout selectColor = view.findViewById(R.id.select_color);
		List<Integer> colors = new ArrayList<>();
		for (AppearanceListItem appearanceListItem : getAppearanceItems(app, GpxAppearanceAdapterType.TRACK_COLOR)) {
			if (!colors.contains(appearanceListItem.getColor())) {
				colors.add(appearanceListItem.getColor());
			}
		}
		for (int color : colors) {
			selectColor.addView(createColorItemView(color, selectColor), new FlowLayout.LayoutParams(0, 0));
		}

		if (selectedGpxFile.isShowCurrentTrack()) {
			selectedColor = app.getSettings().CURRENT_TRACK_COLOR.get();
		} else {
			selectedColor = selectedGpxFile.getGpxFile().getColor(0);
		}
		updateColorSelector(selectedColor, selectColor);
	}

	private View createColorItemView(@ColorInt final int color, final FlowLayout rootView) {
		FrameLayout colorItemView = (FrameLayout) UiUtilities.getInflater(rootView.getContext(), nightMode)
				.inflate(R.layout.point_editor_button, rootView, false);
		ImageView outline = colorItemView.findViewById(R.id.outline);
		outline.setImageDrawable(
				UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, R.drawable.bg_point_circle_contour),
						ContextCompat.getColor(app,
								nightMode ? R.color.stroked_buttons_and_links_outline_dark
										: R.color.stroked_buttons_and_links_outline_light)));
		ImageView backgroundCircle = colorItemView.findViewById(R.id.background);
		backgroundCircle.setImageDrawable(UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, R.drawable.bg_point_circle), color));
		backgroundCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateColorSelector(color, rootView);
			}
		});
		colorItemView.setTag(color);
		return colorItemView;
	}

	private void updateColorSelector(int color, View rootView) {
		View oldColor = rootView.findViewWithTag(selectedColor);
		if (oldColor != null) {
			oldColor.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView icon = oldColor.findViewById(R.id.icon);
			icon.setImageDrawable(UiUtilities.tintDrawable(icon.getDrawable(), R.color.icon_color_default_light));
		}
		View newColor = rootView.findViewWithTag(color);
		if (newColor != null) {
			newColor.findViewById(R.id.outline).setVisibility(View.VISIBLE);
		}
		selectedColor = color;
		setGpxColor(color);
	}

	private void setGpxColor(int color) {
		GPXFile gpxFile = selectedGpxFile.getGpxFile();
		if (gpxFile != null) {
			if (color != 0) {
				selectedGpxFile.getGpxFile().setColor(color);
				GpxDataItem gpxDataItem = app.getGpxDbHelper().getItem(new File(gpxFile.path));
				if (gpxDataItem != null) {
					app.getGpxDbHelper().updateColor(gpxDataItem, color);
				}
			}
			if (gpxFile.showCurrentTrack) {
				app.getSettings().CURRENT_TRACK_COLOR.set(color);
			}
			mapActivity.refreshMap();
		}
	}

	private GradientScaleType getSelectedScaleType() {
		if (selectedScaleType == null) {
			String gradientScaleType = selectedGpxFile.getGpxFile().getGradientScaleType();
			for (GradientScaleType item : GradientScaleType.values()) {
				if (item.name().equalsIgnoreCase(gradientScaleType)) {
					selectedScaleType = item;
					break;
				}
			}
			if (selectedScaleType == null) {
				selectedScaleType = GradientScaleType.SOLID;
			}
		}
		return selectedScaleType;
	}

	private void updateHeader() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(R.string.select_color);

		TextView descriptionView = view.findViewById(R.id.description);
		descriptionView.setText(getSelectedScaleType().getHumanString(view.getContext()));
	}

	private void updateCustomWidthSlider() {
		boolean visible = GradientScaleType.SOLID == getSelectedScaleType();
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.select_color), visible);
	}

	private void setGradientScaleType(GradientScaleType gradientScaleType) {
		if (selectedGpxFile.getGpxFile() != null) {
			GPXFile gpxFile = selectedGpxFile.getGpxFile();
			gpxFile.setGradientScaleType(gradientScaleType.getTypeName());
			GpxDataItem gpxDataItem = app.getGpxDbHelper().getItem(new File(gpxFile.path));
			if (gpxDataItem != null) {
				app.getGpxDbHelper().updateGradientScaleType(gpxDataItem, gradientScaleType);
			}
			mapActivity.refreshMap();
		}
	}

	private class GpxWidthAdapter extends RecyclerView.Adapter<GpxWidthViewHolder> {

		private List<GradientScaleType> items;

		private GpxWidthAdapter(List<GradientScaleType> items) {
			this.items = items;
		}

		@NonNull
		@Override
		public GpxWidthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater themedInflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			View view = themedInflater.inflate(R.layout.point_editor_group_select_item, parent, false);
			view.getLayoutParams().width = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_width);
			view.getLayoutParams().height = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_height);

			GpxWidthViewHolder holder = new GpxWidthViewHolder(view);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, holder.widthButton, nightMode, R.drawable.ripple_solid_light_6dp,
						R.drawable.ripple_solid_dark_6dp);
			}
			return holder;
		}

		@Override
		public void onBindViewHolder(@NonNull final GpxWidthViewHolder holder, int position) {
			GradientScaleType item = items.get(position);
			holder.widthAttrName.setText(item.getHumanString(holder.itemView.getContext()));

			updateButtonBg(holder, item);

			int colorId;
			if (item == GradientScaleType.SOLID) {
				if (selectedGpxFile.isShowCurrentTrack()) {
					colorId = app.getSettings().CURRENT_TRACK_COLOR.get();
				} else {
					colorId = selectedGpxFile.getGpxFile().getColor(0);
				}
			} else if (item.equals(getSelectedScaleType())) {
				colorId = ContextCompat.getColor(app, nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light);
			} else {
				colorId = AndroidUtils.getColorFromAttr(holder.itemView.getContext(), R.attr.default_icon_color);
			}

			holder.widthIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(item.getIconId(), colorId));

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int prevSelectedPosition = getItemPosition(getSelectedScaleType());
					selectedScaleType = items.get(holder.getAdapterPosition());
					notifyItemChanged(holder.getAdapterPosition());
					notifyItemChanged(prevSelectedPosition);

					setGradientScaleType(selectedScaleType);

					updateHeader();
					updateCustomWidthSlider();
				}
			});
		}

		private void updateButtonBg(GpxWidthViewHolder holder, GradientScaleType item) {
			GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_group_button_outline);
			if (rectContourDrawable != null) {
				if (getSelectedScaleType() != null && getSelectedScaleType().equals(item)) {
					int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), strokeColor);
				} else {
					int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 1), strokeColor);
				}
				holder.widthButton.setImageDrawable(rectContourDrawable);
			}
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		int getItemPosition(GradientScaleType name) {
			return items.indexOf(name);
		}
	}

	private static class GpxWidthViewHolder extends RecyclerView.ViewHolder {

		final TextView widthAttrName;
		final ImageView widthIcon;
		final ImageView widthButton;

		GpxWidthViewHolder(View itemView) {
			super(itemView);
			widthAttrName = itemView.findViewById(R.id.groupName);
			widthIcon = itemView.findViewById(R.id.groupIcon);
			widthButton = itemView.findViewById(R.id.outlineRect);
		}
	}
}