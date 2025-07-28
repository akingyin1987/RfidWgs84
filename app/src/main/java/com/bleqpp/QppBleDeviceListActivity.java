package com.bleqpp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.akingyin.rfidwgs.R;
import com.akingyin.rfidwgs.db.BleDeviceRepairInfo;
import com.akingyin.rfidwgs.db.dao.BatichDbUtil;
import com.akingyin.rfidwgs.db.dao.BleDeviceRepairInfoDao;
import com.akingyin.rfidwgs.util.RxUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class QppBleDeviceListActivity extends ListActivity {

  private LeDeviceListAdapter mLeDeviceListAdapter;
  private BluetoothAdapter mBluetoothAdapter;
  private AtomicBoolean mScanning = new AtomicBoolean(false);
  private Handler mHandler;

  private static final int REQUEST_ENABLE_BT = 1;
  // Stops scanning after 10 seconds.
  private static final long SCAN_PERIOD = 10000;

  public TextView local_paired_devices;

  public Long batchId;
  public   Map<String,Integer> repairCache = new HashMap<>();

  private EditText  editText;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.select_bledevice_activity);
    if(null != getActionBar()){
      getActionBar().setTitle("选择蓝牙设备");
    }
    editText =  findViewById(R.id.edit_input);
    editText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
//         if(null != mLeDeviceListAdapter && null != s){
//           mLeDeviceListAdapter.setSelectMac(s.toString());
//         }
      }
    });
    Button button =findViewById(R.id.btn_query);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mLeDeviceListAdapter.setFilterMac(editText.getText().toString());
      }
    });
    mHandler = new Handler(Looper.getMainLooper());
    batchId = getIntent().getLongExtra("batchId", 0L);

    local_paired_devices =  findViewById(R.id.local_paired_devices);
    String  mac = KsiSharedStorageHelper.getBluetoothMac(KsiSharedStorageHelper.getPreferences(this));
    local_paired_devices.setText(MessageFormat.format("当前：{0}", mac));

    // Use this check to determine whether BLE is supported on the device.  Then you can
    // selectively disable BLE-related features.
    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
      finish();
    }

    // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
    // BluetoothAdapter through BluetoothManager.
    final BluetoothManager bluetoothManager =
        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();

    // Checks if Bluetooth is supported on the device.
    if (mBluetoothAdapter == null) {
      Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();

      return;
    }
    BleQppBluetoothServer.getInstance(this).connectDestroy();
    BleQppNfcCameraServer.getInstance(this).connectDestroy();
    Disposable disposable = Observable.create(new ObservableOnSubscribe<List<BleDeviceRepairInfo>>() {
      @Override
      public void subscribe(ObservableEmitter<List<BleDeviceRepairInfo>> emitter) throws Exception {
        emitter.onNext(BatichDbUtil.getBleDeviceRepairInfoDao().queryBuilder().where(BleDeviceRepairInfoDao.Properties.BatchId.eq(batchId)).list());

      }
    }).compose(RxUtil.IO_IO())
            .subscribe(new Consumer<List<BleDeviceRepairInfo>>() {
              @Override
              public void accept(List<BleDeviceRepairInfo> bleDeviceRepairInfos) throws Exception {
                repairCache.clear();
                for(BleDeviceRepairInfo info : bleDeviceRepairInfos){
                  repairCache.put(info.getBleDeviceAddress(), info.getRepairStatus());
                }
              }
            }, new Consumer<Throwable>() {
              @Override
              public void accept(Throwable throwable) throws Exception {

              }
            });
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    if (!mScanning.get()) {
      menu.findItem(R.id.menu_stop).setVisible(false);
      menu.findItem(R.id.menu_scan).setVisible(true);
      menu.findItem(R.id.menu_refresh).setActionView(null);
    } else {
      menu.findItem(R.id.menu_stop).setVisible(true);
      menu.findItem(R.id.menu_scan).setVisible(false);
      menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
    }
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == R.id.menu_scan){
      mLeDeviceListAdapter.clear();
      scanLeDevice(true);

    }else if(item.getItemId() == R.id.menu_stop){
      scanLeDevice(false);
    }

    return true;
  }

  @Override protected void onResume() {
    super.onResume();

    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
    // fire an intent to display a dialog asking the user to grant permission to enable it.
    if (!mBluetoothAdapter.isEnabled()) {
      if (!mBluetoothAdapter.isEnabled()) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
      }
    }
    if(null == mLeDeviceListAdapter){
      mLeDeviceListAdapter = new LeDeviceListAdapter();
      setListAdapter(mLeDeviceListAdapter);
      String  mac = KsiSharedStorageHelper.getBluetoothMac(KsiSharedStorageHelper.getPreferences(this));
      mLeDeviceListAdapter.setSelectMac(mac);

    }
    // Initializes list view adapter.

    scanLeDevice(true);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // User chose not to enable Bluetooth.
    if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
      finish();
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override protected void onPause() {
    super.onPause();
    scanLeDevice(false);
    mLeDeviceListAdapter.clear();
  }

  @Override protected void onListItemClick(ListView l, View v, int position, long id) {
    final BleDevice bleDevice = mLeDeviceListAdapter.getDevice(position);
    if (bleDevice == null) {
      return;
    }
    BluetoothDevice device = bleDevice.device;

    if (mScanning.get()) {
      if(null != mBLEScanner){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          mBLEScanner.stopScan(mScanCallback);
        }
      }else{
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
      }
      mScanning.set(false);
    }
    KsiSharedStorageHelper.setBluetoothMac(
        KsiSharedStorageHelper.getPreferences(QppBleDeviceListActivity.this), device.getAddress());
    local_paired_devices.setText(MessageFormat.format("当前：{0}", device.getAddress()));
    mLeDeviceListAdapter.setSelectMac(device.getAddress());
    mLeDeviceListAdapter.notifyDataSetChanged();
  }

  private  BluetoothLeScanner  mBLEScanner = null;
  private  MyScanCallback   mScanCallback = null;
  private void scanLeDevice(final boolean enable) {
    if(ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.BLUETOOTH_SCAN},100);
            return;
        }
    }

    if (enable) {
      // Stops scanning after a pre-defined scan period.
      mHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          stopScan();
        }
      },SCAN_PERIOD);
      mHandler.post(new Runnable() {
        @Override public void run() {
          mScanning.set(false);
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //5.0采用新的API

            mBLEScanner  = mBluetoothAdapter.getBluetoothLeScanner();
            if(null == mBLEScanner || !mBluetoothAdapter.isEnabled()){
              Toast.makeText(QppBleDeviceListActivity.this,"请打开蓝牙",Toast.LENGTH_SHORT).show();
              Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
              startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
              return;
            }
            if(null == mScanCallback){
              mScanCallback = new MyScanCallback();
            }


            ScanSettings   mScanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
            mBLEScanner.startScan(null,mScanSettings,mScanCallback);

          }else{
            mBluetoothAdapter.startLeScan(mLeScanCallback);
          }
          mScanning.set(true);
          invalidateOptionsMenu();
        }
      });

      mScanning.set(true);
    //  mBluetoothAdapter.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        //5.0采用新的API
          mBLEScanner  = mBluetoothAdapter.getBluetoothLeScanner();
         if(null == mBLEScanner || !mBluetoothAdapter.isEnabled()){
          Toast.makeText(QppBleDeviceListActivity.this,"请打开蓝牙",Toast.LENGTH_SHORT).show();
           Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
           startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
          return;
        }
          if(null == mScanCallback){
            mScanCallback = new MyScanCallback();
          }

        //ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("00007777-0000-1000-8000-00805f9b34fb"))
        //
        //        .build();
        ScanSettings   mScanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();
        mBLEScanner.startScan(null,mScanSettings,mScanCallback);

      }else{
        mBluetoothAdapter.startLeScan(mLeScanCallback);
      }

    } else {
      mScanning.set(false);

      if(null != mBLEScanner){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          mBLEScanner.stopScan(mScanCallback);
        }
      }else{
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
      }
    }
    invalidateOptionsMenu();
  }

  // Adapter for holding devices found through scanning.
  @SuppressLint("InflateParams")
  private class LeDeviceListAdapter extends BaseAdapter {
    private ArrayList<BleDevice> mLeDevices;
    private LayoutInflater mInflator;


    private   String   selectMac;

    private  String  filterMac;

    public String getFilterMac() {
      return filterMac;
    }

    public void setFilterMac(String filterMac) {
      boolean change = TextUtils.equals(filterMac,this.filterMac);
      if(change){
        ArrayList<BleDevice>  list = new ArrayList<>();
        Iterator<BleDevice> iterator =mLeDeviceListAdapter.mLeDevices.iterator();
        while (iterator.hasNext()){
          BleDevice bleDevice = iterator.next();
          if(null != bleDevice){
            if(TextUtils.isEmpty(filterMac)){
              list.add(bleDevice);
            }else{
              if(bleDevice.device.getAddress().contains(filterMac)){
                list.add(bleDevice);
              }
            }

          }
        }
        mLeDevices.clear();
        mLeDevices.addAll(list);
        notifyDataSetChanged();
      }
      this.filterMac = filterMac;
    }

    public String getSelectMac() {
      return selectMac;
    }

    public void setSelectMac(String selectMac) {

      this.selectMac = selectMac;
      this.notifyDataSetChanged();

    }

    public LeDeviceListAdapter() {
      super();
      mLeDevices = new ArrayList<>();
      mInflator = QppBleDeviceListActivity.this.getLayoutInflater();
    }

    public void addDevice(BleDevice device) {
      System.out.println("addDevice="+device.device.getAddress());
      BluetoothDevice dev = device.device;
      Iterator<BleDevice> iterator =mLeDeviceListAdapter.mLeDevices.iterator();
      while (iterator.hasNext()){
        BleDevice bleDevice = iterator.next();
        if(null != bleDevice){
          if (dev.getAddress().equalsIgnoreCase(bleDevice.device.getAddress())) {
//            stopScan();
            return;
          }
        }
      }

      System.out.println("添加新设备="+device.device.getAddress());
      if(!TextUtils.isEmpty(filterMac) ){
        if(device.device.getAddress().contains(filterMac)){
          mLeDevices.add(device);
          notifyDataSetChanged();
        }

      }else{
        mLeDevices.add(device);
        notifyDataSetChanged();
      }

    }

    public BleDevice getDevice(int position) {
      return mLeDevices.get(position);
    }

    public void clear() {
      mLeDevices.clear();
    }

    @Override public int getCount() {
      return mLeDevices.size();
    }

    @Override public Object getItem(int i) {
      return mLeDevices.get(i);
    }

    @Override public long getItemId(int i) {
      return i;
    }

    @Override public View getView(int i, View view, ViewGroup viewGroup) {
      ViewHolder viewHolder;
      // General ListView optimization code.
      if (view == null) {
        view = mInflator.inflate(R.layout.qpp_listitem_device, null);
        viewHolder = new ViewHolder();
        viewHolder.deviceAddress =  view.findViewById(R.id.text_device_address);
        viewHolder.deviceName =  view.findViewById(R.id.text_device_name);
        viewHolder.rssi =  view.findViewById(R.id.text_rssi);
        viewHolder.repairState = view.findViewById(R.id.text_device_repair_state);
        view.setTag(viewHolder);
      } else {
        viewHolder = (ViewHolder) view.getTag();
      }

      BleDevice BleDevice = mLeDevices.get(i);
      BluetoothDevice device = BleDevice.device;
      final String deviceName = device.getName();
      if (deviceName != null && deviceName.length() > 0) {
        viewHolder.deviceName.setText(deviceName);
      } else {
        viewHolder.deviceName.setText(R.string.unknown_device);
      }
      viewHolder.deviceAddress.setText(device.getAddress());
      viewHolder.rssi.setText("RSSI: " + BleDevice.rssi + "db");
      if(BleDevice.repairState == 0){
        viewHolder.repairState.setText("维修状态：未维修");
        viewHolder.repairState.setTextColor(Color.BLACK);
      }else if(BleDevice.repairState == 1){
        viewHolder.repairState.setText("维修状态：维修中");
        viewHolder.repairState.setTextColor(Color.RED);
      }else if(BleDevice.repairState == 2){
        viewHolder.repairState.setTextColor(Color.RED);
        viewHolder.repairState.setText("维修状态：已损坏");
      }else if(BleDevice.repairState == 3){
        viewHolder.repairState.setTextColor(Color.GREEN);
        viewHolder.repairState.setText("维修状态：已维修");
      }
      if(!TextUtils.isEmpty(selectMac)){
         if(TextUtils.equals(selectMac,device.getAddress())){
           viewHolder.deviceAddress.setTextColor(Color.RED);
         }else{
           viewHolder.deviceAddress.setTextColor(Color.BLACK);
         }
      }else{
        viewHolder.deviceAddress.setTextColor(Color.BLACK);
      }
      return view;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private class   MyScanCallback   extends  ScanCallback{
    public MyScanCallback() {
      super();
    }

    @Override public void onScanResult(int callbackType, ScanResult result) {
      super.onScanResult(callbackType, result);
      if(null != mLeScanCallback){
        mLeScanCallback.onLeScan(result.getDevice(),result.getRssi(),null);
      }
    }

    @Override public void onBatchScanResults(List<ScanResult> results) {
      super.onBatchScanResults(results);

    }

    @Override public void onScanFailed(int errorCode) {
      super.onScanFailed(errorCode);
    }
  }


  private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
      System.out.println("onLeScan="+mScanning.get());
      if(!mScanning.get()){
        return;
      }
      if(TextUtils.isEmpty(device.getName())){
        return;
      }
      runOnUiThread(new Runnable() {
        @Override public void run() {

           BleDevice bleDevice = new BleDevice();
           bleDevice.device = device;
           bleDevice.rssi = rssi;
           bleDevice.repairState = null == repairCache.get(device.getAddress())?0:repairCache.get(device.getAddress());
           mLeDeviceListAdapter.addDevice(bleDevice);


        }
      });
    }
  };

  static class BleDevice {
    BluetoothDevice device;
    int rssi;
    int repairState;
  }

  static class ViewHolder {
    TextView deviceName;
    TextView deviceAddress;
    TextView repairState;
    TextView rssi;
  }


  public void stopScan(){
    mScanning.set(false);
    if(null != mBLEScanner){
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mBLEScanner.stopScan(mScanCallback);
      }
    }else{
      mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }
  }
  @Override public void onBackPressed() {
    mHandler.removeCallbacksAndMessages(null);
    mScanning.set(false);
    if(null != mBLEScanner){
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mBLEScanner.stopScan(mScanCallback);
      }
    }else{
      mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }
    super.onBackPressed();
  }
}
