# OmniFaces

*Version 1.1.0*

OmniFaces is a utility library for Jakarta Faces that fills gaps in the standard API.
When a project includes OmniFaces, prefer its utilities over hand-rolling equivalent solutions.

## @ViewScoped (org.omnifaces.cdi.ViewScoped)

Prefer over standard `jakarta.faces.view.ViewScoped` when:
- You need `@PreDestroy` to fire on browser unload (navigating away, closing tab); standard Faces `@ViewScoped` only calls `@PreDestroy` on session expiration.
- You want to reduce memory usage — OmniFaces immediately destroys view state and bean on page unload via `navigator.sendBeacon`.

Configuration/gotchas:
- `saveInViewState=true` stores the bean in Faces view state (client-side state saving only); only use this for form state control (disabled, readonly, rendered attributes), never for data collections; with this mode, beans never expire and `@PreDestroy` is never invoked.
- Max active view scopes configurable via context parameter (default 20).
- For non-ajax download links, use `OmniFaces.Unload.disable()` or set the `download` attribute on the link to prevent unload event from firing.

## Converters

OmniFaces provides several converters that fill gaps in the standard API.

- **`<o:selectItemsConverter>`** (`omnifaces.SelectItemsConverter`): use for any `UISelectOne`/`UISelectMany` with complex objects as values; eliminates the need for a custom `@FacesConverter` that re-fetches entities from the database. Requires a good `toString()` (must uniquely identify instances, e.g. `"Entity[id=123]"`) and proper `equals()`/`hashCode()`.
- **`<o:selectItemsIndexConverter>`** (`omnifaces.SelectItemsIndexConverter`): alternative to `SelectItemsConverter` that converts by list position instead of `toString()`; use when `toString()` is not suitable as unique identifier.
- **`<o:listConverter>`** (`omnifaces.ListConverter`): like `SelectItemsConverter` but works without `<f:selectItems>` — resolves values from a provided `List` attribute instead. Also requires `toString()`/`equals()`/`hashCode()`.
- **`<o:listIndexConverter>`** (`omnifaces.ListIndexConverter`): position-based variant of `ListConverter`.
- **`<o:genericEnumConverter>`** (`omnifaces.GenericEnumConverter`): automatically detects enum type in generic collections (e.g. `List<MyEnum>`); use on `UISelectMany` bound to enum collections where the standard `EnumConverter` fails because EL cannot resolve the generic type.
- **`<o:implicitNumberConverter>`**: extends `<f:convertNumber>` to automatically drop/imply currency and percent symbols on `UIInput`; users type `100` instead of `$100` while the display still shows the formatted value.
- **`<o:trimConverter>`** (`omnifaces.TrimConverter`): trims leading/trailing whitespace from submitted values.
- **`<o:toLowerCaseConverter>`** (`omnifaces.ToLowerCaseConverter`): converts submitted values to lower case based on current locale.
- **`<o:toUpperCaseConverter>`** (`omnifaces.ToUpperCaseConverter`): converts submitted values to upper case based on current locale.
- **`<o:toCollectionConverter>`** (`omnifaces.ToCollectionConverter`): converts between delimited `String` and `Collection`; attributes: `delimiter` (split pattern), `collectionType` (FQCN of target collection), `itemConverter` (converter for individual items).
- **`<o:compositeConverter>`**: chains multiple converters on a single component (standard Faces only allows one). `converterIds` attribute: comma-separated list of converter IDs. `getAsObject()` executes left-to-right; `getAsString()` executes in reverse order to maintain symmetry.
  ```xml
  <h:inputText value="#{bean.product}">
      <o:compositeConverter converterIds="trimConverter,sanitizeConverter,productConverter" />
  </h:inputText>
  ```
- **`ValueChangeConverter`**: abstract base class — extend and override `getAsChangedObject()` instead of `getAsObject()` to skip conversion when the submitted value hasn't changed; use for converters that perform expensive operations (e.g. database lookups) on `@ViewScoped` beans.

