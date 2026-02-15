package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Layered boundaries for the marketplace bounded context.
 *
 * <p>Marketplace uses a feature-slice structure: ..web.., ..service.., ..repository...
 */
@DisplayName("Marketplace layered rules")
class LayeredMarketplaceArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.advertmarket.marketplace");
    }

    @Test
    @DisplayName("Marketplace web layer must not access repository layer")
    void webMustNotAccessRepository() {
        noClasses()
                .that().resideInAPackage("..web..")
                .should().accessClassesThat()
                .resideInAPackage("..repository..")
                .because("Controllers must call services/use-cases; repositories are persistence adapters")
                .check(classes);
    }

    @Test
    @DisplayName("Marketplace repository layer must not depend on web layer")
    void repositoryMustNotDependOnWeb() {
        noClasses()
                .that().resideInAPackage("..repository..")
                .should().dependOnClassesThat()
                .resideInAPackage("..web..")
                .because("Persistence must not depend on web/transport concerns")
                .check(classes);
    }

    @Test
    @DisplayName("Marketplace service layer must not depend on web layer")
    void serviceMustNotDependOnWeb() {
        noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat()
                .resideInAPackage("..web..")
                .because("Business services should be transport-agnostic")
                .check(classes);
    }
}

