/*
 * Copyright (c) 2010-2026. Axon Framework
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

package org.axonframework.migration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Migrates the AF4 Spring Boot snapshotting configuration to the AF5 {@code @Snapshotting} annotation.
 * <p>
 * In AF4, snapshotting required two parts:
 * <ol>
 *     <li>A {@code @Bean} method in a {@code @Configuration} class returning a
 *     {@code SnapshotTriggerDefinition} (e.g. {@code new EventCountSnapshotTriggerDefinition(snapshotter, 100)}).</li>
 *     <li>A {@code snapshotTriggerDefinition = "beanName"} attribute on the aggregate's {@code @Aggregate}
 *     annotation.</li>
 * </ol>
 * In AF5 this is replaced by {@code @Snapshotting(afterEvents = 100)} directly on the entity class.
 * <p>
 * This recipe operates in two phases:
 * <ol>
 *     <li><b>Scan</b> – collects all {@code @Bean} methods that return a {@code SnapshotTriggerDefinition},
 *     recording the bean name and trigger type (event-count, load-time, or custom).</li>
 *     <li><b>Edit</b> – for each {@code @Aggregate}/{@code @EventSourced} class referencing a collected bean:
 *         <ul>
 *             <li>{@code EventCountSnapshotTriggerDefinition} → adds {@code @Snapshotting(afterEvents = N)}</li>
 *             <li>{@code AggregateLoadTimeSnapshotTriggerDefinition} → adds
 *             {@code @Snapshotting(afterSourcingTime = "PTxS")}</li>
 *             <li>Custom implementation → prepends a manual-review note</li>
 *         </ul>
 *         In all cases, the {@code snapshotTriggerDefinition} attribute is removed from the annotation.
 *         Known-type {@code @Bean} methods are deleted from the configuration class; custom ones receive a
 *         manual-review comment and are left in place.</li>
 * </ol>
 *
 * @since 5.1.1
 */
