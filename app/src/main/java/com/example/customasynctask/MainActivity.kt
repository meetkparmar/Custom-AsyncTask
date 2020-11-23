package com.example.customasynctask

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnDoAsync.setOnClickListener {
            buttonEnable(false)
            val task = MyAsyncTask(this)
            task.execute(10)
        }
    }

    fun buttonEnable(enabled: Boolean) {
        btnDoAsync.isEnabled = enabled
        btnDoAsync.isClickable = enabled
    }

    companion object {

        class MyAsyncTask internal constructor(context: MainActivity) : CustomAsyncTask<Int, String, String>() {

            private var resp: String? = null
            private val activityReference: WeakReference<MainActivity> = WeakReference(context)

            override fun onPreExecute() {
                val activity = activityReference.get()
                if (activity == null || activity.isFinishing) return
                activity.progressBar.visibility = View.VISIBLE
            }

            override suspend fun doInBackground(params: Array<out Int>): String {
                publishProgress("Sleeping Started")
                try {
                    for (i in 10 downTo 0) {
                        delay(1000)
                        publishProgress(i.toString())
                    }
                    resp = "Click on the Start Async Task button to start 10 second long Background Task"
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    resp = e.message
                } catch (e: Exception) {
                    e.printStackTrace()
                    resp = e.message
                }
                return resp ?: ""
            }

            override fun onProgressUpdate(vararg values: String) {
                super.onProgressUpdate(*values)
                val activity = activityReference.get()
                if (activity == null || activity.isFinishing) return
                activity.textView.text = values[0]
            }

            override fun onPostExecute(result: String?) {
                val activity = activityReference.get()
                if (activity == null || activity.isFinishing) return
                activity.progressBar.visibility = View.GONE
                activity.textView.text = result.let { it }
                activity.buttonEnable(true)
            }
        }
    }
}
