# Conversion and Validation

*Version 1.8.0*

Conversion and validation are central to the Faces MVC lifecycle.
Converters transform between String (HTTP) and Object (model); validators enforce business rules on the converted value.
Both run during Process Validations (phase 3), or during Apply Request Values (phase 2) when `immediate="true"`.

## How Converters Work

- Every `ValueHolder` component (both `UIOutput` and `UIInput`) can have a `Converter`.
- During **Render Response**: the converter's `getAsString()` converts the model value to a String for HTML output.
- During **Process Validations**: the converter's `getAsObject()` converts the submitted String back to the model type.
- A converter MUST be symmetric: `getAsObject(getAsString(x)).equals(x)` must hold; if not, `UISelectOne`/`UISelectMany` will fail with "Validation Error: Value is not valid".

## Implicit vs Explicit Converters

- Most standard Java types have **implicit** converters that activate automatically based on the model property type. The complete standard by-type list is `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`, `BigDecimal`, `BigInteger`, `Character`, `Boolean`, `Enum`, and `UUID` (the last since Faces 4.1) — nothing else.
- These require NO configuration; just bind the value to a bean property of that type and Faces handles conversion.
- Note `java.util.Date` and the `java.time.*` types (`LocalDate`, `LocalDateTime`, etc.) are NOT in that implicit list — an input bound to such a property with no explicit `<f:convertDateTime>` renders fine (via `toString()`) but throws a normal `ConverterException` on submit, since no converter is found for the target type. ALWAYS attach `<f:convertDateTime>`.
- Only `<f:convertNumber>` and `<f:convertDateTime>` require **explicit** registration because the desired conversion algorithm isn't necessarily obvious from the model type alone (e.g. number pattern, date format, locale).
- **Generic collection gotcha**: EL cannot detect the parameterized type of a generic collection (`List<Integer>` returns `Object.class` from `ValueExpression#getType()`). When editing items in a collection via `<ui:repeat>` or `<h:dataTable>`, you must explicitly specify the converter:
  ```xml
  <ui:repeat value="#{bean.integers}" varStatus="loop">
      <h:inputText value="#{bean.integers[loop.index]}" converter="jakarta.faces.Integer" />
  </ui:repeat>
  ```

## `<f:convertNumber>`

- Uses `java.text.NumberFormat` under the covers.
- `type` attribute: `number` (default), `currency`, `percent`.
- `pattern` attribute: overrides `type` with a `java.text.DecimalFormat` pattern (e.g. `"#,##0.00"`).
- `locale` attribute: defaults to `UIViewRoot#getLocale()`.
- When used on `UIInput` for currency: set `currencySymbol=""` to avoid requiring the user to type the symbol.
- For prices, use `BigDecimal` (not `Double`/`Float`) to avoid floating-point arithmetic errors.

## `<f:convertDateTime>`

- Uses `java.text.DateFormat` (for `java.util.Date`) and `java.time.format.DateTimeFormatter` (for `java.time` types, since JSF 2.3).
- The `type` attribute MUST match the model property type:
  - `date` (default) -> `java.util.Date`
  - `time` -> `java.util.Date`
  - `both` -> `java.util.Date`
  - `localDate` -> `java.time.LocalDate`
  - `localTime` -> `java.time.LocalTime`
  - `localDateTime` -> `java.time.LocalDateTime`
  - `offsetTime` -> `java.time.OffsetTime`
  - `offsetDateTime` -> `java.time.OffsetDateTime`
  - `zonedDateTime` -> `java.time.ZonedDateTime`
- Always specify the `pattern` attribute when the end user needs to enter the value, to avoid locale-dependent ambiguity.
- The `timeZone` attribute defaults to **`GMT`**, NOT the view locale's zone; set it explicitly whenever the displayed/parsed time must reflect a specific zone.
- `dateStyle`/`timeStyle` (localized `FormatStyle`) apply only to full date/time types; they have no effect on partial temporals (`year`, `yearMonth`, `monthDay`), which are ISO/pattern-only.
- Combine with HTML5 input types via pass-through attributes for native date pickers:
  ```xml
  <h:inputText id="date" a:type="date" value="#{bean.localDate}">
      <f:convertDateTime type="localDate" pattern="yyyy-MM-dd" />
  </h:inputText>
  ```

