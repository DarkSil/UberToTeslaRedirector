package com.waze

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.waze.databinding.DialogUpdateBinding

class UpdateDialog : DialogFragment() {

    private val binding by lazy { DialogUpdateBinding.inflate(layoutInflater) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.downloadButton.setOnClickListener {
            FreeMapAppActivity.isRedirect = true
            var url = BuildConfig.baseUrl
            if (url.last() != '/') {
                url += "/"
            }
            url += "update/app.apk"
            requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            dismiss()
        }
    }

}