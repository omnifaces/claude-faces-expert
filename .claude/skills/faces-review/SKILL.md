---
name: faces-review
description: Review Jakarta Faces code for common mistakes, anti-patterns, and rule violations
disable-model-invocation: true
argument-hint: "[file or directory, defaults to current project]"
allowed-tools: Read, Glob, Grep, Agent
---

*Version 1.8.0*

Review the Jakarta Faces code in `$ARGUMENTS` (if no argument, scan the project for `.xhtml` and backing bean files) against the rules in `.claude/faces/rules.md` and its topic files.

## Step 1: Determine the Faces Version

Before reviewing anything, establish BOTH Faces versions — they are not per definition the same. See "Version Discipline" in `.claude/faces/rules.md`.

**Runtime version** (which APIs exist), in this order:

1. An explicit `jakarta.faces-api`/`javax.faces` dependency, or a bundled Faces implementation (`org.glassfish:jakarta.faces` for Mojarra up to 4.x, `org.glassfish.mojarra:mojarra` for Mojarra 5.0+, `org.apache.myfaces.core:myfaces-impl` for MyFaces) — whose major.minor tracks the Faces version it implements.
2. Otherwise the `jakarta.jakartaee-api`/`jakarta.jakartaee-web-api` version, mapped via the version lineage in `.claude/faces/rules.md` (full Jakarta EE server only).
3. Otherwise the target application server, or as a last resort the XML namespaces used in the `.xhtml` files.

**Declared version** (which descriptor schema applies): the `version` attribute and `xsi:schemaLocation` of `faces-config.xml` — see the Configuration checklist below.

State both at the top of the report, with how each was detected. If the runtime version cannot be determined, say so and assume the most recent RELEASED version (Faces 4.1) — never assume an unreleased one. Never substitute the declared version for the runtime version.

Then hold every suggestion to the runtime version:

- Verify an API before recommending it, against the sources listed under "References" in `.claude/faces/rules.md`, using the set for the detected runtime version — this skill has no web access, so delegate the lookup to an `Agent`. If it cannot be verified, do not recommend it; report the API as unconfirmed instead.
- A genuinely useful newer API may be mentioned only as `info` severity, explicitly labelled with the version that introduces it and the fact that it requires an upgrade. It is never an `error` or `warning`.

Finally, establish the project's MARKUP STYLE the same way — plain Faces components (`<h:inputText>`) or passthrough elements (`<input type="text" faces:value="...">`) — by taking the dominant one across the `.xhtml` files. Both are valid; see "Passthrough Elements" in `.claude/faces/rules.md`. When there is no dominant style, as in a new or empty project, default to plain Faces components.

- Write every suggestion in the detected style. Do not nudge a project toward the other one.
- Report a view that mixes both as `info`, never as `error` or `warning`.
- NEVER propose a wholesale conversion from one style to the other, and never as a "modernisation". Some components have no passthrough element form at all, so such a conversion can only ever be partial, which is worse than either style consistently applied.

## Step 2: Review Checklist

For every non-trivial construct, ask in this order: (1) is it still needed, or does a built-in already do this? (2) is the mechanism available in the detected runtime version? (3) is it implemented correctly? Report at the highest level that fires and stop — NEVER propose modernising something you would delete.

### Superseded Constructs

Hand-rolled code that a built-in has since replaced. Report `info` when it merely duplicates the built-in, `warning` when the hand-rolled version has a defect the built-in does not. The replacement MUST exist in the detected runtime version. Suggest a plain Faces replacement; only propose an OmniFaces one when the project already depends on OmniFaces.

| Hand-rolled | Built-in | Since |
|---|---|---|
| `PhaseListener` shuttling `FacesMessage`s through the session to survive a redirect | navigation from `<f:viewAction>`, which keeps them automatically; else `Flash.setKeepMessages(true)` at the redirecting call site | 2.2 |
| `immediate="true"` on an ajax `UICommand` to skip input processing | `<f:ajax>` — `execute` already defaults to `@this`; on a PrimeFaces command, `process="@this"` | 2.0 |
| `isPostback()` guard in `@PostConstruct` for GET-only initialization | `<f:viewAction>` | 2.2 |
| Custom converter for a standard type | implicit converters (incl. `UUID` since 4.1) | varies |
| `Application.evaluateExpressionGet()` calls | `@Inject @ManagedProperty` | 2.3 |
| Hand-rolled server push (one-way, server to client) | `<f:websocket>`/`<o:socket>`, or `<o:sse>` | Faces 2.3 / OmniFaces 5.2 |

A `PhaseListener` calling `Flash.setKeepMessages(true)` on every postback is not a fix for the first row: the flash saves the messages whether or not the response is a redirect, so they are rendered again on the next request and keep echoing forward. A `NavigationHandler` decorator checking `NavigationCase.isRedirect()` is sound but is global machinery for a rare case.

