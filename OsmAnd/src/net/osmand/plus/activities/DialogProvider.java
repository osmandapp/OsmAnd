package net.osmand.plus.activities;

import android.app.Dialog;

public interface DialogProvider {

    public Dialog onCreateDialog(int id);

    public void onPrepareDialog(int id, Dialog dialog);

}
