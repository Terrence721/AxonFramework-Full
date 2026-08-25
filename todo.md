# 📝 TODO

**Last Updated: August 25, 2026**

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
| `common` module | `build.gradle.kts` skeleton done. Source conversion in progress: 61 files done, byte-identical to the Maven source, converted one at a time in dependency order. All 30 top-level files in `org.axonframework.common` are done, plus the `annotation`, `digest`, `function`, `io`, `tx`, `nullability`, and `util` subpackages in full, `lifecycle` (4 of 5 — `Phase.java` deferred until `configuration` exists), and `infra` (1 of 6, same `configuration`-dependency blocker for the rest). 83 files under `src/main/java` still to go (plus 1 of 2 resource files), one at a time |
| `axon-<name>` artifactId fix | Real bug found while setting up `common`: `base.archivesName` doesn't propagate to `MavenPublication.artifactId` — a second, separate default that also needed overriding. Fixed once in `axonframework.published-conventions.gradle.kts` for every module; retroactively fixes `test-logging`, which had been generating a POM with `<artifactId>test-logging</artifactId>` instead of `axon-test-logging` |
| Javadoc doclint gap | Real gap found converting `common`'s first real source: Gradle's javadoc task had no doclint suppression, unlike upstream's own root `pom.xml` (`<doclint>none</doclint>`). Axon's source uses self-closing `<p/>` tags throughout, which JDK 25's stricter javadoc rejects outright — confirmed by a real build failure. Fixed centrally in `axonframework.published-conventions.gradle.kts` |
| CI workflow | `.github/workflows/build.yml` — JDK 25 (Temurin) + `gradle/actions/setup-gradle@v6`, running `./gradlew build` on push to `main` and on pull requests. Originally used `gradle-version: '9.2.0'` before the wrapper existed — first real run that way caught a real bug: the shorthand `"9.2"` fails with `"Error: Gradle version 9.2 does not exist"`, needs the exact release string. Now that the wrapper is committed (see "Gradle wrapper bootstrap"), the pinned version lives there instead and `gradle-version:` was removed. No dedicated quality/badge workflow beyond this. Every push since is checked against a real `gh run list` result before being called done — see "Resolved: local Gradle/Kotlin build flakiness" below for why this mattered more than usual on this machine |
| CodeQL default setup + `build.yml` permissions fix | GitHub's automatic default code scanning is enabled on this repo (`actions` + `java-kotlin`, weekly) — not something explicitly configured here, just GitHub's own default for public repos. Real gap found: `build.yml` had no `permissions:` block, so it ran with the default (potentially read-write) `GITHUB_TOKEN` — flagged as a medium-severity `actions/missing-workflow-permissions` alert. Fixed with an explicit `permissions: contents: read` block, the minimum this workflow actually needs. The `java-kotlin` scan itself found zero code issues (76 rules checked) but reported scan-quality warnings ("Required Gradle version not specified", "Failed to extract dependency information") tracing directly back to the missing Gradle wrapper — the trigger for actually bootstrapping it, see "Gradle wrapper bootstrap" below |
| Documentation & presentation | `README.md`, this file, a [wiki](https://github.com/Terrence721/AxonFramework-Full/wiki) (7 short pointer pages), and [`docs/diagrams/`](docs/diagrams) (4 self-contained HTML pages) — cross-linked, none duplicating another. Every link points at this fork, never upstream's `AxonFramework/AxonFramework` repo |
| GitHub Pages | Enabled, source `main` branch `/docs` — same config as this user's other portfolio forks, makes the diagrams viewable live at [terrence721.github.io/AxonFramework-Full](https://terrence721.github.io/AxonFramework-Full/) |
| Full drift sweep (2026-08-23) | Checked `portfolio.html`, the wiki, all four diagrams, the landing-page card, and the GitHub profile README against actual current state. Found and fixed real drift: `portfolio.html`'s stats were stale (framework source claimed unstarted after 17 files landed; gap count was 5 of the real 8), the landing-page card matched; `build-logic-conventions.html` didn't mention the `artifactId`/doclint fixes; `migration-pipeline.html`'s `common` chip showed no progress. **Most notably: the GitHub profile README (`Terrence721/Terrence721`) already had an AxonFramework-Full entry linking to upstream** (`github.com/AxonFramework/AxonFramework`) — predates this session, not something added here, but a real violation of the never-link-upstream rule caught only by checking every surface directly rather than assuming it was covered |
| Full drift sweep (2026-08-24) | Checked this file, `README.md`, `portfolio.html`, all four diagrams, the wiki, the project board, the GitHub profile, and the landing page. **Biggest find: the "17 of 218 files" figure quoted everywhere since early in the session was wrong from the start** — the actual Maven source has 144 Java files (plus 2 resource files) under `common/src/main`, not 218; nobody had counted it directly until this sweep, every doc had just been propagating the original guess. Corrected everywhere to the real total (144, with 61 done as of this sweep). Also added an `active`/in-progress visual state to `module-dependency-graph.html` (it only had done/not-done before, so `common` rendered as unstarted), and noted the `Implementation-Vendor` fix on `build-logic-conventions.html`'s jar-manifest bullet |
| Project board layout | Fixed to board (Kanban column) layout grouped by Status — was silently defaulting to table/row layout even though the Status column options already matched the reference board |
| Jar manifest `Implementation-Vendor` fix | Real bug, caught on a routine re-read rather than a build failure: `axonframework.java-conventions.gradle.kts` hardcoded `"AxonIQ B.V."`, copied verbatim from what `maven-jar-plugin`'s `addDefaultImplementationEntries` derives from root `pom.xml`'s `<organization>` — every other piece of POM metadata already pointed at this fork's own ownership, this one slipped through. Fixed to `"Terrence Daniels"` |
| UTF-8 idiom modernization (2 files) | `digest.Digester.md5Hex()` and `io.IOUtils.UTF8` both used the pre-Java-7 `getBytes("UTF-8")`/`Charset.forName("UTF-8")` pattern — `getBytes(String)` throws a checked `UnsupportedEncodingException` that can never actually fire (UTF-8 support is JLS-guaranteed), so upstream's own source carries a dead catch block. Modernized both to `StandardCharsets.UTF_8`, removing the unreachable catch in `Digester` entirely. No behavioral change |

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
`gradle build` and `gradle :projects` both succeed. The Gradle wrapper itself wasn't generated in this repo yet
at the time, so building required a system-installed Gradle rather than `./gradlew` — resolved 2026-08-25, see
"Gradle wrapper bootstrap" below.

### Resolved: local Gradle/Kotlin build flakiness (was Windows-specific, this machine)

Starting around the `ProcessRetriesExhaustedException.java` conversion, local `gradle` builds on this development
machine began intermittently failing at `:build-logic:compilePluginsBlocks`/`compileKotlin` with a different
symptom nearly every time: `Source file or directory not found` for a `kotlin-dsl`-generated accessor file,
`NoSuchFileException` mid-directory-walk, a `ClassNotFoundException` from a transformed jar missing a class, an
`Unresolved reference` for `signing`/`maven-publish` DSL members that had compiled fine moments earlier, even a
Kotlin compiler argument the compiler itself couldn't parse.

**Root cause, confirmed 2026-08-24 by a controlled 2×2 test, not guessed at:** VS Code's `redhat.java` Java
language server runs its own background Gradle activity against this project while it's alive, racing manual/CLI
Gradle builds over the same `build-logic/build` output directory. Two independent Gradle-aware processes writing
and reading the same generated-sources tree — sometimes computing different content-hash directory names for
what should be the same logical state — explains every one of the different symptoms above; which stage the two
processes collide at determines which specific exception surfaces. Proven with four back-to-back local builds,
alternating the one variable that mattered:

| `redhat.java` state at build start | Result |
| --- | --- |
| Freshly killed immediately before | SUCCESS |
| Freshly killed immediately before | SUCCESS |
| Left running | FAILURE |
| Left running | FAILURE |

Ruled out before landing on this, in order: stray/duplicate Gradle and Kotlin compile daemons (killed, recurred);
`vscjava.vscode-gradle`'s own `GradleServer` process (disabling the extension stopped it respawning, but the
failure still recurred — a *different* extension, `redhat.java`, was the real culprit, confirmed only later);
global Kotlin-DSL and artifact-transform cache corruption (`~/.gradle/caches/9.2.0/kotlin-dsl` and
`~/.gradle/caches/9.2.0/transforms` both cleared, recurred); `--no-daemon` (recurred, ruling out daemon reuse);
Windows Defender real-time scanning (added exclusions for the repo directory and `~/.gradle` — legitimate
general-purpose improvement, but the failure still recurred with `redhat.java` left running, ruling this out as
the primary cause). The same source, same Gradle 9.2.0, same JDK 25 build **stayed green on every CI run all
session** (Ubuntu runners, no `redhat.java` in the loop) — consistent with the confirmed cause the whole time.

