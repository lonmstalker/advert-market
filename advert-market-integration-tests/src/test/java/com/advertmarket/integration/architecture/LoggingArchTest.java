package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates logging conventions via ArchUnit.
 */
@DisplayName("Logging architecture tests")
class LoggingArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.advertmarket");
    }

    @Test
    @DisplayName("No System.out/System.err in production code")
    void noSystemOutOrErr() {
        noClasses()
                .that().resideInAPackage("com.advertmarket..")
                .should().accessClassesThat()
                .haveFullyQualifiedName("java.io.PrintStream")
                .because("use SLF4J logging instead of "
                        + "System.out/System.err")
                .check(classes);
    }

    @Test
    @DisplayName("No java.util.logging in production code")
    void noJavaUtilLogging() {
        noClasses()
                .that().resideInAPackage("com.advertmarket..")
                .should().dependOnClassesThat()
                .resideInAPackage("java.util.logging..")
                .because("use SLF4J instead of java.util.logging")
                .check(classes);
    }

    @Test
    @DisplayName("Kafka listener classes must have SLF4J logger")
    void kafkaListenerClassesShouldHaveLogger() {
        ArchRuleDefinition.classes()
                .that().resideInAnyPackage("com.advertmarket..")
                .and().haveSimpleNameEndingWith("Listener")
                .and().areNotInterfaces()
                .should(haveSlf4jLoggerField())
                .because("Kafka listener classes must have"
                        + " an SLF4J logger for structured logging")
                .check(classes);
    }

    @Test
    @DisplayName("Exception handler classes must have SLF4J logger")
    void exceptionHandlerClassesShouldHaveLogger() {
        ArchRuleDefinition.classes()
                .that().resideInAnyPackage(
                        "com.advertmarket.app.error..")
                .and().haveSimpleNameEndingWith("Handler")
                .and().areNotInterfaces()
                .should(haveSlf4jLoggerField())
                .because("exception handlers must have"
                        + " an SLF4J logger for structured logging")
                .check(classes);
    }

    private static ArchCondition<JavaClass> haveSlf4jLoggerField() {
        return new ArchCondition<>(
                "have an SLF4J Logger field (via @Slf4j)") {
            @Override
            public void check(JavaClass clazz,
                    ConditionEvents events) {
                boolean hasLoggerField = clazz.getFields()
                        .stream()
                        .anyMatch(field ->
                                field.getRawType().getName()
                                        .equals("org.slf4j.Logger"));
                if (!hasLoggerField) {
                    events.add(SimpleConditionEvent.violated(
                            clazz,
                            clazz.getSimpleName()
                                    + " is missing SLF4J Logger"
                                    + " (add @Slf4j)"));
                }
            }
        };
    }
}