public class MigrateSnapshotTriggerDefinitionToAnnotation
        extends ScanningRecipe<MigrateSnapshotTriggerDefinitionToAnnotation.Accumulator> {

    private static final String SNAPSHOTTING_FQN =
            "org.axonframework.eventsourcing.annotation.Snapshotting";

    private static final String SNAPSHOT_TRIGGER_DEFINITION_FQN =
            "org.axonframework.eventsourcing.SnapshotTriggerDefinition";
    private static final String EVENT_COUNT_FQN =
            "org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition";
    private static final String LOAD_TIME_FQN =
            "org.axonframework.eventsourcing.AggregateLoadTimeSnapshotTriggerDefinition";
    private static final String SNAPSHOTTER_FQN =
            "org.axonframework.eventsourcing.Snapshotter";
    // The eventsourcing recipe renames Snapshotter before the Spring recipe runs, so both FQNs
    // must be tried.
    private static final String SNAPSHOTTER_AF5_FQN =
            "org.axonframework.eventsourcing.snapshot.api.Snapshotter";
    private static final String BEAN_FQN =
            "org.springframework.context.annotation.Bean";

    /** AF4 {@code @Aggregate} FQN before {@code ChangePackage} runs. */
    private static final String AGGREGATE_AF4 =
            "org.axonframework.spring.stereotype.Aggregate";
    /** AF4 {@code @Aggregate} FQN after {@code ChangePackage} (but before {@code ChangeType}). */
    private static final String AGGREGATE_AF4_POST_MOVE =
            "org.axonframework.extension.spring.stereotype.Aggregate";
    /** AF5 {@code @EventSourced} FQN, used as a fallback when {@code ChangeType} ran first. */
    private static final String EVENT_SOURCED_AF5 =
            "org.axonframework.extension.spring.stereotype.EventSourced";

    /** Classification of a scanned {@code SnapshotTriggerDefinition} bean. */
    public enum TriggerType { EVENT_COUNT, LOAD_TIME, CUSTOM }

    /** Metadata collected for a single {@code @Bean SnapshotTriggerDefinition} method. */
    public record SnapshotBeanInfo(TriggerType type, int eventCount, long timeMillis) {}

    /** Cross-file accumulator populated during the scan phase. */
    public static class Accumulator {
        final Map<String, SnapshotBeanInfo> beans = new HashMap<>();
    }

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate SnapshotTriggerDefinition @Bean to @Snapshotting";
    }

    @Override
    public @NonNull String getDescription() {
        return "Replaces AF4 Spring Boot @Bean methods returning SnapshotTriggerDefinition with the"
                + " AF5 @Snapshotting annotation on the corresponding aggregate class."
                + " EventCountSnapshotTriggerDefinition maps to afterEvents;"
                + " AggregateLoadTimeSnapshotTriggerDefinition maps to afterSourcingTime."
                + " Custom implementations leave a TODO comment for manual review.";
    }

    @Override
    public Accumulator getInitialValue(@NonNull ExecutionContext ctx) {
        return new Accumulator();
    }

    // -------------------------------------------------------------------------
    // Phase 1: scan
    // -------------------------------------------------------------------------

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.@NonNull MethodDeclaration visitMethodDeclaration(J.@NonNull MethodDeclaration method,
                                                               @NonNull ExecutionContext ctx) {
                if (hasBeanAnnotation(method) && isSnapshotTriggerDefinitionMethod(method)) {
                    String beanName = extractBeanName(method);
                    acc.beans.put(beanName, detectTriggerType(method));
                }
                return super.visitMethodDeclaration(method, ctx);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Phase 2: edit
    // -------------------------------------------------------------------------

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new JavaIsoVisitor<>() {

            @Override
            public J.@NonNull ClassDeclaration visitClassDeclaration(J.@NonNull ClassDeclaration classDecl,
                                                                     @NonNull ExecutionContext ctx) {
                // Detect before super processes children (annotation args not yet modified).
                String beanName = findSnapshotTriggerBeanName(classDecl.getLeadingAnnotations());

                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

                // ── Handle the aggregate/entity side ──────────────────────────
                if (beanName != null) {
                    if (!hasSnapshottingAnnotation(cd)) {
                        SnapshotBeanInfo info = acc.beans.get(beanName);

                        if (info != null && info.type() == TriggerType.EVENT_COUNT) {
                            // Apply the template FIRST (while getCursor() still matches the cd
                            // returned by super), then remove the attribute from the result.
                            cd = applySnapshottingTemplate(
                                    cd, "@Snapshotting(afterEvents = " + info.eventCount() + ")");
                            // Force import unconditionally — Snapshotting may be unresolved on the
                            // test classpath, so maybeAddImport(onlyIfReferenced=true) would skip it.
                            doAfterVisit(new AddImport<>(SNAPSHOTTING_FQN, null, false));
                        } else if (info != null && info.type() == TriggerType.LOAD_TIME) {
                            String duration = Duration.ofMillis(info.timeMillis()).toString();
                            cd = applySnapshottingTemplate(
                                    cd, "@Snapshotting(afterSourcingTime = \"" + duration + "\")");
                            doAfterVisit(new AddImport<>(SNAPSHOTTING_FQN, null, false));
                        } else {
                            // Custom trigger or bean not found in accumulator.
                            cd = prependTodoOnAggregateAnnotation(cd, beanName);
                        }
                    }
                    // Always remove the snapshotTriggerDefinition attribute — after any template
                    // application so the withLeadingAnnotations change is not overwritten.
                    cd = removeSnapshotTriggerAttribute(cd);
                }

                // ── Handle the configuration class side ───────────────────────
                if (cd == null) {
                    throw new IllegalStateException("Class declaration visitor returned null");
                }
                return processSnapshotBeanMethods(cd);
            }

            private J.ClassDeclaration applySnapshottingTemplate(J.ClassDeclaration cd,
                                                                   @NonNull String template) {
                JavaParser.Builder<?, ?> parser = JavaParser.fromJavaVersion();
                parser.classpath(JavaParser.runtimeClasspath());
                return Objects.requireNonNull(JavaTemplate.builder(template)
                        .imports(SNAPSHOTTING_FQN)
                        .javaParser(parser)
                        .build()
                        .apply(getCursor(),
                               cd.getCoordinates().addAnnotation(
                                   (Comparator<J.Annotation>) (first, second) ->
                                       first.getSimpleName().compareTo(second.getSimpleName()))));
            }

            private J.@NonNull ClassDeclaration processSnapshotBeanMethods(J.@NonNull ClassDeclaration cd) {
                boolean[] removedABean = {false};
                List<Statement> statements = new ArrayList<>();
                for (Statement stmt : cd.getBody().getStatements()) {
                    if (!(stmt instanceof J.MethodDeclaration method)
                            || !hasBeanAnnotation(method)
                            || !isSnapshotTriggerDefinitionMethod(method)) {
                        statements.add(stmt);
                        continue;
                    }
                    String name = extractBeanName(method);
                    SnapshotBeanInfo info = acc.beans.get(name);
                    if (info == null || info.type() == TriggerType.CUSTOM) {
                        statements.add(prependTodoOnBeanMethod(method, name));
                        continue;
                    }
                    // Remove known-type bean method and schedule import cleanup.
                    removedABean[0] = true;
                }
                J.Block body = cd.getBody().withStatements(statements);
                if (removedABean[0]) {
                    maybeRemoveImport(SNAPSHOTTER_FQN);
                    maybeRemoveImport(SNAPSHOTTER_AF5_FQN);
                    maybeRemoveImport(SNAPSHOT_TRIGGER_DEFINITION_FQN);
                    maybeRemoveImport(EVENT_COUNT_FQN);
                    maybeRemoveImport(LOAD_TIME_FQN);
                    // Only remove @Bean import when no @Bean-annotated methods remain — if the
                    // class has other @Bean methods, removing the import would break them.
                    // Note: maybeRemoveImport with onlyIfReferenced=true is unreliable here when
                    // the type isn't resolved (e.g. spring-context not on classpath), so we guard
                    // explicitly.
                    boolean hasBeanMethods = body.getStatements().stream()
                            .filter(s -> s instanceof J.MethodDeclaration)
                            .map(s -> (J.MethodDeclaration) s)
                            .anyMatch(MigrateSnapshotTriggerDefinitionToAnnotation::hasBeanAnnotation);
                    if (!hasBeanMethods) {
                        maybeRemoveImport(BEAN_FQN);
                    }
                }
                return cd.withBody(body);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Scan helpers
    // -------------------------------------------------------------------------

    private static boolean hasBeanAnnotation(J.MethodDeclaration method) {
        for (J.Annotation ann : method.getLeadingAnnotations()) {
            if (TypeUtils.isOfClassType(ann.getType(), BEAN_FQN)) {
                return true;
            }
            if (ann.getAnnotationType() instanceof J.Identifier id
                    && "Bean".equals(id.getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} when the method's declared return type is (or contains)
     * {@code SnapshotTriggerDefinition}, or when a known snapshot-trigger constructor
     * appears in the method body.
     */
    private static boolean isSnapshotTriggerDefinitionMethod(J.MethodDeclaration method) {
        TypeTree returnTypeExpression = method.getReturnTypeExpression();
        if (returnTypeExpression != null) {
            JavaType returnType = returnTypeExpression.getType();
            if (TypeUtils.isOfClassType(returnType, SNAPSHOT_TRIGGER_DEFINITION_FQN)
                    || TypeUtils.isOfClassType(returnType, EVENT_COUNT_FQN)
                    || TypeUtils.isOfClassType(returnType, LOAD_TIME_FQN)) {
                return true;
            }
            // Simple-name fallback for unresolved types.
            if (returnTypeExpression instanceof J.Identifier id
                    && id.getSimpleName().endsWith("SnapshotTriggerDefinition")) {
                return true;
            }
        }
        // Body check: look for a new-class or Kotlin constructor call returning a known type.
        J.Block body = method.getBody();
        if (body != null) {
            for (Statement stmt : body.getStatements()) {
                Expression returnExpr = null;
                if (stmt instanceof J.Return ret) {
                    returnExpr = ret.getExpression();
                } else if (stmt instanceof K.Return kRet) {
                    returnExpr = kRet.getExpression().getExpression();
                }
                if (returnExpr == null) {
                    continue;
                }
                if (returnExpr instanceof J.NewClass nc && isSnapshotNewClass(nc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSnapshotNewClass(J.NewClass nc) {
        TypeTree clazz = nc.getClazz();
        if (clazz == null) {
            return false;
        }
        JavaType type = clazz.getType();
        if (TypeUtils.isOfClassType(type, EVENT_COUNT_FQN)
                || TypeUtils.isOfClassType(type, LOAD_TIME_FQN)
                || TypeUtils.isOfClassType(type, SNAPSHOT_TRIGGER_DEFINITION_FQN)) {
            return true;
        }
        // Simple-name fallback.
        if (clazz instanceof J.Identifier id
                && id.getSimpleName().endsWith("SnapshotTriggerDefinition")) {
            return true;
        }
        return false;
    }

    /** Extracts the Spring bean name: explicit {@code @Bean("x")} / {@code name = "x"} or method name. */
    private static String extractBeanName(J.MethodDeclaration method) {
        for (J.Annotation ann : method.getLeadingAnnotations()) {
            if (!TypeUtils.isOfClassType(ann.getType(), BEAN_FQN)
                    && !(ann.getAnnotationType() instanceof J.Identifier id
                                 && "Bean".equals(id.getSimpleName()))) {
                continue;
            }
            List<Expression> arguments = ann.getArguments();
            if (arguments == null || arguments.isEmpty()) {
                break;
            }
            J firstArg = arguments.get(0);
            // @Bean("name")
            if (firstArg instanceof J.Literal lit && lit.getValue() instanceof String name) {
                return name;
            }
            // @Bean(name = "name")
            if (firstArg instanceof J.Assignment assign
                    && assign.getVariable() instanceof J.Identifier id
                    && "name".equals(id.getSimpleName())
                    && assign.getAssignment() instanceof J.Literal lit
                    && lit.getValue() instanceof String name) {
                return name;
            }
        }
        return method.getSimpleName();
    }

    private static SnapshotBeanInfo detectTriggerType(J.MethodDeclaration method) {
        J.Block body = method.getBody();
        if (body == null) {
            return new SnapshotBeanInfo(TriggerType.CUSTOM, 0, 0);
        }
        for (Statement stmt : body.getStatements()) {
            Expression returnExpr = null;
            if (stmt instanceof J.Return ret) {
                returnExpr = ret.getExpression();
            } else if (stmt instanceof K.Return kRet) {
                // Kotlin: K.Return wraps J.Return; the actual expression is one level deeper
                returnExpr = kRet.getExpression().getExpression();
            }
            if (returnExpr == null) {
                continue;
            }
            // Java: new EventCountSnapshotTriggerDefinition(snapshotter, N)
            if (returnExpr instanceof J.NewClass nc) {
                if (isOfNewClassType(nc, "EventCountSnapshotTriggerDefinition", EVENT_COUNT_FQN)) {
                    if (nc.getArguments() != null && nc.getArguments().size() >= 2) {
                        J secondArg = nc.getArguments().get(1);
                        if (secondArg instanceof J.Literal lit && lit.getValue() instanceof Number n) {
                            return new SnapshotBeanInfo(TriggerType.EVENT_COUNT, n.intValue(), 0);
                        }
                    }
                    return new SnapshotBeanInfo(TriggerType.CUSTOM, 0, 0);
                }
                if (isOfNewClassType(nc, "AggregateLoadTimeSnapshotTriggerDefinition", LOAD_TIME_FQN)) {
                    if (nc.getArguments() != null && nc.getArguments().size() >= 2) {
                        J secondArg = nc.getArguments().get(1);
                        if (secondArg instanceof J.Literal lit && lit.getValue() instanceof Number n) {
                            return new SnapshotBeanInfo(TriggerType.LOAD_TIME, 0, n.longValue());
                        }
                    }
                    return new SnapshotBeanInfo(TriggerType.CUSTOM, 0, 0);
                }
            }
        }
        return new SnapshotBeanInfo(TriggerType.CUSTOM, 0, 0);
    }

    private static boolean isOfNewClassType(J.NewClass nc, String simpleName, String fqn) {
        J clazz = nc.getClazz();
        if (clazz == null) {
            return false;
        }
        if (fqn != null && TypeUtils.isOfClassType(nc.getType(), fqn)) {
            return true;
        }
        return clazz instanceof J.Identifier id && simpleName.equals(id.getSimpleName());
    }

    // -------------------------------------------------------------------------
    // Edit helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the value of the {@code snapshotTriggerDefinition} attribute on the first
     * {@code @Aggregate} or {@code @EventSourced} annotation found, or {@code null} if absent.
     */
    @Nullable
    private static String findSnapshotTriggerBeanName(List<J.Annotation> annotations) {
        for (J.Annotation ann : annotations) {
            List<Expression> arguments = ann.getArguments();
            if (!isAggregateOrEventSourced(ann) || arguments == null) {
                continue;
            }
            for (Expression arg : arguments) {
                if (arg instanceof J.Assignment assign
                        && assign.getVariable() instanceof J.Identifier id
                        && "snapshotTriggerDefinition".equals(id.getSimpleName())
                        && assign.getAssignment() instanceof J.Literal lit
                        && lit.getValue() instanceof String name) {
                    return name;
                }
            }
        }
        return null;
    }

    private static boolean isAggregateOrEventSourced(J.Annotation ann) {
        if (TypeUtils.isOfClassType(ann.getType(), AGGREGATE_AF4)
                || TypeUtils.isOfClassType(ann.getType(), AGGREGATE_AF4_POST_MOVE)
                || TypeUtils.isOfClassType(ann.getType(), EVENT_SOURCED_AF5)) {
            return true;
        }
        if (ann.getAnnotationType() instanceof J.Identifier id) {
            String n = id.getSimpleName();
            return "Aggregate".equals(n) || "EventSourced".equals(n);
        }
        return false;
    }

    private static boolean hasSnapshottingAnnotation(J.ClassDeclaration cd) {
        for (J.Annotation ann : cd.getLeadingAnnotations()) {
            if (TypeUtils.isOfClassType(ann.getType(), SNAPSHOTTING_FQN)) {
                return true;
            }
            if (ann.getAnnotationType() instanceof J.Identifier id
                    && "Snapshotting".equals(id.getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    /** Removes the {@code snapshotTriggerDefinition = "..."} argument from the {@code @Aggregate} annotation. */
    private static J.ClassDeclaration removeSnapshotTriggerAttribute(J.ClassDeclaration cd) {
        List<J.Annotation> leadingAnnotations = cd.getLeadingAnnotations();
        if (leadingAnnotations == null || leadingAnnotations.isEmpty()) {
            return cd;
        }
        List<J.Annotation> updatedAnnotations = new ArrayList<>(leadingAnnotations.size());
        for (J.Annotation ann : leadingAnnotations) {
            List<Expression> arguments = ann.getArguments();
            if (isAggregateOrEventSourced(ann) && arguments != null) {
                ann = ann.withArguments(
                        ListUtils.map(arguments, arg -> {
                            if (arg instanceof J.Assignment assign
                                    && assign.getVariable() instanceof J.Identifier id
                                    && "snapshotTriggerDefinition".equals(id.getSimpleName())) {
                                return null;
                            }
                            return arg;
                        })
                );
                List<Expression> remainingArguments = ann.getArguments();
                if (remainingArguments == null || remainingArguments.isEmpty()) {
                    ann = ann.withArguments(null);
                } else {
                    // Strip leading whitespace from the first surviving argument. When the removed
                    // attribute was not the last in the list (e.g. "snapshotTriggerDefinition = x,
                    // repository = y"), the next argument keeps its " " prefix that came after the
                    // comma — producing @Aggregate( repository = "y") instead of @Aggregate(repository = "y").
                    boolean[] first = {true};
                    ann = ann.withArguments(
                            ListUtils.map(remainingArguments, arg -> {
                                if (first[0]) {
                                    first[0] = false;
                                    if (!arg.getPrefix().getWhitespace().isEmpty()) {
                                        return (Expression) ((J) arg)
                                                .withPrefix(arg.getPrefix().withWhitespace(""));
                                    }
                                }
                                return arg;
                            })
                    );
                }
            }
            updatedAnnotations.add(ann);
        }
        return cd.withLeadingAnnotations(updatedAnnotations);
    }

    /**
    * Prepends a migration note as a line comment above the {@code @Aggregate}/{@code @EventSourced}
    * annotation on the class for the custom-trigger case.
     */
    private static J.ClassDeclaration prependTodoOnAggregateAnnotation(J.ClassDeclaration cd,
                                                                         String beanName) {
        List<J.Annotation> leadingAnnotations = cd.getLeadingAnnotations();
        if (leadingAnnotations == null || leadingAnnotations.isEmpty()) {
            return cd;
        }
        List<J.Annotation> updatedAnnotations = new ArrayList<>(leadingAnnotations.size());
        boolean done = false;
        for (J.Annotation ann : leadingAnnotations) {
            if (!done && isAggregateOrEventSourced(ann)) {
                done = true;
                updatedAnnotations.add(prependLineComment(ann,
                        " TODO(axon4to5): Custom SnapshotTriggerDefinition \""
                                + beanName
                                + "\" cannot be migrated automatically."));
            } else {
                updatedAnnotations.add(ann);
            }
        }
        return cd.withLeadingAnnotations(updatedAnnotations);
    }

    /**
    * Prepends a line comment above the bean method for custom trigger types.
     */
    private static J.MethodDeclaration prependTodoOnBeanMethod(J.MethodDeclaration method,
                                                                 String beanName) {
        // Idempotency: if the comment is already present, skip (second recipe cycle would re-add it).
        if (method.getPrefix().getComments().stream()
                .anyMatch(c -> c instanceof TextComment tc && tc.getText().contains("TODO(axon4to5)"))) {
            return method;
        }
        Space prefix = method.getPrefix();
        String leading = prefix.getWhitespace();
        String indent = leading.contains("\n")
                ? leading.substring(leading.lastIndexOf('\n') + 1)
                : "";
        String suffix = "\n" + indent;
        TextComment todo = new TextComment(false,
                " TODO(axon4to5): Custom SnapshotTriggerDefinition bean \""
                        + beanName
                        + "\" — remove this bean manually.",
                suffix, Markers.EMPTY);
        return method.withPrefix(prefix.withComments(ListUtils.concat(prefix.getComments(), todo)));
    }

    /**
     * Prepends a {@code //}-style line comment to {@code ann}, placed on its own line above the
     * annotation at the same indent level.
     */
    private static J.Annotation prependLineComment(J.Annotation ann, @NonNull String text) {
        Space prefix = ann.getPrefix();
        String leading = prefix.getWhitespace();
        String indent = leading.contains("\n")
                ? leading.substring(leading.lastIndexOf('\n') + 1)
                : "";
        String suffix = "\n" + indent;
        TextComment todo = new TextComment(false, text, suffix, Markers.EMPTY);
        return ann.withPrefix(prefix.withComments(ListUtils.concat(prefix.getComments(), todo)));
    }
}
