package com.atakmap.android.LoRaBridge.Contacts;

import org.json.JSONException;
import org.json.JSONObject;

public class PluginContact {
    private String id;
    private String callsign;
    private String deviceCallsign;
    private String ipAddress;
    private int port = -1;
    private String phone;
    private double latitude;
    private double longitude;
    private double altitude;
    private int battery = -1;
    private long lastUpdated;
    private boolean isLocal = false;

    public PluginContact(String id, String callsign, String deviceCallsign) {
        this.id = id;
        this.callsign = callsign;
        this.deviceCallsign = deviceCallsign;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters 和 Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }

    public String getDeviceCallsign() { return deviceCallsign; }
    public void setDeviceCallsign(String deviceCallsign) { this.deviceCallsign = deviceCallsign; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    public int getBattery() { return battery; }
    public void setBattery(int battery) { this.battery = battery; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isLocal() { return isLocal; }
    public void setLocal(boolean local) { isLocal = local; }


    public String getFullAddress() {
        if (ipAddress != null && port > 0) {
            return ipAddress + ":" + port;
        }
        return "N/A";
    }

    public String toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("callsign", callsign);
            json.put("deviceCallsign", deviceCallsign);
            json.put("ipAddress", ipAddress);
            json.put("port", port);
            json.put("phone", phone);
            json.put("latitude", latitude);
            json.put("longitude", longitude);
            json.put("altitude", altitude);
            json.put("battery", battery);
            json.put("lastUpdated", lastUpdated);
            json.put("isLocal", isLocal);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public static PluginContact fromJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            PluginContact contact = new PluginContact(
                    json.getString("id"),
                    json.getString("callsign"),
                    json.getString("deviceCallsign")
            );

            if (json.has("ipAddress")) contact.setIpAddress(json.getString("ipAddress"));
            if (json.has("port")) contact.setPort(json.getInt("port"));
            if (json.has("phone")) contact.setPhone(json.getString("phone"));
            if (json.has("latitude")) contact.setLatitude(json.getDouble("latitude"));
            if (json.has("longitude")) contact.setLongitude(json.getDouble("longitude"));
            if (json.has("altitude")) contact.setAltitude(json.getDouble("altitude"));
            if (json.has("battery")) contact.setBattery(json.getInt("battery"));
            if (json.has("lastUpdated")) contact.setLastUpdated(json.getLong("lastUpdated"));
            if (json.has("isLocal")) contact.setLocal(json.getBoolean("isLocal"));

            return contact;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}