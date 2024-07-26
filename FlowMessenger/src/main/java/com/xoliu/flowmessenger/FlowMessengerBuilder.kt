package com.xoliu.flowmessenger

import com.xoliu.aptprocessor.annotations.MethodInvoker
import com.xoliu.flowmessengers.MsgHub.FlowMessenger

class FlowMessengerBuilder {
    var methodInvoker: MethodInvoker? = null

    fun setMethodHandle(aptInvoke: MethodInvoker): FlowMessengerBuilder {
        this.methodInvoker = aptInvoke
        return this
    }

    fun build(): FlowMessenger {
        val flowMessenger = FlowMessenger(this)
        if (FlowMessenger.instance == null) {
            FlowMessenger.instance = flowMessenger
        }
        return flowMessenger
    }
}
