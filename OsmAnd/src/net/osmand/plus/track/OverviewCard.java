package net.osmand.plus.track;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.myplaces.SegmentGPXAdapter;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.TextViewEx;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.myplaces.TrackActivityFragmentAdapter.isGpxFileSelected;
import static net.osmand.plus.track.OptionsCard.APPEARANCE_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.DIRECTIONS_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.EDIT_BUTTON_INDEX;
import static net.osmand.plus.track.OptionsCard.SHOW_ON_MAP_BUTTON_INDEX;

public class OverviewCard extends BaseCard {

	private RecyclerView rvOverview;
	private ImageView direction;
	private TextView distanceText;
	private MapContextMenu menu;
	private View showButton;
	private View appearanceButton;
	private View editButton;
	private View directionsButton;

	private TrackDisplayHelper displayHelper;
	private GPXFile gpxFile;
	private GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT};
	private SegmentGPXAdapter adapter;
	private SegmentActionsListener listener;

	public OverviewCard(@NonNull MapActivity mapActivity, @NonNull TrackDisplayHelper displayHelper,
						@NonNull SegmentActionsListener listener) {
		super(mapActivity);
		this.displayHelper = displayHelper;
		this.gpxFile = displayHelper.getGpx();
		this.listener = listener;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_overview_fragment;
	}

	@Override
	protected void updateContent() {
//		ViewGroup container = (ViewGroup) view;
//		container.removeAllViews();

		int iconColorDef = R.color.icon_color_active_light;
		int iconColorPres = R.color.active_buttons_and_links_text_dark;

		showButton = view.findViewById(R.id.show_button);
		appearanceButton = view.findViewById(R.id.appearance_button);
		editButton = view.findViewById(R.id.edit_button);
		directionsButton = view.findViewById(R.id.directions_button);

		boolean fileAvailable = gpxFile.path != null && !gpxFile.showCurrentTrack;

		initShowButton(iconColorDef, iconColorPres);
		initAppearanceButton(iconColorDef, iconColorPres);
		if (fileAvailable) {
			initEditButton(iconColorDef, iconColorPres);
			initDirectionsButton(iconColorDef, iconColorPres);
		}

		menu = mapActivity.getContextMenu();
		distanceText = (TextView) view.findViewById(R.id.distance);
		direction = (ImageView) view.findViewById(R.id.direction);
		UpdateLocationViewCache updateLocationViewCache = app.getUIUtilities().getUpdateLocationViewCache();
		app.getUIUtilities().updateLocationView(updateLocationViewCache, direction, distanceText, menu.getLatLon());

		SegmentItem item1 = new SegmentItem("Distance", "700 km", R.drawable.ic_action_track_16);
		SegmentItem item2 = new SegmentItem("Ascent", "156 km", R.drawable.ic_action_arrow_up_16);
		SegmentItem item3 = new SegmentItem("Descent", "338 km", R.drawable.ic_action_arrow_down_16);
		SegmentItem item4 = new SegmentItem("Average speed", "9.9 km/h", R.drawable.ic_action_speed_16);
		SegmentItem item5 = new SegmentItem("Max. speed", "12.7 km/h", R.drawable.ic_action_max_speed_16);
		SegmentItem item6 = new SegmentItem("Time span", "4:00:18", R.drawable.ic_action_time_span_16);
		List<SegmentItem> items = new ArrayList<>();
		items.add(item1);
		items.add(item2);
		items.add(item3);
		items.add(item4);
		items.add(item5);
		items.add(item6);

		rvOverview = view.findViewById(R.id.recycler_overview);
		LinearLayoutManager llManager = new LinearLayoutManager(app);
		llManager.setOrientation(LinearLayoutManager.HORIZONTAL);
		rvOverview.setLayoutManager(llManager);
		rvOverview.setItemAnimator(new DefaultItemAnimator());
		final SegmentItemAdapter oiAdapter = new SegmentItemAdapter(items);
		rvOverview.setAdapter(oiAdapter);
		rvOverview.addItemDecoration(new HorizontalDividerDecoration(app));

	}

	private void initShowButton(final int iconColorDef, final int iconColorPres) {
		final AppCompatImageView image = showButton.findViewById(R.id.image);
		final AppCompatImageView filled = showButton.findViewById(R.id.filled);
		final int iconShowResId = R.drawable.ic_action_view;
		final int iconHideResId = R.drawable.ic_action_hide;
		final boolean[] gpxFileSelected = {isGpxFileSelected(app, gpxFile)};
		filled.setImageResource(R.drawable.bg_topbar_shield_exit_ref);
		filled.setAlpha(gpxFileSelected[0] ? 1f : 0.1f);
		setImageDrawable(image, gpxFileSelected[0] ? iconShowResId : iconHideResId,
				gpxFileSelected[0] ? iconColorPres : iconColorDef);
		showButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gpxFileSelected[0] = !gpxFileSelected[0];

				setImageDrawable(image, gpxFileSelected[0] ? iconShowResId : iconHideResId,
						gpxFileSelected[0] ? iconColorPres : iconColorDef);

				filled.setAlpha(gpxFileSelected[0] ? 1f : 0.1f);

				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(OverviewCard.this, SHOW_ON_MAP_BUTTON_INDEX);
				}
			}
		});
	}

	private void initAppearanceButton(int iconColorDef, int iconColorPres) {
		initButton(appearanceButton, APPEARANCE_BUTTON_INDEX, R.drawable.ic_action_appearance, iconColorDef, iconColorPres);
	}

	private void initEditButton(int iconColorDef, int iconColorPres) {
		initButton(editButton, EDIT_BUTTON_INDEX, R.drawable.ic_action_edit_dark, iconColorDef, iconColorPres);
	}

	private void initDirectionsButton(int iconColorDef, int iconColorPres) {
		initButton(directionsButton, DIRECTIONS_BUTTON_INDEX, R.drawable.ic_action_gdirections_dark, iconColorDef, iconColorPres);
	}

	private void initButton(View item, final int buttonIndex, int iconResId, int iconColorDef, int iconColorPres) {
		final AppCompatImageView image = item.findViewById(R.id.image);
		final AppCompatImageView filled = item.findViewById(R.id.filled);
		filled.setImageResource(R.drawable.bg_topbar_shield_exit_ref);
		filled.setAlpha(0.1f);
		setImageDrawable(image, iconResId, iconColorDef);
		setOnTouchItem(item, image, filled, iconResId, iconColorDef, iconColorPres);
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(OverviewCard.this, buttonIndex);
				}
			}
		});
	}

	private void setImageDrawable(ImageView iv, @DrawableRes int resId, @ColorRes int color) {
		Drawable icon = app.getUIUtilities().getIcon(resId, color);
		iv.setImageDrawable(icon);
	}

	private void setOnTouchItem(View item, final ImageView image, final ImageView filled, @DrawableRes final int resId, @ColorRes final int colorDef, @ColorRes final int colorPres) {
		item.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN: {
						filled.setAlpha(1f);
						setImageDrawable(image, resId, colorPres);
						break;
					}
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL: {
						filled.setAlpha(0.1f);
						setImageDrawable(image, resId, colorDef);
						break;
					}
				}
				return false;
			}
		});
	}

	private class SegmentItemAdapter extends RecyclerView.Adapter<SegmentItemAdapter.SegmentItemViewHolder> {
		private final List<SegmentItem> segmentItems;

		public SegmentItemAdapter(List<SegmentItem> segmentItems) {
			this.segmentItems = segmentItems;
		}

		@Override
		public int getItemCount() {
			return segmentItems.size();
		}

		@NonNull
		@Override
		public SegmentItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View itemView = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_gpx_action_segment, parent, false);
			return new SegmentItemViewHolder(itemView);
		}

		@Override
		public void onBindViewHolder(SegmentItemViewHolder holder, int position) {
			SegmentItem item = segmentItems.get(position);
			holder.bind(item);
		}


		class SegmentItemViewHolder extends RecyclerView.ViewHolder {
			private final TextViewEx valueText;
			private final TextView titleText;
			private final AppCompatImageView imageView;

			SegmentItemViewHolder(View view) {
				super(view);
				valueText = view.findViewById(R.id.value);
				titleText = view.findViewById(R.id.title);
				imageView = view.findViewById(R.id.image);
			}

			public void bind(SegmentItem overviewItem) {
				valueText.setText(overviewItem.value);
				titleText.setText(overviewItem.title);
				setImageDrawable(imageView, overviewItem.imageResId, R.color.text_color_primary_light); //todo change color
			}
		}
	}

	private class HorizontalDividerDecoration extends RecyclerView.ItemDecoration {
		private final Drawable divider;

		public HorizontalDividerDecoration(Context context) {
			int[] ATTRS = new int[]{android.R.attr.listDivider};
			final TypedArray a = context.obtainStyledAttributes(ATTRS);
			divider = a.getDrawable(0);
			a.recycle();
//			mDivider = getMyApplication().getUIUtilities().getIcon(R.drawable.divider_solid, R.color.divider_color_light); //todo change drawable
		}

		@Override
		public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
			drawHorizontal(c, parent);
		}

		public void drawHorizontal(Canvas c, RecyclerView parent) {
			final int marginV = parent.getContext().getResources().getDimensionPixelSize(R.dimen.map_small_button_margin);
			final int marginH = parent.getContext().getResources().getDimensionPixelSize(R.dimen.content_padding);
			final int childCount = parent.getChildCount();
			for (int i = 0; i < childCount; i++) {
				final View child = parent.getChildAt(i);
				final int left = child.getRight() - divider.getIntrinsicWidth() + marginH;
				final int right = left + divider.getIntrinsicHeight();
				final int top = child.getTop() + marginV;
				final int bottom = child.getBottom() - marginV;
				divider.setBounds(left, top, right, bottom);
				divider.draw(c);
			}
		}

		@Override
		public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
			int marginV = parent.getContext().getResources().getDimensionPixelSize(R.dimen.map_small_button_margin);
			int marginH = parent.getContext().getResources().getDimensionPixelSize(R.dimen.content_padding);
			outRect.set(marginH - divider.getIntrinsicWidth(), marginV, marginH + divider.getIntrinsicWidth(), marginV);
		}
	}

	private class SegmentItem {
		private String title;
		private String value;
		private int imageResId;

		public SegmentItem(String title, String value, int imageResId) {
			this.title = title;
			this.value = value;
			this.imageResId = imageResId;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public int getImageResId() {
			return imageResId;
		}

		public void setImageResId(int imageResId) {
			this.imageResId = imageResId;
		}
	}
}