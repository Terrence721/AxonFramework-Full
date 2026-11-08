package org.axonframework.migration;

import org.openrewrite.Recipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MigrationRecipesSmokeTest {

    @Test
    void recipesInstantiate() {
        Recipe[] recipes = {
            new AddAxonTestFixtureTearDown(),
            new AddCommandAnnotation(),
            new AddEntityCreatorAnnotation(),
            new AddEventAnnotation(),
            new AddEventTagAnnotation(),
            new AnnotateObsoleteSequencingPolicyProperty(),
            new Axon4ToAxon5QueryResponseTypes(),
            new ConfigureEventSourcedAnnotation(),
            new ConvertCommandHandlerConstructorToCompanionObject(),
            new ConvertCommandHandlerConstructorToStaticMethod(),
            new MigrateAggregateTestFixtureSetup(),
            new MigrateAxonTestFixtureFluentApi(),
            new MigrateCommandGatewayInEventHandler(),
            new MigrateKotlinDslBomImport("org.axonframework","axon-framework-bom","org revised","axon-framework-bom",null,null),
            new MigrateMessageInterceptorSignatures(),
            new MigrateSequencingPolicyLambda(),
            new MigrateSnapshotTriggerDefinitionToAnnotation(),
            new RemoveTypeArguments("Anything"),
            new ReplaceAggregateLifecycleApply(),
            new UpgradeJava(),
            new UpgradeKotlin()
        };

        for (Recipe recipe : recipes) {
            assertNotNull(recipe);
            assertNotNull(recipe.getDisplayName());
        }
    }
}
