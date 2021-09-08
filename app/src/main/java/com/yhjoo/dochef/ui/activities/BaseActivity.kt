package com.yhjoo.dochef.ui.activities

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.AppCompatEditText
import com.yhjoo.dochef.R
import io.reactivex.rxjava3.disposables.CompositeDisposable

open class BaseActivity : AppCompatActivity() {
    var progressDialog: AppCompatDialog? = null
    val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }

    // TODO
    // 용도가?
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // TODO
    // 용도가?
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val view = currentFocus
        val ret = super.dispatchTouchEvent(event)
        if (view is AppCompatEditText) {
            val w = currentFocus
            val scrcoords = IntArray(2)
            w!!.getLocationOnScreen(scrcoords)
            val x = event.rawX + w.left - scrcoords[0]
            val y = event.rawY + w.top - scrcoords[1]
            if (event.action == MotionEvent.ACTION_UP
                && (x < w.left || x >= w.right || y < w.top || y > w.bottom)
            ) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(window.currentFocus!!.windowToken, 0)
            }
        }
        return ret
    }

    override fun onDestroy() {
        super.onDestroy()
        progressOFF()
        if (!compositeDisposable.isDisposed) compositeDisposable.clear()
    }

    fun progressON(activity: Activity?) {
        if (activity == null || activity.isFinishing) {
            return
        }
        if (progressDialog == null || !progressDialog!!.isShowing) {
            progressDialog = AppCompatDialog(activity)
            progressDialog!!.setCancelable(false)
            progressDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            progressDialog!!.setContentView(R.layout.v_progress)
            progressDialog!!.show()
        }
    }

    fun progressOFF() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
    }
}