## Custom Converters

- Implement `jakarta.faces.convert.Converter<T>` with `getAsString()` and `getAsObject()`.
- Register with `@FacesConverter`:
  - `forClass=Entity.class`: activates **implicitly** for any `ValueHolder` bound to that type (no `converter` attribute needed in the view).
  - `value="entityConverter"`: activates **explicitly** via `converter="entityConverter"` in the view.
  - `forClass` takes precedence when both the type matches and an explicit converter ID is absent.
- **CDI injection** (`@Inject`) in converters:
  - JSF 2.3 through Faces 4.1: requires `managed=true` on `@FacesConverter`; without it the converter is not CDI managed and every injected field stays null.
  - Faces 5.0: all converters are CDI managed, so injection works unconditionally and the `managed` attribute is deprecated for removal and ignored. Keep it on code that must also run on 4.x, where it is still load-bearing.
  - When OmniFaces is present, its `ConverterManager` additionally makes every `@FacesConverter` annotated class WITHOUT `managed=true` eligible for injection, and `<o:converter>` requires `managed=true` to be absent — see "Deferred Attributes" under Custom Validators below.
    - A `forClass` converter is the exception: it is only eligible when the class declares no public `Class` argument constructor.
- `getAsString()` must return `""` (empty string) for null objects, not `null`.
- `getAsObject()` must return `null` for null/empty strings, not throw an exception.
- On conversion failure, throw `ConverterException(new FacesMessage(...))`.
- **Chaining multiple converters**: standard Faces only supports one converter per component. When OmniFaces is available, use `<o:compositeConverter>` to declaratively compose multiple converters in sequence (e.g. trim -> sanitize -> toEntity):
  ```xml
  <h:inputText value="#{bean.product}">
      <o:compositeConverter converterIds="trimConverter,sanitizeConverter,productConverter" />
  </h:inputText>
  ```
  - `getAsObject()` (submission): converters execute left-to-right; each converter's output feeds the next.
  - `getAsString()` (rendering): converters execute in **reverse** order to maintain symmetry.
  - See `.claude/faces/topics/omnifaces.md` for full documentation.

```java
@FacesConverter(forClass = Product.class, managed = true)
public class ProductConverter implements Converter<Product> {

    @Inject
    private ProductService productService;

    @Override
    public String getAsString(FacesContext context, UIComponent component, Product product) {
        if (product == null) {
            return "";
        }
        return product.getId().toString();
    }

    @Override
    public Product getAsObject(FacesContext context, UIComponent component, String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return productService.findById(Long.valueOf(id));
    }
}
```

## Standard Validators

Validators run AFTER successful conversion. Multiple validators on a single component all execute independently, regardless of each other's outcome.

- `<f:validateLongRange minimum="..." maximum="...">`: validates `Number` value is within range (attributes are Long).
- `<f:validateDoubleRange minimum="..." maximum="...">`: validates `Number` value is within range (attributes are Double).
- `<f:validateLength minimum="..." maximum="...">`: validates `String` length (calls `toString()` first).
- `<f:validateRegex pattern="...">`: validates `String` matches regex (the value MUST be `String`).
- `<f:validateRequired>`: marks component required; primarily exists for composite components using `<cc:editableValueHolder>` — for regular components, just use the `required` attribute.
- `<f:validateBean>` / `<f:validateWholeBean>`: controls Bean Validation integration (see below).

## Bean Validation Integration

- Faces automatically detects the Bean Validation API on the classpath and transparently processes all Bean Validation constraints (`@NotNull`, `@Size`, `@Pattern`, `@Email`, etc.) during Process Validations, after Faces' own validators.
- On full EE servers, Bean Validation is always available; on bare servlet containers (Tomcat, Jetty), add Hibernate Validator dependency.
- **`@NotNull` requires `INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL=true`** in `web.xml`; otherwise empty strings slip through as `""` instead of `null` and `@NotNull` never triggers.
- Disable Bean Validation globally: `jakarta.faces.validator.DISABLE_DEFAULT_BEAN_VALIDATOR=true` in `web.xml`.
- Fine-grained control with `<f:validateBean>`:
  - `<f:validateBean disabled="true">` wrapping inputs disables Faces-managed Bean Validation on those inputs.
  - `<f:validateBean validationGroups="...">` restricts which validation groups are processed.
  - NOTE: this only disables Faces-managed Bean Validation; JPA-managed Bean Validation still runs independently.

