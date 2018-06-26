package com.AR.Teaching;

import com.unity3d.player.*;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class UnityPlayerActivity extends Activity
{
    private static final String TAG = "UnityPlayerActivity";
    protected UnityPlayer mUnityPlayer; // don't change the name of this variable; referenced from native code
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    public BluetoothLEService mBluetoothLEService;
    private String bLEDevAddress = "";
    public boolean connect;
    public boolean run1 = false;
    public static boolean data_ready = false;
    public static String receive_data = null;
    private boolean ble_connect_mode = false;
    public ArrayList<BluetoothGattCharacteristic> characteristics;
    public BluetoothGattCharacteristic mNotifyCharacteristic;
    int dataget[], dataold[];
    int bt1 = 0, bt2 = 0, bt3 = 0;
    private boolean post_connect = false;
    private boolean voice_connect = false;
    int voicefirsttime = 0;
    int firsttime_resume = 0;
    int exitbegin = 0;
    String teachingip = "";
    String teachingselect = "";
    String groupcode = "";
    String LoginUser = "";
    String teachingmodel = "";
    String postcmdsend = "begin";
    String[] postcmdget;
    String[] postpeopleget;
    int postcmdgetnumber = 0;
    int postcmdcount = 0;
    int peoplenumber = 0;
    String freemodechangetext = "Free Mode";
    public boolean videothreadrun = false;
    public int videodatafirstget = 0;
    public static boolean datalogshow = false;
    public static boolean timelogshow = false;
    long startTime1, startTime2;
    long consumingTime1, consumingTime2;

    // Setup activity layout
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.RGBX_8888); // <--- This makes xperia play happy
        mUnityPlayer = new UnityPlayer(this);
        setContentView(mUnityPlayer);
        mUnityPlayer.requestFocus();
        Bundle bundle;
        try {
            bundle = this.getIntent().getExtras();
            teachingip = bundle.getString("teachingip");
            teachingselect = bundle.getString("teachingselect");
            bLEDevAddress = bundle.getString("getdeviceblemac");
            groupcode = bundle.getString("groupcode");
            LoginUser = bundle.getString("LoginUser");
        } catch (Exception e) {
        }
        if (teachingselect.equals("")) {
            Toast.makeText(this, "Just watching mode", Toast.LENGTH_SHORT).show();
        } else {
            if (bLEDevAddress.equals("")) {
                Toast.makeText(this, "Video mode", Toast.LENGTH_SHORT).show();
            } else {
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(this, "Bluetooth not support", Toast.LENGTH_SHORT).show();
                    UnityPlayerActivity.this.finish();
                }
                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();
                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                Intent intent = new Intent(this, BluetoothLEService.class);
                bindService(intent, conn, BIND_AUTO_CREATE);
                dataget = new int[12];
                dataold = new int[12];
                for (int i = 0; i < 12; i++)
                    dataold[i] = 0x00;
                ble_connect_mode = true;
            }
        }
    }

    //文本信息传递
    public void SceneState(String datatmp) {
        post_connect = true;
        Log.e(TAG, "SceneState:" + datatmp);
        if ((!teachingselect.equals("")) && (!bLEDevAddress.equals("")))
            UnityPlayer.UnitySendMessage("BasicText", "SetText", "Code: " + groupcode + "\nPeople");
    }

    //操作指令
    public void Data_Processing() {
        if (receive_data.length() == 3 && post_connect == true) {
            dataget[0] = (receive_data.charAt(0) & 0x18) >> 3;
            dataget[1] = (receive_data.charAt(0) & 0x06) >> 1;
            dataget[6] = receive_data.charAt(0) & 0x01;
            dataget[2] = (receive_data.charAt(1) & 0x18) >> 3;
            dataget[3] = (receive_data.charAt(1) & 0x06) >> 1;
            dataget[7] = receive_data.charAt(1) & 0x01;
            dataget[4] = (receive_data.charAt(2) & 0x20) >> 5;
            dataget[5] = (receive_data.charAt(2) & 0x10) >> 4;
            dataget[8] = (receive_data.charAt(2) & 0x08) >> 3;
            dataget[9] = (receive_data.charAt(2) & 0x04) >> 2;
            dataget[10] = (receive_data.charAt(2) & 0x02) >> 1;
            dataget[11] = receive_data.charAt(2) & 0x01;
            if (datalogshow)
                Log.e(TAG, "" + dataget[0] + "," + dataget[1] + "," + dataget[2] + "," + dataget[3] + "," + dataget[4] + "," + dataget[5] + "," + dataget[6] + "," + dataget[7] + "," + dataget[8] + "," + dataget[9] + "," + dataget[10] + "," + dataget[11]);
            /*if (teachingselect.equals("2") && voicefirsttime == 0) {
                voicefirsttime = 1;
                UnityPlayer.UnitySendMessage("MyVoice", "JoinChannel", groupcode);
            }*/
            if (dataget[5] == 0) {
                try {
                    postcmdgetnumber = 0;
                    String mobilecmd = "dataexchange";
                    String mobilecmdsql = groupcode + "^" + LoginUser + "^" + postcmdsend + "^" + postcmdcount;
                    String strResult = HttpPostAR.submitPostData(mobilecmd, mobilecmdsql, Integer.parseInt(teachingselect), teachingip);
                    JSONArray jsonArray = new JSONArray(strResult);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    String err = jsonObject.getString("err");
                    if (datalogshow)
                        Log.e(TAG, "POST: " + err);
                    if (err.equals("ok")) {
                        String datajson = jsonObject.getString("data");
                        String[] datajsoncut = datajson.split("\\^");
                        if (datajsoncut.length >= 3 && datajsoncut.length % 2 == 1) {
                            peoplenumber = Integer.parseInt(datajsoncut[0]);
                            postcmdcount = Integer.parseInt(datajsoncut[1]);
                            UnityPlayer.UnitySendMessage("BasicText", "SetText", "Code: " + groupcode + "\nPeople: " + peoplenumber);
                            if (postcmdcount == 0)
                                Unity_Command(17);
                            else {
                                postcmdgetnumber = Integer.parseInt(datajsoncut[2]);
                                if (postcmdgetnumber != 0) {
                                    postcmdget = new String[postcmdgetnumber];
                                    postpeopleget = new String[postcmdgetnumber];
                                }
                                for (int i = 0; i < postcmdgetnumber; i++) {
                                    postpeopleget[i] = datajsoncut[i * 2 + 3];
                                    postcmdget[i] = datajsoncut[i * 2 + 4];
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                postcmdsend = "begin";
            }
            if (dataget[4] == 1 || dataget[5] == 1) {
                if (dataget[4] == 1) {
                    if (datalogshow)
                        Log.e(TAG, "Control Mode");
                    if (freemodechangetext.equals("Free Mode")) {
                        UnityPlayer.UnitySendMessage("TextSet", "SetText", "Control Mode");
                        Unity_Command(17);
                    }
                    for (int i = 0; i < postcmdgetnumber; i++) {
                        UnityPlayer.UnitySendMessage("TextSet", "SetText", postpeopleget[i] + " Control");
                        freemodechangetext = postpeopleget[i] + " Control";
                        String cmdtmpstr = postcmdget[i];
                        String[] cmdtmpstrcut = cmdtmpstr.split("&");
                        for (int j = 0; j < cmdtmpstrcut.length; j++) {
                            Unity_Command(Integer.parseInt(cmdtmpstrcut[j]));
                        }
                    }
                    if (dataget[0] == 1) {
                        postcmdsend += "&1";
                    } else if (dataget[0] == 2) {
                        postcmdsend += "&2";
                    }
                    if (dataget[1] == 1) {
                        postcmdsend += "&3";
                    } else if (dataget[1] == 2) {
                        postcmdsend += "&4";
                    }
                    if (dataget[2] == 1) {
                        postcmdsend += "&5";
                    } else if (dataget[2] == 2) {
                        postcmdsend += "&6";
                    }
                    if (dataget[3] == 1) {
                        postcmdsend += "&7";
                    } else if (dataget[3] == 2) {
                        postcmdsend += "&8";
                    }
                    if (dataget[6] == 1) {
                        postcmdsend += "&9";
                    }
                    if (dataget[7] == 1) {
                        postcmdsend += "&10";
                    }
                    if (dataget[8] != dataold[8]) {
                        if (dataget[8] == 1) {
                            if (bt1 == 0) {
                                bt1 = 1;
                                postcmdsend += "&11";
                            } else {
                                bt1 = 0;
                                postcmdsend += "&12";
                            }
                        }
                    }
                    if (dataget[9] != dataold[9]) {
                        if (dataget[9] == 1) {
                            if (bt2 == 0) {
                                bt2 = 1;
                                postcmdsend += "&13";
                            } else {
                                bt2 = 0;
                                postcmdsend += "&14";
                            }
                        }
                    }
                    if (dataget[10] != dataold[10]) {
                        if (dataget[10] == 1) {
                            if (bt3 == 0) {
                                bt3 = 1;
                                postcmdsend += "&15";
                            } else {
                                bt3 = 0;
                                postcmdsend += "&16";
                            }
                        }
                    }
                    if (dataget[11] != dataold[11]) {
                        if (dataget[11] == 1) {
                            postcmdsend += "&17";
                        }
                    }
                } else {
                    if (datalogshow)
                        Log.e(TAG, "Free Mode");
                    postcmdcount = 0;
                    UnityPlayer.UnitySendMessage("TextSet", "SetText", "Free Mode");
                    UnityPlayer.UnitySendMessage("BasicText", "SetText", "Code: " + groupcode + "\nPeople: -");
                    freemodechangetext = "Free Mode";
                    if (dataget[0] == 1) {
                        Unity_Command(1);
                    } else if (dataget[0] == 2) {
                        Unity_Command(2);
                    }
                    if (dataget[1] == 1) {
                        Unity_Command(3);
                    } else if (dataget[1] == 2) {
                        Unity_Command(4);
                    }
                    if (dataget[2] == 1) {
                        Unity_Command(5);
                    } else if (dataget[2] == 2) {
                        Unity_Command(6);
                    }
                    if (dataget[3] == 1) {
                        Unity_Command(7);
                    } else if (dataget[3] == 2) {
                        Unity_Command(8);
                    }
                    if (dataget[6] == 1) {
                        Unity_Command(9);
                    }
                    if (dataget[7] == 1) {
                        Unity_Command(10);
                    }
                    if (dataget[8] != dataold[8]) {
                        if (dataget[8] == 1) {
                            if (bt1 == 0) {
                                bt1 = 1;
                                Unity_Command(11);
                            } else {
                                bt1 = 0;
                                Unity_Command(12);
                            }
                        }
                    }
                    if (dataget[9] != dataold[9]) {
                        if (dataget[9] == 1) {
                            if (bt2 == 0) {
                                bt2 = 1;
                                Unity_Command(13);
                            } else {
                                bt2 = 0;
                                Unity_Command(14);
                            }
                        }
                    }
                    if (dataget[10] != dataold[10]) {
                        if (dataget[10] == 1) {
                            if (bt3 == 0) {
                                bt3 = 1;
                                Unity_Command(15);
                            } else {
                                bt3 = 0;
                                Unity_Command(16);
                            }
                        }
                    }
                    if (dataget[11] != dataold[11]) {
                        if (dataget[11] == 1) {
                            Unity_Command(17);
                        }
                    }
                }
            } else {
                if (datalogshow)
                    Log.e(TAG, "Watch Mode");
                if (freemodechangetext.equals("Free Mode")) {
                    UnityPlayer.UnitySendMessage("TextSet", "SetText", "Watch Mode");
                    Unity_Command(17);
                }
                for (int i = 0; i < postcmdgetnumber; i++) {
                    UnityPlayer.UnitySendMessage("TextSet", "SetText", postpeopleget[i] + " Control");
                    freemodechangetext = postpeopleget[i] + " Control";
                    String cmdtmpstr = postcmdget[i];
                    String[] cmdtmpstrcut = cmdtmpstr.split("&");
                    for (int j = 0; j < cmdtmpstrcut.length; j++) {
                        Unity_Command(Integer.parseInt(cmdtmpstrcut[j]));
                    }
                }
            }
            for (int i = 0; i < 12; i++)
                dataold[i] = dataget[i];
        }
    }

    void Unity_Command(int number) {
        if (number == 1)
            UnityPlayer.UnitySendMessage("ShowPoint", "MakeMoveMinus_x", "0.1");
        else if (number == 2)
            UnityPlayer.UnitySendMessage("ShowPoint", "MakeMovePlus_x", "0.1");
        else if (number == 3)
            UnityPlayer.UnitySendMessage("ShowPoint", "MakeMoveMinus_z", "0.1");
        else if (number == 4)
            UnityPlayer.UnitySendMessage("ShowPoint", "MakeMovePlus_z", "0.1");
        else if (number == 5)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnPlus_y", "1");
        else if (number == 6)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnMinus_y", "1");
        else if (number == 7) {
            if (teachingmodel.equals("Brain"))
                UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnPlus_x", "1");
            if (teachingmodel.equals("Heart"))
                UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnPlus_z", "1");
        } else if (number == 8) {
            if (teachingmodel.equals("Brain"))
                UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnMinus_x", "1");
            if (teachingmodel.equals("Heart"))
                UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnMinus_z", "1");
        } else if (number == 9)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeSmaller", "0.01");
        else if (number == 10)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeBiger", "0.01");
        else if (number == 11) {
            if (teachingmodel.equals("Brain")) {
                UnityPlayer.UnitySendMessage("BrainPart1", "BrainPart1Cut", "1");
                UnityPlayer.UnitySendMessage("BrainPart2", "BrainPart2Cut", "1");
                UnityPlayer.UnitySendMessage("BrainPart3", "BrainPart3Cut", "1");
            }
            if (teachingmodel.equals("Heart")) {
                UnityPlayer.UnitySendMessage("HeartPart1", "HeartPart1Cut", "1");
                UnityPlayer.UnitySendMessage("HeartPart2", "HeartPart2Cut", "1");
            }
        } else if (number == 12) {
            if (teachingmodel.equals("Brain")) {
                UnityPlayer.UnitySendMessage("BrainPart1", "BrainPart1Cut", "0");
                UnityPlayer.UnitySendMessage("BrainPart2", "BrainPart2Cut", "0");
                UnityPlayer.UnitySendMessage("BrainPart3", "BrainPart3Cut", "0");
            }
            if (teachingmodel.equals("Heart")) {
                UnityPlayer.UnitySendMessage("HeartPart1", "HeartPart1Cut", "0");
                UnityPlayer.UnitySendMessage("HeartPart2", "HeartPart2Cut", "0");
            }
        } else if (number == 13)
            UnityPlayer.UnitySendMessage("TextObject", "ShowText", "0");
        else if (number == 14)
            UnityPlayer.UnitySendMessage("TextObject", "ShowText", "1");
        else if (number == 15)
            UnityPlayer.UnitySendMessage("RealCamera", "ShowBackground", "0");
        else if (number == 16)
            UnityPlayer.UnitySendMessage("RealCamera", "ShowBackground", "1");
        else if (number == 17) {
            if (teachingmodel.equals("Brain")) {
                UnityPlayer.UnitySendMessage("BrainPart1", "BrainPart1Cut", "0");
                UnityPlayer.UnitySendMessage("BrainPart2", "BrainPart2Cut", "0");
                UnityPlayer.UnitySendMessage("BrainPart3", "BrainPart3Cut", "0");
            }
            if (teachingmodel.equals("Heart")) {
                UnityPlayer.UnitySendMessage("HeartPart1", "HeartPart1Cut", "0");
                UnityPlayer.UnitySendMessage("HeartPart2", "HeartPart2Cut", "0");
            }
            UnityPlayer.UnitySendMessage("ShowPoint", "MakeDefault_All", "");
            UnityPlayer.UnitySendMessage("ShowObject", "MakeDefault_All", "");
            UnityPlayer.UnitySendMessage("TextObject", "ShowText", "1");
            UnityPlayer.UnitySendMessage("RealCamera", "ShowBackground", "1");
        }
    }

    //蓝牙线程
    class MyThread1 extends Thread {
        public void run() {
            int count_data_in = 0;
            while (run1) {
                if (data_ready == true) {
                    data_ready = false;
                    if (timelogshow) {
                        consumingTime1 = System.nanoTime() - startTime1;
                        Log.e(TAG, "ble--->" + consumingTime1 / 1000000 + "ms");
                        consumingTime1 += consumingTime2;
                        Log.e(TAG, "total----------->" + consumingTime1 / 1000000 + "ms");
                        startTime2 = System.nanoTime();
                    }
                    count_data_in = 0;
                    Data_Processing();
                    if (timelogshow) {
                        consumingTime2 = System.nanoTime() - startTime2;
                        Log.e(TAG, "net--->" + consumingTime2 / 1000000 + "ms");
                        startTime1 = System.nanoTime();
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count_data_in++;
                if (count_data_in > 1000) {
                    count_data_in = 0;
                    if (mBluetoothLEService != null) {
                        connect = mBluetoothLEService.connect(bLEDevAddress);
                        if (connect) {
                            NotifyThread thread = new NotifyThread();
                            thread.execute();
                        }
                    }
                    try {
                        Thread.sleep(1050);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLEService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLEService = ((BluetoothLEService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initBluetoothParam()) {
                Toast.makeText(UnityPlayerActivity.this, "Bluetooth error", Toast.LENGTH_SHORT).show();
                UnityPlayerActivity.this.finish();
            }
            if (mBluetoothLEService == null) {
                Toast.makeText(UnityPlayerActivity.this, "Bluetooth error", Toast.LENGTH_SHORT).show();
                UnityPlayerActivity.this.finish();
            }
        }
    };

    @Override protected void onNewIntent(Intent intent)
    {
        // To support deep linking, we need to make sure that the client can get access to
        // the last sent intent. The clients access this through a JNI api that allows them
        // to get the intent set on launch. To update that after launch we have to manually
        // replace the intent with the one caught here.
        setIntent(intent);
    }

    // Quit Unity
    @Override protected void onDestroy ()
    {
        mUnityPlayer.quit();
        super.onDestroy();
    }

    // Pause Unity
    @Override protected void onPause()
    {
        super.onPause();
        mUnityPlayer.pause();
    }

    // Resume Unity
    @Override protected void onResume()
    {
        super.onResume();
        mUnityPlayer.resume();
    }

    @Override protected void onStart()
    {
        super.onStart();
        mUnityPlayer.start();
    }

    @Override protected void onStop()
    {
        super.onStop();
        mUnityPlayer.stop();
    }

    // Low Memory Unity
    @Override public void onLowMemory()
    {
        super.onLowMemory();
        mUnityPlayer.lowMemory();
    }

    // Trim Memory Unity
    @Override public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_RUNNING_CRITICAL)
        {
            mUnityPlayer.lowMemory();
        }
    }

    // This ensures the layout will be correct.
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    // For some reason the multiple keyevent type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }

    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
    /*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }
}
