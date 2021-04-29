package com.zjh.btim.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectionModel implements Parcelable {
    private String mobileNumber;
    private String deviceMac;
    private String deviceName;

    public ConnectionModel(String mobileNumber, String deviceMac, String deviceName) {
        this.mobileNumber = mobileNumber;
        this.deviceMac = deviceMac;
        this.deviceName = deviceName;
    }

    protected ConnectionModel(Parcel in) {
        mobileNumber = in.readString();
        deviceMac = in.readString();
        deviceName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mobileNumber);
        dest.writeString(deviceMac);
        dest.writeString(deviceName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ConnectionModel> CREATOR = new Creator<ConnectionModel>() {
        @Override
        public ConnectionModel createFromParcel(Parcel in) {
            return new ConnectionModel(in);
        }

        @Override
        public ConnectionModel[] newArray(int size) {
            return new ConnectionModel[size];
        }
    };

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
