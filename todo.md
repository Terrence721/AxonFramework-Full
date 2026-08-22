# 📝 TODO

**Last Updated: August 22, 2026**

A living list of what's done and what's left on this build. This is a Maven-to-Gradle build-system migration of
[Axon Framework 5](https://github.com/AxonFramework/AxonFramework) — the Maven reactor is being converted **one file
at a time**, each file individually inspected against the Maven source and given its own migration decision, not a
wholesale/automated conversion or a bulk copy of the source tree. See the [project board](https://github.com/users/Terrence721/projects/8)
for live status.

## At a glance

**Done, in full:**

| Item | Detail |
| --- | --- |
| Repo bootstrap | git init, public GitHub repo live at [Terrence721/AxonFramework-Full](https://github.com/Terrence721/AxonFramework-Full) |
| Root `pom.xml` conversion | `settings.gradle.kts` (module list, `examples`/`coverage` Maven-profile gating replicated as conditional Gradle-property includes, Central Portal snapshot repo carried over with its release/snapshot enable flags) and `gradle.properties` (`group`/`version`) — see "Root pom.xml conversion" below |
| License | Apache License 2.0, verbatim copy from the Maven source — required for redistribution under License §4(a) |
| Editor tooling | Kotlin, Checkstyle, XML, and TOML VS Code extensions installed (Gradle/Java support was already present from prior projects) |

**Still to do:** the entire rest of the Maven reactor — `build/parent`, `axon-framework-bom`, `common`, `conversion`,
`extensions` (its own sub-reactor: `kotlin`, `metrics`, `reactor`, `spring`), `eventsourcing`, `messaging`,
`migration`, `modelling`, `test`, `test-logging`, `update`, `integrationtests`, `docs/_samples` — converted bottom-up
by dependency direction, plus the build-logic convention plugins and the `gradle/libs.versions.toml` version catalog
that most of the above depend on. See "Still to do" below.

## ✅ Done

### Root pom.xml conversion

The root `pom.xml` is an aggregator POM (`packaging=pom`), and itself a published artifact
(`org.axonframework:axon`) — not excluded from `central-publishing-maven-plugin`'s `excludeArtifacts`, so the Gradle
root project will replicate it as a POM-only publication once the build-logic convention plugins exist (decision
made, not yet implemented).

Its content split across two files, matching Gradle's own idioms rather than trying to be a literal 1:1 file map:

- `settings.gradle.kts` — the `<modules>` list, the `examples`/`coverage` profile-gated modules (Maven's
  `-Dexamples`/`-Dcoverage` property activation replicated as `providers.gradleProperty(...).isPresent` conditional
  `include(...)` calls), and the Central Portal Snapshots repository (both for dependency resolution now and as the
  Phase 5 SNAPSHOT publish target later)
- `gradle.properties` — `group=org.axonframework` and `version=1.0.0-SNAPSHOT` (replaces the Maven
  `${revision}`/flatten-maven-plugin CI-friendly-version mechanism, which has no Gradle equivalent — `maven-publish`
  always writes the resolved version). Version is independent of upstream's `5.4.0-SNAPSHOT` — this fork versions on
  its own scheme, starting at `1.0.0-SNAPSHOT`, so there's no implication of drop-in compatibility with a specific
  upstream Axon release

Deliberately **not yet converted**: the root POM's developer/license/SCM metadata, the Java-21/Maven-3.9+ enforcer
check, GPG signing, and Central-publishing plugin config. These belong in the build-logic convention plugins
(Gradle's answer to `axon-parent`), which don't exist yet — bolting them onto `settings.gradle.kts`/`gradle.properties`
now would misplace them.

### Known open issue

Gradle wrapper generation is currently blocked: Gradle's `wrapper` task requires every module directory named in
`settings.gradle.kts` to actually exist before it will evaluate the build at all (confirmed — attempting
`./gradlew wrapper --project-dir` against this repo fails with "Configuring project ':build' without an existing
directory is not allowed"). Since modules are being created one at a time as each is converted, this is expected, not
a bug — but it means the project isn't buildable yet, and the wrapper still needs to be bootstrapped some other way
(generated in an isolated location and its files copied in, most likely) before `./gradlew` works here.

## Still to do

Bottom-up by dependency direction, per the source migration plan:

1. **Build-logic convention plugins** — "published Java library" and "internal/test-only module" plugins, plus the
   `gradle/libs.versions.toml` version catalog seeded from `build/parent/pom.xml`'s `<properties>` block
2. **Leaf modules** — `test-logging`, `test`, `common`
3. **Core domain modules** — `conversion`, `messaging`, `eventsourcing`, `modelling`, `migration`, `update`
4. **`extensions` sub-reactor** — `kotlin`, `metrics`, `reactor`, `spring`
5. **Aggregation & verification** — `integrationtests`, `docs/_samples`, JaCoCo coverage aggregation
   (`build/coverage-report` equivalent)
6. **Publishing** — `axon-framework-bom` (via Gradle's `java-platform` plugin), signing, Central Portal publishing,
   source/javadoc jars — last, once every publishable module's coordinates are final

Each phase gets a `./gradlew build` + `publishToMavenLocal` + scratch-consumer-project check, and a dependency-tree
diff against the equivalent Maven output, before moving to the next.