Two notes on `immediate="true"`. On a non-ajax command it is still the mechanism, but ask first whether the command should be ajax at all — converting it drops the need entirely on a standard `UICommand`, and on a PrimeFaces command only together with `process="@this"`, which is not its default; only a command that genuinely must not use ajax (e.g. a file download) is left needing it. On a `UIInput` it means something else entirely — conversion and validation move to Apply Request Values — so it must never be stripped mechanically.

Because these findings are the most prone to false positives, each one MUST state what the built-in covers and confirm nothing else is lost. If the construct does more than the built-in, report `info` naming the difference — never as a fix-this.

### XHTML / Facelets
- XML namespaces match the project's Faces version. A namespace outside the standard ones is invalid only when nothing registers it — check for a `@FacesComponent(createTag = true)` class, a `*.taglib.xml`, a composite library and third-party dependencies first, and report a namespace that resolves but that no tag uses as `info`, never `warning`/`error`. `jakarta.faces.component` and `http://xmlns.jcp.org/jsf/component` both resolve on Faces 4.0+, so neither is a broken reference; an `@FacesComponent` that pins the old one while the page uses the new one IS broken and is an `error` — see "XML Namespaces" in `.claude/faces/rules.md`.
- HTML5 doctype `<!DOCTYPE html>` is used, not XHTML doctype.
- No "god form", no nested `UIForm`, and `UIInput`/`UICommand` components are inside a `UIForm` — see "Component Rules" in `.claude/faces/rules.md`.
- Every `UIInput` has a corresponding `UIMessage`. A `<h:message for>` naming a composite component does NOT count for the input inside it — it resolves to the composite's own client id and stays empty; report it as an `error` unless the composite already carries its own message in `<cc:implementation>`, or its `<cc:interface componentType>` backing component extends `UIInput`, which makes the composite itself the input and `for="compositeId"` correct. `<p:message for>`/`<p:messages for>` naming a composite does count, but only on PrimeFaces 14.0.0+ and only for the first input inside it; on PrimeFaces 13 and older, report it exactly as `<h:message>` — see "Component Rules" in `.claude/faces/rules.md`.
- A catch-all `UIMessages` with `redisplay="false"` exists in each view; when using ajax, its ID is covered by `render`/`update`.
- Conditionally rendered components that are ajax-updated are wrapped in an always-rendered container, and ajax updates target the wrapper ID.
- A `<h:panelGroup>` whose only attribute is `rendered` emits no element at all and reads as `<ui:fragment rendered="...">`; when it wraps a single component, `rendered` belongs on that component and the wrapper goes. Report `info`. Neither rewrite applies inside `<h:panelGrid>`, where the wrapper is a cell, nor to a wrapper that is an ajax `render`/`update` target, which needs the element it emits — see "Facelets Rules" in `.claude/faces/rules.md`.
- `NamingContainer`, `UIInput` and `UICommand` components have explicit IDs (no generated IDs); IDs follow naming convention (property name for `value`, method name for `action`, view ID name for forms/panels).
- `binding` is not used for data binding; `binding` is only used on page-scoped variables for component cross-referencing within the same view.
- Ajax `render`/`update` references across `NamingContainer` boundaries use full client ID with leading colon.
- JSTL tags are only used for build-time view construction, not for conditional rendering (use `rendered` attribute instead).
- No inline styles; CSS is in separate files.
- Resources are referenced via `<h:outputScript>`, `<h:outputStylesheet>`, `<h:graphicImage>`, or `#{resource[]}`, never by a hardcoded path; a hardcoded path bypasses any `ResourceHandler`, including those possibly adding cache busting.
- No duplicate/copy-pasted XHTML blocks; reusable code is in templates, includes, tag files, or composite components.
- File download commands do not use ajax (or use `<p:fileDownload>` if PrimeFaces).

### Directory Structure

Run this over every `.xhtml` OUTSIDE `/WEB-INF/`, before reporting on what any of them contain. It is a check on WHERE each file is, so a file whose contents are flawless still fails it — passing the XHTML checklist above is not grounds for clearing a file here, and a report that clears an `.xhtml` outside `/WEB-INF/` without naming its kind has skipped this section.

Classify each one, because the webapp root is the correct home for exactly one kind:

| Kind | How to recognize it | Verdict |
|---|---|---|
| View | none of the below — a page a user navigates to | correct where it is |
| Template | contains `<ui:insert>`, or another file points `template=` at it | `error` |
| Include | another file points `<ui:include src=>` at it | `error` |
| Tag file | declared in a `*.taglib.xml` | `error` |
| Composite | contains `<cc:interface>` — that alone is the marker; the root is usually `<ui:component>` but `<ui:composition>` is equally valid, so never key recognition off the root element | `warning` |
| Asset (script, style, image, font) | not `.xhtml`; under the resources directory | `warning` |

The severities differ because the fixes do. Moving a template, include or tag file into `/WEB-INF/` needs no configuration — only the `template`/`src` references to it have to follow. Moving a composite or asset has TWO halves, and either alone breaks the application: move the files into `/WEB-INF/resources/` AND set `jakarta.faces.WEBAPP_RESOURCES_DIRECTORY` to `WEB-INF/resources`. Name both in the fix.

