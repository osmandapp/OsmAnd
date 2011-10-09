package net.osmand.plus.activities;

import android.app.Dialog;
import android.os.Bundle;

public interface DialogProvider {

    public Dialog onCreateDialog(int id, Bundle args);

    public void onPrepareDialog(int id, Dialog dialog, Bundle args);

}
