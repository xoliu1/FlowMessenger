package com.xoliu.flowmessengers

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xoliu.aptprocessor.annotations.ExecutionMode
import com.xoliu.aptprocessor.annotations.Subscribe
import com.xoliu.flowmessengers.R
import com.xoliu.flowmessengers.Event.PriorityEvent
import com.xoliu.flowmessengers.Event.ViewEvent
import com.xoliu.flowmessengers.Event.WorkEvent
import com.xoliu.flowmessengers.MsgHub.FlowMessenger

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val startTime = System.currentTimeMillis()
        Log.d(TAG, "onStart: startTime=$startTime")
        registryByReflect()
        val endTime = System.currentTimeMillis()
        Log.d(TAG, "onStart: endTime=$endTime")
        Log.d(TAG, "onStart: 花费时间： " + (endTime - startTime))


        // 测试：发送WorkEvent
        findViewById<View>(R.id.btn_send1).setOnClickListener { v: View? ->
            FlowMessenger.getDefault().post(WorkEvent(5))
        }

        // 测试：主线程发送ViewEvent
        findViewById<View>(R.id.btn_send2).setOnClickListener { v: View? ->
            FlowMessenger.getDefault().post(ViewEvent("主线程测试文字"))
        }

        // 测试：子线程发送ViewEvent
        findViewById<View>(R.id.btn_send3).setOnClickListener { v: View? ->
            object : Thread() {
                override fun run() {
                    super.run()
                    FlowMessenger.getDefault().post(ViewEvent("子线程测试文字"))
                }
            }.start()
        }

        // 解注册bus
        findViewById<View>(R.id.btn_send4).setOnClickListener { v: View? ->
            FlowMessenger.getDefault().unregister(this@MainActivity)
        }
        // 注册bus
        findViewById<View>(R.id.btn_send5).setOnClickListener { v: View? ->
            FlowMessenger.getDefault().register(this@MainActivity)
        }
        // 事件优先级测试
        findViewById<View>(R.id.btn_send6).setOnClickListener { v: View? ->
            FlowMessenger.getDefault().post(PriorityEvent())
        }
    }

    @Subscribe(priority = 1)
    fun onEvent(event: WorkEvent) {
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                "Thread is " + Thread.currentThread().name + " Thread, WorkEvent num=" + event.num,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Subscribe(exeutionMode = ExecutionMode.MAIN_THREAD)
    fun handleView(event: ViewEvent) {
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                "Thread is " + Thread.currentThread().name + " Thread, ViewEvent text=" + event.text,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Subscribe(priority = 1)
    fun onEventPriority1(event: PriorityEvent?) {
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                "priority = 1 ",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Subscribe(priority = 2)
    fun onEventPriority2(event: PriorityEvent?) {
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                "priority = 2 ",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Subscribe(priority = 3)
    fun onEventPriority3(event: PriorityEvent?) {
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                "priority = 3 ",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /**
     * 方法一：以反射调用
     */
    fun registryByReflect() {
        FlowMessenger.getDefault().register(this@MainActivity)
    }

    /**
     * 方法二：APT方式调用
     */
//    fun registryByApt() {
//        val aptMethodFinder: AptMethodFinder = AptMethodFinder()
//        //注解处理器代码的模板类
////        AptMethodFinderTemplate aptMethodFinder = new AptMethodFinderTemplate();
//        //注解处理调用方式
//        FlowMessenger.builder().setMethodHandle(aptMethodFinder).build().register(this)
//    }


    override fun onPause() {
        super.onPause()
        FlowMessenger.getDefault().unregister(this)
    }


    companion object {
        private const val TAG = "MainActivity"
    }
}