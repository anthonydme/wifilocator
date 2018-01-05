package com.example.user.wifiprob;

import java.io.Serializable;
import java.util.ArrayList;

import static java.lang.Math.round;

/**
 * Created by Anthony on 9/26/2017.
 */

public class WAP implements Serializable {
    ArrayList<String> mac;//decided to change both of these vectors to ArrayLists for ease of use
    ArrayList<Integer> rss;
    ArrayList<String> filtMacs;
    boolean valid=false;
    private static final long serialVersionUID = 45L;

    public ArrayList<String> getMac(){
        return mac;
    }

    public ArrayList<Integer> getRss(){
        return rss;
    }

    public ArrayList<String> getFiltMacs(){return filtMacs;}

    public void setFiltMacs(String m){this.filtMacs.add(m);}

    public void setVector(ArrayList<String> m, ArrayList<Integer> r, ArrayList<String> f){
        //once the mac and rss vectors are passed to this method for storage in the object
        //the vectors will be sorted based on signal level (highest to lowest)
        //this sort will be done on int[] r using quicksort
        this.mac = m;
        this.rss = r;
        this.filtMacs = f;
        quickSort(0,r.size()-1);
        valid = true;



    }

    public boolean isValid(){
        return valid;
    }

    private void quickSort(int l, int h){
        //first find pivot (middle element)

        int pivot = rss.get(round(l+(h-l)/2));

        //counting elements
        int cl = l;
        int ch = h;

        while(cl<=ch){
            while(rss.get(cl)>pivot){
                cl++;
            }
            while(rss.get(ch)<pivot){
                ch--;
            }

            if(cl<=ch){
                swap(cl,ch);
                cl++;
                ch--;
            }
        }

        if(l<ch){
            quickSort(l,ch);
        }

        if(cl<h){
            quickSort(cl,h);
        }

    }

    private void swap(int l, int h){
        int rssTemp = rss.get(h);
        String macTemp = mac.get(h);

        rss.set(h,rss.get(l));
        rss.set(l, rssTemp);

        mac.set(h,mac.get(l));
        mac.set(l,macTemp);
    }


}