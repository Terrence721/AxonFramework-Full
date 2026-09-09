/*
 * Copyright (c) 2026. Terrence Daniels
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.terrencedaniels.buildlogic;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Hand-written replacement for the former {@code axonframework.internal-conventions.gradle.kts} precompiled
 * script plugin - see {@link AxonframeworkJavaConventionsPlugin} for why this is now a plain {@link Plugin}
 * class rather than a Kotlin script.
 * <p>
 * For modules that are never published to Maven Central - just {@link AxonframeworkJavaConventionsPlugin},
 * nothing more. Source-equivalent of {@code docs/_samples/pom.xml}'s
 * {@code maven.deploy.skip}/{@code maven.javadoc.skip}/{@code maven.source.skip=true} trio (the only module
 * upstream actually marks this way), extended in this fork to also cover {@code integrationtests} (decided
 * 2026-08-23: an integration-test suite shouldn't ship to Maven Central either, upstream not skipping it there
 * looks like an oversight, not a choice).
 * <p>
 * Applying this plugin instead of just {@link AxonframeworkJavaConventionsPlugin} directly is purely for
 * self-documentation: it states a module's non-published status explicitly and greppably in its own
 * {@code build.gradle.kts}, rather than leaving it as an implicit fact inferred from the absence of
 * {@link AxonframeworkPublishedConventionsPlugin}.
 */
public class AxonframeworkInternalConventionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(AxonframeworkJavaConventionsPlugin.class);
    }
}
