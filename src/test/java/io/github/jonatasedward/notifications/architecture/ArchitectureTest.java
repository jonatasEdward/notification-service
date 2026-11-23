package io.github.jonatasedward.notifications.architecture;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "io.github.jonatasedward.notifications", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule featuresAreFreeOfCycles = slices()
            .matching("io.github.jonatasedward.notifications.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule sharedKnowsNoFeature = noClasses()
            .that().resideInAPackage("..shared..")
            .should().dependOnClassesThat().resideInAnyPackage("..events..", "..notifications.notifications..",
                    "..devices..", "..push..");

    @ArchTest
    static final ArchRule notificationsDoNotKnowWhereEventsComeFrom = noClasses()
            .that().resideInAPackage("..notifications.notifications..")
            .should().dependOnClassesThat().resideInAnyPackage("..events..", "org.apache.kafka..", "org.springframework.kafka..");

    @ArchTest
    static final ArchRule onlyTheEventsFeatureTalksToKafka = noClasses()
            .that().resideOutsideOfPackage("..events..")
            .should().dependOnClassesThat().resideInAnyPackage("org.apache.kafka..", "org.springframework.kafka..");

    @ArchTest
    static final ArchRule repositoriesStayInsideTheirFeature = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should(beAccessedOnlyFromTheirOwnPackage());

    private static ArchCondition<JavaClass> beAccessedOnlyFromTheirOwnPackage() {
        return new ArchCondition<>("be accessed only from their own package") {
            @Override
            public void check(JavaClass repository, ConditionEvents events) {
                for (JavaAccess<?> access : repository.getAccessesToSelf()) {
                    if (!access.getOriginOwner().getPackageName().equals(repository.getPackageName())) {
                        events.add(SimpleConditionEvent.violated(access, access.getDescription()));
                    }
                }
            }
        };
    }
}
