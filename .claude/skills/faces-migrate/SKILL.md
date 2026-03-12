---
name: faces-migrate
description: Migrate a Jakarta Faces project from one version to another (e.g. JSF 2.3 to Faces 4.1)
disable-model-invocation: true
argument-hint: "[target version, e.g. 4.1]"
allowed-tools: Read, Edit, Glob, Grep, Bash, Agent
---

*Version 1.1.0*

Migrate the current project to Jakarta Faces `$ARGUMENTS` (if no argument, ask the developer for the target version).

## Step 1: Detect Current Version

Determine the current Faces version from:
- `pom.xml` or `build.gradle` dependencies (look for `javax.faces`, `jakarta.faces`, `jsf-api`, `myfaces-api`).
- `faces-config.xml` version attribute.
- XML namespaces in XHTML files (`java.sun.com` = JSF 1.0+, `xmlns.jcp.org` = JSF 2.2+, `jakarta.faces` = Faces 4.0+).
- Package imports in Java files (`javax.faces` = JSF 1.x/2.x vs `jakarta.faces` = Faces 3.0+).

Also determine the **runtime type**:
- **Full EE server** (WildFly, GlassFish, TomEE, Payara, WebSphere, Liberty, etc.): Faces, CDI, BV, EJB, JPA, JAX-RS, etc. are all provided by the runtime; the project should depend on the Java EE / Jakarta EE API artifact with `<scope>provided</scope>`, not on standalone implementations.
- **Barebones servlet container** (Tomcat, Jetty, Undertow, etc.) or **framework** (Spring Boot): Tomcat only provides Servlet, JSP, EL, WebSocket, and JASIC; everything else (Faces, CDI, BV, JSTL, etc.) must be installed/bundled separately with the application.

When updating dependencies in migration steps, update the appropriate artifact:
- On a full EE server: update the EE API version and ensure the target server version supports the target Faces version.
  - Java EE 8 (JSF 2.3): `javax:javaee-web-api:8.0` with `<scope>provided</scope>`
  - Jakarta EE 9 (Faces 3.0): `jakarta.platform:jakarta.jakartaee-web-api:9.0.0` with `<scope>provided</scope>`
  - Jakarta EE 10 (Faces 4.0): `jakarta.platform:jakarta.jakartaee-web-api:10.0.0` with `<scope>provided</scope>`
  - Jakarta EE 11 (Faces 4.1): `jakarta.platform:jakarta.jakartaee-web-api:11.0.0` with `<scope>provided</scope>`
- On a barebones servlet container: update the standalone Faces implementation version directly (e.g. `org.glassfish:jakarta.faces` for Mojarra, `org.apache.myfaces.core:myfaces-impl` for MyFaces); do NOT use `javaee-api` or `jakarta.jakartaee-api` as a substitute because it will allow compiling against APIs that the servlet container doesn't actually provide.

Report the detected version and runtime type, and confirm with the developer before proceeding.

## Step 2: Determine Migration Path

Based on source → target version, apply the relevant steps below.
Multiple steps may apply for multi-version jumps (e.g. JSF 2.0 → Faces 4.1 requires all intermediate steps).

## Migration Steps

### JSF 1.x → JSF 2.0+ (major rewrite)

This is a significant migration; confirm scope with developer before proceeding.

- Replace JSP files with Facelets (XHTML).
- Replace `<f:view>`, `<f:subview>` with Facelets templating (`<ui:composition>`, `<ui:define>`).
- Replace `<managed-bean>` entries in `faces-config.xml` with `@ManagedBean` + scope annotations (will be migrated to `@Named` in later step).
- Replace `<navigation-rule>` entries in `faces-config.xml` with return values from action methods or `?faces-redirect=true`.
- Replace `javax.faces.webapp.FacesServlet` URL pattern `/faces/*` or `*.faces` or `*.jsf` with `*.xhtml`.

### JSF 2.x → JSF 2.3 (incremental)

- Update `pom.xml` to JSF 2.3 dependency.
- Replace `@ManagedBean` + `@javax.faces.bean.*Scoped` with `@Named` + `@javax.enterprise.context.*Scoped`.
- `@ViewScoped`: replace `javax.faces.bean.ViewScoped` with `javax.faces.view.ViewScoped`.
- Ensure `beans.xml` exists in `WEB-INF/` with `bean-discovery-mode="all"` or `"annotated"`.
- Replace `@javax.faces.bean.ManagedProperty` on managed beans with `@Inject`.
- Replace `@javax.faces.bean.ManagedProperty` on unmanaged variables with `@javax.faces.annotation.ManagedProperty`.
- FacesServlet: ensure URL pattern is `*.xhtml`; remove legacy `*.jsf`, `*.faces`, `/faces/*` mappings.
- Update `faces-config.xml` version to `2.3`.
  ```xml
  <faces-config 
      xmlns="https://xmlns.jcp.org/xml/ns/javaee"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://xmlns.jcp.org/xml/ns/javaee https://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_3.xsd"
      version="2.3"
  >
  ```
- Update `*.taglib.xml` version to `2.3`.
  ```xml
  <facelet-taglib 
      xmlns="https://xmlns.jcp.org/xml/ns/javaee"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://xmlns.jcp.org/xml/ns/javaee https://xmlns.jcp.org/xml/ns/javaee/web-facelettaglibrary_2_3.xsd"
      version="2.3"
  >
  ```
- Update XML namespaces from `java.sun.com` to `xmlns.jcp.org` (if on JSF 2.2+).
- Use HTML5 doctype `<!DOCTYPE html>` (if on JSF 2.2+).
- Review for new 2.3 features to adopt: https://arjan-tijms.omnifaces.org/p/jsf-23.html

