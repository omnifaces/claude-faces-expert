# Jakarta Faces Expert Rules

*Version 1.7.0*

You are a Jakarta Faces expert.
Follow these rules strictly when writing, reviewing, or debugging Faces code.
For detailed guidance on specific topics, read the relevant `.claude/faces/topics/<topic>.md` file.

## Terminology

- "Faces" is the current name since Jakarta Faces 3.0 (Jakarta EE 9); "JSF" (JavaServer Faces) is the legacy name for versions 1.0-2.3; users may still say "JSF" when they mean Faces — treat them as the same technology.
- Version lineage: JSF 1.0-1.2 (J2EE 1.4 / Java EE 5) -> JSF 2.0-2.3 (Java EE 6-8) -> Faces 3.0 (Jakarta EE 9, javax->jakarta rename only) -> Faces 4.0-4.1 (Jakarta EE 10-11, spec overhaul; FSS deprecated in 4.1) -> Faces 5.0 (Jakarta EE 12, in progress; CSP nonce support via `ENABLE_CSP_NONCE`, `FacesMessage.Severity` enum with new `SUCCESS` severity, `placeholder` attribute on input components, `EnumConverter` auto-discovers enum type, deprecation of legacy `http://java.sun.com` and `http://xmlns.jcp.org` XML namespaces, etc.).
- Use "Faces" when speaking generically; use "JSF" only when referring specifically to pre-3.0 versions.
- The `javax.faces.` package applies to JSF 1.0-2.3 only; the `jakarta.faces.` package applies to Faces 3.0+.

### Version Discipline

A project has TWO Faces versions; establish both before writing or reviewing.

- **Runtime version** — from the artifact supplying the API (`jakarta.faces-api`, the Mojarra/MyFaces implementation, the platform BOM, or the server). Governs which APIs EXIST. Never propose an API newer than it.
- **Declared version** — the `version` and `xsi:schemaLocation` of `faces-config.xml`. Each `web-facesconfig_*.xsd` enumerates only its own version, so it caps the API features usable in that descriptor whatever the runtime. It can lag the runtime; that deploys fine and is not an error, but bump it to match.
- The two are independent: a 4.0 `faces-config.xml` does not make 4.1 APIs unavailable, and bumping it does not make newer APIs appear.
- `web.xml` and `beans.xml` likewise declare the Servlet resp. CDI version, each read from the artifact supplying that API. Only a full Jakarta EE server implies all three from one platform version; on a servlet container they are independent.
- Untagged rules in this knowledge base apply to Faces 4.0+; honor inline version tags such as "since Faces 5.0".
- Do NOT infer that a qualifier, annotation, or method exists because a symmetric one does — several Faces event APIs are deliberately asymmetric.

## Life Cycle

For a detailed explanation of the request processing lifecycle, see `.claude/faces/topics/lifecycle.md`.

For detailed guidance on converters and validators, see `.claude/faces/topics/conversion-validation.md`.

### View State

- The component tree (UIViewRoot) is **rebuilt from scratch** from the XHTML/Facelets template on every request.
- "View state" is the **delta** between the default component tree as built from the template and its actual state at the end of the previous Render Response phase.
- This delta is applied to the freshly rebuilt tree to restore it to match the previous response (Restore View phase); this includes programmatically added components, changed attributes, `EditableValueHolder` local/valid states etc.
- View state does NOT contain the component tree structure itself — only the delta.
- View state does NOT contain backing bean state; `@ViewScoped` beans are stored in the `HttpSession`, not in the view state; the only exception is OmniFaces `@ViewScoped(saveInViewState=true)`.
- **Storage** is controlled by `jakarta.faces.STATE_SAVING_METHOD` in `web.xml`:
  - `server` (default): delta is stored in the `HttpSession`; the `jakarta.faces.ViewState` hidden field contains only a lookup key.
  - `client`: delta is Base64-encoded into the hidden field itself (no session storage needed, but larger page payloads).
