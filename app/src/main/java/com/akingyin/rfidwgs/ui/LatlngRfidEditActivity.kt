package com.akingyin.rfidwgs.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.GpsStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.StackingBehavior
import com.akingyin.rfidwgs.BuildConfig
import com.akingyin.rfidwgs.R
import com.akingyin.rfidwgs.db.Batch
import com.akingyin.rfidwgs.db.LatLngRfid
import com.akingyin.rfidwgs.db.dao.BatichDbUtil
import com.akingyin.rfidwgs.db.dao.LatLngRfidDao
import com.akingyin.rfidwgs.db.vo.LatLngVo
import com.akingyin.rfidwgs.ext.*
import com.akingyin.rfidwgs.util.*
import com.bleqpp.BleQppNfcCameraServer
import com.bleqpp.KsiSharedStorageHelper
import com.bleqpp.QppBleDeviceListActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zlcdgroup.nfcsdk.ConStatus
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_rfid_latlng_edit.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.MessageFormat
import java.util.concurrent.TimeUnit

/**
 * 获取定位与标签信息
 * @ Description:
 * @author king
 * @ Date 2019/12/6 16:01
 * @version V1.0
 */

@SuppressLint("MissingPermission")
class LatlngRfidEditActivity : BaseActivity(), LocationListener, GpsStatus.Listener {

    var MAX_LATLNG = 30
    var MAX_REPEAT_LATLNG = 15
    var MAX_ACCURACY = 5F
    var MIN_SATELLITE = 24
    //卫星的信噪比
    var MIN_SNR = 0.1F


    var batchId: Long = 0L
    //当前获取卫星数据
    var currentSatellite = -1

    //通过设置信噪比符合要求的卫星数
    var accordSatellite = -1



    var batch: Batch? = null

    var latLngRfid: LatLngRfid? = null

    lateinit var locationManager: LocationManager

    override fun getLayoutId() = R.layout.activity_rfid_latlng_edit


    var cacheLatlngs = mutableListOf<LatLngVo>()

    var averageLatlng = LatLngVo()

    var repeatCount: Int = 0

    var currentLatlng: Location? = null

