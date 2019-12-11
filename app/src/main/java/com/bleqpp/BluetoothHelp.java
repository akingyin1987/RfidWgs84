package com.bleqpp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

/**
 * 蓝牙配对帮助类
 * @author zlcd
 *
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothHelp {
	// 蓝牙打印的UUID
		public static final String SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb";
		private static final String TAG = "BluetoothHelp";
		// 蓝牙适配器
		private BluetoothAdapter mBluetoothAdapter;
		private ProgressDialog mpd;
		private AlertDialog dialog = null;
		private BluetoothDevice mBluetoothReader;

		private WeakReference<Context>  mContextWeakReference;

		// 蓝牙读卡器设备标识
		private   String BLUETOOTH_MAC = "C0:00:00:0C:8F";

		// 消息标识符
		public static final int MESSAGE_STATE_CHANGE = 1;

		public static final int MESSAGE_DEVICE_NAME = 4;
		public static final int MESSAGE_DIALOG = 5;
		public static final String DIALOG = "dialog";

		public static final int STATE_CONNECTED = 2;
		public static final int STATE_CONNECTING = 1;

		private EnableBluetoothDeviceTask mEnableBluetoothDeviceTask;// 开启蓝牙

		
        public static final int MODE_PRIVATE = 0;
		
		private    BluetConnectListion   bluetConnectListion;
		
		
		

		

		public Context getContext() {
			return mContextWeakReference.get();
		}

		public void setContext(Context context) {
			//this.context = context;
			if(null == mContextWeakReference){
				mContextWeakReference = new WeakReference<>(context);
			}else{
				if(mContextWeakReference.get() != context){
					mContextWeakReference.clear();
					mContextWeakReference = new WeakReference<>(context);
				}

			}
		}

		public BluetConnectListion getBluetConnectListion() {
			return bluetConnectListion;
		}

		public void setBluetConnectListion(BluetConnectListion bluetConnectListion) {
			this.bluetConnectListion = bluetConnectListion;
		}

		// 是否搜索完毕
		private boolean serchOver = false;

	

		public BluetoothHelp(Context context,String   macblue) {
			//this.context = context;
			this.BLUETOOTH_MAC = macblue;

			if(null == mContextWeakReference){
				mContextWeakReference = new WeakReference<>(context);
			}else{
				mContextWeakReference.clear();
				mContextWeakReference = new WeakReference<>(context);
			}
			init();
		}

		/**
		 * 初始化
		 */
		public void init() {
			// 初始化
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if(null != mContextWeakReference.get()){
				mpd = new ProgressDialog(mContextWeakReference.get());
				mpd.setCancelable(false);
				dialog = new AlertDialog.Builder(mContextWeakReference.get()).setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create();

			}

		}

		public boolean CheckBluetooth() {

			// 检查蓝牙设备
			if (mBluetoothAdapter == null) {
				Log.d(TAG, "没有找到蓝牙设备");
				if(null == mContextWeakReference.get()){
					return  false;
				}
				new AlertDialog.Builder(mContextWeakReference.get()).setCancelable(false).setIcon(android.R.drawable.ic_dialog_info).setTitle("警告").setMessage("没有找到蓝牙设备").setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override public void onClick(DialogInterface dialog, int which) {
						// 通知用户开启蓝牙
						Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						mContextWeakReference.get().startActivity(intent);
					}
				});
				return false;
			}
			return true;
		}

		
		// 搜索蓝牙设备
		public void SearchBluetoothDevice() {

			// mBluetoothReader = getPairedDevice();
			// 查看所有已配对成功的蓝牙设备
			System.out.println("BLUETOOTH_MAC="+BLUETOOTH_MAC);
			if (!TextUtils.isEmpty(BLUETOOTH_MAC)) {
				Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
				for (int i = 0; i < devices.size(); i++) {
					BluetoothDevice device = devices.iterator().next();
					if (device.getAddress().equalsIgnoreCase(BLUETOOTH_MAC)) {
						mBluetoothReader = device;
						break;
					}
				}

			}

			//如果蓝牙未打开则请求打开蓝牙
			if (!mBluetoothAdapter.isEnabled()) {
				mEnableBluetoothDeviceTask = new EnableBluetoothDeviceTask();// 开启蓝牙
				mEnableBluetoothDeviceTask.execute();
				return;
			}

			if (mBluetoothReader != null) {
                if(null != bluetConnectListion){
                	bluetConnectListion.onSuccess(mBluetoothReader.getAddress());
                }

			} 
			if(mBluetoothAdapter.getBondedDevices().size() == 0){
				return;
			}
			if(null != mContextWeakReference.get()){
				DialogUtil.showBluetoothDeviceDialog(mContextWeakReference.get(), new ArrayList<BluetoothDevice>(mBluetoothAdapter.getBondedDevices()), new DialogCallback() {

					@Override
					public void onSussce(Object o) {
						try {
							BluetoothDevice  device = (BluetoothDevice) o;
							if(null !=  bluetConnectListion){
								BLUETOOTH_MAC = device.getAddress();
								bluetConnectListion.onSuccess(device.getAddress());
							}
						} catch (Exception e) {
							// TODO: handle exception
						}

					}
				});
			}

		}

		// 注册蓝牙监听相关事件

		public void registerBluetooth() {


			try {
				if (null != mBluetoothAdapter && !mBluetoothAdapter.isEnabled()) {
					System.out.println("未打开蓝牙");
					mEnableBluetoothDeviceTask = new EnableBluetoothDeviceTask();// 开启蓝牙
					mEnableBluetoothDeviceTask.execute();

				}
			}catch (Exception  e){
				e.printStackTrace();
			}

		}

		// 回收广播
	
		public void removeBluetoothListion() {
      //try {
			//	context.unregisterReceiver(mReceiver);
			//}catch (Exception e){
      	//e.printStackTrace();
			//}
		}

		private class EnableBluetoothDeviceTask extends AsyncTask<Integer, Integer, Boolean> {

			private static final int miSLEEP_TIME = 200;
			private static final int miWATI_TIME = 20;
			private static final int mTRY_TIME = 3;
			private BluetoothAdapter adapter = mBluetoothAdapter;

			private EnableBluetoothDeviceTask() {
			}

			@Override
			protected void onPreExecute() {
				closeDialog();
				if(null != mContextWeakReference.get()){
					mpd = new ProgressDialog(mContextWeakReference.get());
					mpd.setCancelable(false);
					mpd.setTitle("提示");
					mpd.setMessage("正在开启蓝牙设备，请稍后....");
					mpd.show();
				}

			}

			@Override
			protected Boolean doInBackground(Integer... params) {
				Boolean res = false;
				for (int i = 0; i < mTRY_TIME; i++) {

					if (!adapter.isEnabled()) {
						// 打开蓝牙设备
						adapter.enable();
					}

					for (int j = 0; j < miWATI_TIME; j++) {
						if (adapter.isEnabled()) {
							res = true;
							break;
						} else {
							try {
								Thread.sleep(miSLEEP_TIME);
							} catch (InterruptedException e) {
								break;
							}
						}
					}
				}

				return res;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				closeDialog();
				if (result) {
					findRfidReader();
					if(null != bluetConnectListion){
						bluetConnectListion.onSuccess(null);
					}
				} else {
					adapter.disable();
				}
			}
		}

	

		/**
		 * 扫描蓝牙设备
		 */
		public void findRfidReader() {
//			closeDialog();
//			mHandler.post(new Runnable() {
//				
//				@Override
//				public void run() {
//					if (mBluetoothAdapter.isDiscovering()) {
//						mBluetoothAdapter.cancelDiscovery();
//					}
//					serchOver = false;
//					mBluetoothAdapter.startDiscovery();
//					
//				}
//			});
			
//			mpd.setTitle("提示");
//			mpd.setMessage("正在寻找蓝牙设备，请稍后....");
//			mpd.show();

			
		}

		

	
		

		@SuppressWarnings("unused")
		private BluetoothDevice getPairedDevice() {
			if(null == mContextWeakReference.get()){
				return null;
			}
			String mac = KsiSharedStorageHelper.getBluetoothMac(KsiSharedStorageHelper.getPreferences(mContextWeakReference.get()));
			if (TextUtils.isEmpty(mac)) {
				return null;
			}
			
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac);
			if (device == null) {
				return null;
			}
			if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
				if(null != mContextWeakReference.get()){
					KsiSharedStorageHelper.deleteBluetoothMac(KsiSharedStorageHelper.getPreferences(mContextWeakReference.get()));
				}

				return null;
			}
			return device;
		}

	

		private void connectionLost(String msg) {
			if(null != mContextWeakReference.get()){
				@SuppressWarnings("unused")
				AlertDialog dialog = new AlertDialog.Builder(mContextWeakReference.get()).setTitle("警告").setCancelable(false).setIcon(android.R.drawable.ic_dialog_info).setMessage(msg).setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).show();
			}

		}

		private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				Log.d(TAG, action);

				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					//BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					//if (null == device || null == device.getName()) {
					//	return;
					//}
					//Log.d(TAG, device.getName() + ""+device.getAddress());
					//if (device.getAddress().equalsIgnoreCase(BLUETOOTH_MAC)) {
					//	closeDialog();
					//	mBluetoothReader = device;
					//	mBluetoothAdapter.cancelDiscovery();
					//	KsiSharedStorageHelper.setBluetoothMac(KsiSharedStorageHelper.getPreferences(context), device.getAddress());
					//    if(null != bluetConnectListion){
					//    	bluetConnectListion.onSuccess(device.getAddress());
					//    }
					//}
				} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					//if (serchOver) {
					//	return;
					//}
           //         removeBluetoothListion();
					//mBluetoothAdapter.cancelDiscovery();
          //
					//if (mBluetoothReader == null) {
					//	closeDialog();
					//	if(mBluetoothAdapter.getBondedDevices().size()  == 0){
					//		connectionLost("没有找到已匹配过的蓝牙设备");
					//		return;
					//	}
					//	DialogUtil.showBluetoothDeviceDialog(context, new ArrayList<BluetoothDevice>(mBluetoothAdapter.getBondedDevices()), new DialogCallback() {
					//
					//		@Override
					//		public void onSussce(Object o) {
					//			try {
					//				BluetoothDevice  device = (BluetoothDevice) o;
					//				if(null !=  bluetConnectListion){
					//					BLUETOOTH_MAC = device.getAddress();
					//					bluetConnectListion.onSuccess(device.getAddress());
					//				}
					//			} catch (Exception e) {
					//				// TODO: handle exception
					//			}
					//
					//		}
					//	});
					//}
					//serchOver = true;
				} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					System.out.println("addr="+device.getAddress()+":"+device.getBondState());
					//if (device.getAddress().equalsIgnoreCase(BLUETOOTH_MAC)) {
					//	mBluetoothReader = device;
					//	switch (mBluetoothReader.getBondState()) {
					//	case BluetoothDevice.BOND_BONDING:
					//		Log.d(TAG, "正在配对......");
					//		break;
					//	case BluetoothDevice.BOND_BONDED:
					//		Log.d(TAG, "完成配对");
					//		try {
					//			Thread.sleep(1000);
					//		} catch (InterruptedException e) {
					//			Log.e(TAG, e.toString());
					//		}
					//		closeDialog();
					//		 if(null != bluetConnectListion){
					//		    	bluetConnectListion.onSuccess(device.getAddress());
					//		   }
					//		break;
					//	case BluetoothDevice.BOND_NONE:
					//		Log.d(TAG, "取消配对");
					//		closeDialog();
					//		if(mBluetoothAdapter.getBondedDevices().size()  == 0){
					//			connectionLost("没有找到已匹配过的蓝牙设备");
					//			return;
					//		}
					//		DialogUtil.showBluetoothDeviceDialog(context, new ArrayList<BluetoothDevice>(mBluetoothAdapter.getBondedDevices()), new DialogCallback() {
					//
					//			@Override
					//			public void onSussce(Object o) {
					//				try {
					//					BluetoothDevice  device = (BluetoothDevice) o;
					//					if(null !=  bluetConnectListion){
					//						BLUETOOTH_MAC = device.getAddress();
					//						bluetConnectListion.onSuccess(device.getAddress());
					//					}
					//				} catch (Exception e) {
					//					// TODO: handle exception
					//				}
					//
					//			}
					//		});
          //
					//		break;
					//	default:
					//		Log.d(TAG, "未知行为");
					//		break;
					//	}
					//}

				} else if ("android.bluetooth.device.action.PAIRING_REQUEST".equals(action)) {
					Log.d(TAG, "设备密码");
					// try {
					// Method removeBondMethod = BluetoothDevice.class
					// .getDeclaredMethod("setPin",
					// new Class[] { byte[].class });
					// removeBondMethod.invoke(mBluetoothReader,
					// new Object[] { READER_PASSWORD.getBytes() });
					// } catch (Exception e) {
					// Log.e(TAG, e.toString());
					// }

				}
			}
		};

		// The Handler that gets information back from the BluetoothChatService
		@SuppressLint("HandlerLeak")
		private final Handler mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MESSAGE_STATE_CHANGE:
					switch (msg.arg1) {
					case STATE_CONNECTED:
						mpd.dismiss();
						Log.d(TAG, "已建立连接");

						break;
					case STATE_CONNECTING:
						Log.d(TAG, "正在建立连接");
						break;
					}
					break;

				case MESSAGE_DIALOG:
					mpd.dismiss();

					connectionLost(msg.getData().getString(DIALOG));
					break;
				}
			}
		};

		/**
		 * 关闭
		 */
		public void closeBluetoohHelp() {
			closeDialog();
			// 当Activity 被销毁是，取消所有的异步刷新
			if (mBluetoothAdapter != null) {
				mBluetoothAdapter.cancelDiscovery();

			}
			try {
				// 保证广播资源被正确回收
				if (mReceiver != null && null != mContextWeakReference.get()) {
					mContextWeakReference.get().unregisterReceiver(mReceiver);

				}
			} catch (Exception e) {
				// TODO: handle exception
			}

			if (mEnableBluetoothDeviceTask != null) {
				mEnableBluetoothDeviceTask.cancel(true);
			}
			
		}

		private void closeDialog() {
      try{
				if (null != mpd && mpd.isShowing()) {
					mpd.dismiss();
				}
				if (null != dialog && dialog.isShowing()) {
					dialog.dismiss();
				}
			}catch (Exception e){
				e.printStackTrace();
			}

		}
}
