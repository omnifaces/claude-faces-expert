# Jakarta Faces Diagnostic Decision Trees

*Version 1.2.1*

When a user reports one of these symptoms, walk through the causes in order.
Each cause has a check (how to verify) and a fix (how to resolve).

## Action Not Invoked

Symptom: `UICommand` or `AjaxBehavior` don't reach the backing bean action/listener method when the form is submitted.

1. **Outside `UIForm`**: The command component must be inside `UIForm`, not plain HTML `<form>`.
2. **Nested `UIForm`**: HTML doesn't allow nested forms.
   Remove the inner form or restructure.
3. **Validation/conversion error**: If any field fails validation, the action phase is skipped entirely.
   Add `UIMessages` to the form to ensure that all messages are caught.
4. **`rendered`/`disabled`/`readonly` evaluates false during decode**: If these attributes depend on request-scoped state, the condition may differ during apply-request-values.
   Use `@ViewScoped` to preserve state.
5. **Inside `UIData` with `@RequestScoped` bean**: The data model must be preserved during postback.
   Use `@ViewScoped` or load data in `@PostConstruct`.
6. **`onclick` returns false**: JavaScript returning false prevents form submission.
7. **Wrong method binding**: `action="#{bean.method}"` -- method must be public, return String or void.
8. **Wrong bean name in EL**: `#{beanName.method}` must match `@Named public class BeanName {}` or `@Named("beanName")`.
9. **`type="button"`**: This prevents form submission.
   Remove it so default of `type="submit"` is used.
10. **Parent `rendered="false"`**: If any parent container has `rendered="false"` during postback, children are skipped.
11. **Missing/corrupt ViewState**: Check HTML element inspector and HTTP request payload for the `jakarta.faces.ViewState` hidden input.
12. **Exception swallowed/unhandled**: Check server logs.
    Add try-catch or custom ExceptionHandler.
    When using ajax in PROJECT_STAGE=Production, there is by default no GUI feedback.
    Either use Development stage or add/activate ajax exception handler from OmniFaces or PrimeFaces.
13. **jQuery handlers lost after ajax update**: Use event delegation `$(document).on(eventName, '#id', handler)` instead of direct binding `$('#id').on(eventName, handler)`.
14. **First-click-fails (JSF 2.2 bug)**: Upgrade to JSF 2.3+ or ensure the parent form is in the ajax render.
    PrimeFaces handles this automatically.

## Target Unreachable / PropertyNotFoundException

Symptom: `jakarta.el.PropertyNotFoundException: Target Unreachable, identifier 'bean' resolved to null`

1. **Bean not registered**: Add `@Named` (CDI) and a scope annotation.
   Ensure `beans.xml` exists in `WEB-INF/`.
2. **Name mismatch**: Default `@Named` name is class name with lowercase first letter.
   `MyBean` -> `#{myBean}`.
   Use `@Named("exact")` to override.
3. **Property naming**: `#{bean.foo}` requires `getFoo()`/`setFoo()`.
   Follow JavaBeans conventions.
4. **Nested property null**: `#{bean.foo.bar}` -- if `getFoo()` returns null, 'bar' is unreachable.
   Initialize in `@PostConstruct`.
5. **Wrong scope import**: Don't mix `javax.faces.bean.*` with `javax.inject.Named`.
   Use a CDI-compatible scope.
6. **Missing/wrong `beans.xml`**: Needs `bean-discovery-mode="all"` or `"annotated"` in `WEB-INF/`.
7. **Deployment error**: Check server startup logs for class loading failures.
8. **Map/list key doesn't exist**: `#{bean.map['key'].foo}` fails if map is null or key missing.

## ViewExpiredException

Symptom: `jakarta.faces.application.ViewExpiredException: viewId:/page.xhtml - View could not be restored`

1. **Session timeout**: Check `<session-timeout>` in `web.xml`.
   Default is typically 30 minutes.
2. **Browser back button after logout**: Add no-cache filter.
   Implement custom ViewExpiredException handler.
3. **POST URL after session expired**: Use POST-Redirect-GET pattern.
   Configure ExceptionHandler redirect.
4. **Missing ViewState hidden field**: Check HTML source.
   Ensure JavaScript isn't removing it.
