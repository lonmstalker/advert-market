package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates layered architecture boundaries.
 */
@DisplayName("Layer architecture rules")
class LayerArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.advertmarket");
    }

    @Test
    @DisplayName("Controllers must not depend on jOOQ directly")
    void controllersShouldNotDependOnJooq() {
        noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat()
                .resideInAPackage("org.jooq..")
                .because("controllers should use service layer, not access DB directly")
                .check(classes);
    }

    @Test
    @DisplayName("API modules must not depend on Spring framework internals")
    void apiModulesShouldNotDependOnSpringInternals() {
        noClasses()
                .that().resideInAnyPackage(
                        "com.advertmarket.identity.api..",
                        "com.advertmarket.financial.api..",
                        "com.advertmarket.marketplace.api..",
                        "com.advertmarket.deal.api..",
                        "com.advertmarket.delivery.api..",
                        "com.advertmarket.communication.api..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.kafka..",
                        "org.springframework.data.redis..",
                        "org.jooq..",
                        "io.lettuce..")
                .because("API modules define pure contracts without infrastructure dependencies")
                .check(classes);
    }

    @Test
    @DisplayName("API modules must not depend on Spring web or security")
    void apiModulesShouldNotDependOnSpringWeb() {
        noClasses()
                .that().resideInAnyPackage(
                        "com.advertmarket.identity.api..",
                        "com.advertmarket.financial.api..",
                        "com.advertmarket.marketplace.api..",
                        "com.advertmarket.deal.api..",
                        "com.advertmarket.delivery.api..",
                        "com.advertmarket.communication.api..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.security..",
                        "jakarta.servlet..")
                .because("API modules are framework-agnostic contracts")
                .check(classes);
    }
}
