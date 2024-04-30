package com.example.flashlight

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SimpleAlertDlg : DialogFragment()
{
    companion object
    {
        fun newInstance(alertDlgUniqueID : Int) : SimpleAlertDlg
        {
            val dlg = SimpleAlertDlg()
            dlg.alertDlgUniqueID = alertDlgUniqueID
            return dlg
        }
    }
    interface OnClickListener
    {
        fun onClickSimpleAlertPositiveBtn(dlg : SimpleAlertDlg, dlgUniqueID : Int)
        fun onClickSimpleAlertNegativeBtn(dlg : SimpleAlertDlg, dlgUniqueID: Int)
    }
    var title = ""
    var body = ""
    var cancellable : Boolean = true
    var positiveBtnTitle = ""
    var negativeBtnTitle = ""
    var alertDlgUniqueID : Int = 0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        if(savedInstanceState != null)
        {
            title = savedInstanceState.getString("title", title)
            body = savedInstanceState.getString("body", body)
            cancellable = savedInstanceState.getBoolean("cancellable", cancellable)
            positiveBtnTitle = savedInstanceState.getString("positiveBtnTitle", positiveBtnTitle)
            negativeBtnTitle = savedInstanceState.getString("negativeBtnTitle", negativeBtnTitle)
            alertDlgUniqueID = savedInstanceState.getInt("alertDlgUniqueID", alertDlgUniqueID)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        if(context !is OnClickListener)
        {
            dismiss()
            return super.onCreateDialog(savedInstanceState)
        }

        val dlg = MaterialAlertDialogBuilder(requireContext())
        dlg.setTitle(title)
        dlg.setMessage(body)
        isCancelable = cancellable
        if(positiveBtnTitle.isNotEmpty())
        {
            dlg.setPositiveButton(positiveBtnTitle) { _, _ ->
                (context as OnClickListener).onClickSimpleAlertPositiveBtn(this, alertDlgUniqueID)
            }
        }
        if(negativeBtnTitle.isNotEmpty())
        {
            dlg.setNegativeButton(negativeBtnTitle) { _, _ ->
                (context as OnClickListener).onClickSimpleAlertNegativeBtn(this, alertDlgUniqueID)
            }
        }
        return dlg.create()
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)
        outState.putString("title", title)
        outState.putString("body", body)
        outState.putBoolean("cancellable", cancellable)
        outState.putString("positiveBtnTitle", positiveBtnTitle)
        outState.putString("negativeBtnTitle", negativeBtnTitle)
        outState.putInt("alertDlgUniqueID", alertDlgUniqueID)
    }
}