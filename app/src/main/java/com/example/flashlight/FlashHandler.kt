package com.example.flashlight

import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

class FlashHandler(private val context : Context)
{
    var flashState : Boolean
        get()
        {
            return _flashState
        }
        set(value)
        {
            //serviceInstance?.changeAppIcon(value)
            updateFlashlight(value)
            _flashState = value
            //_flashState is updated in torch callback aswell, but we do it here because callback takes
            //some time
        }
    private var _flashState : Boolean = false

    fun getMaxFlashStrength() : Int?
    {
        if(Build.VERSION.SDK_INT < 33)return null

        val cm = camManager ?: return null

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

        if(!isFlashStrengthApplicable(strength))return false

        val p = context.getSharedPreferences("FlashHandler", Context.MODE_PRIVATE)
        val e = p.edit()
        e.putInt("flashStrength", strength)
        e.apply()

        if(flashState)
        {
            updateFlashlight(false)
            updateFlashlight(true, strength)
        }

        return true
    }

    private fun isFlashStrengthApplicable(strength : Int) : Boolean
    {
        val max = getMaxFlashStrength()
        return (max != null && strength <= max)
    }

    fun getFlashStrength(context : Context) : Int?
    {
        if(Build.VERSION.SDK_INT < 33)return null

        val cm = camManager ?: return null
        if(flashState)
            return cm.getTorchStrengthLevel(cm.cameraIdList[0])

        val p = context.getSharedPreferences("FlashHandler", Context.MODE_PRIVATE)

        val defaultFlashStrength = cm.getCameraCharacteristics(cm.cameraIdList[0]).get(
            CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) ?: return null

        val maxFlashStrength = getMaxFlashStrength() ?: defaultFlashStrength

        return p.getInt("flashStrength", maxFlashStrength)
    }

    private fun readFlashStateFromFile()
    {
        val p = context.getSharedPreferences("FlashHandler", Context.MODE_PRIVATE)
        _flashState = p.getBoolean("flashState", false)
    }

    private fun saveFlashStateToFile()
    {
        val p = context.getSharedPreferences("FlashHandler", Context.MODE_PRIVATE)
        val e = p.edit()
        e.putBoolean("flashState", flashState)
        e.apply()
    }

    private val torchCallBack : CameraManager.TorchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId : String, enabled : Boolean)
        {
            super.onTorchModeChanged(cameraId, enabled)
            _flashState = enabled
            saveFlashStateToFile()
            //changeAppIcon(enabled)
        }
    }


    private var camManager : CameraManager? = null

    init
    {
        if(context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            camManager = context.getSystemService(Service.CAMERA_SERVICE) as CameraManager
            camManager?.registerTorchCallback(torchCallBack, null)
            readFlashStateFromFile()
        }
    }

    private fun updateFlashlight(on : Boolean, strength : Int? = null)
    {
        try {
            val camID = camManager?.cameraIdList?.get(0) ?: return

            if(on)
            {
                val s = if(strength != null && isFlashStrengthApplicable(strength))
                    strength
                else
                    getFlashStrength(context)

                //we don't need the version check below because s will be null if version is
                //smaller than 33, but we check to remove the error android studio shows
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
}