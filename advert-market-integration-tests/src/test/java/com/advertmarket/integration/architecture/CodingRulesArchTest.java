package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates project coding conventions via ArchUnit.
 */
@DisplayName("Coding rules architecture tests")
class CodingRulesArchTest {

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
}
