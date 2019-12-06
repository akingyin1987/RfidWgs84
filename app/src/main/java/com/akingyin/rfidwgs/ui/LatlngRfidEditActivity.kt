package com.akingyin.rfidwgs.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.afollestad.materialdialogs.MaterialDialog
import com.akingyin.rfidwgs.BuildConfig
import com.akingyin.rfidwgs.R
import com.akingyin.rfidwgs.db.Batch
import com.akingyin.rfidwgs.db.LatLngRfid
import com.akingyin.rfidwgs.db.dao.BatichDbUtil
import com.akingyin.rfidwgs.db.dao.LatLngRfidDao
import com.akingyin.rfidwgs.db.vo.LatLngVo
import com.akingyin.rfidwgs.ext.*
import com.akingyin.rfidwgs.util.DialogUtil
import com.akingyin.rfidwgs.util.HtmlUtils
import com.akingyin.rfidwgs.util.RxUtil
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_rfid_latlng_edit.*
import java.text.MessageFormat
import java.util.concurrent.TimeUnit

/**
 * 获取定位与标签信息
 * @ Description:
 * @author king
 * @ Date 2019/12/6 16:01
 * @version V1.0
 */
class LatlngRfidEditActivity :BaseActivity(), LocationListener {

    var  MAX_LATLNG = 30
    var  MAX_REPEAT_LATLNG = 15
    var  MAX_ACCURACY = 5F

    var   batchId : Long = 0L

    var   batch :Batch?= null

    var   latLngRfid:LatLngRfid? = null

    lateinit var  locationManager:LocationManager

    override fun getLayoutId() = R.layout.activity_rfid_latlng_edit


    var   cacheLatlngs = mutableListOf<LatLngVo>()

    var   averageLatlng = LatLngVo()

    var   repeatCount : Int = 0

    var   currentLatlng : Location?= null

    override fun initView() {
        setToolBar(toolbar,"定位与标签")
        MAX_LATLNG = spGetInt("max_latlng",30)
        MAX_REPEAT_LATLNG = spGetInt("max_repeat_latlng",15)
        MAX_ACCURACY = spGetFloat("max_accuracy",5F)
        batchId = intent.getLongExtra("batchId",0)
        batch = BatichDbUtil.getBatichDao().load(batchId)
        if(null == batch){
            showMsg("数据出错了")
            finish()
            return
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            openGPS()
            btn_lat_lng.isEnabled = false
        }

        btn_lat_lng.setOnClickListener {
            onStartLocation()
        }
        btn_delect_latlng.setOnClickListener {
            cacheLatlngs.clear()
            averageLatlng = LatLngVo()
            repeatCount = 0
            currentLatlng = null
            btn_lat_lng.tag="0"
            btn_lat_lng.text="开始定位"
            btn_delect_latlng.visibility =View.VISIBLE
            btn_lat_lng.visibility = View.VISIBLE
            initLocationInfo()


        }
        initBatchInfo(batch!!)

    }


