# Faces Lifecycle

*Version 1.0.0*

The Faces request processing lifecycle defines the order in which the server processes a Faces HTTP request.
Understanding this lifecycle is essential for knowing when things happen and why certain rules exist.

## Phases

Every Faces request passes through up to 6 phases in this order:

### 1. Restore View
- The component tree (UIViewRoot) is rebuilt from scratch from the XHTML template (the "view build").
- During view building, only these expressions are evaluated (causing getter calls on backing beans):
  - Any attribute of a Facelets template tag (e.g. `<ui:include>`, `<ui:repeat>`) and JSTL core tag (e.g. `<c:if>`, `<c:forEach>`).
  - Only the `id` and `binding` attributes of Faces components; all other component attributes are NOT evaluated during view build.
- On postback, the view state delta from the previous response is applied to restore the tree to its previous state.
- On initial (non-postback/GET) request, an empty view is created and the lifecycle skips directly to Render Response.
- `@PostConstruct` on backing beans is called when the bean is first referenced during this phase (or any later phase).
- `<f:viewAction>` is executed after this phase (before Apply Request Values) on initial requests, or after Invoke Application on postbacks if `onPostback="true"`.

### 2. Apply Request Values
- Input and command components first get model values from the backing bean to evaluate `rendered`, `disabled`, and `readonly` attributes, to determine whether to process the request.
- Each eligible `EditableValueHolder` component extracts its "submitted value" (raw String) from the HTTP request parameters.
- Command components detect from HTTP request parameters whether they were invoked client-side, and if so, queue an `ActionEvent`.
- Components with `immediate="true"` perform conversion and validation in this phase instead of in Process Validations.
- If conversion or validation fails on an immediate component, it adds a `FacesMessage` and calls `renderResponse()`.

### 3. Process Validations
- Each non-immediate `EditableValueHolder` component converts and then validates its submitted value.
- Conversion: the submitted String value is converted to the model type using a `Converter` (explicit or implicit).
- Validation: the converted value is validated by attached `Validator` instances and Bean Validation constraints.
- The successfully converted and validated value is stored as the component's "local value" (distinct from the raw "submitted value").
- Each input component then gets the OLD model value from the backing bean and compares it with the new local value; if they differ, a `ValueChangeEvent` is queued.
- After ALL conversion and validation is finished, any queued `ValueChangeEvent` listeners are invoked on the backing bean.
- If any conversion or validation fails, the component adds a `FacesMessage`, marks itself invalid, and calls `renderResponse()` — skipping Update Model Values and Invoke Application entirely.
- This is why ALL setters are skipped when any single field has a validation error.

### 4. Update Model Values
- Each valid `EditableValueHolder` component pushes its converted and validated value to the backing bean property via the setter.
- Only reached if ALL components passed validation.

### 5. Invoke Application
- The queued `ActionEvent` listeners (`actionListener`) are invoked first; if any throws `AbortProcessingException`, the `action` method is skipped.
- The `action` method is then invoked and can return a navigation outcome (String) or null/void for no navigation.
- Navigation outcomes with `?faces-redirect=true` trigger POST-Redirect-GET.

### 6. Render Response
- The component tree is rendered to HTML and sent to the client.
- Practically any attribute of a Facelets component and a Faces component involved in generating HTML output will get executed, causing getter calls on the backing bean; a single getter may be called multiple times (once per referencing component attribute).
- The view state delta (difference between default tree and current tree) is computed and stored.
- State saving does NOT evaluate value expressions; it saves the component state that is already in the component tree.

## Phase Shortcuts

The lifecycle can be short-circuited:
- **Initial (GET) request**: only Restore View and Render Response execute; phases 2-5 are skipped.
- **Validation/conversion failure**: skips from Process Validations directly to Render Response; setters and actions are never called.
- **`immediate="true"` on UICommand**: action executes during Apply Request Values; non-immediate inputs are not processed at all.
- **`FacesContext.renderResponse()`**: called programmatically to skip remaining phases and jump to Render Response.
- **`FacesContext.responseComplete()`**: called programmatically to skip all remaining phases including Render Response (e.g. for file downloads or redirects in a PhaseListener).

## Ajax Lifecycle

Ajax requests follow the same 6 phases but with partial processing:
- **`execute`/`process`**: only the specified components are processed in phases 2-5.
- **`render`/`update`**: only the specified components are rendered in phase 6; the response is a partial XML update, not a full page.
- Default `execute` is `@this` (only the triggering component); default `render` is `@none`.

## PhaseListener

A `PhaseListener` intercepts lifecycle phases for cross-cutting concerns.

- Registered in `faces-config.xml` or via `<f:phaseListener>` tag.
- `beforePhase(PhaseEvent)` / `afterPhase(PhaseEvent)` methods.
- `getPhaseId()` returns which phase to listen to, or `PhaseId.ANY_PHASE` for all.
- Common use cases: logging, authorization checks, locale/theme switching.
- Prefer `<f:viewAction>` over PhaseListener for page-specific logic.
- Prefer CDI `@Observes` with Faces events over PhaseListener for global logic (since Faces 2.3).

## Why This Matters

The lifecycle explains many rules:
- **Getters must be pure**: they are called during view build (for `id`/`binding`/JSTL/Facelets-template attributes), during Apply Request Values (for `rendered`/`disabled`/`readonly`), during Process Validations (for old model value comparison), and during Render Response (for HTML output generation).
- **`@PostConstruct` for initialization**: it runs once when the bean is created, not on every request; constructors cannot access injected dependencies.
- **Validation errors skip setters**: Update Model Values is skipped entirely if any validation fails in Process Validations.
- **`immediate="true"` behavior**: it moves conversion/validation to Apply Request Values, before non-immediate components are processed.
- **`rendered="false"` blocks decoding**: a component that evaluates `rendered="false"` during Apply Request Values will not process its submitted value; likewise for `disabled="true"` and `readonly="true"`.
- **JSTL is build-time**: JSTL tags execute during Restore View (view build) while the component tree is being populated; `rendered` is evaluated during Render Response on the already-built tree.
