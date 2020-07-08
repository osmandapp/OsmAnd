
package net.osmand.plus.wikimedia.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WikipediaResponse {

    @SerializedName("claims")
    @Expose
    private Claims claims;

    public Claims getClaims() {
        return claims;
    }

    public void setClaims(Claims claims) {
        this.claims = claims;
    }

}
