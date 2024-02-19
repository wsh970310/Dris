package com.dji.ux.sample;

import dji.sdk.payload.Payload;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.dji.mapkit.core.camera.DJICameraUpdate;
import com.dji.mapkit.core.camera.DJICameraUpdateFactory;
import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJICameraPosition;
import com.dji.mapkit.core.models.DJILatLng;
import dji.keysdk.CameraKey;
import dji.keysdk.KeyManager;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;
import dji.ux.widget.controls.CameraControlsWidget;

//add

import dji.common.camera.SettingsDefinitions;
import dji.keysdk.DJIKey;
import dji.keysdk.PayloadKey;
import dji.keysdk.callback.KeyListener;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.opencsv.CSVReader;
import static android.content.ContentValues.TAG;

import java.util.ArrayList;
import java.util.List;

// import end

/**
 * Activity that shows all the UI elements together
 */

/**
 * editied by cheolung Kang before 2021
 */
/** from 2021  editied by WangShiHe*/

public class CompleteWidgetActivity<gammaConnection_Success> extends Activity {

    /** declaring  variables **/
    private MapWidget mapWidget;
    private ViewGroup parentView;
    private FPVWidget fpvWidget;
    private FPVWidget secondaryFPVWidget;
    private RelativeLayout primaryVideoView;
    private FrameLayout secondaryVideoView;
    private boolean isMapMini = true;
    private int height;
    private int width;
    private int margin;
    private int deviceWidth;
    private int deviceHeight;

    // add
    private DJIKey getDataKey;
    private DJIKey payloadNameKey;
    private String payloadName = "";
    private TextView pushTextView;
    private ImageView gammaView;
    private TextView payloadNameView;
    int total_data = 0;
    int data_len = 0;
    int maxRange = 1500000;
    static int division = 2000;

    static float a = (float) 7.423444;// parameters after energy correction
    static float b = -147632.12f;

    byte[] all_virtual_data = new byte[1];
    byte[] remain_virtual_data = new byte[1];
    private static int timetotal_data = 0;
    private static int all_total_data = 0;

    int all_data_len = 0 ;
    private static final float[] energy = new float[500];
    private static final float[] Xaxis = new float[2005];
    private static final int[] Ycounts = new int[2005];

    private static volatile int total_count = 0;
    private static byte[] ch = new byte[8004];
    private static int[][] ch_data = new int[500][4];

    //private static volatile int[][] total_ch_data = new int[15000][4];//gamma count  array in 1 minute. the smaller the better.
   // private static volatile long[] total_ch_Time = new long[15000];//gamma count input time. The size must be same to the first dimension of total_ch_data[][]
    private static volatile int[][] total_ch_data = new int[18000][4];//gamma count  array in 1 minute. the smaller the better.
    private static volatile long[] total_ch_Time = new long [18000];//gamma count input time. The size must be same to the first dimension of total_ch_data[][]


    Canvas canvas;
    Paint paint = new Paint();
    private int gamma_img_trans=255;
    private int dist_position =0;

    int gammaViewwidth = 923;
    private int[][] segmap = new int[512][512];
    private int scint_width = 20;
    private int scint_height = 20;
    private int[][] segmap_img = new int[scint_width][scint_height];  //segmentation img /after index
    private int[] mColorbar;
    Bitmap mColorbitmap;

    private TextView pushTextViewFromUsb;
    private TextView gamma_connection_status;
    private FrameLayout gamma_frame;

    private ConstraintLayout histogram;
    private int window_flag;
    private static volatile int cps = 0;

    private long the_lasttime_datainput ;

    private boolean isRunning = false;
    private LineChart chart;
    private Thread thread;

    private static volatile long the_firsttime_datainput;
    private SeekBar trans_controller;
    private SeekBar colorbar_controller;
    private SeekBar dist_controller;
    private TextView dis_txt;

    private int color_position;
    private int energywin_1=0;
    private int energywin_2=0;
   // private GoogleMap mMap;

    HandlerThread mHandlerThread ;//HandleThread
    Handler workHandler;//HandleThread
    Handler mainHandler;//HandleThread

    Intent intent;//HandleThread

    private static long data_generation_finish;
    boolean isIntent = false;//IntentService ---- true;


    /** multi-thread ____　InentService
     * what it does when new intentservice is formed.
     * Automatically end when all processes are completed**/

