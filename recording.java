package com.example.user.wifiprob;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.example.user.wifiprob.R.id.wknnOrPedo;
import static java.lang.Math.round;

public class recording extends AppCompatActivity {

    //GUI variables
    private NumberPicker numX;
    private NumberPicker numY;

    private NumberPicker numMin;

    private Button recordbtn;
    private TextView recordResultsText;

    //wifi scan variables
    WifiManager mainWm;
    int PERM_REQ;
    Context context;
    WifiReceiver recWifi = new WifiReceiver();


    //programming variables
    //the dimensions for the main fpdb grid
    private int gridX = 10;
    private int gridY = 10;


    //the list of recording results
    ArrayList<RecordResult> resultList;

    //the main grid storing all the location objects
    //probabilistic method done here
    location [][] probGrid = new location[gridX][gridY];
    boolean gridPresent = false;

    //deterministic here
    WAP [][] detGrid = new WAP [gridX][gridY];

    //record deterministic results here
    ArrayList<WAP> waps;

    //settings
    boolean prob;
    boolean det;
    boolean knn;
    boolean wknn;

    //intent
    Intent i;

    int testcount = 0;


    private static final String TAG = "recording";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recording);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setTitle("Database Radio Mapping");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        //
        i = new Intent(getApplicationContext(), MainActivity.class);

        //get the intent that called this activity
        Intent intent = getIntent();

        det = intent.getBooleanExtra("det",true);
        prob = intent.getBooleanExtra("prob",false);
        knn = intent.getBooleanExtra("knn",true);
        wknn = intent.getBooleanExtra("wknn",false);


        //GUI Setup

        //permissions
        //permissions
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERM_REQ);
        }
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_WIFI_STATE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE},PERM_REQ);
        }

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PERM_REQ);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},PERM_REQ);
        }

        //coordinates
        numX = (NumberPicker) findViewById(R.id.numberPicker1);
        numY = (NumberPicker) findViewById(R.id.numberPicker2);

        numX.setMaxValue(9);
        numX.setMinValue(0);

        numY.setMaxValue(9);
        numY.setMinValue(0);

        numX.setWrapSelectorWheel(true);
        numY.setWrapSelectorWheel(true);

        //amount of time
        numMin = (NumberPicker) findViewById(R.id.numberPicker);

        numMin.setMaxValue(9);
        numMin.setMinValue(1);

        numMin.setWrapSelectorWheel(true);

        //recording button
        recordbtn = (Button) findViewById(R.id.record);

        resultList = new ArrayList<>();
        waps = new ArrayList<>();

        recordbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultList.clear();
                waps.clear();
                scan();
                testcount++;

