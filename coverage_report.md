# Repository Code Coverage Report

**Generated on:** 2026-11-14  
**Analysis Type:** JaCoCo Test Coverage Analysis

---

## Overall Summary

| Metric | Value | Percentage |
|--------|-------|------------|
| Total Modules with Coverage | 6 | - |
| Total Classes Covered | 407/907 | 44.9% |
| Total Methods Covered | 1944/5937 | 32.7% |
| Uncovered Key Methods | 3432 | - |

---

## Module Breakdown

### 📦 COMMON
| Metric | Count | Percentage |
|--------|-------|------------|
| Classes Covered | 2/137 | 1.5% |
| Methods Covered | 2/828 | 0.2% |
| Uncovered Key Methods | 687 | - |

### 📦 CONVERSION
| Metric | Count | Percentage |
|--------|-------|------------|
| Classes Covered | 34/35 | 97.1% |
| Methods Covered | 144/180 | 80.0% |
| Uncovered Key Methods | 36 | - |

### 📦 MESSAGING
| Metric | Count | Percentage |
|--------|-------|------------|
| Classes Covered | 307/512 | 60.0% |
| Methods Covered | 1549/3571 | 43.4% |
| Uncovered Key Methods | 1774 | - |

### 📦 MODELLING
| Metric | Count | Percentage |
|--------|-------|------------|
| Classes Covered | 3/87 | 3.4% |
| Methods Covered | 10/457 | 2.2% |
| Uncovered Key Methods | 364 | - |

### 📦 EVENTSOURCING
| Metric | Count | Percentage |
|--------|-------|------------|
| Classes Covered | 40/115 | 34.8% |
| Methods Covered | 133/789 | 16.9% |
| Uncovered Key Methods | 567 | - |

### 📦 UPDATE
| Metric | Count | Percentage |
|--------|-------|------------|
| Classes Covered | 21/21 | 100.0% |
| Methods Covered | 106/112 | 94.6% |
| Uncovered Key Methods | 4 | - |

---

## Files Without Test Coverage (Common Module)

The following source files in the `common` module have no corresponding test files:

```
AbstractMethodPropertyAccessStrategy.java
AbstractPriorityTask.java
ApplicationConfigurer.java
AxonConfiguration.java
Component.java
ComponentDefinition.java
ComponentDescriptor.java
ComponentFactory.java
ComponentLifecycleHandler.java
ComponentRegistry.java
ConfigurableComponentRegistry.java
Configuration.java
ConfigurationExtension.java
ConnectionProvider.java
DeadlockException.java
DirectlyAccessedProperty.java
DirectPropertyAccessStrategy.java
EntityManagerProvider.java
ExecutorServiceFactory.java
ExtendedConfiguration.java
ExtensibleConfigurer.java
LifecycleHandlerInvocationException.java
LifecycleRegistry.java
Lock.java
LockAcquisitionFailedException.java
LockFactory.java
MethodAccessedProperty.java
Module.java
Nullability.java
NullabilityResolver.java
NullLockFactory.java
OverridePolicy.java
PersistenceExceptionResolver.java
Priority.java
PriorityCallable.java
PriorityRunnable.java
Property.java
PropertyAccessException.java
SearchScope.java
ShutdownInProgressException.java
TransactionalExecutor.java
UniformPropertyAccessStrategy.java
```

---

## High-Priority Uncovered Classes

### Common Module (Critical Gap)
- **Assert.java** - 11 uncovered methods
- **BuilderUtils.java** - 14 uncovered methods
- **ClassUtils.java** - 3 uncovered methods
- **ClockUtils.java** - 5 uncovered methods
- **CollectionUtils.java** - 4 uncovered methods
- **ConstructorUtils.java** - 10 uncovered methods
- **DateTimeUtils.java** - 3 uncovered methods
- **DefaultIdentifierFactory.java** - 2 uncovered methods
- **DirectExecutor.java** - 3 uncovered methods
- **ExceptionUtils.java** - 4 uncovered methods
- **FutureUtils.java** - 11 uncovered methods
- **IdentifierFactory.java** - 4 uncovered methods
- **IdentifierValidator.java** - 4 uncovered methods
- **ListUtils.java** - 1 uncovered method
- **MemberAccessibilityCallback.java** - 2 uncovered methods
- **ObjectUtils.java** - 9 uncovered methods
- **ProcessRetriesExhaustedException.java** - 2 uncovered methods
- **ProcessUtils.java** - 6 uncovered methods
- **ReflectionUtils.java** - 24 uncovered methods
- **TypeReference.java** - 6 uncovered methods
- **TypeReflectionUtils.java** - 6 uncovered methods

### Messaging Module (Largest Gap)
- **536 main source files, only 62 test files**
- **307 classes covered out of 512**
- **43.4% method coverage**
- **1774 uncovered key methods**

### Modelling Module (Critical Gap)
- **96 main source files, only 3 test files**
- **3 classes covered out of 87**
- **2.2% method coverage**
- **364 uncovered key methods**

---

## Recommendations

### Immediate Priority
1. **common module** - Most critical gap (only 1.5% class coverage)
2. **modelling module** - Second most critical (3.4% class coverage)
3. **eventsourcing module** - Moderate gap (34.8% class coverage)

### Well Covered
1. **conversion module** - 97.1% class coverage ✅
2. **update module** - 100% class coverage ✅

---

*Report generated from JaCoCo XML reports across 6 modules*
