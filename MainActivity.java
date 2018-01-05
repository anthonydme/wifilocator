package com.example.user.wifiprob;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener{

    public static final String EXTRA_MESSAGE = "com.example.WifiProb.MESSAGE";

    //my variables here
    //locate me button
    Button locate;
    Button wrongLoc;
    //TextView locatetext;
    TextView debug;

    //wifi stuff
    WifiManager mainWm;
    MainActivity.WifiReceiver recWifi;
    Context context;

    //pedometer stuff
    private SensorManager sensorManager;
    boolean activityRunning;
    int pedoCount=0;
    int currentCount=0;
    int stepsTaken=0;




    //main grids
    int gridX = 10;
    int gridY = 10;

    //FINAL LOCATION
    int finalX=-1;
    int finalY=-1;
    int backUpX = -1;
    int backUpY = -1;
    int backUpSteps = 0;



    //probGrid
    location [][] probGrid = new location[gridX][gridY];
    double [][] probsStorage = new double [gridX][gridY];
    ArrayList<ArrayList<Integer>> firstScans = new ArrayList<>();
    ArrayList<Double> firstScansProb = new ArrayList<>();
    int firstCount=0;

    //detGrid
    WAP [][] detGrid = new WAP [gridX][gridY];

    //request location
    int request = 0;
    int numScans = 1;//this variable determines how many scans take place at the loc req. before performing the localization
    ArrayList<WAP>  reqRes = new ArrayList<>();
    double[][] cosineGrid = new double [gridX][gridY];//this holds the cosine similarity index for each element when requested
    double [][] distanceGrid= new double [gridX][gridY];//this matrix holds the distance values calculated from the cosine sim
    WAP rssVec; //this is the object for the given location


    boolean gridPresent = false;
    int prevX=-1;
    int prevY=-1;

    //settings
    boolean detSystem;
    boolean probSystem;
    boolean knn;
    boolean wknn;



    //grid and storage for pedo data
    pedoLocation [][] pedoGrid = new pedoLocation[gridX][gridY];
    ArrayList<ArrayList<Integer>> finalPedoList = new ArrayList<>();
    ArrayList<ArrayList<Integer>> checkedList = new ArrayList<>();

    int rescanX=-1;
    int rescanY=-1;

    //for the environment probability
    location globalLoc = new location();
    int validCounter=0;
    boolean requested = false;

    //shared prferences for storages

    SharedPreferences.Editor editor;

    NumberPicker hVal;

    //tv for testing
    TextView results;

    ArrayList<ArrayList<Integer>> resList = new ArrayList<>();

    //for drawing
    ImageView floor;

    BitmapFactory.Options myOptions;


    Bitmap bitmap;
    Paint paint;



    Bitmap workingBitmap;
    Bitmap mutableBitmap;
    Canvas canvas;



    private static final String TAG = "MainActivity";
    int PERM_REQ;

    public MainActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final SharedPreferences sharedPref;
        sharedPref = this.getSharedPreferences("Settings",Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        //tv for testing
       // results = (TextView) findViewById(R.id.results);
        floor = (ImageView) findViewById(R.id.imageView);

        myOptions = new BitmapFactory.Options();
        myOptions.inScaled = false;
        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.floorplanengfinal,myOptions);
        workingBitmap = Bitmap.createBitmap(bitmap);

        paint = new Paint();
        mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);


        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        canvas = new Canvas(mutableBitmap);
        //canvas.drawCircle(250, 424, 20, paint);



        //canvas.drawCircle(105, 285, 10, paint);jeffs room
        //canvas.drawCircle(65, 140, 10, paint);lounge
        //canvas.drawCircle(210, 330, 10, paint);my room perfect
        //canvas.drawCircle(65, 230, 20, paint);//entrance hall
        //canvas.drawCircle(80, 50, 10, paint);balcony
        //canvas.drawCircle(200,50, 10, paint);moms room
        //canvas.drawCircle(280,100, 10, paint);moms bathroom
        //canvas.drawCircle(200, 170, 10, paint);kitchen
        //canvas.drawCircle(230, 240, 10, paint);bathroom
        //canvas.drawCircle(180, 240, 10, paint);hall

//        canvas.drawCircle(250, 85, 20, paint);
//        canvas.drawCircle(363, 85, 20, paint);
//        canvas.drawCircle(476, 85, 20, paint);
//        canvas.drawCircle(589, 85, 20, paint);
//        canvas.drawCircle(250, 198, 20, paint);
//        canvas.drawCircle(363, 198, 20, paint);
//        canvas.drawCircle(476, 198, 20, paint);
//        canvas.drawCircle(589, 198, 20, paint);
//        canvas.drawCircle(250, 311, 20, paint);
//        canvas.drawCircle(363, 311, 20, paint);
//        canvas.drawCircle(476, 311, 20, paint);
//        canvas.drawCircle(589, 311, 20, paint);
//        canvas.drawCircle(250, 424, 20, paint);
//        canvas.drawCircle(363, 424, 20, paint);
//        canvas.drawCircle(476, 424, 20, paint);
//        canvas.drawCircle(589, 424, 20, paint);
//        canvas.drawCircle(250, 537, 20, paint);
//        canvas.drawCircle(363, 537, 20, paint);
//        canvas.drawCircle(476, 537, 20, paint);
//        canvas.drawCircle(589, 537, 20, paint);
//        canvas.drawCircle(115, 198, 20, paint);
//        canvas.drawCircle(115, 311, 20, paint);
//        canvas.drawCircle(115, 424, 20, paint);
//        canvas.drawCircle(115, 537, 20, paint);
//        canvas.drawCircle(115, 650, 20, paint);
//        canvas.drawCircle(250, 650, 20, paint);
//        canvas.drawCircle(363, 650, 20, paint);
//        canvas.drawCircle(476, 650, 20, paint);
//        canvas.drawCircle(589, 650, 20, paint);
//        canvas.drawCircle(720, 650, 20, paint);
//        canvas.drawCircle(850, 650, 20, paint);
//        canvas.drawCircle(970, 650, 20, paint);
//        canvas.drawCircle(740, 170, 20, paint);
//        canvas.drawCircle(860, 170, 20, paint);
//        canvas.drawCircle(980, 170, 20, paint);
//        canvas.drawCircle(740, 283, 20, paint);
//        canvas.drawCircle(860, 283, 20, paint);
//        canvas.drawCircle(980, 283, 20, paint);
//        canvas.drawCircle(740, 396, 20, paint);
//        canvas.drawCircle(860, 396, 20, paint);
//        canvas.drawCircle(980, 396, 20, paint);
//        canvas.drawCircle(740, 509, 20, paint);
//        canvas.drawCircle(860, 509, 20, paint);
//        canvas.drawCircle(980, 509, 20, paint);







        floor.setAdjustViewBounds(true);
        floor.setImageBitmap(mutableBitmap);




        //this is all menu code
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //my own code goes here
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
        //initialise button and TV

        debug = (TextView) findViewById(R.id.debug);

        locate = (Button) findViewById(R.id.locate);

       // wrongLoc = (Button) findViewById(R.id.wrongLoc);

        //hVal = (NumberPicker) findViewById(R.id.h);

        //hVal.setMinValue(1);
        //hVal.setMaxValue(50);



        locate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debug.setText("");
                //results.setText("");
               // locatetext.setText("");
                //this starts the localising process
                //first do the wifi scan, then analyse the amount of steps taken since the last measurement
                //if(probSystem) {
                    //locatetext.setText("Locating using Probabilistic methods...");

                //for(int i=0;i<5;i++) {
                    requested = true;

                    mainWm.startScan();
                //}
                //}
               // else if(detSystem){
                    //locatetext.setText("Locating using Deterministic methods...");
                    //requested=true;
                    request = 0;
                    //mainWm.startScan();

                //}
                //locatetext.setText("You've taken: " + stepsTaken + " since the last scan\n");

            }
        });

        //locatetext = (TextView) findViewById(R.id.locText);


        //initialise pedometer stuff
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        currentCount = pedoCount;



        //initialise the wifi stuff
        //getting wifimanager instance
        mainWm = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        context = getApplicationContext();


        recWifi = new MainActivity.WifiReceiver();
        registerReceiver(recWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));



        //check the settings from last time here
        int sys = sharedPref.getInt("sys",0);

        int sysSet = sharedPref.getInt("sysSet",0);

        detSystem=false;
        probSystem=false;
        knn=false;
        wknn=false;

        if(sys==0){
            detSystem=true;
        }
        else{
            probSystem=true;
        }

        if(sysSet==0){
            knn=true;
        }
        else{
            wknn=true;
        }



        //read from the SD into the grid on startup, provided there is a grid available
        //read in the correct grid based on whether det or prob system is used
        //need two different reading functions to read correct objects



        //readFromSD(context,"grid");
        if(probSystem) {

            readProbData(context,"grid");

            if(gridPresent) {
                environmentProbabilities();

                readProbData(context, "grid");

            }

       if(knn){
                setTitle("Prob-Sys: No Pedometer");
            }
            else{
                setTitle("Prob-Sys: Pedometer on");
            }

            //readFromSD(context, "grid");
        }
        else if(detSystem){
            if(knn) {
                setTitle("Det-Sys: KNN used");
            }
            else{
                setTitle("Det-Sys: WKNN used");
            }
        }

        for(int i=0;i<gridX;i++){
            for(int j=0;j<gridY;j++){
                detGrid[i][j] = new WAP();
            }
        }
        readDetData(context,"grid");



        //read in the pedometer data into a grid if it is required
       // if(wknn) {
            //initiate the PEDOMETER grid
        addPedometerData();

            for(int i=0;i<gridX;i++){
                for(int j=0;j<gridY;j++){
                    pedoGrid[i][j] = new pedoLocation();
                }
            }
            readPedoData(context, "pedoGrid");
       // }




        debug.setText("Localization ready!\n");


    }

    //*********************************************************************************************
    //METHODS START HERE
    //*********************************************************************************************




    //PEDOMTER FUNCTIONS


    public void drawCircle(int [] coords){



        myOptions = new BitmapFactory.Options();
        myOptions.inScaled = false;
        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.floorplanengfinal,myOptions);
        workingBitmap = Bitmap.createBitmap(bitmap);

        paint = new Paint();
        mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);


        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        canvas = new Canvas(mutableBitmap);

        if(coords[0] < 5 && coords[1] > 0 && coords[1] < 5){
            //Netlabs
            canvas.drawCircle((250+((coords[1]-1)*113)), (85+(coords[0]*113)), 20, paint);
                debug.append("NETLAB A\n");
        }
        else if(coords[0] > 0 && coords[0] < 5 && coords[1] > 4 ){
            //Project Labs
            canvas.drawCircle((740+((coords[1]-5)*120)), (170+((coords[0]-1)*110)), 20, paint);
            debug.append("PROJECT LAB 2\n");
        }
        else if(coords[1] < 1 ){
            //left corridor
            canvas.drawCircle(115, (198+(coords[0]-1)*113), 20, paint);
            debug.append("CORRIDOR\n");
        }
        else if(coords[0]>4 && coords[1] > 0 && coords[1] < 5){
            canvas.drawCircle((250+((coords[1]-1)*113)), 650, 20, paint);
            debug.append("CORRIDOR\n");
        }
        else if(coords[0]==7 && coords[1]==7){
            canvas.drawCircle(720, 590, 20, paint);
            debug.append("PROJECT LAB ENTRANCE\n");
        }
        else if(coords[1]>4 && coords[0] > 4){
            canvas.drawCircle((720+((coords[1]-5)*120)), 650, 20, paint);
            debug.append("CORRIDOR\n");
        }

        floor.setAdjustViewBounds(true);
        floor.setImageBitmap(mutableBitmap);

