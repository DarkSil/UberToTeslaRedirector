package com.waze

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.waze.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.ZoneId


class FreeMapAppActivity : AppCompatActivity() {

    // TODO Change +2 to the server timezone
    // TODO Change email address

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val WAZE_CONST = "waze://?ll="

    companion object {
        var isRedirect = false
    }

    private val fetchService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FetchApi::class.java)
    }

    private val sharedPreferences by lazy { getSharedPreferences("details", Context.MODE_PRIVATE) }
    private val id by lazy { Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.textUserId.text = getString(R.string.user_id).replace("{id}", id)
        binding.textSupport.setOnClickListener {
            isRedirect = true
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:example@gmail.com")),
                "Select an app"
            ))
        }

        val date = sharedPreferences.getString("date", null)
        with(date) {
            this?.let {

                val list = it.split(":")

                val period = list[1].toLongOrNull() ?: 0L

                val endTime = Instant
                    .ofEpochMilli(list[0].toLongOrNull() ?: 0L)
                    .atZone(ZoneId.of("+2"))
                    .plusHours(period)
                    .toInstant()
                    .toEpochMilli()

                val currentTime = System.currentTimeMillis()

                if (currentTime >= endTime) {
                    return@let
                } else {
                    val status = FetchData.STATUS.entries
                        .filter { it.status == sharedPreferences.getString("status", FetchData.STATUS.TEST.status) }[0]
                    val statusText = when(status) {
                        FetchData.STATUS.TEST -> {
                            getString(R.string.modeTesting)
                        }
                        FetchData.STATUS.PAID -> {
                            getString(R.string.modePaid)
                        }
                        FetchData.STATUS.UNPAID -> {
                            ""
                        }
                    }

                    val stringBuilder = StringBuilder()
                    stringBuilder.append(getString(R.string.statusMode).replace("{status}", statusText))
                    stringBuilder.append("\n")
                    stringBuilder.append(getString(R.string.remainTime).replace("{time}", getRemainingTime()))
                    binding.testStatus.text = stringBuilder.toString()
                    binding.progressLoad.isVisible = false
                    binding.testStatus.isVisible = true
                    binding.linearSupport.isVisible = true

                    processDeeplink(intent)
                }

                return@with
            }

            fetch()
        }
    }

    private fun fetch() {
        fetchService.fetch(id, BuildConfig.VERSION_CODE).enqueue(object : Callback<FetchData> {
            override fun onResponse(call: Call<FetchData>, response: Response<FetchData>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        handleResponse(it)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@FreeMapAppActivity, "Error. Try again later", Toast.LENGTH_LONG).show()
                    }
                    println("Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<FetchData>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this@FreeMapAppActivity, "Error. Try again later", Toast.LENGTH_LONG).show()
                }
                println("Error: ${t.message}")
            }
        })
    }

    private fun handleResponse(fetchData: FetchData) {
        when (fetchData.getStatus()) {
            FetchData.STATUS.TEST -> {
                processPaidFeature(getString(R.string.modeTesting), fetchData)
            }
            FetchData.STATUS.PAID -> {
                processPaidFeature(getString(R.string.modePaid), fetchData)
            }
            FetchData.STATUS.UNPAID -> {
                handleUnpaid(fetchData)
            }
        }
    }

    private fun handleUnpaid(fetchData: FetchData) {
        binding.progressLoad.isVisible = false
        binding.linearSubscription.isVisible = true
        binding.linearSupport.isVisible = true

        val periodStatus = sharedPreferences.getString("status", FetchData.STATUS.PAID.status)
        val period = when (periodStatus) {
            FetchData.STATUS.TEST.status -> getString(R.string.modeTesting)
            FetchData.STATUS.PAID.status -> getString(R.string.modePaid)
            else -> { getString(R.string.modePaid) }
        }
        binding.textPeriodEnded.text = getString(R.string.periodEnded).replace("{period}", period)

        binding.paymentButton.setOnClickListener {
            val intent = Intent(this, PaymentActivity::class.java)
            intent.action = this.intent.action
            intent.data = this.intent.data
            startActivity(intent)
            finish()
        }

        if (intent.data == null && fetchData.updateRequired && !fetchData.downloadUrl.isNullOrEmpty()) {
            UpdateDialog()
                .setDownloadUrl(fetchData.downloadUrl)
                .show(supportFragmentManager, null)
        }
    }

    private fun processPaidFeature(status: String, fetchData: FetchData) {
        val edit = sharedPreferences.edit()
        edit.putString("date", "${fetchData.date}:${fetchData.period}")
        edit.putString("status", fetchData.getStatus().status)
        edit.apply()

        val stringBuilder = StringBuilder()
        stringBuilder.append(getString(R.string.statusMode).replace("{status}", status))
        stringBuilder.append("\n")
        stringBuilder.append(getString(R.string.remainTime).replace("{time}", getRemainingTime()))
        binding.testStatus.text = stringBuilder.toString()
        binding.progressLoad.isVisible = false
        binding.testStatus.isVisible = true
        binding.linearSupport.isVisible = true
        processDeeplink(intent)

        if (intent.data == null && fetchData.updateRequired) {
            UpdateDialog().show(supportFragmentManager, null)
        }
    }

    private fun processDeeplink(intent: Intent?) {
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

    private fun getRemainingTime(): String {
        val date = sharedPreferences.getString("date", null)

        date?.let {
            val list = it.split(":")

            val period = list[1].toLongOrNull() ?: 0L

            val endTime = Instant
                .ofEpochMilli(list[0].toLongOrNull() ?: 0L)
                .atZone(ZoneId.of("+2"))
                .plusHours(period)
                .toInstant()
                .toEpochMilli()

            val currentTime = System.currentTimeMillis()

            var different = if (endTime > currentTime) {
                endTime - currentTime
            } else {
                0L
            }

            val minutesInMilli = 1000 * 60
            val hoursInMilli = minutesInMilli * 60
            val daysInMilli = hoursInMilli * 24

            val days: Long = different / daysInMilli
            different %= daysInMilli

            val hours: Long = different / hoursInMilli
            different %= hoursInMilli

            val minutes: Long = different / minutesInMilli
            different %= minutesInMilli

            return "${days}D ${hours}H ${minutes}M"
        }

        return ""
    }

    override fun onPause() {
        super.onPause()
        if (!isRedirect) {
            finish()
        }
        isRedirect = false
    }
}