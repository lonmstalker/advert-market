package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates inter-module dependency invariants.
 */
@DisplayName("Module dependency architecture rules")
class ModuleDependencyArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.advertmarket");
    }

    @Test
    @DisplayName("Financial module must not depend on deal,"
            + " marketplace, delivery, or communication")
    void financialModuleShouldNotDependOnBusinessModules() {
        noClasses()
                .that().resideInAPackage("com.advertmarket.financial..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.advertmarket.deal..",
                        "com.advertmarket.marketplace..",
                        "com.advertmarket.delivery..",
                        "com.advertmarket.communication..")
                .because("financial module must remain isolated from other business modules")
                .check(classes);
    }

    @Test
    @DisplayName("API modules must not depend on impl modules")
    void apiModulesShouldNotDependOnImplModules() {
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
                        "com.advertmarket.identity.bot..",
                        "com.advertmarket.identity.service..",
                        "com.advertmarket.financial.service..",
                        "com.advertmarket.financial.repository..",
                        "com.advertmarket.marketplace.service..",
                        "com.advertmarket.marketplace.repository..",
                        "com.advertmarket.deal.service..",
                        "com.advertmarket.deal.repository..",
                        "com.advertmarket.delivery.service..",
                        "com.advertmarket.delivery.repository..",
                        "com.advertmarket.communication.bot..",
                        "com.advertmarket.communication.webhook..",
                        "com.advertmarket.communication.notification..",
                        "com.advertmarket.communication.canary..")
                .because("API modules define contracts and must not reference implementations")
                .check(classes);
    }

    @Test
    @DisplayName("Shared module must not depend on business modules")
    void sharedModuleShouldNotDependOnBusinessModules() {
        noClasses()
                .that().resideInAPackage("com.advertmarket.shared..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.advertmarket.identity..",
                        "com.advertmarket.financial..",
                        "com.advertmarket.marketplace..",
                        "com.advertmarket.deal..",
                        "com.advertmarket.delivery..",
                        "com.advertmarket.communication..",
                        "com.advertmarket.app..")
                .because("shared kernel must not have upstream dependencies")
                .check(classes);
    }
}
