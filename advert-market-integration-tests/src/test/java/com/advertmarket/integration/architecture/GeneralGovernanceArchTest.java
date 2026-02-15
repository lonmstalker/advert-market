package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-cutting governance rules that should hold for the whole codebase.
 */
@DisplayName("General governance rules")
class GeneralGovernanceArchTest {

    private static final String BASE = "com.advertmarket";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("No cycles between top-level packages under com.advertmarket")
    void noCyclesBetweenTopLevelPackages() {
        SlicesRuleDefinition.slices()
                .matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .because("Top-level packages represent bounded contexts/modules and must not form dependency cycles")
                .check(classes);
    }

    @Test
    @DisplayName("Controllers must not access repository implementations directly")
    void controllersMustNotAccessRepositoriesDirectly() {
        noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().accessClassesThat()
                .resideInAPackage("..repository..")
                .because("Web layer must depend on services/use-cases, not on persistence implementations")
                .check(classes);
    }

    @Test
    @DisplayName("@Transactional must not be used in repository packages")
    void transactionalMustNotBeUsedInRepositoryPackages() {
        noClasses()
                .that().resideInAPackage("..repository..")
                .should().beAnnotatedWith(Transactional.class)
                .because("@Transactional belongs to service/use-case boundaries; repository transactions hide business atomicity")
                .check(classes);

        noMethods()
                .that().areDeclaredInClassesThat()
                .resideInAPackage("..repository..")
                .should().beAnnotatedWith(Transactional.class)
                .because("@Transactional belongs to service/use-case boundaries; repository transactions hide business atomicity")
                .check(classes);
    }

    @Test
    @DisplayName("Controllers must not use ResponseStatusException (use DomainException + ProblemDetail)")
    void controllersMustNotUseResponseStatusException() {
        noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.web.server.ResponseStatusException")
                .because("Controllers must preserve the unified error contract (ProblemDetail + error_code)")
                .check(classes);
    }
}

