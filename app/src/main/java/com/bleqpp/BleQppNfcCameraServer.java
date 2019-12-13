package com.bleqpp;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.RequiresApi;
import com.akingyin.rfidwgs.BuildConfig;
import com.akingyin.rfidwgs.R;
import com.akingyin.rfidwgs.ext.PreferencesExtKt;
import com.blankj.utilcode.util.ConvertUtils;
import com.quintic.libqpp.QppUsbCameraApi;
import com.quintic.libqpp.iQppCallback;
import com.zlcdgroup.nfcsdk.ConStatus;
import com.zlcdgroup.nfcsdk.RfidConnectorInterface;
import com.zlcdgroup.nfcsdk.SDKInterface;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author king
 * @version V1.0
 * @ Description:
 * @ Date 2018/1/31 17:10
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleQppNfcCameraServer   implements SDKInterface, BluetConnectListion,iQppCallback {

  public   static final   byte[]   OPEN_DATA={1};
  public   static final   byte[]   CLOSE_DATA={0};

  public static BluetoothGatt mBluetoothGatt = null;
  protected static final String TAG = BleQppNfcCameraServer.class.getSimpleName();



  private WeakReference<Context>  mContextWeakReference;
  private BluetoothManager bluetoothManager = null;

  private BluetoothAdapter mBluetoothAdapter;
  private List<String> blemac = new ArrayList<>();

  protected static String uuidQppService = "0000fee9-0000-1000-8000-00805f9b34fb";
  protected static String uuidQppCharWrite = "d44bc439-abfd-45a2-b575-925416129600";

  private BluetoothGattCallback mGattCallback;

  private BleQppNfcCameraServer(Context context) {
    // TODO Auto-generated constructor stub
    if(null != mContextWeakReference){
      mContextWeakReference.clear();
    }
    mContextWeakReference = new WeakReference<>(context);
    init();
  }

  private static BleQppNfcCameraServer instance;

  public Handler mHandler;



  private static BluetoothHelp bluetoothHelp;

  protected static SoundPool pool =null;
  protected static int   soundID;
  public static BleQppNfcCameraServer getInstance(Context context) {

    if (null == instance) {
      instance = new BleQppNfcCameraServer(context);

    }
    if(null != instance.mContextWeakReference.get()){
      if(instance.mContextWeakReference.get() != context){
        instance.mContextWeakReference.clear();
        instance.mContextWeakReference=new WeakReference<>(context);
      }
    }
   // instance.context = context;
    if (null == bluetoothHelp ) {
      bluetoothHelp = new BluetoothHelp(instance.mContextWeakReference.get(), address);
    }
    bluetoothHelp.setContext(instance.mContextWeakReference.get());
    bluetoothHelp.setBluetConnectListion(instance);

    initPool(context);

    return instance;
  }


  private   static   void    initPool(Context context){
    if(null != pool){
      pool.release();
      pool =null;
    }
    pool =	new SoundPool(13, AudioManager.STREAM_SYSTEM, 8);
    soundID =  pool.load(context, R.raw.nfc,1);

  }

  private RfidConnectorInterface rfidlistion;

  public static String address = "";

  // 重复连接次数
  private int reConnectCount;
  // 最大连接次数
  private static final int MAX_RECONNECT_TIMES = 10;
  private static boolean   connectionFailed = true;

  private  boolean   connectSucess=false;//是否连接成功过
  private  Handler    callbackHandle = new Handler(Looper.getMainLooper()){
    @Override public void handleMessage(Message msg) {
      super.handleMessage(msg);
      if(msg.what == 5){
        String  addr = msg.obj.toString();
        connect(addr);
      }else if(msg.what == 4){
        if(msg.arg1 == 1){
          showDialog(msg.obj.toString());
        }else if(msg.arg1 == 2){

          showConfigDialog(msg.obj.toString());
        }

      }
    }
  };

  private Dialog    alertDialog = null;
  private Dialog    configDialog = null;

  private   void   hideDialog(){
    if(null != alertDialog && alertDialog.isShowing()){
      alertDialog.dismiss();
    }
    if(null != configDialog && configDialog.isShowing()){
      configDialog.dismiss();
    }
  }

  private    void    showDialog(String  message){
    try {
      hideDialog();
      if(null != mContextWeakReference.get()  ){
        if(mContextWeakReference.get()  instanceof Activity){
          if(!((Activity) mContextWeakReference.get()).isFinishing()){
            AlertDialog.Builder builder = new AlertDialog.Builder(mContextWeakReference.get());
            alertDialog = builder.setTitle("警告")
                .setMessage(message)
                .show();
          }
        }

      }
    }catch (Exception e){
      e.printStackTrace();
    }


  }

  private   void   showConfigDialog(String  message){
    try {

      hideDialog();
      if(null != mContextWeakReference.get()){
        if(mContextWeakReference.get() instanceof  Activity){
          if(!((Activity) mContextWeakReference.get()).isFinishing()){
            AlertDialog.Builder builder = new AlertDialog.Builder(mContextWeakReference.get());
            builder.setTitle("提示")
                .setMessage(message)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int id) {

                    PreferencesExtKt.spSetInt("ble_cardread",0);

                    dialog.dismiss();
                    connectDestroy();
                  }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                  }
                });
            configDialog =  builder.show();
          }
        }

      }
    }catch (Exception e){
      e.printStackTrace();
    }

  }

  @SuppressLint({ "InlinedApi", "NewApi" })
  public void init() {
    hideDialog();
    if(null == mHandler){
      mHandler = new Handler(Looper.getMainLooper());
    }
    if(null == bluetoothManager && null != mContextWeakReference.get()){
      bluetoothManager = (BluetoothManager) mContextWeakReference.get().getSystemService(Context.BLUETOOTH_SERVICE);
    }

    if (null != bluetoothManager) {
      mBluetoothAdapter = bluetoothManager.getAdapter();

    }
    initBleCallback();

    QppUsbCameraApi.setCallback(this);

  }

  /**
   * 是否正在连接中
   */
  protected  AtomicBoolean    connectStatus = new AtomicBoolean(false);

  @Override
  public boolean connectDestroy() {
    if(null != callbackHandle){
      callbackHandle.removeMessages(1);
      callbackHandle.removeMessages(2);
      callbackHandle.removeMessages(4);
      callbackHandle.removeMessages(5);
    }
    disconnect();
    close();
    blemac.clear();
    bluetoothHelp = null;
    address="";

    instance = null;
    return false;
  }

  @Override
  public boolean initConnect() {
    init();
    return false;
  }

  @SuppressLint("SimpleDateFormat")
  public   static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @SuppressLint("NewApi")
  @Override
  public void onregistered(RfidConnectorInterface rfidlistion) {
    this.rfidlistion = rfidlistion;
    if(connectStatus.get()){
      return;
    }
    initBleCallback();

    if(null != mBluetoothAdapter && !mBluetoothAdapter.isEnabled()){
      connectionFailed = true;
    }
    if (null != bluetoothHelp) {
      bluetoothHelp.registerBluetooth();
    }
    String mac = "";
    if(null != mContextWeakReference.get()){
      mac = KsiSharedStorageHelper.getBluetoothMac(KsiSharedStorageHelper.getPreferences(mContextWeakReference.get()));
    }
    showToast("当前地址："+mac);
    reConnectCount = 0;
    if (!TextUtils.isEmpty(mac) && (connectionFailed || !address.equalsIgnoreCase(mac))) {
      blemac.clear();
      if(connectionFailed || !address.equalsIgnoreCase(mac)){
        if(connectSucess){
          if(null != mBluetoothGatt){
            //mBluetoothGatt.close();
            try {
              mBluetoothGatt.disconnect();
              mBluetoothGatt.close();
            }catch (Exception e){
              e.printStackTrace();
            }
          }
        }
      }
      if (!blemac.contains(mac)) {
        showToast("缓存地址："+address+": 新地址："+mac);
        address = mac;
        connect(mac);
        blemac.clear();
        blemac.add(mac);
      }


    }
  }

  @Override
  public void setAutoConnect(boolean arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void unregistered(RfidConnectorInterface arg0) {
    this.rfidlistion = null;
    if (null != bluetoothHelp) {
      bluetoothHelp.closeBluetoohHelp();
    }
  }


  public   void   showToast(String  msg){
    try {
      if(BuildConfig.DEBUG){
        //if(null != mContextWeakReference.get()){
        //  callbackHandle.post(new Runnable() {
        //    @Override public void run() {
        //      Toast.makeText(mContextWeakReference.get(),msg,Toast.LENGTH_SHORT).show();
        //    }
        //  });
        //
        //}

      }
    }catch (Exception e){
      e.printStackTrace();
    }


  }


  @Override
  public void onSuccess(String bluemac) {
    initBleCallback();
    if(TextUtils.isEmpty(bluemac)){
      String mac = "";
      if(null != mContextWeakReference.get()){
        mac = KsiSharedStorageHelper
            .getBluetoothMac(KsiSharedStorageHelper.getPreferences(mContextWeakReference.get()));
      }
      if(!TextUtils.isEmpty(mac)){
        try{
          if(null != mBluetoothGatt && connectSucess){
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
          }
        }catch (Exception e){
          e.printStackTrace();
        }
        address = mac;
        connect(mac);
        blemac.clear();
        blemac.add(mac);
      }
    }else{
      if (!blemac.contains(bluemac)) {
        address = bluemac;
        reConnectCount=0;
        connect(bluemac);
        blemac.clear();
        blemac.add(bluemac);
      }
    }

  }
  //回调是否成功
  public  boolean   isCallBack= false;

  public  void    connect(String  address){
    System.out.println("开始连接--------->>>"+connectStatus.get());
    if(null == instance){
      connectStatus.set(false);
      return ;
    }
     isCallBack = false;
     connect(address,false);
  }


  private Disposable mSubscription = null;

  /**
   * 连接时间
   */
  private    long     connectTime = 0L;
  //连接
  @SuppressLint("NewApi")
  public void connect(  final String address,boolean  authConnect) {
    if(null != mSubscription && !mSubscription.isDisposed()){
      mSubscription.dispose();
    }
    if(connectTime == 0){
      connectTime = System.currentTimeMillis();
    }
    if(null == instance){
      connectStatus.set(false);
      return ;
    }
    if(connectStatus.get()){
      return ;
    }
    connectStatus.set(true);
    if(null != rfidlistion){
      rfidlistion.onConnectStatus(ConStatus.CONNECTING);
    }
    if(null != bluetoothHelp){
      mHandler.post(new Runnable() {
        @Override public void run() {
          if(null != rfidlistion){
            if(null != bluetoothHelp && bluetoothHelp.CheckBluetooth()){
              bluetoothHelp.registerBluetooth();
            }
          }
        }
      });
    }

    if (mBluetoothAdapter == null || address == null || TextUtils.isEmpty(address)) {
      System.out.println("BluetoothAdapter not initialized or unspecified address."+address);
      Log.w("Qn Dbg",
          "BluetoothAdapter not initialized or unspecified address.");
      setConnectBleStatus(ConStatus.NONE);
      connectStatus.set(false);
      connectSucess = false;
      connectionFailed = true;
      connectTime = 0;
      Message  message = callbackHandle.obtainMessage();
      message.what = 4;
      message.arg1=1;
      message.obj="当前未设置读卡器或蓝牙设备无法开启，请检查";
      callbackHandle.sendMessage(message);
      System.out.println("当前蓝牙未连接---------->>");
      return ;
    }
      mSubscription = Observable.just(address).map(new Function<String, Boolean>() {
        @Override public Boolean apply(String s) throws Exception {
          BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(s);

          if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            connectStatus.set(false);
            connectSucess = false;
            connectionFailed = true;
            setConnectBleStatus(ConStatus.CONNECTFAIL);

            return false;
          }
          if(null == mGattCallback){
            initBleCallback();
          }
          if(null != mContextWeakReference.get()){
            mBluetoothGatt = device.connectGatt(mContextWeakReference.get(), false, mGattCallback);
          }
          // setting the autoConnect parameter to false.



          Log.d(TAG, "Trying to create a new connection. Gatt: " + mBluetoothGatt);


          return true;
        }
      }).subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(new Consumer<Boolean>() {
          @Override public void accept(Boolean aBoolean) throws Exception {
            if (aBoolean && System.currentTimeMillis() - connectTime > 10 * 60 * 1000) {
              Message message = callbackHandle.obtainMessage();
              message.what = 4;
              message.arg1 = 2;
              message.obj = "已尝试连接读卡器已超过10分钟，是否关闭读卡器功能？";
              callbackHandle.sendMessage(message);
            }
          }
        }, new Consumer<Throwable>() {
          @Override public void accept(Throwable throwable) throws Exception {
            throwable.printStackTrace();
            connectStatus.set(false);
            setConnectBleStatus(ConStatus.CONNECTFAIL);
          }
        });

  }


  private   void    connectBleDeviceDelay(String  address){
    Message  message = callbackHandle.obtainMessage();
    message.what = 5;
    message.obj = address;
    callbackHandle.sendMessageDelayed(message,1000);
  }

  private void   setConnectBleStatus(ConStatus  conStatus){
    if(null != rfidlistion){
      rfidlistion.onConnectStatus(conStatus);
    }
  }
  //取消连接
  public void disconnect() {
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w("Qn Dbg", "BluetoothAdapter not initialized");
      return;
    }
    try{
      mBluetoothGatt.disconnect();
      connectSucess = false;
      setConnectBleStatus(ConStatus.UNCONNECT);
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  //回收
  public void close() {
    try{
      if (mBluetoothGatt != null) {
        mBluetoothGatt.close();
        mBluetoothGatt = null;
      }
      connectSucess = false;
    } catch (Exception e){
      e.printStackTrace();
    }

  }


  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public   void   initBleCallback(){
    if(null == mGattCallback){

      mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
            int newState) {
          connectStatus.set(false);
          isCallBack = true;
          if(null != mSubscription && !mSubscription.isDisposed()){
            mSubscription.dispose();
          }
          showToast("onConnectionStateChange : " + status + "  newState : " + newState);
          Log.w(TAG, "onConnectionStateChange : " + status + "  newState : " + newState);
          //133问题，iPhone 和 某些Android手机作为旁支会出现蓝牙初始连接就是133，
          // 此情况下应该立刻重新扫描连接
          if(status == 133){
            try{
              gatt.disconnect();
              gatt.close();
            }catch (Exception  e){
              e.printStackTrace();
            }
            connectBleDeviceDelay(address);
            return;
          }

          if (newState == BluetoothProfile.STATE_CONNECTED) {
            System.out.println("第一步--->成功");
            connectTime = 0;
            connectSucess = true;

            setConnectBleStatus(ConStatus.CONNECTED);
            mBluetoothGatt.discoverServices();

            connectionFailed = false;
          } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

            connectTime = System.currentTimeMillis();
            connectionFailed = true;
            reConnectCount =0;

            setConnectBleStatus(ConStatus.CONNECTFAIL);
            if(null != mBluetoothGatt){
              try {
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt =null;
              }catch (Exception e){
                e.printStackTrace();
              }
            }
            //if(status == 133 || status == 19 ||status == BluetoothGatt.GATT_FAILURE ){
            //
            //  if(null != mBluetoothGatt && connectSucess){
            //    try{
            //      mBluetoothGatt.disconnect();
            //      mBluetoothGatt.close();
            //    }catch (Exception e){
            //      e.printStackTrace();
            //    }
            //    connectSucess = false;
            //  }
            // // return;
            //}
            connectSucess = false;
            try{
              gatt.disconnect();
              gatt.close();
            }catch (Exception  e){
              e.printStackTrace();
            }
            if(!connectionFailed){
              connectionFailed = true;
            }
            //if (reConnectCount == MAX_RECONNECT_TIMES) {
            //  return;
            //}
            close();

            connect(address);

           // reConnectCount++;
          }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
          connectStatus.set(false);
          QppUsbCameraApi.qppEnable(gatt);
          Log.w("Ble APP", "连接状态  onServicesDiscovered   status="+status);
          isCallBack = true;
          if(status == BluetoothGatt.GATT_SUCCESS){
            System.out.println("第二步----->>成功");
          }
          showToast("onServicesDiscovered : " + status );
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
          connectStatus.set(false);
          QppUsbCameraApi.updateValueForNotification(gatt, characteristic);
          isCallBack = true;

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
          super.onDescriptorWrite(gatt,descriptor,status);
          Log.w("Ble APP", "连接状态  onDescriptorWrite  status="+status);
           showToast("连接状态  onDescriptorWrite  status="+status);
          connectStatus.set(false);
          isCallBack = true;
          Log.w(TAG, "onDescriptorWrite");
          Log.w(TAG,"status="+status);
          if(status ==  BluetoothGatt.GATT_SUCCESS){
            System.out.println("第三步--->成功");
            if(descriptor.equals(QppUsbCameraApi.getDescriptor_FEEA())){
              if(null != QppUsbCameraApi.getmDescriptor()){
                mBluetoothGatt.writeDescriptor(QppUsbCameraApi.getmDescriptor());
              }

            }
          }



        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
          connectStatus.set(false);
          isCallBack = true;
          if (status == BluetoothGatt.GATT_SUCCESS) {
          } else {
            Log.e(TAG, "Send failed!!!!");
          }
        }

      };

    }
  }

  private   byte[]   defaultEmpty={0,0,0,0,0,0,0,0,0,0};
  @Override
  public void onQppReceiveData(BluetoothGatt mBluetoothGatt, String qppUUIDForNotifyChar, byte[] result) {


    if(null != rfidlistion && null != result && result.length>0){

      if(QppUsbCameraApi.uuidQppNfcCharNotice.equalsIgnoreCase(qppUUIDForNotifyChar)){
        //NFC读卡数据

        int  dataLength  = result[result.length-1];
        if(dataLength == 0){
          return;
        }

        byte[]   datas = Arrays.copyOf(result,dataLength-1);
        byte    valbyte = getXor(datas);
        if(valbyte == result[dataLength-1]){

          play();
          rfidlistion.onNewRfid(datas, null);


        }

      }else if(QppUsbCameraApi.uuidQppElectricityNotice.equalsIgnoreCase(qppUUIDForNotifyChar)){
        //电量


        Integer  elect = getonElectricity(result);
        if(null != elect){
          rfidlistion.onElectricity(elect);
        }

      }

    }



  }

  public   ConStatus   getCurrent(){

    System.out.println("getCurrent--->"+address+":"+connectSucess+":"+connectionFailed);
    if(TextUtils.isEmpty(address)){
      return  ConStatus.NONE;
    }
    if(connectionFailed){
      return  ConStatus.CONNECTFAIL;
    }
    if(connectSucess ){
      return  ConStatus.CONNECTED;
    }
    return  ConStatus.UNCONNECT;
  }

  /**
   * 向蓝牙发送消息（灯光）
   * @param datas
   */
  public   void   sendLightData(byte[]   datas){
    if(null != mBluetoothGatt && connectSucess){
      QppUsbCameraApi.qppSendLightData(mBluetoothGatt,datas);
    }

  }

  /**清洗 */
  public   void   sendCleanData(byte[] datas){
    if(null != mBluetoothGatt && connectSucess){
      QppUsbCameraApi.qppSendCleanData(mBluetoothGatt,datas);
    }
  }

  private   void   play(){
    pool.play(soundID, 1f, 1f, 1, 0, 1.2f);
  }


  protected    Integer   getonElectricity(byte[]  results){
    String   data = ConvertUtils.bytes2HexString(results);
    if(data.equalsIgnoreCase("CC")){
      return 100;
    }else if(data.equalsIgnoreCase("c7")){
      return 90;
    }else if(data.equalsIgnoreCase("c3")){
      return 80;
    }else if(data.equalsIgnoreCase("BE")){
      return 70;
    }else if(data.equalsIgnoreCase("b9")){
      return 60;
    }else if(data.equalsIgnoreCase("b4")){
      return 50;
    }else if(data.equalsIgnoreCase("af")){
      return 40;
    }else if(data.equalsIgnoreCase("aa")){
      return 30;
    }else if(data.equalsIgnoreCase("a6")){
      return 20;
    }else if(data.equalsIgnoreCase("a1")){
      return 10;
    }else if(data.equalsIgnoreCase("9c")){
      return 0;
    }
    int   ele = Integer.parseInt(data,16);

    if(ele <204 && ele>=156){
      return 100-(204-ele)*2;
    }

    return null;
  }

  public static byte getXor(byte[] datas){

    byte temp=datas[0];

    for (int i = 1; i <datas.length; i++) {
      temp ^=datas[i];
    }

    return temp;
  }
}
