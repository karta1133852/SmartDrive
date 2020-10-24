package com.example.smartdrive.util;

import android.util.Log;

import com.example.smartdrive.instance.ICDevice;
import com.example.smartdrive.instance.Phase;

public class PhaseOrder {

    private static Phase[] returnStatus(String status) {
        Phase[] mStatus = {new Phase(status, -1)};
        return mStatus;
    }

    private static Phase[] returnStatus(String status, int countdown, String phaseOrder) {
        Phase phase = new Phase(status, countdown);
        phase.phaseOrder = phaseOrder;
        Phase[] mStatus = {phase};
        return mStatus;
    }

    public static Phase[] calculateStatus(ICDevice device) {
        String status;
        Phase[] mPhases;
        if (device.planOrder == -1) {
            status = "phase讀取錯誤";
            return returnStatus(status);
        } else if (device.phaseOrder == null) {
            status = "找不到phaseOrder";
            return returnStatus(status);
        } else if (device.phaseOrder.equals("B0")) {
            if (device.planOrder == 0)
                status = "閃黃燈";
            else
                status = "閃紅燈";

            return returnStatus(status, 0, device.phaseOrder);
        } else if (device.phaseOrder.equals("00")) {
            int index = device.planOrder;
            mPhases = PhaseOrder.normalPhase(device, index);
        } else if (device.phaseOrder.equals("A0")) {
            int index = device.planOrder;
            mPhases = PhaseOrder.normalPhase(device, index);
        } else if (device.phaseOrder.equals("A2")) {
            int index = device.planOrder;
            mPhases = PhaseOrder.normalPhase(device, index);
        } else if (device.phaseOrder.equals("40")) {
            if (device.planOrder == 0) {
                mPhases = PhaseOrder.straightLeftPhase(device, 0);
            } else {
                mPhases = PhaseOrder.normalPhase(device, 2);
            }
        } else if (device.phaseOrder.equals("60") || device.phaseOrder.equals("52")) {
            if (device.planOrder == 0) {
                mPhases = PhaseOrder.straightLeftPhase(device, 0);
            } else {
                mPhases = PhaseOrder.straightLeftPhase(device, 2);
            }
        } else if (device.phaseOrder.equals("D2")) {
            status = "此時相尚未支援";
            return returnStatus(status);
        } else if (device.phaseOrder.equals("D8")) {
            if (device.planOrder == 0) {
                mPhases = PhaseOrder.phaseD8(device, 0);
            } else {
                mPhases = PhaseOrder.straightLeftPhase(device, 2);
            }
        } else if (device.phaseOrder.equals("E2")) {
            if (device.planOrder == 0) {
                mPhases = PhaseOrder.phaseE2(device, 0, true);
            } else if (device.planOrder == 1) {
                mPhases = PhaseOrder.phaseE2(device, 0, false);
            } else {
                mPhases = PhaseOrder.straightLeftPhase(device, 3);
            }
        } else {
            status = "此時相尚未支援";
            return returnStatus(status);
        }

        mPhases[0].phaseOrder = device.phaseOrder;

        return mPhases;
    }

    public static Phase[] calculateStatus(ICDevice device, int phase) {
        Phase[] mPhases = PhaseOrder.normalPhase(device, phase-1);
        return mPhases;
    }

    public static Phase[] normalPhase(ICDevice device, int index) {
        String status;
        Phase mPhase = null;
        if (index > device.green.length-1)
            return returnStatus("無此phase");
        //Log.d("phase", device.green[index]+","+device.yellow[index]+","+device.allred[index]+","+device.cycle[index]);
        long difference = System.currentTimeMillis() - device.currentPlanMillisecond;
        int seconds = (int) (difference / 1000);
        if (device.phaseOrder.equals("B0"))
            status = "閃燈";
        else if (device.cycle[index] == 0)
            status = "cycle = 0";
        else {
            int s = seconds % device.cycle[index];
            int red = 0;
            for (int i = 0; i < index; i++)
                red += device.green[i] + device.yellow[i] + device.allred[i];
            //Log.d("calculate", String.valueOf(device.green[index]));
            if (s < red) {
                mPhase = new Phase("red", red - s);
                status = "紅燈 " + String.format("% 3d", red - s);
            } else {
                s -= red;
                if (s < device.green[index]) {
                    mPhase = new Phase("green", device.green[index] - s);
                    status = "綠燈 " + String.format("% 3d", device.green[index] - s);
                } else if (s < device.green[index] + device.yellow[index]) {
                    mPhase = new Phase("yellow", device.yellow[index] - (s - device.green[index]));
                    status = "黃燈 " + String.format("% 3d", device.yellow[index] - (s - device.green[index]));
                } else {
                    mPhase = new Phase("red", device.cycle[index] - s);
                    status = "紅燈 " + String.format("% 3d", device.cycle[index] - s);
                }
            }
        }

        return new Phase[] {mPhase};
    }

    public static Phase[] overlapPrev(ICDevice device, int index) {
        ICDevice tmpDevice = new ICDevice(device);
        tmpDevice.green[index] += tmpDevice.green[index-1] + tmpDevice.yellow[index-1] + tmpDevice.allred[index-1];
        tmpDevice.green[index-1] = 0;
        tmpDevice.yellow[index-1] = 0;
        tmpDevice.allred[index-1] = 0;
        return normalPhase(tmpDevice, index);
    }

    public static Phase[] overlapNext(ICDevice device, int index) {
        ICDevice tmpDevice = new ICDevice(device);
        tmpDevice.green[index] += tmpDevice.green[index+1];
        return normalPhase(tmpDevice, index);
    }

    public static Phase[] straightLeftPhase(ICDevice device, int index) {
        Phase statusStraight = normalPhase(device, index)[0];
        Phase statusLeft = normalPhase(device, index+1)[0];

        return new Phase[] {statusStraight, statusLeft};
    }

    public static Phase[] phaseD8(ICDevice device, int index) {
        Phase statusAll = normalPhase(device, index)[0];
        Phase statusLeft = overlapPrev(device, index+1)[0];

        return new Phase[] {statusAll, statusLeft};
    }

    public static Phase[] phaseE2(ICDevice device, int index, boolean greenFirst) {
        Phase[] mPhases;
        if (greenFirst) {
            mPhases = overlapPrev(device, index+1);
        } else {
            Phase statusAll = overlapNext(device, index+1)[0];
            ICDevice tmpDevice = new ICDevice(device);
            tmpDevice.yellow[index+1] = 0;
            Phase statusLeft = normalPhase(tmpDevice, index+2)[0];
            mPhases = new Phase[] {statusAll, statusLeft};
        }

        return mPhases;
    }
}