## Class-Level (Cross-Field) Validation

- Bean Validation normally validates individual fields, but class-level constraints (e.g. "start date before end date") need all field values present.
- This conflicts with the lifecycle: model values are only SET in phase 4 (Update Model Values), but validation runs in phase 3.
- Both `<f:validateWholeBean>` (standard, since JSF 2.3) and `<o:validateBean>` (OmniFaces) solve this by copying the bean, populating it with the converted values, running Bean Validation on the copy, and discarding the copy.
- **When OmniFaces is available, ALWAYS prefer `<o:validateBean>` over `<f:validateWholeBean>`** — it is more flexible and powerful:
  - Works per-command (nest in `UICommand`) or per-form, not just per-form.
  - Supports `@jakarta.validation.Valid` for cascading validation on nested properties.
  - `showMessageFor` attribute controls where messages appear: `@form` (default), `@all`, `@global`, `@violating` (maps messages to the violating input components), or specific client IDs.
  - `method` attribute: `validateCopy` (default, validates on a copy) or `validateActual` (validates on the actual bean during Update Model Values — use when the bean cannot be copied).
  - `copier` attribute for custom bean copy strategy.
  - `messageFormat` attribute for custom message formatting.
  - No placement restriction — `<f:validateWholeBean>` MUST be the last child of `<h:form>`, but `<o:validateBean>` can be placed anywhere inside the form.
- For full `<o:validateBean>` documentation, see `.claude/faces/topics/omnifaces.md`.
- Example with `<o:validateBean>`:
  ```xml
  <h:form>
      <h:inputText value="#{booking.period.startDate}">
          <f:convertDateTime type="localDate" pattern="yyyy-MM-dd" />
      </h:inputText>
      <h:inputText value="#{booking.period.endDate}">
          <f:convertDateTime type="localDate" pattern="yyyy-MM-dd" />
      </h:inputText>
      <h:commandButton value="Submit" action="#{booking.save}" />
      <h:messages />
      <o:validateBean value="#{booking.period}" showMessageFor="@violating" />
  </h:form>
  ```
- Fallback example with `<f:validateWholeBean>` (when OmniFaces is not available):
  ```xml
  <h:form>
      <!-- ... same inputs ... -->
      <f:validateWholeBean value="#{booking.period}" />
  </h:form>
  ```
  - MUST be placed as the **last child** of `<h:form>`; the implementation may throw a runtime exception if misplaced.

## Custom Validators

- Implement `jakarta.faces.validator.Validator<T>` with `validate(FacesContext, UIComponent, T value)`.
- Register with `@FacesValidator("validatorId")`; add `managed=true` only when the class needs `@Inject`, and only after reading the OmniFaces caveat below.
- **CDI injection** (`@Inject`) in validators, same lineage as for converters:
  - JSF 2.3 through Faces 4.1: requires `managed=true` on `@FacesValidator`; without it the validator is not CDI managed and every injected field stays null.
  - Faces 5.0: all validators are CDI managed, so injection works unconditionally and the `managed` attribute is `@Nonbinding`, deprecated for removal and ignored. Keep it on code that must also run on 4.x, where it is still load-bearing.
  - When OmniFaces is present, its `ValidatorManager` additionally makes every `@FacesValidator` annotated class WITHOUT `managed=true` eligible for injection — see "Deferred Attributes" below.
- On validation failure, throw `ValidatorException(new FacesMessage(...))`.
- Activate in view via the `validator="validatorId"` attribute, a nested `<f:validator validatorId="...">` tag, or — when OmniFaces is available — `<o:validator validatorId="...">`. Registering it under `<default-validators><validator-id>` in `faces-config.xml` instead attaches it to EVERY `EditableValueHolder` in the application.
- A custom validator has NO tag attributes of its own: `<f:validator>` passes nothing but `validatorId`/`binding`/`disabled`, so configuring one per component needs a tag file, a custom `ValidatorHandler`, or `<o:validator>` below.

