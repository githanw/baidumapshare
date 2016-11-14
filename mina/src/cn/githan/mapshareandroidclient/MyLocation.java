package cn.githan.mapshareandroidclient;


import java.io.Serializable;

/**
 * Created by BW on 16/6/5.
 */
public class MyLocation implements Serializable {

    private double longitude;//经度
    private double latitude;//纬度
    private long id;//Session ID
    private String status;//session状态

    public MyLocation() {
    }

    public MyLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longtitude) {
        this.longitude = longtitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String toString() {
        return "Longitude(" + longitude + ") Latitude(" + latitude+")";
    }
}
