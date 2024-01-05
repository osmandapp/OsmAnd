package net.osmand.plus.track.cards;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnSliderTouchListener;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.track.AppearanceViewHolder;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.AppearanceListItem;
import net.osmand.plus.track.GpxAppearanceAdapter.GpxAppearanceAdapterType;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.List;

public class TrackWidthCard extends BaseCard {

	private static final String CUSTOM_WIDTH = "custom_width";
	private static final int CUSTOM_WIDTH_MIN = 1;
	private static final int CUSTOM_WIDTH_MAX = 24;

	private final TrackDrawInfo trackDrawInfo;
	private final OnNeedScrollListener onNeedScrollListener;

	private List<AppearanceListItem> appearanceItems;

	private GpxWidthAdapter widthAdapter;
	private View sliderContainer;
	private RecyclerView groupRecyclerView;

	public TrackWidthCard(@NonNull FragmentActivity activity, @NonNull TrackDrawInfo trackDrawInfo,
	                      @NonNull OnNeedScrollListener onNeedScrollListener) {
		super(activity);
		this.trackDrawInfo = trackDrawInfo;
		this.onNeedScrollListener = onNeedScrollListener;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_width_card;
	}

	@Override
	protected void updateContent() {
		appearanceItems = getWidthAppearanceItems();
		updateHeader();
		updateCustomWidthSlider();

		widthAdapter = new GpxWidthAdapter(appearanceItems);
		groupRecyclerView = view.findViewById(R.id.recycler_view);
		groupRecyclerView.setAdapter(widthAdapter);
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		scrollMenuToSelectedItem();
	}

	public void updateItems() {
		if (widthAdapter != null) {
			widthAdapter.notifyDataSetChanged();
		}
	}

	@Nullable
	private AppearanceListItem getSelectedItem() {
		String selectedWidth = trackDrawInfo.getWidth();
		for (AppearanceListItem item : appearanceItems) {
			if (selectedWidth != null && (Algorithms.objectEquals(item.getValue(), selectedWidth)
					|| Algorithms.isEmpty(selectedWidth) && Algorithms.isEmpty(item.getValue())
					|| Algorithms.isInt(selectedWidth) && CUSTOM_WIDTH.equals(item.getAttrName()))) {
				return item;
			}
		}
		return null;
	}

	private List<AppearanceListItem> getWidthAppearanceItems() {
		List<AppearanceListItem> items = GpxAppearanceAdapter.getAppearanceItems(app, GpxAppearanceAdapterType.TRACK_WIDTH);

		String selectedWidth = trackDrawInfo.getWidth();
		String customWidth = !Algorithms.isEmpty(selectedWidth) && Algorithms.isInt(selectedWidth) ? selectedWidth : String.valueOf(CUSTOM_WIDTH_MIN);

		items.add(new AppearanceListItem(CUSTOM_WIDTH, customWidth, app.getString(R.string.shared_string_custom)));
		return items;
	}

