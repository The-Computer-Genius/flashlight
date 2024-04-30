package com.example.flashlight

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.TorchCallback
import android.os.Build
import android.os.IBinder
import android.util.Log


class FlashService : Service()
{
    companion object {
        var serviceInstance : FlashService? = null

        var flashState : Boolean
            get()
            {
                return _flashState
            }
            set(value)
            {
                //serviceInstance?.changeAppIcon(value)
                serviceInstance?.updateFlashlight(value)
                _flashState = value
                //_flashState is updated in torch callback aswell, but we do it here because callback takes
                //some time
            }

        fun getMaxFlashStrength() : Int?
        {
            if(Build.VERSION.SDK_INT < 33)return null

            val cm = serviceInstance?.camManager ?: return null

            val i = cm.getCameraCharacteristics(cm.cameraIdList[0])
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: return null
            return if(i > 1)
                i
            else
                null


        }

        fun setFlashStrength(context : Context, strength : Int) : Boolean
        {
            if(Build.VERSION.SDK_INT < 33)return false
            val cm = serviceInstance?.camManager ?: return false

            val max = getMaxFlashStrength()
            if(max == null || strength > max)return false


            val p = context.getSharedPreferences("FlashService", Context.MODE_PRIVATE)
            val e = p.edit()
            e.putInt("flashStrength", strength)
            e.apply()

            if(flashState)
            {
                cm.setTorchMode(cm.cameraIdList[0], false)
                cm.turnOnTorchWithStrengthLevel(cm.cameraIdList[0], strength)
            }

            return true
        }

        fun getFlashStrength(context : Context) : Int?
        {
            if(Build.VERSION.SDK_INT < 33)return null

            val cm = serviceInstance?.camManager ?: return null
            if(flashState)
                return cm.getTorchStrengthLevel(cm.cameraIdList[0])

            val p = context.getSharedPreferences("FlashService", Context.MODE_PRIVATE)

            val defaultFlashStrength = cm.getCameraCharacteristics(cm.cameraIdList[0]).get(
                CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) ?: return null

            val maxFlashStrength = getMaxFlashStrength() ?: defaultFlashStrength

            return p.getInt("flashStrength", maxFlashStrength)
        }


        private var _flashState : Boolean = false
    }


    private val torchCallBack : TorchCallback = object : TorchCallback() {
        override fun onTorchModeChanged(cameraId : String, enabled : Boolean)
        {
            super.onTorchModeChanged(cameraId, enabled)
            _flashState = enabled
            //changeAppIcon(enabled)
        }
    }


    private var camManager : CameraManager? = null


    override fun onStartCommand(intent : Intent?, flags : Int, startId : Int) : Int
    {
        super.onStartCommand(intent, flags, startId)

        if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            camManager = getSystemService(CAMERA_SERVICE) as CameraManager
        else
        {
            stopSelf()
            return START_NOT_STICKY
        }

        camManager?.registerTorchCallback(torchCallBack, null)

        serviceInstance = this


        return START_STICKY
    }


    private fun updateFlashlight(on : Boolean)
    {
        try {
            val camID = camManager?.cameraIdList?.get(0) ?: return

            val s = getFlashStrength(this)

            //we don't need the version check below because s will be null if version is
            //smaller than 33, but we check to remove the error android studio shows
            if(on)
            {
                if (s != null && Build.VERSION.SDK_INT > 33)
                    camManager?.turnOnTorchWithStrengthLevel(camManager!!.cameraIdList[0], s)
                else
                    camManager?.setTorchMode(camID, true)
            }
            else
                camManager?.setTorchMode(camID, false)

        }
        catch (e : CameraAccessException)
        {
            e.printStackTrace()
        }
    }

    override fun onBind(p0 : Intent?) : IBinder?
    {
        return null
    }


    override fun onDestroy()
    {
        super.onDestroy()
        serviceInstance = null
    }


    private fun changeAppIcon(enabled : Boolean)
    {
        if(enabled)
        {
            packageManager.setComponentEnabledSetting(
                ComponentName(
                    applicationContext,
                    "com.example.flashlight.SplashActivityAlias"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)

            packageManager.setComponentEnabledSetting(
                ComponentName(
                    applicationContext,
                    "com.example.flashlight.SplashActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
        }
        else
        {
            packageManager.setComponentEnabledSetting(
                ComponentName(
                    applicationContext,
                    "com.example.flashlight.SplashActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)

            packageManager.setComponentEnabledSetting(
                ComponentName(
                    applicationContext,
                    "com.example.flashlight.SplashActivityAlias"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
        }
    }
}