## FullAjaxExceptionHandler

Use to get consistent exception handling for both ajax and non-ajax requests.
Standard Faces shows nothing useful for ajax exceptions in Production stage.

- Register in `faces-config.xml`:
  ```xml
  <factory>
      <exception-handler-factory>org.omnifaces.exceptionhandler.FullAjaxExceptionHandlerFactory</exception-handler-factory>
  </factory>
  ```
- Error pages in `web.xml` MUST be Facelets files (not JSP) and URL must match FacesServlet mapping.
- Either HTTP 500 error page or `java.lang.Throwable` error page must be present.
- Each exception gets a UUID for log correlation.

## @Eager

Causes scoped beans to be instantiated eagerly at the start of their scope instead of lazily on first EL reference.

- Supported scopes: `@RequestScoped`, `@ViewScoped` (both Faces and OmniFaces), `@SessionScoped`, `@ApplicationScoped`.
- For `@RequestScoped`/`@ViewScoped`, specify `requestURI` or `viewId` attribute to control when.
  - `requestURI`: instantiation before first servlet filter (FacesContext NOT available in `@PostConstruct`).
  - `viewId`: instantiation during Restore View phase (FacesContext available).
- For `@ApplicationScoped`: use `@org.omnifaces.cdi.Startup` stereotype as shorthand for `@Eager @ApplicationScoped`.

## @RateLimit / RateLimiter

Thread-safe sliding window rate limiting per client identifier.
Available since OmniFaces 5.0 (so Faces 4.1 required).

- Usage: `@RateLimit(clientId="FooAPI", maxRequestsPerTimeWindow=10, maxRetries=3)`.
- Throws `RateLimitExceededException` when limit exceeded.
- Can also inject `RateLimiter` bean and call `checkRateLimit()` directly.
- Supports custom identifiers (IP, user ID, API key).

## <o:graphicImage>

Use to render `InputStream` or `byte[]` bean properties directly as images without a separate servlet.

- Image property MUST be in a `@GraphicImageBean` or `@ApplicationScoped` bean (stateless).
- `dataURI="true"`: renders inline in HTML; only for small images/icons (~10KB max) as browser can't cache these.
- `lazy="true"` for lazy loading.
- Supports `lastModified` attribute for browser cache headers (ETag/Last-Modified).

## <o:form>

Extends `<h:form>` to submit to the exact request URI including query string, preserving path/query parameters.

- `useRequestURI` (default true): submits to request URI with query string instead of view ID.
- `includeRequestParams` (default false): includes request parameters as hidden fields.
- `partialSubmit` (default true): in ajax requests, sends only form data needed per execute attribute (reduces payload on large forms).
- Essential when using FacesViews or when beans depend on query parameters.

## FacesViews / MultiViews

Extensionless URLs with zero or minimal configuration.

- **Zero config**: place Facelets in `/WEB-INF/faces-views/` directory.
- **Minimal config**: set scan path context parameter `org.omnifaces.FACES_VIEWS_SCAN_PATHS` to `/*.xhtml`.
  - URLs with `.xhtml` extension are 301-redirected to extensionless versions.
- **MultiViews**: suffix scan path context parameter with `/*` (e.g. `/*.xhtml/*`) to capture path parameters.
  - URL `/foo/bar/baz` where `foo.xhtml` exists forwards to `foo.xhtml` with `bar`, `baz` as path parameters injectable via `@Inject @Param(pathIndex=0)`.

## <o:pathParam>

Use with MultiViews to render URL path segments in outcome target components.

- Nested in `UIOutcomeTarget` components (`<h:link>`, `<h:button>`, etc).
- Parameters rendered in declaration order as path segments (e.g. `/item/123/456`).
- Only renders when target view is defined as MultiViews.

## <o:viewParam>

Extends `<f:viewParam>` with fixes for common issues.

- **Stateless by default**: converter and validators NOT called on postbacks (avoids unnecessary re-evaluation).
- Provides `default` attribute for default value when parameter is null/empty.
- Null model values don't produce empty query string parameters.
- Triggers bean validation on missing parameters.

