---
name: faces-review
description: Review Jakarta Faces code for common mistakes, anti-patterns, and rule violations
disable-model-invocation: true
argument-hint: "[file or directory, defaults to current project]"
allowed-tools: Read, Glob, Grep, Agent
---

*Version 1.1.0*

Review the Jakarta Faces code in `$ARGUMENTS` (if no argument, scan the project for `.xhtml` and backing bean files) against the rules in `.claude/faces/rules.md` and its topic files.

## Review Checklist

### XHTML / Facelets
- XML namespaces match the project's Faces version.
- HTML5 doctype `<!DOCTYPE html>` is used, not XHTML doctype.
- No "god form" (single form wrapping entire page); forms are scoped to logical sections.
- No nested `UIForm` components.
- `UIInput` and `UICommand` components are inside `UIForm`.
- Every `UIInput` has a corresponding `UIMessage`.
- A catch-all `UIMessages` with `redisplay="false"` exists in each view; when using ajax, its ID is covered by `render`/`update`.
- Conditionally rendered components that are ajax-updated are wrapped in an always-rendered container, and ajax updates target the wrapper ID.
- `NamingContainer`, `UIInput` and `UICommand` components have explicit IDs (no generated IDs); IDs follow naming convention (property name for `value`, method name for `action`, view ID name for forms/panels).
- `binding` is not used for data binding; `binding` is only used on page-scoped variables for component cross-referencing within the same view.
- Ajax `render`/`update` references across `NamingContainer` boundaries use full client ID with leading colon.
- JSTL tags are only used for build-time view construction, not for conditional rendering (use `rendered` attribute instead).
- No inline styles; CSS is in separate files.
- Templates, includes, tag files, and composites are inside `/WEB-INF/` to prevent direct client access.
- Resources (scripts, styles, images) are referenced via `<h:outputScript>`, `<h:outputStylesheet>`, `<h:graphicImage>`, or `#{resource[]}`; when `WEBAPP_RESOURCES_DIRECTORY` is set to `WEB-INF/resources`, verify resources are actually in that location.
- No duplicate/copy-pasted XHTML blocks; reusable code is in templates, includes, tag files, or composite components.
- File download commands do not use ajax (or use `<p:fileDownload>` if PrimeFaces).

### Backing Beans
- `@Named` is used with a CDI scope annotation (not `@ManagedBean`, not missing scope).
- `@ViewScoped` is imported from `jakarta.faces.view` (or `javax.faces.view`), not from `jakarta.enterprise.context` or `javax.faces.bean`.
- Beans stored in `HttpSession` (`@ViewScoped`, `@SessionScoped`, `@ConversationScoped`, `@FlowScoped`, `@ClientWindowScoped`) implement `Serializable`.
- Initial state is loaded in `@PostConstruct`, not in constructors, field initializers, or getters.
- Getters are pure (no business logic, no lazy-loading, no side effects).
- `UISelectMany` backing properties use mutable collections (`new ArrayList`), not `List.of()`, `Arrays.asList()`, or `Stream.toList()`.
- `action` methods are used for business logic; `actionListener` is only used to prepare/gate the action.
- Scope matches usage: `@RequestScoped` for simple non-ajax forms, `@ViewScoped` for ajax forms/datatables, `@ApplicationScoped` for shared caches (must be thread-safe).

### Configuration (if accessible)
- `web.xml` has `FACELETS_SKIP_COMMENTS` set to `true`.
- `web.xml` has `INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL` set to `true`.
- `WEBAPP_RESOURCES_DIRECTORY`: when the resources directory contains composite components (`.xhtml` files), it MUST be set to `WEB-INF/resources` to prevent direct client access to composite component source code; for plain assets only (scripts, styles, images, fonts) it's merely a recommendation.
- FacesServlet is mapped to `*.xhtml` only (no legacy `*.jsf`, `*.faces`, `/faces/*`).
- `faces-config.xml` version matches `pom.xml` Faces dependency version.

### PrimeFaces (if present)
- Consult `.claude/faces/topics/primefaces.md` and check its rules.

### OmniFaces (if present)
- Consult `.claude/faces/topics/omnifaces.md` and check for opportunities to simplify code.

## Output Format

For each finding, report:
1. **File and line** — the location.
2. **Rule violated** — short description of which rule.
3. **Severity** — error (will cause bugs), warning (anti-pattern/risk), or info (improvement opportunity).
4. **Fix** — concrete suggestion.

Group findings by file. If no issues are found, confirm the code follows Faces best practices.
