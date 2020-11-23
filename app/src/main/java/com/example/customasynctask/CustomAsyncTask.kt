package com.example.customasynctask

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

abstract class CustomAsyncTask<Params, Progress, Result> {

    private val LOG_TAG = "CustomAsyncTask"
    private var mStatus = Status.PENDING
    private val mCancelled = AtomicBoolean()
    private val mTaskInvoked = AtomicBoolean()

    enum class Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    @WorkerThread
    protected abstract suspend fun doInBackground(params: Array<out Params>): Result

    @MainThread
    protected open fun onPreExecute() {}

    @MainThread
    protected open fun onPostExecute(result: Result?) {}

    @MainThread
    protected open fun onProgressUpdate(vararg values: Progress) {}

    @WorkerThread
    protected suspend fun publishProgress(vararg values: Progress) {
        if (!isCancelled()) {
            withContext(Dispatchers.Main) {
                onProgressUpdate(*values)
            }
        }
    }

    @MainThread
    protected open fun onCancelled(vararg result: Result?) {
        onCancelled()
    }

    @MainThread
    protected open fun onCancelled() {}

    fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        mCancelled.set(true)
        try {
            GlobalScope.cancel()
            return true
        } catch (e: Exception) {
            e.message?.let { Log.e(LOG_TAG, it) }
        }
        return false
    }

    fun isCancelled(): Boolean {
        return mCancelled.get()
    }

    @MainThread
    fun execute(vararg params: Params): CustomAsyncTask<Params, Progress, Result> {
        return executeOnExecutor(*params)
    }

    @MainThread
    fun executeOnExecutor(vararg params: Params): CustomAsyncTask<Params, Progress, Result> {
        if (mStatus != Status.PENDING) {
            when (mStatus) {
                Status.RUNNING -> throw IllegalStateException("Cannot execute task: the task is already running.")
                Status.FINISHED -> throw IllegalStateException("Cannot execute task: the task has already been executed (a task can be executed only once)")
            }
        }

        mStatus = Status.RUNNING
        onPreExecute()
        startBackgroundTaskAndGetResult(*params)
        return this
    }

    private fun startBackgroundTaskAndGetResult(vararg params: Params) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    mTaskInvoked.set(true)
                    var result: Result? = null
                    try {
                        result = doInBackground(params)
                    } catch (throwable: Throwable) {
                        mCancelled.set(true)
                        throw throwable
                    } finally {
                        withContext(Dispatchers.Main) {
                            finish(result)
                        }
                    }
                } catch (e: Exception) {
                    postResultIfNotInvoked(null)
                }
            }
        }
    }

    private suspend fun postResultIfNotInvoked(result: Result?) {
        val wasTaskInvoked = mTaskInvoked.get()
        if (!wasTaskInvoked) {
            withContext(Dispatchers.Main) {
                finish(result)
            }
        }
    }

    private fun finish(result: Result?) {
        if (isCancelled()) {
            onCancelled(result)
        } else {
            onPostExecute(result)
        }
        mStatus = Status.FINISHED
    }
}