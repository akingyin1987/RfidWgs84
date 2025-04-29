package com.akingyin.rfidwgs.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.GpsStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.StackingBehavior
import com.akingyin.rfidwgs.R
import com.akingyin.rfidwgs.databinding.ActivityRfidLatlngEditBinding
import com.akingyin.rfidwgs.db.Batch
import com.akingyin.rfidwgs.db.BleDeviceRepairInfo
import com.akingyin.rfidwgs.db.LatLngRfid
import com.akingyin.rfidwgs.db.dao.BatichDbUtil
import com.akingyin.rfidwgs.db.dao.LatLngRfidDao
import com.akingyin.rfidwgs.db.vo.LatLngVo
import com.akingyin.rfidwgs.ext.currentTimeMillis
import com.akingyin.rfidwgs.ext.spGetFloat
import com.akingyin.rfidwgs.ext.spGetInt
import com.akingyin.rfidwgs.ext.spSetFloat
import com.akingyin.rfidwgs.ext.spSetInt
import com.akingyin.rfidwgs.util.DialogUtil
import com.akingyin.rfidwgs.util.ExcelUtil
import com.akingyin.rfidwgs.util.HtmlUtils
import com.akingyin.rfidwgs.util.RxUtil
import com.bleqpp.BleQppNfcCameraServer
import com.bleqpp.KsiSharedStorageHelper
import com.bleqpp.QppBleDeviceListActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zlcdgroup.nfcsdk.ConStatus
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.MessageFormat
import java.util.concurrent.TimeUnit

/**
 * 获取定位与标签信息
 * @ Description:
 * @author king
 * @ Date 2019/12/6 16:01
 * @version V1.0
 */

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
class LatlngRfidEditActivity : BaseActivity(), LocationListener, GpsStatus.Listener {

    var MAX_LATLNG = 50
    var MAX_REPEAT_LATLNG = 15
    var MAX_ACCURACY = 10F
    var MIN_SATELLITE = 4
    //卫星的信噪比
    var MIN_SNR = 21F


    var batchId: Long = 0L
    //当前获取卫星数据
    var currentSatellite = -1

    //通过设置信噪比符合要求的卫星数
    var accordSatellite = 0



    var batch: Batch? = null

    var latLngRfid: LatLngRfid? = null

    private lateinit var locationManager: LocationManager

    override fun getLayoutId() = R.layout.activity_rfid_latlng_edit


    var cacheLatlngs = mutableListOf<LatLngVo>()

    var averageLatlng = LatLngVo()

    var repeatCount: Int = 0

    var currentLatlng: Location? = null
    
    private lateinit var viewBinding: ActivityRfidLatlngEditBinding


    private var gnssStatusCallback: GnssStatus.Callback? = null


    override val useViewBind: Boolean
        get() = true

    override fun initViewBind() {
        super.initViewBind()
         viewBinding = ActivityRfidLatlngEditBinding.inflate(layoutInflater)
         setContentView(viewBinding.root)
    }
    
