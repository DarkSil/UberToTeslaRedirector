package com.waze

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.waze.databinding.ActivityMainBinding


class FreeMapAppActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val WAZE_CONST = "waze://?ll="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        intent?.data?.let {

            val uri = it.toString()

            val geo = if (uri.startsWith("geo:")) {
                uri
            } else if (uri.startsWith(WAZE_CONST)) {

                var lastIndex = uri.indexOf("&")
                if (lastIndex == -1) {
                    lastIndex = uri.length
                }

                val ll = uri.substring(WAZE_CONST.length, lastIndex)

                val text = "geo:$ll"

                if (!text.contains("?")) {
                    "$text?q=$ll"
                } else {
                    text
                }

            } else {
                ""
            }

            if (geo.isEmpty()) {
                return@let
            }

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(geo)
            )
            intent.setPackage("com.teslamotors.tesla")
            startActivity(intent)
            finish()
        }
    }
}