    override fun initView() {

        tv_toolbar_title.text="定位与标签"
        setToolBar(toolbar, "")

        MAX_LATLNG = spGetInt("max_latlng", 30)
        MAX_REPEAT_LATLNG = spGetInt("max_repeat_latlng", 15)
        MAX_ACCURACY = spGetFloat("max_accuracy", 5F)
        MIN_SATELLITE = spGetInt("min_satellite",24)
        MIN_SNR= spGetFloat("min_snr",0.1F)
        batchId = intent.getLongExtra("batchId", 0)

        batch = BatichDbUtil.getBatichDao().load(batchId)
        if (null == batch) {
            showMsg("数据出错了")
            finish()
            return
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            openGPS()
            btn_lat_lng.isEnabled = false
        }
        initGpsStatus()

        btn_lat_lng.setOnClickListener {
            onStartLocation()
        }
        btn_delect_latlng.setOnClickListener {
            cacheLatlngs.clear()
            averageLatlng = LatLngVo()
            repeatCount = 0
            currentLatlng = null
            btn_lat_lng.tag = "0"
            btn_lat_lng.text = "开始定位"
            onStopLocation()
            btn_delect_latlng.visibility = View.VISIBLE
            btn_lat_lng.visibility = View.VISIBLE
            initLocationInfo()


        }
        tv_ble_status.setOnClickListener {
           try {
               var  status = it.tag.toString().toInt()
               when(status){
                   1->{
                       startActivity(Intent(this@LatlngRfidEditActivity,QppBleDeviceListActivity::class.java))
                   }
                   2->{
                       handleConnectStatus(ConStatus.CONNECTING)
                       if(BLE_NFC_CARD == 1 && isSupportBle){
                           BleQppNfcCameraServer.getInstance(this).onSuccess(null)
                       }
                   }
                   4->{
                       handleConnectStatus(ConStatus.CONNECTING)
                       if(BLE_NFC_CARD == 1 && isSupportBle){
                           BleQppNfcCameraServer.getInstance(this).onSuccess(null)
                       }
                   }
                   else ->{}
               }
           }catch (e : Exception){
               e.printStackTrace()
           }
        }
        val mac = KsiSharedStorageHelper.getBluetoothMac(KsiSharedStorageHelper.getPreferences(this))
        tv_ble_addr.text=MessageFormat.format("读卡器地址：{0}",mac)
        BLE_NFC_CARD = spGetInt("ble_cardread")
        println("ble=$BLE_NFC_CARD")
        sc_setting_ble.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                spSetInt("ble_cardread",1)
                BLE_NFC_CARD =1
                BleQppNfcCameraServer.getInstance(this).onregistered(this)

                BleQppNfcCameraServer.getInstance(this).connect(mac)
            }else{
                spSetInt("ble_cardread",0)
                BLE_NFC_CARD =0
                BleQppNfcCameraServer.getInstance(this).connectDestroy()

            }
            initBleInfo()
        }
        initBleInfo()
        tv_ble_addr.setOnClickListener {
            startActivity(Intent(this,QppBleDeviceListActivity::class.java))
        }
        initBatchInfo(batch!!)

    }

    @SuppressLint("MissingPermission")
    private   fun   initGpsStatus(){
        var rxPermissions = RxPermissions(this)
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe {
                    if(it){
                        locationManager.addGpsStatusListener(this)
                    }else{
                        showMsg("请允许必要的条件")
                    }
                }

    }


    private   fun  onStartTencentLocation(){

//        val request = TencentLocationRequest.create().apply {
//            interval = 2000
//        }
//        val mLocationManager = TencentLocationManager.getInstance(this)
//        mLocationManager.removeUpdates(null)
//        mLocationManager.coordinateType = TencentLocationManager.COORDINATE_TYPE_WGS84
//       var error =  mLocationManager.requestLocationUpdates(request,this)
//       if(error != 0){
//           showMsg("注册定位失败，代码：$error")
//       }
    }

    private   fun   onStopTencentLocation(){

//        TencentLocationManager.getInstance(this).removeUpdates(null)
    }

    private   fun  initBleInfo(){
        if(BLE_NFC_CARD == 0){
            tv_ble_addr.visibility = View.GONE
            tv_ble_status.visibility = View.GONE
            tv_ble_elect.visibility = View.GONE
            sc_setting_ble.isChecked = false
        }else{
            tv_ble_addr.visibility = View.VISIBLE
            tv_ble_status.visibility = View.VISIBLE
            tv_ble_elect.visibility = View.VISIBLE
            sc_setting_ble.isChecked = true
        }
    }

    private fun initBatchInfo(batch: Batch) {
        batch.todayTotal = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.OperationTime.gt(BatichDbUtil.getTodayStartTime()), LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        batch.total = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        batch.uploadedTotal = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.UploadTime.gt(0), LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        batch.exportedTotal = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.ExportTime.gt(0), LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        BatichDbUtil.getBatichDao().save(batch)
        tv_bacth_name.text = batch.name
        tv_bacth_total.text = MessageFormat.format("总数：{0}  今日：{1}  已导：{2}  已传：{3}", batch.total, batch.todayTotal, batch.exportedTotal, batch.uploadedTotal)

    }


    private fun onStartLocation() {
        val tagStr = btn_lat_lng.tag.toString()

        if (tagStr == "1") {
            mDisposable?.dispose()
            btn_lat_lng.tag = "0"
            btn_lat_lng.text = "开始定位"
            cacheLatlngs.clear()
            averageLatlng = LatLngVo()
            onStopLocation()
            showMsg("定位已取消！")
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showMsg("请允许定位权限！")
            return
        }
        cacheLatlngs.clear()
        averageLatlng = LatLngVo()
        var rxPermissions = RxPermissions(this)
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe({
                    if (it) {
                        //备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新
                        if (BuildConfig.DEBUG) {

                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0F, this)
                        } else {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0F, this)
                        }

                        onStartTencentLocation()
                        btn_lat_lng.tag = "1"
                        btn_lat_lng.text = "定位中.."
                        mDisposable?.dispose()
                        initLocationInfo()
                        onStartTime()
                    } else {
                        showMsg("请给必要的权限")
                    }
                }, {
                    it.printStackTrace()
                })

    }


    private fun initLocationInfo() {
        val stringBuilder = StringBuilder("缓存数：").append(cacheLatlngs.size)
                .append("<br>").append("平均lat：").append(averageLatlng.lat).append("   :").append(ConvertUtils.changeToDFM(averageLatlng.lat))
                .append("<br>").append("平均lng：").append(averageLatlng.lng).append("   :").append(ConvertUtils.changeToDFM(averageLatlng.lng))
                .append("<br>重复数：").append(repeatCount)

        if ( cacheLatlngs.size == MAX_LATLNG || accordSatellite>= MIN_SATELLITE) {
            if(cacheLatlngs.size == MAX_LATLNG && accordSatellite>= MIN_SATELLITE){
                tv_latlng.text = Html.fromHtml(HtmlUtils.getGreenHtml(stringBuilder.toString()))
            }else{
                tv_latlng.text = Html.fromHtml(HtmlUtils.getColorHtml(stringBuilder.toString(),"#F57F17"))

            }
        } else {
            tv_latlng.text = Html.fromHtml(stringBuilder.toString())

        }
        tv_current_latlng.text = "当前定位：无"
        currentLatlng?.apply {
            val stringBuilder1 = StringBuilder("")
            stringBuilder1.append("当前精度：").append(accuracy).append("米")
                    .append("  有效卫星数：").append(if(accordSatellite>0){accordSatellite}else{0})
                    .append("<br> lat:").append(latitude).append("   ,").append(ConvertUtils.changeToDFM(latitude))
                    .append("<br> lng:").append(longitude).append("   ,").append(ConvertUtils.changeToDFM(longitude))
            if (accuracy <= MAX_ACCURACY) {
                tv_current_latlng.text = Html.fromHtml(HtmlUtils.getGreenHtml(stringBuilder1.toString()))
            } else {
                tv_current_latlng.text = Html.fromHtml(HtmlUtils.getColorHtml(stringBuilder1.toString(), "#4d4d4d"))
            }

        }
    }

    private var mDisposable: Disposable? = null
    private var startTime: Long = 0
    private fun onStartTime() {
        startTime = System.currentTimeMillis()
        mDisposable = Observable.interval(1, TimeUnit.SECONDS)
                .compose(RxUtil.IO_Main())
                .subscribe({
                    btn_lat_lng.text = MessageFormat.format("定位中...(耗时:{0}s)", (System.currentTimeMillis() - startTime) / 1000)
                }, {
                    it.printStackTrace()
                })
    }

    private fun onStopLocation() {

        val tag = btn_lat_lng.tag
        if (tag == "1") {
            //手动取消定位
            averageLatlng = LatLngVo()
            cacheLatlngs.clear()
            btn_lat_lng.text = "开始定位"
            btn_lat_lng.tag = "0"
            btn_lat_lng.visibility = View.VISIBLE
            btn_delect_latlng.visibility = View.GONE
        }
        if (tag == "2") {
            btn_lat_lng.visibility = View.GONE
            btn_delect_latlng.visibility = View.VISIBLE
        }
        initLocationInfo()
        mDisposable?.dispose()
        onStopTencentLocation()
        locationManager.removeUpdates(this)
    }

    override fun handTag(rfid: String, block0: String?) {
        val tag = btn_lat_lng.tag.toString()
        if (tag == "2") {
            if (BatichDbUtil.onValRfid(rfid)) {
                showMsg("当前标签信息已被使用！")
                return
            }
            tv_rfid.text = MessageFormat.format("标定ID：{0}", rfid)
            if (null == latLngRfid || null == latLngRfid?.id) {
                latLngRfid = LatLngRfid().apply {
                    wgsLat = averageLatlng.lat
                    wgsLng = averageLatlng.lng
                    operationTime = System.currentTimeMillis()
                    this.rfid = rfid
                }
                latLngRfid?.batchId = batchId
                BatichDbUtil.getLatlngRfidDao().save(latLngRfid)
                initBatchInfo(batch!!)
                showMsg("保存成功！")
            } else {
                DialogUtil.showConfigDialog(this, "确定要替换当前标签？") {
                    if (it) {
                        latLngRfid?.apply {
                            this.rfid = rfid
                            operationTime = currentTimeMillis
                            uploadTime = 0
                            exportTime = 0
                            BatichDbUtil.getLatlngRfidDao().save(this)
                            showMsg("更换电子身分成功")
                        }


                    }
                }
            }

        }


    }

    /**
     * 方法描述：转到手机设置界面，用户设置GPS
     */
    fun openGPS() {
        Toast.makeText(this, "请打开GPS设置", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            btn_lat_lng.isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    override fun onLocationChanged(location: Location?) {

        location?.let {
            if (it.accuracy <= MAX_ACCURACY && accordSatellite>= MIN_SATELLITE) {
                cacheLatlngs.add(LatLngVo(it.latitude, it.longitude))
                var lat = 0.0
                var lng = 0.0


                if (cacheLatlngs.size >= MAX_LATLNG) {
                    btn_lat_lng.tag = "2"
                    cacheLatlngs.sortBy {
                         latlngVo ->
                        latlngVo.lng *latlngVo.lat
                    }
                    val  removeLen = (MAX_LATLNG*0.05).toInt()
                    if(removeLen>0){
                        //去除前面第X个
                        println("size=${cacheLatlngs.size}  $removeLen")
                       cacheLatlngs.drop(removeLen)
                        var  index = 0
                       cacheLatlngs.dropLastWhile {
                           index++
                           index<= removeLen
                       }
                        println("size2=${cacheLatlngs.size}  $removeLen")
                    }
                    onStopLocation()

                }
                cacheLatlngs.forEach { latlng ->
                    lat += latlng.lat
                    lng += latlng.lng
                }
                averageLatlng.lat = lat / cacheLatlngs.size
                averageLatlng.lng = lng / cacheLatlngs.size
                currentLatlng?.let { latlng ->
                    if (it.latitude == latlng.latitude && it.longitude == latlng.longitude) {
                        repeatCount++
                        if (repeatCount >= MAX_REPEAT_LATLNG) {
                            btn_lat_lng.tag = "2"
                            averageLatlng = LatLngVo(latlng.latitude, latlng.longitude)
                            cacheLatlngs.clear()
                            for (index in 1..MAX_LATLNG) {
                                cacheLatlngs.add(LatLngVo(latlng.latitude, latlng.longitude))
                            }
                            onStopLocation()
                        }
                    } else {
                        repeatCount = 0
                    }
                }
            }

            currentLatlng = it
            println("it.=${it.toString()}")
            initLocationInfo()
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        println("onStatusChanged")
    }

    override fun onProviderEnabled(provider: String?) {
        println("onProviderEnabled")
    }

    override fun onProviderDisabled(provider: String?) {
        println("onProviderDisabled")
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_rfid_latlng, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_setting -> {
                showSettingDialog()
            }
            R.id.action_export -> {
                MaterialDialog.Builder(this).title("数据导出")
                        .content("1、导出(默认)未导出过的数据" +
                                "2、导出所有数据")
                        .neutralText("取消")
                        .negativeText("导出")
                        .positiveText("导出所有")
                        .autoDismiss(true)
                        .onPositive { _, _ ->
                            onExportData(type = 1)
                        }
                        .onNegative { _, _ ->
                            onExportData()
                        }
                        .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onExportData( type : Int = 0) {
        var data = mutableListOf<LatLngRfid>()
        if(type == 0){
          data =  BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.ExportTime.le(0), LatLngRfidDao.Properties.BatchId.eq(batchId)).list()
        }else {
            data =  BatichDbUtil.getLatlngRfidDao().queryBuilder().where( LatLngRfidDao.Properties.BatchId.eq(batchId)).list()

        }
        if (data.isEmpty()) {
            showMsg("当前批次没有可以导出的数据")
            return
        }
        GlobalScope.launch(Main) {
            val dialog = MaterialDialog.Builder(this@LatlngRfidEditActivity).autoDismiss(true)
                    .progress(false, 0)
                    .stackingBehavior(StackingBehavior.ADAPTIVE)
                    .show()
            ExcelUtil.onExportExcel(data, batch!!) { result, error ->
                if (result) {
                    showMsg("导出成功")
                } else {
                    showMsg("导出失败,$error")
                }
            }
            dialog.hide()

        }

    }


    private fun showSettingDialog() {
        val view = layoutInflater.inflate(R.layout.custiom_dialog_setting, null)
        view.findViewById<AppCompatEditText>(R.id.edit_max_accuracy).setText(MAX_ACCURACY.toString())
        view.findViewById<AppCompatEditText>(R.id.edit_max_repeat).setText(MAX_REPEAT_LATLNG.toString())
        view.findViewById<AppCompatEditText>(R.id.edit_max_loc).setText(MAX_LATLNG.toString())
        view.findViewById<AppCompatEditText>(R.id.edit_min_satellite).setText(MIN_SATELLITE.toString())
        view.findViewById<AppCompatEditText>(R.id.edit_min_snv).setText(MIN_SNR.toString())
        MaterialDialog.Builder(this).title("定位参数设置")
                .customView(view, true)
                .positiveText("确定")
                .negativeText("取消")
                .autoDismiss(true)
                .onPositive { dialog, which ->
                    val editLoc = dialog.findViewById(R.id.edit_max_loc) as AppCompatEditText
                    if (editLoc.text.toString().trim().isNotEmpty()) {
                        MAX_LATLNG = editLoc.text.toString().trim().toInt()
                        spSetInt("max_latlng", MAX_LATLNG)
                    }
                    val repeat = dialog.findViewById(R.id.edit_max_repeat) as AppCompatEditText
                    if (repeat.text.toString().trim().isNotEmpty()) {
                        MAX_REPEAT_LATLNG = repeat.text.toString().trim().toInt()
                        spSetInt("max_repeat_latlng", MAX_REPEAT_LATLNG)
                    }

                    val accuracy = dialog.findViewById(R.id.edit_max_accuracy) as AppCompatEditText
                    if (accuracy.text.toString().trim().isNotEmpty()) {
                        MAX_ACCURACY = accuracy.text.toString().trim().toFloat()
                        spSetFloat("max_accuracy", MAX_ACCURACY)
                    }

                    val snv = dialog.findViewById(R.id.edit_min_snv) as AppCompatEditText
                    if(snv.text.toString().trim().isNotEmpty()){
                        MIN_SNR = snv.text.toString().trim().toFloat()
                        spSetFloat("min_snr",MIN_SNR)
                    }

                    val satellite = dialog.findViewById(R.id.edit_min_satellite) as AppCompatEditText
                    if(satellite.text.toString().trim().isNotEmpty()){
                        MIN_SATELLITE = satellite.text.toString().trim().toInt()
                        spSetInt("min_satellite",MIN_SATELLITE)
                    }
                }.show()
    }


    override fun handleConnectStatus(conSttus: ConStatus) {
        var bledrable = when (conSttus) {
            ConStatus.CONNECTED -> {
                tv_ble_status.tag = "5"
                resources.getDrawable(R.drawable.ic_connected)

            }
            ConStatus.CONNECTFAIL -> {
                tv_ble_status.tag = "4"
                resources.getDrawable(R.drawable.ic_connectfail)
            }
            ConStatus.CONNECTING -> {
                tv_ble_status.tag = "3"
                resources.getDrawable(R.drawable.ic_connectfail)
            }
            ConStatus.NONE -> {
                tv_ble_status.tag = "1"
                resources.getDrawable(R.drawable.ic_none)
            }

            ConStatus.UNCONNECT -> {
                tv_ble_status.tag = "2"
                resources.getDrawable(R.drawable.ic_connectfail)
            }
            else -> {
                resources.getDrawable(R.drawable.ic_none)
            }
        }

        if (null != bledrable) {
            bledrable.setBounds(0, 0, bledrable.minimumWidth, bledrable.minimumHeight)
            tv_ble_status.setCompoundDrawables(bledrable, null, null, null)
            tv_ble_status.text = conSttus.getName()
        }
    }

    override fun handleElectricity(elect: Int) {
        tv_ble_elect.text=MessageFormat.format("电量：{0}%",elect)
    }

    override fun onGpsStatusChanged(event: Int) {
       when(event){
           GpsStatus.GPS_EVENT_FIRST_FIX ->{
               println("第一次定位")
           }

           GpsStatus.GPS_EVENT_SATELLITE_STATUS ->{
              println("卫星状态发生改变")
               //实例化卫星状态
               locationManager.getGpsStatus(null)?.apply {
                    //获取到卫星
                   currentSatellite = satellites.count()
                   accordSatellite = 0
                   satellites?.forEach {
                       println("snr=${it.snr}")
                        if(it.snr>= MIN_SNR){
                            accordSatellite++
                        }
                   }
                   if(accordSatellite>= MIN_SATELLITE){
                       tv_current_satellite.text = HtmlCompat.fromHtml(HtmlUtils.getGreenHtml("可见卫星数：$currentSatellite    信噪比>$MIN_SNR  卫星数：$accordSatellite"), HtmlCompat.FROM_HTML_MODE_LEGACY)

                   }else{
                       tv_current_satellite.text = HtmlCompat.fromHtml(HtmlUtils.getRedHtml("可见卫星数：$currentSatellite    信噪比>$MIN_SNR  卫星数：$accordSatellite"), HtmlCompat.FROM_HTML_MODE_LEGACY)

                   }
               }
           }

           GpsStatus.GPS_EVENT_STARTED->{
               println("启动定位")
           }

           GpsStatus.GPS_EVENT_STOPPED->{
               println("结束定位")
           }
       }
    }

    override fun onDestroy() {
         onStopLocation()
         locationManager.removeGpsStatusListener(this)
        super.onDestroy()
    }
}