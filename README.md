# 🚀 AxonFramework-Full — A Deliberate Maven→Gradle Migration

**Last Updated: August 23, 2026**

**[📜 View the portfolio page →](https://terrence721.github.io/AxonFramework-Full/portfolio.html)**

A fork of Axon Framework 5 whose entire purpose is migrating
its build system from Maven to Gradle — **one file at a time**, each one individually inspected against the Maven
source and given its own conversion decision, not run through an automated converter or bulk-copied and edited in
place. Not affiliated with the Axon Framework team or AxonIQ — a technical portfolio artifact only.

## 🧭 Start Here

- [`todo.md`](todo.md) — the living status doc: what's done, what's left, every non-obvious decision and why
- [GitHub Project board](https://github.com/users/Terrence721/projects/8) — the same status, as a board
- [Wiki](https://github.com/Terrence721/AxonFramework-Full/wiki) — short pointer pages into the real detail below
- [`docs/diagrams/`](docs/diagrams) — the migration pipeline, module graph, and build-logic architecture, visually
- **[`portfolio.html`](https://terrence721.github.io/AxonFramework-Full/portfolio.html)** — this repo as a
  portfolio piece: real gaps found, real corrections made, and why, for anyone scanning it rather than reading it
  as documentation

## 🧭 Why This Project Matters

Anyone can point a conversion tool at a Maven reactor and commit whatever comes out. The interesting engineering
work is everywhere that approach breaks down: a Gradle convention plugin doesn't have Maven's parent-POM
inheritance, so a shared dependency block that works fine in `axon-parent` creates a literal circular dependency
the moment it's applied to the one module that dependency points at ([`test-logging`](test-logging)). A checkstyle
rule copied byte-for-byte looks complete until you notice it's missing half a symmetric pair on inspection, not
because a script flagged it. Sonatype's newest publishing API has no official Gradle plugin at all, so the
community one you pick has to be verified against its live docs, not assumed from training data — and even then it
can still hit a real Gradle-version interaction bug nobody's documented yet.

This repo is a record of making — and explaining — that kind of call, one file at a time, with the reasoning kept
next to the decision instead of buried in a commit message nobody reads later.

## 🧩 What Axon Framework Is (Summary)

Axon Framework is a Java framework for building applications on CQRS and event-sourcing principles — command/query
separation, an event store as the system of record, and the messaging infrastructure to wire it all together. This
fork doesn't change any of that; right now it changes nothing about the framework's actual code at all. Every
module in the reactor still needs its Java/Kotlin source converted — what exists so far is the **build system**
those modules will eventually sit inside: the Gradle project structure, the shared conventions every module will
apply, and the publishing pipeline that will ship them to Maven Central.

## 🏗 Architecture Overview

A multi-module Gradle build, structurally mirroring the Maven reactor's module list (see
[`settings.gradle.kts`](settings.gradle.kts)), with the cross-module conventions Maven expressed as parent-POM
inheritance now expressed as Gradle convention plugins in [`build-logic/`](build-logic):

- **`axonframework.java-conventions`** — everything `axon-parent` applies to every module: Java 25 toolchain,
  checkstyle, the standard test stack, jar manifest
- **`axonframework.published-conventions`** — layers on `maven-publish`, sources/javadoc jars, POM metadata, and
  signing for modules that ship to Maven Central
- **`axonframework.internal-conventions`** — the explicit non-published counterpart, for modules like
  `docs/_samples` that never should

See [`docs/diagrams/`](docs/diagrams) for how these fit together, and the wiki's
[Divergences From Upstream](https://github.com/Terrence721/AxonFramework-Full/wiki/%E2%AD%90-Divergences-From-Upstream)
page for every place this fork deliberately doesn't match the Maven source's behavior.

## 📋 Project Tracking

- [`todo.md`](todo.md) — done/still-to-do at a glance, plus the reasoning behind every non-obvious call
- [GitHub Project board](https://github.com/users/Terrence721/projects/8) — Backlog → Planned → In Progress →
  Verification & QA → Done
- [Milestones](https://github.com/Terrence721/AxonFramework-Full/milestones) — completed phases, each closed once
  everything in it landed
- [Wiki](https://github.com/Terrence721/AxonFramework-Full/wiki) — one page per subsystem, each a pointer to the
  authoritative source rather than a duplicate of it

On AI-assisted development: Commits co-authored as Claude are AI-assisted implementations directed, reviewed, and merged by Terrence Daniels — same process as every other change, documented in docs/code-review.md.
