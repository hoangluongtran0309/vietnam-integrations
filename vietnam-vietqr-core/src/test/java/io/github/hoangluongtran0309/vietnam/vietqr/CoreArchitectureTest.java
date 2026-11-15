package io.github.hoangluongtran0309.vietnam.vietqr;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CoreArchitectureTest {

    private static final String CORE_PACKAGE = "io.github.hoangluongtran0309.vietnam.vietqr";
    private static final String GSON_PACKAGE = "com.google.gson.";

    @Test
    void coreDoesNotDependOnFrameworkOrJsonLibraries() {
        noClasses()
                .that().resideInAPackage(CORE_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "com.fasterxml.jackson..",
                        "tools.jackson..")
                .check(importCoreClasses());
    }

    @Test
    void publicApiDoesNotExposeGsonTypes() {
        classes()
                .that().resideInAPackage(CORE_PACKAGE + "..")
                .and().arePublic()
                .should(notExposeGsonTypes())
                .check(importCoreClasses());
    }

    private static JavaClasses importCoreClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(CORE_PACKAGE);
    }

    private static ArchCondition<JavaClass> notExposeGsonTypes() {
        return new ArchCondition<>("not expose Gson types through public members") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getFields().stream()
                        .filter(CoreArchitectureTest::isPublic)
                        .forEach(field -> checkType(field, field.getType(), events));
                javaClass.getMethods().stream()
                        .filter(CoreArchitectureTest::isPublic)
                        .forEach(method -> checkMethod(method, events));
                javaClass.getConstructors().stream()
                        .filter(CoreArchitectureTest::isPublic)
                        .forEach(constructor -> checkConstructor(constructor, events));
            }
        };
    }

    private static boolean isPublic(JavaField field) {
        return field.getModifiers().contains(JavaModifier.PUBLIC);
    }

    private static boolean isPublic(JavaMethod method) {
        return method.getModifiers().contains(JavaModifier.PUBLIC);
    }

    private static boolean isPublic(JavaConstructor constructor) {
        return constructor.getModifiers().contains(JavaModifier.PUBLIC);
    }

    private static void checkMethod(JavaMethod method, ConditionEvents events) {
        checkType(method, method.getReturnType(), events);
        method.getParameterTypes().forEach(type -> checkType(method, type, events));
    }

    private static void checkConstructor(JavaConstructor constructor, ConditionEvents events) {
        constructor.getParameterTypes().forEach(type -> checkType(constructor, type, events));
    }

    private static void checkType(Object member, JavaType type, ConditionEvents events) {
        type.getAllInvolvedRawTypes().stream()
                .filter(rawType -> rawType.getName().startsWith(GSON_PACKAGE))
                .forEach(rawType -> events.add(SimpleConditionEvent.violated(
                        member,
                        member + " exposes Gson type " + rawType.getName())));
    }
}
