package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates project coding conventions via ArchUnit.
 */
@DisplayName("Coding rules architecture tests")
class CodingRulesArchTest {

    private static final String NONNULL_SIMPLE = "NonNull";
    private static final String NULLABLE_SIMPLE = "Nullable";

    private static final Set<String> PRIMITIVE_NAMES = Set.of(
            "boolean", "byte", "char", "short",
            "int", "long", "float", "double", "void");

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.advertmarket");
    }

    @Test
    @DisplayName("Controllers must not use @Transactional")
    void controllersShouldNotBeTransactional() {
        noClasses()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                .should().beAnnotatedWith(
                        "org.springframework.transaction.annotation.Transactional")
                .because("transactions belong in service layer, not controllers")
                .check(classes);
    }

    @Test
    @DisplayName("No field injection via @Autowired")
    void noFieldInjection() {
        noFields()
                .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .because("use constructor injection (Lombok @RequiredArgsConstructor) instead")
                .check(classes);
    }

    @Test
    @DisplayName("All custom exceptions must extend DomainException")
    void exceptionsShouldExtendDomainException() {
        classes()
                .that().haveSimpleNameEndingWith("Exception")
                .and().resideInAnyPackage(
                        "com.advertmarket.shared.exception..",
                        "com.advertmarket.identity..",
                        "com.advertmarket.financial..",
                        "com.advertmarket.marketplace..",
                        "com.advertmarket.deal..",
                        "com.advertmarket.delivery..",
                        "com.advertmarket.communication..")
                .and().doNotHaveFullyQualifiedName(
                        "com.advertmarket.shared.exception.DomainException")
                .should().beAssignableTo(
                        "com.advertmarket.shared.exception.DomainException")
                .because("all domain exceptions must carry errorCode via DomainException")
                .check(classes);
    }

    @Test
    @DisplayName("@ConfigurationProperties classes must be records")
    void configurationPropertiesShouldBeRecords() {
        classes()
                .that().areAnnotatedWith(
                        "org.springframework.boot.context.properties.ConfigurationProperties")
                .should().beRecords()
                .because("ConfigurationProperties must be immutable records with @PropertyDoc")
                .check(classes);
    }

    @Test
    @DisplayName("Port interface methods must annotate parameters with @NonNull or @Nullable")
    void portMethodParametersMustHaveNullnessAnnotation() {
        methods()
                .that().areDeclaredInClassesThat()
                .resideInAnyPackage("..api.port..")
                .should(haveNullnessAnnotatedParameters())
                .because("port interfaces define API contracts"
                        + " and must specify nullness on all reference parameters")
                .check(classes);
    }

    @Test
    @DisplayName("DTO records in api.dto packages must have @Schema annotation")
    void dtoRecordsShouldHaveSchemaAnnotation() {
        classes()
                .that().resideInAnyPackage("..api.dto..")
                .and().areRecords()
                .should().beAnnotatedWith(
                        "io.swagger.v3.oas.annotations.media.Schema")
                .because("all DTO records must have @Schema"
                        + " for OpenAPI documentation")
                .check(classes);
    }

    @Test
    @DisplayName("Port interface methods must annotate return types with @NonNull or @Nullable")
    void portMethodReturnTypesMustHaveNullnessAnnotation() {
        methods()
                .that().areDeclaredInClassesThat()
                .resideInAnyPackage("..api.port..")
                .should(haveNullnessAnnotatedReturnType())
                .because("port interfaces define API contracts"
                        + " and must specify nullness on non-void return types")
                .check(classes);
    }

    private static ArchCondition<JavaMethod> haveNullnessAnnotatedParameters() {
        return new ArchCondition<>(
                "have @NonNull or @Nullable on all reference parameters") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Method reflected;
                try {
                    reflected = method.reflect();
                } catch (Exception e) {
                    return;
                }
                Parameter[] params = reflected.getParameters();
                for (int i = 0; i < params.length; i++) {
                    if (params[i].getType().isPrimitive()) {
                        continue;
                    }
                    if (!hasNullnessAnnotation(
                            params[i].getAnnotatedType())) {
                        events.add(SimpleConditionEvent.violated(
                                method,
                                String.format(
                                        "Parameter %d of %s.%s"
                                                + " is missing"
                                                + " @NonNull/@Nullable",
                                        i,
                                        method.getOwner()
                                                .getSimpleName(),
                                        method.getName())));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> haveNullnessAnnotatedReturnType() {
        return new ArchCondition<>(
                "have @NonNull or @Nullable on non-void return types") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                JavaClass returnType = method.getRawReturnType();
                if (isPrimitive(returnType)
                        || returnType.getName().equals("void")) {
                    return;
                }
                Method reflected;
                try {
                    reflected = method.reflect();
                } catch (Exception e) {
                    return;
                }
                if (!hasNullnessAnnotation(
                        reflected.getAnnotatedReturnType())) {
                    events.add(SimpleConditionEvent.violated(
                            method,
                            String.format(
                                    "Return type of %s.%s"
                                            + " is missing"
                                            + " @NonNull/@Nullable",
                                    method.getOwner()
                                            .getSimpleName(),
                                    method.getName())));
                }
            }
        };
    }

    private static boolean hasNullnessAnnotation(
            AnnotatedType annotatedType) {
        for (Annotation annotation
                : annotatedType.getAnnotations()) {
            String name = annotation.annotationType()
                    .getSimpleName();
            if (NONNULL_SIMPLE.equals(name)
                    || NULLABLE_SIMPLE.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrimitive(JavaClass type) {
        return PRIMITIVE_NAMES.contains(type.getName());
    }

    // --- Review-driven rules (commit 94b0011) ---

    @Test
    @DisplayName("Shared module must not use Spring stereotype annotations")
    void sharedModuleShouldNotHaveSpringStereotypes() {
        noClasses()
                .that().resideInAPackage("com.advertmarket.shared..")
                .should().beAnnotatedWith(
                        "org.springframework.stereotype.Component")
                .orShould().beAnnotatedWith(
                        "org.springframework.stereotype.Service")
                .orShould().beAnnotatedWith(
                        "org.springframework.stereotype.Repository")
                .because("shared module classes must be registered "
                        + "as @Bean in app module @Configuration, "
                        + "not auto-scanned via @Component")
                .check(classes);
    }

    @Test
    @DisplayName("No @Value annotation — use @ConfigurationProperties")
    void noValueAnnotation() {
        noFields()
                .should().beAnnotatedWith(
                        "org.springframework.beans.factory.annotation.Value")
                .because("use @ConfigurationProperties records "
                        + "with @PropertyDoc instead of @Value")
                .check(classes);
    }

    @Test
    @DisplayName("@ConfigurationProperties must have @PropertyGroupDoc")
    void configurationPropertiesMustHavePropertyGroupDoc() {
        classes()
                .that().areAnnotatedWith(
                        "org.springframework.boot.context.properties.ConfigurationProperties")
                .should(havePropertyGroupDocAnnotation())
                .because("all @ConfigurationProperties records "
                        + "must have @PropertyGroupDoc for "
                        + "generated documentation")
                .check(classes);
    }

    @Test
    @DisplayName("No direct MeterRegistry — use MetricsFacade")
    void noDirectMeterRegistryUsage() {
        noClasses()
                .that().resideInAnyPackage(
                        "com.advertmarket.identity..",
                        "com.advertmarket.financial..",
                        "com.advertmarket.marketplace..",
                        "com.advertmarket.deal..",
                        "com.advertmarket.delivery..",
                        "com.advertmarket.communication..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(
                        "io.micrometer.core.instrument.MeterRegistry")
                .because("use MetricsFacade from shared module "
                        + "instead of MeterRegistry directly")
                .check(classes);
    }

    // --- Localization enforcement ---

    @Test
    @DisplayName("Error handler methods setting title/detail"
            + " must use LocalizationService or buildProblem")
    void errorHandlersMustLocalizeMessages() {
        methods()
                .that().areDeclaredInClassesThat()
                .resideInAPackage("com.advertmarket.app.error..")
                .should(useLocalizationForProblemDetails())
                .because("all error messages in error handlers"
                        + " must go through LocalizationService"
                        + " for i18n support")
                .check(classes);
    }

    // --- Guava anti-pattern bans ---

    @Test
    @DisplayName("No Guava Strings — use JDK or Commons StringUtils")
    void noGuavaStrings() {
        noClasses()
                .that().resideInAPackage("com.advertmarket..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.google.common.base.Strings")
                .because("use JDK String.isBlank()/isEmpty()"
                        + " or Commons StringUtils instead")
                .check(classes);
    }

    @Test
    @DisplayName("Event records must implement DomainEvent")
    void eventRecordsMustImplementDomainEvent() {
        classes()
                .that().resideInAnyPackage("..api.event..")
                .and().haveSimpleNameEndingWith("Event")
                .and().areNotEnums()
                .and().areNotInterfaces()
                .and().doNotHaveModifier(
                        com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)
                .should().implement(
                        "com.advertmarket.shared.event.DomainEvent")
                .because("event records (named *Event) in "
                        + "api.event packages must implement "
                        + "DomainEvent marker interface")
                .check(classes);
    }

    @Test
    @DisplayName("No raw ObjectMapper in business modules — use JsonFacade")
    void noDirectObjectMapperUsage() {
        noClasses()
                .that().resideInAnyPackage(
                        "com.advertmarket.identity..",
                        "com.advertmarket.financial..",
                        "com.advertmarket.marketplace..",
                        "com.advertmarket.deal..",
                        "com.advertmarket.delivery..",
                        "com.advertmarket.communication..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.fasterxml.jackson.databind.ObjectMapper")
                .because("use JsonFacade from shared module "
                        + "instead of ObjectMapper directly")
                .check(classes);
    }

    @Test
    @DisplayName("No @Autowired on constructors — use Lombok")
    void noAutowiredOnConstructors() {
        noClasses()
                .that().resideInAPackage("com.advertmarket..")
                .should(haveAutowiredConstructor())
                .because("use Lombok @RequiredArgsConstructor "
                        + "instead of @Autowired on constructors")
                .check(classes);
    }

    @Test
    @DisplayName("No Guava collection factories"
            + " — use JDK List.of/Set.of/Map.of")
    void noGuavaCollectionFactories() {
        noClasses()
                .that().resideInAPackage("com.advertmarket..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.google.common.collect.Lists")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.google.common.collect.Sets")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.google.common.collect.Maps")
                .because("use JDK List.of(), Set.of(), Map.of()"
                        + " or new ArrayList<>() in Java 25")
                .check(classes);
    }

    private static ArchCondition<JavaMethod>
            useLocalizationForProblemDetails() {
        return new ArchCondition<>(
                "use LocalizationService.msg() or delegate"
                        + " to buildProblem() when calling"
                        + " setTitle/setDetail") {
            @Override
            public void check(JavaMethod method,
                    ConditionEvents events) {
                boolean callsSetTitleOrDetail = method
                        .getMethodCallsFromSelf().stream()
                        .anyMatch(call -> {
                            String target = call.getTargetOwner()
                                    .getSimpleName();
                            String name = call.getName();
                            return "ProblemDetail".equals(target)
                                    && ("setTitle".equals(name)
                                    || "setDetail".equals(name));
                        });
                if (!callsSetTitleOrDetail) {
                    return;
                }
                boolean usesLocalization = method
                        .getMethodCallsFromSelf().stream()
                        .anyMatch(call -> {
                            String target = call.getTargetOwner()
                                    .getSimpleName();
                            String name = call.getName();
                            return ("LocalizationService"
                                    .equals(target)
                                    && "msg".equals(name))
                                    || "buildProblem".equals(name);
                        });
                if (!usesLocalization) {
                    events.add(SimpleConditionEvent.violated(
                            method,
                            method.getFullName()
                                    + " sets ProblemDetail"
                                    + " title/detail without"
                                    + " using LocalizationService"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> haveAutowiredConstructor() {
        return new ArchCondition<>(
                "have @Autowired on a constructor") {
            @Override
            public void check(JavaClass clazz,
                    ConditionEvents events) {
                for (JavaConstructor ctor
                        : clazz.getConstructors()) {
                    boolean hasAutowired = ctor.getAnnotations()
                            .stream()
                            .map(JavaAnnotation::getRawType)
                            .map(JavaClass::getFullName)
                            .anyMatch(name -> name.equals(
                                    "org.springframework.beans"
                                            + ".factory.annotation"
                                            + ".Autowired"));
                    if (hasAutowired) {
                        events.add(SimpleConditionEvent.violated(
                                clazz,
                                clazz.getSimpleName()
                                        + " has @Autowired"
                                        + " on constructor"));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> havePropertyGroupDocAnnotation() {
        return new ArchCondition<>(
                "have @PropertyGroupDoc annotation") {
            @Override
            public void check(JavaClass clazz,
                    ConditionEvents events) {
                boolean found = clazz.getAnnotations()
                        .stream()
                        .map(JavaAnnotation::getRawType)
                        .map(JavaClass::getSimpleName)
                        .anyMatch("PropertyGroupDoc"::equals);
                if (!found) {
                    events.add(SimpleConditionEvent.violated(
                            clazz,
                            clazz.getSimpleName()
                                    + " is missing "
                                    + "@PropertyGroupDoc"));
                }
            }
        };
    }
}
