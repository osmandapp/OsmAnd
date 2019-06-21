package net.osmand.turnScreenOn.helpers;

import android.util.SparseArray;
import android.widget.RadioButton;

public class RadioGroupWrapper {
    SparseArray<RadioButton> radioButtons;

    public RadioGroupWrapper() {
        radioButtons = new SparseArray<>();
    }

    public void addRadioButton(int id, RadioButton rb) {
        radioButtons.put(id, rb);
    }

    public void removeRadioButton(int id) {
        radioButtons.remove(id);
    }

    public void setChecked(int id) {
        if (radioButtons != null && radioButtons.size() > 0) {
            RadioButton rb = radioButtons.get(id);
            if (rb != null) {
                rb.setChecked(true);
                throwOtherRadioButtonsExceptThis(id);
            } else {
                int firstElementId = radioButtons.keyAt(0);
                rb = radioButtons.get(firstElementId);
                rb.setChecked(true);
                throwOtherRadioButtonsExceptThis(firstElementId);
            }
        }
    }

    private void throwOtherRadioButtonsExceptThis(int id) {
        for (int i = 0; i < radioButtons.size(); i++) {
            int currentId = radioButtons.keyAt(i);
            if (currentId != id) {
                radioButtons.get(currentId).setChecked(false);
            }
        }
    }
}
