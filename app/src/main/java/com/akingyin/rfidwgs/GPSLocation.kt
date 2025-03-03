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
class GPSLocation(private var mGpsLocationListener: GPSLocationListener?) :LocationListener {






    override fun onLocationChanged(location: Location) {
        mGpsLocationListener?.UpdateLocation(location)
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


    override fun onProviderDisabled(provider: String) {
        super.onProviderDisabled(provider)
        mGpsLocationListener?.UpdateGPSProviderStatus(GPSProviderStatus.GPS_DISABLED)
    }

    override fun onProviderEnabled(provider: String) {
        super.onProviderEnabled(provider)
        mGpsLocationListener?.UpdateGPSProviderStatus(GPSProviderStatus.GPS_ENABLED)
    }


}