package com.bleqpp;



import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author king
 * @version V1.0
 * @ Description:
 *
 * Company:重庆中陆承大科技有限公司
 * @ Date 2017/12/12 11:53
 */
public class KsiSharedStorageHelper {

	public static final String BLUETOOTH_MAC = "bluetooth_mac";

	public static final String BLUETOOTH_MAC2 = "bluetooth_mac2";

	public static final String BLUETOOTH_MAC3="bluetooth_mac3";

	/** 清洗 */
	public  static  final String  BLUETOOTH_CLEAN="bluetooth_clean";

	/** 灯光 */
	public  static  final String  BLUETOOTH_LIGHT="bluetooth_light";
	public  static  final   String   BLUETOOTH_SETTING="ble_setting";
	
	
	public  static    SharedPreferences   getPreferences(Context  context){
		return  context.getSharedPreferences(BLUETOOTH_SETTING, 0);
	}

	public static void setBluetoothMac(SharedPreferences sharedPreferences,
			String mac) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(BLUETOOTH_MAC, mac);
		editor.apply();
	}
	public static void setBluetoothMac2(SharedPreferences sharedPreferences,
			String mac) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(BLUETOOTH_MAC2, mac);
		editor.apply();
	}


	public static void setBluetoothMac3(SharedPreferences sharedPreferences,
			String mac) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(BLUETOOTH_MAC3, mac);
		editor.apply();
	}

	public static String getBluetoothMac(SharedPreferences sharedPreferences) {
		String strData = sharedPreferences.getString(BLUETOOTH_MAC, "");
		return strData;
	}
	public static String getBluetoothMac2(SharedPreferences sharedPreferences) {
		String strData = sharedPreferences.getString(BLUETOOTH_MAC2, "");
		return strData;
	}

	public static String getBluetoothMac3(SharedPreferences sharedPreferences) {
		String strData = sharedPreferences.getString(BLUETOOTH_MAC3, "");
		return strData;
	}

	/**
	 * 获取保存的灯光
	 * @param sharedPreferences
	 * @return
	 */
	public  static   int    getBluetoothLight(SharedPreferences  sharedPreferences){
		return  sharedPreferences.getInt(BLUETOOTH_LIGHT,0);
	}

	public   static   void    saveBluetoothLight(SharedPreferences sharedPreferences,int  light){
		sharedPreferences.edit().putInt(BLUETOOTH_LIGHT,light).apply();
	}

	/**
	 * 获取保存的清洗
	 * @param sharedPreferences
	 * @return
	 */
	public  static  int     getBluetoothClean(SharedPreferences sharedPreferences){
		return  sharedPreferences.getInt(BLUETOOTH_CLEAN,0);
	}

	public   static   void    saveBleutoothClean(SharedPreferences  sharedPreferences,int  clean){
		sharedPreferences.edit().putInt(BLUETOOTH_CLEAN,clean).apply();
	}

	public static void deleteBluetoothMac(SharedPreferences sharedPreferences) {
		sharedPreferences.edit().clear().apply();
	}

}
