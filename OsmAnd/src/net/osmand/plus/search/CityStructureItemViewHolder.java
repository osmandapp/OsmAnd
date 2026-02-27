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
import net.osmand.plus.helpers.FileNameTranslationHelper;
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
	public final View addressDotDivider;
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
		addressDotDivider = view.findViewById(R.id.address_dot_divider);
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
		String addressText = item.getAddress();
		String typeName = item.getTypeName();

		if (mapObject instanceof City city) {
			BinaryMapIndexReader mapReaderResource = null;
			if (mapObject.getReferenceFile() instanceof BinaryMapIndexReader) {
				mapReaderResource = (BinaryMapIndexReader) mapObject.getReferenceFile();
			} else if(city.getType() == City.CityType.POSTCODE) {
				mapReaderResource = item.getSearchResult().file;
			}
			if (mapReaderResource != null) {
				addressText = FileNameTranslationHelper.getFileNameWithRegion(app, mapReaderResource.getFile().getName());
			} else if (item.getSearchResult().relatedObject instanceof City relatedCity &&
					relatedCity.getReferenceFile() instanceof BinaryMapIndexReader relatedMapReaderResource) {
				addressText = String.format("%s, %s", relatedCity, FileNameTranslationHelper.getFileNameWithRegion(app, relatedMapReaderResource.getFile().getName()));
			}
			typeName = switch (city.getType()) {
				case VILLAGE -> app.getString(R.string.city_type_village);
				case SUBURB -> app.getString(R.string.city_type_suburb);
				case TOWN -> app.getString(R.string.city_type_town);
				case BOUNDARY -> app.getString(R.string.poi_boundary_stone);
				case POSTCODE -> app.getString(R.string.postcode);
				default -> app.getString(R.string.city_type_city);
			};
		} else if (mapObject instanceof Street street) {
			if (street.getNamesMap(false).containsKey(OLD_NAME_TAG)) {
				title = String.format("%s (%s)", title, street.getName(OLD_NAME_TAG));
			}
			addressText = street.getCity().getName();
			if (item.getSearchResult().objectType == ObjectType.STREET) {
				typeName = app.getString(R.string.search_address_street);
			} else if (item.getSearchResult().objectType == ObjectType.STREET_INTERSECTION) {
				typeName = app.getString(R.string.intersection);
			}
		} else if (mapObject instanceof Building) {
			StringBuilder address = new StringBuilder(item.getSearchResult().localeRelatedObjectName);
			if (item.getSearchResult().relatedObject instanceof Street street) {
				address.append(", ").append(street.getCity().getName());
			}
			addressText = address.toString();
			typeName = app.getString(R.string.search_address_building);
		}
		addressTv.setText(addressText);
		AndroidUiHelper.updateVisibility(addressTv, !Algorithms.isEmpty(addressText));
		AndroidUiHelper.updateVisibility(addressDotDivider, !Algorithms.isEmpty(addressText));
		titleTv.setText(title);
		type.setText(typeName);
		bindImage(item, mapObject);
		QuickSearchListAdapter.updateCompass(itemView, item, locationViewCache, useMapCenter);
	}

	private void bindImage(@NonNull QuickSearchListItem item, MapObject mapObject) {
		Drawable drawable = item.getIcon();
		icon.setImageDrawable(drawable);
		if (mapObject instanceof Amenity amenity) {
			if (Algorithms.stringsEqual(amenity.getSubType(), "city")) {
				addressTv.setText(amenity.getRegionName());
			}
			String wikiImageUrl = amenity.getWikiImageStubUrl();
			boolean shouldLayoutWithImages = !Algorithms.isEmpty(wikiImageUrl) && image != null;
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
	}
}
