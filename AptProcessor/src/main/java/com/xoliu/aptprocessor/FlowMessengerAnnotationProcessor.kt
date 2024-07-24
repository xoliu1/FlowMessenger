package com.xoliu.aptprocessor

import com.google.auto.service.AutoService
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.xoliu.flowmessenger.annotations.MethodInvoker
import com.xoliu.flowmessenger.annotations.Subscribe
import com.xoliu.flowmessenger.annotations.SubscribedMethod
import com.xoliu.flowmessenger.annotations.Subscription
import java.io.IOException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.xoliu.flowmessenger.annotations.Subscribe")
class FlowMessengerAnnotationProcessor : AbstractProcessor() {
    /**
     * 处理注解和环境圆形的进程。
     * 这个方法的核心职责是收集所有被@Subscribe注解的方法，并生成一个辅助类，该类用于在运行时查找和调用这些订阅方法。
     *
     * @param annotations 当前处理轮次中所有涉及到的注解类型集合。
     * @param roundEnv 当前处理轮次的环境，用于获取所有被注解的元素。
     * @return false，指示处理器不处理任何后续轮次。
     */
    private var mCachedCreateMethod: MutableMap<String?, CreateMethod?> =
        HashMap<String?, CreateMethod?>()

    //用于log打印
    private var messager: Messager? = null

    // 用于处理类中的元素
    private var elementUtils: Elements? = null

    // 用来创建java文件
    private var filer: Filer? = null

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        messager = processingEnv.messager
        elementUtils = processingEnv.elementUtils
        filer = processingEnv.filer
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

/**
 * 处理注解订阅方法的信息，生成辅助类。
 *
 * @param annotations 可能包含的注解元素集，此处未使用。
 * @param roundEnv    本轮编译中所有被注解处理器处理的元素环境，用于获取被[@Subscribe]注解的方法。
 * @return 总是返回false，表示不需要进一步处理。
 */
override fun process(annotations: Set<TypeElement?>?, roundEnv: RoundEnvironment): Boolean {
    // 定义存储订阅方法的Map的泛型类型
    val parameterizedTypeName = ParameterizedTypeName.get(
        MutableList::class.java,
        SubscribedMethod::class.java
    )
    // 定义Map和Object类名，用于后续构建泛型Map
    val map = ClassName.get("java.util", "Map")
    val `object` = ClassName.get("java.lang", "Object")
    // 构建一个静态最终的Map字段，用于存储订阅方法信息
    val aptMap = FieldSpec.builder(
        ParameterizedTypeName.get(map, `object`, parameterizedTypeName),
        "aptMap")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer("new \$T<>()", HashMap::class.java)
        .build()
    // 构建一个方法，用于获取指定订阅者的所有订阅方法
    val getSubscribMethod = MethodSpec.methodBuilder("getAllSubscribedMethods")
        .addModifiers(Modifier.PUBLIC)
        .returns(
            ParameterizedTypeName.get(
                MutableList::class.java,
                SubscribedMethod::class.java
            )
        )
        .addParameter(Any::class.java, "subscriber")
        .addCode("return aptMap.get(subscriber);")
        .build()
    // 构建一个方法，用于根据订阅信息调用相应的方法
    val invokeMethod: MethodSpec = MethodSpec.methodBuilder("invokeMethod")
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeName.VOID)
        .addParameter(Subscription::class.java, "subscription")
        .addParameter(Any::class.java, "event")
        .build()
    // 构建一个辅助类，用于查找和调用订阅方法
    val aptMethodFinder: TypeSpec.Builder = TypeSpec.classBuilder("AptMethodFinder")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterface(MethodInvoker::class.java)
        .addField(aptMap)
        .addMethod(getSubscribMethod)
        .addMethod(invokeMethod)
    // 获取所有被[@Subscribe]注解的方法
    val elements: Set<Element> = roundEnv.getElementsAnnotatedWith(
        Subscribe::class.java
    )
    for (element in elements) {
        // 转换为可执行方法元素，并获取其所在类元素
        val executableElement = element as ExecutableElement
        val typeElement = executableElement.enclosingElement as TypeElement
        // 获取类的全限定名
        val qualifiedName = typeElement.qualifiedName.toString()
        // 从缓存中获取或创建该类的创建方法辅助类
        var createMethod: CreateMethod? = mCachedCreateMethod!![qualifiedName]
        if (createMethod == null) {
            createMethod = CreateMethod(typeElement)
            mCachedCreateMethod!![qualifiedName] = createMethod
        }
        // 获取方法的简单名称，并将方法元素添加到创建方法辅助类中
        val methodName = executableElement.simpleName.toString()
        createMethod.putElement(methodName, executableElement)
    }
    // 构建静态代码块，用于初始化aptMap
    val codeBlock = CodeBlock.builder()
    for (key in mCachedCreateMethod!!.keys) {
        // 从缓存中获取创建方法辅助类，并生成相应的方法
        val createMethod: CreateMethod? = mCachedCreateMethod!![key]
        if (createMethod != null) {
            aptMethodFinder.addMethod(createMethod.generateMethod())
        }
        // 将生成的方法添加到aptMap中
        if (createMethod != null) {
            codeBlock.add("aptMap.put(\$L.class, \$L());\n", key, createMethod.methodName)
        }
    }
    // 将静态代码块和之前构建的类信息合并，生成最终的类定义
    val typeSpec = aptMethodFinder.addStaticBlock(codeBlock.build()).build()
    // 生成Java文件并写入文件系统
    val javaFile = JavaFile.builder("com.xoliu.flowmessenger", typeSpec)
        .build()
    try {
        javaFile.writeTo(filer)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    // 返回false，表示不需要进一步处理
    return false
}


}