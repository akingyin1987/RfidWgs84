package com.bleqpp;

import java.util.List;

import android.app.AlertDialog;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class DialogUtil {
	public  static  int   postion=0;
	
	public static void showBluetoothDeviceDialog(Context context,
			final List<BluetoothDevice> devices, final DialogCallback cb) {
        postion = 0;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("请选择蓝牙设备");
		String[] blues = new String[devices.size()];
		for (int i = 0; i < devices.size(); i++) {
			BluetoothDevice device = devices.get(i);
			blues[i] = device.getName() + "[" + device.getAddress() + "]";
		}
		builder.setSingleChoiceItems(blues, 0, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
                 postion = which;
			}
		});
		builder.setNegativeButton("确定", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (null != cb) {
                     cb.onSussce(devices.get(postion));
				}

			}
		});
		builder.setNeutralButton("取消", null);
		builder.show();
	}

}
