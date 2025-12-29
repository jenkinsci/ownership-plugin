---
name: Architectural Review Plan
overview: Comprehensive architectural review of the Jenkins Ownership Plugin covering Jenkins best practices compliance, CRUD principles, and DRY violations with specific recommendations for improvements.
todos: []
---

# Architectural Review of Jenkins Ownership Plugin

## 1. Compliance with Jenkins Plugin Development Best Practices

### 1.1 Use of Deprecated APIs

**Issue:** Use of `Jenkins.getActiveInstance()` (deprecated since Jenkins 2.0)

**Files with the issue:**

- `src/main/java/com/synopsys/arc/jenkins/plugins/ownership/OwnershipPlugin.java` (lines 106, 175, 181, 200)

- `src/main/java/com/synopsys/arc/jenkins/plugins/ownership/extensions/ItemOwnershipPolicy.java` (lines 85, 95)

- `src/main/java/org/jenkinsci/plugins/ownership/model/OwnershipHelperLocator.java` (line 56)

- `src/main/java/com/synopsys/arc/jenkins/plugins/ownership/nodes/OwnerNodeProperty.java` (line 85)

- And 6 more files

**Solution:** Replace with `Jenkins.getInstance()` or `Jenkins.get()` (Jenkins 2.0+)

### 1.2 Package Structure

**Issue:** Mixed package structure:

- `com.synopsys.arc.jenkins.plugins.ownership.*` (old code)

- `org.jenkinsci.plugins.ownership.*` (new code)

