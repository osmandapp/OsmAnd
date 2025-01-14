package net.osmand.plus.track.clickable;

import net.osmand.CallbackWithObject;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseLoadAsyncTask;

public class ClickableWayReaderTask extends BaseLoadAsyncTask<Void, Void, ClickableWay> {
    private final ClickableWay clickableWay;
    private final CallbackWithObject<ClickableWay> callback;

    public ClickableWayReaderTask(MapActivity mapActivity, ClickableWay clickableWay,
                                  CallbackWithObject<ClickableWay> callback) {
        super(mapActivity);
        this.callback = callback;
        this.clickableWay = clickableWay;
    }

    @Override
    protected ClickableWay doInBackground(Void... voids) {
        // TODO read height data, get analytics (RouteSectionReader.java?)
//        for (int i = 0; i < 3; i++) {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
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
        if (callback != null) {
            callback.processResult(clickableWay);
        }
        hideProgress();
    }
}