    public static class MyIntentService extends IntentService {
        public MyIntentService(){
            super("MyIntentService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            int temp = 0;
            long inputtime = System.currentTimeMillis();

            byte[] data = intent.getByteArrayExtra("dating");
            if (the_firsttime_datainput == 0) {
                the_firsttime_datainput = System.currentTimeMillis();
                Log.d(TAG, "dummy_data:response time = "+ Integer.toString((int)(the_firsttime_datainput - data_generation_finish)));

            }

            int data_len = ((((int) data[3] & 0xff) << 24) |                                                 //head(4byte)  content: how many byte
                    (((int) data[2] & 0xff) << 16) |
                    (((int) data[1] & 0xff) << 8) |
                    (((int) data[0] & 0xff)));

                if (data_len > 4) {             //data processing
                temp=(data_len-4)/16;
                byte[] data_temp  = byteArrayToInt(data, data_len);         //byte to int
                time_input_count(ch_data, temp);        // storage of int and time     (in memory)
                count(temp); // energy spectrum Y axis
                total_count += temp;
                int delta = ((int) (inputtime - the_firsttime_datainput)/1000);
                if (delta != 0) { cps = total_count/delta;}
               //
                 mergeDataToFile(data_temp);//save all data in file __ turn on it only when it's need
                    //Log.d(TAG, "dummy_data: delta"+delta);


            }
        }
        private boolean mergeDataToFile(byte[] bytes){    // merge Data to file "AllDataFile.bin"
            File file ;
            FileInputStream fis = null;
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            try {

                File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                file = new File(dir, "AllDataFile.bin");
                //if(file.exists()&&cps==0){file.delete();}
                if (file.exists()){
                    fos = new FileOutputStream(file,true);
                    bos = new BufferedOutputStream(fos);
                    file.createNewFile();
                    bos.write(bytes);
                }else {
                    fos = new FileOutputStream(file);
                    bos = new BufferedOutputStream(fos);
                    file.createNewFile();
                    bos.write(bytes);
                    bos.close();
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {

                    if (bos != null) {
                        bos.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
    }


    /**
     * multi-thread ____ HandleThread
     */
    class childCallback implements Handler.Callback{

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            ByteArrayOutputStream  os = (ByteArrayOutputStream)msg.obj;
            byte[] data = os.toByteArray();
            if (the_firsttime_datainput == 0) {
                the_firsttime_datainput = System.currentTimeMillis();
                Log.d(TAG, "dummy_data:response time = "+ Integer.toString((int)(the_firsttime_datainput - data_generation_finish)));
            }
            //mergeDataToFile(data);

            int data_len = ((((int) data[3] & 0xff) << 24) |                                                 //head(4byte)  content: how many byte
                    (((int) data[2] & 0xff) << 16) |
                    (((int) data[1] & 0xff) << 8) |
                    (((int) data[0] & 0xff)));
            if(data_len > 4){
                int temp=(data_len-4)/16;
                byte[] Data_temp = byteArrayToInt(data, data_len);
                time_input_count(ch_data, temp);
                count(temp);
                total_count += temp;
                mergeDataToFile(Data_temp);
            }
            long inputtime = System.currentTimeMillis();
            int delta = (int) ((inputtime - the_firsttime_datainput)/1000);

            if (delta != 0) { cps = (int) (total_count/delta);}
           // Log.d(TAG, "delta: " + Integer.toString(delta) + "  s; cps:" + Integer.toString(cps) + "  counts/s; counts:" + Integer.toString(total_count));
           // Log.d(TAG, "dummy_data: delta"+delta);

            return false;
        }
    }




    //from DJI MSDK
    private KeyListener getDataListener = new KeyListener() {
        @Override
        public void onValueChange(@Nullable Object oldValue, @Nullable final Object newValue) {
            if (pushTextView != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (newValue instanceof byte[]) {
                            String str = Helper.getString((byte[]) newValue);
                            pushTextView.setText(str + "\n");
                            pushTextView.invalidate();
                        }
                    }
                });
            }
        }
    };




 /**   I think it is no need! But I didnt delete just in case..............
  *
  */ private KeyListener getNameListener = new KeyListener() {
        @Override
        public void onValueChange(@Nullable Object oldValue, @Nullable final Object newValue) {
            if (pushTextView != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (newValue instanceof String) {
                            payloadName = newValue.toString();
                            payloadNameView.setText("Payload Name:"
                                    + (TextUtils.isEmpty(payloadName) ? "N/A" : payloadName));
                            payloadNameView.invalidate();
                        }
                    }
                });
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(isIntent){
            intent = new Intent(this,MyIntentService.class);//HandleThread
        }else {
            /**HandleThread */
            mHandlerThread = new HandlerThread("handlerThread");
            mHandlerThread.start();
            workHandler = new Handler(mHandlerThread.getLooper(), new childCallback());
            mainHandler = new Handler(getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                }
            };

        }