//                recordResultsText.setText("DONE");


            }
        });

        //textview
        recordResultsText = (TextView) findViewById(R.id.RecordResults);


        //getting wifimanager instance
        mainWm = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        context = getApplicationContext();



        registerReceiver(recWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


        //the recording list of results initialised here
        resultList = new ArrayList<>();


        //initiate the grid

            //initialise the grid
            for (int i = 0; i < gridX; i++) {
                for (int j = 0; j < gridY; j++) {
                    probGrid[i][j] = new location();
                }
            }
            //and then read current grid into matrix for restorage at end run

            //initialise the grid
            for (int i = 0; i < gridX; i++) {
                for (int j = 0; j < gridY; j++) {
                    detGrid[i][j] = new WAP();
                }
            }

            //and then read current grid into matrix for restorage at end run

            //readDetData(context,"grid");


//        if(gridPresent) {
//            //print from the grid
//            for (int i = 0; i < gridX; i++) {
//                for (int j = 0; j < gridY; j++) {
//                    if (grid[i][j].isValid()) {
//                        Log.d(TAG, "POINT:" + i + "," + j);
//                        for (int y = 0; y < grid[i][j].getMacs().size(); y++) {
//                            Log.d(TAG, grid[i][j].getMacs().get(y) + ":\n");
//                            for (int u = 0; u < grid[i][j].getRss().get(y).size(); u++) {
//                                Log.d(TAG, grid[i][j].getRss().get(y).get(u) + " at " + (double) Math.round(grid[i][j].getProbs().get(y).get(u) * 100d) / 100d + "\n");
//                            }
//                        }
//                    }
//                }
//            }
//        }





    }

    @Override
    protected void onPause(){
        super.onPause();

       // unregisterReceiver(recWifi);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Log.d(TAG,"SAVE RECORDINGS, RETURN TO PREVIOUS ACTIVITY");

                i.putExtra("det",det);
                i.putExtra("prob",prob);
                i.putExtra("knn",knn);
                i.putExtra("wknn",wknn);

                startActivityIfNeeded(i,0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    public void readProbData(Context mC, String fn){
        try{
            String root = Environment.getExternalStorageDirectory().getAbsolutePath().toString();
            String finalRoot = root + "/probGrid/";

            FileInputStream fis = new FileInputStream(new File(finalRoot + fn + ".dat"));


            ObjectInputStream ois = new ObjectInputStream(fis);

            //fetch arraylist from file
            ArrayList<location> tempIn = new ArrayList<>();

            tempIn = (ArrayList<location>) ois.readObject();
            int index = 0;
            StringBuffer stringBuffer = new StringBuffer();

            // read tempIn into grid now
            for (int i=0;i<gridX;i++){
                for(int j=0; j<gridY;j++) {

                    if(index<tempIn.size()) {
                        probGrid[i][j] = tempIn.get(index);
                        if(probGrid[i][j].isValid()) {
                            stringBuffer.append(i + ", " + j + ":" + probGrid[i][j].getMacs().get(0) + "\n");
                        }

                        index++;
                    }
                }
            }

            //some testing to see its all working


            //recordResultsText.setText("Read Success!");
            //recordResultsText.setText(stringBuffer);
            gridPresent = true;
            ois.close();
            fis.close();


        } catch (Exception e){
            e.printStackTrace();
            //recordResultsText.setText("FAILED READ");
        }
    }

    public void readDetData(Context mC, String fn){
        try{
            String root = Environment.getExternalStorageDirectory().getAbsolutePath().toString();
            String finalRoot = root + "/detGrid/";

            FileInputStream fis = new FileInputStream(new File(finalRoot + fn + ".dat"));


            ObjectInputStream ois = new ObjectInputStream(fis);

            //fetch arraylist from file
            ArrayList<WAP> tempIn = new ArrayList<>();

            tempIn = (ArrayList<WAP>) ois.readObject();
            int index = 0;
            StringBuffer stringBuffer = new StringBuffer();

            // read tempIn into grid now
            for (int i=0;i<gridX;i++){
                for(int j=0; j<gridY;j++) {

                    if(index<tempIn.size()) {
                        detGrid[i][j] = tempIn.get(index);
                        if(detGrid[i][j].isValid()) {
                            stringBuffer.append(i + ", " + j + ":" + detGrid[i][j].getMac().get(0) + "\n");
                        }

                        index++;
                    }
                }
            }

            //some testing to see its all working


            //recordResultsText.setText("Read Success!");
            //recordResultsText.setText(stringBuffer);
            gridPresent = true;
            ois.close();
            fis.close();


        } catch (Exception e){
            e.printStackTrace();
            //recordResultsText.setText("FAILED READ");
        }
    }

    public void recordDetResults(int c1,int c2) {
        //take the two array lists created and extrapolate data into text file
        //do this after the averages and variance and whatnot
        String writing = c1 + "," + c2 + "\n";

        ArrayList<String> macs = new ArrayList<>();
        ArrayList<Double> avgs = new ArrayList<>();
        ArrayList<String> unreliable = new ArrayList<>();
        ArrayList<ArrayList<Integer>> readings = new ArrayList<>();


        for (int i = 0; i < waps.size(); i++) {
            if (i == 0) {
                for (int j = 0; j < waps.get(i).getMac().size(); j++) {
                    macs.add(waps.get(i).getMac().get(j));
                    readings.add(new ArrayList<Integer>());
                    readings.get(j).add(waps.get(i).getRss().get(j));
                }
            } else {
                for (int z = 0; z < waps.get(i).getMac().size(); z++) {
                    int ind = macs.indexOf(waps.get(i).getMac().get(z));
                    if (ind < 0) {
                        macs.add(waps.get(i).getMac().get(z));
                        readings.add(new ArrayList<Integer>());
                        readings.get(z).add(waps.get(i).getRss().get(z));
                    } else {
                        readings.get(ind).add(waps.get(i).getRss().get(z));
                    }


                }
            }
        }

        //averages calculation
        //as well as filtering out unreliable WAPs based on the amount of samples collected in the time frame
        //testing range here
        int high=-1;
        int low=999;
        for (int i = 0; i < macs.size(); i++) {
            if (readings.get(i).size() >= (readings.get(0).size() / 2)) {
                double temp = 0;
                for (int j = 0; j < readings.get(i).size(); j++) {
                    temp = temp + readings.get(i).get(j);

                    if(readings.get(i).get(j)<low){
                        low = readings.get(i).get(j);
                    }
                    if(readings.get(i).get(j)>high){
                        high = readings.get(i).get(j);
                    }
                }
                writing = writing +  i + " " + macs.get(i) + " level: " + (temp / readings.get(i).size()) + " range: " + (high-low) + "\n";
                //over here filtering is done by checking how many samples each mac address has
                // if it has more than half of the most amount of samples then we can use it
                //otherwise we remove it and its readings

                avgs.add(temp / readings.get(i).size());
                high=-1;
                low=999;
            } else {
                //over here we remove the unreliable WAPs that were found to only have a couple samples at this location
                //we store these unreliable WAPs in a separate ArrayList in the WAP object at this location for later comparison
                unreliable.add(macs.get(i));
                macs.remove(i);
                readings.remove(i);
                i--;
            }
        }

        //writing to a file
        String root = Environment.getExternalStorageDirectory().getAbsolutePath().toString();
        String finalRoot = root + "/probGrid/";
        File dir = new File (finalRoot);
        dir.mkdirs();

        try{
            File file = new File (dir, "redData" + testcount + ".txt");

            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(writing);

            myOutWriter.close();

            fOut.flush();
            fOut.close();


        } catch (Exception e){
            e.printStackTrace();
            //recordResultsText.setText("FAILED");
        }

        //THE CODE BELOW SAVES THE AVERAGE OF EACH MAC FOR A POINT IN THE DB

        ArrayList<Integer> averageRss = new ArrayList<>();

        for (int i = 0; i < avgs.size(); i++) {
            averageRss.add(avgs.get(i).intValue());
        }

        //pre filter
        detGrid[c1][c2] = new WAP();
        detGrid[c1][c2].setVector(macs, averageRss, unreliable);
//
//        //filter
        recordResultsText.append("\nFiltering\n");
        detGrid[c1][c2] = filterWAP(detGrid[c1][c2]);

    }

    public WAP filterWAP (WAP wp){
        //this function will be called if the filtering function must be implemented

        //this is the new WAP point after filtering
        WAP retWap = wp;

        for(int i=0;i<retWap.getMac().size();i++){

            // this is the filtering scheme for removing repeaters with poor signal strength
            // the other filtering (Unreliable samples) has to be done in fpdb recording functions
            String temp = retWap.getMac().get(i).substring(0,8);
            //recordResultsText.append(temp + "\n");

            if(!temp.equals("6c:f3:7f")){
                recordResultsText.append("Rejected: " + retWap.getMac().get(i) + "\n");
                retWap.getMac().remove(i);
                retWap.getRss().remove(i);
                i--;
            }

            else if(retWap.getRss().get(i) < 40){
                    //retWap.setFiltMacs(retWap.getMac().get(j));
                    recordResultsText.append("Rejected: " + retWap.getMac().get(i) + "\n");
                    retWap.getMac().remove(i);
                    retWap.getRss().remove(i);
                    i--;
            }


        }


        return retWap;
    }




    //Own added methods here for various functions

    //scanning method that scans over a certain period of time
    public void scan(){

        int time = numMin.getValue()*60*1000;


        new CountDownTimer(time, 1000) {

            int intervalCounter=0;

            public void onTick(long millisUntilFinished) {
                intervalCounter++;
                recordResultsText.setText(millisUntilFinished / 1000 + " seconds remaining");


                if(intervalCounter%60==0){
                    //write a new grid file every minute
                    int xCo = numX.getValue();
                    int yCo = numY.getValue();
                    //we need to break into two recording functions here for det/prob
                    //and two writing functions

                    //record and save the det data

//                    readDetData(context,"grid-" + intervalCounter);
//                    recordDetResults(xCo,yCo);
//                    saveDetData(context,"grid-" + intervalCounter);


                    //record and save the prob data
//                        readProbData(context,"grid-" + intervalCounter);
//                        recordProbResults(xCo,yCo);
//                        saveProbData(context,"grid-" + intervalCounter);




                }
                mainWm.startScan();
            }

            public void onFinish() {
                int xCo = numX.getValue();
                int yCo = numY.getValue();
                //we need to break into two recording functions here for det/prob
                //and two writing functions
                //record and save the det data

                readDetData(context,"grid-" + intervalCounter);
                recordDetResults(xCo,yCo);
                saveDetData(context,"grid-" + intervalCounter);


                //record and save the prob data
                readProbData(context,"grid-" + intervalCounter);
                recordProbResults(xCo,yCo);
                saveProbData(context,"grid-" + intervalCounter);
                //saveToSD(context,"grid");
                recordResultsText.append("Finished storing: " + xCo + "," + yCo + "\n");
                recordResultsText.append("deterministic:\n");
                for(int i=0;i<gridX;i++){
                    for (int j=0;j<gridY;j++){
                        if(detGrid[i][j].isValid()){
                            recordResultsText.append(i + ","+j + "\n");
                        }
                    }
                }

                recordResultsText.append("probabilistic:\n");
                for(int i=0;i<gridX;i++){
                    for (int j=0;j<gridY;j++){
                        if(probGrid[i][j].isValid()){
                            recordResultsText.append(i + ","+j + "\n");
                        }
                    }
                }
            }
        }.start();



    }

    public void recordProbResults(int xCo, int yCo){
        //after all the results have been recorded over time this method will store the data appropriately
        //both in the grid and in the grid.dat file automatically
        //new object types will be created for the final storage type as they require different data
        ArrayList<String> macList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> rssList = new ArrayList<>();


        //this populates a list of all macs and a list of their levels over time
        for (int i=0;i<resultList.size();i++){
            for (int j=0;j<resultList.get(i).getMac().size();j++) {
                int ind = macList.indexOf(resultList.get(i).getMac().get(j));

                if (ind == -1){
                    macList.add(resultList.get(i).getMac().get(j));
                    rssList.add(new ArrayList<Integer>());
                    rssList.get(rssList.size()-1).add(resultList.get(i).getRss().get(j));
                }
                else{
                    rssList.get(ind).add(resultList.get(i).getRss().get(j));
                }

            }
        }



        //this section will look at all the readings for each mac, determine if it is reliable and continue recording if so
        //if it is reliable the probability of each signal level recorded is determined and stored in the final
        // object type

        StringBuffer strBuf = new StringBuffer();

        int readingThreshold = rssList.get(0).size();

        ArrayList<ArrayList<Integer>> rssLevels = new ArrayList<>();
        ArrayList<ArrayList<Double>> rssProbs = new ArrayList<>();

        ArrayList<Integer> levels = new ArrayList<>();
        ArrayList<Integer> count = new ArrayList<>();

        for (int i=0; i<macList.size();i++){

            //filtering of unreliable macs is done here
            if((double)rssList.get(i).size() <= 0.5*(double)readingThreshold){
                macList.remove(i);
                rssList.remove(i);
                i--;
            }
            else {
                //this does a count of all the levels so it can be used for prob
                //int total = 0;
                for (int j = 0; j < rssList.get(i).size(); j++) {
                    int ind = levels.indexOf(rssList.get(i).get(j));

                    if(ind == -1){
                        levels.add(rssList.get(i).get(j));
                        count.add(1);
                    }
                    else{
                        count.set(ind,count.get(ind)+1);
                    }
                }

                //use the counters to form prob

                rssProbs.add(new ArrayList<Double>());
                rssLevels.add(new ArrayList<Integer>());
                double probTot =0;

                strBuf.append(macList.get(i) + " ");
                for (int j=0;j<levels.size();j++){

                    rssLevels.get(rssLevels.size()-1).add(levels.get(j) );
                    rssProbs.get(rssProbs.size()-1).add((double)count.get(j)/(double)rssList.get(i).size());

                    probTot = probTot + rssProbs.get(rssProbs.size()-1).get(rssProbs.get(rssProbs.size()-1).size()-1);
                    strBuf.append(rssLevels.get(rssLevels.size()-1).get(rssLevels.get(rssLevels.size()-1).size()-1) + " at " + (double)Math.round(rssProbs.get(rssProbs.size()-1).get(rssProbs.get(rssProbs.size()-1).size()-1) * 100d)/100d + "\n");
                }
                strBuf.append("Tot: " + probTot + "\n");



                levels.clear();
                count.clear();

            }
        }

        //display the results over here
        //recordResultsText.setText(strBuf);

        //now save the result at the correct coords in the grid
        location loc = new location();
        loc.setObject(macList,rssLevels,rssProbs);

        probGrid[xCo][yCo] = loc;

        //save the results to the .dat database file everytime
        //saveToSD(context,"grid");

    }


    public void saveProbData(Context mC, String fn){

        String root = Environment.getExternalStorageDirectory().getAbsolutePath().toString();
        String finalRoot = root + "/probGrid/";
        File dir = new File (finalRoot);
        dir.mkdirs();

        try{
            ArrayList<location> objWri= new ArrayList<>();
            //write grid into arraylist
            for (int i=0;i<gridX;i++){
                for(int j=0; j<gridY;j++) {
                    objWri.add(probGrid[i][j]);
                }
            }


            FileOutputStream fos = new FileOutputStream(new File(finalRoot + fn + ".dat"));



            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject((objWri));
            //recordResultsText.setText("Write Success!");
            oos.close();
            fos.close();

        } catch (Exception e){
            e.printStackTrace();
            recordResultsText.setText("FAILED");
        }
    }

    public void saveDetData(Context mC, String fn){

        String root = Environment.getExternalStorageDirectory().getAbsolutePath().toString();
        String finalRoot = root + "/detGrid/";
        File dir = new File (finalRoot);
        dir.mkdirs();

        try{
            ArrayList<WAP> objWri= new ArrayList<>();
            //write grid into arraylist
            for (int i=0;i<gridX;i++){
                for(int j=0; j<gridY;j++) {
                    objWri.add(detGrid[i][j]);
                }
            }


            FileOutputStream fos = new FileOutputStream(new File(finalRoot + fn + ".dat"));



            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject((objWri));
            //recordResultsText.setText("Write Success!");
            oos.close();
            fos.close();

        } catch (Exception e){
            e.printStackTrace();
            recordResultsText.setText("FAILED");
        }
    }


    public class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent){
            List<ScanResult> wifiList;
            wifiList = mainWm.getScanResults();
            ArrayList<String> macs = new ArrayList<>();
            ArrayList<Integer> rss = new ArrayList<>();
            ArrayList<String> unreliable = new ArrayList<>();


            StringBuffer strBuf = new StringBuffer();
            strBuf.append("Results:\n");
            for(int i=0; i < wifiList.size(); i++) {
                strBuf.append(wifiList.get(i).BSSID.toString() + " "
                        + WifiManager.calculateSignalLevel(wifiList.get(i).level,100) + "\n");

                // this section is for data recording and testing purposes
                macs.add(wifiList.get(i).BSSID);
                rss.add(WifiManager.calculateSignalLevel(wifiList.get(i).level,100));

            }


                RecordResult rr = new RecordResult();
                rr.setResult(macs, rss);
                resultList.add(rr);

                WAP temp = new WAP();
                temp.setVector(macs,rss,unreliable);
                waps.add(temp);








        }
    }

    public static class RecordResult implements Serializable {
        ArrayList<String> mac;//list of mac addresses
        ArrayList<Integer> rss;

        public ArrayList<String> getMac(){
            return mac;
        }

        public ArrayList<Integer> getRss(){
            return rss;
        }

        public void setResult(ArrayList<String> m, ArrayList<Integer> r){
            this.mac = m;
            this.rss = r;
        }

    }



}
