# Conversion and Validation

*Version 1.0.0*

Conversion and validation are central to the Faces MVC lifecycle.
Converters transform between String (HTTP) and Object (model); validators enforce business rules on the converted value.
Both run during Process Validations (phase 3), or during Apply Request Values (phase 2) when `immediate="true"`.

## How Converters Work

- Every `ValueHolder` component (both `UIOutput` and `UIInput`) can have a `Converter`.
- During **Render Response**: the converter's `getAsString()` converts the model value to a String for HTML output.
- During **Process Validations**: the converter's `getAsObject()` converts the submitted String back to the model type.
- A converter MUST be symmetric: `getAsObject(getAsString(x)).equals(x)` must hold; if not, `UISelectOne`/`UISelectMany` will fail with "Validation Error: Value is not valid".

## Implicit vs Explicit Converters

- Most standard Java types have **implicit** converters that activate automatically based on the model property type: `BigDecimal`, `BigInteger`, `Boolean`, `Byte`, `Character`, `Double`, `Enum`, `Float`, `Integer`, `Long`, `Short`.
- These require NO configuration; just bind the value to a bean property of that type and Faces handles conversion.
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
  - JSF 2.3: requires `managed=true` on `@FacesConverter`.
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
- Register with `@FacesValidator("validatorId", managed=true)`.
- CDI injection (`@Inject`) requires `managed=true` on `@FacesValidator`, same as for converters.
- On validation failure, throw `ValidatorException(new FacesMessage(...))`.
- Activate in view via `validator="validatorId"` attribute or nested `<f:validator validatorId="...">` tag.

## Common Pitfalls

- **"Validation Error: Value is not valid"** on `UISelectOne`/`UISelectMany`: the selected value is compared to the list of available values using `equals()`. If the entity class is missing `equals()`/`hashCode()`, or the converter is not symmetric, or the list changed between render and postback (use `@ViewScoped`), this error occurs.
- **Converter not invoked on `UIOutput`**: the converter is still invoked during Render Response to format the model value for display. If the output shows the raw `toString()`, check that the converter is correctly registered (e.g. `forClass` matches the exact type).
- **`<f:validateRegex>` backslash escaping**: the number of backslashes depends on the EL implementation. Oracle EL (`com.sun.el.*`, used by Payara/WildFly/Liberty/WebLogic) needs two backslashes (`\\d`), Apache EL (`org.apache.el.*`, used by TomEE/Tomcat) needs one (`\d`). Prefer character classes like `[0-9]` over `\d` for portability.
- **Empty string vs null**: without `INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL=true`, empty form fields arrive as `""` instead of `null`, breaking `@NotNull` and polluting the database with empty strings.
