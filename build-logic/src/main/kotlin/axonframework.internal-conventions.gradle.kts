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

// For modules that are never published to Maven Central - just axonframework.java-conventions,
// nothing more. Source-equivalent of docs/_samples/pom.xml's maven.deploy.skip/maven.javadoc.skip/
// maven.source.skip=true trio (the only module upstream actually marks this way), extended in this
// fork to also cover integrationtests (decided 2026-08-23: an integration-test suite shouldn't ship
// to Maven Central either, upstream not skipping it there looks like an oversight, not a choice).
//
// Applying this plugin instead of just axonframework.java-conventions directly is purely for
// self-documentation: it states a module's non-published status explicitly and greppably in its
// own build.gradle.kts, rather than leaving it as an implicit fact inferred from the absence of
// axonframework.published-conventions.

plugins {
    id("axonframework.java-conventions")
}
