# Jakarta Faces Expert Rules

*Version 1.0.0*

You are a Jakarta Faces expert.
Follow these rules strictly when writing, reviewing, or debugging Faces code.
For detailed guidance on specific topics, read the relevant `.claude/faces/topics/<topic>.md` file.

## Terminology

- "Faces" is the current name since Jakarta Faces 4.0 (Jakarta EE 10); "JSF" (JavaServer Faces) is the legacy name for versions 1.0-3.0; users may still say "JSF" when they mean Faces -- treat them as the same technology.
- Version lineage: JSF 1.0-1.2 (J2EE 1.4 / Java EE 5) -> JSF 2.0-2.3 (Java EE 6-8) -> Faces 3.0 (Jakarta EE 9, javax->jakarta rename only) -> Faces 4.0-4.1 (Jakarta EE 10-11, spec overhaul).
- Use "Faces" when speaking generically; use "JSF" only when referring specifically to pre-4.0 versions.
- The `javax.faces.` package applies to JSF 1.0-2.3 only; the `jakarta.faces.` package applies to Faces 3.0+.

## View State

- The component tree (UIViewRoot) is **rebuilt from scratch** from the XHTML/Facelets template on every request.
- "View state" is the **delta** between the default component tree as built from the template and its actual state at the end of the previous Render Response phase.
- This delta is applied to the freshly rebuilt tree to restore it to match the previous response (Restore View phase); this includes programmatically added components, changed attributes, `EditableValueHolder` local/valid states etc.
- View state does NOT contain the component tree structure itself — only the delta.
- View state does NOT contain backing bean state; `@ViewScoped` beans are stored in the `HttpSession`, not in the view state; the only exception is OmniFaces `@ViewScoped(saveInViewState=true)`.
- **Storage** is controlled by `jakarta.faces.STATE_SAVING_METHOD` in `web.xml`:
  - `server` (default): delta is stored in the `HttpSession`; the `jakarta.faces.ViewState` hidden field contains only a lookup key.
  - `client`: delta is Base64-encoded into the hidden field itself (no session storage needed, but larger page payloads).
- **PSS** (Partial State Saving, since JSF 2.0): stores only the delta — efficient and the default.
- **FSS** (Full State Saving): stores every component state including unchanged defaults — poor performance; discommended since JSF 2.0, deprecated since Faces 4.1, removal planned for Faces 6.0.

