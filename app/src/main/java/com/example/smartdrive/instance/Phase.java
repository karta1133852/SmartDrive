package com.example.smartdrive.instance;

public class Phase {

    public String phaseOrder;
    public int planOrder;

    public String phaseState;
    public int countdown;

    public Phase() {

    }

    public Phase(String phaseState, int countdown) {
        this.phaseState = phaseState;
        this.countdown = countdown;
    }
}
