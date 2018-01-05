package com.example.user.wifiprob;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by User on 8/31/2017.
 */

public class location implements Serializable {
    ArrayList<String> macs;
    ArrayList<ArrayList<Integer>> rss;
    ArrayList<ArrayList<Double>> probs;
    boolean valid=false;

    private static final long serialVersionUID = 42L;

    public void setObject(ArrayList<String> macs, ArrayList<ArrayList<Integer>> rss, ArrayList<ArrayList<Double>> probs) {
        this.macs = macs;
        this.rss = rss;
        this.probs = probs;
        this.valid = true;
    }

    public ArrayList<String> getMacs() {
        return macs;
    }

    public ArrayList<ArrayList<Double>> getProbs() {
        return probs;
    }

    public ArrayList<ArrayList<Integer>> getRss() {
        return rss;
    }

    public boolean isValid(){
        return valid;
    }
}

