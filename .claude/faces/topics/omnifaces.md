# OmniFaces

*Version 1.0.0*

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

## SelectItemsConverter

Use for any `UISelectOne`/`UISelectMany` with complex objects as values.
Eliminates the need for a custom `@FacesConverter` that re-fetches entities from the database.

- Converter ID: `omnifaces.SelectItemsConverter` or tag `<o:selectItemsConverter />`.
- Requires a good `toString()` implementation on the entity (must uniquely identify instances, e.g. `"Entity[id=123]"`).
- Entity must also have proper `equals()` and `hashCode()`.
- Alternative: `SelectItemsIndexConverter` converts by list position instead of `toString()`.

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

## <o:validateBean>

Enables bean validation control per component and class-level (cross-field) validation.

- Per-component: nest in `UICommand` or `UIForm` with `validationGroups` attribute.
- Class-level validation: nest in `UIForm` with `value="#{bean}"` to validate the whole bean.
- Supports `@jakarta.validation.Valid` for cascading validation on nested properties.

## Multi-Field Validators

Validate multiple related input fields together.

- **`<o:validateEqual>`**: all fields must have same value (e.g. password confirmation); usage: `<o:validateEqual components="password1 password2" />`.
- **`<o:validateMultiple>`**: custom validator method or `MultiFieldValidator` bean; method signature: `boolean method(FacesContext, List<UIInput>, List<Object>)`; usage: `<o:validateMultiple components="foo bar baz" validator="#{bean.method}" />`.
- `message` attribute to customize error message.
- `components` attribute: space-separated component IDs.

## Utility Classes

### Faces (org.omnifaces.util.Faces)
Flattens the Faces API hierarchy into static one-liners.
Use instead of chaining `FacesContext.getCurrentInstance().getExternalContext().get...()`.
Key methods: `getContext()`, `getApplication()`, `redirect()`, `sendFile()`, `getLocale()`, `isAjaxRequest()`, `getViewId()`, `navigate()`.
API docs: https://omnifaces.org/docs/javadoc/5.1/org.omnifaces/org/omnifaces/util/Faces.html

### Messages (org.omnifaces.util.Messages)
Convenience methods for Faces messages.
Key methods: `addError()`, `addGlobalError()`, `addFlashGlobalInfo()`, `throwValidatorException()`.
Builder pattern: `Messages.create().detail().error().add()`.
API docs: https://omnifaces.org/docs/javadoc/5.1/org.omnifaces/org/omnifaces/util/Messages.html

### Servlets (org.omnifaces.util.Servlets)
Utility methods for servlet API, usable in filters/listeners without FacesContext.
Key methods: `setCacheHeaders()`, `isFacesAjaxRequest()`, `isFacesResourceRequest()`, `facesRedirect()`.
API docs: https://omnifaces.org/docs/javadoc/5.1/org.omnifaces/org/omnifaces/util/Servlets.html

## References

- Source code: https://github.com/omnifaces/omnifaces
- Java API: https://omnifaces.org/docs/javadoc/current/
- VDL (tag docs): https://omnifaces.org/docs/vdldoc/current/
- Showcase with examples: https://showcase.omnifaces.org
 