**Fix applied:** `.vscode/settings.json` now sets `"java.import.gradle.enabled": false`, so `redhat.java` stops
doing Gradle-based project import/sync for this workspace at all. Requires a VS Code window reload
(`Developer: Reload Window`) to take effect — a process kill alone respawns the language server with the same
stale in-memory config. **Fallback if flakiness ever recurs:** close VS Code, or kill the `redhat.java` process
(`jps -l`, then `taskkill //PID <pid> //F`), before running a local Gradle build — proven to work above.

Two Windows Defender exclusions were added along the way and are worth keeping regardless of whether they were
the actual fix — real-time-scanning a directory full of freshly-written `.class`/`.jar` files on every build is
wasted CPU either way: `Add-MpPreference -ExclusionPath "C:\Users\Terre\source\repos\AxonFramework-Full"` and
`Add-MpPreference -ExclusionPath "C:\Users\Terre\.gradle"` (both require an elevated PowerShell).

**Local build note:** `./gradlew`/`gradlew.bat` are now committed (2026-08-25, see "Gradle wrapper bootstrap"
below) — use those for a local build. Before this, this machine had no `gradle` on `PATH` either; a Gradle 9.2.0
distribution happened to already be extracted at
`C:\Users\Terre\.gradle\wrapper\dists\gradle-9.2.0-bin\<hash>\gradle-9.2.0\bin\gradle.bat` from an earlier
IDE-triggered download, which is what got invoked directly for every local build attempt in this section above.

