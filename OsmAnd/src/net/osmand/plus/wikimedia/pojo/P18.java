
package net.osmand.plus.wikimedia.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class P18 {

    @SerializedName("mainsnak")
    @Expose
    private Mainsnak mainsnak;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("rank")
    @Expose
    private String rank;

    public Mainsnak getMainsnak() {
        return mainsnak;
    }

    public void setMainsnak(Mainsnak mainsnak) {
        this.mainsnak = mainsnak;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

}