	private void updateHeader() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);

		View headerView = view.findViewById(R.id.header_view);
		headerView.setBackground(null);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(R.string.select_track_width);

		TextView descriptionView = view.findViewById(R.id.description);
		AppearanceListItem item = getSelectedItem();
		if (item != null) {
			descriptionView.setText(item.getLocalizedValue());
		}
	}

	private void updateCustomWidthSlider() {
		sliderContainer = view.findViewById(R.id.slider_container);
		AppearanceListItem item = getSelectedItem();
		if (item != null && CUSTOM_WIDTH.equals(item.getAttrName())) {
			Slider widthSlider = view.findViewById(R.id.width_slider);

			widthSlider.setValueTo(CUSTOM_WIDTH_MAX);
			widthSlider.setValueFrom(CUSTOM_WIDTH_MIN);

			((TextView) view.findViewById(R.id.width_value_min)).setText(String.valueOf(CUSTOM_WIDTH_MIN));
			((TextView) view.findViewById(R.id.width_value_max)).setText(String.valueOf(CUSTOM_WIDTH_MAX));

			String width = getSelectedItem().getValue();
			if (!Algorithms.isEmpty(width) && Algorithms.isInt(width)) {
				try {
					widthSlider.setValue(Integer.parseInt(width));
				} catch (NumberFormatException e) {
					widthSlider.setValue(1);
				}
			} else {
				widthSlider.setValue(1);
			}

			TextView selectedCustomWidth = view.findViewById(R.id.width_value_tv);
			widthSlider.addOnChangeListener(new Slider.OnChangeListener() {
				@Override
				public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
					if (fromUser) {
						String valueStr = String.valueOf((int) value);
						selectedCustomWidth.setText(valueStr);
						getSelectedItem().setValue(valueStr);
						setGpxWidth(valueStr);
					}
				}
			});

			// Disable arrows in OpenGL while touching slider to prevent arrows blinking
			widthSlider.addOnSliderTouchListener(new OnSliderTouchListener() {

				boolean prevShowArrows;

				@Override
				public void onStartTrackingTouch(@NonNull Slider slider) {
					if (hasMapRenderer()) {
						if (trackDrawInfo.isShowArrows()) {
							prevShowArrows = true;
							setShowArrows(false);
						}
					}
				}

				@Override
				public void onStopTrackingTouch(@NonNull Slider slider) {
					if (hasMapRenderer()) {
						if (prevShowArrows) {
							setShowArrows(true);
						}
					}
				}

				private boolean hasMapRenderer() {
					return app.getOsmandMap().getMapView().getMapRenderer() != null;
				}
			});
			UiUtilities.setupSlider(widthSlider, nightMode, null, true);
			ScrollUtils.addOnGlobalLayoutListener(sliderContainer, () -> {
				if (sliderContainer.getVisibility() == View.VISIBLE) {
					onNeedScrollListener.onVerticalScrollNeeded(sliderContainer.getBottom());
				}
			});
			AndroidUiHelper.updateVisibility(sliderContainer, true);
		} else {
			AndroidUiHelper.updateVisibility(sliderContainer, false);
		}
	}

	private void setGpxWidth(String width) {
		trackDrawInfo.setWidth(width);
		app.getOsmandMap().getMapView().refreshMap();
	}

	private void setShowArrows(boolean showArrows) {
		trackDrawInfo.setShowArrows(showArrows);
		app.getOsmandMap().getMapView().refreshMap();
	}

	private void scrollMenuToSelectedItem() {
		AppearanceListItem selectedItem = getSelectedItem();
		int position = widthAdapter.getItemPosition(selectedItem);
		if (position != -1) {
			groupRecyclerView.scrollToPosition(position);
		}
	}

	private class GpxWidthAdapter extends RecyclerView.Adapter<AppearanceViewHolder> {

		private final List<AppearanceListItem> items;

		private GpxWidthAdapter(List<AppearanceListItem> items) {
			this.items = items;
		}

		@NonNull
		@Override
		public AppearanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = themedInflater.inflate(R.layout.point_editor_group_select_item, parent, false);
			view.getLayoutParams().width = getDimen(R.dimen.gpx_group_button_width);
			view.getLayoutParams().height = getDimen(R.dimen.gpx_group_button_height);

			AppearanceViewHolder holder = new AppearanceViewHolder(view);
			AndroidUtils.setBackground(app, holder.button, nightMode, R.drawable.ripple_solid_light_6dp,
					R.drawable.ripple_solid_dark_6dp);
			return holder;
		}

		@Override
		public void onBindViewHolder(@NonNull AppearanceViewHolder holder, int position) {
			AppearanceListItem item = items.get(position);
			holder.title.setText(item.getLocalizedValue());

			updateButtonBg(holder, item);
			updateWidthIcon(holder, item);

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int prevSelectedPosition = getItemPosition(getSelectedItem());
					AppearanceListItem selectedItem = items.get(holder.getAdapterPosition());
					notifyItemChanged(holder.getAdapterPosition());
					notifyItemChanged(prevSelectedPosition);

					setGpxWidth(selectedItem.getValue());

					updateHeader();
					updateCustomWidthSlider();
					scrollMenuToSelectedItem();

					notifyCardPressed();
				}
			});
		}

		private void updateWidthIcon(AppearanceViewHolder holder, AppearanceListItem item) {
			int color = trackDrawInfo.getColor();

			int iconId;
			if (CUSTOM_WIDTH.equals(item.getAttrName())) {
				iconId = R.drawable.ic_action_settings;
				color = AndroidUtils.getColorFromAttr(holder.itemView.getContext(), R.attr.active_color_basic);
			} else {
				iconId = TrackAppearanceFragment.getWidthIconId(item.getValue());
			}
			if (color == 0) {
				color = GpxAppearanceAdapter.getTrackColor(app);
			}
			holder.icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconId, color));
		}

		private void updateButtonBg(AppearanceViewHolder holder, AppearanceListItem item) {
			GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_group_button_outline);
			if (rectContourDrawable != null) {
				if (getSelectedItem() != null && getSelectedItem().equals(item)) {
					int strokeColor = ContextCompat.getColor(app, ColorUtilities.getActiveColorId(nightMode));
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), strokeColor);
				} else {
					int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 1), strokeColor);
				}
				holder.button.setImageDrawable(rectContourDrawable);
			}
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		int getItemPosition(AppearanceListItem name) {
			return items.indexOf(name);
		}
	}
}