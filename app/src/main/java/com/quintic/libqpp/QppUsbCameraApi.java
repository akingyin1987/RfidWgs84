package com.quintic.libqpp;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;

import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author king
 * @version V1.0
 * @ Description:
 * @ Date 2017/12/13 10:47
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class QppUsbCameraApi {

  private static ArrayList<BluetoothGattCharacteristic> arrayNtfCharList =
      new ArrayList<>();

  private static ArrayList<BluetoothGattCharacteristic> arrayElectricityCharlist
      = new ArrayList<>();


  /**灯光 */
  private static BluetoothGattCharacteristic writeLightCharacteristic;
  /** 清洗 */
  private static BluetoothGattCharacteristic writeCleanCharacteristic;

  /** 服务 */
  private static String uuidQppService = "0000fee9-0000-1000-8000-00805f9b34fb";


  /** 控制灯光UUID*/
  private static String uuidQPPLightWrite="0000feec-0000-1000-8000-00805f9b34fb";

  /** 控制清洗UUID */
  private static String uuidQppCleanWrite="0000feed-0000-1000-8000-00805f9b34fb";

  /** NFC数据 */
  public static String uuidQppNfcCharNotice="0000feea-0000-1000-8000-00805f9b34fb";

  /**电量数据 */
  public  static  String uuidQppElectricityNotice="0000feeb-0000-1000-8000-00805f9b34fb";

  /** 心跳数据 */
  public static String HEART_RATE_MEASUREMENT = "0000ffa6-0000-1000-8000-00805f9b34fb";

  public static final int qppServerBufferSize = 20;
  /// receive data
  /** NFC */
  private static BluetoothGattCharacteristic notifyNfcCharacteristic;
  private static BluetoothGattCharacteristic notifyElectricityCharacteristic;
  /** notify Characteristic*/
  private static byte notifyCharaIndex = 0;
  private static byte notifyElectricityIndex = 0;
  private static boolean NotifyEnabled = false;
  private static final String UUID_NFC = "00002902-0000-1000-8000-00805f9b34fb";
  private static final String UUID_ELECTRICITY="00002902-0000-1000-8000-00805f9b34fb";
  private static String TAG = QppApi.class.getSimpleName();

  private static iQppCallback iQppCallback;

  public static void setCallback(iQppCallback mCb) {
    iQppCallback = mCb;
  }


  public static void updateValueForNotification(BluetoothGatt bluetoothGatt,
      BluetoothGattCharacteristic characteristic) {
    if (bluetoothGatt == null || characteristic == null) {
      Log.e(TAG, "invalid arguments");
      return;
    }

    String strUUIDForNotifyChar = characteristic.getUuid().toString();

     byte[] qppData = characteristic.getValue();
    if (qppData != null && qppData.length > 0) {
      iQppCallback.onQppReceiveData(bluetoothGatt, strUUIDForNotifyChar, qppData);
    }
  }

  private static void resetQppField() {
    writeCleanCharacteristic = null;
    writeLightCharacteristic = null;
    notifyElectricityCharacteristic = null;
    notifyNfcCharacteristic = null;
    arrayNtfCharList.clear();
    arrayElectricityCharlist.clear();
    //NotifyEnabled=false;
    notifyCharaIndex = 0;
    notifyElectricityIndex = 0;
  }

  public static boolean qppEnable(BluetoothGatt bluetoothGatt) {
    resetQppField();

    if (bluetoothGatt == null ) {
      Log.e(TAG, "invalid arguments");
      return false;
    }
    BluetoothGattService qppService = bluetoothGatt.getService(UUID.fromString(uuidQppService));
    if (qppService == null) {
      Log.e(TAG, "Qpp service not found");
      return false;
    }
    List<BluetoothGattCharacteristic> gattCharacteristics = qppService.getCharacteristics();
    for (int j = 0; j < gattCharacteristics.size(); j++) {
      BluetoothGattCharacteristic chara = gattCharacteristics.get(j);
      if (chara.getUuid().toString().equals(uuidQPPLightWrite)) {
        writeLightCharacteristic = chara;
        System.out.println("灯光控制");
      }else if(chara.getUuid().toString().equals(uuidQppCleanWrite)){
        writeCleanCharacteristic = chara;
        System.out.println("清洗控制");
      } else if (chara.getUuid().toString().equalsIgnoreCase(uuidQppNfcCharNotice)) {

        notifyNfcCharacteristic = chara;

        setCharacteristicNotification(bluetoothGatt, notifyNfcCharacteristic, true);
      }else if(chara.getUuid().toString().equalsIgnoreCase(uuidQppElectricityNotice)){
        System.out.println("电量==="+uuidQppElectricityNotice);
        notifyElectricityCharacteristic = chara;
        setCharacteristicNotification(bluetoothGatt, notifyElectricityCharacteristic, true);
      }else if(chara.getUuid().toString().equalsIgnoreCase(HEART_RATE_MEASUREMENT)){
        //添加心跳
        setCharacteristicNotification(bluetoothGatt,chara,true);
      }
    }



    return true;
  }

  /// data sent
  public static boolean qppSendLightData(BluetoothGatt bluetoothGatt, byte[] qppData) {
    boolean ret = false;
    if (bluetoothGatt == null) {
      Log.e(TAG, "BluetoothAdapter not initialized !");
      return false;
    }

    if (qppData == null) {
      Log.e(TAG, "qppData = null !");
      return false;
    }
    return writeValue(bluetoothGatt, writeLightCharacteristic, qppData);
  }

  public static boolean qppSendCleanData(BluetoothGatt bluetoothGatt, byte[] qppData) {
    boolean ret = false;
    if (bluetoothGatt == null) {
      Log.e(TAG, "BluetoothAdapter not initialized !");
      return false;
    }

    if (qppData == null) {
      Log.e(TAG, "qppData = null !");
      return false;
    }
    return writeValue(bluetoothGatt, writeCleanCharacteristic, qppData);
  }

  public static void PrintBytes(byte[] bytes) {
    if (bytes == null) {
      return;
    }
    final StringBuilder stringBuilder = new StringBuilder(bytes.length);
    for (byte byteChar : bytes) {
      stringBuilder.append(String.format("%02X ", byteChar));
    }
    Log.i(TAG, " :" + stringBuilder.toString());
  }

  private static boolean writeValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
      byte[] bytes) {
    if (gatt == null) {
      Log.e(TAG, "BluetoothAdapter not initialized");
      return false;
    }
    if(null == characteristic){
      return  false;
    }
    characteristic.setValue(bytes);
    return gatt.writeCharacteristic(characteristic);
  }

  public static boolean setQppNextNotify(BluetoothGatt bluetoothGatt, boolean EnableNotifyChara) {

      return true;
  }


  private  static BluetoothGattDescriptor    mDescriptor;
  private  static BluetoothGattDescriptor    descriptor_FEEA;

  public static BluetoothGattDescriptor getDescriptor_FEEA() {
    return descriptor_FEEA;
  }

  public static BluetoothGattDescriptor getmDescriptor() {
    return mDescriptor;
  }

  private static boolean setCharacteristicNotification(BluetoothGatt bluetoothGatt,
      BluetoothGattCharacteristic characteristic, boolean enabled) {
    if (bluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized");
      return false;
    }

    bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    try {
      BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(UUID_NFC));

      if (descriptor != null) {
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if(characteristic.getUuid().toString().equalsIgnoreCase(uuidQppElectricityNotice)){
          mDescriptor = descriptor;
          return  true;
        }
        if(characteristic.getUuid().toString().equalsIgnoreCase(uuidQppNfcCharNotice)){
          descriptor_FEEA = descriptor;
        }
        return (bluetoothGatt.writeDescriptor(descriptor));
      } else {
        Log.e(TAG, "descriptor is null");
        return false;
      }
    } catch (NullPointerException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
    return true;
  }
}