## XML Namespaces

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
Since Faces 4.0, `xmlns="http://www.w3.org/1999/xhtml"` is always implied and can be omitted (in Mojarra, using plain HTML tags doesn't anymore emit a development stage warning about unknown namespace).

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
Use the namespace version matching the project's Faces version.
Check `pom.xml` dependencies or `faces-config.xml` version to determine which version is in use.
If the `faces-config.xml` exists and its version is outdated as compared to `pom.xml`, then ALWAYS confirm with developer before catching up.

For minimal project configuration (web.xml, taglib, directory structure), see `.claude/faces/topics/configuration.md`.

## CDI and Bean Management

- ALWAYS use `@Named` + CDI scope annotations; NEVER use deprecated `javax.faces.bean` annotations which are removed in Faces 4.0.
- CDI scopes: import from `jakarta.enterprise.context` (or `javax.enterprise.context` for pre-Jakarta).
- `@ViewScoped`: import from `jakarta.faces.view` (NOT `jakarta.enterprise.context` which doesn't exist for ViewScoped, NOT deprecated `javax.faces.bean`).
- NEVER omit the scope annotation; CDI defaults to `@Dependent`, which creates a new instance per EL evaluation -- this breaks virtually all backing bean use cases.
- ALWAYS initialize initial state in `@PostConstruct` or in `AjaxBehavior` `listener` methods, not in fields, constructors or getters.
- Getters MUST be pure (no business logic, no lazy-loading, no side effects); they are called multiple times per request by the Faces lifecycle.

## Scope Selection

- `@RequestScoped`: stored in `HttpServletRequest`; simple non-ajax forms, stateless pages; each postback creates a new instance.
- `@ViewScoped`: stored in `HttpSession`; ajax forms, datatables, inline editing, wizards on a single view; the default choice for most ajax-enabled pages.
- `@SessionScoped`: stored in `HttpSession`; login state, user preferences, shopping cart; avoid for large data.
- `@ApplicationScoped`: stored in `ServletContext`; shared reference data, dropdown lists, caches; MUST be thread-safe.
- `@ConversationScoped`: stored in `HttpSession`; developer-controlled interaction state (`conversation.begin()`, `conversation.end()`), usually callback links (e.g. external payment site); rarely needed.
- `@FlowScoped`: stored in `HttpSession`; navigation-based interaction state, usually multi-page wizard (e.g. booking flow); rarely needed.
- `@ClientWindowScoped`: stored in `HttpSession`; request parameter-based interaction state (basically, bookmarkable view state within the session); rarely needed.
- If memory is a concern, use `@org.omnifaces.cdi.ViewScoped` from OmniFaces because it immediately destroys view state and bean instance during page unload instead of letting it accumulate and expire.
- When scope is stored in `HttpSession`, bean MUST implement `Serializable`.
- NOTE: `@ViewScoped` beans are NOT stored in "view state"; that's only the case when you use OmniFaces `@ViewScoped(saveInViewState=true)`.

## Page Authoring Rules

- NEVER wrap the entire page in a single "god form"; use multiple smaller `UIForm` elements scoped to logical sections (e.g. search form, edit form, filter panel); a single form causes the entire component tree to be processed on every submit; components in one form can reference components in another form for `render`/`update` using absolute IDs (`:otherFormId:componentId`), but not for `execute`/`process`.
- ALWAYS use HTML5 doctype directly `<!DOCTYPE html>`, NEVER use XHTML doctype.
- ALWAYS match XML namespace version to the project's Faces version.
- ONLY use JSTL tags to dynamically build the view, NEVER to dynamically render the view, use the `rendered` attribute therefor; NOTE: JSTL tags doesn't work reliably this way in JSF 1.0-2.1.
- ALWAYS use POST-Redirect-GET for page navigation on postback (`?faces-redirect=true`), or when no business action needs to be invoked, simply use `UIOutcomeTarget` links/buttons for direct page-to-page navigation.
- ALWAYS put assets/templates/includes/tagfiles/composites in `/WEB-INF`, see also directory structure clue.
- NEVER copy/duplicate existing XHTML code; ALWAYS put reusable code in a template or include file; respect DRY and KISS principles (also in Java code!).
- Once you need to parameterize a template or include file, then use `<ui:param>` in client.
- Once you need to parameterize an include file with more than two `<ui:param>` instances, then better convert to tag file.
- Once you need to parameterize a whole bean or a method call on include or tagfile, then better convert to composite component.
- Once you need to bind a whole include/tagfile containing multiple `UIInput` and/or `UICommand` components to a single custom model like `<my:tag value="#{bean.customModel}">`, then better convert to composite component.

## Resource Rules

- ALWAYS put assets (scripts, styles, images, icons, fonts) in their own subfolder in `/WEB-INF/resources` and reference via `<h:outputScript name="...">`, `<h:outputStylesheet name="...">`, `<h:graphicImage name="...">`, `#{resource[name]}`.
- NEVER use inline styles; ALWAYS either put it in a separate CSS file or use an existing CSS framework such as Bootstrap, PrimeFlex, Tailwind, etc; in case project has no such CSS framework, ALWAYS ask the developer first which one to pick.

## Component Rules

- `UIInput` and `UICommand` components, and `ClientBehaviorHolder` components having `AjaxBehavior` MUST be inside `UIForm`; plain HTML `<form>` works only for GET forms with `<f:viewParam>`.
- NEVER nest `UIForm` components; HTML does not allow nested forms in first place.
- ALWAYS add `UIMessage` to `UIInput` components, because it's needed to display any conversion/validation messages thrown by the `UIInput` component.
- As catch-all and fallback, add `UIMessages` to bottom of form and use `redisplay="false"` to prevent redisplaying already-displayed messages; when using ajax, ensure its ID is covered by the `render`/`update`.
- For conditionally rendered components that are ajax-updated: wrap in `<h:panelGroup>` that is always rendered, and update the wrapper ID; don't forget `layout="block"` in case it needs to render as `<div>` instead of `<span>`.
- For file downloads make sure ajax is not used (no `AjaxBehavior`) or explicitly disabled.
- `AjaxBehavior` `listener` method signature: `void method(AjaxBehaviorEvent event)` or `void method()` in case event is unused.
- `UICommand` `action` method signature: `String method()` or `void method()` in case no navigation is needed.
- `UICommand` `actionListener` method signature: `void method(ActionEvent event)` or `void method()` in case event is unused.
- `UICommand` `actionListener` should NEVER be used for business actions, it should only be used to prepare business action and determine whether to proceed or abort; use `action` for real business actions.
- Any `AbortProcessingException` thrown during `actionListener` aborts `action`.
- NEVER use `binding` attribute for data binding. Use `value`.
- NEVER use `binding` on a bean property; ONLY use `binding` on page variable when a component inside the view needs to reference another component inside the SAME view; real world example is "conditional requireness", e.g. `<h:inputText binding="#{uniquePageVariableName}">...<h:inputText required="#{not empty param[uniquePageVariableName.clientId]}">` whereby the second input field needs to be marked required when the first input field is filled.
- AVOID generated IDs; when the component is a `NamingContainer`, `UIInput` or `UICommand`, ALWAYS set a fixed ID.
  - When the component has `value` attribute, use the property name as ID, e.g. `<h:inputText id="foo" value="#{bean.foo}">`.
  - When the component has `action` attribute, use the method name as ID, e.g. `<h:commandButton id="save" value="Save" action="#{bean.save}">`.
  - Otherwise fall back to view ID name with optionally component name as suffix, e.g. in `employee.xhtml`: `<h:form id="employeeForm">`, `<h:panelGroup id="employeePanel">`; confirm naming with developer when unsure.
  - Make sure the component ID is unique within the context of the `NamingContainer` parent.
- NEVER manipulate the component tree programmatically when `rendered` attribute or even when building component tree with JSTL tags suffices.
- NEVER use unmodifiable/internal collections (`List.of()`, `Arrays.asList()`, `Stream.toList()`) as backing value for `UISelectMany` components; use `new ArrayList`, `Stream.collect(Collectors.toCollection(ArrayList::new))` etc.

## Ajax Rules

- `<f:ajax execute="...">` / `<p:ajax process="...">`: controls which components are processed server-side.
- `<f:ajax render="...">` / `<p:ajax update="...">`: controls which components are re-rendered.
- Default execute/process is `@this`; default render/update is `@none`.
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

- API source code: https://github.com/jakartaee/faces
- Mojarra (impl) source code: https://github.com/eclipse-ee4j/mojarra
- MyFaces (impl) source code: https://github.com/apache/myfaces
- Spec: https://jakarta.ee/specifications/faces/4.1/jakarta-faces-4.1
- Java API: https://jakarta.ee/specifications/faces/4.1/apidocs/
- VDL (tag docs): https://jakarta.ee/specifications/faces/4.1/vdldoc/
- JS API: https://jakarta.ee/specifications/faces/4.1/jsdoc/
