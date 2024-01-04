package com.waze

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TextAppearanceSpan
import android.util.TypedValue
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

    fun Context.toPx(dp: Int): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics)

    private val paidEnjoy by lazy {
        val text = getString(R.string.paid_enjoy)
        val spannableString = SpannableString(text)
        spannableString.setSpan(
            TextAppearanceSpan(null, 0, toPx(16).toInt(), null, null),
            text.indexOf("You"),
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString
    }

    private val welcome by lazy {
        val text = getString(R.string.welcome)
        val spannableString = SpannableString(text)
        spannableString.setSpan(
            TextAppearanceSpan(null, 0, toPx(16).toInt(), null, null),
            text.indexOf("You"),
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.textUserId.text = getString(R.string.user_id).replace("{id}", id)
        binding.textSupport.setOnClickListener {
            isRedirect = true

            val subject = getString(R.string.emailSubject).replace("{id}", id)
            val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@teslagpsconnection.space?subject=$subject"))
            startActivity(Intent.createChooser(mailIntent, "Select an app"))
        }

        binding.refreshLayout.isRefreshing = true
        binding.refreshLayout.setOnRefreshListener {
            fetch()
        }

        if (intent.extras?.getBoolean("reload") == true || intent?.data == null) {
            fetch()
        } else {
            create()
        }
    }

    private fun create() {
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
                    when(status) {
                        FetchData.STATUS.TEST -> {
                            val stringBuilder = StringBuilder()
                            stringBuilder.append(getString(R.string.statusMode))
                            stringBuilder.append("\n")
                            stringBuilder.append(getString(R.string.remainTime).replace("{time}", getRemainingTime()))
                            binding.proceedText.text = stringBuilder.toString()
                            binding.refreshLayout.isRefreshing = false
                            binding.textPeriodEnded.isVisible = true
                            binding.textPeriodEnded.text = welcome
                            binding.paymentButton.isVisible = true
                            binding.linearSubscription.isVisible = true
                            binding.linearSupport.isVisible = true

                            binding.paymentButton.setOnClickListener {
                                val intent = Intent(this@FreeMapAppActivity, RoutesActivity::class.java)
                                intent.action = this@FreeMapAppActivity.intent.action
                                intent.data = this@FreeMapAppActivity.intent.data
                                startActivity(intent)
                                finish()
                            }
                        }
                        FetchData.STATUS.PAID -> {
                            binding.proceedText.text = paidEnjoy
                            binding.refreshLayout.isRefreshing = false
                            binding.textPeriodEnded.isVisible = false
                            binding.paymentButton.isVisible = false
                            binding.linearSubscription.isVisible = true
                            binding.linearSupport.isVisible = true
                        }
                        FetchData.STATUS.UNPAID -> {
                        }
                    }

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
                processPaidFeature(fetchData)

                val stringBuilder = StringBuilder()
                stringBuilder.append(getString(R.string.statusMode))
                stringBuilder.append("\n")
                stringBuilder.append(getString(R.string.remainTime).replace("{time}", getRemainingTime()))
                binding.proceedText.text = stringBuilder.toString()
                binding.refreshLayout.isRefreshing = false
                binding.textPeriodEnded.isVisible = true
                binding.textPeriodEnded.text = welcome
                binding.paymentButton.isVisible = true
                binding.linearSubscription.isVisible = true
                binding.linearSupport.isVisible = true

                processDeeplink(intent)

                binding.paymentButton.setOnClickListener {
                    val intent = Intent(this, RoutesActivity::class.java)
                    intent.action = this.intent.action
                    intent.data = this.intent.data
                    startActivity(intent)
                    finish()
                }
            }
            FetchData.STATUS.PAID -> {
                processPaidFeature(fetchData)

                binding.proceedText.text = paidEnjoy
                binding.refreshLayout.isRefreshing = false
                binding.textPeriodEnded.isVisible = false
                binding.paymentButton.isVisible = false
                binding.linearSubscription.isVisible = true
                binding.linearSupport.isVisible = true

                processDeeplink(intent)
            }
            FetchData.STATUS.UNPAID -> {
                handleUnpaid(fetchData)
            }
        }
    }

    private fun handleUnpaid(fetchData: FetchData) {

        val edit = sharedPreferences.edit()
        edit.putString("date", null)
        edit.putString("status", fetchData.getStatus().status)
        edit.apply()

        binding.refreshLayout.isRefreshing = false
        binding.linearSubscription.isVisible = true
        binding.textPeriodEnded.isVisible = true
        binding.textPeriodEnded.text = welcome
        binding.paymentButton.isVisible = true
        binding.linearSupport.isVisible = true

        binding.textPeriodEnded.text = getString(R.string.periodEnded)
        binding.proceedText.text = getString(R.string.proceed)

        binding.paymentButton.setOnClickListener {
            val intent = Intent(this, RoutesActivity::class.java)
            intent.action = this.intent.action
            intent.data = this.intent.data
            startActivity(intent)
            finish()
        }

        if (intent.data == null && fetchData.updateRequired) {
            UpdateDialog()
                .show(supportFragmentManager, null)
        }
    }

    private fun processPaidFeature(fetchData: FetchData) {
        val edit = sharedPreferences.edit()
        edit.putString("date", "${fetchData.date}:${fetchData.period}")
        edit.putString("status", fetchData.getStatus().status)
        edit.apply()

        if (intent.data == null && fetchData.updateRequired) {
            UpdateDialog()
                .show(supportFragmentManager, null)
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