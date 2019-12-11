package net.osmand.router;


public class ExitInfo {

    private String ref;

    private String shieldName;

    private String shieldIconName;

    private String exitStreetName;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getShieldName() {
        return shieldName;
    }

    public void setShieldName(String shieldName) {
        this.shieldName = shieldName;
    }

    public String getExitStreetName() {
        return exitStreetName;
    }

    public void setExitStreetName(String exitStreetName) {
        this.exitStreetName = exitStreetName;
    }

    public String getShieldIconName() {
        return shieldIconName;
    }

    public void setShieldIconName(String shieldIconName) {
        this.shieldIconName = shieldIconName;
    }
}
