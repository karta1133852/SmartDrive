package com.example.smartdrive.instance;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class ICDevice {

    public String deviceID;
    public long currentPlanMillisecond, nextPlanMillisecond;
    public int[] cycle;
    public int[] green, yellow, allred;
    public int planOrder;
    public String phaseOrder;
    public JSONObject phaseData;

    public ICDevice() {

    }

    public ICDevice(ICDevice d) {
        this.deviceID = d.deviceID;
        this.currentPlanMillisecond = d.currentPlanMillisecond;
        this.nextPlanMillisecond = d.nextPlanMillisecond;
        this.cycle = Arrays.copyOf(d.cycle, d.cycle.length);
        this.green = Arrays.copyOf(d.green, d.green.length);
        this.yellow = Arrays.copyOf(d.yellow, d.yellow.length);
        this.allred = Arrays.copyOf(d.allred, d.allred.length);
        this.planOrder = d.planOrder;
        this.phaseOrder = d.phaseOrder;
    }
}