   private fun    initBatchInfo(batch: Batch){
        batch.todayTotal = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.OperationTime.gt(BatichDbUtil.getTodayStartTime()),LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        batch.total = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        batch.uploadedTotal = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.UploadTime.gt(0),LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        batch.exportedTotal = BatichDbUtil.getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.ExportTime.gt(0),LatLngRfidDao.Properties.BatchId.eq(batchId)).buildCount().count().toInt()
        BatichDbUtil.getBatichDao().save(batch)
        tv_bacth_name.text = batch.name
        tv_bacth_total.text = MessageFormat.format("总数：{0}  今日：{1}  已导：{2}  已传：{3}",batch.total,batch.todayTotal,batch.exportedTotal,batch.uploadedTotal)

    }


   private fun   onStartLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showMsg("请允许定位权限！")
            return
        }
        cacheLatlngs.clear()
        averageLatlng = LatLngVo()
        var  rxPermissions = RxPermissions(this)
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe({
                    if(it){
                        //备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新
                        if(BuildConfig.DEBUG){
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2000,0F, this)
                        }else{
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000,0F, this)
                        }
                        btn_lat_lng.tag="1"
                        btn_lat_lng.text="定位中.."
                        mDisposable?.dispose()
                        initLocationInfo()
                        onStartTime()
                    }else{
                        showMsg("请给必要的权限")
                    }
                },{
                    it.printStackTrace()
                })

    }


    private fun   initLocationInfo(){
        val  stringBuilder = StringBuilder("缓存数：").append(cacheLatlngs.size)
                .append("<br>").append("平均lat：").append(averageLatlng.lat).append("<br>")
                .append("平均lng：").append(averageLatlng.lng)
                .append("<br>重复数：").append(repeatCount)
         currentLatlng?.apply {
             stringBuilder.append("<br>").append("当前精度：").append(accuracy)
                     .append("<br> lat:").append(latitude)
                     .append("<br> lng:").append(longitude)
         }
         if(repeatCount>= MAX_REPEAT_LATLNG || cacheLatlngs.size == MAX_LATLNG){
             tv_latlng.text = Html.fromHtml(HtmlUtils.getGreenHtml(stringBuilder.toString()))
         }else{
             tv_latlng.text = Html.fromHtml(stringBuilder.toString())

         }

    }

    private   var   mDisposable :Disposable?= null
    private   var   startTime :Long  = 0
   private  fun   onStartTime(){
         startTime = System.currentTimeMillis()
         mDisposable = Observable.interval(1,TimeUnit.SECONDS)
                 .compose(RxUtil.IO_Main())
                 .subscribe({
                     btn_lat_lng.text=MessageFormat.format("定位中...(耗时:{0}s)",(System.currentTimeMillis()-startTime)/1000)
                 },{
                     it.printStackTrace()
                 })
    }

    private fun   onStopLocation(){

          val  tag  = btn_lat_lng.tag
          if(tag == "1"){
              //手动取消定位
              averageLatlng = LatLngVo()
              cacheLatlngs.clear()
              btn_lat_lng.text="开始定位"
              btn_lat_lng.tag="0"
              btn_lat_lng.visibility = View.VISIBLE
              btn_delect_latlng.visibility = View.GONE
          }
          if(tag == "2"){
              btn_lat_lng.visibility = View.GONE
              btn_delect_latlng.visibility = View.VISIBLE
          }
          initLocationInfo()
          mDisposable?.dispose()
         locationManager.removeUpdates(this)
    }

    override fun handTag(rfid: String, block0: String?) {
        val   tag = btn_lat_lng.tag.toString()
        if(tag == "2"){
            if(BatichDbUtil.onValRfid(rfid)){
                showMsg("当前标签信息已被使用！")
                return
            }
            tv_rfid.text = MessageFormat.format("标定ID：{0}",rfid)
            if(null == latLngRfid || null == latLngRfid?.id){
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
            }else{
                DialogUtil.showConfigDialog(this,"确定要替换当前标签？"){
                    if(it){
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
        if(requestCode == 0){
            btn_lat_lng.isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            if(it.accuracy <= MAX_ACCURACY){
                cacheLatlngs.add(LatLngVo(it.latitude,it.longitude))
                var lat = 0.0
                var lng = 0.0
                cacheLatlngs.forEach {
                    latlng->
                    lat += latlng.lat
                    lng += latlng.lng
                }
                averageLatlng.lat = lat/cacheLatlngs.size
                averageLatlng.lng = lng/cacheLatlngs.size
                if(cacheLatlngs.size >= MAX_LATLNG){
                    btn_lat_lng.tag = "2"
                    onStopLocation()

                }
            }
            currentLatlng?.let {
                latlng->
                if(it.latitude == latlng.latitude && it.longitude == latlng.longitude){
                    repeatCount++
                    if(repeatCount>= MAX_REPEAT_LATLNG){
                        btn_lat_lng.tag = "2"
                        averageLatlng = LatLngVo(latlng.latitude,latlng.longitude)
                        cacheLatlngs.clear()
                        for (index in 1..MAX_LATLNG){
                            cacheLatlngs.add(LatLngVo(latlng.latitude,latlng.longitude))
                        }
                        onStopLocation()
                    }
                }else{
                    repeatCount=0
                }
            }
            currentLatlng = it
            initLocationInfo()
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_rfid_latlng, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.action_setting ->  {
                showSettingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private   fun   showSettingDialog(){
        val   view  = layoutInflater.inflate(R.layout.custiom_dialog_setting,null)
          view.findViewById<AppCompatEditText>(R.id.edit_max_accuracy).setText(MAX_ACCURACY.toString())
          view.findViewById<AppCompatEditText>(R.id.edit_max_repeat).setText(MAX_REPEAT_LATLNG.toString())
          view.findViewById<AppCompatEditText>(R.id.edit_max_loc).setText(MAX_LATLNG.toString())
        MaterialDialog.Builder(this).title("定位参数设置")
                .customView(view,true)
                .positiveText("确定")
                .negativeText("取消")
                .autoDismiss(true)
                .onPositive { dialog, which ->
                  val editLoc =  dialog.findViewById(R.id.edit_max_loc) as AppCompatEditText
                  if(editLoc.text.toString().trim().isNotEmpty()){
                      MAX_LATLNG = editLoc.text.toString().trim().toInt()
                      spSetInt("max_latlng",MAX_LATLNG)
                  }
                  val repeat  = dialog.findViewById(R.id.edit_max_repeat) as AppCompatEditText
                   if(repeat.text.toString().trim().isNotEmpty()){
                       MAX_REPEAT_LATLNG = repeat.text.toString().trim().toInt()
                       spSetInt("max_repeat_latlng",MAX_REPEAT_LATLNG)
                   }

                  val  accuracy  = dialog.findViewById(R.id.edit_max_accuracy) as AppCompatEditText
                  if(accuracy.text.toString().trim().isNotEmpty()){
                      MAX_ACCURACY = accuracy.text.toString().trim().toFloat()
                      spSetFloat("max_accuracy",MAX_ACCURACY)
                  }
                }.show()
    }

    override fun onDestroy() {
        onStopLocation()
        super.onDestroy()
    }
}