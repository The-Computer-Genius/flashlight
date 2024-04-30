package com.example.flashlight

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.flashlight.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity()
{

    private lateinit var mainBinding : ActivitySplashBinding


    override fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()

        mainBinding = ActivitySplashBinding.inflate(layoutInflater)

        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            Toast.makeText(this, "No flash detected", Toast.LENGTH_LONG).show()
            finishAndRemoveTask()
            return
        }

        if(appOpenedFirstTime())
        {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        //if(FlashService.serviceInstance == null)
        //    startService(Intent(this, FlashService::class.java))

        val fh = FlashHandler(this)

        fh.flashState = !fh.flashState

        finishAndRemoveTask()
    }

    fun appOpenedFirstTime() : Boolean
    {
        val p = getSharedPreferences("SplashActivity", Context.MODE_PRIVATE)
        return if(p.getBoolean("appOpenedTest", false))
            false
        else
        {
            p.edit().putBoolean("appOpenedTest", true).apply()
            true
        }

    }

}