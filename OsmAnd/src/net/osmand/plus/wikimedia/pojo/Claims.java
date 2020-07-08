
package net.osmand.plus.wikimedia.pojo;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Claims {

    @SerializedName("P18")
    @Expose
    private List<P18> p18 = null;

    public List<P18> getP18() {
        return p18;
    }

    public void setP18(List<P18> p18) {
        this.p18 = p18;
    }

}
