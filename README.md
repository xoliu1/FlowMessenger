# FlowMessenger
 我要写个库！

## 关键API

flowOn(Thread.xxx)

initialize / finalize

emit() / collect()

## 主要角色

Subscription：
职责：表示一个订阅者与消息类型之间的关系。
功能：当事件发布时，会遍历所有的 Subscription，根据事件类型找到匹配的订阅者方法并执行。

SubscribedMethod：
职责：封装了订阅者方法的相关信息，包括所在类、参数类型、线程模式、优先级和方法名。
功能： 在事件发布时，FlowMessenger 将根据这些信息找到并调用订阅者方法

Subscribe：
职责：用于标识一个类或方法为订阅者。
功能：通过注解的方式，标识一个类或方法为订阅者，并指定订阅的优先级和线程模式。

MethodInvoker：
职责：负责调用订阅者的方法。
功能：根据订阅者获取所有方法，并根据参数类型匹配到合适的方法，然后执行方法调用逻辑。

