package net.osmand.plus.search;

import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.dialogs.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchWikiItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.Calendar;

public class WikiItemViewHolder extends RecyclerView.ViewHolder {

	public final OsmandApplication app;
	public final UpdateLocationViewCache locationViewCache;

	public final TextView title;
	public final TextView addressTv;
	public final TextView description;
	public final TextView type;
	public final ImageView icon;
	public final ImageView image;
	public final ViewGroup imageViewContainer;
	public final ImageView errorImageView;
	public final LinearLayout timeLayout;

	public final boolean nightMode;

	public WikiItemViewHolder(@NonNull View view,
			@NonNull UpdateLocationViewCache locationViewCache, boolean nightMode) {
		super(view);

		this.app = AndroidUtils.getApp(view.getContext());
		this.locationViewCache = locationViewCache;
		this.nightMode = nightMode;

		title = view.findViewById(R.id.item_title);
		addressTv = view.findViewById(R.id.address);
		description = view.findViewById(R.id.item_description);
		type = view.findViewById(R.id.item_type);
		icon = view.findViewById(R.id.item_icon);
		image = view.findViewById(R.id.item_image);
		imageViewContainer = view.findViewById(R.id.item_image_container);
		errorImageView = itemView.findViewById(R.id.item_image_error);
		timeLayout = view.findViewById(R.id.time_layout);
	}

	public void bindItem(@NonNull QuickSearchWikiItem item, @Nullable PoiUIFilter poiUIFilter,
			boolean useMapCenter) {
		String address = item.getAddress();
		String descr = item.getDescription();
		if (description != null) {
			description.setText(descr);
			if (!Algorithms.isEmpty(descr)) {
				description.setVisibility(View.VISIBLE);
			} else {
				description.setVisibility(View.GONE);
			}
		}
		title.setText(item.getName());
		type.setText(item.getTypeName());

		Drawable drawable = item.getIcon();
		icon.setImageDrawable(drawable);
		errorImageView.setImageDrawable(drawable);

		AndroidUiHelper.setTextAndChangeVisibility(addressTv, address);

		if (timeLayout != null) {
			if (item.getSearchResult().object instanceof Amenity
					&& ((Amenity) item.getSearchResult().object).getOpeningHours() != null) {
				Amenity amenity = (Amenity) item.getSearchResult().object;
				OpeningHoursParser.OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				if (rs != null && rs.getInfo() != null) {
					int colorOpen = R.color.text_color_positive;
					int colorClosed = R.color.text_color_negative;
					SpannableString openHours = MenuController.getSpannableOpeningHours(
							rs.getInfo(),
							ContextCompat.getColor(app, colorOpen),
							ContextCompat.getColor(app, colorClosed));
					int colorId = rs.isOpenedForTime(Calendar.getInstance()) ? colorOpen : colorClosed;
					timeLayout.setVisibility(View.VISIBLE);

					TextView timeText = timeLayout.findViewById(R.id.time);
					ImageView timeIcon = timeLayout.findViewById(R.id.time_icon);
					timeText.setText(openHours);
					timeIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_opening_hour_16, colorId));
				} else {
					timeLayout.setVisibility(View.GONE);
				}
			} else {
				timeLayout.setVisibility(View.GONE);
			}
		}

		boolean shouldLayoutWithImages = poiUIFilter != null && poiUIFilter.showLayoutWithImages();
		AndroidUiHelper.updateVisibility(imageViewContainer, shouldLayoutWithImages);
		if (shouldLayoutWithImages) {
			String wikiImageUrl = item.getImage();
			if (image.getTag() != wikiImageUrl) {
				image.setTag(wikiImageUrl);
				if (wikiImageUrl != null) {
					RequestCreator creator = Picasso.get().load(wikiImageUrl);
					creator.into(image, new Callback() {
						@Override
						public void onSuccess() {
							AndroidUiHelper.updateVisibility(image, true);
							AndroidUiHelper.updateVisibility(errorImageView, false);
							PicassoUtils.getPicasso(app).setResultLoaded(wikiImageUrl, true);
						}

						@Override
						public void onError(Exception e) {
							AndroidUiHelper.updateVisibility(image, false);
							AndroidUiHelper.updateVisibility(errorImageView, true);
							PicassoUtils.getPicasso(app).setResultLoaded(wikiImageUrl, false);
						}
					});
				}
			}
		}
		QuickSearchListAdapter.updateCompass(itemView, item, locationViewCache, useMapCenter);
	}
}
