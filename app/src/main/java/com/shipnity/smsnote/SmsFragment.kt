package com.shipnity.smsnote

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.concurrent.Executors

class SmsFragment : Fragment() {

    private lateinit var adapter: SmsAdapter
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var polling = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            loadData()
            if (polling) handler.postDelayed(this, 5000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_sms, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SmsAdapter()
        view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SmsFragment.adapter
        }
        view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).setOnRefreshListener { loadData() }
    }

    override fun onResume() {
        super.onResume()
        polling = true
        handler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        polling = false
        handler.removeCallbacks(pollRunnable)
    }

    private fun loadData() {
        executor.execute {
            val messages = try { ApiClient.getSmsMessages() } catch (_: Exception) { null }
            handler.post {
                messages?.let { adapter.update(it.reversed()) }
                view?.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)?.isRefreshing = false
            }
        }
    }
}