    override fun initView() {

        viewBinding.tvToolbarTitle.text="定位与标签"
        setToolBar(viewBinding.toolbar, "")


        MAX_LATLNG = spGetInt("max_latlng", MAX_LATLNG)
        MAX_REPEAT_LATLNG = spGetInt("max_repeat_latlng", MAX_REPEAT_LATLNG)
        MAX_ACCURACY = spGetFloat("max_accuracy", MAX_ACCURACY)
        MIN_SATELLITE = spGetInt("min_satellite",MIN_SATELLITE)
        MIN_SNR= spGetFloat("min_snr",MIN_SNR)
        batchId = intent.getLongExtra("batchId", 0)

        batch = BatichDbUtil.getBatichDao().load(batchId)
        if (null == batch) {
            showMsg("数据出错了")
            finish()
            return
        }

       // KsiSharedStorageHelper.setBluetoothMac(KsiSharedStorageHelper.getPreferences(this),"123")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED){
                val mac = KsiSharedStorageHelper.getBluetoothMac(KsiSharedStorageHelper.getPreferences(this@LatlngRfidEditActivity))
                if(mac.isNotEmpty()){
                    withContext(IO){
                        BatichDbUtil.getBleDeviceRepairInfo(batchId,mac)
                    }?.let {state->
                        when(state.repairStatus){
                            0->viewBinding.rbUnRepair.isChecked=true
                            1->viewBinding.rbRepairIng.isChecked=true
                            2->viewBinding.rbDamage.isChecked=true
                            3->viewBinding.rbRepairCompleted.isChecked=true
                        }
                    }

                }
                BLE_NFC_CARD = spGetInt("ble_cardread")
                viewBinding.tvBleAddr.text=MessageFormat.format("读卡器地址：{0}",mac)
                if(mac.isNotEmpty() && BLE_NFC_CARD == 1){
                    viewBinding.rgBleDevice.visibility = View.VISIBLE
                }else{
                    viewBinding.rgBleDevice.visibility = View.GONE
                }


            }
        }
        viewBinding.rbUnRepair.setOnClickListener {
            saveRepairStatus(0)
        }
        viewBinding.rbRepairIng.setOnClickListener {
            saveRepairStatus(1)
        }
        viewBinding.rbDamage.setOnClickListener {
            saveRepairStatus(2)
        }
        viewBinding.rbRepairCompleted.setOnClickListener {
            saveRepairStatus(3)
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            openGPS()
            viewBinding.btnLatLng.isEnabled = false
        }
        initGpsStatus()

        viewBinding.btnLatLng.setOnClickListener {
            onStartLocation()
        }
        viewBinding.btnDelectRfid.setOnClickListener {
            latLngRfid?.let {
                if(it.rfid.isNotEmpty() &&  it.wgsLat >0 ){
                    DialogUtil.showConfigDialog(this,"当前定位点信息已完整，确定要修改？"){result->
                        if(result){
                            cleanLatLng()
                        }
                    }
                }
            }?:cleanLatLng()


        }
        viewBinding.tvBleStatus.setOnClickListener {
           try {
               when(it.tag.toString().toInt()){
                   1->{
                       startActivity(Intent(this@LatlngRfidEditActivity,QppBleDeviceListActivity::class.java).apply{
                           putExtra("batchId",batchId)
                       })
                   }
                   3->{
                       startActivity(Intent(this@LatlngRfidEditActivity,QppBleDeviceListActivity::class.java).apply{
                           putExtra("batchId",batchId)
                       })
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
        viewBinding.tvBleAddr.text=MessageFormat.format("读卡器地址：{0}",mac)
        BLE_NFC_CARD = spGetInt("ble_cardread")

        initBleInfo()
        viewBinding.scSettingBle.setOnClickListener {
            val  isChecked = viewBinding.scSettingBle.isChecked
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

        viewBinding.llBleAddr.setOnClickListener {
            startActivity(Intent(this,QppBleDeviceListActivity::class.java).apply {
                putExtra("batchId",batchId)
            })
        }
        viewBinding.tvBleAddr.setOnClickListener {
            startActivity(Intent(this,QppBleDeviceListActivity::class.java).apply {
                putExtra("batchId",batchId)
            })
        }
        initBatchInfo(batch!!)

    }


    private fun saveRepairStatus(status:Int){
        val mac = KsiSharedStorageHelper.getBluetoothMac(KsiSharedStorageHelper.getPreferences(this))
        lifecycleScope.launch {
            withContext(IO){
               val repairInfo =  BatichDbUtil.getBleDeviceRepairInfo(batchId,mac)?:BleDeviceRepairInfo().apply {
                    batchId = batch?.id?:0
                    bleDeviceAddress = mac
                    createTime = System.currentTimeMillis()
                    updateTime = createTime
                }
                repairInfo.repairStatus = status
                if(null != repairInfo.id && repairInfo.id>0){
                    repairInfo.updateTime = System.currentTimeMillis()
                    BatichDbUtil.getBleDeviceRepairInfoDao().update(repairInfo)
                }else{
                    BatichDbUtil.getBleDeviceRepairInfoDao().insert(repairInfo)
                }


            }
        }
    }

    private   fun   cleanLatLng(){
        cacheLatlngs.clear()
        averageLatlng = LatLngVo()
        repeatCount = 0
        currentLatlng = null
        viewBinding.btnLatLng.tag = "0"
        viewBinding.btnLatLng.text = "开始定位"
        onStopLocation()
        viewBinding.btnDelectLatlng.visibility = View.GONE
        viewBinding.btnLatLng.visibility = View.VISIBLE
        initLocationInfo()

    }

    @SuppressLint("MissingPermission")
    private   fun   initGpsStatus(){
        var rxPermissions = RxPermissions(this)
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe {
                    if(it){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            gnssStatusCallback = object :GnssStatus.Callback(){
                                override fun onSatelliteStatusChanged(status: GnssStatus) {
                                    super.onSatelliteStatusChanged(status)
                                    //实例化卫星状态
                                    //获取到卫星
                                    currentSatellite = status.satelliteCount
                                    accordSatellite = 0
                                    for (i in 0 until status.satelliteCount) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            if(status.getBasebandCn0DbHz(i)> MIN_SNR){
                                                accordSatellite++
                                            }
                                        }
                                    }
//                                    status.satellites?.forEach {
//
//                                        if(it.snr>= MIN_SNR){
//                                            accordSatellite++
//                                        }
//                                    }
                                    if(accordSatellite>= MIN_SATELLITE){
                                        viewBinding.tvCurrentSatellite.text = HtmlCompat.fromHtml(HtmlUtils.getGreenHtml("可见卫星数：$currentSatellite    信噪比>$MIN_SNR  卫星数：$accordSatellite"), HtmlCompat.FROM_HTML_MODE_LEGACY)

                                    }else{
                                        viewBinding.tvCurrentSatellite.text = HtmlCompat.fromHtml(HtmlUtils.getRedHtml("可见卫星数：$currentSatellite    信噪比>$MIN_SNR  卫星数：$accordSatellite"), HtmlCompat.FROM_HTML_MODE_LEGACY)

                                    }
                                }

                                override fun onFirstFix(ttffMillis: Int) {
                                    super.onFirstFix(ttffMillis)
                                }

                                override fun onStarted() {
                                    super.onStarted()
                                }

                                override fun onStopped() {
                                    super.onStopped()
                                }
                            }

                            locationManager.registerGnssStatusCallback(gnssStatusCallback!!,null)
                        }else{
                            locationManager.addGpsStatusListener(this)
                        }



                    }else{
                        showMsg("请允许必要的条件")
                    }
                }

    }

   // private   var  tencentLocationListener :TencentLocationListener?= null

    private   fun  onStartTencentLocation(){
//         if(null == tencentLocationListener){
//             tencentLocationListener = object :TencentLocationListener{
//                 override fun onStatusUpdate(name: String?, status: Int, desc: String?) {
//
//                 }
//
//                 override fun onLocationChanged(location: TencentLocation, error: Int, reason: String) {
//                     println("error=$error  :$reason   ${location.coordinateType}")
//                   if(error == TencentLocation.ERROR_OK){
//
//                       onLocationChanged(Location(LocationManager.GPS_PROVIDER).apply {
//                           latitude = location.latitude
//                           longitude = location.longitude
//                           accuracy = location.accuracy
//                       })
//                   }
//                 }
//             }
//         }
//        val request = TencentLocationRequest.create().apply {
//            interval = 2000
//            isAllowGPS = true
//        }
//        val mLocationManager = TencentLocationManager.getInstance(this)
//        if(mLocationManager.coordinateType == TencentLocationManager.COORDINATE_TYPE_WGS84){
//            mLocationManager.coordinateType = TencentLocationManager.COORDINATE_TYPE_GCJ02
//        }else{
//            mLocationManager.coordinateType = TencentLocationManager.COORDINATE_TYPE_WGS84
//        }
//
//       val error =  mLocationManager.requestLocationUpdates(request,tencentLocationListener)
//       if(error != 0){
//           showMsg("注册定位失败，代码：$error")
//       }
    }

    private   fun   onStopTencentLocation(){

     // TencentLocationManager.getInstance(this).removeUpdates(tencentLocationListener)
    }

    private   fun  initBleInfo(){
        if(BLE_NFC_CARD == 0){
            viewBinding.ivBleAddr.visibility = View.GONE
            viewBinding.tvBleAddr.visibility = View.GONE

            viewBinding.tvBleStatus.visibility = View.GONE
            viewBinding.tvBleElect.visibility = View.GONE
            viewBinding.scSettingBle.isChecked = false
        }else{

            viewBinding.ivBleAddr.visibility = View.VISIBLE
            viewBinding.tvBleAddr.visibility = View.VISIBLE
            viewBinding.tvBleStatus.visibility = View.VISIBLE
            viewBinding.tvBleElect.visibility = View.VISIBLE
            viewBinding.scSettingBle.isChecked = true
            handleConnectStatus( BleQppNfcCameraServer.getInstance(this).current)
        }
    }

    private fun initBatchInfo(batch: Batch) {
        batch.todayTotal = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.OperationTime.gt(BatichDbUtil.getTodayStartTime()), LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        batch.total = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        batch.uploadedTotal = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.UploadTime.gt(0), LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        batch.exportedTotal = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.ExportTime.gt(0), LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        BatichDbUtil.getBatichDao().save(batch)
        viewBinding.tvBacthName.text = batch.name
        viewBinding.tvBacthTotal.text = MessageFormat.format("总数：{0}  今日：{1}  已导：{2}  已传：{3}", batch.total, batch.todayTotal, batch.exportedTotal, batch.uploadedTotal)

    }


    private fun onStartLocation() {
        val tagStr = viewBinding.btnLatLng.tag.toString()

        if (tagStr == "1") {
            mDisposable?.dispose()
            viewBinding.btnLatLng.tag = "0"
            viewBinding.btnLatLng.text = "开始定位"
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
        repeatCount =0
        cacheLatlngs.clear()
        averageLatlng = LatLngVo()
        var rxPermissions = RxPermissions(this)
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe({
                    if (it) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0F, this)
                        //备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新
//                        if (BuildConfig.DEBUG) {
//
//                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0F, this)
//                        } else {
//
//                        }

                      //  onStartTencentLocation()
                        viewBinding.btnLatLng.tag = "1"
                        viewBinding.btnLatLng.text = "定位中.."
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
                .append("<br>").append("平均lat：").append(averageLatlng.lat)
                .append("<br>").append("平均lng：").append(averageLatlng.lng)
                .append("<br>重复数：").append(repeatCount)

        if ( cacheLatlngs.size == MAX_LATLNG || accordSatellite>= MIN_SATELLITE) {
            if(cacheLatlngs.size == MAX_LATLNG && accordSatellite>= MIN_SATELLITE){
                viewBinding.tvLatlng.text = Html.fromHtml(HtmlUtils.getGreenHtml(stringBuilder.toString()))
            }else{
                viewBinding.tvLatlng.text = Html.fromHtml(HtmlUtils.getColorHtml(stringBuilder.toString(),"#F57F17"))

            }
        } else {
            viewBinding.tvLatlng.text = Html.fromHtml(stringBuilder.toString())

        }
        viewBinding.tvCurrentLatlng.text = "当前定位：无"
        currentLatlng?.apply {
            val stringBuilder1 = StringBuilder("")
            stringBuilder1.append("当前精度：").append(accuracy).append(" 米")

                    .append("<br> lat:").append(latitude).append("   ")
                    .append("<br> lng:").append(longitude).append("  ")
            if (accuracy <= MAX_ACCURACY) {

                viewBinding.tvCurrentLatlng.text = HtmlCompat.fromHtml(HtmlUtils.getGreenHtml(stringBuilder1.toString()),HtmlCompat.FROM_HTML_MODE_LEGACY)
            } else {
                viewBinding.tvCurrentLatlng.text = HtmlCompat.fromHtml(HtmlUtils.getColorHtml(stringBuilder1.toString(), "#4d4d4d"),HtmlCompat.FROM_HTML_MODE_LEGACY)
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
                    viewBinding.btnLatLng.text = MessageFormat.format("定位中...(耗时:{0}s)", (System.currentTimeMillis() - startTime) / 1000)
                }, {
                    it.printStackTrace()
                })
    }

    private fun onStopLocation() {

        val tag = viewBinding.btnLatLng.tag
        if (tag == "1") {
            //手动取消定位
            averageLatlng = LatLngVo()
            cacheLatlngs.clear()
            viewBinding.btnLatLng.text = "开始定位"
            viewBinding.btnLatLng.tag = "0"
            viewBinding.btnLatLng.visibility = View.VISIBLE
            viewBinding.btnDelectLatlng.visibility = View.GONE
        }
        if (tag == "2") {
            viewBinding.btnLatLng.visibility = View.VISIBLE
            viewBinding.btnLatLng.text="定位完成,点击重新定位"
            viewBinding.btnDelectLatlng.visibility = View.GONE
        }
        initLocationInfo()
        mDisposable?.dispose()
       // onStopTencentLocation()
        locationManager.removeUpdates(this)
    }

    private   var  dialog:MaterialDialog?=null
    private   var    readRfidCount = 0
    override fun handTag(rfid: String, block0: String?) {
        showMsg("标签：$rfid  区块：$block0")
        if(null != dialog && dialog!!.isShowing){
            return
        }

        val tag = viewBinding.btnLatLng.tag.toString()
        if (tag == "2") {
            if (BatichDbUtil.onValRfid(rfid)) {
                showMsg("当前标签信息已被使用！")
                return
            }
            viewBinding.tvRfid.text = MessageFormat.format("标签ID：{0}", rfid)
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
                dialog =  DialogUtil.showConfigDialog(this,"定位点数据保存成功，点击确定新增下一个点?"){
                    if(it){
                        latLngRfid = LatLngRfid().apply {
                            batchId = batch!!.id
                        }
                        averageLatlng = LatLngVo()
                        cacheLatlngs.clear()
                        viewBinding.tvRfid.text=""
                        cleanLatLng()
                    }
                }
            } else {
             dialog =   DialogUtil.showConfigDialog(this, "确定要替换当前标签？") {
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

        }else{
           // showMsg("当前正在定位或定位信息不完整！")
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
            viewBinding.btnLatLng.isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }



    override fun onLocationChanged(location: Location) {
        location.let {

            if (it.accuracy <=MAX_ACCURACY && it.accuracy>0F && accordSatellite>= MIN_SATELLITE) {
                cacheLatlngs.add(LatLngVo(it.latitude, it.longitude))
                var lat = 0.0
                var lng = 0.0
                if (cacheLatlngs.size >= MAX_LATLNG) {
                    viewBinding.btnLatLng.tag = "2"
                    cacheLatlngs.sortBy { latlngVo ->
                        latlngVo.lng *latlngVo.lat
                    }
                    val  removeLen = (MAX_LATLNG*0.05).toInt()

                    if(removeLen>0){
                        //去除前面第X个
                        cacheLatlngs.drop(removeLen)
                        var  index = 0
                        cacheLatlngs.dropLastWhile {
                            index++
                            index<= removeLen
                        }

                    }
                    onStopLocation()
                    cacheLatlngs.forEach { latlng ->
                        lat += latlng.lat
                        lng += latlng.lng
                    }
                    averageLatlng.lat = lat / cacheLatlngs.size
                    averageLatlng.lng = lng / cacheLatlngs.size

                    averageLatlng.lat = BigDecimal.valueOf(averageLatlng.lat).setScale(6,BigDecimal.ROUND_HALF_UP).toDouble()
                    averageLatlng.lng = BigDecimal.valueOf(averageLatlng.lng).setScale(6,BigDecimal.ROUND_HALF_UP).toDouble()
                    latLngRfid?.let { latlng->
                        if(!latlng.rfid.isNullOrEmpty()){
                            latlng.wgsLat = averageLatlng.lat
                            latlng.wgsLng = averageLatlng.lng
                            latlng.uploadTime=0
                            latlng.exportTime=0
                            latlng.operationTime = currentTimeMillis
                            BatichDbUtil.getLatlngRfidDao().save(latlng)
                            initBatchInfo(batch!!)
                            showMsg("修改定位点位置信息成功!")
                        }else{
                            showMsg("定位成功，请扫描标签！")
                        }
                    }?:showMsg("定位成功，请扫描标签！")
                }else{
                    cacheLatlngs.forEach { latlng ->
                        lat += latlng.lat
                        lng += latlng.lng
                    }
                    averageLatlng.lat = lat / cacheLatlngs.size
                    averageLatlng.lng = lng / cacheLatlngs.size

                    averageLatlng.lat = BigDecimal.valueOf(averageLatlng.lat).setScale(6,BigDecimal.ROUND_HALF_UP).toDouble()
                    averageLatlng.lng = BigDecimal.valueOf(averageLatlng.lng).setScale(6,BigDecimal.ROUND_HALF_UP).toDouble()

                }



                currentLatlng?.let { latlng ->
                    if (it.latitude == latlng.latitude && it.longitude == latlng.longitude) {
                        repeatCount++
                        if (repeatCount >= MAX_REPEAT_LATLNG) {
                            viewBinding.btnLatLng.tag = "2"
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
            println("it.=$it")
            initLocationInfo()
        }
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
                                "\r\n 2、导出所有数据")
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
        val data: MutableList<LatLngRfid> = if(type == 0){
            BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.ExportTime.le(0), LatLngRfidDao.Properties.BatchId.eq(batchId)).list()
        }else {
            BatichDbUtil.getLatlngRfidDao().queryBuilder().where( LatLngRfidDao.Properties.BatchId.eq(batchId)).list()

        }
        if (data.isEmpty()) {
            showMsg("当前批次没有可以导出的数据")
            return
        }

        lifecycleScope.launch(Main) {
            val dialog = MaterialDialog.Builder(this@LatlngRfidEditActivity).autoDismiss(true)
                    .progress(false, 0)
                    .stackingBehavior(StackingBehavior.ADAPTIVE)
                    .show()
            withContext(IO){
                ExcelUtil.onExportExcel(data, batch!!) { result, error ->
                    if (result) {
                        showMsg("导出成功,路径：${error}")
                    } else {
                        showMsg("导出失败,$error")
                    }
                }
            }

            dialog.hide()

        }

    }


    private fun showSettingDialog() {
        val view = layoutInflater.inflate(R.layout.custiom_dialog_setting, null)
        view.findViewById<AppCompatEditText>(R.id.edit_max_accuracy).setText("$MAX_ACCURACY")
        view.findViewById<AppCompatEditText>(R.id.edit_max_repeat).setText("$MAX_REPEAT_LATLNG")
        view.findViewById<AppCompatEditText>(R.id.edit_max_loc).setText("$MAX_LATLNG")
        view.findViewById<AppCompatEditText>(R.id.edit_min_satellite).setText("$MIN_SATELLITE")
        view.findViewById<AppCompatEditText>(R.id.edit_min_snv).setText("$MIN_SNR")
        MaterialDialog.Builder(this).title("定位参数设置")
                .customView(view, true)
                .positiveText("确定")
                .negativeText("取消")
                .autoDismiss(true)
                .onPositive { dialog, _ ->
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
        val bledrable = when (conSttus) {
            ConStatus.CONNECTED -> {
                viewBinding.tvBleStatus.tag = "5"
                resources.getDrawable(R.drawable.ic_connected)

            }
            ConStatus.CONNECTFAIL -> {
                viewBinding.tvBleStatus.tag = "4"
                resources.getDrawable(R.drawable.ic_connectfail)
            }
            ConStatus.CONNECTING -> {
                viewBinding.tvBleStatus.tag = "3"
                resources.getDrawable(R.drawable.ic_connectfail)
            }
            ConStatus.NONE -> {
                viewBinding.tvBleStatus.tag = "1"
                resources.getDrawable(R.drawable.ic_none)
            }

            ConStatus.UNCONNECT -> {
                viewBinding.tvBleStatus.tag = "2"
                resources.getDrawable(R.drawable.ic_connectfail)
            }
            else -> {
                resources.getDrawable(R.drawable.ic_none)
            }
        }

        if (null != bledrable) {
            bledrable.setBounds(0, 0, bledrable.minimumWidth, bledrable.minimumHeight)
            viewBinding.tvBleStatus.setCompoundDrawables(bledrable, null, null, null)
            viewBinding.tvBleStatus.text = conSttus.getName()
        }
    }

    override fun handleElectricity(elect: Int) {
        viewBinding.tvBleElect.text=MessageFormat.format("电量：{0}%",elect)
    }

    override fun onGpsStatusChanged(event: Int) {
       when(event){
           GpsStatus.GPS_EVENT_FIRST_FIX ->{
               println("第一次定位")
           }

           GpsStatus.GPS_EVENT_SATELLITE_STATUS ->{

               //实例化卫星状态
               locationManager.getGpsStatus(null)?.apply {
                    //获取到卫星
                   currentSatellite = satellites.count()
                   accordSatellite = 0
                   satellites?.forEach {

                        if(it.snr>= MIN_SNR){
                            accordSatellite++
                        }
                   }
                   if(accordSatellite>= MIN_SATELLITE){
                       viewBinding.tvCurrentSatellite.text = HtmlCompat.fromHtml(HtmlUtils.getGreenHtml("可见卫星数：$currentSatellite    信噪比>$MIN_SNR  卫星数：$accordSatellite"), HtmlCompat.FROM_HTML_MODE_LEGACY)

                   }else{
                       viewBinding.tvCurrentSatellite.text = HtmlCompat.fromHtml(HtmlUtils.getRedHtml("可见卫星数：$currentSatellite    信噪比>$MIN_SNR  卫星数：$accordSatellite"), HtmlCompat.FROM_HTML_MODE_LEGACY)

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
        super.onDestroy()
        onStopLocation()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            if(null != gnssStatusCallback){
                locationManager.unregisterGnssStatusCallback(gnssStatusCallback!!)
            }
        }else{
            locationManager.removeGpsStatusListener(this)
        }
    }
}