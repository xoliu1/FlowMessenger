package com.xoliu.aptprocessor.annotations

/***
 * Subscription 类表示一个订阅关系，包括订阅者对象（Subscriber）和订阅方法（SubscribedMethod）。
 * 当事件发布时，EventBus 会遍历所有的 Subscription，根据事件类型找到匹配的订阅者方法并执行。
 * @param null
 * @return
 * @author xoliu
 * @create 24-7-25
 **/

class Subscription(//订阅者类
    val subscriber: Any, //订阅者方法类
    private val subscribedMethod: SubscribedMethod, // 优先级
    private val priority: Int
) : Comparable<Subscription?> {
    fun subscriber(): Any {
        return subscriber
    }

    fun getTheSubscribedMethod(): SubscribedMethod {
        return subscribedMethod
    }


    /**
     * 比较当前订阅对象与另一个订阅对象的优先级。
     *
     * 此方法用于实现Subscription类的排序逻辑，特别是当多个订阅对象需要被系统根据优先级排序时。
     * 通过比较两个订阅对象的优先级数值，决定它们的排序关系。
     *
     * @param other 另一个订阅对象，用于与当前对象进行比较。
     * @return 返回一个整型值，表示两个订阅对象的优先级关系。
     *         如果other不为空，返回other.priority和this.priority的比较结果；
     *         如果other为空，表示当前对象优先级更高，返回0。
     */
    override fun compareTo(other: Subscription?): Int {
        if (other != null) {
            // 使用Integer.compare方法安全地比较两个优先级值，确保排序的稳定性。
            return Integer.compare(other.priority, this.priority)
        }
        // 当other为空时，认为当前对象的优先级更高，返回0。
        return 0
    }

    override fun toString(): String {
        return "Subscription{" +
                "subscriber=" + subscriber +
                ", subscribedMethod=" + subscribedMethod +
                '}'
    }
}