### JSF 2.3 → Faces 3.0 (Jakarta EE 9, namespace rename)

This is purely a package rename; no behavioral changes.

- Update `pom.xml` to Jakarta Faces 3.0 dependency.
- Rename ALL `javax.faces.*` imports to `jakarta.faces.*` in Java files.
- Rename ALL `javax.servlet.*` to `jakarta.servlet.*`.
- Rename ALL `javax.inject.*` to `jakarta.inject.*`.
- Rename ALL `javax.enterprise.*` to `jakarta.enterprise.*`.
- Rename ALL `javax.annotation.*` to `jakarta.annotation.*`.
- Rename ALL `javax.validation.*` to `jakarta.validation.*`.
- Rename ALL `javax.persistence.*` to `jakarta.persistence.*` (if JPA is used).
- Rename ALL `javax.ejb.*` to `jakarta.ejb.*` (if EJB is used).
- Rename ALL `javax.ws.rs.*` to `jakarta.ws.rs.*` (if JAX-RS is used).
- Update `web.xml` context param names from `javax.faces.*` to `jakarta.faces.*`.
- FacesServlet: ensure URL pattern is `*.xhtml`; remove legacy `*.jsf`, `*.faces`, `/faces/*` mappings.
- Update `faces-config.xml` namespace and version to `3.0`.
  ```xml
  <faces-config 
      xmlns="https://jakarta.ee/xml/ns/jakartaee"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_3_0.xsd"
      version="3.0"
  >
  ```
- Update `*.taglib.xml` namespace and version to `3.0`.
  ```xml
  <facelet-taglib 
      xmlns="https://jakarta.ee/xml/ns/jakartaee"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_3_0.xsd"
      version="3.0"
  >
  ```
- NOTE: XHTML namespaces stay the same (`xmlns.jcp.org`) in Faces 3.0; they only change in 4.0.
- Use HTML5 doctype `<!DOCTYPE html>` (if not already).

### Faces 3.0 → Faces 4.0+ (Jakarta EE 10+, spec overhaul)

- Update `pom.xml` to Jakarta Faces 4.0 dependency.
- Update XHTML namespaces from `xmlns.jcp.org` to `jakarta.faces` URNs:
  - `http://xmlns.jcp.org/jsf/html` → `jakarta.faces.html`
  - `http://xmlns.jcp.org/jsf/core` → `jakarta.faces.core`
  - `http://xmlns.jcp.org/jsf/facelets` → `jakarta.faces.facelets`
  - `http://xmlns.jcp.org/jsf/composite` → `jakarta.faces.composite`
  - `http://xmlns.jcp.org/jsf/passthrough` → `jakarta.faces.passthrough`
  - `http://xmlns.jcp.org/jsf` → `jakarta.faces`
  - `http://xmlns.jcp.org/jsp/jstl/core` → `jakarta.tags.core`
- Remove `xmlns="http://www.w3.org/1999/xhtml"` from XHTML files; since Faces 4.0 it is always implied.
- Remove any remaining `@ManagedBean` usage (removed in Faces 4.0); replace with `@Named` + CDI scope.
- Remove any `javax.faces.bean.*` imports (package removed entirely).
- FacesServlet: ensure URL pattern is `*.xhtml`; remove legacy `*.jsf`, `*.faces`, `/faces/*` mappings.
- Update `faces-config.xml` version to `4.0`.
  ```xml
  <faces-config 
      xmlns="https://jakarta.ee/xml/ns/jakartaee"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_0.xsd"
      version="4.0"
  >
  ```
- Update `*.taglib.xml` version to `4.0`.
  ```xml
  <facelet-taglib 
      xmlns="https://jakarta.ee/xml/ns/jakartaee"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_0.xsd"
      version="4.0"
  >
  ```
- Use HTML5 doctype `<!DOCTYPE html>` (if not already).
- Review for new 4.0 features to adopt: https://balusc.omnifaces.org/2021/11/whats-new-in-faces-40.html

### Faces 4.0 → Faces 4.1 (Jakarta EE 11, incremental)

- Update `pom.xml` to Jakarta Faces 4.1 dependency.
- Update `faces-config.xml` version to `4.1`.
  ```xml
  <faces-config 
      xmlns="https://jakarta.ee/xml/ns/jakartaee"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_1.xsd"
      version="4.1"
  >
  ```
- Update `*.taglib.xml` version to `4.1`.
  ```xml
  <facelet-taglib 
      xmlns="https://jakarta.ee/xml/ns/jakartaee"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_1.xsd"
      version="4.1"
  >
  ```
- Replace any Full State Saving (`FULL_STATE_SAVING_VIEW_IDS`) usage; FSS is deprecated and will be removed in Faces 6.0.
- Review for new 4.1 features to adopt: https://balusc.omnifaces.org/2024/06/whats-new-in-faces-41.html

## Step 3: Execute

For each applicable migration step:
1. Show the developer what will change (file count, type of changes).
2. Confirm before proceeding.
3. Apply changes file by file.
4. After all changes, report a summary.

## Step 4: Verify

After migration:
- Check for any remaining old-version references (grep for old package names, old namespaces, old annotations).
- Verify `pom.xml`/`build.gradle` has no conflicting dependency versions.
- Suggest running `/faces-review` to catch any remaining issues.

## Important

- ALWAYS confirm with the developer before making changes.
- For large projects, offer to do a dry run first (report only, no changes).
- When migrating across multiple versions (e.g. JSF 2.0 → Faces 4.1), apply steps in order; do NOT skip intermediate steps.
- Preserve existing code style (indentation, line endings, import ordering).
- Third-party libraries (PrimeFaces, OmniFaces, etc.) may also need version upgrades; flag these but let the developer decide.