State the exposure from the `FacesServlet` mapping rather than assuming source disclosure:

- Mapped to `*.xhtml` (the recommended mapping): the file is processed, not served, so the source does NOT leak. What is reachable is the file rendered standalone at its own URL, outside the view that was meant to supply its `ui:param` values or `cc.attrs`, its `f:metadata`, and its access checks.
- Mapped only to a legacy pattern (`*.jsf`, `*.faces`, `/faces/*`), which overrides the implicit `*.xhtml` mapping: `*.xhtml` reaches the container's default servlet and the raw Facelets source is downloadable — EL expressions, bean and property names, and markup a `rendered` attribute would have withheld. Confirm no `<security-constraint>` covers `*.xhtml` before reporting the source as exposed; that constraint is the other way a project blocks this.

`/WEB-INF/` closes both cases whatever the mapping. Report the composite before a plain asset when both apply, since a composite exposes its `<cc:implementation>` while an asset exposes only itself.

### Backing Beans
- Bean declaration, scope choice, `Serializable`, `@PostConstruct` initialization and getter purity all follow "CDI and Bean Management" and "Scope Selection" in `.claude/faces/rules.md`.
- `UISelectMany` backing properties use mutable collections (`new ArrayList`), not `List.of()`, `Arrays.asList()`, or `Stream.toList()`.
- `action` methods are used for business logic; `actionListener` is only used to prepare/gate the action.
- Event listeners use a mechanism that exists in the detected version — see the "System Events and Phase Listeners" section in `.claude/faces/rules.md`.

### Configuration (if accessible)
- `web.xml` has `FACELETS_SKIP_COMMENTS` set to `true`.
- `web.xml` has `INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL` set to `true`.
- `WEBAPP_RESOURCES_DIRECTORY`: when the resources directory contains composite components (`.xhtml` files), it MUST be set to `WEB-INF/resources` to prevent direct client access to composite component source code; for plain assets only (scripts, styles, images, fonts) it's merely a recommendation.
- FacesServlet is mapped to `*.xhtml` only (no legacy `*.jsf`, `*.faces`, `/faces/*`).
- Each deployment descriptor declares the version of ITS OWN specification — not the Jakarta EE platform version — and should match the API version the runtime actually supplies. A lagging declaration caps the available API features to that older version and should be bumped (`warning` if it lags, not an error — it deploys fine). Check the `version` attribute AND the `xsi:schemaLocation`, since bumping only one is the usual half-fix:
  - `faces-config.xml` -> Jakarta Faces API version, from `jakarta.faces-api` or the bundled Mojarra/MyFaces implementation.
  - `web.xml` -> Jakarta Servlet API version, from `jakarta.servlet-api` or the servlet container.
  - `beans.xml` -> Jakarta CDI API version, from `jakarta.enterprise.cdi-api` or the bundled Weld/OpenWebBeans; an empty or absent `beans.xml` is valid and needs no bump.
  - Take each version from the artifact that supplies that API; only on a full Jakarta EE server may they be derived from the platform version. Verify rather than assume the three move together.

### PrimeFaces (if present)
- Consult `.claude/faces/topics/primefaces.md` and check its rules.

### OmniFaces (if present)
- Consult `.claude/faces/topics/omnifaces.md` and check for opportunities to simplify code.
- A `@FacesConverter`/`@FacesValidator` class whose attributes are set by `<o:converter>`/`<o:validator>`: `managed=true` MUST be absent — report `error`, since the implementation wraps a managed artifact and every attribute is then silently dropped — and the class should be `@Dependent`. Condition this on the implementation rather than the spec version: only Mojarra 5.0 and newer is exempt, MyFaces still wraps on its 5.0 snapshots. Report `warning` when such a class is ALSO attached the standard way (`validator="id"`, `<f:validator>`, `converter="id"`, `<f:converter>`, `<default-validators>`), because it then runs unconfigured there. See "Deferred Attributes" in `.claude/faces/topics/conversion-validation.md`.

## Step 3: Validate Proposed Fixes

A fix that resolves one finding must not introduce another. Before reporting any fix that restructures markup — moving, splitting or wrapping elements — re-check the result against the rest of the checklist.

For form-boundary changes in particular: enumerate every `ActionSource` and `EditableValueHolder` in the affected region FIRST, then draw the boundary so each one still has a `UIForm` ancestor afterwards. Watch for those sitting outside the visual body of a section — in headers, toolbars, `ui:fragment` or `ui:include` — and for decorated elements such as `<a faces:action>`, which do not look like Faces components.

## Step 4: Output Format

Open the report with the detected Faces version and how it was detected.

For each finding, report:
1. **File and line** — the location.
2. **Rule violated** — short description of which rule.
3. **Severity** — error (will cause bugs), warning (anti-pattern/risk), or info (improvement opportunity).
4. **Fix** — concrete suggestion, using only APIs available in the detected version. If the fix depends on a newer version, say which version and mark the finding `info`.

Group findings by file. If no issues are found, confirm the code follows Faces best practices.
