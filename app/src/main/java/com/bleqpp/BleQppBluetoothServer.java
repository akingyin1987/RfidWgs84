package com.bleqpp;
import android.os.Looper;

import android.os.Message;
import com.zlcdgroup.nfcsdk.ConStatus;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.quintic.libqpp.QppApi;
import com.quintic.libqpp.iQppCallback;
import com.zlcdgroup.nfcsdk.RfidConnectorInterface;
import com.zlcdgroup.nfcsdk.SDKInterface;

/**
 * 基于ble连接的外置读卡器
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleQppBluetoothServer implements SDKInterface, BluetConnectListion,iQppCallback {
	public static BluetoothGatt mBluetoothGatt = null;
	protected static final String TAG = BleQppBluetoothServer.class.getSimpleName();


	private WeakReference<Context>  mContextWeakReference;

	private BluetoothManager bluetoothManager = null;

	private BluetoothAdapter mBluetoothAdapter;
	private List<String> blemac = new ArrayList<>();

	protected static String uuidQppService = "0000fee9-0000-1000-8000-00805f9b34fb";
	protected static String uuidQppCharWrite = "d44bc439-abfd-45a2-b575-925416129600";

	private  BluetoothGattCallback mGattCallback;

	private BleQppBluetoothServer(Context context) {
		// TODO Auto-generated constructor stub
		if(null == mContextWeakReference){
			mContextWeakReference = new WeakReference<>(context);
		}else{
			if(null == mContextWeakReference.get() || mContextWeakReference.get() != context){
				mContextWeakReference.clear();
				mContextWeakReference = new WeakReference<>(context);
			}

		}
		init();
	}

	private static BleQppBluetoothServer instance;

	public Handler mHandler;

	

	private static BluetoothHelp bluetoothHelp;

	public static BleQppBluetoothServer getInstance(Context context) {

		if (null == instance) {
			instance = new BleQppBluetoothServer(context);

		}
		if(null == instance.mContextWeakReference){
			instance.mContextWeakReference = new WeakReference<>(context);
		}else{
			if(null == instance.mContextWeakReference.get() || instance.mContextWeakReference.get() != context){
				instance.mContextWeakReference.clear();
				instance.mContextWeakReference = new WeakReference<>(context);
			}

		}
		if (null == bluetoothHelp) {
			bluetoothHelp = new BluetoothHelp(instance.mContextWeakReference.get(), address);
		}
		bluetoothHelp.setContext(instance.mContextWeakReference.get());
		bluetoothHelp.setBluetConnectListion(instance);
		return instance;
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
			 if(msg.what == 1){


			 }else if(msg.what == 2){

			 }
		 }
	 };

	@SuppressLint({ "InlinedApi", "NewApi" })
	public void init() {

		mHandler = new Handler(Looper.getMainLooper());
		bluetoothManager = (BluetoothManager) mContextWeakReference.get().getSystemService(Context.BLUETOOTH_SERVICE);
		if (null != bluetoothManager) {
			mBluetoothAdapter = bluetoothManager.getAdapter();

		}
		initBleCallback();

		QppApi.setCallback(this);
        
	}

	
	

	@Override
	public boolean connectDestroy() {
		if(null != callbackHandle){
			callbackHandle.removeMessages(1);
			callbackHandle.removeMessages(2);
		}
		disconnect();
		close();
		blemac.clear();
		bluetoothHelp = null;
		address="";
		System.out.println("关闭成功");
		instance = null;
		return false;
	}

	@Override
	public boolean initConnect() {
		init();
		return false;
	}
	
	@SuppressLint("SimpleDateFormat")
	public   static SimpleDateFormat   format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@SuppressLint("NewApi")
	@Override
	public void onregistered(RfidConnectorInterface rfidlistion) {

		initBleCallback();
		this.rfidlistion = rfidlistion;
		if(null != mBluetoothAdapter && !mBluetoothAdapter.isEnabled()){
			connectionFailed = true;
		}
		if (null != bluetoothHelp) {
			bluetoothHelp.registerBluetooth();
		}		
		String mac = "";
		if(null != mContextWeakReference.get()){
			mac=KsiSharedStorageHelper.getBluetoothMac(KsiSharedStorageHelper.getPreferences(mContextWeakReference.get()));
		}
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



	@Override
	public void onSuccess(String bluemac) {
		initBleCallback();
		if(TextUtils.isEmpty(bluemac)){
			String mac ="";
			if(null != mContextWeakReference.get()){
				mac =  KsiSharedStorageHelper
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
	public  boolean   isCallBack= false;//回调是否成功
	public  boolean   connect(String  address){
		if(null == instance){
			return false;
		}
		isCallBack = false;
		//callbackHandle.sendEmptyMessageDelayed(1,10*1000);
		return  connect(address,false);
	}
	//连接
	@SuppressLint("NewApi")
	public boolean connect(  String address,boolean  authConnect) {
		if(null == instance){
			return false;
		}
		if(null != rfidlistion){
			rfidlistion.onConnectStatus(ConStatus.CONNECTING);
		}
		if(null != bluetoothHelp){
			mHandler.post(new Runnable() {
				@Override public void run() {
					if(null != rfidlistion){
						if(bluetoothHelp.CheckBluetooth()){
							bluetoothHelp.registerBluetooth();
						}
					}
				}
			});
		}

		if (mBluetoothAdapter == null || address == null || TextUtils.isEmpty(address)) {
			System.out.println("BluetoothAdapter not initialized or unspecified address.");
			Log.w("Qn Dbg",
					"BluetoothAdapter not initialized or unspecified address.");
			setConnectBleStatus(ConStatus.NONE);
			return false;
		}
		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");

			setConnectBleStatus(ConStatus.CONNECTFAIL);
			return false;
		}

    if(null == mGattCallback){
			initBleCallback();
		}
		// setting the autoConnect parameter to false.
		if(null != mContextWeakReference.get()){
			mBluetoothGatt = device.connectGatt(mContextWeakReference.get(), false, mGattCallback);

		}


		Log.d(TAG, "Trying to create a new connection. Gatt: " + mBluetoothGatt);


		return true;
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
	

	public   void   initBleCallback(){
		if(null == mGattCallback){
			System.out.println("====mGattCallback====");
			mGattCallback = new BluetoothGattCallback() {
				@Override
				public void onConnectionStateChange(BluetoothGatt gatt, int status,
						int newState) {
					isCallBack = true;
          System.out.println("onConnectionStateChange:"+status+':'+newState);
					Log.i(TAG, "onConnectionStateChange : " + status + "  newState : "
							+ newState);
					if (newState == BluetoothProfile.STATE_CONNECTED) {
						connectSucess = true;
						Log.d("Ble APP", "连接成功");
						setConnectBleStatus(ConStatus.CONNECTED);
						mBluetoothGatt.discoverServices();
						connectionFailed = false;
					} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.w("Ble APP", "连接失败");

						setConnectBleStatus(ConStatus.CONNECTFAIL);
						if(status == 133 || status == 19 ||status == BluetoothGatt.GATT_FAILURE ){
							connectionFailed = true;
							reConnectCount =0;
							if(null != mBluetoothGatt && connectSucess){
								try{
									mBluetoothGatt.disconnect();
									mBluetoothGatt.close();
								}catch (Exception e){
									e.printStackTrace();
								}
								connectSucess = false;
							}
							return;
						}

						try{
							gatt.disconnect();
							gatt.close();
						}catch (Exception  e){
							e.printStackTrace();
						}
						if(!connectionFailed){
							connectionFailed = true;
						}
						if (reConnectCount == MAX_RECONNECT_TIMES) {
							return;
						}
						close();
						connect(address);
						connectionFailed= true;
						reConnectCount++;
					}

				}

				@Override
				public void onServicesDiscovered(BluetoothGatt gatt, int status) {
					System.out.println("onServicesDiscovered:"+status);
					isCallBack = true;
					if (QppApi.qppEnable(mBluetoothGatt, uuidQppService, uuidQppCharWrite)) {

					} else {

					}
				}

				@Override
				public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
					QppApi.updateValueForNotification(gatt, characteristic);
					isCallBack = true;
					System.out.println("onCharacteristicChanged");
				}

				@Override
				public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

					isCallBack = true;
					Log.w(TAG, "onDescriptorWrite");
					Log.w(TAG,"status="+status);

					QppApi.setQppNextNotify(gatt, true);
				}

				@Override
				public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
					// super.onCharacteristicWrite(gatt, characteristic, status);
					isCallBack = true;
					if (status == BluetoothGatt.GATT_SUCCESS) {
					} else {
						Log.e(TAG, "Send failed!!!!");
					}
				}
			};
		}
	}

	@Override
	public void onQppReceiveData(BluetoothGatt mBluetoothGatt, String qppUUIDForNotifyChar, byte[] result) {

        System.out.println("接受到返回数据---->>");
		if(null != rfidlistion && null != result && result.length>0){
			rfidlistion.onNewRfid(result, null);
		}

	}

	public   ConStatus   getCurrent(){

		if(TextUtils.isEmpty(address)){
			return  ConStatus.NONE;
		}

		if(connectionFailed){
			return  ConStatus.CONNECTFAIL;
		}

		if(connectSucess && !connectionFailed){
			return  ConStatus.CONNECTED;
		}


		return  ConStatus.UNCONNECT;
	}
}