## CacheControlFilter

Controls HTTP cache headers (Cache-Control, Expires, Pragma).

- **Default (no `expires` param)**: sets no-cache headers — prevents ViewExpiredException on browser back button.
- **`expires` param**: number with optional suffix (`s`, `m`, `h`, `d`, `w`); e.g. `10m`, `1d`, `6w`.
- Disabled in Development stage.
- Auto-skips Faces resources (handled by `ResourceHandler` whose expiration time in millis is configurable via `com.sun.faces.defaultResourceMaxAge` (Mojarra) and `org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES` (MyFaces)).
- Map multiple filters to different URL patterns for granular control of expiration time.

## CompressedResponseFilter

Compresses HTTP responses using Brotli, GZIP, or Deflate.

- Auto-selects best algorithm based on client support (prefers Brotli).
- Threshold default 150 bytes (only compress above this).
- Default MIME types: `text/*`, `application/javascript`, `application/json`, `image/svg+xml`.
- Not for binary content (images, PDFs, etc.).

## #{faces} implicit EL object

Exposes all no-argument getter methods of the `Faces` utility class in EL.

- `#{faces.development}` — is Development stage.
- `#{faces.ajaxRequest}` — is current request ajax.
- `#{faces.requestBaseURL}` — base URL of request.

## <o:importConstants>

Maps all `public static final` fields of a class/interface/enum into request scope as a map.

- `<o:importConstants type="com.example.StatusEnum" />` — accessible as `#{StatusEnum.ACTIVE}`.
- `var` attribute to override variable name.
- Since 4.6, enums support `#{EnumClass.members()}` to iterate over enum values.
- Can be placed anywhere (unlike `<f:importConstants>` which requires `<f:metadata>`).

## Validators

### <o:validateBean>

ALWAYS prefer over `<f:validateWholeBean>` when OmniFaces is available.
Enables per-component validation group control AND class-level (cross-field) bean validation with far more flexibility than the standard tags.

Per-command / per-input — nest in `UICommand` or `UIInput` to control validation groups or disable validation for that component:
```xml
<h:commandButton value="Submit" action="#{bean.submit}">
    <o:validateBean validationGroups="jakarta.validation.groups.Default,com.example.MyGroup" />
</h:commandButton>

<h:selectOneMenu value="#{bean.selectedItem}">
    <f:selectItems value="#{bean.availableItems}" />
    <o:validateBean disabled="true" />
    <f:ajax execute="@form" listener="#{bean.itemChanged}" render="@form" />
</h:selectOneMenu>
```

Class-level (cross-field) — nest in `UIForm` with `value="#{bean}"`. Can be placed anywhere inside the form (unlike `<f:validateWholeBean>` which must be the last child):
```xml
<h:inputText value="#{bean.product.item}" />
<h:inputText value="#{bean.product.order}" />
<o:validateBean value="#{bean.product}" showMessageFor="@violating" />
```

Cascading with `@Valid` (since OmniFaces 3.8) — validates the parent bean and cascades into annotated nested objects:
```xml
<o:validateBean value="#{bean}" />
```
```java
@Valid
private Product product;
```

Attributes:
- **`value`**: the bean to validate at class level.
- **`validationGroups`**: comma-separated validation groups to activate.
- **`disabled`**: disables bean validation for the enclosing component.
- **`method`**: `validateCopy` (default, validates on a copy) or `validateActual` (validates the actual bean during Update Model Values — use when the bean cannot be copied; trade-off: invalid values remain in the model, check `FacesContext.isValidationFailed()` in the action method).
- **`copier`**: custom `org.omnifaces.util.copier.Copier` implementation. Default tries in order: `Cloneable` -> `Serializable` -> copy constructor -> no-arg constructor.
- **`showMessageFor`**: `@form` (default), `@all`, `@global`, `@violating` (maps messages to the specific violating input components), or space-separated client IDs.
- **`messageFormat`** (since OmniFaces 3.12): custom format string; `{0}` = error message, `{1}` = field labels.

