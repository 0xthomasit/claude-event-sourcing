package com.example.order.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing DDD layer dependencies:
 * <pre>
 *   domain  →  (nothing — pure Java)
 *   application  →  domain
 *   infrastructure  →  domain, application
 *   interfaces  →  application
 * </pre>
 */
@AnalyzeClasses(
        packages = "com.example.order",
        importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_interfaces =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..interfaces..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_interfaces =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..interfaces..");

    @ArchTest
    static final ArchRule interfaces_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..interfaces..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
}
