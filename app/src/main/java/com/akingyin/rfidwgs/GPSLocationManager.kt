package com.akingyin.rfidwgs

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.lang.ref.WeakReference


/**
 * @ Description:
 * @author king
 * @ Date 2019/12/6 12:10
 * @version V1.0
 */
class GPSLocationManager  private constructor(context: Activity){

    private val GPS_LOCATION_NAME: String = GPS_PROVIDER
    private var gpsLocationManager: GPSLocationManager? = null
    private val objLock: Any = Any()
    private var isGpsEnabled = false
    private var mLocateType: String = GPS_PROVIDER
    lateinit var mContext: WeakReference<Activity>
    lateinit var locationManager: LocationManager
    lateinit var mGPSLocation: GPSLocation
    private var isOPenGps = false
    private var mMinTime: Long = 0
    private var mMinDistance = 0f

   init {
       initData(context)
   }
    private fun initData(context: Activity) {
        mContext = WeakReference(context)
        if (mContext.get() != null) {
            locationManager = (mContext.get()?.getSystemService(Context.LOCATION_SERVICE)) as LocationManager
        }
        //定位类型：GPS
        mLocateType = GPS_PROVIDER
        //默认强制打开GPS设置面板
        isOPenGps = true
        //默认定位时间间隔为2000ms
        mMinTime = 2000
        //默认位置可更新的最短距离为0m
        mMinDistance = 0f
    }

    fun getInstances(context: Activity): GPSLocationManager? {
        if (gpsLocationManager == null) {
            synchronized(objLock) {
                if (gpsLocationManager == null) {
                    gpsLocationManager = GPSLocationManager(context)
                }
            }
        }
        return gpsLocationManager
    }

    /**
     * 方法描述：设置发起定位请求的间隔时长
     *
     * @param minTime 定位间隔时长（单位ms）
     */
    fun setScanSpan(minTime: Long) {
        mMinTime = minTime
    }

    /**
     * 方法描述：设置位置更新的最短距离
     *
     * @param minDistance 最短距离（单位m）
     */
    fun setMinDistance(minDistance: Float) {
        mMinDistance = minDistance
    }

    /**
     * 方法描述：开启定位（默认情况下不会强制要求用户打开GPS设置面板）
     *
     * @param gpsLocationListener
     */
    fun start(gpsLocationListener: GPSLocationListener) {
        this.start(gpsLocationListener, isOPenGps)
    }

    /**
     * 方法描述：开启定位
     *
     * @param gpsLocationListener
     * @param isOpenGps           当用户GPS未开启时是否强制用户开启GPS
     */
    fun start(gpsLocationListener: GPSLocationListener, isOpenGps: Boolean) {
        isOPenGps = isOpenGps
        if (mContext.get() == null) {
            return
        }
        mGPSLocation = GPSLocation(gpsLocationListener)
        isGpsEnabled = locationManager.isProviderEnabled(GPS_LOCATION_NAME)
        if (!isGpsEnabled && isOPenGps) {
            openGPS()
            return
        }

        mContext.get()?.let {
            if (ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return
            }

            val lastKnownLocation: Location? = locationManager.getLastKnownLocation(mLocateType)
            if(null != lastKnownLocation){
                mGPSLocation.onLocationChanged(lastKnownLocation)
            }
            //备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新
            locationManager.requestLocationUpdates(mLocateType, mMinTime, mMinDistance, mGPSLocation)

        }


    }

    /**
     * 方法描述：转到手机设置界面，用户设置GPS
     */
    fun openGPS() {
        Toast.makeText(mContext.get(), "请打开GPS设置", Toast.LENGTH_SHORT).show()
        if (Build.VERSION.SDK_INT > 15) {
            val intent = Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            mContext.get()?.startActivityForResult(intent, 0)
        }
    }

    /**
     * 方法描述：终止GPS定位,该方法最好在onPause()中调用
     */
    fun stop() {
        if (mContext.get() != null) {

            if (ActivityCompat.checkSelfPermission(mContext.get()!!, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext.get()!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            locationManager.removeUpdates(mGPSLocation)
        }
    }
}