5. **Too many tabs**: Increase `com.sun.faces.numberOfLogicalViews` (Mojarra) or `org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION` (MyFaces).
6. **Cluster without session replication**: Enable session replication (sticky sessions).
7. **Cluster with session replication**: Put `<distributable/>` flag in `web.xml` so Servlet runtime and Faces runtime performs more aggressive session dirtying and HTTP sessions (including view scoped beans) are properly synced across servers.
8. **Client-side state saving in cluster ("MAC did not verify")**: Client-side state is AES-encrypted.
   Each server generates its own key on startup, so state encrypted by one node can't be decrypted by another.
   Fix: configure the same fixed AES key (Base64-encoded) on all nodes via JNDI entry `faces/ClientSideSecretKey` (`jsf/ClientSideSecretKey` before Faces 4.0).
   In production, configure this in the server's JNDI environment.
   For local development only, you can use `<env-entry>` in `web.xml`.
   Generate a key with: `KeyGenerator.getInstance("AES").init(256)` then `Base64.getEncoder().encodeToString(key.getEncoded())`.
   NEVER publish/opensource the key.
9. **Mojarra `clientStateTimeout`**: The context-param `com.sun.faces.clientStateTimeout` (seconds) causes ViewExpiredException when client-side state exceeds the configured age.
   Check `web.xml` for this param.
10. **State saving method**: As last resort: set `jakarta.faces.STATE_SAVING_METHOD` to `client` in `web.xml` for client-side state.

## Validation Error: Value is not valid

Symptom: `Validation Error: Value is not valid` on `UISelectOne`/`UISelectMany` (`<h:selectOneMenu>`, `<h:selectManyMenu>`, `<p:selectOneMenu>`, etc).

1. **Missing `equals()`/`hashCode()`**: Faces compares submitted value against available items using `equals()`.
   Implement both on the value object.
2. **List changed between render and postback**: Use `@ViewScoped` to preserve the select items list.
3. **Broken converter**: `getAsObject()` must return the correct type and match an item in the list.
4. **Missing converter**: Complex objects (not String/Number) require a `@FacesConverter`.
5. **Non-symmetric conversion**: `getAsObject(getAsString(x)).equals(x)` must be true.
6. **Null handling**: Handle null/empty explicitly in the converter.
7. **Custom collection type**: Use standard collections (`ArrayList`, `LinkedHashSet`) as backing property, not `List.of()`, `Arrays.asList()`, `Stream.toList()`, or custom types.
8. **Custom converter**: When using objects as select item values, implement `toString()` with unique outcome and use OmniFaces `SelectItemsConverter` so that custom converter is not needed.

## Input Value Not Updated / Setter Not Called

Symptom: `UIInput` values don't reach the backing bean setter when form is submitted.

1. **Conversion/Validation error on another field**: If ANY field fails validation, ALL setters are skipped.
   Add `UIMessages` to the form to ensure that all errors are caught.
2. **`UIInput` outside `UIForm`**: Must be inside `UIForm`.
3. **Wrong value binding**: `value="#{bean.property}"` requires a matching setter.
4. **Using `binding` instead of `value`**: `binding` is for component instances, not data.
   Use `value`.
5. **`disabled` or `readonly`**: Disabled inputs are not submitted by browser and readonly inputs are not processed by Faces.
6. **`<f:ajax execute>`/`<p:ajax process>` of `UICommand` excludes the `UIInput`**: Add the input ID or set to `execute="@form"`/`process="@form"`.
7. **Getter creates new wrapper object each call**: Initialize in `@PostConstruct` and return same instance.

## Component Not Found for Update/Render

Symptom: `Cannot find component with expression "componentId" referenced from "sourceId"`

1. **Different NamingContainer**: components implementing `NamingContainer` interface (`<h:form>`, `<h:dataTable>`, `<ui:repeat>`, `<p:dataTable>`, `<p:tabView>`, composite components, etc) prefix child IDs.
   Use full client ID: `":formId:componentId"`.
2. **Missing leading colon**: Absolute reference from within a NamingContainer needs `":"` prefix.
3. **`rendered="false"`**: Component isn't in output.
   Wrap in always-rendered `<h:panelGroup>` and update wrapper.
4. **Inside composite component**: Composites are NamingContainers.
   Use `<cc:clientBehavior>` or full prefixed ID.
5. **Dynamic iteration ID**: Inside `<ui:repeat>`/`UIData`, IDs include row index.
   Update the entire container instead.
6. **Typo**: Inspect rendered HTML for the actual client ID.
7. **Dynamically added component not yet in tree**: Use `rendered` attribute instead of programmatic tree manipulation.
