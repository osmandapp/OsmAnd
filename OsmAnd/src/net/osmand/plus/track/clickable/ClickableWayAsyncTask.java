package net.osmand.plus.track.clickable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.binary.HeightDataLoader.CancellableCallback;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseLoadAsyncTask;

public class ClickableWayAsyncTask extends BaseLoadAsyncTask<Void, Void, ClickableWay> {
    private final ClickableWay clickableWay;
    private final CancellableCallback<ClickableWay> readHeightData;
    private final CallbackWithObject<ClickableWay> openAsGpxFile;

    public ClickableWayAsyncTask(@NonNull MapActivity mapActivity,
                                 @NonNull ClickableWay clickableWay,
                                 @NonNull CancellableCallback<ClickableWay> readHeightData,
                                 @NonNull CallbackWithObject<ClickableWay> openAsGpxFile) {
        super(mapActivity);
        this.clickableWay = clickableWay;
        this.readHeightData = readHeightData;
        this.openAsGpxFile = openAsGpxFile;
    }

    @Nullable
    @Override
    protected ClickableWay doInBackground(Void... voids) {
        return readHeightData.callback(clickableWay, this::isCancelled) ? this.clickableWay : null;
    }

    @Override
    protected void onPreExecute() {
        if (isShouldShowProgress()) {
            showProgress(true);
        }
    }

    @Override
    protected void onPostExecute(@Nullable ClickableWay clickableWay) {
        openAsGpxFile.processResult(clickableWay);
        hideProgress();
    }
}
