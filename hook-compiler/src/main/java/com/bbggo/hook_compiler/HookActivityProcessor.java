package com.bbggo.hook_compiler;

import com.bbggo.hook_annotation.HookActivity;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * @Description:
 * @Author: wangyuebin
 * @Date: 2021/9/17 7:21 下午
 */
@AutoService(Processor.class)
public class HookActivityProcessor extends AbstractProcessor {

    private Messager mMessager;

    private String hookActivityClass;

    private final String clzName = "HookUtils";
    private final String pkg = "com.bbggo.hook";
    private final String annotationClzPath = "com.bbggo.hooklib.annotation.HookActivity";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        /*try {
            Class<?> clz = Class.forName(annotationClzPath);
            supportTypes.add(clz.getCanonicalName());
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        supportTypes.add(HookActivity.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        mMessager.printMessage(Diagnostic.Kind.WARNING, "\nprocessing...\n");
        // 1，获取所有添加了注解的Activity，保存到List中
        parseAnnotation(roundEnv);

//        ClassName activity = ClassName.get("androidx.appcompat.app", "AppCompatActivity");

        // 2，创建名为 Hook$$Activity类名 的类
        TypeSpec typeSpec = TypeSpec.classBuilder(clzName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(createHookActivityFun())
//                .superclass(activity)
                .build();

        // 4，设置包路径：ProcConsts.PKG_NAME
        JavaFile javaFile = JavaFile.builder(pkg, typeSpec).build();
        try {
            // 5，生成文件
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMessager.printMessage(Diagnostic.Kind.WARNING, "\nprocess finish ...\n");
        return true;// 返回false则只会执行一次
    }

    /**
     * 获取所有注解的Activity,并保存
     * @param roundEnv
     */
    private void parseAnnotation(RoundEnvironment roundEnv) {
        mMessager.printMessage(Diagnostic.Kind.WARNING,"开始处理注解");
        try {
//            Class<? extends Annotation> clz = (Class<? extends Annotation>) Class.forName(annotationClzPath);
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(HookActivity.class);
            for (Element element : elements) {
                // 检查元素是否是一个class.  注意：不能用instanceof TypeElement来判断，因为接口类型也是TypeElement.
                if (element.getKind() != ElementKind.CLASS) {
                    mMessager.printMessage(Diagnostic.Kind.WARNING,
                            element.getSimpleName().toString() + "不是类，不予处理");
                    continue;
                }
                // 放心大胆地强转成TypeElement
                TypeElement classElement = (TypeElement) element;
                // 包名+类型
                hookActivityClass = classElement.getQualifiedName().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MethodSpec createHookActivityFun() {
        ClassName stringName = ClassName.get(String.class);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getHookActivity")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(stringName);

        methodBuilder.addStatement("return \"" + hookActivityClass + "\"");
        return methodBuilder.build();
    }
}
