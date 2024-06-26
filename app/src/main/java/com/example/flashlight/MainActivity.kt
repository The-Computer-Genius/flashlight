package com.example.flashlight

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.flashlight.databinding.ActivityMainBinding
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity(), SimpleAlertDlg.OnClickListener
{
    companion object
    {
        const val ABOUT_DEV_DLG : Int = 1
        const val DEV_EMAIL : String = "haraseessingh01@gmail.com"
        const val GITHUB_LINK : String = "https://github.com/The-Computer-Genius/flashlight"
    }

    private lateinit var fh : FlashHandler


    private lateinit var mainBinding : ActivityMainBinding

    override fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            Toast.makeText(this, "No flash detected", Toast.LENGTH_LONG).show()
            finishAndRemoveTask()
            return
        }

        initTextView()

        fh = FlashHandler(this)

        val maxFlashStrength = fh.getMaxFlashStrength()
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
            val s = fh.getFlashStrength(this)!!
            mainBinding.slider.value = s.toFloat()
        }

        updateBtnText(fh.flashState)

        mainBinding.slider.addOnChangeListener { _, fl, _ ->
            fh.setFlashStrength(this, fl.toInt())
        }

        mainBinding.flashStateBtn.setOnClickListener {
            val newState = !fh.flashState
            fh.flashState = newState
            updateBtnText(newState)
        }

        startReadingFlashState()


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
            var flashStateStart = fh.flashState
            while (true)
            {
                if (stopReadingFlashState) break
                if (fh.flashState != flashStateStart)
                {
                    runOnUiThread { updateBtnText(fh.flashState) }
                    flashStateStart = fh.flashState
                }
            }
        }
        thread.start()
    }

    private fun openGitHub()
    {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_LINK))
        startActivity(browserIntent)
    }

    private fun initTextView()
    {
        val ss =
                SpannableString("The Torch Project developed by Harasees Singh is free and open source, which means you can view it's source code on GitHub.")
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
        ss.setSpan(clickableSpan, 31, 45, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val textView = mainBinding.textViewOpenSourceInfo
        textView.text = ss
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT

        mainBinding.textViewOpenSourceCode.setOnClickListener { openGitHub() }
    }

    fun updateBtnText(newState : Boolean)
    {
        if (newState)
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

    fun getColorFromTheme(context : Context, color : Int) : Int
    {
        val typedValue = TypedValue()

        val a : TypedArray =
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
        if (dlgUniqueID == ABOUT_DEV_DLG)
        {
            copyTextToClipboard(this, DEV_EMAIL)
            if (Build.VERSION.SDK_INT <= 32)
                Toast.makeText(this, "Email copied", Toast.LENGTH_LONG).show()
        }
    }
}