        /** app initialization **/
        //from DJI MSDK
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_default_widgets);
        height = DensityUtil.dip2px(this, 100);
        width = DensityUtil.dip2px(this, 150);
        margin = DensityUtil.dip2px(this, 12);
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);         //
        final Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        display.getRealSize(outPoint);
        deviceHeight = outPoint.y;
        deviceWidth = outPoint.x;

        gamma_connection_status = (TextView) findViewById(R.id.Connection_Status);
        gammaView =  findViewById(R.id.gammaview);
        gamma_frame = (FrameLayout) findViewById(R.id.gamma_frame);
        gamma_frame.setTranslationY(227370/10000);

        /** google map defalt config / initialization **/
        //wsh
        mapWidget = findViewById(R.id.map_widget);
        mapWidget.initGoogleMap(new MapWidget.OnMapReadyListener() {
            final DJILatLng latLng = new DJILatLng(37.5858621,127.0247502);  // seoul's LatLng
            final DJICameraPosition cameraPosition = new DJICameraPosition(latLng,18,0,0);
            final DJICameraUpdate cameraUpdate= DJICameraUpdateFactory.newCameraPosition(cameraPosition);
            @Override

            public void onMapReady(@NonNull DJIMap map) {
                map.moveCamera(cameraUpdate);
                map.setOnMapClickListener(new DJIMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(DJILatLng latLng) {
                        onViewClick(mapWidget);
                    }
                });
            }
        });
        mapWidget.onCreate(savedInstanceState);

        /** fpv labbbvyout initialization **/
        // parts of them from wsh
        parentView = (ViewGroup) findViewById(R.id.root_view);
        fpvWidget = findViewById(R.id.fpv_widget);
        fpvWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //  onViewClick(fpvWidget);
            }
        });
        primaryVideoView = (RelativeLayout) findViewById(R.id.fpv_container);
        secondaryVideoView = (FrameLayout) findViewById(R.id.secondary_video_view);
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);
        secondaryFPVWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onViewClick(secondaryFPVWidget);
                //   swapVideoSource();
            }
        });
        resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);//add display bitmap

        // DJI payload name printing.  no need !!
        payloadNameView = (TextView) findViewById(R.id.payload_name);
        pushTextViewFromUsb = (TextView) findViewById(R.id.push_info_text_usb);

        /** reading segmentation map  **/
        //wsh
        try {
            if (scint_height == 29 || scint_height == 38) {
                /** making segmap format "csv "*/
                readcsv();
            } else {
                if (scint_height == 23 || scint_height == 20) {
                    /** making segmap format "bin "*/
                    read_segmap(this);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // from DJI MSDK
        updateprimaryVideoVisibility();
        initListener();
        readcolorbar();
        window_flag = 0;

        /**only for virtual data*/
        /**choose A virtual data*/
        //wsh
        String filename1 = "Ba133_3.5M23x23_Energy.bin";//60.5M         scint_size → 23x23
        String filename2 = "Cs137_1.5M23x23_Energy.bin";//27M

        String filename3 = "cs137_10000K.bin";//174M        defult segmap   scint_size → 29x29
        String filename4 = "ba133_9000K.bin";//153M

        String filename5 = "Ba133_for_Ehisto.bin";//8M        defult segmap  scint_size → 20x20
        String filename6 = "Cs137_for_Ehisto.bin";//11M
//------------------------------------------------------------------------------------------------

        // int lenth = read_testdata(this, all_virtual_data, filename5);//only when reading bin data
//-----------------------------------------------------------------------------------------------
        // when calculating a &　b
        // float[] var = getcorrect();//var[0]=a;var[1]=b; //Correct Energy Use
//----------------------------------------------------------------------------------------------

        //debug_time_segmap_img(this);// only when reading bin data
//----------------------------------------------------------------------------------------------

        /**histogram initialization */
        //wsh
        Xaxis();//preinstall energy interval and selection ;
        chart = findViewById(R.id.lineChart);
        histogram = (ConstraintLayout) findViewById(R.id.histogram);

        RelativeLayout.LayoutParams params_init= new RelativeLayout.LayoutParams(histogram.getLayoutParams());
        RelativeLayout.LayoutParams params_big= new RelativeLayout.LayoutParams(histogram.getLayoutParams());
        histogram.setLayoutParams(params_init);

        params_big.width= 1600;
        params_big.height= 900;
        params_big.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE);

        //params_init.height= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,243,getResources().getDisplayMetrics());
        params_init.height = 438;
        params_init.width= 438; // 498
        params_init.addRule(RelativeLayout.BELOW,R.id.gamma_inf);
        params_init.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params_init.rightMargin = 30;
        params_init.leftMargin = 30;
        params_init.topMargin =40;


        XAxis x = chart.getXAxis();
        x.setTextColor(Color.BLACK);
        x.setDrawGridLines(false);//��X�S��
        x.setPosition(XAxis.XAxisPosition.BOTTOM);//�ј˻`�ŵײ�
        x.setLabelCount(4, true);//�O���@ʾ20���˻`
        x.setAxisMaximum(maxRange / 1000);
        initChart();
        startRun();

        histogram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (window_flag == 0) {




                    histogram.setLayoutParams(params_big);
                    XAxis x = chart.getXAxis();

                    x.setTextColor(Color.BLACK);
                    x.setDrawGridLines(false);//��X�S��
                    x.setPosition(XAxis.XAxisPosition.BOTTOM);//�ј˻`�ŵײ�
                    x.setLabelCount(16, true);//�O���@ʾ20���˻`
                    x.setAxisMaximum(maxRange / 1000);
                    initChart();
                    startRun();

                    window_flag = 1;
                } else {
                    XAxis x = chart.getXAxis();
                    x.setTextColor(Color.BLACK);
                    x.setDrawGridLines(false);//��X�S��
                    x.setPosition(XAxis.XAxisPosition.BOTTOM);//�ј˻`�ŵײ�
                    x.setLabelCount(4, true);//�O���@ʾ20���˻`
                    x.setAxisMaximum(maxRange / 1000);
                    histogram.setLayoutParams(params_init);
                    window_flag = 0;
                }
            }

        });

        /**cps update thread*/
        //wsh
        controlling_distance();
        cotrolling_transparency();
        controlling_colorbar();
        trans_controller.setProgress(gamma_img_trans);
        colorbar_controller.setProgress(color_position);
        dist_controller.setProgress(dist_position);
        dis_txt=findViewById(R.id.distance_text);
        dist_controller.setTranslationY(200);
        /** energy window: doubleseekbar **/
        //wsh
        final TextView txtLeft = findViewById(R.id.text_left);
        final TextView txtRight = findViewById(R.id.text_right);
        DoubleSeekbar doubleSeekbar = findViewById(R.id.double_seekbar);
        doubleSeekbar.setMinValue(0);
        doubleSeekbar.setMaxValue(1500);
        txtLeft.setText(String.valueOf(0)+"KeV");
        txtRight.setText(String.valueOf(1500)+"KeV");
        doubleSeekbar.setOnChanged(new DoubleSeekbar.OnChanged() {
            @Override
            public void onChange(int leftValue, int rightValue) {
                energywin_1=leftValue;
                energywin_2=rightValue;
                txtLeft.setText(String.valueOf(leftValue)+"KeV");
                txtRight.setText(String.valueOf(rightValue)+"KeV");
            }
        });
        // random_dummy_data_test();
    }

    /** zoom in/out map widget **/
    //wsh
    private void onViewClick(View view) {
        if (view == secondaryFPVWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = true;
            findViewById(R.id.Connection_Status).setVisibility(View.VISIBLE);
            histogram.setVisibility(View.VISIBLE);

        } else if (view == mapWidget && isMapMini) {
            hidePanels();
            resizeFPVWidget(width, height, margin, 12);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = false;
            histogram.setVisibility(View.INVISIBLE);
        }
    }

       /** From DJI MSDK **/
    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams) secondaryVideoView.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.rightMargin = margin;
        fpvParams.bottomMargin = margin;
        if (isMapMini) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }
        secondaryVideoView.setLayoutParams(fpvParams);

        parentView.removeView(secondaryVideoView);
        parentView.addView(secondaryVideoView, fpvInsertPosition);
    }

    private void reorderCameraCapturePanel() {
        findViewById(R.id.secondary_fpv_widget).setVisibility(View.VISIBLE);
        findViewById(R.id.secondary_video_view).setVisibility(View.VISIBLE);
        View cameraCapturePanel = findViewById(R.id.CameraCapturePanel);
        parentView.removeView(cameraCapturePanel);
        parentView.addView(cameraCapturePanel, isMapMini ? 9 : 13);

    }
    private void updateprimaryVideoVisibility() {
        if (fpvWidget.getVideoSource() == null) {
            fpvWidget.setVisibility(View.GONE);
        } else {
            fpvWidget.setVisibility(View.VISIBLE);
        }
    }

    private void hidePanels() {
        findViewById(R.id.secondary_fpv_widget).setVisibility(View.INVISIBLE);
        findViewById(R.id.secondary_video_view).setVisibility(View.INVISIBLE);
        //findViewById(R.id.payload_name).setVisibility(View.INVISIBLE);
        findViewById(R.id.Connection_Status).setVisibility(View.INVISIBLE);
        //These panels appear based on keys from the drone itself.
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.HISTOGRAM_ENABLED), false, null);
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.COLOR_WAVEFORM_ENABLED), false, null);
        }

        //These panels have buttons that toggle them, so call the methods to make sure the button state is correct.
        CameraControlsWidget controlsWidget = findViewById(R.id.CameraCapturePanel);
        controlsWidget.setAdvancedPanelVisibility(false);
        controlsWidget.setExposurePanelVisibility(false);

        //These panels don't have a button state, so we can just hide them.
        findViewById(R.id.pre_flight_check_list).setVisibility(View.GONE);
        findViewById(R.id.rtk_panel).setVisibility(View.GONE);
        findViewById(R.id.spotlight_panel).setVisibility(View.GONE);
        findViewById(R.id.speaker_panel).setVisibility(View.GONE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mapWidget.onResume();

    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
        int his_with = histogram.getWidth();
        Log.d(TAG, "onDestroy: hiswith = "+his_with+",deviceheight = "+deviceHeight+"getTop"+gamma_frame.getTop());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    private class ResizeAnimation extends Animation {

        private View mView;
        private int mToHeight;
        private int mFromHeight;

        private int mToWidth;
        private int mFromWidth;
        private int mMargin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            mMargin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = mMargin;
            p.bottomMargin = mMargin;
            mView.requestLayout();
        }
    }

    private void initListener() {

        getDataKey = PayloadKey.create(PayloadKey.GET_DATA_FROM_PAYLOAD);
        payloadNameKey = PayloadKey.create(PayloadKey.PAYLOAD_PRODUCT_NAME);
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().addListener(getDataKey, getDataListener);
            KeyManager.getInstance().addListener(payloadNameKey, getNameListener);
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.MODE),
                    SettingsDefinitions.CameraMode.SHOOT_PHOTO, null);
        }




        //wsh
        new Payload().setStreamDataCallback((bytes, i) -> {
            updateGammaConnectionStatus();
            /** receiving data **/
            if(isIntent){
                intent.putExtra("dating",bytes);//InentService
                startService(intent);
            }else {

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    os.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Message msg1 = new Message();
                msg1.obj = os;
                workHandler.sendMessage(msg1);

            }
        });

        //about Payload name. I think it's no use. Remain it just in case..................................
        Object name = KeyManager.getInstance().getValue(payloadNameKey);
        if (name != null) {
            payloadName = name.toString();
            payloadNameView.setText("Payload Name:" + (TextUtils.isEmpty(payloadName) ? "N/A" : payloadName));
            payloadNameView.invalidate();
        }

        /** receiving data __ by cheolung Kang **/
        /*
       DJIPayloadUsbDataManager.getInstance().setDataListener(new DJIPayloadUsbDataManager.PayloadUsbDataListener() {
           @Override
            public void onDataInput(byte[] data, int len) {
               intent.putExtra("dating",data);
               startService(intent);
            }
      });
*/
    }

    /** segmentation maping **/
    //wsh
    private double makefloodimg(int[][] ch, int data_row) {
        //channel data? ?? Anger logic ??

        for (int i = 0; i < (data_row - 1); i++) {

            double x = (double) (ch[i][1] - ch[i][0]) / (double) (ch[i][0] + ch[i][1]);
            double y = (double) (ch[i][2] - ch[i][3]) / (double) (ch[i][2] + ch[i][3]);


            if (x > -1 && x < 1 && y > -1 && y < 1) {
                int x_idx = (int) Math.round((511 * (x + 1) / 2));                                                //make index from -1~1   to  0~511
                int y_idx = (int) Math.round((511 * (y + 1) / 2));
                int seg_idx = 0;
                //flood_result[x_idx][y_idx] += 1;
                if (y_idx > 511 || x_idx > 511) {
                    Log.d(TAG, "makefloodimg: idx_out");
                } else {
                    seg_idx = segmap[x_idx][y_idx] - 1;
                }
                //int seg_idx = segmap[x_idx][y_idx];
                int x_idx_seg = seg_idx / scint_width;
                int y_idx_seg = seg_idx % scint_height;
                if (y_idx_seg >= scint_height || x_idx_seg >= scint_width) {
                    Log.d(TAG, "makefloodimg: idx_seg_out");
                } else {
                    segmap_img[x_idx_seg][y_idx_seg] += 1;
                }
            }
        }
        return 2.0;
    }

    /*******************************************************************************************/
    /** transfer byte to Int **/
    //wsh
    private static byte[] byteArrayToInt(byte[] data_temp, int data_len) {                                               //put the data in ch_data[],and return the 'count'
        byte[] data2 = new byte[data_len];
        for (int s=0;s<data_len;s++) {
           if(s<=data_temp.length) {
               data2[s]= data_temp[s];
           }else{
               data2[s]= 0;//如果接受的数组长度小于data_len则用　0　填充成data_len 长度。
           }
        }
        for (int i = 0; i < (data_len - 4) / 16; i++) {                                                               //length-4(head)/every count's 16(4chennal*4byte)byte=the whole count
            int offset = i * 16;                                                                       //offset: count's every chennal data use 4chennal*4byte=16byte

            ch_data[i][0] = ((((int) data2[7 + offset] & 0xff) << 24) |                                        //chennal[0]'s 4byte data is put after 4byte head
                    (((int) data2[6 + offset] & 0xff) << 16) |
                    (((int) data2[5 + offset] & 0xff) << 8) |
                    (((int) data2[4 + offset] & 0xff)));

            ch_data[i][1] = ((((int) data2[11 + offset] & 0xff) << 24) |
                    (((int) data2[10 + offset] & 0xff) << 16) |
                    (((int) data2[9 + offset] & 0xff) << 8) |                                                  //ps: data[]'s data:  head-count[0]chennal[0]-count[0]chennal[2]����  ��������������(continously)
                    (((int) data2[8 + offset] & 0xff)));                                                       //ch_data4 is devided into 4 group  : ch_data[count number][chennel number]

            ch_data[i][2] = ((((int) data2[15 + offset] & 0xff) << 24) |
                    (((int) data2[14 + offset] & 0xff) << 16) |
                    (((int) data2[13 + offset] & 0xff) << 8) |
                    (((int) data2[12 + offset] & 0xff)));

            ch_data[i][3] = ((((int) data2[19 + offset] & 0xff) << 24) |
                    (((int) data2[18 + offset] & 0xff) << 16) |
                    (((int) data2[17 + offset] & 0xff) << 8) |
                    (((int) data2[16 + offset] & 0xff)));

            energy[i] = (float) ((ch_data[i][0] + ch_data[i][1] + ch_data[i][2] + ch_data[i][3]) * a + b);
        }
        return data2;
    }

    protected void updateUsbPushData(final String str) {
        if (pushTextViewFromUsb != null) {
            runOnUiThread(new Runnable() {                                                                           //update the ui in the main thread layout
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    //if (cps_flag==1){
                    pushTextViewFromUsb.setText(" 계수율: "+str+"cps");
                    pushTextViewFromUsb.invalidate();
                    //}                                                           //draw this view again
                }
            });
        }
    }

    private void updateGammaConnectionStatus() {                                                        //update the ui in the main thread layout
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gamma_connection_status.setText("Connected");
                gamma_connection_status.invalidate();
                gamma_connection_status.setTextColor(Color.GREEN);
            }
        });

    }

    /** when segmentation map is .csv file use this **/
    private void readcsv() throws Exception {// function: put the data in R.raw.segmentationmap to segmap[row][ i ]

        try {
            InputStreamReader is = null;
            if (scint_height == 29) {
                is = new InputStreamReader(getResources().openRawResource(R.raw.segmap_ba133_29x29));   //read the raw file
            } else {
                is = new InputStreamReader(getResources().openRawResource(R.raw.segmentationmap_38x38));
            }
            BufferedReader reader = new BufferedReader(is);                                                         //read the data in raw

            CSVReader read = new CSVReader(reader);                                                                //CSV (Comma Separated Values)read 'reader' in string type

            String[] line;
            String[] line_2;

            int row = 0;

            while ((line = read.readNext()) != null) {                                                             //read the 'read' in line to 'line'
                for (int i = 0; i < 512; i++)
                    segmap[row][i] = Integer.parseInt(line[i]);                              //put the string data to segmap[row][i] in int type

                row++;
            }

//            while ((line_2 = read_2.readNext()) != null){
//                int j=0;
//                VirtualData_test[j] = Integer.parseInt(line_2[j]);
//                j+=1;
//            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Can not found", Toast.LENGTH_SHORT).show();
        }

    }


    //wsh
    private void readcolorbar() {
        byte[] bardata;
        try {
            InputStream is = this.getResources().openRawResource(+R.drawable.colorbar);
            ByteArrayOutputStream os = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            bardata = os.toByteArray();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            mColorbitmap = BitmapFactory.decodeByteArray(bardata, 0, bardata.length, options);
            mColorbar = new int[mColorbitmap.getHeight()];

            for (int i = 0; i < mColorbitmap.getHeight(); i++) {
                int color = mColorbitmap.getPixel(0, i);
                //if (i<40){color=0; }
                mColorbar[i] = color;

                int a = Color.alpha(color);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                //int a = Color.alpha(color);
                Log.d(TAG, "readColorbar  a=" + a + "r=" + r + "g=" + g + "b=" + b);
            }
        } catch (IOException e) {
            Log.e(TAG, "load colorbar exception");
        }

    }
        //wsh
    private int getMax(int[][] A) {
        int max = 0;
        int min = 0;
        if (A != null) {
            for (int i = 0; i < A.length; i++) {
                if (A != null) {
                    for (int j = 0; j < A[i].length; j++) {
                        if (max < A[i][j]) {
                            max = A[i][j];
                        }
                        if (min > A[i][j]) {
                            min = A[i][j];
                        }
                    }
                }
            }
        }
        if (max != 0) return max;
        else return 1;
    }


    //wsh
    private Bitmap makingbitmap() {

        final int SIZE = gammaViewwidth;
        final int SEGS = scint_width;
        int maxValue = getMax(segmap_img);
        final float delta = SIZE / SEGS;
        Bitmap mBitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(mBitmap);
        canvas.drawBitmap(mBitmap,0,0, null);
        canvas.translate(25, 25);
        canvas.drawColor(Color.TRANSPARENT);
        paint.setStrokeWidth(delta);
        int[] mcolorbar_temp= new int[500];
        for(int i=0;i<500;i++) {
            mcolorbar_temp[i] = mColorbar[i];
        }
        for (int i = 0; i < scint_height; i++) {
            for (int j = 0; j < scint_width; j++) {
                int colorIndex = (segmap_img[j][i] * (mColorbitmap.getHeight() - 1)) / maxValue;
                if (colorIndex>=color_position&&colorIndex!=0&&colorIndex < 500) {
                    paint.setColor(mcolorbar_temp[colorIndex]);
                    paint.setAlpha(gamma_img_trans);
                    canvas.drawPoint(j * delta, i * delta, paint);
                }
            }

        }
        return mBitmap;
    }

    /**
     * loding histogram
     */
    //wsh
    private void initChart() {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);//�O�ò��ɻ���
        chart.setDrawBorders(true);
        Legend l = chart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);
        YAxis y = chart.getAxisLeft();
        y.setTextColor(Color.BLACK);
        y.setSpaceTop(5);
        y.setAxisMinimum(0);
        chart.getAxisRight().setEnabled(false);
    }

    /**
     * setting data
     */
