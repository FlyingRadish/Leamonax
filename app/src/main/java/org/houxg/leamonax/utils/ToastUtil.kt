package org.houxg.leamonax.utils

import android.content.Context
import android.support.annotation.StringRes
import android.widget.Toast
import org.houxg.leamonax.Leamonax
import org.houxg.leamonax.R

class ToastUtil {
    companion object {

        fun show(message: String) {
            Toast.makeText(Leamonax.getContext(), message, Toast.LENGTH_SHORT).show()
        }

        fun show(@StringRes resId: Int) {
            Toast.makeText(Leamonax.getContext(), resId, Toast.LENGTH_SHORT).show()
        }

        fun showNetworkUnavailable() {
            show(R.string.network_is_unavailable)
        }

        fun showNetworkError() {
            show(R.string.network_error)
        }
    }
}