//        canvas.drawCircle(250, 85, 20, paint);
//        canvas.drawCircle(363, 85, 20, paint);
//        canvas.drawCircle(476, 85, 20, paint);
//        canvas.drawCircle(589, 85, 20, paint);
//
//        canvas.drawCircle(250, 198, 20, paint);
//        canvas.drawCircle(363, 198, 20, paint);
//        canvas.drawCircle(476, 198, 20, paint);
//        canvas.drawCircle(589, 198, 20, paint);
//
//        canvas.drawCircle(250, 311, 20, paint);
//        canvas.drawCircle(363, 311, 20, paint);
//        canvas.drawCircle(476, 311, 20, paint);
//        canvas.drawCircle(589, 311, 20, paint);
//
//        canvas.drawCircle(250, 424, 20, paint);
//        canvas.drawCircle(363, 424, 20, paint);
//        canvas.drawCircle(476, 424, 20, paint);
//        canvas.drawCircle(589, 424, 20, paint);
//
//        canvas.drawCircle(250, 537, 20, paint);
//        canvas.drawCircle(363, 537, 20, paint);
//        canvas.drawCircle(476, 537, 20, paint);
//        canvas.drawCircle(589, 537, 20, paint);

//        canvas.drawCircle(115, 198, 20, paint);
//        canvas.drawCircle(115, 311, 20, paint);
//        canvas.drawCircle(115, 424, 20, paint);
//        canvas.drawCircle(115, 537, 20, paint);

//        canvas.drawCircle(115, 650, 20, paint);
//        canvas.drawCircle(250, 650, 20, paint);
//        canvas.drawCircle(363, 650, 20, paint);
//        canvas.drawCircle(476, 650, 20, paint);
//        canvas.drawCircle(589, 650, 20, paint);
//        canvas.drawCircle(720, 650, 20, paint);
//        canvas.drawCircle(850, 650, 20, paint);
//        canvas.drawCircle(970, 650, 20, paint);