### Deferred Attributes: `<o:validator>` and `<o:converter>` (OmniFaces)

`<o:validator>` and `<o:converter>` extend the standard `<f:validator>`/`<f:converter>` tag family with DEFERRED value expressions in every attribute: instead of being evaluated once at view build time, each attribute is re-evaluated on every access, like a component attribute or a bean property. Prefer them over the standard tags whenever an attribute value is not a literal.

- What they buy:
  - Attribute values that vary per iteration inside `<ui:repeat>`/`<h:dataTable>`.
  - Attribute values that change between postbacks, which the standard tags freeze at the first build — see "Dynamic converter/validator config is not re-applied on postback" under Common Pitfalls.
  - Setting attributes on a CUSTOM converter/validator straight from the view, without a tag file or a custom `ConverterHandler`/`ValidatorHandler`.
  - `<o:validator>` only: a per-validator `message` attribute (any `{0}` is substituted with the input's label; ignored when the parent component already has `validatorMessage`) and a deferred `disabled` attribute. `<o:converter>` has neither — it declares only `converterId`, `binding` and `for`.
  ```xml
  <h:inputText id="amount" value="#{item.amount}">
      <o:validator validatorId="jakarta.faces.LongRange" minimum="#{item.minimum}" maximum="#{item.maximum}"
          message="#{i18n['error.amountRange']}" />
  </h:inputText>
  <h:message for="amount" />
  ```
- How the attributes are applied: OmniFaces introspects the JavaBean properties of the converter/validator INSTANCE and matches tag attributes to setters by name. An attribute with no matching setter on that instance is SILENTLY IGNORED — no exception, the artifact simply runs unconfigured. Literal values are set once when the tag is applied; EL values are re-applied right before every `getAsObject()`/`getAsString()`/`validate()` call.
- **`managed=true` MUST be absent** (every implementation except Mojarra 5.0 and newer) on a `@FacesConverter`/`@FacesValidator` class whose attributes are set by `<o:converter>`/`<o:validator>`. Both implementations hand out a WRAPPER for a managed artifact — Mojarra `com.sun.faces.cdi.CdiValidator`/`CdiConverter`, MyFaces `org.apache.myfaces.cdi.wrapper.FacesValidatorCDIWrapper`/`FacesConverterCDIWrapper` — and that wrapper carries none of the class's own setters, so by the previous bullet every attribute is dropped without a word. Symptom: the artifact behaves as if unconfigured — typically a `NullPointerException` inside `validate()`, or a check that never fires. (The OmniFaces javadoc names only Mojarra, but the MyFaces code wraps managed artifacts the same way.)
  - `managed=true` is harmless with an `<o:validator>` that sets no attributes beyond `validatorId`/`binding`/`disabled`/`message`/`for` — those are handled by OmniFaces' own deferred wrapper, not by setters on the validator.
- Removing `managed=true` does NOT cost the injection: OmniFaces installs `OmniApplication`, which routes every `Application.createConverter()`/`createValidator()` through its `ConverterManager`/`ValidatorManager`, and those resolve a `@FacesConverter`/`@FacesValidator` class without `managed=true` as a CDI bean. NEVER strip `managed=true` in a project that does not have OmniFaces on the classpath — injection then silently breaks everywhere that class is used.
  - EXCEPT for a `forClass` converter: `ConverterManager.createConverter(Application, Class)` resolves a CDI bean only when the converter class has a public no-arg constructor and DECLARES no public `Class` argument constructor. Constructors are not inherited, so a subclass of `EnumConverter` declaring only a no-arg constructor is still injected, while one that also declares `public MyConverter(Class<? extends Enum> targetClass)` is not and keeps needing `managed=true` for `@Inject`. Mojarra 5.0 is exempt because it CDI-manages every converter itself. The converter ID path carries no such guard, so `<o:converter converterId>` is unaffected either way.
  - Only ANNOTATED classes are covered. One registered in `faces-config.xml` (`<validator><validator-class>`) is CDI managed by neither mechanism.
  - CDI discovery still applies: under `bean-discovery-mode="annotated"` — the CDI 4.0 default, which an empty or absent `beans.xml` also means — the class needs a bean defining annotation, so put `@Dependent` on it rather than switching the archive to `bean-discovery-mode="all"` (discouraged since CDI 4.0).
  - A `@FacesValidator` class extending another `@FacesValidator` class that in turn extends a standard validator can throw `AmbiguousResolutionException` under `bean-discovery-mode="all"`; `@Specializes` on the subclass resolves it.
- Faces 5.0 drops the conflict on Mojarra: `managed` is ignored and `CdiUtils.createValidator()`/`createConverter()` return the CDI reference itself instead of wrapping it (the `CdiValidator`/`CdiConverter` wrappers are gone), so the setters reach the real instance either way. MyFaces 5.0 snapshots still key off `managed()` and still wrap, so keep to the 4.x rule there until that changes.
- **WARN when one converter/validator class is attached BOTH via `<o:validator>`/`<o:converter>` AND via a standard attachment** (`validator="id"`, `<f:validator>`, `converter="id"`, `<f:converter>`, `<default-validators>`): the same class then runs configured in one place and unconfigured in another.
  - It MUST be `@Dependent`. `ValidatorManager`/`ConverterManager` hand out `BeanManager.getReference(...)`, a client proxy resolving to ONE instance per scope: the `<o:validator>` attachment re-applies its setters on that instance before each `validate()`, so the standard attachment — which sets nothing — observes whatever the last `<o:validator>` call left behind. How far that reaches follows the scope: `@ApplicationScoped` shares the instance across all concurrent requests, `@SessionScoped` across one user's, `@RequestScoped`/`@ViewScoped` only within a single request or view.
  - The standard attachment leaves every attribute-backed property at its default, so the class MUST tolerate that (documented defaults, or an explicit check that throws a clear error) instead of failing with a `NullPointerException` during Process Validations.
  - `managed=true` is a per-CLASS, application-wide switch: dropping it for the `<o:validator>` use changes every other attachment of that class too.
  - Check that such a class is not ALSO registered under `<default-validators>` — it would be attached to every input on top of the explicit attachment, and validate twice.

## Common Pitfalls

- **"Validation Error: Value is not valid"** on `UISelectOne`/`UISelectMany`: the selected value is compared to the list of available values using `equals()`. If the entity class is missing `equals()`/`hashCode()`, or the converter is not symmetric, or the list changed between render and postback (use `@ViewScoped`), this error occurs.
- **Converter not invoked on `UIOutput`**: the converter is still invoked during Render Response to format the model value for display. If the output shows the raw `toString()`, check that the converter is correctly registered (e.g. `forClass` matches the exact type).
- **`<f:validateRegex>` backslash escaping**: the number of backslashes depends on the EL implementation. Oracle EL (`com.sun.el.*`, used by Payara/WildFly/Liberty/WebLogic) needs two backslashes (`\\d`), Apache EL (`org.apache.el.*`, used by TomEE/Tomcat) needs one (`\d`). Prefer character classes like `[0-9]` over `\d` for portability.
- **Empty string vs null**: without `INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL=true`, empty form fields arrive as `""` instead of `null`, breaking `@NotNull` and polluting the database with empty strings.
- **Dynamic converter/validator config is not re-applied on postback**: EL attributes on an *attached* `<f:converter>`/`<f:validator>`/`<f:convertDateTime>` (e.g. `<f:convertDateTime pattern="#{bean.pattern}">`) are resolved only when the component is FIRST built. On a component that survives postbacks, the attached-object tag handler bails out (the parent already exists), so the converter/validator is restored from state with its ORIGINAL config — toggling the bound value on a later postback has no effect (Jakarta Faces #1499). This differs from a component's own `value`/`rendered` expressions, which are stored on the component and re-evaluated every request. For genuinely dynamic conversion, read the varying value from the live model INSIDE a custom converter, not from a tag attribute; when OmniFaces is available, `<o:converter>`/`<o:validator>` is the direct fix, since all their attributes are deferred and re-evaluated on every access — see "Deferred Attributes" under Custom Validators. (A `<c:forEach>` body is exempt: it unrolls fresh components on every build.)
