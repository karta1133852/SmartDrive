package com.example.smartdrive.ui.database;

import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartdrive.MainActivity;
import com.example.smartdrive.callback.DeviceCallback;
import com.example.smartdrive.instance.ICDevice;
import com.example.smartdrive.instance.Phase;
import com.example.smartdrive.util.PhaseOrder;
import com.example.smartdrive.util.Traffic;

import java.util.ArrayList;

public class DatabaseViewModel extends ViewModel {

    private MutableLiveData<String> mStatusStr;
    private MutableLiveData<ArrayList<String>> mDeviceList;
    private MutableLiveData<String> mDeviceID;
    private MutableLiveData<String> mPhase;
    private MutableLiveData<Phase[]> mPhases;

    private final static int INTERVAL = 1000;
    private Handler mHandler;
    private ICDevice device;


    public DatabaseViewModel() {
        mStatusStr = new MutableLiveData<>();
        mDeviceList = new MutableLiveData<>();
        mDeviceID = new MutableLiveData<>();
        mPhase = new MutableLiveData<>();
        mPhases = new MutableLiveData<>();
        mHandler = new Handler();

        mDeviceList.setValue(Traffic.getDeviceList(MainActivity.deviceIDList));
        /*Traffic.getDeviceList(new ArrayListCallback() {
            @Override
            public void callback(ArrayList<String> list) {
                mDeviceList.setValue(list);
            }
        });*/
        setDeviceID("S014401");
    }

    public LiveData<String> getStatusStr() {
        return mStatusStr;
    }

    public LiveData<ArrayList<String>> getDeviceList() {
        return mDeviceList;
    }

    public LiveData<String> getDeviceID() {
        return mDeviceID;
    }

    public LiveData<String> getPhase() {
        return mPhase;
    }

    public LiveData<Phase[]> getPhases() {
        return mPhases;
    }

    public void setDevice(ICDevice device) {
        this.device = device;
        setDeviceID(device.deviceID);
    }

    public void setDeviceID(String deviceID) {
        if (deviceID == null || (device != null && deviceID == device.deviceID))
            return;

        mDeviceID.setValue(deviceID);
        stopRepeatingTask();
        Traffic.getDeviceInfo(deviceID, new DeviceCallback() {
            @Override
            public void callback(ICDevice d) {
                device = d;
                startRepeatingTask();
            }
        });
    }

    public void setPhase(String phase) {
        //mPhase.setValue(phase);
        if (device != null)
            device.planOrder = Integer.parseInt(phase)-1;
    }

    Runnable mHandlerTask = new Runnable() {
        @Override
        public void run() {
            Traffic.updateDevicePhase(device);
            //String status = Traffic.calculateStatus(device);
            //String status = Traffic.calculateStatus(device, Integer.parseInt(mPhase.getValue()));
                        /*String status = Traffic.calculateStatus(device, 1) + "\n"
                    + Traffic.calculateStatus(device, 2) + "\n"
                    + Traffic.calculateStatus(device, 3) + "\n";*/
            //String status = Traffic.calculateStatus(device, 2);
            //mStatusStr.postValue(status);
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