//wsh
    private LineDataSet createSet() {
        List<Entry> list = new ArrayList<>();
        for (int i = 0; i < division; i++) {
            list.add(new Entry(Xaxis[i] / 1000, Ycounts[i]));
        }
        LineDataSet lineDataSet = new LineDataSet(list, "Energy/KeV");
        lineDataSet.setColor(Color.BLACK);
        lineDataSet.setLineWidth(1);
        lineDataSet.setFillColor(Color.RED);
        lineDataSet.setFillAlpha(50);
        lineDataSet.setDrawFilled(false);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setValueTextColor(Color.BLACK);
        return lineDataSet;
    }
    /**
     * displaying histogram and gamma image
     */
    //wsh
    private void startRun() {
        if (isRunning) return;
        if (thread != null) thread.interrupt();
        isRunning = true;
        @SuppressLint("ResourceAsColor") Runnable runnable = () -> {
            long currenttime= System.currentTimeMillis();
            updateUsbPushData(Integer.toString(cps));
            if(currenttime-the_lasttime_datainput>1000 && gamma_connection_status.getText()!="Disconnected"){
                gamma_connection_status.setText("Disconnected");
                gamma_connection_status.setTextColor(Color.RED);
            }
            time_get_segmap(30);
            Bitmap bit_temp = makingbitmap();
            gammaView.setImageBitmap(bit_temp);
            LineDataSet set = createSet();
            LineData line = new LineData(set);
            chart.setData(line);
            chart.invalidate();
        };

        thread = new Thread(() -> {
            while (isRunning) {
                runOnUiThread(runnable);
                if (!isRunning) break;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    //wsh
    public int read_testdata(Context c, byte[] data2, String filename) {//read BIN file`s 8000 BYTE
        AssetManager manager = c.getResources().getAssets();
        try {
            InputStream inputStream = null;
            inputStream = manager.open(filename);
            int length = inputStream.available();
            int remainbyte = 0;
            int readfiletimes = length / 64000000;
            int remain_data = length % 64000000;
            switch (readfiletimes) {
                case 0:
                    inputStream.read(data2);//reead 64M byte data ;
                    remainbyte = displaycount(length, 0);//no remain;
                    break;
                case 1: //how many times virtual data has been read
                    inputStream.read(data2);//reead 64M byte data
                    remainbyte = displaycount(64000000, 0);
                    length -= 64000000;
                    inputStream.read(data2);
                    remainbyte = displaycount(length, remainbyte);//has remain
                    break;
                case 2:
                    inputStream.read(data2);
                    remainbyte = displaycount(64000000, 0);
                    length -= 64000000;
                    inputStream.read(data2);
                    remainbyte = displaycount(64000000, remainbyte);
                    length -= 64000000;
                    inputStream.read(data2);
                    remainbyte = displaycount(length, remainbyte);
                    break;
            }
            inputStream.close();
            inputStream.close();
            inputStream = null;

            return length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //wsh
    public int displaycount(int length, int remainbytes) {
        data_len = 0;
        total_data = 0;
        while (total_data + data_len <= length) {

            for (int i = 0; i < 4; i++) {
                if (remainbytes != 0) {
                    ch[i] = remain_virtual_data[i];
                } else {
                    ch[i] = all_virtual_data[i + total_data];
                }
            }
            data_len = ((((int) ch[3] & 0xff) << 24) | //head(4byte)  content: how many byte
                    (((int) ch[2] & 0xff) << 16) |
                    (((int) ch[1] & 0xff) << 8) |
                    (((int) ch[0] & 0xff)));
            if (remainbytes != 0) {
                if (remainbytes > data_len) {
                    // remainbytes -= data_len;
                    for (int k = 0; k < data_len; k++) {
                        ch[k] = remain_virtual_data[k];
                    }
                } else {
                    for (int k = 0; k < remainbytes; k++) {
                        ch[k] = remain_virtual_data[k];
                    }
                    for (int j = remainbytes; j < data_len; j++) {
                        ch[j] = all_virtual_data[j - remainbytes];
                    }
                }
            } else {
                for (int m = 0; m < data_len; m++) {

                    ch[m] = all_virtual_data[total_data + m];
                }
            }
            byteArrayToInt(ch, data_len);//total_data: start point ， data_len：input byte lenth ;
      //      makefloodimg(ch_data, (1 + data_len - 4) / 16); //original lose a count;
            count((data_len - 4) / 16);
            time_input_count(ch_data, (data_len - 4) / 16);

            if (remainbytes > data_len) {
                total_data = 0;
                remainbytes -= data_len;
            } else {
                if (remainbytes != 0) {
                    total_data += data_len - remainbytes;
                    remainbytes = 0;
                } else {
                    total_data += data_len;
                }
            }

        }
        for (int n = 0; n < length - total_data; n++) {
            remain_virtual_data[n] = all_virtual_data[total_data + n];
        }
        if (length - total_data < 10) {
            return 0;
        } else {
            return length - total_data;
        }
    }

    public float[] halfheightwidth( float Maxcount , int j ){
        int interval = maxRange / division;
        int i = 0;
        int m = 0;//上、下移下标数
        float rightenergy =0;
        float leftenergy = 0;

        float maxenergy = Xaxis[j];
//leftenergy。
        m = j;
        for (i=j;i >= 0;i--){
            if (Ycounts[i] > Maxcount/2){
                m = i;
            }else{
                leftenergy = Xaxis[i] + interval*(Maxcount/2- Ycounts[i])/(Ycounts[m] - Ycounts[i]);
                break;
            }

        }
// rightenergy。
        m=j;
        for (i=j;i<Ycounts.length;i++){
            if (Ycounts[i] > Maxcount/2){
                m = i;
            }else{
                rightenergy = Xaxis[i] - interval*(Maxcount/2- Ycounts[i])/(Ycounts[m] - Ycounts[i]);
                break;
            }
        }

        return new float[]{leftenergy, rightenergy,maxenergy};
    }
    //wsh
    public  float[] averageEnergy(int type){
        float[] maxenergy;
        float averageEnergy = 0 ;
        float temp = 0;
        int n = 0 ;
        if(type == 0 ){
            maxenergy = TestMaxEn();
        }else{
            maxenergy = lastMaxEn();}
        float[] RLenergy = halfheightwidth(maxenergy[1],(int)maxenergy[2]);

        return new float[] {RLenergy[0],RLenergy[1],maxenergy[0]} ;
    }
    //wsh
    private float[] TestMaxEn() {
        float MaxY = 0;
        int j = 0;
        int i = 0;
        for (i = 0; i < Ycounts.length; i++) {
            if (MaxY < Ycounts[i]) {
                MaxY = Ycounts[i];
                j = i;
            }
        }
        float MaxX = Xaxis[j];
        return new float[]{MaxX, MaxY,j };
    }
    //wsh
    private float[] lastMaxEn() {
        float MaxY = 0;
        int j = 0;
        int i = 0;

        int leftbide;
        if(a==1){leftbide = 109000;}else{leftbide = 661657 ;}                   // when finding the second peak .(Cs_137) change it when u got new source.
        for (j = 0 ;j < division ; j++){
            if(Xaxis[j] > leftbide - 2*(maxRange / division)){
                i = j;
                break;
            }
        }
        j = i;


        for (i = j; i < division; i++) {
            if (MaxY < Ycounts[i]) {
                MaxY = Ycounts[i];
                j = i;
            }
        }
        float MaxX = Xaxis[j];
        return new float[]{MaxX, MaxY , j};
    }

    //wsh
    public float[] getcorrect() {
        float averageEn1;
        float averageEn2;
        float[] var;
        String filename1 = "Cs137_for_Ehisto.bin";//11M
        String filename2 = "Ba133_for_Ehisto.bin";//8M      scint_size → 20x20
        data_len = read_testdata(this, all_virtual_data, filename1);
        var = averageEnergy(1);//cs137
        averageEn1 = var[2];

        float maxvar = var[2];
        float fwhmL = var[0];
        float fwhmR = var[1];
        float E0CS137 = 661657f;

        data_len = read_testdata(this, all_virtual_data, filename2);
        var = averageEnergy(0);//ba133
        averageEn2 = var[2];

        maxvar = var[2];
        fwhmL = var[0];
        fwhmR = var[1];
        float EoBa133 = 356013f;

        float X1 = averageEn1;//cs137
        float X2 = averageEn2;//ba133

        float Y1 = 661657f;//cs137
        float Y2 = 356013f;//ba133

        float a1 = (Y2 - Y1) / (X2 - X1);//y = aX+b    Y1 Y2 are true energy ，X1 X2 are Measurements
        float b1 = (Y1 + Y2 - a1 * (X1 + X2)) / 2;
        a = a*a1;b=a1*b+b1;//y=a1(ax+b)+b1=a1*ax+a1*b+b1;
        Log.d(TAG, "getcorrect:a=" + a + "b=" + b);
        return new float[]{a, b};
    }
    //wsh
    public float[] Xaxis() {
        float interval = maxRange / division;
        for (int i = 0; i < division; i++) {
            Xaxis[i] = i * interval;
        }
        return Xaxis;
    }
    //wsh
    public static void count(int count) {
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < division + 1; j++) {
                if ((energy[i] >= Xaxis[j]) & (energy[i] < Xaxis[j + 1])) {
                    Ycounts[j] += 1;
                }
            }
        }
    }
    //wsh
    private void read_segmap(Context c) {
        AssetManager manager = c.getResources().getAssets();
        try {
            InputStream inputStream = null;
            if (scint_height == 23) {
                inputStream = manager.open("segmapstaionmap_23x23.bin");
            } else {
                inputStream = manager.open("SegMap_DRIS_20x20_Ba133.bin.new.bin");
            }

            byte[] data = new byte[512 * 512 * 4 + 12];//1048588 byte
            int length = inputStream.available();
            length = inputStream.read(data);

            int temp[] = new int[512 * 512 + 3];//262147 int
            for (int m = 0; m < length / 4; m++) {
                int offset = m * 4;
                temp[m] = ((((int) data[3 + offset] & 0xff) << 24) |                                                 //head(4byte)  content: how many byte
                        (((int) data[2 + offset] & 0xff) << 16) |
                        (((int) data[1 + offset] & 0xff) << 8) |
                        (((int) data[offset] & 0xff)));
            }
            for (int m = 0; m < 512; m++) {
                for (int n = 0; n < 512; n++) {
                    segmap[m][n] = temp[m * 512 + (n + 3)];
                }
            }

            inputStream.close();
            inputStream.close();
            inputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /** storage time & data **/
    //wsh
    private static void time_input_count(int[][] ch, int data_len) {
        long inputtime = System.currentTimeMillis();
        int j = 0;
        int dataLeng = total_ch_Time.length;
        timetotal_data += data_len;
        if(timetotal_data >=dataLeng){timetotal_data = dataLeng;}
        for (int i = 0; i < timetotal_data; i++) {
            if (inputtime - total_ch_Time[i] > 1 * 60 * 1000) {
                total_ch_data[i][0] = ch[j][0];
                total_ch_data[i][1] = ch[j][1];
                total_ch_data[i][2] = ch[j][2];
                total_ch_data[i][3] = ch[j][3];
                total_ch_Time[i] = System.currentTimeMillis();
                j += 1;
                if (j >= data_len) {
                   break;
                }
            }

        }
    }


    /** accumulate time **/
    //wsh
    private void time_get_segmap(int seconds) {
        long inputtime = System.currentTimeMillis();
        int j = 0;
        int dataLeng = total_ch_Time.length;
        int[][] chtime = new int[dataLeng][4];
        try{
            if(energywin_1>energywin_2){energywin_1 =0;
                updateUsbPushData("Rang error");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if(energywin_1 !=0 && energywin_2 !=0 ) {
            for (int i = 0; i < timetotal_data; i++) {
                if (inputtime - total_ch_Time[i] < seconds * 1000) {
                    float temp = a*(total_ch_data[i][0]+total_ch_data[i][1]+total_ch_data[i][2]+total_ch_data[i][3]) + b ;
                    if( temp > energywin_1*1000 && temp < energywin_2*1000) {
                        chtime[j][0] = total_ch_data[i][0];
                        chtime[j][1] = total_ch_data[i][1];
                        chtime[j][2] = total_ch_data[i][2];
                        chtime[j][3] = total_ch_data[i][3];
                        j += 1;
                        //if (j > dataLeng || timetotal_data + j >= dataLeng) {
                        if (j > dataLeng) {
                            break;
                        }
                    }
                }
            }
        }else{
            for (int i = 0; i < timetotal_data; i++) {
                if (inputtime - total_ch_Time[i] < seconds * 1000) {
                    chtime[j][0] = total_ch_data[i][0];
                    chtime[j][1] = total_ch_data[i][1];
                    chtime[j][2] = total_ch_data[i][2];
                    chtime[j][3] = total_ch_data[i][3];
                    j += 1;
                    if (j > dataLeng ) {
                        break;
                    }
                }
            }
        }
        for (int m = 0; m < scint_width; m++) {
            for (int n = 0; n < scint_height; n++) {
                segmap_img[m][n] = 0;
            }
        }
        double len = makefloodimg(chtime, j+1);
    }

    //for virtual data
    //wsh
    private void debug_time_segmap_img(Context c) {

        all_total_data=0;
        cps = 0 ;
        total_count=0;
        the_firsttime_datainput = 0 ;

        String filename1 = "Ba133_for_Ehisto.bin";//8M  0  3000   defult segmap  scint_size → 20x20
        String filename2 = "Cs137_for_Ehisto.bin";//11M   8000
        String filename3 = "DATA20211112.bin";
        long inputtime = System.currentTimeMillis();
        AssetManager manager = c.getResources().getAssets();
        InputStream inputStream = null;
        try {
            inputStream = manager.open(filename2);
            int length = inputStream.available();
            byte[] data = new byte[length];
            length=inputStream.read(data);

            for (int j  = 0; j < 100000 ;j++){

                if(the_firsttime_datainput==0){the_firsttime_datainput=System.currentTimeMillis();}
                int data_len = ((((int) data[3+all_total_data] & 0xff) << 24) |                                                 //head(4byte)  content: how many byte
                        (((int) data[2+all_total_data] & 0xff) << 16) |
                        (((int) data[1+all_total_data] & 0xff) << 8) |
                        (((int) data[0+all_total_data] & 0xff)));

                byte[] data2 = new byte[data_len];
                for(int i = 0 ; i < data_len ; i++){data2[i] = data[i+all_total_data];}

                /** receiving data **/
                if(isIntent){
                    intent.putExtra("dating",data2);//InentService
                    startService(intent);//InentService
                }else {
                    //handlerthread
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        os.write(data2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Message msg1 = new Message();
                    msg1.obj = os;
                    workHandler.sendMessage(msg1);

                }

                all_total_data += data_len;
                if(all_total_data + 4 > length){Toast.makeText(getApplicationContext(),Integer.toString(j), Toast.LENGTH_SHORT).show();break;}
            }
            inputStream.close();
            inputStream.close();
            inputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //wsh
    private boolean mergeDataToFile(byte[] bytes){
        File file ;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            //创建标准目录
           File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            file = new File(dir, "AllDataFile.bin");
           // if(file.exists()&&cps==0){file.delete();}
            //if (file.exists()&&cps!=0){
                if (file.exists()){
                fos = new FileOutputStream(file,true);
                bos = new BufferedOutputStream(fos);
                file.createNewFile();
                bos.write(bytes);
            }else {
                fos = new FileOutputStream(file);
                bos = new BufferedOutputStream(fos);
                file.createNewFile();
                bos.write(bytes);
                bos.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {

                if (bos != null) {
                    bos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    //wsh
    private void cotrolling_transparency(){
        trans_controller = (SeekBar) findViewById(R.id.transparency_controller);
        //txt_cur = (TextView) findViewById(R.id.txt_cur);
        trans_controller.setMax(255);
        trans_controller.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
              //  txt_cur.setText("transparency" + progress + "  / 100 ");
                gamma_img_trans=progress;
                Bitmap bit_temp = makingbitmap();
                gammaView.setImageBitmap(bit_temp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //wsh
    private void controlling_colorbar(){
        colorbar_controller = (SeekBar) findViewById(R.id.colorbar_controller);
        colorbar_controller.setMax(499);
        colorbar_controller.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            color_position=progress;
                Bitmap bit_temp = makingbitmap();
                gammaView.setImageBitmap(bit_temp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //wsh
    private void controlling_distance(){
        dist_controller = (SeekBar) findViewById(R.id.distance_controller);
        //txt_cur = (TextView) findViewById(R.id.txt_cur);
        dist_controller.setMax(50000);
        dist_controller.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress!=0){
                Log.d(TAG, "onProgressChanged: "+progress);
                //  txt_cur.setText("transparency" + progress + "  / 100 ");
                dist_position=progress;
                float temp_dis =(float) (227370/(progress));
                double temp_meter=dist_position*0.001;
                gamma_frame.setTranslationY(temp_dis);
                dis_txt.setText(temp_meter+"m");}

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }



    //wsh
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[3] = (byte)((i >> 24) & 0xFF);
        result[2] = (byte)((i >> 16) & 0xFF);
        result[1] = (byte)((i >> 8) & 0xFF);
        result[0] = (byte)(i & 0xFF);
        return result;
    }

    //wsh
    public void random_dummy_data_test() {

        /** initialize variables **/
        all_total_data = 0;
        cps = 0;
        total_count = 0;
        the_firsttime_datainput = 0;

        /** local variables **/
        int BytesNumber = total_ch_Time.length * 16;
        BytesNumber = 5000000;
        int total_dummy_byte = 0;
        byte[] temp_total_Bytedata = new byte[BytesNumber];
        byte[] random_header_Byte = new byte[4];
        byte[] random_ch_data_Byte = new byte[4];
        int dummy_data_CountNum = 0;
        int random_ch_data_int = 0;

        /** generation random header **/
        java.util.Random random = new java.util.Random();
        while (total_dummy_byte + 4004 < BytesNumber) {
            int random_CountNum = 0;
            while (random_CountNum > 250 || random_CountNum < 1) {
                random_CountNum = (int) ((random.nextInt(4004) - 4) / 16);
                //Log.d(TAG, "random_dummy_data_test: "+random_CountNum);
            }

            random_header_Byte = intToByteArray(random_CountNum * 16 + 4);

            /** storage random header in Byte array **/
            for (int n = 0; n < 4; n++) {
                temp_total_Bytedata[n + total_dummy_byte] = random_header_Byte[n];
            }
            total_dummy_byte += 4;

            /** generation of 4 chennal data   **/
            for (int i = 0; i < random_CountNum * 4; i++) {
                random_ch_data_int = 0;
                while (random_ch_data_int > 60000 || random_ch_data_int <= 0) {
                    random_ch_data_int = random.nextInt(60000);
                }
                random_ch_data_Byte = intToByteArray(random_ch_data_int);
                for (int m = 0; m < 4; m++) {
                    temp_total_Bytedata[m + total_dummy_byte] = random_ch_data_Byte[m];
                }
                total_dummy_byte += 4;
            }
            dummy_data_CountNum += random_CountNum;
        }
        data_generation_finish = System.currentTimeMillis() ;
        /** processing data: send data to handlerthread every package **/
        long time1 = System.currentTimeMillis();
        while (all_total_data + 4004 < BytesNumber) {

            int data_len = ((((int) temp_total_Bytedata[3 + all_total_data] & 0xff) << 24) |                                                 //head(4byte)  content: how many byte
                    (((int) temp_total_Bytedata[2 + all_total_data] & 0xff) << 16) |
                    (((int) temp_total_Bytedata[1 + all_total_data] & 0xff) << 8) |
                    (((int) temp_total_Bytedata[0 + all_total_data] & 0xff)));

            byte[] package_data = new byte[data_len];
            for (int i = 0; i < data_len; i++) {
                package_data[i] = temp_total_Bytedata[i + all_total_data];
            }
            /** receiving data **/
            if (isIntent) {
                intent.putExtra("dating", package_data);
                startService(intent);
            } else {

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    os.write(package_data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Message msg1 = new Message();
                msg1.obj = os;
                workHandler.sendMessage(msg1);

            }

            all_total_data += data_len;
            /** when data processing is done     **/
            if (all_total_data + 4004 > BytesNumber) {
                Log.d(TAG, "dummy_data : total_count: " + Integer.toString(dummy_data_CountNum));
                break;
            }
        }
    }
}







