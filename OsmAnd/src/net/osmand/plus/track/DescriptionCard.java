package net.osmand.plus.track;

import android.text.TextUtils;
import android.view.View;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.PicassoUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.wikipedia.WikiArticleHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class DescriptionCard extends BaseCard {

    private final GPXFile gpxFile;

    public DescriptionCard(MapActivity mapActivity, GPXFile gpxFile) {
        super(mapActivity);
        this.gpxFile = gpxFile;
    }

    @Override
    public int getCardLayoutId() {
        return R.layout.gpx_description_preview_card;
    }

    @Override
    protected void updateContent() {
        final String title = gpxFile.metadata.getArticleTitle();
        final String imageUrl = getMetadataImageLink(gpxFile.metadata);
        final String descriptionHtml = gpxFile.metadata.getDescription();

        setupImage(imageUrl);

        TextViewEx tvDescription = view.findViewById(R.id.description);
        tvDescription.setText(getFirstParagraph(descriptionHtml));

        TextViewEx readBtn = view.findViewById(R.id.btn_read_full);
        readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GpxReadDescriptionDialogFragment.showInstance(mapActivity, title, imageUrl, descriptionHtml);
            }
        });
        TextViewEx editBtn = view.findViewById(R.id.btn_edit);
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GpxEditDescriptionDialogFragment.showInstance(mapActivity, descriptionHtml);
            }
        });
    }

    @Nullable
    private String getMetadataImageLink(@NonNull GPXUtilities.Metadata metadata) {
        String link = metadata.link;
        if (!TextUtils.isEmpty(link)) {
            String lowerCaseLink = link.toLowerCase();
            if (lowerCaseLink.contains(".jpg")
                    || lowerCaseLink.contains(".jpeg")
                    || lowerCaseLink.contains(".png")
                    || lowerCaseLink.contains(".bmp")
                    || lowerCaseLink.contains(".webp")) {
                return link;
            }
        }
        return null;
    }

    private String getFirstParagraph(String descriptionHtml) {
        if (descriptionHtml != null) {
            String firstParagraph = WikiArticleHelper.getPartialContent(descriptionHtml);
            if (!TextUtils.isEmpty(firstParagraph)) {
                return firstParagraph;
            }
        }
        return descriptionHtml;
    }

    private void setupImage(final String imageUrl) {
        if (imageUrl == null) {
            return;
        }
        final PicassoUtils picasso = PicassoUtils.getPicasso(app);
        RequestCreator rc = Picasso.get().load(imageUrl);
        final AppCompatImageView image = view.findViewById(R.id.main_image);
        rc.into(image, new Callback() {
            @Override
            public void onSuccess() {
                picasso.setResultLoaded(imageUrl, true);
                AndroidUiHelper.updateVisibility(image, true);
            }

            @Override
            public void onError(Exception e) {
                picasso.setResultLoaded(imageUrl, false);
            }
        });
    }

}
