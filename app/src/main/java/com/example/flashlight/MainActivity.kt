package com.example.flashlight

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.flashlight.databinding.ActivityMainBinding
import android.hardware.camera2.CameraManager
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), SimpleAlertDlg.OnClickListener
{
    companion object {
        const val ABOUT_DEV_DLG : Int = 1
        const val DEV_EMAIL : String = "haraseessingh01@gmail.com"
    }


    private lateinit var mainBinding : ActivityMainBinding

    override fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            Toast.makeText(this, "No flash detected", Toast.LENGTH_LONG).show()
            finishAndRemoveTask()
            return
        }

        if(FlashService.serviceInstance == null)
            startService(Intent(this, FlashService::class.java))

        initTextView()

        val thread = Thread {

            while(FlashService.serviceInstance == null);

            runOnUiThread {
                val maxFlashStrength = FlashService.getMaxFlashStrength()
                if (maxFlashStrength == null)
                {
                    mainBinding.textView.visibility = View.VISIBLE
                    mainBinding.slider.isEnabled = false
                }
                else
                {
                    mainBinding.textView.visibility = View.GONE
                    mainBinding.slider.isEnabled = true

                    mainBinding.slider.valueTo = maxFlashStrength.toFloat()
                    val s = FlashService.getFlashStrength(this)!!
                    mainBinding.slider.value = s.toFloat()
                }

                updateBtnText(FlashService.flashState)

                mainBinding.slider.addOnChangeListener { _, fl, _ ->
                    FlashService.setFlashStrength(this, fl.toInt())
                }

                mainBinding.flashStateBtn.setOnClickListener {
                    val newState = !FlashService.flashState
                    FlashService.flashState = newState
                    updateBtnText(newState)
                }

                startReadingFlashState()
            }
        }
        thread.start()

    }

    override fun onDestroy()
    {
        super.onDestroy()
        stopReadingFlashState = true
    }

    private var stopReadingFlashState : Boolean = false
    private fun startReadingFlashState()
    {
        val thread = Thread {
            var flashStateStart = FlashService.flashState
            while(true)
            {
                if(stopReadingFlashState)break
                if(FlashService.flashState != flashStateStart)
                {
                    runOnUiThread { updateBtnText(FlashService.flashState) }
                    flashStateStart = FlashService.flashState
                }
            }
        }
        thread.start()
    }

    private fun initTextView()
    {
        val ss = SpannableString("The Flashlight Project developed by Harasees Singh is free and open source, which means you can view it's source code on GitHub.")
        val clickableSpan : ClickableSpan = object : ClickableSpan()
        {
            override fun onClick(textView : View)
            {
                showAboutDevDlg()
            }

            override fun updateDrawState(ds : TextPaint)
            {
                super.updateDrawState(ds)
                ds.color = getColorFromTheme(this@MainActivity, R.attr.linkColor)
                ds.isUnderlineText = false
            }
        }
        ss.setSpan(clickableSpan, 36, 50, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val textView = mainBinding.textViewOpenSourceInfo
        textView.text = ss
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
    }

    fun updateBtnText(newState : Boolean)
    {
        if(newState)
            mainBinding.flashStateBtn.text = "Turn off"
        else
            mainBinding.flashStateBtn.text = "Turn on"
    }

    private fun showAboutDevDlg()
    {
        val dlg = SimpleAlertDlg.newInstance(ABOUT_DEV_DLG)
        dlg.title = "About the developer"
        dlg.body =
                "Harasees Singh is a 12th grade school student. He likes to program desktop applications in C++, Microsoft VC++, Web applications in Python and Android apps in Kotlin.\n\nFor any queries contact:\nharaseessingh01@gmail.com"
        dlg.positiveBtnTitle = "OK"
        dlg.negativeBtnTitle = "Copy email to clipboard"
        dlg.show(supportFragmentManager, ABOUT_DEV_DLG.toString())
    }

    fun getColorFromTheme(context: Context, color : Int) : Int
    {
        val typedValue = TypedValue()

        val a: TypedArray =
                context.obtainStyledAttributes(typedValue.data, intArrayOf(color))
        val retrievedColor = a.getColor(0, 0)
        a.recycle()
        return retrievedColor
    }

    private fun copyTextToClipboard(context : Context, text : String)
    {
        val clipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Email", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    override fun onClickSimpleAlertPositiveBtn(dlg : SimpleAlertDlg, dlgUniqueID : Int)
    {

    }

    override fun onClickSimpleAlertNegativeBtn(dlg : SimpleAlertDlg, dlgUniqueID : Int)
    {
        if(dlgUniqueID == ABOUT_DEV_DLG)
        {
            copyTextToClipboard(this, DEV_EMAIL)
            if(Build.VERSION.SDK_INT <= 32)
                Toast.makeText(this, "Email copied", Toast.LENGTH_LONG).show()
        }
    }
}