package com.xoliu.aptprocessor


import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.xoliu.aptprocessor.annotations.ExecutionMode
import com.xoliu.aptprocessor.annotations.Subscribe
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

/**
 * 每个有注册Subscribe类中要创建的方法
 */
class CreateMethod(private val typeElement: TypeElement) {
    private val methodMap: MutableMap<String, ExecutableElement> = HashMap()

    /**
     * 放入方法名strin和方法的map，这些都是需要创建的方法索引
     *
     * @param methodName        方法名
     * @param executableElement 方法
     */
    fun putElement(methodName: String, executableElement: ExecutableElement) {
        methodMap[methodName] = executableElement
    }

    /**
     * 创建方法
     *
     * @return
     */
    fun generateMethod(): MethodSpec {
        val subscribedMethod = ClassName.get("com.xoliu.flowmessengers.annotations", "SubscribedMethod")
        val list = ClassName.get("java.util", "List")
        val arrayList = ClassName.get("java.util", "ArrayList")
        val listSubscribeMethods = ParameterizedTypeName.get(list, subscribedMethod)
        val methodBuilder = MethodSpec.methodBuilder(methodName.toString())
        methodBuilder.returns(ParameterizedTypeName.get(list, subscribedMethod))
        methodBuilder.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        methodBuilder.addStatement(
            "\$T subscribedMethods = new \$T<>()",
            listSubscribeMethods,
            arrayList
        )
        for ((methodName1, executableElement) in methodMap) {
            val parameters = executableElement.parameters
            val annotation = executableElement.getAnnotation(Subscribe::class.java)
            val executionMode = annotation.exeutionMode
            val priority = annotation.priority
            val sticky = annotation.sticky // Retrieve sticky attribute
            methodBuilder.addStatement(
                "subscribedMethods.add(new SubscribedMethod(\$T.class, \$T.class, \$T.\$L, \$L, \$S, \$L))",
                typeElement.asType(),
                parameters[0]!!.asType(),
                executionMode.javaClass,
                executionMode.toString(),
                priority,
                sticky.toString() // Add sticky attribute to generated code
            )
        }
        methodBuilder.addStatement("return subscribedMethods")
        return methodBuilder.build()
    }


    val methodName: Any
        get() = "findMethodsIn" + typeElement.simpleName
}