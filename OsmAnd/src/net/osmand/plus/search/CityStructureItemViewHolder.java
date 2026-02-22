package net.osmand.plus.search;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.search.dialogs.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.search.core.ObjectType;
import net.osmand.util.Algorithms;

public class CityStructureItemViewHolder extends RecyclerView.ViewHolder {

	public static final String OLD_NAME_TAG = "old_name";
	public final OsmandApplication app;
	public final UpdateLocationViewCache locationViewCache;

	public final TextView titleTv;
	public final TextView addressTv;
	public final TextView type;
	public final ImageView icon;
	public final ImageView image;
	public final ViewGroup imageViewContainer;

	public boolean nightMode;

	public CityStructureItemViewHolder(@NonNull View view,
	                                   @NonNull UpdateLocationViewCache locationViewCache) {
		super(view);

		this.app = AndroidUtils.getApp(view.getContext());
		this.locationViewCache = locationViewCache;

		titleTv = view.findViewById(R.id.item_title);
		addressTv = view.findViewById(R.id.address);
		type = view.findViewById(R.id.item_type);
		icon = view.findViewById(R.id.item_icon);
		image = view.findViewById(R.id.item_image);
		imageViewContainer = view.findViewById(R.id.item_image_container);
	}

	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
	}

	public void bindItem(@NonNull QuickSearchListItem item, boolean useMapCenter) {
		CharSequence title = item.getSpannableName();
		MapObject mapObject = (MapObject) item.getSearchResult().object;
		BinaryMapIndexReader mapReaderResource = null;
		if (mapObject.getReferenceFile() instanceof BinaryMapIndexReader) {
			mapReaderResource = (BinaryMapIndexReader) mapObject.getReferenceFile();
		}

		if (mapObject instanceof City city) {
			if (mapReaderResource != null) {
				addressTv.setText(mapReaderResource.getRegionName());
			} else if (item.getSearchResult().relatedObject instanceof City relatedCity) {
				if (relatedCity.getReferenceFile() instanceof BinaryMapIndexReader relatedMapReaderResource) {
					addressTv.setText(String.format("%s, %s", relatedCity, relatedMapReaderResource.getRegionName()));
				}
			}
		} else if (mapObject instanceof Street street) {
			if (!Algorithms.isEmpty(street.getOtherNames()) && street.getOtherNames().contains(OLD_NAME_TAG)) {
				String oldName = street.getName(OLD_NAME_TAG);
				title = String.format("%s (%s)", title, oldName);
			}
			addressTv.setText(street.getCity().getName());
		} else if (mapObject instanceof Building) {
			StringBuilder address = new StringBuilder(item.getSearchResult().localeRelatedObjectName);
			if (item.getSearchResult().relatedObject instanceof Street street) {
				address.append(", ");
				address.append(street.getCity().getName());
			}
			addressTv.setText(address.toString());
		} else {
			addressTv.setText(item.getAddress());
		}
		if (mapObject instanceof City city) {
			City.CityType cityType = city.getType();
			String cityTypeString;
			switch (cityType) {
				case VILLAGE -> cityTypeString = app.getString(R.string.city_type_village);
				case SUBURB -> cityTypeString = app.getString(R.string.city_type_suburb);
				case TOWN -> cityTypeString = app.getString(R.string.city_type_town);
				default -> cityTypeString = app.getString(R.string.city_type_city);
			}
			type.setText(cityTypeString);
		} else if (mapObject instanceof Street) {
			if (item.getSearchResult().objectType == ObjectType.STREET) {
				type.setText(R.string.search_address_street);
			} else if (item.getSearchResult().objectType == ObjectType.STREET_INTERSECTION) {
				type.setText(R.string.intersection);
			}
		} else if (mapObject instanceof Building) {
			type.setText(R.string.search_address_building);
		} else {
			type.setText(item.getTypeName());
		}
		titleTv.setText(title);

		Drawable drawable = item.getIcon();
		icon.setImageDrawable(drawable);
		if (mapObject instanceof Amenity amenity) {
			String wikiImageUrl = amenity.getWikiImageStubUrl();
			boolean shouldLayoutWithImages = !Algorithms.isEmpty(wikiImageUrl);
			AndroidUiHelper.updateVisibility(imageViewContainer, shouldLayoutWithImages);
			if (shouldLayoutWithImages) {
				if (image.getTag() != wikiImageUrl) {
					image.setTag(wikiImageUrl);
					RequestCreator creator = Picasso.get().load(wikiImageUrl);
					creator.error(drawable);
					creator.into(image, new Callback() {
						@Override
						public void onSuccess() {
							PicassoUtils.getPicasso(app).setResultLoaded(wikiImageUrl, true);
						}

						@Override
						public void onError(Exception e) {
							PicassoUtils.getPicasso(app).setResultLoaded(wikiImageUrl, false);
						}
					});
				}
			}
		}

		QuickSearchListAdapter.updateCompass(itemView, item, locationViewCache, useMapCenter);
	}
}
