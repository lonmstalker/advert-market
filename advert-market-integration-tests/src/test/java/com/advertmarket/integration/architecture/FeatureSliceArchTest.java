package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates feature-slice package conventions for marketplace module.
 */
@DisplayName("Feature-slice package conventions")
class FeatureSliceArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.advertmarket.marketplace");
    }

    @Test
    @DisplayName("Mapper classes must reside in ..mapper.. packages")
    void mappersShouldResideInMapperPackages() {
        classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .or().haveSimpleNameEndingWith("MapperImpl")
                .should().resideInAPackage("..mapper..")
                .because("mappers must be in feature-slice mapper packages "
                        + "(e.g., channel.mapper, pricing.mapper)")
                .check(classes);
    }

    @Test
    @DisplayName("Repository implementations must reside in ..repository.. packages")
    void repositoriesShouldResideInRepositoryPackages() {
        classes()
                .that().haveSimpleNameEndingWith("Repository")
                .and().areNotInterfaces()
                .should().resideInAPackage("..repository..")
                .because("repository implementations must be in "
                        + "feature-slice repository packages "
                        + "(e.g., channel.repository, pricing.repository)")
                .check(classes);
    }

    @Test
    @DisplayName("Controllers must reside in ..web.. packages")
    void controllersShouldResideInWebPackages() {
        classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..web..")
                .because("controllers must be in feature-slice web packages "
                        + "(e.g., channel.web, pricing.web)")
                .check(classes);
    }

    @Test
    @DisplayName("Service classes must reside in ..service.. packages")
    void servicesShouldResideInServicePackages() {
        classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().resideInAPackage("..service..")
                .because("services must be in feature-slice service packages "
                        + "(e.g., channel.service, pricing.service)")
                .check(classes);
    }

    @Test
    @DisplayName("ConfigurationProperties must reside in ..config.. packages")
    void configPropertiesShouldResideInConfigPackages() {
        classes()
                .that().areAnnotatedWith(
                        "org.springframework.boot.context.properties"
                                + ".ConfigurationProperties")
                .and().resideInAPackage("com.advertmarket.marketplace..")
                .should().resideInAPackage("..config..")
                .because("@ConfigurationProperties must be in "
                        + "feature-slice config packages")
                .check(classes);
    }
}