**Recommendation:** According to [Jenkins Plugin Best Practices](https://www.jenkins.io/doc/developer/publishing/), it is recommended to use `org.jenkinsci.plugins.*` for all new components. Old code in `com.synopsys.arc.*` can be kept for backward compatibility, but new classes should be in `org.jenkinsci.plugins.*`.

### 1.3 Documentation

**Current state:**

- ✅ README.md exists

- ✅ CONTRIBUTING.md exists

- ✅ CHANGELOG.md exists

- ✅ Documentation exists in `doc/`

**Recommendation:** Add Javadoc for all public APIs according to [Jenkins Documentation Guidelines](https://www.jenkins.io/doc/developer/publishing/plugin-documentation/).

### 1.4 Extension Points

**Good:** Correct use of `@Extension` and `@Extension(optional = true)` for integrations with other plugins.

**Issue:** In `ItemOwnershipPolicy.java` (line 94) there is a TODO comment about migrating to `ExtensionList.lookup()`.

## 2. DRY Principle Violations (Don't Repeat Yourself)

### 2.1 Duplication of Ownership Setting Logic

**Issue:** The `setOwnership()` method is duplicated in three classes with nearly identical logic:

```java
// JobOwnerHelper.java (lines 151-160)
public static void setOwnership(@Nonnull Job<?, ?> job, 
        @CheckForNull OwnershipDescription descr) throws IOException {
    JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(job);
    if (prop == null) {
        prop = new JobOwnerJobProperty(descr, null);
        job.addProperty(prop);
    } else {
        prop.setOwnershipDescription(descr);
    }
}

// FolderOwnershipHelper.java (lines 159-168) - similar
// NodeOwnerHelper.java (lines 120-129) - similar
```



**Solution:** Extract common logic to `AbstractOwnershipHelper` or create a utility class.

### 2.2 Duplication of Ownership Inheritance Logic

**Issue:** The logic for traversing parent elements for ownership inheritance is duplicated in:

- `JobOwnerHelper.getOwnershipInfo()` (lines 122-139)

- `FolderOwnershipHelper.getOwnershipInfo()` (lines 122-138)

**Solution:** Extract to a common method in `AbstractOwnershipHelper` or create a separate class `OwnershipInheritanceResolver`.

### 2.3 Duplication of Getting Possible Owners Logic

**Issue:** The `getPossibleOwners()` method has similar logic in:

- `JobOwnerHelper` (lines 179-186)

- `FolderOwnershipHelper` (lines 144-151)

- `NodeOwnerHelper` (lines 105-112)

- `NodeOwnerPropertyHelper` (lines 87-94)

**Solution:** Extract to a base class with parameterization by permission type (CONFIGURE for different element types).

### 2.4 Duplication of Singleton Patterns

**Issue:** Different approaches to singleton:

- `JobOwnerHelper.Instance` (public field)

- `FolderOwnershipHelper.INSTANCE` + `getInstance()` (method)

- `NodeOwnerHelper.Instance` (public field)

**Solution:** Unify the approach - use `getInstance()` everywhere.

## 3. CRUD Principle Violations

### 3.1 Create Operations

**Good:** There are methods for creating ownership via `setOwnership()`.

**Issue:** No unified interface for creation. Each helper has its own method.

### 3.2 Read Operations

**Good:** There are methods `getOwnershipDescription()`, `getOwnershipInfo()`.

**Issue:** `NodeOwnerHelper.getOwnershipInfo()` (line 90) returns `DISABLED_DESCR` instead of the actual ownership from the property - this is a bug.

### 3.3 Update Operations

**Good:** There are methods `setOwnershipDescription()` in properties.

**Issue:** Update logic is duplicated (see DRY section 2.1).

### 3.4 Delete Operations

**Issue:** No explicit methods for removing ownership. Removal happens by setting `null`, but there is no unified interface.

**Recommendation:** Add `removeOwnership()` method to `AbstractOwnershipHelper`.

## 4. Additional Architectural Issues

### 4.1 TODO Comments

Found many TODO comments indicating technical debt:

- `ItemOwnershipPolicy.java:94` - migrate to ExtensionList.lookup()

- `NodeOwnerHelper.java:52` - add reference to bug

- `OwnershipPlugin.java:96` - graceful error handling

- `OwnershipDescriptionHelper.java:33` - question about necessity of the class

### 4.2 Deprecated Methods

29 occurrences of `@Deprecated` methods that are still used in the code. Recommended:

1. Complete migration from deprecated methods

2. Remove deprecated methods after full migration

3. Update documentation

### 4.3 Error Handling

**Issue:** In `OwnershipPlugin.resolveEmail()` (lines 266-270) there is a TODO about error logging, but errors are not logged.

### 4.4 Naming Inconsistencies

- `Instance` vs `INSTANCE` vs `getInstance()`

- `coownersIds` (old name) vs `secondaryOwners` (new name in documentation)

## 5. Improvement Recommendations

### Priority 1 (Critical)

1. Replace all `Jenkins.getActiveInstance()` with `Jenkins.getInstance()`

2. Fix bug in `NodeOwnerHelper.getOwnershipInfo()` (line 90)

3. Extract duplicated `setOwnership()` logic to a base class

### Priority 2 (Important)

4. Extract ownership inheritance logic to a common class

5. Unify singleton patterns

6. Add `removeOwnership()` method for explicit removal

### Priority 3 (Desirable)

7. Add Javadoc for all public APIs

8. Resolve all TODO comments or create issues

9. Improve error handling with logging

10. Unify naming (coowners → secondaryOwners)

## 6. Code Quality Metrics

- **Test Coverage:** Tests exist in `src/test/java/` (20 test classes)

- **Project Size:** ~80 Java files, ~48 Jelly files

- **Dependencies:** Correctly marked as `optional=true` for integrations

- **Structure:** Good modularity with separation into jobs, nodes, folders, security

## 7. Compliance with Jenkins Plugin Publishing Guidelines

✅ **Compliant:**

- Correct Maven project structure

- Use of Jenkins BOM

- Correct SCM configuration in pom.xml

- Jenkinsfile for CI/CD

- License specified (MIT)

⚠️ **Requires Attention:**

- Plugin version in pom.xml: `0.13.1-SNAPSHOT` - need to remove `-SNAPSHOT` for release