package com.example.smartdrive.ui.home;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smartdrive.MainActivity;
import com.example.smartdrive.callback.ArrayListCallback;
import com.example.smartdrive.callback.DeviceCallback;
import com.example.smartdrive.callback.StringCallback;
import com.example.smartdrive.instance.Direction;
import com.example.smartdrive.instance.ICDevice;
import com.example.smartdrive.instance.Node;
import com.example.smartdrive.instance.Phase;
import com.example.smartdrive.util.PhaseOrder;
import com.example.smartdrive.util.Traffic;

import java.util.ArrayList;

public class HomeViewModel extends AndroidViewModel {

    private MutableLiveData<String> mRoadName;
    private MutableLiveData<String> mRoadsStr;
    private MutableLiveData<Direction> mDirection;
    private MutableLiveData<Location> mCurLocation;
    private MutableLiveData<ICDevice> mDevice;
    private MutableLiveData<String> mStatusStr;
    private MutableLiveData<Phase[]> mPhases;

    private double curLng, prevLng;
    private double curLat, prevLat;
    private boolean isGPSRunning = false;
    private LocationManager mLocationManager;
    private static final int LOCATION_UPDATE_MIN_DISTANCE = 0;
    private static final int LOCATION_UPDATE_MIN_TIME = 1000;

    private final static int INTERVAL = 1000;
    private Handler mHandler;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        mRoadName = new MutableLiveData<>();
        mRoadsStr = new MutableLiveData<>();
        mDirection = new MutableLiveData<>();
        mCurLocation = new MutableLiveData<>();
        mDevice = new MutableLiveData<>();
        mStatusStr = new MutableLiveData<>();
        mPhases = new MutableLiveData<>();

        mHandler = new Handler();
        mLocationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
        startGPS();
    }

    public LiveData<String> getRoadName() {
        return mRoadName;
    }

    public LiveData<String> getRoadsStr() {
        return mRoadsStr;
    }

    public LiveData<Direction> getDirection() {
        return mDirection;
    }

    public LiveData<Location> getCurLocation() {
        return mCurLocation;
    }

    public LiveData<ICDevice> getDevice() {
        return mDevice;
    }

    public LiveData<String> getStatusStr() {
        return mStatusStr;
    }

    public LiveData<Phase[]> getPhases() {
        return mPhases;
    }

    private void getRoadInfo() {
        Node approachedNode = null;
        if (mCurLocation.getValue() != null)
            Traffic.getNearbyNodes(MainActivity.nodes,
                    mCurLocation.getValue(), mDirection.getValue(), 50,
                    new StringCallback() {
                        @Override
                        public void callback(String str) {
                            Log.d("TestPhase", str);
                            mRoadName.setValue(str);
                        }
                    },
                    new DeviceCallback() {
                        @Override
                        public void callback(ICDevice device) {
                            Log.d("TestPhase", "deviceID: "+device.deviceID);
                            if (device != null) {
                                mDevice.setValue(device);
                                stopRepeatingTask();
                                startRepeatingTask();
                            }
                        }
                    });
        //mDeviceID.setValue("S014401");
        if (approachedNode != null) {
            /*Traffic.getDeviceByNode(approachedNode, mDirection.getValue(), new DeviceCallback() {
                @Override
                public void callback(ICDevice device) {
                    Log.e("Test", "DeviceID change.");
                    mDevice.setValue(device);
                }
            });*/

            /*Traffic.getRoads(approachedNode.nodeID, new ArrayListCallback() {
                @Override
                public void callback(ArrayList<String> list) {
                    String strList = "列表:\n";
                    for (String str : list) {
                        strList = strList + str + "\n";
                    }
                    mRoadsStr.setValue(strList);
                }
            });*/
        }
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                mCurLocation.setValue(location);
                prevLng = curLng;
                prevLat = curLat;
                curLng = location.getLongitude();
                curLat = location.getLatitude();
                double dLng = (double) (Math.round((curLng-prevLng)*100000))/100000;
                double dLat = (double) (Math.round((curLat-prevLat)*100000))/100000;
                mDirection.setValue(new Direction(dLng, dLat));
                //Log.e("GPS", msg);

                getRoadInfo();
            } else {
                // Logger.d("Location is null");
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    private void startGPS() {

        if (ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mRoadName.setValue("GPS沒有開啟！");
            return;
        }

        Location location = null;
        if (!isGPSRunning) {
            boolean isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!(isGPSEnabled || isNetworkEnabled)) {
                // location_provider error
            } else {
                /*if (isNetworkEnabled) {
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_MIN_TIME,
                            LOCATION_UPDATE_MIN_DISTANCE, mLocationListener);
                    location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } else */if (isGPSEnabled) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_MIN_TIME,
                            LOCATION_UPDATE_MIN_DISTANCE, mLocationListener);
                    location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Log.d("Android9", "startGPS: ");
                }

                if (location != null) {
                    mCurLocation.setValue(location);
                    mDirection.setValue(new Direction(0, 0));
                    prevLng = curLng = location.getLongitude();
                    prevLat = curLat = location.getLatitude();
                    Log.d("Android9", "startGPS: not null");
                }
                isGPSRunning = true;
            }
        }

        getRoadInfo();
    }

    Runnable mHandlerTask = new Runnable() {
        @Override
        public void run() {
            ICDevice device = Traffic.updateDevicePhase(mDevice.getValue());
            mDevice.setValue(device);
            //String status = Traffic.calculateStatus(device);
            //String status = Traffic.calculateStatus(device, Integer.parseInt(mPhase.getValue()));
            /*String status = Traffic.calculateStatus(device, 1) + "\n"
                    + Traffic.calculateStatus(device, 2) + "\n"
                    + Traffic.calculateStatus(device, 3) + "\n";*/
            //String status = Traffic.calculateStatus(device, 2);
            //mStatusStr.postValue(status);

            /*Phase[] phases = PhaseOrder.calculateStatus(mDevice.getValue());
            mPhases.setValue(phases);*/

            Phase[] phases = PhaseOrder.calculateStatus(device);
            phases[0].planOrder = device.planOrder;
            mPhases.setValue(phases);

            mHandler.postDelayed(mHandlerTask, INTERVAL);
        }
    };
    void startRepeatingTask() {
        mHandlerTask.run();
    }
    void stopRepeatingTask() {
        mHandler.removeCallbacks(mHandlerTask);
    }
}