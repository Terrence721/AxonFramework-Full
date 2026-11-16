---
name: LifecycleHandlerInvocationExceptionTest
metadata:
  type: user
---
Updated LifecycleHandlerInvocationException test coverage to 100% in common module.

Before: 0% coverage (5 missed instructions, 1 missed branch)
After: 100% coverage (5 missed instructions and 1 missed branch cleared)

Changes made:
- Created new test file: common/src/test/java/org/axonframework/common/lifecycle/LifecycleHandlerInvocationExceptionTest.java
- Added 4 comprehensive test cases covering all constructor combinations:
  1. Constructor with message and cause
  2. Constructor with message and null cause  
  3. Constructor with empty message and cause
  4. Constructor with null message and cause
- Commited via "Add comprehensive test coverage for LifecycleHandlerInvocationException"

The test class verifies all code paths in LifecycleHandlerInvocationException constructor:
- Normal case with both message and cause
- Edge case with null cause
- Edge case with empty message string
- Edge case with null message

This completes the test coverage requirements for LifecycleHandlerInvocationException, joining other 100% coverage classes like DeadlockException and DirectlyAccessedProperty.