//        canvas.drawCircle(740, 170, 20, paint);
//        canvas.drawCircle(860, 170, 20, paint);
//        canvas.drawCircle(980, 170, 20, paint);
//
//        canvas.drawCircle(740, 283, 20, paint);
//        canvas.drawCircle(860, 283, 20, paint);
//        canvas.drawCircle(980, 283, 20, paint);
//
//        canvas.drawCircle(740, 396, 20, paint);
//        canvas.drawCircle(860, 396, 20, paint);
//        canvas.drawCircle(980, 396, 20, paint);
//
//        canvas.drawCircle(740, 509, 20, paint);
//        canvas.drawCircle(860, 509, 20, paint);
//        canvas.drawCircle(980, 509, 20, paint);
    }




    @Override
    protected void onResume() {
        super.onResume();
        activityRunning = true;

        registerReceiver(recWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);

        } else {
            Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "activity resumed");

        Intent intent = getIntent();
        detSystem = intent.getBooleanExtra("det",true);
        probSystem = intent.getBooleanExtra("prob",false);
        knn = intent.getBooleanExtra("knn",true);
        wknn = intent.getBooleanExtra("wknn",false);

        if(probSystem) {

            readProbData(context,"grid");

            if(gridPresent) {
                environmentProbabilities();

                readProbData(context, "grid");
            }

            editor.putInt("sys", 1);
            editor.apply();

            if(knn){
                setTitle("Prob-Sys: No Pedometer");
                editor.putInt("sysSet", 0);
                editor.apply();
            }
            else{
                setTitle("Prob-Sys: Pedometer on");
                editor.putInt("sysSet", 1);
                editor.apply();
            }

            //readFromSD(context, "grid");
        }
        else if(detSystem){
            readDetData(context,"grid");

            editor.putInt("sys", 0);
            editor.apply();

            if(knn) {
                setTitle("Det-Sys: KNN used");
                editor.putInt("sysSet", 0);
                editor.apply();
            }
            else{
                setTitle("Det-Sys: WKNN used");
                editor.putInt("sysSet", 1);
                editor.apply();
            }
        }

    }

    @Override
    protected void onStop(){
        super.onStop();
        if(probSystem) {

            editor.putInt("sys", 1);
            editor.apply();

            if(knn){
                editor.putInt("sysSet", 0);
                editor.apply();
            }
            else{
                editor.putInt("sysSet", 1);
                editor.apply();
            }

            //readFromSD(context, "grid");
        }
        else if(detSystem){

            editor.putInt("sys", 0);
            editor.apply();

            if(knn) {
                editor.putInt("sysSet", 0);
                editor.apply();
            }
            else{
                editor.putInt("sysSet", 1);
                editor.apply();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityRunning = false;

        if(probSystem) {

            editor.putInt("sys", 1);
            editor.apply();

            if(knn){
                editor.putInt("sysSet", 0);
                editor.apply();
            }
            else{
                editor.putInt("sysSet", 1);
                editor.apply();
            }

            //readFromSD(context, "grid");
        }
        else if(detSystem){

            editor.putInt("sys", 0);
            editor.apply();

            if(knn) {
                editor.putInt("sysSet", 0);
                editor.apply();
            }
            else{
                editor.putInt("sysSet", 1);
                editor.apply();
            }
        }


        // if you unregister the last listener, the hardware will stop detecting step events
//        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (activityRunning) {
            //this method os accessed very time ONE step is taken
            //so we will keep counting the amount of steps taken until the next scan is requested
            pedoCount = (int)event.values[0];
            //if(prevY!=-1) {
                stepsTaken++;
                //locatetext.setText("Steps:" + stepsTaken);
            //}
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }






    public void saveToSD(Context mC, String fn){

        String root = Environment.getExternalStorageDirectory().getAbsolutePath().toString();
        String finalRoot = root + "/probGrid/temp/";
        File dir = new File (finalRoot);
        dir.mkdirs();

        try{
            ArrayList<location> objWri= new ArrayList<>();
            //write grid into arraylist
            for (int i=0;i<gridX;i++){
                for(int j=0; j<gridY;j++) {

                        //objWri.add(grid[i][j]);

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
            //locatetext.setText("FAILED");
        }
    }

    public void readPedoData(Context c,String fn){
        //this function is just to append two grid files together to form one grid result
        try{
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            String finalRoot = root + "/probGrid/";

            FileInputStream fis = new FileInputStream(new File(finalRoot + fn + ".dat"));


            ObjectInputStream ois = new ObjectInputStream(fis);

            //fetch arraylist from file
            ArrayList<pedoLocation> tempIn;

            tempIn = (ArrayList<pedoLocation>) ois.readObject();
            int index = 0;
            StringBuffer stringBuffer = new StringBuffer();

            // read tempIn into grid now
            for (int i=0;i<gridX;i++){
                for(int j=0; j<gridY;j++) {

                    if(index<tempIn.size()) {

                            pedoGrid[i][j] = tempIn.get(index);


                        index++;
                    }
                }
            }

            //some testing to see its all working


            //locatetext.setText("Read Success!");
            //locatetext.setText(stringBuffer);
            //gridPresent = true;
            ois.close();
            fis.close();


        } catch (Exception e){
            e.printStackTrace();
            gridPresent=false;
            //locatetext.setText("FAILED READ");
        }
    }


    public void readProbData(Context mC, String fn){
        try{
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            String finalRoot = root + "/probGrid/";

            FileInputStream fis = new FileInputStream(new File(finalRoot + fn + ".dat"));


            ObjectInputStream ois = new ObjectInputStream(fis);

            //fetch arraylist from file
            ArrayList<location> tempIn;

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


            //locatetext.setText("Read Success!");
            //locatetext.setText(stringBuffer);
            gridPresent = true;
            ois.close();
            fis.close();


        } catch (Exception e){
            e.printStackTrace();
            gridPresent=false;
            //locatetext.setText("FAILED READ");
        }
    }

    public void readDetData(Context mC, String fn){
        try{
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            String finalRoot = root + "/detGrid/";

            FileInputStream fis = new FileInputStream(new File(finalRoot + fn + ".dat"));


            ObjectInputStream ois = new ObjectInputStream(fis);

            //fetch arraylist from file
            ArrayList<WAP> tempIn;

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


            //locatetext.setText("Read Success!");
            //locatetext.setText(stringBuffer);
            gridPresent = true;
            ois.close();
            fis.close();


        } catch (Exception e){
            e.printStackTrace();
            gridPresent=false;
            //locatetext.setText("FAILED READ");
        }
    }

    public void reqSignal(){

        List<ScanResult> wifiList;
        wifiList = mainWm.getScanResults();
        ArrayList<String> macRes = new ArrayList<String>();
        ArrayList<Integer> rssRes = new ArrayList<Integer>();
        ArrayList<String> unreliable = new ArrayList<>();

        StringBuffer strbuf = new StringBuffer();

        for(int i=0; i < wifiList.size(); i++) {

            macRes.add(wifiList.get(i).BSSID);
            rssRes.add(WifiManager.calculateSignalLevel(wifiList.get(i).level,100));


        }

        // this is the object at the recording location
        rssVec = new WAP();
        rssVec.setVector(macRes,rssRes,unreliable);


        //perform filtering on the requested position signals
        rssVec = filterWAP(rssVec);

        //for close quaters testing
        //test[testCount] = rssVec;
        //scanRes.setText("Saved " + String.valueOf(request));
        reqRes.add(rssVec);

       // results.append("Deterinistic:\n");

        long start = System.nanoTime();

        if(request==(numScans-1)){
            //WAP altered = findUnreliable();
            WAP altered = rssVec;
            reqRes.clear();
            if(altered.getMac().size() >0) {
                //debug.append("Cosine Altered Method Running...\n");
                cosineAltered(altered);

            }
            requested=false;
            //request=0;
        }
        long end = System.nanoTime();
        //debug.append("" + (end - start)/1000000 + "\n");

    }

    public void cosineAltered(WAP res){
        ArrayList<String> reqMacs = new ArrayList<>();
        ArrayList<Integer> reqRss = new ArrayList<>();
        ArrayList<String> dbMacs = new ArrayList<>();
        ArrayList<Integer> dbRss = new ArrayList<>();

        StringBuffer stringTemp = new StringBuffer();


        //only have to populate the reqMacs and reqRss once per request


        for(int i=0;i<gridX;i++){
            for (int j=0;j<gridY;j++){

                //populate the req obj portion from the scan results
                //clear all the array lists used in this loop section
                reqMacs.clear();
                reqRss.clear();
                dbMacs.clear();
                dbRss.clear();


                for (int u=0;u<res.getMac().size();u++){
                    reqMacs.add(res.getMac().get(u));
                    reqRss.add(res.getRss().get(u));

                    dbMacs.add(res.getMac().get(u));
                    dbRss.add(0);
                }

                WAP temp = detGrid[i][j];
                //Log.d(TAG,"req: " + reqRss.size() + " actual: " + res.getRss().length + " db: " + dbRss.size() + " actual: " + temp.getRss().length);
                //This if statement checks the point in the db weve pulled out is a valid stored point and then calculates the
                // similarity bewteen the two points and stores it in a corresponding similarity grid
                //otherwise we store a 0 in the grid
                if(temp.isValid()) {
                    for (int k = 0; k < temp.getMac().size(); k++) {
                        int ind = dbMacs.indexOf(temp.getMac().get(k));

                        if (ind != -1) {
                            //dbMacs.add(ind,temp.getMac()[k]);
                            //Log.d(TAG,"before: " + dbRss.size());
                            dbRss.set(ind, temp.getRss().get(k));
                            //Log.d(TAG,"after: " + dbRss.size());
                            //Log.d(TAG,"here");
                        } else {
                            reqMacs.add(temp.getMac().get(k));
                            reqRss.add(0);

                            dbMacs.add(temp.getMac().get(k));
                            dbRss.add(temp.getRss().get(k));
                        }
                    }

                    //check lengths here
                    //Log.d(TAG,"req: " + reqRss.size() + " actual: " + res.getRss().length + " db: " + dbRss.size() + " actual: " + temp.getRss().length);

                    //every time this runs we need to check and store the cosine sim metric correspoding to this comparison
                    //we also need to clear both arrayLists

                    Integer[] finalRss1 = reqRss.toArray(new Integer[reqRss.size()]);
                    int[] primRss = new int[reqRss.size()];

                    for (int c = 0; c < finalRss1.length; c++) {
                        primRss[c] = finalRss1[c];
                    }

                    Integer[] finalRss2 = dbRss.toArray(new Integer[dbRss.size()]);
                    int[] primRss2 = new int[dbRss.size()];

                    for (int c = 0; c < finalRss2.length; c++) {
                        primRss2[c] = finalRss2[c];
                    }

                    double sim = cosineVectors(primRss2, primRss);

                    cosineGrid[i][j] = sim;

                    stringTemp.append("Coord at " + i + "," + j + ": " + sim + "\n");

                }
                else{
                    cosineGrid[i][j] =0;
                }


                //Log.d(TAG,sim + "\n");

//                Log.d(TAG,"Coords: " + i + " " + j);
//                for(int y=0;y<reqRss.size();y++){
//                    Log.d(TAG,reqRss.get(y) + " " + dbRss.get(y) + "\n");
//                }
//                Log.d(TAG,"\n\n");
            }
        }



        distanceMetrics();


        //testing
        if(knn){
            int [] coords = knn();
            //coords = knn();

            drawCircle(coords);
        }
        else if(wknn){
            int [] coords = standardWknn();
            //c/oords = ();

            drawCircle(coords);
        }

    }

    public int[] knn(){
        int[] coords = new int[2];

        int k = 3; //this is the main k value that decides how many nn's we look at afterr ranking
        int [][] nn = new int [2][k];//nn vector
        double [] distances = new double[k];//distances vector for checking and sorting
        //fill the smallest distances array with 1 values first
        for(int i=0;i<k;i++){
            distances[i] = 1;
            nn[0][i]=9;
            nn[1][i]=9;
        }


        // we need another distances matrix to read and manipulate values
        double [][] tempDist = distanceGrid;

        for (int i=0;i<gridX;i++){
            for (int j=0;j<gridY;j++){
                for(int p=0;p<k;p++){
                    if (tempDist[i][j] < distances[p]){
                        //Log.d(TAG,tempDist[i][j] + "<" + distances[p] + "\n");

                        double [] temp = new double [k-p];
                        int [][] tempCoords = new int [2][k-p];
                        int tempIndex=0;

                        for(int y=p;y<k;y++) {
                            temp[tempIndex] = distances[y];
                            tempCoords[0][tempIndex] = nn[0][y];
                            tempCoords[1][tempIndex] = nn[1][y];
                            tempIndex++;
                        }
                        tempIndex=0;

                        distances[p] = tempDist[i][j];
                        nn[0][p] = i;
                        nn[1][p] = j;

                        for(int y=p+1;y<k;y++){
                            distances[y] = temp[tempIndex];
                            nn[0][y] = tempCoords[0][tempIndex];
                            nn[1][y] = tempCoords[1][tempIndex];
                            tempIndex++;
                        }



                        break;
                    }
                }

            }
        }

        double avgX=0;
        double avgY=0;
        double probDivisor = 0;

        for (int i=0;i<k;i++){
            avgX = avgX + nn[0][i];
            avgY = avgY + nn[1][i];

        }

        avgX = avgX/(double)k;
        avgY = avgY/(double)k;

        //debug.append("Estimated sim coords: " + nn[0][0] + "," + nn[1][0] + "\n");

        coords [0] = (int) Math.round(avgX);
        coords [1] = (int) Math.round(avgY);
        return coords;


    }

    public int[] alteredWknn(){
        int [] coords = new int[2];

        //FIRST WE EMPLOY NORMAL KNN TO FIND K NNs

        int k = 3; //this is the main k value that decides how many nn's we look at afterr ranking
        int [][] nn = new int [2][k];//nn vector
        double [] distances = new double[k];//distances vector for checking and sorting
        //fill the smallest distances array with 9 values first
        for(int i=0;i<k;i++){
            distances[i] = 1;
            nn[0][i]=9;
            nn[1][i]=9;
        }


        // we need another distances matrix to read and manipulate values
        double [][] tempDist = distanceGrid;

        for (int i=0;i<gridX;i++){
            for (int j=0;j<gridY;j++){
                for(int p=0;p<k;p++){
                    if (tempDist[i][j] < distances[p]){
                        //Log.d(TAG,tempDist[i][j] + "<" + distances[p] + "\n");

                        double [] temp = new double [k-p];
                        int [][] tempCoords = new int [2][k-p];
                        int tempIndex=0;

                        for(int y=p;y<k;y++) {
                            temp[tempIndex] = distances[y];
                            tempCoords[0][tempIndex] = nn[0][y];
                            tempCoords[1][tempIndex] = nn[1][y];
                            tempIndex++;
                        }
                        tempIndex=0;

                        distances[p] = tempDist[i][j];
                        nn[0][p] = i;
                        nn[1][p] = j;

                        for(int y=p+1;y<k;y++){
                            distances[y] = temp[tempIndex];
                            nn[0][y] = tempCoords[0][tempIndex];
                            nn[1][y] = tempCoords[1][tempIndex];
                            tempIndex++;
                        }



                        break;
                    }
                }

            }
        }

        double avgX=0;
        double avgY=0;
        double probDivisor = 0;

        for (int i=0;i<k;i++){
            avgX = avgX + nn[0][i] * probsStorage[nn[0][i]][nn[1][i]];
            avgY = avgY + nn[1][i]* probsStorage[nn[0][i]][nn[1][i]];
            probDivisor = probDivisor + probsStorage[nn[0][i]][nn[1][i]];

        }

        avgX = avgX/(double)probDivisor;
        avgY = avgY/(double)probDivisor;

        coords[0] = (int) Math.round(avgX);
        coords[1] = (int) Math.round(avgY);

        return coords;
    }

    public int[] standardWknn(){
        int[] coords = new int[2];

        int k = 4+1; //this is the main k value, we need the extra +1 for a standardization calc
        int [][] nn = new int [2][k];//nn vector
        double [] distances = new double[k];//distances vector for checking and sorting
        int start =0; // need this to know where to start searching in the grid after pulling in the first distances
        for (int p=0;p<k;p++){
            //fill the first lot of distances from the fist row
            if(distanceGrid[0].length-1 > p) {
                distances[p] = distanceGrid[0][p];
                nn[0][p] = 0;
                nn[1][p] = p;
            }
            else{
                distances[p] = distanceGrid[1][distanceGrid[0].length-p];
                start = p-distanceGrid[0].length+1;
                nn[0][p] = 1;
                nn[1][p] = start-1;
            }
        }
        //rank all the distances
        for (int i=1;i<gridX;i++){
            for(int j=start;j<gridY;j++){
                for(int x=0;x<k;x++){
                    if(distanceGrid[i][j] < distances[x]){
                        //if the distance at the current grid point is smaller than any of the current nn's
                        //replace it, store it in teh distances array and store the coordinates of the grid
                        distances[x] = distanceGrid[i][j];
                        nn[0][x] = i;
                        nn[1][x] = j;
                        x=k;
                    }
                }
            }
        }

        //standardize the rest of the distances in the following way
        for (int i=0;i<k-1;i++){
            distances[i] = distances[i]/distances[k-1];
        }

        //going to try the gauss kernel as it seems most successful
        double [] weights = new double[k-1];
        double neighbourWeight =0;
        for(int i=0;i<k-1;i++){
            //kernel used for weights
            weights[i] = (1/(Math.sqrt(2*Math.PI)))*Math.exp(-(Math.pow(distances[i],2))/2);
            //we use the neighbour with the heighest weight to classify the testing result
            if(weights[i]>neighbourWeight){
                neighbourWeight = weights[i];
                coords[0] = nn[0][i];
                coords[1] = nn[1][i];
            }
        }





        return coords;
    }

    public void distanceMetrics(){
        // im going to try using sqrt(1-s) to begin with
        // others are: 1-s; -log(s); (1/s)-1

        StringBuffer strBuf = new StringBuffer();//for testing

        for (int i=0; i < gridX;i++){
            for(int j=0; j<gridY;j++){
                //we can change this conversion below if need be
                //if there is a 0 similarity in the grid this should still provide real results
                distanceGrid[i][j] = Math.sqrt(1-cosineGrid[i][j]);
                if(detGrid[i][j].isValid()) {
                    debug.append("Pos: " + i + "," + j + ": " + distanceGrid[i][j] + "\n");
                }
                //results.append(i + "," + j + ": " + distanceGrid[i][j] + "\n");
            }
        }

        //scanRes.setText(strBuf.toString());

    }

    public double cosineVectors(int[] tVec, int[] rVec){
        double dotProd=0.0;
        double nA=0.0;
        double nB=0.0;

        for(int i=0;i<tVec.length;i++){
            dotProd = dotProd + tVec[i]*rVec[i];
            nA = nA + Math.pow(tVec[i],2);
            nB = nB + Math.pow(rVec[i],2);
        }
        double ans = dotProd/(Math.sqrt(nA)*Math.sqrt(nB));
        return ans;
    }

    public WAP filterWAP (WAP wp){
        //this function will be called if the filtering function must be implemented

        //this is the new WAP point after filtering
        WAP retWap = wp;

        for(int i=0;i<retWap.getMac().size();i++){

            // this is the filtering scheme for removing repeaters with poor signal strength
            // the other filtering (Unreliable samples) has to be done in fpdb recording functions
            String temp = retWap.getMac().get(i).substring(0,7);

            for (int j=i;j<retWap.getMac().size();j++){
                if(retWap.getMac().get(j).contains(temp) && retWap.getRss().get(j) < 20){
                    //retWap.setFiltMacs(retWap.getMac().get(j));
                    retWap.getMac().remove(j);
                    retWap.getRss().remove(j);

                    j--;

                }
            }
        }


        return retWap;
    }

    public WAP findUnreliable(){

        //this function takes the input of the numScans performed at the user's loc
        //it decides which scan has the largest amount of WAPs
        //then finds all the WAPs common between all the scans and adds them (with corresp. rss lev) to the list for the final obj
        // all the macs that arent common are seen as unreliable (added to unreliable list for the object)
        Log.d(TAG, "Finding unreliable WAPs...\n");
        Log.d(TAG,"Amount of scans: " + reqRes.size() + "\n");
        WAP newPoint = new WAP();
        ArrayList<String> macRes = new ArrayList<String>();
        ArrayList<Integer> rssRes = new ArrayList<Integer>();
        ArrayList<String> unreliable = new ArrayList<>();

        //use the largest list to run through and check all the macs against the other scans
        for (int j=0;j<reqRes.size();j++){
            Log.d(TAG,"The list size: " + reqRes.get(j).getMac().size() +"\n");

            for (int i=0;i<reqRes.get(j).getMac().size();i++){
                int ind = macRes.indexOf(reqRes.get(j).getMac().get(i));

                if(j==0 && ind == -1){
                    macRes.add(reqRes.get(j).getMac().get(i));
                    rssRes.add(reqRes.get(j).getRss().get(i));
                }
                else if(ind == -1){
                    unreliable.add(reqRes.get(j).getMac().get(i));
                    Log.d(TAG, "Unreliable WAP found: " + reqRes.get(j).getMac().get(i) + "\n");
                }
                else{
                    rssRes.set(ind,(rssRes.get(ind)+reqRes.get(j).getRss().get(i))/2);
                }
            }
        }

        //cleanup the list by checking if there were any unreliable WAPs in the first list created
        for (int i=0;i<macRes.size();i++){
            int ind = reqRes.get(1).getMac().indexOf(macRes.get(i));

            if (ind == -1){
                unreliable.add(macRes.get(i));

                Log.d(TAG, "Unreliable WAP found: " + macRes.get(i) + "\n");
                macRes.remove(i);
                rssRes.remove(i);
                i--;
            }
        }


        newPoint.setVector(macRes,rssRes,unreliable);

        return newPoint;
    }


    //calculating level probabailities per mac for whole environment
    public void environmentProbabilities(){

        //NEED TO FIX THIS METHOD BEFORE USE

        ArrayList<String> enviroMacs = new ArrayList<>();
        ArrayList<ArrayList<Double>> enviroProbs = new ArrayList<>();
        ArrayList<ArrayList<Integer>> enviroRss = new ArrayList<>();

        for(int i=0;i<gridX;i++){
            for(int j=0;j<gridY;j++){
                //check each coordinate is valid first
                //then check all the macs at that point, add any new ones to the global storage
                // if theyre already in then use the probability
                StringBuffer strbuf = new StringBuffer();


                if(probGrid[i][j].isValid()){
                    //validCounter++;
                    for (int k=0;k<probGrid[i][j].getMacs().size();k++) {
                        int ind = enviroMacs.indexOf(probGrid[i][j].getMacs().get(k));

                        if(ind==-1){
                            enviroMacs.add(probGrid[i][j].getMacs().get(k));
                            enviroRss.add(probGrid[i][j].getRss().get(k));
                            enviroProbs.add(probGrid[i][j].getProbs().get(k));
                        }
                        else{
                            //combining probabilities, if the same level then add and divide by 2
                            // otherwise divide by 2
                            for (int v=0;v<probGrid[i][j].getRss().get(k).size();v++){
                                int ind2 = enviroRss.get(ind).indexOf(probGrid[i][j].getRss().get(k).get(v));

                                if (ind2 == -1){
                                    //also divide all other probabilities by 2
                                    for (int y=0;y<enviroProbs.get(ind).size();y++){
                                        enviroProbs.get(ind).set(y,enviroProbs.get(ind).get(y));///2
                                    }

                                    //the signal levels are not the same, therefore add the new level and its
                                    // probability divided by 2
                                    enviroRss.get(ind).add(probGrid[i][j].getRss().get(k).get(v));
                                    enviroProbs.get(ind).add(probGrid[i][j].getProbs().get(k).get(v));///2

                                }
                                else{
                                    //the signal level does match combine the two probabilities
                                    enviroProbs.get(ind).set(ind2,(enviroProbs.get(ind).get(ind2)+probGrid[i][j].getProbs().get(k).get(v)));///2
                                }


                            }

                        }


                    }
                }
            }
        }


        globalLoc.setObject(enviroMacs,enviroRss,enviroProbs);

    }

    public void revisedProbabilityCalculation(int x, int y, location loc, recording.RecordResult res){
        //going to perform Bayes estimation of the distribution of the RPs signal distribution

        double numerator = 0;
        //might replace this with variance
        int h=23;//smoothing parameter for the dist estimation

        for (int i=0;i<res.getMac().size();i++){
            //for each mac in the result scan we check it against the specified loc in the grid
            int ind = loc.getMacs().indexOf(res.getMac().get(i));

            if( ind == -1){
                //we didnt find a matching MAC address, so we can't do the normal calculation

            }
            else{
                //we found the mac, find the most likely signal level (highest probability)
                //use this signal as the "mean" value for the RSS for this mac at this location
                //calculating the variance
                double avgSum=0;
                for (int j=0;j<loc.getProbs().get(ind).size();j++){
                    int quantity = (int)(loc.getProbs().get(ind).get(j)*100);
                    avgSum = avgSum + (quantity * loc.getRss().get(ind).get(j));
                }
                avgSum = avgSum/(double)100;

                //now the variance
                double var = 0;
                for (int j=0;j<loc.getProbs().get(ind).size();j++){
                    int quantity = (int)(loc.getProbs().get(ind).get(j)*100);
                    var = var + quantity*Math.pow(loc.getRss().get(ind).get(j)-avgSum,2);
                }
                var = var/(double)100;

                if(var==0){
                    var=1;
                }

                //int rpRSS = loc.getRss().get(ind).get(highProbInd);

                //if we use variance for h here we might need to change the formula slightly
                numerator = numerator + (1/(Math.sqrt(2*Math.PI*var))) * (Math.exp(-0.5 *((Math.pow((res.getRss().get(i)-(int)avgSum),2))/var)));

            }

        }

        numerator = numerator/res.getMac().size();
        //results.append(x + "," + y + ": " + numerator + "\n");
        probsStorage[x][y] = numerator;

    }

    public void revisedProbabilityCalculation(int x, int y, WAP loc, recording.RecordResult res){
        //going to perform Bayes estimation of the distribution of the RPs signal distribution

        double numerator = 0;

        //int h=hVal.getValue();//smoothing parameter for the dist estimation
        int h = 12;

        for (int i=0;i<res.getMac().size();i++){
            //for each mac in the result scan we check it against the specified loc in the grid
            String temp = res.getMac().get(i).substring(0,8);

            if(temp.equals("6c:f3:7f"))
            {
                int ind = loc.getMac().indexOf(res.getMac().get(i));

                if( ind == -1){
                    //we didnt find a matching MAC address, so we can't do the normal calculation
                    //numerator = numerator * 0.95;
                }
                else{
                    //we found the mac, use the average RSS

                    int rpRSS = loc.getRss().get(ind);

//                if(rpRSS>98){
//                    h=50;
//                }

                    if(rpRSS >= 40) {

                        numerator = numerator + (1 / (h * Math.sqrt(2 * Math.PI))) * (Math.exp(-((Math.pow((res.getRss().get(i) - rpRSS), 2)) / (2 * Math.pow(h, 2)))));

                    }

                }

            }


        }

        numerator = numerator/res.getMac().size();

        if(wknn && prevY != -1){
            //using pedometer information
                ArrayList<Integer> tempCoord = new ArrayList<>();
                tempCoord.add(x);
                tempCoord.add(y);
                int ind = finalPedoList.indexOf(tempCoord);


                if(ind != -1){
                    //debug.append("Prior: " + x + "," + y + ":" + numerator + "\n");
                    numerator = numerator * 1.20;
                    debug.append("Pos: " + x + "," + y + ":" + numerator + "\n");
                }
                else{
                    //numerator = numerator * 1/43.0;
                    numerator = numerator * 0.95;
                    debug.append("Pos: " + x + "," + y + ":" + numerator + "\n");
                }

        }
        else {
            debug.append("Pos: " + x + "," + y + ":" + numerator + "\n");
        }

        probsStorage[x][y] = numerator;


    }


    //proabbaility calculations
    public void calculateProbabilities(int x, int y, location loc, recording.RecordResult res){
        //in here we take in the scan result, compare it to the location in the grid
        //and calculate its probability accordingly
        //then store the probability in the probsGrid at the right location
        Log.d(TAG,"POSITION:" + x + "," + y + "***************************************************************************************************\n");


        //the starting probability is just the probability of the location in the environment
        //which is 1/totalAmountOfLocations
        double numerator=0;///(double)(validCounter);
        //Log.d(TAG,"Numerator initial: " + String.valueOf(numerator) + "\n");
        double denom=0;

        int missedMac=0;

        for (int i=0;i<res.getMac().size();i++){
            //for each mac in the result scan we check it against the specified loc in the grid
            int ind = loc.getMacs().indexOf(res.getMac().get(i));

            if( ind == -1){
                //ie the mac could not be found at this location, so it's probability is zero
                //finalProb=0;
                //break;

                //numerator needs to be decreased because there's quite a difference in location
                //if one mac address is not there.
                //numerator = numerator * (1-((double)1/(double)loc.getMacs().size()));
                //numerator =numerator/1.015;
                //numerator = numerator - ((double)1/(double)loc.getMacs().size());

            }
            else{
                //we found the mac, now we have to compare signal levels
                int lvlInd = loc.getRss().get(ind).indexOf(res.getRss().get(i));


                if(lvlInd == -1){
                    //we could not find a matching level, so we check for the closest level
                    //this has to be within a certain bound
                    int lowest = 3;
                    int closestIndex=-1;

                    for(int j=0;j<loc.getRss().get(ind).size();j++){
                        int check = Math.abs(loc.getRss().get(ind).get(j)-res.getRss().get(i));
                        if(check < lowest){
                            lowest = check;
                            closestIndex = j;
                        }
                    }

                    //weve found the closest level to the level found in the scan
                    if(closestIndex != -1){
                      //  numerator = numerator + loc.getProbs().get(ind).get(closestIndex);
                    }
                    else{
                        //numerator = 0;
                        //numerator = numerator/1.015;
                        //numerator = numerator * (1-((double)1/(double)loc.getMacs().size()));
                        //numerator = numerator - ((double)1/(double)loc.getMacs().size());
                        //numerator = numerator - lowest
                    }

                    //testing
                    //if we have found the right mac, but not the correct level then decrease
                    //the numerator: to do this look how far the signal level is off




                }
                else{

                        numerator = numerator + loc.getProbs().get(ind).get(lvlInd);
                    //Log.d(TAG,"New Numerator: " + String.valueOf(numerator) + "\n");
                }
            }


            Log.d(TAG,"New Numerator: " + String.valueOf(numerator) + "\n");

            //we have calculated the numerator for the probability
            //we will now use the environemnt data to calculate the denominator

            ind = globalLoc.getMacs().indexOf(res.getMac().get(i));

            if(ind != -1){
                //we found the mac in the environment readings
                //now use the probability of the recorded rss level in the environment
                int lvlInd = globalLoc.getRss().get(ind).indexOf(res.getRss().get(i));

                if(lvlInd != -1){
                    //we found the same level, so use the corresp prob stored for it
                    if(denom==0){
                        denom = globalLoc.getProbs().get(ind).get(lvlInd);
                    }
                    else{
                        denom = denom + globalLoc.getProbs().get(ind).get(lvlInd);
                    }

                    //Log.d(TAG,"New Denom: " + String.valueOf(denom) + "\n");

                }
                else{
                    //we didnt find the same level, find the closest level
                    int lowest = 3;
                    int closestIndex=-1;

                    for(int j=0;j<globalLoc.getRss().get(ind).size();j++){
                        if(Math.abs(globalLoc.getRss().get(ind).get(j)-res.getRss().get(i)) < lowest){
                            lowest = Math.abs(globalLoc.getRss().get(ind).get(j)-res.getRss().get(i));
                            closestIndex = j;
                        }
                    }

                    //weve found the closest level to the level found in the scan
                    if(closestIndex != -1){
                        denom = denom + globalLoc.getProbs().get(ind).get(closestIndex);
                    }
                    else{
                        //numerator = 0;
                    }
                }
            }



        }
        //now combine the numerator and denom
        //Log.d(TAG,String.valueOf(numerator) + "/" + String.valueOf(denom) + "\n");
        double finalProb = numerator;//experimenting with just numerator, no denom
        //Log.d(TAG,finalProb +"\n");
       // results.append(x + "," + y + ": " + finalProb + "\n");
        probsStorage[x][y] = finalProb;




    }

    public void firstLocationUsingProb (recording.RecordResult rr) {
        //in this method were going to calculate all the probabilities of the locations stored in the grid
        //with comparison to the scan result just received

        StringBuffer strbuf = new StringBuffer();
        StringBuffer debugStr = new StringBuffer();
        // environmentProbabilities();

        //these will store the likely locations from RSS
        double biggest = 0;

        //THIS IS THE PROBABILITY OPTION
        //Log.d(TAG, "PROB SYSTEM");
        for (int i = 0; i < gridX; i++) {
            for (int j = 0; j < gridY; j++) {
                if (probGrid[i][j].isValid()) {
                    calculateProbabilities(i, j, probGrid[i][j], rr);
                    strbuf.append("Loc " + i + "," + j + " Prob: " + probsStorage[i][j] + "\n");

                    //this is where the closest coords are found
                    if (probsStorage[i][j] > biggest) {
                        biggest = probsStorage[i][j];
                        ArrayList<Integer> temp = new ArrayList<>();
                        temp.add(i);
                        temp.add(j);
                        firstScans.add(temp);
                        firstScansProb.add(biggest);
                    }
                }
            }
        }

        if(firstCount==numScans){
            findFirstFinal();
        }
        else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    // this code will be executed after 200 miliseconds
                    requested = true;
                    mainWm.startScan();
                }
            }, 100);

        }
    }

    public void findFirstFinal(){
        double [] coordProbCount = new double [firstScans.size()];

        ArrayList<ArrayList<Integer>> checkedCoords = new ArrayList<>();


        for (int i=0;i<firstScans.size();i++){
            ArrayList<Integer> temp = firstScans.get(i);
            //firstScans.remove(i);
            if(!checkedCoords.contains(temp)) {
                for (int j = i; j < firstScans.size(); j++) {
                    coordProbCount[i] = coordProbCount[i]+ firstScansProb.get(i);
                }
            }
            checkedCoords.add(temp);
        }

        double highest=0;
        for(int i=0;i<coordProbCount.length;i++){
            if(coordProbCount[i] > highest){
                highest = coordProbCount[i];
                finalX = firstScans.get(i).get(0);
                finalY = firstScans.get(i).get(1);

            }
        }

       // debug.append("FINAL LOC: " + finalX + "," + finalY + "\n");
        prevX=finalX;
        prevY=finalY;
        //Log.d(TAG,"saving prev loc: " + pedoGrid[prevX][prevY].getConnections().size());
        stepsTaken=0;

    }


    public void probWKNN(){
        //find the k most probable coords, then average them
        int[] coords = new int[2];

        int k = 3; //this is the main k value that decides how many nn's we look at afterr ranking
        int [][] nn = new int [2][k];//nn vector
        double [] distances = new double[k];//distances vector for checking and sorting
        for(int i=0;i<k;i++){
            distances[i] = 0;
            nn[0][i]=9;
            nn[1][i]=9;
        }


        // we need another distances matrix to read and manipulate values
        double [][] tempDist = probsStorage;

        for (int i=0;i<gridX;i++){
            for (int j=0;j<gridY;j++){
                for(int p=0;p<k;p++){
                    if (tempDist[i][j] > distances[p]){
                        Log.d(TAG,tempDist[i][j] + ">" + distances[p] + "\n");

                        double [] temp = new double [k-p];
                        int [][] tempCoords = new int [2][k-p];
                        int tempIndex=0;

                        for(int y=p;y<k;y++) {
                            temp[tempIndex] = distances[y];
                            tempCoords[0][tempIndex] = nn[0][y];
                            tempCoords[1][tempIndex] = nn[1][y];
                            tempIndex++;
                        }
                        tempIndex=0;

                        distances[p] = tempDist[i][j];
                        nn[0][p] = i;
                        nn[1][p] = j;

                        for(int y=p+1;y<k;y++){
                            distances[y] = temp[tempIndex];
                            nn[0][y] = tempCoords[0][tempIndex];
                            nn[1][y] = tempCoords[1][tempIndex];
                            tempIndex++;
                        }



                        break;
                    }
                }

            }
        }

        double avgX=0;
        double avgY=0;
        double probDivisor = 0;

        for (int i=0;i<k;i++){
            avgX = avgX + nn[0][i] * probsStorage[nn[0][i]][nn[1][i]];
            avgY = avgY + nn[1][i]* probsStorage[nn[0][i]][nn[1][i]];
            probDivisor = probDivisor + probsStorage[nn[0][i]][nn[1][i]];

        }

        avgX = avgX/(double)probDivisor;
        avgY = avgY/(double)probDivisor;

        debug.append("WKNN: " + Math.round(avgX) + "," + Math.round(avgY) + "\n");



    }

    public void probKNN(){
        //find the k most probable coords, then average them
        int[] coords = new int[2];

        int k = 3; //this is the main k value that decides how many nn's we look at afterr ranking
        int [][] nn = new int [2][k];//nn vector
        double [] distances = new double[k];//distances vector for checking and sorting
        for(int i=0;i<k;i++){
            distances[i] = 0;
            nn[0][i]=9;
            nn[1][i]=9;
        }


        // we need another distances matrix to read and manipulate values
        double [][] tempDist = probsStorage;

        for (int i=0;i<gridX;i++){
            for (int j=0;j<gridY;j++){
                for(int p=0;p<k;p++){
                    if (tempDist[i][j] > distances[p]){
                        Log.d(TAG,tempDist[i][j] + ">" + distances[p] + "\n");

                        double [] temp = new double [k-p];
                        int [][] tempCoords = new int [2][k-p];
                        int tempIndex=0;

                        for(int y=p;y<k;y++) {
                            temp[tempIndex] = distances[y];
                            tempCoords[0][tempIndex] = nn[0][y];
                            tempCoords[1][tempIndex] = nn[1][y];
                            tempIndex++;
                        }
                        tempIndex=0;

                        distances[p] = tempDist[i][j];
                        nn[0][p] = i;
                        nn[1][p] = j;

                        for(int y=p+1;y<k;y++){
                            distances[y] = temp[tempIndex];
                            nn[0][y] = tempCoords[0][tempIndex];
                            nn[1][y] = tempCoords[1][tempIndex];
                            tempIndex++;
                        }



                        break;
                    }
                }

            }
        }


        double avgX=0;
        double avgY=0;
        double probDivisor = 0;

        for (int i=0;i<k;i++){
            avgX = avgX + nn[0][i];
            avgY = avgY + nn[1][i];

        }

        avgX = avgX/(double)k;
        avgY = avgY/(double)k;

        debug.append("KNN: " + Math.round(avgX) + "," + Math.round(avgY) + "\n");



    }



    public void locateUsingProb (recording.RecordResult rr){
        //in this method were going to calculate all the probabilities of the locations stored in the grid
        //with comparison to the scan result just received

        StringBuffer strbuf = new StringBuffer();
        StringBuffer debugStr = new StringBuffer();
        //debug.setText("");

        //these will store the likely locations from RSS
        double biggest=0;
        backUpX = prevX;
        backUpY = prevY;
        backUpSteps =stepsTaken;
        ArrayList<ArrayList<Integer>> wifiLocations = new ArrayList<>();

        //add this into the if
        //wknn &&
        boolean movement = false;
        if (wknn &&  prevY != -1) {
            //stepsTaken=10;
            debug.append("Using pedometer information...\n");
            //Log.d(TAG, "Previous: " + prevX + "," + prevY);
            movement = pedoProbability();

            //there was movement detected
            if (movement) {
                //we have to compare the two lists: finalPedoList and wifiLocations
                //basially for now going to pick the coord with the highest probability present in the
                //nearest coords
                //debug.append("Level: " + (Math.round(stepsTaken / 4.0) * 4) / pedoGrid[prevX][prevY].getConDist() + "\n");
                strbuf.append("Possible locations:");
                for (int i = 0; i < finalPedoList.size(); i++) {
                    debug.append(finalPedoList.get(i).get(0) + "," + finalPedoList.get(i).get(1) + "/");
                }
                debug.append("\n");


            }

        }
        for (int i=0;i<gridX;i++) {
            for (int j = 0; j < gridY; j++) {
                if (detGrid[i][j].isValid()) {
                    revisedProbabilityCalculation(i, j, detGrid[i][j], rr);
                    //debug.append("Loc " + i + "," + j + ": " + probsStorage[i][j] + "\n");

                    //this is where the closest coords are found
                    if (probsStorage[i][j] > biggest) {
                        biggest = probsStorage[i][j];
                        finalX = i;
                        finalY = j;

                    }
                }
            }
        }
        if(prevY != -1 && !movement && wknn){

                //finalY = prevY;
                //finalX = prevX;

        }




        int [] drawCoords = new int [2];
        drawCoords[0] = finalX;
        drawCoords[1] = finalY;
        drawCircle(drawCoords);
        //probKNN();
        //probWKNN();

        debug.append(finalX + "," + finalY + "\n");

        long end = System.nanoTime();

        //set the prev location here for the next reading
        prevX=finalX;
        prevY=finalY;
        finalPedoList.clear();
        checkedList.clear();
        //Log.d(TAG,"saving prev loc: " + pedoGrid[prevX][prevY].getConnections().size());
        stepsTaken=0;

    }


    public void wifiPedoFusion(){
        //in this function look at the connections to the prev location
        //then look at the same amount of locations in the probability grid witht he highest probs

        double highProb=0;
        ArrayList<Double> tempList = new ArrayList<>();



        for (int i=0;i<gridX;i++) {
            for (int j = 0; j < gridY; j++) {
                if(pedoGrid[i][j].isValid()) {
                    tempList.add(probsStorage[i][j]);
                }
            }
        }

        //sort all the probabilities so we can find the nearest however many connections there are
        Collections.sort(tempList, Collections.<Double>reverseOrder());

        int possibleConns = pedoGrid[prevX][prevY].getConnections().size();
        ArrayList<ArrayList<Integer>> wifiCons = new ArrayList<>();

        for(int i=0;i<possibleConns;i++){
            double tempCompare = tempList.get(i);

            for(int g=0;g<gridX;g++){
                for(int h=0;h<gridY;h++){
                    if(probsStorage[g][h] == tempCompare){
                        //storing coords of nearest conns
                        wifiCons.add(new ArrayList<Integer>());
                        wifiCons.get(wifiCons.size()-1).add(g);
                        wifiCons.get(wifiCons.size()-1).add(h);

                    }
                }
            }
        }


        //print out the connections
        for (int i=0; i < wifiCons.size();i++){
            debug.append(wifiCons.get(i).get(0) + "," + wifiCons.get(i).get(1) + "/");
        }
        debug.append("\n");




        //this is where the actual comparsion happens
        //before we check anything else, we check the most likely option

        //the highest prob coord is available in the connections list
//        int ind = finalPedoList.indexOf(wifiCons.get(0));
//
//        if(ind != -1){
//            finalX = wifiCons.get(0).get(0);
//            finalY = wifiCons.get(0).get(1);
//            debug.append("Probability+Pedometer=match\n");
//            return;
//        }
//
//        //if the highest probability coord is not present in the connections list,
//        //then we have to find the next most likely option available
//
//        boolean foundConnection=false;
//        for(int i=1;i<wifiCons.size();i++){
//            ind = finalPedoList.indexOf(wifiCons.get(i));
//
//            if(ind != -1){
//                finalX = wifiCons.get(i).get(0);
//                finalY = wifiCons.get(i).get(1);
//                debug.append("Using Prob location present in pedo data\n");
//                foundConnection=true;
//                return;
//            }
//
//        }
//
//            //none of the pedo locations and wifi locations match up
//            //find the next most likely option, the connection with the highest probab
//            double highestProb=0;
//            for(int i=0;i<possibleConns;i++){
//                double tempProb = probsStorage[finalPedoList.get(i).get(0)][finalPedoList.get(i).get(1)];
//
//                if(tempProb>highestProb){
//                    highestProb=tempProb;
//                    rescanX = finalPedoList.get(i).get(0);
//                    rescanY = finalPedoList.get(i).get(1);
//                    //checkRescan
//                    finalX = finalPedoList.get(i).get(0);
//                    finalY = finalPedoList.get(i).get(1);
//
//                    //return;
//                }
//            }
//
//
//            //perform a rescan
//            //requested = true;
//            //mainWm.startScan();
//            debug.append("Had to use pedo loc with highest prob\n");
            return;




    }

    public void noMovement(int x, int y, ArrayList<ArrayList<Integer>> wifiLocs){

        //there was no movement recorded here, so we have to look at the wifi results
        //and analyse them to see if the same location is still most likely
        if(x==prevX && y==prevY){
            //we are definitely at the same location, weight it highly
            finalX=x;
            finalY=y;
        }
        else{
            //there wasnt any movement, but the wifi scan says we are in a different location
            //check if this is possible (at the nearest locations to the current prev location)
            for(int i=0;i<pedoGrid[x][y].getConnections().size();i++){
                int possibleX = pedoGrid[x][y].getConnections().get(i).get(0);
                int possibleY = pedoGrid[x][y].getConnections().get(i).get(1);



            }
        }

    }

    public boolean pedoProbability(){
        //find out what "level" we go to in connections
        //Log.d(TAG,String.valueOf(stepsTaken));
        //if the level is 0 it means no steps were taken, so most likely at same position
        boolean movement=false;
        debug.append("Steps: " + stepsTaken + "\n");
        int level = (int)(Math.round((stepsTaken/4.0))*4)/pedoGrid[prevX][prevY].getConDist();

        //debug.append("Level: " + level + "\n");

        if(level!=0) {
            //run a loop down the conections to the correct level
            int levelCount = 1;

            if(pedoGrid[prevX][prevY].isValid) {
                pedoRecur(prevX, prevY, prevX, prevY, level, levelCount);
            }
            movement = true;

            return movement;
        }
        else{
            return movement;
        }



        //will have to return soemthing here

    }

    public void pedoRecur(int x,int y,int preX, int preY, int level,int lvlCount){
        for(int i=0;i<pedoGrid[x][y].getConnections().size();i++){
            ArrayList<Integer> current = new ArrayList<>();
            current.add(x);
            current.add(y);
            checkedList.add(current);
            int newX = pedoGrid[x][y].getConnections().get(i).get(0);
            int newY = pedoGrid[x][y].getConnections().get(i).get(1);
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(newX);
            temp.add(newY);

            if (lvlCount>=level  ){

                int check = checkedList.indexOf(temp);

                if(!(newX==preX && newY==preY) && check==-1) {

                    finalPedoList.add(temp);
                    Log.d(TAG, "Connection added" + newX + "," + newY + "\n");
                }
            }
            else{
                lvlCount=lvlCount+1;
//                if(i==0) {
//                    for (int j = 0; j < pedoGrid[x][y].getConnections().size(); j++) {
//                        int tX = pedoGrid[x][y].getConnections().get(j).get(0);
//                        int tY = pedoGrid[x][y].getConnections().get(j).get(1);
//                        temp = new ArrayList<>();
//                        temp.add(tX);
//                        temp.add(tY);
//                        checkedList.add(temp);
//                       // Log.d(TAG,"Added: " + tX + "," + tY);
//                    }
//                }

                Log.d(TAG,"Call Recur: " + level + " " + lvlCount + " at point: " + newX + "," + newY);

                int check = checkedList.indexOf(temp);
                if(!(newX==preX && newY==preY) && check==-1)
                    pedoRecur(newX,newY,x,y,level,lvlCount);

                lvlCount = lvlCount-1;
            }
            checkedList.add(temp);
        }
    }






    //this function is only used to add the data to begin with, not during runtime
    public void addPedometerData(){
        //we have to add each location individually
        //then we will write all of the data to a .dat file like the fpdb

        //0,1

        ArrayList<ArrayList<Integer>> tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(0);
        tempCons.get(tempCons.size()-1).add(2);

        pedoGrid[0][1] = new pedoLocation();
        pedoGrid[0][1].setPedoLoc(tempCons);


        //0,2
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(0);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(0);
        tempCons.get(tempCons.size()-1).add(3);

        pedoGrid[0][2] = new pedoLocation();
        pedoGrid[0][2].setPedoLoc(tempCons);

        //0,3
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(0);
        tempCons.get(tempCons.size()-1).add(2);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(0);
        tempCons.get(tempCons.size()-1).add(4);

        pedoGrid[0][3] = new pedoLocation();
        pedoGrid[0][3].setPedoLoc(tempCons);

        //0,4
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(0);
        tempCons.get(tempCons.size()-1).add(3);



        pedoGrid[0][4] = new pedoLocation();
        pedoGrid[0][4].setPedoLoc(tempCons);

        //1,1
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(1);


        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(0);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(2);



        pedoGrid[1][1] = new pedoLocation();
        pedoGrid[1][1].setPedoLoc(tempCons);

        //2,1
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(1);


        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(1);


        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(2);

        pedoGrid[2][1] = new pedoLocation();
        pedoGrid[2][1].setPedoLoc(tempCons);

        //3,1
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(2);



        pedoGrid[3][1] = new pedoLocation();
        pedoGrid[3][1].setPedoLoc(tempCons);

        //4,1
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(0);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(0);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(2);



        pedoGrid[4][1] = new pedoLocation();
        pedoGrid[4][1].setPedoLoc(tempCons);


        //1,2
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(3);

        pedoGrid[1][2] = new pedoLocation();
        pedoGrid[1][2].setPedoLoc(tempCons);

        //1,3
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(2);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(4);

        pedoGrid[1][3] = new pedoLocation();
        pedoGrid[1][3].setPedoLoc(tempCons);

        //1,4
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(0);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(3);





        pedoGrid[1][4] = new pedoLocation();
        pedoGrid[1][4].setPedoLoc(tempCons);

        //2,4
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(3);



        pedoGrid[2][4] = new pedoLocation();
        pedoGrid[2][4].setPedoLoc(tempCons);

        //3,4
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(3);



        pedoGrid[3][4] = new pedoLocation();
        pedoGrid[3][4].setPedoLoc(tempCons);

        //4,4
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(3);



        pedoGrid[4][4] = new pedoLocation();
        pedoGrid[4][4].setPedoLoc(tempCons);

        //2,2
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(3);

        pedoGrid[2][2] = new pedoLocation();
        pedoGrid[2][2].setPedoLoc(tempCons);

        //2,3
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(2);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(4);

        pedoGrid[2][3] = new pedoLocation();
        pedoGrid[2][3].setPedoLoc(tempCons);

        //3,2
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(3);

        pedoGrid[3][2] = new pedoLocation();
        pedoGrid[3][2].setPedoLoc(tempCons);

        //3,3
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(2);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(4);

        pedoGrid[3][3] = new pedoLocation();
        pedoGrid[3][3].setPedoLoc(tempCons);

        //4,2
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(3);

        pedoGrid[4][2] = new pedoLocation();
        pedoGrid[4][2].setPedoLoc(tempCons);

        //4,3
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(2);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(4);

        pedoGrid[4][3] = new pedoLocation();
        pedoGrid[4][3].setPedoLoc(tempCons);


        //1,0
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(0);


        pedoGrid[1][0] = new pedoLocation();
        pedoGrid[1][0].setPedoLoc(tempCons);


        //2,0
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(0);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(0);



        pedoGrid[2][0] = new pedoLocation();
        pedoGrid[2][0].setPedoLoc(tempCons);

        //3,0
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(0);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(0);



        pedoGrid[3][0] = new pedoLocation();
        pedoGrid[3][0].setPedoLoc(tempCons);

        //4,0
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(0);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(0);







        pedoGrid[4][0] = new pedoLocation();
        pedoGrid[4][0].setPedoLoc(tempCons);

        //5,0
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(0);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(1);



        pedoGrid[5][0] = new pedoLocation();
        pedoGrid[5][0].setPedoLoc(tempCons);

        //5,1
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(2);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(0);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(0);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(1);



        pedoGrid[5][1] = new pedoLocation();
        pedoGrid[5][1].setPedoLoc(tempCons);

        //5,2
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(1);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(3);


        pedoGrid[5][2] = new pedoLocation();
        pedoGrid[5][2].setPedoLoc(tempCons);

        //5,3
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(2);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(4);


        pedoGrid[5][3] = new pedoLocation();
        pedoGrid[5][3].setPedoLoc(tempCons);

        //5,4
        tempCons = new ArrayList<>();



        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(5);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(3);


        pedoGrid[5][4] = new pedoLocation();
        pedoGrid[5][4].setPedoLoc(tempCons);

        //5,5
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(7);
        tempCons.get(tempCons.size()-1).add(7);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(4);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(6);
        //**************************************************************************************************



        pedoGrid[5][5] = new pedoLocation();
        pedoGrid[5][5].setPedoLoc(tempCons);

        //5,6
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(5);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(7);


        pedoGrid[5][6] = new pedoLocation();
        pedoGrid[5][6].setPedoLoc(tempCons);

        //5,7
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(6);


        pedoGrid[5][7] = new pedoLocation();
        pedoGrid[5][7].setPedoLoc(tempCons);

        //project labs
        //1,5
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(5);

        pedoGrid[1][5] = new pedoLocation();
        pedoGrid[1][5].setPedoLoc(tempCons);

        //1,6
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(6);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(7);

        pedoGrid[1][6] = new pedoLocation();
        pedoGrid[1][6].setPedoLoc(tempCons);

        //1,7
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(7);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(6);

        pedoGrid[1][7] = new pedoLocation();
        pedoGrid[1][7].setPedoLoc(tempCons);

        //2,5
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(5);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(6);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(5);

        pedoGrid[2][5] = new pedoLocation();
        pedoGrid[2][5].setPedoLoc(tempCons);

        //3,5
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(5);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(6);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(5);

        pedoGrid[3][5] = new pedoLocation();
        pedoGrid[3][5].setPedoLoc(tempCons);

        //4,5
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(5);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(6);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(7);
        tempCons.get(tempCons.size()-1).add(7);

        pedoGrid[4][5] = new pedoLocation();
        pedoGrid[4][5].setPedoLoc(tempCons);

        //2,6
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(5);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(7);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(6);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(6);

        pedoGrid[2][6] = new pedoLocation();
        pedoGrid[2][6].setPedoLoc(tempCons);

        //3,6
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(5);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(7);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(6);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(6);

        pedoGrid[3][6] = new pedoLocation();
        pedoGrid[3][6].setPedoLoc(tempCons);

        //4,6
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(5);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(7);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(6);

        pedoGrid[4][6] = new pedoLocation();
        pedoGrid[4][6].setPedoLoc(tempCons);

        //2,7
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(1);
        tempCons.get(tempCons.size()-1).add(7);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(6);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(7);

        pedoGrid[2][7] = new pedoLocation();
        pedoGrid[2][7].setPedoLoc(tempCons);

        //3,7
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(6);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(2);
        tempCons.get(tempCons.size()-1).add(7);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(7);

        pedoGrid[3][7] = new pedoLocation();
        pedoGrid[3][7].setPedoLoc(tempCons);

        //4,7
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(6);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(3);
        tempCons.get(tempCons.size()-1).add(7);

        pedoGrid[4][7] = new pedoLocation();
        pedoGrid[4][7].setPedoLoc(tempCons);


        //the random connection point
        //7,7
        tempCons = new ArrayList<>();

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(5);
        tempCons.get(tempCons.size()-1).add(5);

        tempCons.add(new ArrayList<Integer>());
        tempCons.get(tempCons.size()-1).add(4);
        tempCons.get(tempCons.size()-1).add(5);

        pedoGrid[7][7] = new pedoLocation();
        pedoGrid[7][7].setPedoLoc(tempCons);




        //****************************
        //now write the data to file
        String root = Environment.getExternalStorageDirectory().getAbsolutePath().toString();
        String finalRoot = root + "/probGrid/";
        File dir = new File (finalRoot);
        dir.mkdirs();

        try{
            ArrayList<pedoLocation> objWri= new ArrayList<>();
            //write grid into arraylist
            for (int i=0;i<gridX;i++){
                for(int j=0; j<gridY;j++) {
                    objWri.add(pedoGrid[i][j]);

                }
            }


            FileOutputStream fos = new FileOutputStream(new File(finalRoot + "pedoGrid" + ".dat"));



            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject((objWri));
            //locatetext.setText("Write Success!");
            oos.close();
            fos.close();

        } catch (Exception e){
            e.printStackTrace();
            //locatetext.setText("FAILED");
        }




    }


    //The wifi scanning method
    public class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent){
            List<ScanResult> wifiList;
            wifiList = mainWm.getScanResults();
            ArrayList<String> macs = new ArrayList<>();
            ArrayList<Integer> rss = new ArrayList<>();


            StringBuffer strBuf = new StringBuffer();
            strBuf.append("Results:\n");
            for(int i=0; i < wifiList.size(); i++) {
                strBuf.append(wifiList.get(i).BSSID.toString() + " "
                        + WifiManager.calculateSignalLevel(wifiList.get(i).level,100) + "\n");

                // this section is for data recording and testing purposes
                macs.add(wifiList.get(i).BSSID);
                rss.add(WifiManager.calculateSignalLevel(wifiList.get(i).level,100));

            }


            if(requested) {
                if(probSystem) {
                    final recording.RecordResult rr = new recording.RecordResult();
                    rr.setResult(macs, rss);
                    locateUsingProb(rr);
                }
                else if(detSystem){
                        reqSignal();
                        //mainWm.startScan();
                }
            }

        }


    }

    //the class for the pedometer objects to be stored
    public static class pedoLocation extends MainActivity implements Serializable{
        private static final long serialVersionUID = 41L;
        ArrayList<ArrayList<Integer>> connections;
        boolean isValid=false;

        //this is the step count between each coordinate in the environment
        int conDist;

        public void setPedoLoc(ArrayList<ArrayList<Integer>> cons){
            this.connections = cons;
            this.isValid=true;
            this.conDist=4;
        }

        public ArrayList<ArrayList<Integer>> getConnections (){
            return connections;
        }

        public int getConDist(){
            return conDist;
        }

        public boolean isValid(){
            return isValid;
        }


    }



    //*********************************************************************
    //These are all menu methods
    //*********************************************************************

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.home){
            Log.d(TAG, "Going home");
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_record) {
            // Handle the recording action
            Intent intent = new Intent(this, recording.class);
            intent.putExtra("det",detSystem);
            intent.putExtra("prob",probSystem);
            intent.putExtra("knn",knn);
            intent.putExtra("wknn",wknn);

            //finish();

            startActivity(intent);


        } else if (id == R.id.nav_settings) {
            unregisterReceiver(recWifi);
            Intent intent = new Intent(this, settings.class);
            //unregisterReceiver(recWifi);
            //finish();

            startActivity(intent);
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