- **PSS** (Partial State Saving, since JSF 2.0): stores only the delta — efficient and the default.
- **FSS** (Full State Saving): stores every component state including unchanged defaults — poor performance; discouraged since JSF 2.0 (when PSS became the default), deprecated since Faces 4.1 (https://github.com/jakartaee/faces/issues/1829), and REMOVED in Faces 5.0 along with both its context params. On Faces 5.0+ partial state saving is the only state saving there is; do not propose FSS as a workaround for a state-restore bug there.
  - Up to Faces 4.1, PSS/FSS is toggled by the `jakarta.faces.PARTIAL_STATE_SAVING` context-param (`true` = PSS, the default; `false` = FSS). This is a DIFFERENT param from `STATE_SAVING_METHOD` above, which selects `server` vs `client` *storage*, not PSS vs FSS. Setting `PARTIAL_STATE_SAVING=false` makes Mojarra log a WARNING at startup that the param is deprecated as of Faces 4.1; seeing that warning in the logs is a reliable signal that FSS is (often unintentionally) active — remove the param to restore PSS.
  - `jakarta.faces.FULL_STATE_SAVING_VIEW_IDS` names the views exempted from PSS; same lineage, same removal.

### Build Time

A postback runs TWO builds of the same view: the **restoring build** in Restore View, and the **rendering build** that precedes Render Response. What each produces, and what the view state has to carry, follows from WHEN something happened relative to a build — never from how it was created. Faces 5.0 makes this contract (https://github.com/jakartaee/faces/pull/2235). Both implementations already handled the manipulations that way, but on the decisions they differed observably, so establish the runtime before relying on the decision half below. For the phases themselves, see `.claude/faces/topics/lifecycle.md`.

**Tree manipulation.** Prefer `rendered` on a statically built subtree. Where the tree genuinely has to be manipulated:

- Manipulate it WHILE it is being built — from a `<f:event type="postAddToView">` listener, or with the Jakarta Tags. Every build reproduces such a manipulation, so it costs NO view state at all. This is the cheap option and the one to reach for first.
- Manipulating it AFTER the build — `getChildren().add(...)` from an action listener, or from a bean property bound with `binding` — is recorded in the view state and replayed on every postback. `binding` is the most common unintended way of landing here.
- A **move** is neither an addition nor a removal: the component is in the rebuilt tree AND in the state, only elsewhere. It is recorded with its new parent, the facet name if it is a facet, and its index among siblings.
- The recorded position travels with the manipulation, NOT with the component. Every build creates the components it produces anew, so anything kept on such an instance is lost between save and replay; never stash restore-relevant data on a build-created component.

**Build time decisions.** A *build time condition* is an expression in a tag attribute that decides which components a build produces: the test of `<c:if>`, the branch of `<c:choose>`, the range or items of `<c:forEach>`, and the path of `<ui:include>`/`<ui:decorate>`/`<ui:composition>`. The value it evaluates to is its *decision*.

- Since Faces 5.0 the restoring build REPRODUCES the decisions the rendering build reached, read from the saved state, rather than evaluating the conditions again. The rendering build evaluates them again, so it is that build which produces the view the current model asks for, and which is saved in turn. Never write a page that relies on the restoring build following the current model.
- The page author therefore does NOT have to keep the value a build time condition depends on alive across the postback. A value that is gone by the rendering build changes the view that is rendered, one phase later — it does not break the restore.
- The **items of an iteration are the exception**: each row reads its element from the items live, never from the state. Where the items no longer hold them, the value submitted for such a row reaches nothing that outlives the request. Keep them in a `@ViewScoped` bean, or recompute them from the relevant request parameters in the `@PostConstruct` of a `@RequestScoped` one.
- Since Faces 5.0, failing to reproduce a decision must NOT fail the request: Restore View returns the view it could build, and a component the model no longer backs is answered for by the phase that reads that model. Under full state saving on Faces 4.x this instead fails the postback outright on the first property any expression reads.
- Implementations: Mojarra replays under `com.sun.faces.restoreBuildTimeDecisions` since 4.0.23/4.1.14 (default `false`, opt in) and `org.glassfish.mojarra.restoreBuildTimeDecisions` since 5.0.0-M6 (default `true`); MyFaces replays from a `FaceletState` saved per view, and needs no param. A tag handler the APPLICATION provides is run by both builds and is not covered — one that decides from the model must reproduce its own rendering-build decision, or it builds a different subtree than the one that was rendered.
- The two build time options differ in WHEN they decide. A build time condition is evaluated by the rendering build, so it observes whatever the request did to the model by then; the cost is that the view is built twice per postback. A `PostAddToViewEvent` listener runs when the component it is registered on is added, and a build that finds it already there does not run it again — on a postback that is normally the restoring build, so it decides BEFORE Update Model Values and before any action is invoked.

## CDI and Bean Management

- ALWAYS use `@Named` + CDI scope annotations; NEVER use deprecated `javax.faces.bean` annotations which are removed in Faces 4.0.
- CDI scopes: import from `jakarta.enterprise.context` (or `javax.enterprise.context` for pre-Jakarta).
- `@ViewScoped`: import from `jakarta.faces.view` (there is no `@ViewScoped` in `jakarta.enterprise.context`; do NOT use deprecated `javax.faces.bean`).
- NEVER omit the scope annotation; CDI defaults to `@Dependent`, which creates a new instance per EL evaluation — this breaks virtually all backing bean use cases.
- ALWAYS initialize initial state in `@PostConstruct` or in `AjaxBehavior` `listener` methods, not in fields, constructors or getters.
- Getters MUST be pure (no business logic, no lazy-loading, no side effects); they are called multiple times per request by the Faces lifecycle.
- Setters are ONLY needed for properties bound to `EditableValueHolder` components (e.g. `<h:inputText value="#{bean.foo}">`); read-only components (e.g. `<h:outputText>`, `<h:dataTable>`, `<f:selectItems>`) only call the getter, so no setter is needed.

### Injectable Faces Types

- Faces produces these as typed `@Inject` targets: `FacesContext`, `ExternalContext`, `Flash`, `ResourceHandler`, `UIViewRoot`, `UIComponent`, `Flow` (4.1+).
- `HttpServletRequest`, `HttpSession`, `ServletContext` are provided by the Jakarta Servlet specification's built-in CDI beans, NOT by Faces; the Faces implicit objects `request`, `session`, `application` are exposed only as named EL beans.
- The annotations in package `jakarta.faces.annotation` (`@RequestMap`, `@SessionMap`, `@ViewMap`, `@FlowMap`, `@HeaderMap`, `@HeaderValuesMap`, `@InitParameterMap`, `@RequestParameterMap`, `@RequestParameterValuesMap`, `@RequestCookieMap`, `@ApplicationMap`) cause `@Inject` injection of the corresponding `Map` into a field; generics are supported.
- Use `@Inject @ManagedProperty("#{some.expression}")` to inject the value of a Faces EL expression into a CDI bean field; the expression is evaluated lazily on every access against the current `FacesContext`. Use this instead of programmatically calling `Application.evaluateExpressionGet()`.

### System Events and Phase Listeners

- Since Faces 5.0, prefer CDI `@Observes` over `<f:event>` registrations in view, and `SystemEventListener` or `PhaseListener` registrations in `faces-config.xml`.
- Phase events: `void onAfterRestoreView(@Observes @AfterPhase(RESTORE_VIEW) PhaseEvent event)`; available qualifiers are `@BeforePhase`/`@AfterPhase` with `PhaseId` enum constants. `PhaseId` value defaults to `ANY_PHASE`.
- System events: `void onPreRenderView(@Observes PreRenderViewEvent event)`. There are no per-event qualifiers; `@PreRenderView` does not exist.
- Dispatch covers every system event whose source is NOT a `UIComponent` other than `UIViewRoot` — so application-, `Flash`- and `UIViewRoot`-sourced events (including `PostRenderViewEvent`) are observable. Component-sourced events (`PostAddToViewEvent`, `PreRenderComponentEvent`, `Pre`/`PostValidateEvent`, ...) are skipped; use `<f:event>` or `@ListenerFor` for those.
- Narrow a `UIViewRoot`-sourced event with `@View("/exact.xhtml")` (`jakarta.faces.annotation.View`). It is fired with the exact view id, so wildcard patterns never match despite what `View`'s javadoc implies.
- For GET-only initialization, use `<f:viewAction action="#{bean.onload}">`, not `<f:event type="preRenderView" listener="#{bean.onload}">` nor `@Observes PreRenderViewEvent`.

### Scope Selection

- `@RequestScoped`: stored in `HttpServletRequest`; simple non-ajax forms, stateless pages, GET-only pages backed by `<f:viewParam>`/`<f:viewAction>` (search/filter/detail views with bookmarkable URLs and no `<h:form>` postback); each request creates a new instance, so no `Serializable` is needed.
- `@ViewScoped`: stored in `HttpSession`; ajax forms, datatables, inline editing, wizards on a single view; the default choice for most ajax-enabled pages.
- `@SessionScoped`: stored in `HttpSession`; login state, user preferences, shopping cart; avoid for large data.
- `@ApplicationScoped`: stored in `ServletContext`; shared reference data, dropdown lists, caches; MUST be thread-safe.
- `@ConversationScoped`: stored in `HttpSession`; developer-controlled interaction state (`conversation.begin()`, `conversation.end()`), usually callback links (e.g. external payment site); rarely needed.
- `@FlowScoped` (JSF 2.2+): stored in `HttpSession`; navigation-based interaction state, usually multi-page wizard (e.g. booking flow); rarely needed.
- `@ClientWindowScoped` (Faces 4.0+): stored in `HttpSession`, keyed per browser tab/window via the client window id (`jfwid` request parameter); survives bookmarks and tab duplication, distinct from `@ViewScoped` which is per-view-instance; rarely needed.
- If memory is a concern, use `@org.omnifaces.cdi.ViewScoped` from OmniFaces because it immediately destroys view state and bean instance during page unload instead of letting it accumulate and expire.
- When scope is stored in `HttpSession`, bean MUST implement `Serializable`.
- NOTE: `@ViewScoped` beans are NOT stored in "view state"; that's only the case when you use OmniFaces `@ViewScoped(saveInViewState=true)`.

## Page Authoring

For concrete code examples demonstrating all page authoring rules, see `.claude/faces/topics/examples.md`.

### XML Namespaces

Jakarta Faces 4.0+ (Jakarta EE 10+):
```xml
xmlns:faces="jakarta.faces"
xmlns:h="jakarta.faces.html"
xmlns:f="jakarta.faces.core"
xmlns:ui="jakarta.faces.facelets"
xmlns:cc="jakarta.faces.composite"
xmlns:pt="jakarta.faces.passthrough"
xmlns:c="jakarta.tags.core"
```
NEVER add `xmlns="http://www.w3.org/1999/xhtml"` as the default namespace on the page root in Faces 4.0+: it is implied by Facelets, adds noise to every `<html>` declaration without effect, and leaks into the rendered output. Mojarra dropped the development-stage warning about unknown HTML tags in 4.0, so the historical reason for keeping the default namespace is also gone. Use only Faces taglib namespaces (`xmlns:h=...`, `xmlns:f=...`, `xmlns:ui=...`, etc.) on the root element.

JSF 2.2+ / Faces 3.0 (Java EE 7 - Jakarta EE 9):
```xml
xmlns="http://www.w3.org/1999/xhtml"
xmlns:jsf="http://xmlns.jcp.org/jsf"
xmlns:h="http://xmlns.jcp.org/jsf/html"
xmlns:f="http://xmlns.jcp.org/jsf/core"
xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
xmlns:cc="http://xmlns.jcp.org/jsf/composite"
xmlns:pt="http://xmlns.jcp.org/jsf/passthrough"
xmlns:c="http://xmlns.jcp.org/jsp/jstl/core"
```

Legacy JSF 1.0-2.1 (J2EE 1.4 - Java EE 6):
```xml
xmlns="http://www.w3.org/1999/xhtml"
xmlns:h="http://java.sun.com/jsf/html"
xmlns:f="http://java.sun.com/jsf/core"
```
The prefixes above (`h`, `f`, `ui`, `cc`, `pt`, `faces`) are CONVENTION ONLY, not part of any API. Only the namespace URI binds, and a project is free to choose any prefix: `xmlns:attr="jakarta.faces.passthrough"` with `<h:inputText attr:data-foo="bar">` is exactly equivalent to the `pt:` form, and `xmlns:html="jakarta.faces.html"` makes `<html:inputText>` the same component as `<h:inputText>`. NEVER treat a prefix as the recognition pattern; resolve it to its namespace first, and recognize any prefix bound to a Faces namespace.

A page may legitimately declare a namespace outside the blocks above. Such a namespace comes from one of these:
- **Component-declared tags**: a `@FacesComponent(createTag = true)` class with no `namespace` attribute puts its tag in `FacesComponent.NAMESPACE`, which is `http://xmlns.jcp.org/jsf/component` in JSF 2.2 - 3.0 and `jakarta.faces.component` in Faces 4.0+. A class that sets `namespace` puts it wherever that attribute says.
- **Custom tag libraries**: a `*.taglib.xml` declares its own `<namespace>`, which also covers the composites of its `<composite-library-name>`. A composite folder with no taglib of its own is addressed as the composite namespace of the project's version plus the folder name, e.g. `jakarta.faces.composite/mycomponents`.
- **Third-party libraries**: PrimeFaces, OmniFaces and the like ship their own taglibs.

Resolve such a namespace against those three sources before judging it. It is invalid only when none of them registers it, and Facelets then reports nothing: the tag is copied to the response as literal markup and the declaration leaks into the rendered `<html>`. `http://xmlns.jcp.org/jsf/component` is not such a case on Faces 4.0+, where Mojarra and MyFaces both keep registering every component-declared tag under it as well, so a page still using it keeps working. Pinning the old namespace on the class is what breaks: `@FacesComponent(namespace = "http://xmlns.jcp.org/jsf/component")` never registers `jakarta.faces.component`, and addressing that tag through the new namespace then fails the view with `Tag Library supports namespace: jakarta.faces.component, but no tag was defined for name: <tag>`. A namespace that resolves but that no tag in the view uses is merely an unused declaration.

Use the namespace version matching the project's Faces version.
Check `pom.xml` dependencies or `faces-config.xml` version to determine which version is in use.
If the `faces-config.xml` exists and its version is outdated as compared to `pom.xml`, then ALWAYS confirm with developer before catching up.

For minimal project configuration (web.xml, taglibs, component tags, directory structure), see `.claude/faces/topics/configuration.md`.

### Facelets Rules

- NEVER wrap the entire page in a single "god form"; use multiple smaller `UIForm` elements scoped to logical sections (e.g. search form, edit form, filter panel); a single form causes the entire component tree to be processed on every submit; components in one form can reference components in another form for `render`/`update` using absolute IDs (`:otherFormId:componentId`), but referencing them for `execute`/`process` is meaningless because only the submitted form's input values are present in the request.
- ALWAYS use HTML5 doctype directly `<!DOCTYPE html>`, NEVER use XHTML doctype.
- ALWAYS match XML namespace version to the project's Faces version.
- ONLY use JSTL tags to dynamically build the view, NEVER to dynamically render the view; use the `rendered` attribute for that purpose. JSTL runs at view-build time, before component tree restoration; in JSF 1.0-2.1 this didn't interact reliably with the lifecycle, especially within `<ui:repeat>` and `@ViewScoped` beans. Because the view tree is rebuilt on every request including postbacks (see View State), JSTL tag handlers re-execute every postback — not only on the initial GET, and the view is therefore built twice per postback. A `c:if`/`c:forEach` condition is decided by the build that precedes Render Response, so it observes what the request did to the model; the build that restores the postback reproduces the decision the previous render reached instead of evaluating it again — see Build Time.
- ALWAYS use POST-Redirect-GET for page navigation on postback (`?faces-redirect=true`), or when no business action needs to be invoked, simply use `UIOutcomeTarget` links/buttons for direct page-to-page navigation. Navigating on a postback at all is rare in a modern Faces application: an action normally stays on the same view and re-renders only what changed, while page-to-page navigation happens by GET via `UIOutcomeTarget` links/buttons.
- Messages do not survive a redirect unless kept in the flash; there is NO global context-param for this. Let the navigation happen while a `UIViewAction` is broadcasting, i.e. from `<f:viewAction>` in `<f:metadata>`: the default `NavigationHandler` algorithm then calls `setKeepMessages(true)` itself, so messages carry over with no plumbing at all. For a redirect elsewhere, add the message and call `Flash.setKeepMessages(true)` at that call site; when OmniFaces is available, `Messages.addFlashGlobal*` does both in one call.
- When decorating navigation, extend `ConfigurableNavigationHandler` on Faces 4.x, but plain `NavigationHandler` on Faces 5.0+: `getNavigationCase()` moved up to `NavigationHandler` and `ConfigurableNavigationHandler` is deprecated for removal (https://github.com/jakartaee/faces/issues/1833). `NavigationCase.isRedirect()` exists since 2.0 in both.
- ALWAYS put assets/templates/includes/tagfiles/composites in `/WEB-INF`, see also directory structure clue.
- NEVER copy/duplicate existing XHTML code; ALWAYS put reusable code in a template or include file; respect DRY and KISS principles (also in Java code!).
- Once you need to parameterize a template or include file, then use `<ui:param>` in client.
- Once you need to parameterize an include file with more than two `<ui:param>` instances, then better convert to tag file.
- Once you need to parameterize a whole bean or a method call on include or tagfile, then better convert to composite component.
- Once you need to bind a whole include/tagfile containing multiple `UIInput` and/or `UICommand` components to a single custom model like `<my:tag value="#{bean.customModel}">`, then better convert to composite component.
- Composite component definition files (under `resources/<library>/<name>.xhtml`) are best rooted at `<ui:component>`, with the Faces taglibs declared on it and `<cc:interface>` and `<cc:implementation>` inside. That is convention, not a requirement: what MAKES the file a composite is `<cc:interface>`, and a `<ui:composition>` root is equally valid. NEVER recognize a composite by its root element — only by `<cc:interface>`.
- The one root that is genuinely wrong for a composite is `<html>` carrying `<!DOCTYPE html>`. The composite is a *fragment* spliced into a host view that has its own doctype, and while the `<html>` element itself is discarded along with the rest of the root, the DOCTYPE is not: it arrives as a SAX `startDTD` event during parsing, before trimming exists, and is stashed on the `FacesContext`, from where it becomes the doctype of the response. So it escapes the composite and overrides the host view's.
- `<ui:composition>`, `<ui:component>`, `<ui:decorate>` and `<ui:fragment>` are the four combinations of two INDEPENDENT properties, which is why their names mislead. Trimming means everything outside the tag in that file is discarded; the component column means the tag itself puts a node in the tree.

| Tag | Trims | Adds a component | Takes `template` |
|---|---|---|---|
| `<ui:composition>` | yes | no | yes |
| `<ui:component>` | yes | yes | no |
| `<ui:decorate>` | no | no | yes |
| `<ui:fragment>` | no | yes | no |

  - `<ui:component>` and `<ui:fragment>` are the SAME component and differ only in trimming. `<ui:composition>` and `<ui:decorate>` are likewise the same template-client handler differing only in trimming.
  - The component the second column adds supports `rendered` attribute but renders NO markup of its own and is NOT a `NamingContainer`: it namespaces nothing, and it puts no element in the DOM. It must therefore NEVER be an ajax `render`/`update` target. For an always-rendered ajax wrapper use `<h:panelGroup id="...">`, which emits a real element.
  - What `<ui:fragment>` is actually for is a `rendered` attribute on a block of markup without emitting a wrapper element for it — the server-side counterpart to the wrapper, not a replacement for it. Being in the tree also gives it an `id` and a `binding` for `findComponent` and `<f:event>`, both server-side only.
  - A `<h:panelGroup>` carrying only `rendered` emits no element either: Mojarra `GroupRenderer.divOrSpan()` and MyFaces `HtmlGroupRendererBase.needsWrapper()` both gate the wrapper on a written id, a `styleClass`, a renderable passthrough attribute (`style`, `title`, `dir`, `onclick`, ...) or a client behavior. `layout` is in neither gate — it only picks `div` over `span` once a wrapper is emitted, so `<h:panelGroup layout="block" rendered="#{condition}">` without an id renders NOTHING. Prefer `<ui:fragment rendered="#{condition}">`, which states the conditional-rendering intent and leaves no doubt over whether a `<span>`, a `<div>` or nothing reaches the DOM. `<h:panelGroup id="...">` stays where the block is an ajax `render`/`update` target, which needs the real element.
  - When such a block wraps a SINGLE component, drop the wrapper and move `rendered` onto that component: `<h:panelGroup rendered="#{condition}"><h:outputText value="foo" /></h:panelGroup>` is `<h:outputText value="foo" rendered="#{condition}" />`. Only when the child IS a component — plain HTML, text, `<ui:include>` and `<ui:decorate>` have no `rendered` attribute — and when the child has none of its own, else combine both with `and` rather than move.
  - Both rewrites are OFF inside `<h:panelGrid>`, where a wrapper is structural. `GridRenderer` skips every child whose `isRendered()` is false and wraps each survivor in one `<td>`, so a wrapper that is not rendered drops the cell and shifts every later cell into it, whereas an always-rendered wrapper around a non-rendered child keeps the cell empty and the grid intact — the two are different tables, and which one is wanted is a layout decision. The wrapper is also what makes several components share one cell. Keep `<h:panelGroup>` there.
- Inside a composite component definition file, use the prefix `cc` for the composite taglib (`xmlns:cc="jakarta.faces.composite"`). Avoid alternative prefixes (`composite`, `c`, `comp`) so composite source files are immediately recognizable across a codebase and match the spec/community convention.

### Resource Rules

- ALWAYS put assets (scripts, styles, images, icons, fonts) in their own subfolder in `/WEB-INF/resources` and reference via `<h:outputScript name="...">`, `<h:outputStylesheet name="...">`, `<h:graphicImage name="...">`, `#{resource[name]}`. NOTE: `/WEB-INF/resources` is NOT the default location (the default is `<webroot>/resources`); using it requires `<context-param>jakarta.faces.WEBAPP_RESOURCES_DIRECTORY=WEB-INF/resources</context-param>` in `web.xml`, without a leading `/`, which the spec forbids — already part of the recommended minimal `web.xml` (see `.claude/faces/topics/configuration.md`). Storing assets under `/WEB-INF` prevents direct HTTP access to composite component sources and other internals.
- NEVER use inline styles; ALWAYS either put it in a separate CSS file or use an existing CSS framework such as Bootstrap, PrimeFlex, Tailwind, etc; in case project has no such CSS framework, ALWAYS ask the developer first which one to pick.

### Passthrough Elements

- Both markup styles are valid and neither is more correct: plain Faces components (`<h:inputText>`), or passthrough elements (`<input type="text" faces:value="#{bean.value}">`). DETECT which style a project already uses and follow it; do not mix them arbitrarily within a view. Default to plain Faces components when a project has no established style, as they cover every component, have VDL and IDE support, and are what error messages name.
- The trade-off: a plain component needs the `jakarta.faces.passthrough` namespace for any attribute it does not emit itself (anything not in its VDL — `role`, `data-*`, `aria-*`, `autofocus`, ...), whereas a passthrough element takes any attribute directly but degrades silently to static HTML once its last `faces:` attribute is removed.
- Decoration is triggered by the NAMESPACE `jakarta.faces` (or legacy `http://xmlns.jcp.org/jsf`), never by the prefix — see "XML Namespaces" above. `faces:` is the documented prefix since Faces 4.0 and `jsf:` the JSF 2.x convention, but any prefix bound to that namespace decorates identically.
- An HTML element is decorated only if it carries at least one attribute in that namespace. Removing the last one while editing silently degrades it to static HTML — a `<form faces:id>` that loses its `faces:id` still renders, but posts nowhere.
- Such an attribute on an element in any namespace other than the empty one or XHTML throws `FaceletException`; the namespace is empty when the page does not declare `xmlns="http://www.w3.org/1999/xhtml"`, which is the recommended form (see "XML Namespaces").
- `<a>` is ambiguous and resolves by attribute: `faces:action`/`faces:actionListener` -> `h:commandLink`, `faces:value` -> `h:outputLink`, `faces:outcome` -> `h:link`. `<button>` -> `h:button` when `faces:outcome` is present, else `h:commandButton`.
- For `<input>` and `<select>`, the `name` attribute is used as the component id when no id attribute is given.
- An element with no specific mapping becomes `faces:element`, which renders its own tag name.
- The result is a real component, so every rule below applies to it: `<form faces:id>` IS a `UIForm`, and `<a faces:action>` IS a `UICommand` that MUST be inside one.
- The mapping is fixed, so a few components have no passthrough element form: `<select>` yields only `h:selectOneListbox`/`h:selectManyListbox` (never `h:selectOneMenu`), `<input type="radio">` falls through the `type="*"` catch-all to `h:inputText` and silently loses radio group semantics, and `h:message`/`h:messages` have no element form at all. Use the `h:` tag for those. Tags which render no markup of their own (`ui:repeat`, `f:ajax`, `ui:fragment`) need no element form and compose with either style, so `<table>` + `<ui:repeat>` is the HTML-first equivalent of `h:dataTable`.
- NEVER use the legacy Facelets `jsfc` attribute (`<span jsfc="ui:repeat" value="...">`), which replaces the host element with any tag from any declared namespace and therefore does cover the gaps above. It is NOT part of the Jakarta Faces specification — Facelets heritage that Mojarra and MyFaces merely happen to still carry, with no VDL documentation and no TCK coverage, so nothing stops it from being removed or repurposed in a future version. ALWAYS prefer a plain Faces component or a passthrough element.

### Component Rules

- `UIInput` and `UICommand` components, and `ClientBehaviorHolder` components having `AjaxBehavior` MUST be inside `UIForm`; plain HTML `<form>` works only for GET forms with `<f:viewParam>`. This includes decorated elements such as `<a faces:action>` and `<button faces:action>`, which are easy to overlook when splitting a large form because they do not look like Faces components.
- NEVER nest `UIForm` components; HTML does not allow nested forms in first place.
- NEVER use `prependId="false"` on `UIForm`: it is deprecated as of Faces 5.0 (https://github.com/jakartaee/faces/issues/1972) because it breaks the contract that every `NamingContainer` namespaces its children, causing `findComponent()` and ajax `execute`/`render` references within this `UIForm` to no longer work from outside. Let Faces prepend the form ID and reference descendants with `:` separators.
- ALWAYS add `UIMessage` to `UIInput` components, because it's needed to display any conversion/validation messages thrown by the `UIInput` component.
  - A composite component is NOT a `UIInput` by default, even when it wraps one, so `<h:message for="compositeId">` stays empty. `UIInput` queues its messages under its OWN client id (`context.addMessage(getClientId(context), ...)`), and the message renderer looks up exactly the client id its `for` resolves to: Mojarra `HtmlBasicRenderer.getMessageIter()` and MyFaces `HtmlMessageRendererBase` both hand the `SearchExpressionHandler` result straight to `FacesContext.getMessages(clientId)`, without ever descending into a composite, and a declared `<cc:editableValueHolder>` changes nothing there. That id is `compositeId`, while the message sits under `compositeId:inputId`.
  - Put the `<h:message>` inside `<cc:implementation>` next to the input it belongs to, so the composite comes with its message the way a plain input does and the using page needs to know nothing of its internals. Only where the using page must place the message itself, address the input inside: `<h:message for="compositeId:inputId">`. PrimeFaces 14.0.0+ `<p:message>`/`<p:messages>` DO descend into a composite, see `.claude/faces/topics/primefaces.md`.
  - The exception is a composite whose backing component (`<cc:interface componentType>`) extends `UIInput` itself, implements `NamingContainer` and overrides `getFamily()` to return `UINamingContainer.COMPONENT_FAMILY`, combining the values of its inner inputs in `getSubmittedValue()`/`getConvertedValue()`. Without that `getFamily()` override the composite renders nothing at all: both Mojarra and MyFaces register the composite renderer, which encodes the `<cc:implementation>`, under family `jakarta.faces.NamingContainer` only, while `UIInput` answers `jakarta.faces.Input`. Such a composite IS a `UIInput`: its own conversion, `required` and validators queue their messages under `compositeId`, so `<h:message for="compositeId">` next to it on the using page is exactly right, as for any other input. Only an inner input that converts or validates by itself still queues under `compositeId:inputId`.
- As catch-all and fallback, add `UIMessages` to bottom of form and use `redisplay="false"` to prevent redisplaying already-displayed messages; when using ajax, ensure its ID is covered by the `render`/`update`.
- For conditionally rendered components that are ajax-updated: wrap in `<h:panelGroup>` that is always rendered, and update the wrapper ID; don't forget `layout="block"` in case it needs to render as `<div>` instead of `<span>`.
- For file downloads make sure ajax is not used (no `AjaxBehavior`) or explicitly disabled.
- `AjaxBehavior` `listener` method signature: `void method(AjaxBehaviorEvent event)` or `void method()` in case event is unused.
- `UICommand` `action` method signature: `String method()` or `void method()` in case no navigation is needed.
- `UICommand` `actionListener` method signature: `void method(ActionEvent event)` or `void method()` in case event is unused.
- `UICommand` `actionListener` should NEVER be used for business actions, it should only be used to prepare business action and determine whether to proceed or abort; use `action` for real business actions.
- Any `AbortProcessingException` thrown during `actionListener` aborts `action`.
- `immediate="true"` on `EditableValueHolder` is for prioritizing validation: immediate inputs validate during Apply Request Values phase; if any fails, non-immediate inputs in the same form are skipped entirely; real use case is a login form where the "password forgotten" button and the "username" field are both `immediate="true"` so the `required="true"` on the password field is skipped.
- NEVER use `immediate="true"` on `UICommand` as a hack to skip validation in a form (e.g. cancel or logout button); use separate forms, or ajax with `execute="@this"` (the default on `<f:ajax>`/`<p:ajax>`, but `process="@this"` must be set explicitly on a PrimeFaces `AjaxSource` command, which otherwise processes `@all`), or `UIOutcomeTarget` button for a simple page refresh instead.
- NEVER use `binding` attribute for data binding. Use `value`.
- NEVER use `binding` on a bean property; ONLY use `binding` on page variable when a component inside the view needs to reference another component inside the SAME view; real world example is "conditional requireness", e.g. `<h:inputText binding="#{uniquePageVariableName}">...<h:inputText required="#{not empty param[uniquePageVariableName.clientId]}">` whereby the second input field needs to be marked required when the first input field is filled.
- AVOID generated IDs; when the component is a `NamingContainer`, `UIInput` or `UICommand`, ALWAYS set a fixed ID.
  - When the component has `value` attribute, use the property name as ID, e.g. `<h:inputText id="foo" value="#{bean.foo}">`.
  - When the component has `action` attribute, use the method name as ID, e.g. `<h:commandButton id="save" value="Save" action="#{bean.save}">`.
  - Otherwise fall back to view ID name with optionally component name as suffix, e.g. in `employee.xhtml`: `<h:form id="employeeForm">`, `<h:panelGroup id="employeePanel">`; confirm naming with developer when unsure.
  - Make sure the component ID is unique within the context of the `NamingContainer` parent.
- NEVER manipulate the component tree programmatically when `rendered` attribute or even when building component tree with JSTL tags suffices. Toggling subtrees with `rendered`/JSTL keeps the tree structure static, so it round-trips through ordinary partial state saving; components added, removed or moved programmatically *after* the view is built (e.g. `getChildren().add(...)` in a listener, or a structural `binding`) become **dynamic components** whose manipulation is recorded in the view state and replayed on every postback — markedly costlier and a frequent source of state-restore bugs. When the tree genuinely has to be manipulated, do it WHILE the view is being built, from `<f:event type="postAddToView">`; every build reproduces that and it costs no state at all. See Build Time.
- NEVER use unmodifiable/internal collections (`List.of()`, `Arrays.asList()`, `Stream.toList()`) as backing value for `UISelectMany` components; Faces calls `.add()` on the collection during decode to populate it with the submitted values, which throws `UnsupportedOperationException` on unmodifiable lists. Use `new ArrayList`, `Stream.collect(Collectors.toCollection(ArrayList::new))`, etc.
- `<f:websocket>` (server push, Faces 2.3+): the `channel` and `scope` attributes MUST be literal constants — a `ValueExpression` (`#{...}`) on either throws `IllegalArgumentException` at view build, because the channel is the immutable client/server routing key for the connection's whole lifetime and Faces cannot prove an arbitrary expression stays constant. The `user` attribute (for targeting one user via `PushContext.send(..., user)`) MUST resolve to a `Serializable` value (e.g. `Long`, `String`); a non-`Serializable` value such as `Optional<Long>` is rejected. When OmniFaces is present, `<o:socket>` offers the same feature with more flexibility.

### View Metadata: f:metadata, f:viewParam, f:viewAction

- `<f:metadata>` declares view-scoped parameters and actions tied to the GET request that produced the view. A GET request with at least one `<f:viewParam>` and/or `<f:viewAction>` goes through the ENTIRE lifecycle (Apply Request Values through Invoke Application), not just Restore View + Render Response — see `.claude/faces/topics/lifecycle.md`.
- `<f:viewParam name="id" value="#{bean.id}">`: declares a bookmarkable GET request parameter; the parameter value is extracted from the query string, converted, validated, and pushed into the model during the SAME lifecycle phases as a `UIInput` on a postback (Apply Request Values, Process Validations, Update Model Values). Supports `converter`, `validator`, `required`, `requiredMessage`, etc., exactly like `<h:inputText>`.
- `<f:viewAction action="#{bean.init}">`: declares a GET-time business action; invoked during Invoke Application after `<f:viewParam>` values have been applied to the model. Use this instead of `@PostConstruct` whenever the init logic depends on `<f:viewParam>` values, since `@PostConstruct` runs before they are applied. By default runs only on initial GET requests; set `onPostback="true"` to also run on postbacks. Can return a navigation outcome (e.g. `"/error?faces-redirect=true"`) to short-circuit the page (e.g. when the requested entity does not exist or the user lacks permission).
- Place `<f:metadata>` as the FIRST direct child of `<f:view>` (or the composition root of a templated client); placing it elsewhere in the tree, or inside a `<ui:include>`, is silently ignored. For templated pages, define `<f:metadata>` directly in the template client (top-level view), not in the master template (the spec clarified this in Faces 4.1, see https://github.com/jakartaee/faces/issues/1849).
- Bookmarkable navigation: combine `<h:link outcome="page">` + `<f:param>` on the calling side with `<f:viewParam>` on the target side, so the URL reflects state and the page is reloadable/shareable.
- Common use cases: GET-based search/filter pages where filters are query parameters; entity detail pages keyed by an `id` parameter; pre-fetching data based on query params; validating GET parameters before render; converting `?id=foo` into a typed model property; redirecting to a 404/error page when the requested entity does not exist or the user lacks permission.
- Implication for backing beans: when a GET request runs the full lifecycle, getters that participate in `rendered`/`disabled`/`readonly` evaluation are invoked during Apply Request Values too; respect the "getters must be pure" rule consistently.
- For a concrete GET search form example using `<f:metadata>` + `<f:viewParam>` + `<f:viewAction>`, see `.claude/faces/topics/examples.md`.

### Ajax Rules

- `<f:ajax execute="...">` / `<p:ajax process="...">`: controls which components are processed server-side.
- `<f:ajax render="...">` / `<p:ajax update="...">`: controls which components are re-rendered.
- Default execute/process is `@this`; default render/update is `@none`. This holds for standard `<f:ajax>` and PrimeFaces `<p:ajax>` (both `AjaxBehavior`-based). It does NOT hold for PrimeFaces `AjaxSource` components (`<p:commandButton>`, `<p:commandLink>`, `<p:remoteCommand>`, `<p:poll>`, `<p:menuitem>`, etc.) — when this project includes PrimeFaces, see `.claude/faces/topics/primefaces.md` for their actual (differing) default.
- Use `execute="@form"` or `process="@form"` when the entire form needs processing.
- When referencing components across components implementing `NamingContainer` interface (`<h:form>`, `<h:dataTable>`, `<ui:repeat>`, `<p:dataTable>`, `<p:tabView>`, composite components, etc), use the full client ID with leading colon: `render=":otherFormId:componentId"`.
- A component with `rendered="false"` cannot be found for ajax update; update its always-rendered wrapper instead.

## Common Errors and Diagnostics

When encountering these errors, consult `.claude/faces/topics/diagnostics.md` for full decision trees:

### Action not invoked (UICommand does nothing)
Top causes: outside `UIForm`, nested forms, validation error hiding the real problem, `rendered`/`disabled` evaluating false during decode, `@RequestScoped` bean with dataTable, onclick returning false, `ViewExpiredException` hidden by error page.

### Target Unreachable / PropertyNotFoundException
Top causes: missing `@Named`, bean name mismatch, wrong scope import (mixing packages), missing `beans.xml`, nested property is null, getter/setter naming mismatch.

### ViewExpiredException
Top causes: session expired, state saving misconfigured, multiple tabs exhausting server-side view limit, browser back button after logout; increase `com.sun.faces.numberOfLogicalViews` (Mojarra) or `org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION` (MyFaces), or use client-side state saving.

### Validation Error: Value is not valid (UISelectOne/UISelectMany)
Top causes: missing `equals()`/`hashCode()` on the select item object, list changed between render and postback (use `@ViewScoped`), missing or broken `@FacesConverter`, converter not symmetric (`getAsObject(getAsString(x)).equals(x)` must hold).

### UIInput value not updated / setter not called
Top causes: another field has validation error (ALL setters are skipped), input not inside `UIForm`, `disabled="true"`, `<f:ajax execute="...">` / `<p:ajax process="...">` doesn't include the input, getter creates new object each call.

### Component not found for update/render
Top causes: component in different NamingContainer (use full client ID with `:`), target has `rendered="false"` (update wrapper), inside composite component (`NamingContainer`), typo in ID.

## Third-Party Libraries

When this project includes PrimeFaces, consult `.claude/faces/topics/primefaces.md` for PrimeFaces-specific rules.

When this project includes OmniFaces, or the developer wants to simplify code, consult `.claude/faces/topics/omnifaces.md` for OmniFaces utilities that replace common boilerplate.

## References

Always consult the set matching the project's runtime version, not merely the newest.

Source code (all versions, select the branch for the version at hand):

- API + spec: https://github.com/jakartaee/faces
- Mojarra (impl): https://github.com/eclipse-ee4j/mojarra
- MyFaces (impl): https://github.com/apache/myfaces

### Faces 4.0 (Jakarta EE 10)

- Spec: https://jakarta.ee/specifications/faces/4.0/jakarta-faces-4.0
- Java API: https://jakarta.ee/specifications/faces/4.0/apidocs/
- VDL (tag docs): https://jakarta.ee/specifications/faces/4.0/vdldoc/
- JS API: https://jakarta.ee/specifications/faces/4.0/jsdoc/

### Faces 4.1 (Jakarta EE 11)

- Spec: https://jakarta.ee/specifications/faces/4.1/jakarta-faces-4.1
- Java API: https://jakarta.ee/specifications/faces/4.1/apidocs/
- VDL (tag docs): https://jakarta.ee/specifications/faces/4.1/vdldoc/
- JS API: https://jakarta.ee/specifications/faces/4.1/jsdoc/

### Faces 5.0 (Jakarta EE 12, in progress)

Unreleased — there is no published spec, javadoc or VDL doc. Read the source instead:

- API + spec: https://github.com/jakartaee/faces, branch `5.0` (spec asciidoc under `spec/src/main/asciidoc`, API under `api/src/main/java`, XSDs under `api/src/main/xsd`, TCK under `tck/faces50`)
- Mojarra: https://github.com/eclipse-ee4j/mojarra, branch `5.0` (`main` is the 4.1 line)
- MyFaces: https://github.com/apache/myfaces, branch `main`

An API present on these branches is NOT available to a project on 4.x; see "Version Discipline".
