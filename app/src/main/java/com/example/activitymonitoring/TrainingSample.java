package com.example.activitymonitoring;

class TrainingSample {
    private String activity;
    private double accXDC, accYDC, accZDC;
    private double accXfrqE, accXfrqLoc;
    private double accYfrqE, accYfrqLoc;
    private double accZfrqE, accZfrqLoc;
    private double accXstd, accYstd, accZstd;

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public double getAccXDC() {
        return accXDC;
    }

    public void setAccXDC(double accXDC) {
        this.accXDC = accXDC;
    }

    public double getAccYDC() {
        return accYDC;
    }

    public void setAccYDC(double accYDC) {
        this.accYDC = accYDC;
    }

    public double getAccZDC() {
        return accZDC;
    }

    public void setAccZDC(double accZDC) {
        this.accZDC = accZDC;
    }

    public double getAccXfrqE() {
        return accXfrqE;
    }

    public void setAccXfrqE(double accXfrqE) {
        this.accXfrqE = accXfrqE;
    }

    public double getAccXfrqLoc() {
        return accXfrqLoc;
    }

    public void setAccXfrqLoc(double accXfrqLoc) {
        this.accXfrqLoc = accXfrqLoc;
    }

    public double getAccYfrqE() {
        return accYfrqE;
    }

    public void setAccYfrqE(double accYfrqE) {
        this.accYfrqE = accYfrqE;
    }

    public double getAccYfrqLoc() {
        return accYfrqLoc;
    }

    public void setAccYfrqLoc(double accYfrqLoc) {
        this.accYfrqLoc = accYfrqLoc;
    }

    public double getAccZfrqE() {
        return accZfrqE;
    }

    public void setAccZfrqE(double accZfrqE) {
        this.accZfrqE = accZfrqE;
    }

    public double getAccZfrqLoc() {
        return accZfrqLoc;
    }

    public void setAccZfrqLoc(double accZfrqLoc) {
        this.accZfrqLoc = accZfrqLoc;
    }

    public double getAccXstd() {
        return accXstd;
    }

    public void setAccXstd(double accXstd) {
        this.accXstd = accXstd;
    }

    public double getAccYstd() {
        return accYstd;
    }

    public void setAccYstd(double accYstd) {
        this.accYstd = accYstd;
    }

    public double getAccZstd() {
        return accZstd;
    }

    public void setAccZstd(double accZstd) {
        this.accZstd = accZstd;
    }

    @Override
    public String toString() {
        return "TrainingSample{" +
                "activity='" + activity + '\'' +
                ", accXDC=" + accXDC +
                ", accYDC=" + accYDC +
                ", accZDC=" + accZDC +
                ", accXfrqE=" + accXfrqE +
                ", accXfrqLoc=" + accXfrqLoc +
                ", accYfrqE=" + accYfrqE +
                ", accYfrqLoc=" + accYfrqLoc +
                ", accZfrqE=" + accZfrqE +
                ", accZfrqLoc=" + accZfrqLoc +
                ", accXstd=" + accXstd +
                ", accYstd=" + accYstd +
                ", accZstd=" + accZstd +
                '}';
    }
}
