package com.advertmarket.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.base.ArchUnitException.InconsistentClassPathException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Validates web API conventions via ArchUnit.
 */
@DisplayName("Web API architecture rules")
class WebApiArchTest {

    private static JavaClasses classes;
    private static final List<Class<? extends Annotation>> REQUEST_MAPPING_ANNOTATIONS = List.of(
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class,
            PatchMapping.class
    );

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.advertmarket");
    }

    @Test
    @DisplayName("Request handler methods must not have more than 2 @RequestParam parameters")
    void requestHandlersShouldUseRequestParamObjects() {
        methods()
                .should(requestHandlerMethodsHaveAtMostRequestParamParameters(2))
                .because("endpoints with many query params become unreadable; "
                        + "use a dedicated request params object instead")
                .check(classes);
    }

    private static ArchCondition<JavaMethod> requestHandlerMethodsHaveAtMostRequestParamParameters(
            int max) {
        return new ArchCondition<>(
                "have at most " + max + " @RequestParam parameters") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Method reflected;
                try {
                    reflected = method.reflect();
                } catch (InconsistentClassPathException e) {
                    return;
                }

                if (!isControllerClass(reflected.getDeclaringClass())) {
                    return;
                }
                if (!isRequestHandlerMethod(reflected)) {
                    return;
                }

                long count = Arrays.stream(reflected.getParameters())
                        .filter(WebApiArchTest::isRequestParam)
                        .count();

                if (count > max) {
                    events.add(SimpleConditionEvent.violated(
                            method,
                            String.format(
                                    "%s.%s has %d @RequestParam parameters (max %d). "
                                            + "Use a request params object instead.",
                                    method.getOwner().getName(),
                                    method.getName(),
                                    count,
                                    max)));
                }
            }
        };
    }

    private static boolean isControllerClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(RestController.class)
                || clazz.isAnnotationPresent(Controller.class);
    }

    private static boolean isRequestHandlerMethod(Method method) {
        for (Class<? extends Annotation> mappingAnnotation : REQUEST_MAPPING_ANNOTATIONS) {
            if (method.isAnnotationPresent(mappingAnnotation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRequestParam(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestParam.class);
    }
}
