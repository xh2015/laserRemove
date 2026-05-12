package com.topsky.laserremove.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.blankj.utilcode.util.ActivityUtils;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

public class LocationUtils {
    public double latitude = -1;
    public double longitude = -1;
    private LocationManager locationManager;
    private String locationProvider;
    private int gpsType = -1;//0 gps 1 网络
    private int gpsNum;//gps定位失败数量

    private static class Holder {
        private static LocationUtils mLocationUtils = new LocationUtils();
    }

    public static LocationUtils getInstance() {
        return Holder.mLocationUtils;
    }

    @RequiresPermission(allOf = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION })
    public void getLocation() {
        getLocationAddress();
    }

    /**
     * 获取当前定位
     */
    @RequiresPermission(anyOf = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION })
    public void getLocationAddress() {
        //1.获取位置管理器
        locationManager = (LocationManager) ActivityUtils.getTopActivity().getSystemService(Context.LOCATION_SERVICE);
        // 检查 GPS 和网络定位是否可用
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        switch (gpsType) {
            case -1:
                if (isGPSEnabled) {
                    locationProvider = LocationManager.GPS_PROVIDER;
                    gpsType = 0;
                } else if (isNetworkEnabled) {
                    locationProvider = LocationManager.NETWORK_PROVIDER;
                    gpsType = 1;
                }
                break;
            case 0:
                if (longitude == -1) {//说明gps定位失效尝试网络定位
                    locationProvider = LocationManager.NETWORK_PROVIDER;
                    gpsType = 1;
                } else {
                    locationProvider = LocationManager.GPS_PROVIDER;
                }
                break;
            case 1:
                if (longitude == -1) {//说明网络定位失效尝试gps定位
                    locationProvider = LocationManager.GPS_PROVIDER;
                    gpsType = 0;
                } else {
                    locationProvider = LocationManager.NETWORK_PROVIDER;
                }
                break;
        }

        //3.获取上次的位置，一般第一次运行，此值为null
        if (ActivityCompat.checkSelfPermission(ActivityUtils.getTopActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ActivityUtils.getTopActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            upLocation();
            return;
        }
        if (locationProvider == null) {
            locationProvider = LocationManager.NETWORK_PROVIDER;
            gpsType = 1;
        }
        // 监视地理位置变化，第二个和第三个参数分别为更新的最短时间minTime和最短距离minDistance
        locationManager.requestLocationUpdates(locationProvider, 1000, 0, locationListener);
        Location location = locationManager.getLastKnownLocation(locationProvider);
        if (location != null) {//可以获取到定位
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            upLocation();
        } else {//curLat=34.22912&curLon=117.197088&pageNum=1&pageSize=20 (350ms)
            if (gpsNum < 1) {
                gpsNum++;
                getLocationAddress();
                return;
            }
            upLocation();
            gpsNum = 0;
        }

    }

    /**
     * LocationListern监听器
     * 参数：地理位置提供器、监听位置变化的时间间隔、位置变化的距离间隔、LocationListener监听器
     */
    boolean isFirst = false;
    LocationListener locationListener = new LocationListener() {
        /**
         * 当某个位置提供者的状态发生改变时
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {

        }

        /**
         * 某个设备打开时
         */
        @Override
        public void onProviderEnabled(String provider) {

        }

        /**
         * 某个设备关闭时
         */
        @Override
        public void onProviderDisabled(String provider) {

        }

        /**
         * 手机位置发生变动
         */
        @Override
        public void onLocationChanged(Location location) {
            Activity activity = ActivityUtils.getTopActivity();
            if (activity != null && activity.isDestroyed()) {
                //手动更改位置则不在更新定位
                if (locationManager != null) {
                    locationManager.removeUpdates(locationListener);
                }
            } else {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    upLocation();
                } else {
                    upLocation();
                }
            }
        }
    };

    private void upLocation() {
        if (mOnLocationListener != null) {
            if (longitude > 0 && latitude > 0) {
                if (locationManager != null) {
                    locationManager.removeUpdates(locationListener);
                }
                mOnLocationListener.onLocationSuccess(longitude, latitude);
            }
        }
    }


    public void setOnLocationListener(onLocationListener mOnLocationListener) {
        this.mOnLocationListener = mOnLocationListener;
    }

    private onLocationListener mOnLocationListener;

    public interface onLocationListener {
        void onLocationSuccess(double longitude, double latitude);
    }

}