### <o:requiredCheckboxValidator>

Fixes broken `required="true"` on `<h:selectBooleanCheckbox>` — standard Faces coerces unchecked to `Boolean.FALSE` before validation, so the required check always passes. Validator ID: `omnifaces.RequiredCheckboxValidator`. Message priority: `requiredMessage` attribute > custom message bundle > default `"{0}: a tick is required"`.

### ValueChangeValidator

Abstract base class — extend and override `validateChangedObject(FacesContext, UIComponent, T)` instead of `validate()` to skip validation when the submitted value hasn't changed. Use for validators that perform expensive operations (e.g. database uniqueness checks) on `@ViewScoped` beans. Companion to `ValueChangeConverter`.

### Multi-Field Validators

Validate multiple related input fields together. All share these common attributes:
- `components`: space-separated component IDs of the `UIInput` fields to validate together.
- `message`: custom error message; `{0}` is replaced with comma-separated field labels.
- `showMessageFor`: `@this` (default, on the validator tag), `@all` (all referenced components), `@invalid` (only invalid ones), or specific client IDs.
- `invalidateAll`: when `false`, marks only the actually-invalid fields instead of all referenced fields (default `true`).
- `disabled`: EL expression to conditionally skip validation.

Available validators:
- **`<o:validateAll>`**: ALL fields must be filled out.
- **`<o:validateAllOrNone>`**: either ALL fields are filled out, or NONE are.
- **`<o:validateOne>`**: exactly ONE field must be filled out.
- **`<o:validateOneOrMore>`**: at least ONE field must be filled out.
- **`<o:validateOneOrNone>`**: at most ONE field may be filled out (zero is also OK).
- **`<o:validateEqual>`**: all fields must have the same value (e.g. password confirmation).
- **`<o:validateUnique>`**: all fields must have unique values (no duplicates).
- **`<o:validateOrder>`**: field values must be in order; `type` attribute: `lt` (default, strictly ascending), `lte` (ascending, allows equal), `gt` (strictly descending), `gte` (descending, allows equal); values must implement `Comparable`.
- **`<o:validateMultiple>`**: custom validator logic; specify a `validator` method or `MultiFieldValidator` bean; method signature: `boolean method(FacesContext, List<UIInput>, List<Object>)`; usage: `<o:validateMultiple components="foo bar baz" validator="#{bean.method}" />`.

## Utility Classes

### Faces (org.omnifaces.util.Faces)
Flattens the Faces API hierarchy into static one-liners.
Use instead of chaining `FacesContext.getCurrentInstance().getExternalContext().get...()`.
Key methods: `getContext()`, `redirect()`, `sendFile()`, `getLocale()`, `isAjaxRequest()`, `getViewId()`.
API docs: https://omnifaces.org/docs/javadoc/current/org.omnifaces/org/omnifaces/util/Faces.html

### Messages (org.omnifaces.util.Messages)
Convenience methods for Faces messages.
Key methods: `addError()`, `addGlobalError()`, `addFlashGlobalInfo()`, `throwValidatorException()`.
Builder pattern: `Messages.create().detail().error().add()`.
API docs: https://omnifaces.org/docs/javadoc/current/org.omnifaces/org/omnifaces/util/Messages.html

### Servlets (org.omnifaces.util.Servlets)
Utility methods for servlet API, usable in filters/listeners without FacesContext.
Key methods: `setCacheHeaders()`, `isFacesAjaxRequest()`, `isFacesResourceRequest()`, `facesRedirect()`.
API docs: https://omnifaces.org/docs/javadoc/current/org.omnifaces/org/omnifaces/util/Servlets.html

## References

- Source code: https://github.com/omnifaces/omnifaces
- Java API: https://omnifaces.org/docs/javadoc/current/
- VDL (tag docs): https://omnifaces.org/docs/vdldoc/current/
- Showcase with examples: https://showcase.omnifaces.org
 