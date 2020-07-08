
package net.osmand.plus.wikimedia.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Mainsnak {

    @SerializedName("snaktype")
    @Expose
    private String snaktype;
    @SerializedName("property")
    @Expose
    private String property;
    @SerializedName("hash")
    @Expose
    private String hash;
    @SerializedName("datavalue")
    @Expose
    private Datavalue datavalue;
    @SerializedName("datatype")
    @Expose
    private String datatype;

    public String getSnaktype() {
        return snaktype;
    }

    public void setSnaktype(String snaktype) {
        this.snaktype = snaktype;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Datavalue getDatavalue() {
        return datavalue;
    }

    public void setDatavalue(Datavalue datavalue) {
        this.datavalue = datavalue;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

}
