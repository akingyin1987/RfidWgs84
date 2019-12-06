package com.akingyin.rfidwgs

import android.location.Location
import android.location.LocationListener
import android.location.LocationProvider
import android.os.Bundle

/**
 * @ Description:
 * @author king
 * @ Date 2019/12/6 11:56
 * @version V1.0
 */
class GPSLocation constructor(var mGpsLocationListener: GPSLocationListener?) :LocationListener {




    override fun onLocationChanged(location: Location?) {
        location?.let {
            mGpsLocationListener?.UpdateLocation(it)
        }

    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        mGpsLocationListener?.UpdateStatus(provider,status,extras)
        when(status){
            /**
             * GPS 可见
             */
            LocationProvider.AVAILABLE ->{
                mGpsLocationListener?.UpdateGPSProviderStatus(GPSProviderStatus.GPS_AVAILABLE)
            }

            LocationProvider.OUT_OF_SERVICE->{
                mGpsLocationListener?.UpdateGPSProviderStatus(GPSProviderStatus.GPS_OUT_OF_SERVICE)
            }

            LocationProvider.TEMPORARILY_UNAVAILABLE->{
                  mGpsLocationListener?.UpdateGPSProviderStatus(GPSProviderStatus.GPS_TEMPORARILY_UNAVAILABLE)
            }
            else ->{}
        }
    }

    override fun onProviderEnabled(provider: String?) {
        mGpsLocationListener?.UpdateGPSProviderStatus(GPSProviderStatus.GPS_ENABLED)
    }

    override fun onProviderDisabled(provider: String?) {
        mGpsLocationListener?.UpdateGPSProviderStatus(GPSProviderStatus.GPS_DISABLED)
    }
}