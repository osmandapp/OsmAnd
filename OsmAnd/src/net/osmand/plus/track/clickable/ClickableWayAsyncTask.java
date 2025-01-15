package net.osmand.plus.track.clickable;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseLoadAsyncTask;

public class ClickableWayAsyncTask extends BaseLoadAsyncTask<Void, Void, ClickableWay> {
    private final ClickableWay clickableWay;
    private final CallbackWithObject<ClickableWay> readHeightData;
    private final CallbackWithObject<ClickableWay> openAsGpxFile;

    public ClickableWayAsyncTask(@NonNull MapActivity mapActivity,
                                 @NonNull ClickableWay clickableWay,
                                 @NonNull CallbackWithObject<ClickableWay> readHeightData,
                                 @NonNull CallbackWithObject<ClickableWay> openAsGpxFile) {
        super(mapActivity);
        this.clickableWay = clickableWay;
        this.readHeightData = readHeightData;
        this.openAsGpxFile = openAsGpxFile;
    }

    @Override
    protected ClickableWay doInBackground(Void... voids) {
        readHeightData.processResult(clickableWay);
        return this.clickableWay;
    }

    @Override
    protected void onPreExecute() {
        if (isShouldShowProgress()) {
            showProgress(true);
        }
    }

    @Override
    protected void onPostExecute(ClickableWay clickableWay) {
        openAsGpxFile.processResult(clickableWay);
        hideProgress();
    }
}
