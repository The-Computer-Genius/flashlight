package com.example.flashlight

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel

class MainActivityVM(app : Application) : AndroidViewModel(app)
{
    private val context : Context get() { return getApplication<Application>().applicationContext }

    val fh = FlashHandler(context)
}