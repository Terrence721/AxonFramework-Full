# 📝 TODO

**Last Updated: August 23, 2026**

A living list of what's done and what's left on this build. This is a Maven-to-Gradle build-system migration of
Axon Framework 5 — the Maven reactor is being converted **one file
at a time**, each file individually inspected against the Maven source and given its own migration decision, not a
wholesale/automated conversion or a bulk copy of the source tree. See the [project board](https://github.com/users/Terrence721/projects/8)
for live status, or [Milestones](https://github.com/Terrence721/AxonFramework-Full/milestones) for completed phases.

## At a glance

**Done, in full:**

| Item | Detail |
| --- | --- |
| Repo bootstrap | git init, public GitHub repo live at [Terrence721/AxonFramework-Full](https://github.com/Terrence721/AxonFramework-Full) |
| Root `pom.xml` conversion | `settings.gradle.kts` (module list, `examples`/`coverage` Maven-profile gating replicated as conditional Gradle-property includes, Central Portal snapshot repo carried over with its release/snapshot enable flags) and `gradle.properties` (`group`/`version`) — see "Root pom.xml conversion" below |
| License | Apache License 2.0, verbatim copy from the Maven source — required for redistribution under License §4(a) |
| Editor tooling | Kotlin, Checkstyle, XML, and TOML VS Code extensions installed (Gradle/Java support was already present from prior projects) |
| `build/checkstyle.xml` | Added from `build/parent`'s Maven source, keeping Axon's own copyright header (carried-over content, not new authorship). One deliberate change: also bans `org.springframework.lang.Nullable`, completing the `Nonnull`/`Nullable` pairing every other framework in the list already had |
| `.gitignore` `build/` collision fix | Real bug: the root `build/` directory does double duty as both real tracked content (`build/checkstyle.xml`, `build/parent`) and Gradle's own default output directory for the root project — a blanket exception meant to protect the former was quietly letting Gradle's generated reports through as trackable files too. Replaced with an explicit allow-list of just the real paths, verified with `git check-ignore -v` |
| Stale upstream config found | Root `pom.xml`'s `maven-javadoc-plugin` `skippedModules` list names `axon-todo`, `axon-legacy`, `axon-legacy-aggregate`, `axon-legacy-saga` — none exist anywhere in this Axon 5 reactor's actual module list. Uncleaned drift from an Axon 4-era POM, confirmed by cross-checking every real module name. Not carried forward |
| Build-logic convention plugins | All three exist: `axonframework.java-conventions` (compiler, checkstyle, test deps, jar manifest — everything `axon-parent` applies to every module), `axonframework.published-conventions` (`maven-publish`, sources/javadoc jars, POM metadata, env-var-gated GPG signing), `axonframework.internal-conventions` (explicit non-published marker, applied to `docs/_samples` and, as a deliberate addition beyond upstream, `integrationtests`) |
| Central Portal publishing | Wired via `com.gradleup.nmcp`'s settings plugin — Sonatype has no official Gradle plugin for this. `USER_MANAGED` release type: nothing reaches Maven Central without a manual release step |
| `test-logging` module | First real subproject. Published (`org.axonframework:axon-test-logging`). Fixed a real circular-dependency trap along the way — the shared `testImplementation(project(":test-logging"))` line would otherwise make this module depend on itself |
| `common` module | `build.gradle.kts` skeleton done. Source conversion in progress: 17 files done, byte-identical to the Maven source, converted in dependency order — the base exception hierarchy, `StringUtils`, `Assert`, `ClassUtils`, `ClockUtils`, `DirectExecutor`, `Priority`, `BuilderUtils`, `AxonThreadFactory`, `Registration`, `ObjectUtils`, `CollectionUtils`, `ExceptionUtils`, `ListUtils`. 201 files under `src/main/java` still to go, one at a time |
| `axon-<name>` artifactId fix | Real bug found while setting up `common`: `base.archivesName` doesn't propagate to `MavenPublication.artifactId` — a second, separate default that also needed overriding. Fixed once in `axonframework.published-conventions.gradle.kts` for every module; retroactively fixes `test-logging`, which had been generating a POM with `<artifactId>test-logging</artifactId>` instead of `axon-test-logging` |
| Javadoc doclint gap | Real gap found converting `common`'s first real source: Gradle's javadoc task had no doclint suppression, unlike upstream's own root `pom.xml` (`<doclint>none</doclint>`). Axon's source uses self-closing `<p/>` tags throughout, which JDK 25's stricter javadoc rejects outright — confirmed by a real build failure. Fixed centrally in `axonframework.published-conventions.gradle.kts` |
| CI workflow | `.github/workflows/build.yml` — JDK 25 (Temurin) + `gradle/actions/setup-gradle@v6` pinned to `9.2.0` (no wrapper yet, so `gradle-version` installs it directly), running `gradle build` on push to `main` and on pull requests. First real run caught a real bug: the shorthand `"9.2"` fails with `"Error: Gradle version 9.2 does not exist"` — needs the exact release string. Scoped minimal deliberately — no CodeQL/quality-badge suite yet. A persistent watch on this repo's CI runs is active for the rest of the session |
| Documentation & presentation | `README.md`, this file, a [wiki](https://github.com/Terrence721/AxonFramework-Full/wiki) (7 short pointer pages), and [`docs/diagrams/`](docs/diagrams) (4 self-contained HTML pages) — cross-linked, none duplicating another. Every link points at this fork, never upstream's `AxonFramework/AxonFramework` repo |
| GitHub Pages | Enabled, source `main` branch `/docs` — same config as this user's other portfolio forks, makes the diagrams viewable live at [terrence721.github.io/AxonFramework-Full](https://terrence721.github.io/AxonFramework-Full/) |
| Full drift sweep (2026-08-23) | Checked `portfolio.html`, the wiki, all four diagrams, the landing-page card, and the GitHub profile README against actual current state. Found and fixed real drift: `portfolio.html`'s stats were stale (framework source claimed unstarted after 17 files landed; gap count was 5 of the real 8), the landing-page card matched; `build-logic-conventions.html` didn't mention the `artifactId`/doclint fixes; `migration-pipeline.html`'s `common` chip showed no progress. **Most notably: the GitHub profile README (`Terrence721/Terrence721`) already had an AxonFramework-Full entry linking to upstream** (`github.com/AxonFramework/AxonFramework`) — predates this session, not something added here, but a real violation of the never-link-upstream rule caught only by checking every surface directly rather than assuming it was covered |
| Project board layout | Fixed to board (Kanban column) layout grouped by Status — was silently defaulting to table/row layout even though the Status column options already matched the reference board |

**Still to do:** the rest of the Maven reactor — `build/parent`, `axon-framework-bom`, `common`, `conversion`,
`extensions` (its own sub-reactor: `kotlin`, `metrics`, `reactor`, `spring`), `eventsourcing`, `messaging`,
`migration`, `modelling`, `test`, `update`, `integrationtests`, `docs/_samples` — converted bottom-up
by dependency direction. Dependency versions are declared inline per module
(no central `gradle/libs.versions.toml` catalog — see "Versioning approach" below), matching the style already used in
`saga-full`. See "Still to do" below.

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

### Versioning approach

No central `gradle/libs.versions.toml` version catalog — dependency versions are declared inline, per module, in
each module's own `build.gradle.kts`, matching the style already used in `saga-full`. A catalog was built once
(mechanically added from `build/parent/pom.xml`'s ~50-entry `dependencyManagement` block) and then deliberately
removed in favor of this approach.

**Known, accepted tradeoff:** `build/parent/pom.xml`'s `dependencyManagement` exists specifically to keep dependency
versions in sync across all 14 reactor modules from one place. Inline-per-module drops that guarantee — nothing
prevents two modules drifting to different versions of the same dependency by typo, the way a shared Maven parent
would catch. Accepted deliberately, not overlooked.

### Resolved: the module-directory blocker

Previously, every `include(...)` in `settings.gradle.kts` pointed at a module directory that didn't exist yet,
which made Gradle refuse to configure the build at all — not just the missing module, the whole build (confirmed:
`./gradlew wrapper --project-dir` failed with "Configuring project ':build' without an existing directory is not
allowed"). Fixed by commenting out each `include(...)` individually, uncommenting one at a time as that module is
actually converted — `test-logging` is the first to come back on. The build is real and buildable today:
`gradle build` and `gradle :projects` both succeed. The Gradle wrapper itself still isn't generated in this repo
(bootstrapping it is a separate, still-open task), so building currently requires a system-installed Gradle rather
than `./gradlew`.

### Known open issue: local Gradle/Kotlin build flakiness (Windows-specific, this machine)

Starting around the `ProcessRetriesExhaustedException.java` conversion, local `gradle` builds on this development
machine began intermittently failing at `:build-logic:compilePluginsBlocks` with `Source file or directory not
found: ...kotlin-dsl-external-plugin-spec-builders\...\PluginSpecBuilders.kt` (or the equivalent
`compileKotlin`/`compilePluginsBlocks` variants) — a file that `generateExternalPluginSpecBuilders` is supposed to
have just written moments earlier.

**Diagnosed, not guessed at** — ruled out, in order: stray/duplicate Gradle and Kotlin compile daemons (killed,
recurred); VS Code's Gradle extension (`vscjava.vscode-gradle`, running its own background `GradleServer` process)
racing its own builds against the CLI ones over the same `build-logic/build` directory (confirmed via `jps -l`
showing it respawn repeatedly; disabling the extension for this workspace stopped the respawning, but the same
compile failure still recurred afterward); global Kotlin-DSL cache corruption (`~/.gradle/caches/9.2.0/kotlin-dsl`
cleared, recurred); Gradle's up-to-date task-history cache (`--rerun-tasks` forced, recurred, same file hash);
Kotlin daemon reuse (`-Dkotlin.compiler.execution.strategy=in-process` forced, recurred, same file hash again).

**Working theory, not yet confirmed:** a Windows-specific file-write-visibility race between the task that writes
the generated accessor file and the task that reads it — plausibly antivirus scanning the newly-created file, or
a remaining file watcher (`redhat.java`'s language server, deliberately left running) locking it briefly. The
same source, same Gradle 9.2.0, same JDK 25 build **has stayed green on every CI run all session** (Ubuntu
runners) — strong evidence this is local-machine/OS-specific, not a defect in the project's actual build
configuration or source.

**Current approach:** treat CI as the real verification for now rather than fighting this further locally per
file. If it starts affecting real work (not just verification double-checks), worth trying: a Gradle daemon
health check/reset, disabling Windows Defender real-time scanning for this repo's directory, or filing it
upstream against `kotlin-dsl`/Gradle if a minimal repro can be isolated.

## Still to do

Bottom-up by dependency direction, per the source migration plan:

1. ~~**Build-logic convention plugins**~~ — done: `axonframework.java-conventions`, `axonframework.published-conventions`,
   `axonframework.internal-conventions`. (No version catalog — see "Versioning approach" above, that decision
   stands.)
2. **Leaf modules** — `test-logging` done, `common` skeleton done and source conversion underway (17 of 218
   files, one at a time in dependency order). **`test` is not actually a leaf** despite this grouping's name:
   `test/pom.xml` depends on `axon-eventsourcing` (Phase 3, not started) and `axon-common` (test-jar) — converting
   it now would hit the same "Gradle won't configure the whole build" problem the module-directory blocker
   already caused once. Don't start `test` until `eventsourcing` exists.
3. **Core domain modules** — `conversion`, `messaging`, `eventsourcing`, `modelling`, `migration`, `update`
4. **`extensions` sub-reactor** — `kotlin`, `metrics`, `reactor`, `spring`
5. **Aggregation & verification** — `integrationtests`, `docs/_samples`, JaCoCo coverage aggregation
   (`build/coverage-report` equivalent)
6. **Publishing** — Central Portal publishing (`com.gradleup.nmcp`) and signing are wired; still open:
   `axon-framework-bom` (via Gradle's `java-platform` plugin) and the root `axon` / `axon-parent` POM-only
   publications — last, once every publishable module's coordinates are final
7. **Gradle wrapper bootstrap** — generate in an isolated location and copy the files in, now that the
   module-directory blocker above is resolved
8. ~~**CI workflows**~~ — done, scoped minimal: `.github/workflows/build.yml` runs `gradle build` on push/PR via
   `gradle/actions/setup-gradle`. Not yet doing a full quality/CodeQL/badge suite (unlike this user's other
   portfolio forks) — there's barely any actual code for those to analyze until later phases land

Each phase gets a `./gradlew build` + `publishToMavenLocal` + scratch-consumer-project check, and a dependency-tree
diff against the equivalent Maven output, before moving to the next.
