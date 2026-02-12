package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates test coding conventions via ArchUnit.
 */
@DisplayName("Test conventions architecture rules")
class TestConventionsArchTest {

    private static JavaClasses testClasses;

    @BeforeAll
    static void importClasses() {
        testClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined
                        .ONLY_INCLUDE_TESTS)
                .importPackages("com.advertmarket");
    }

    @Test
    @DisplayName("Test classes must have @DisplayName")
    void testClassesMustHaveDisplayName() {
        classes()
                .that().containAnyMethodsThat(
                        annotatedWithTest())
                .should().beAnnotatedWith(
                        "org.junit.jupiter.api.DisplayName")
                .because("all test classes must have "
                        + "@DisplayName for readable test "
                        + "reports")
                .check(testClasses);
    }

    @Test
    @DisplayName("@Test methods must have @DisplayName")
    void testMethodsMustHaveDisplayName() {
        methods()
                .that().areAnnotatedWith(
                        "org.junit.jupiter.api.Test")
                .should().beAnnotatedWith(
                        "org.junit.jupiter.api.DisplayName")
                .because("all @Test methods must have "
                        + "@DisplayName for readable test "
                        + "reports")
                .check(testClasses);
    }

    private static DescribedPredicate<JavaMethod> annotatedWithTest() {
        return new DescribedPredicate<>("annotated with @Test") {
            @Override
            public boolean test(JavaMethod method) {
                return method.getAnnotations().stream()
                        .map(JavaAnnotation::getRawType)
                        .anyMatch(type -> type.getName()
                                .equals("org.junit.jupiter.api.Test"));
            }
        };
    }
}