## Still to do

Bottom-up by dependency direction, per the source migration plan:

1. ~~**Build-logic convention plugins**~~ — done: `axonframework.java-conventions`, `axonframework.published-conventions`,
   `axonframework.internal-conventions`. (No version catalog — see "Versioning approach" above, that decision
   stands.)
2. **Leaf modules** — `test-logging` done, `common` skeleton done and source conversion underway (61 of 144
   files, one at a time in dependency order — see the "Done" table above for subpackage-level detail). **`test`
   is not actually a leaf** despite this grouping's name:
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
7. ~~**Gradle wrapper bootstrap**~~ — done 2026-08-25: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`,
   and `gradle-wrapper.properties` (pinned to 9.2.0, with an explicit `distributionSha256Sum` sourced from
   Gradle's own distribution server — not just the bare default the `wrapper` task generates) are committed.
   Real gap caught before committing: `gradlew` staged with git file mode `100644` (non-executable) instead of
   `100755` — would have failed with `Permission denied` on every Linux CI checkout. Fixed with
   `git update-index --chmod=+x`, and a new `.gitattributes` locks `gradlew` to LF line endings and
   `gradle-wrapper.jar` to binary, so this machine's global `core.autocrlf=true` can't silently break either
   again on a future commit. `build.yml` updated to use `./gradlew build` instead of `gradle build` with an
   explicit `gradle-version:`, so the wrapper is actually the thing enforcing the pinned version now, not CI
   config duplicating it. Triggered by a real CodeQL default-setup diagnostic ("Required Gradle version not
   specified... may use an incompatible version"), not found proactively.
8. ~~**CI workflows**~~ — done, scoped minimal: `.github/workflows/build.yml` runs `./gradlew build` on push/PR
   via `gradle/actions/setup-gradle`, with an explicit `permissions: contents: read` block. No dedicated
   quality-badge workflow yet (unlike this user's other portfolio forks) — there's barely any actual code for
   those to analyze until later phases land. GitHub's own default CodeQL setup runs regardless (not something
   configured here), found zero code issues so far, and already caught both the missing-permissions gap above
   and the missing-wrapper gap in item 7

Each phase gets a `./gradlew build` + `publishToMavenLocal` + scratch-consumer-project check, and a dependency-tree
diff against the equivalent Maven output, before moving to the next.
