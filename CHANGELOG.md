# Changelog

## Unreleased

### Rules — Added
- A composite component is not a `UIInput`, even when it wraps one, so `<h:message for="compositeId">` stays empty. `UIInput` queues its messages under its own client id, and Mojarra `HtmlBasicRenderer.getMessageIter()` and MyFaces `HtmlMessageRendererBase` both look up exactly the client id `for` resolves to, without ever descending into a composite, `<cc:editableValueHolder>` or not. The message therefore belongs inside `<cc:implementation>` next to the input, or, where the using page must place it, is addressed as `compositeId:inputId`. The exception is a composite whose `componentType` backing component extends `UIInput` itself, and overrides `getFamily()` to return `UINamingContainer.COMPONENT_FAMILY`, without which the composite renderer is never found and the composite renders nothing: it queues its own conversion and validation messages under `compositeId`, so `for="compositeId"` is right there. `faces-review` reports a `<h:message>` naming a composite as an `error`, unless the composite already carries its own or is such a `UIInput`.
- Since PrimeFaces 14.0.0, `<p:message for>` and `<p:messages for>` do descend into a composite (https://github.com/primefaces/primefaces/pull/11157): when the composite's own client id has nothing to show, `CompositeUtils.invokeOnDeepestEditableValueHolder()` finds the input inside, following the `targets` of a declared `<cc:editableValueHolder>` first and skipping unrendered components. It stops at the first `EditableValueHolder`, so a composite wrapping several inputs gets the messages of that one only, and a declared `<cc:editableValueHolder>` whose `targets` resolve to nothing throws a `FacesException` at render time. `<p:messages>` descends only when `forType` is unset or `expression`, never with `forType="key"`.

## 1.7.0

### Rules — Added
- New rule covering the namespaces a page may declare beyond the canonical per-version blocks, and how to tell a legitimate one from a broken one. A `@FacesComponent(createTag = true)` class puts its tag in `FacesComponent.NAMESPACE` — `http://xmlns.jcp.org/jsf/component` in JSF 2.2 - 3.0 and `jakarta.faces.component` in Faces 4.0+ — unless the annotation pins one; a `*.taglib.xml` declares its own and covers its composites; third-party libraries ship theirs. A namespace nothing registers fails without a word: Facelets copies the tag into the response as literal markup and leaks the declaration into the rendered `<html>`. The jcp component namespace is not such a case on Faces 4.0+, since Mojarra and MyFaces both keep registering every component-declared tag under it next to the annotated namespace. What breaks is the reverse, a class pinning the old URI while the page uses `jakarta.faces.component`, which fails the view with `Tag Library supports namespace: jakarta.faces.component, but no tag was defined for name: <tag>`.
- `<h:panelGroup>` carrying only `rendered` emits no element at all: Mojarra `GroupRenderer.divOrSpan()` and MyFaces `HtmlGroupRendererBase.needsWrapper()` both gate the wrapper on a written id, a `styleClass`, a renderable passthrough attribute or a client behavior, and `layout` is in neither gate — it only picks `div` over `span` once a wrapper is emitted, so `<h:panelGroup layout="block" rendered="...">` without an id renders nothing. Such a block reads as `<ui:fragment rendered="...">`, and when it wraps a single component the `rendered` belongs on that component with the wrapper gone — the latter only where the child IS a component and has no `rendered` of its own. Both rewrites are off inside `<h:panelGrid>`, where `GridRenderer` skips unrendered children and gives each survivor its own `<td>`: an unrendered wrapper drops the cell and shifts every later cell into it, whereas an always-rendered wrapper around an unrendered child keeps the cell empty. `faces-review` reports the two rewrites as `info`, and leaves grid cells and ajax `render`/`update` targets alone.
- New rule covering the four Facelets structural tags as the two independent properties they actually are — whether the tag trims everything outside it in that file, and whether it contributes a component — with a table of the four combinations. `<ui:component>` and `<ui:fragment>` register the same `ComponentRef` component type and differ only in trimming; `<ui:composition>` and `<ui:decorate>` are likewise the same template-client handler differing only in trimming, and are the only two carrying a `template` attribute. The component contributed by `<ui:component>`/`<ui:fragment>` renders no markup and is not a `NamingContainer`, so it namespaces nothing and puts no element in the DOM; it must never be an ajax `render`/`update` target, since `faces.ajax` resolves the update id with `getElementById` and throws `During update: <id> not found`, failing the whole response. `<h:panelGroup layout="block">` stays the always-rendered ajax wrapper.

### Rules — Fixed
- `faces-review`: a namespace outside the standard ones is invalid only when nothing registers it — no `@FacesComponent(createTag = true)` class, no `*.taglib.xml`, no composite library, no third-party dependency — and one that resolves but that no tag in the view uses is reported as `info`, never `warning` or `error`.
- `faces-review`: the location of a Facelets file is now checked in its own **Directory Structure** section of the review checklist, run over every `.xhtml` outside `/WEB-INF/` before the checks on what those files contain. It classifies each one — view, template, include, tag file, composite — since the webapp root is the correct home for a view and a finding for everything else, and passing the content checks is not grounds for clearing a file on location. A template, include or tag file outside `/WEB-INF/` is an `error`; a composite or asset is a `warning`, because that fix has two halves — moving the files into `/WEB-INF/resources/` AND setting `jakarta.faces.WEBAPP_RESOURCES_DIRECTORY` to `WEB-INF/resources`, either alone leaving them unresolvable or 404. A composite is recognized by containing `<cc:interface>`, never by its root element, which is `<ui:component>` or `<ui:composition>` interchangeably.
- `faces-review`: the exposure such a file carries now follows the `FacesServlet` mapping instead of being assumed to be source disclosure. Under the recommended `*.xhtml` mapping the file is processed rather than served, so what is reachable is the file rendered standalone outside the view meant to supply its `ui:param` values or `cc.attrs`, its `f:metadata` and its access checks. Only a legacy-only mapping (`*.jsf`, `*.faces`, `/faces/*`), which overrides the implicit `*.xhtml` one, drops `*.xhtml` to the container's default servlet and makes the Facelets source downloadable — and then only where no `<security-constraint>` covers it.
- The composite component root element rule is downgraded from a requirement to a recommendation. `<ui:component>` remains the convention, but `<cc:interface>` is what makes a file a composite and `<ui:composition>` is equally valid. The root of such a file has no runtime effect whatsoever: `CompilationManager.pushTag()` clears the entire unit stack when it reaches `<cc:implementation>` and restarts from the saved `<cc:interface>`, so the root never survives compilation and even a `<ui:component>` root contributes no component. Recognition must therefore never key off the root element. `<html>` with `<!DOCTYPE html>` stays wrong for a different reason than assumed: the element is trimmed like any other, but `SAXCompiler` captures the DOCTYPE in `startDTD` before trimming applies and saves it on the `FacesContext`, so it escapes the composite and overrides the host view's doctype.

### faces-migrate — Added
- `http://xmlns.jcp.org/jsf/component` → `jakarta.faces.component` joins the Faces 3.0 → 4.0 namespace list, above the generic `http://xmlns.jcp.org/jsf` entry that would otherwise consume its prefix, together with the Java-side half: a `@FacesComponent` pinning the old URI in its `namespace` attribute is rewritten too, or loses the attribute so it follows `FacesComponent.NAMESPACE` again.

### Tooling — Added
- Local integration tests for `/faces-review` under `tests/`, run with `./it.sh`. Each fixture is a real Faces webapp with violations planted on purpose; the skill reviews it and a judge model grades the report against a `must`/`must_not` checklist. Five fixtures cover plain Faces 4.1, PrimeFaces, OmniFaces `<o:validator>`, a Faces 2.3 runtime, and lagging deployment descriptors. A run stages the working-tree `.claude/` into a temp copy and isolates `CLAUDE_CONFIG_DIR`, so it measures the branch rather than any user-scope install. Calls the Anthropic API on the developer's own key from `.env.local`; never runs in CI. The runner is stdlib Python driving the `claude` CLI, so the suite needs no build step, no virtualenv and no third-party package. `DEVELOPERS.md` covers setup, cost, the environment knobs and how to add a fixture.

## 1.6.0

### Documentation — Changed
- **CONTRIBUTING.md**: releasing now publishes a GitHub Release for the tag, with the notes taken from the matching `CHANGELOG.md` section. That is what notifies anyone watching the repository for releases, and what makes `/releases/latest` resolve. Every release from 1.0.0 on has one.

### Rules — Added
- New **Deferred Attributes** section under Custom Validators in `conversion-validation.md`, covering OmniFaces `<o:validator>`/`<o:converter>`: every attribute is a deferred value expression re-evaluated on every access, which is what makes per-iteration attribute values, attribute values that change between postbacks, and attributes on a custom converter/validator possible without a tag file or a custom handler.
- `managed=true` MUST be absent on a `@FacesConverter`/`@FacesValidator` class whose attributes are set by `<o:converter>`/`<o:validator>`: Mojarra hands out a `CdiValidator`/`CdiConverter` and MyFaces a `FacesValidatorCDIWrapper`/`FacesConverterCDIWrapper`, neither of which carries the class's own setters, so `MetaTagHandler` finds no setter and drops every attribute silently (https://github.com/eclipse-ee4j/mojarra/issues/4814). Removing it does not cost the injection, since OmniFaces `ConverterManager`/`ValidatorManager` manage the annotated classes without `managed=true` — but only with OmniFaces on the classpath, and for a `forClass` converter only when the class declares no public `Class` argument constructor.
- Warn when one converter/validator class is attached both via `<o:validator>`/`<o:converter>` and the standard way: it MUST be `@Dependent`, or the instance shared by its scope lets the attributes set by the OmniFaces attachment leak into the standard one, which sets none itself. Also covers `@Dependent` for CDI discovery under `bean-discovery-mode="annotated"`, `@Specializes` for `AmbiguousResolutionException`, and the double validation when the class is additionally a default validator.
- Faces 5.0 drops the wrapper conflict on Mojarra — `managed` is `@Nonbinding` and ignored, and `CdiUtils` returns the CDI reference itself — while MyFaces 5.0 snapshots still key off `managed()` and still wrap, so the rule keys off the implementation rather than the spec version.
- `faces-review`: the OmniFaces checklist reports `managed=true` on a class configured by `<o:converter>`/`<o:validator>` as an error, and mixing that with a standard attachment as a warning.

### Rules — Fixed
- The `<o:converter>`/`<o:validator>` tags are now named as the direct fix for the "dynamic converter/validator config is not re-applied on postback" pitfall.
- Faces 5.0 source reference: Mojarra's 5.0 development happens on branch `5.0`, not `master` (which no longer exists); its `main` branch carries the 4.1 line.
- The **Dialogs** section in `primefaces.md` is renamed **Dialogs and Overlays**, since what it now says depends on the overlay family, and it no longer claims that PrimeFaces appends `<p:dialog>` to the end of the `<body>`, nor that a dialog must always carry its own `UIForm`. Verified against PrimeFaces 8 through 16: `DialogBase#getAppendTo()` evaluates to `null` when the attribute is absent and `WidgetBuilder#attr()` skips a `null` value, so `DialogRenderer` emits no `appendTo` at all and `registerDynamicOverlay()` has nothing to act on, which is why `implicitDefaultValue = "@(body)"` on `DialogBase` never takes effect; as of 13.0, `DynamicOverlayWidget` additionally exempts `Dialog` — subclasses included, an `instanceof` check — from the `resolveAppendTo()` fallback that other overlays get. Only the modal mask is appended to `<body>`, and only for `modal="true"`.
- The form rule is now stated per `appendTo` value: none (the default) keeps the dialog in place and needs no form of its own; `@(body)` moves it out of its form and therefore requires one, declared outside the other form since a nested `<form>` start tag is dropped by the HTML parser before the move can happen; `@form` appends it to its own enclosing form; and `@(form)` is a jQuery selector matching every form in the document, which clones the dialog into each of them.
- What a body-appended dialog actually does on submit: PrimeFaces resolves a command's form server-side from the component tree and emits it as `f:`, so the action runs, but the request body is serialized from that form element in the DOM and the dialog's values are silently dropped — unless `process` names components inside the dialog, since `partialSubmit="true"` is skipped when the process ids contain `@all` (the fallback for a command without `process`) and `process="@form"` serializes that same form element.
- The "similar overlay components" generalization is replaced by what each family actually does: an `@(body)` default on `*Base#getAppendTo()` for the panel inputs `<p:selectOneMenu>`, `<p:selectCheckboxMenu>`, `<p:autoComplete>`, `<p:datePicker>`, `<p:cascadeSelect>` and the button menus `<p:menuButton>`, `<p:splitButton>`, so those are always moved; likewise `<p:menu overlay="true">`, `<p:tieredMenu overlay="true">` and `<p:slideMenu overlay="true">` via `BaseMenu#initOverlay()`, and `<p:contextMenu>`, which sets `@(body)` on itself and skips `initOverlay()` for that reason; auto-`@(body)` only inside a `.ui-dialog` for `<p:overlayPanel>`, `<p:confirmPopup>` and `<p:sidebar>`, which carry no default.
- The ajax default rule is scoped by component family. `<f:ajax>` and `<p:ajax>` are `AjaxBehavior`-based and default `execute`/`process` to `@this`, which `AjaxBehaviorRenderer` sets when the attribute is null. `AjaxSource#getProcess()` declares no default value, so `AjaxRequestBuilder#addExpressions()` omits the `p:` key from the emitted request config and `core.ajax` falls back to `@all`: `<p:commandButton>`, `<p:commandLink>`, `<p:remoteCommand>`, `<p:splitButton>`, `<p:poll>`, `<p:hotkey>` and `<p:menuitem>` process the ENTIRE view unless `process="@this"` is set. Verified across PrimeFaces 8 through 16.
- The same scoping is applied everywhere the unscoped `@this` claim was repeated: the `immediate="true"` cancel/logout rule in `rules.md`, and both the `immediate="true"` note and the reinvention table row in `faces-review` — converting a command to ajax only drops the need for `immediate` when `process="@this"` comes with it on PrimeFaces.
- The Ajax Lifecycle section in `lifecycle.md` no longer repeats the defaults; `rules.md` owns them. What that section states is what the phases do — `execute`/`process` in phases 2-5, `render`/`update` in phase 6 as a partial XML response.

## 1.5.0

### Documentation — Changed
- **CONTRIBUTING.md**: the Releases section spells out the whole procedure — the files that carry the version, renaming the `## Unreleased` heading, the `Release X.Y.Z` commit on `develop`, the lightweight tag without a `v` prefix, and the fast-forward of `main` onto it.

### Rules — Added
- New **Build Time** section: a postback runs two builds of the same view, the restoring build in Restore View and the rendering build before Render Response, and what each produces follows from *when* something happened relative to a build rather than from how it was created (https://github.com/jakartaee/faces/pull/2235).
- Tree manipulation performed while the view is being built, from `<f:event type="postAddToView">`, is reproduced by every build and costs no view state at all; only manipulation performed after the build is recorded and replayed. A move is neither an addition nor a removal and carries parent, facet name and index among siblings. The recorded position travels with the manipulation rather than with the component, since every build creates its components anew.
- Build time decisions — the test of a conditional, the branch of a choice, the range or items of an iteration, the path of a dynamic inclusion — are reproduced by the restoring build from the saved state since Faces 5.0, so a page must not rely on the restoring build following the current model, and the author does not have to keep the value such a condition depends on alive across the postback. The items of an iteration are the exception: its rows read their element from the items live, so those belong in a `@ViewScoped` bean or are recomputed in `@PostConstruct` of a `@RequestScoped` one.
- Mojarra replays under `com.sun.faces.restoreBuildTimeDecisions` since 4.0.23/4.1.14 (default off) and `org.glassfish.mojarra.restoreBuildTimeDecisions` since 5.0.0-M6 (default on); MyFaces has always replayed from a `FaceletState`. An application-provided tag handler is run by both builds and is not covered.

### Rules — Fixed
- Full state saving and the `jakarta.faces.PARTIAL_STATE_SAVING` / `jakarta.faces.FULL_STATE_SAVING_VIEW_IDS` context params are removed in **Faces 5.0**, not 6.0; partial state saving is the only state saving there is from that version on.
- The JSTL rule no longer claims a `c:if`/`c:forEach` condition may depend on current request state without qualification. It is the rendering build that observes the updated model; the restoring build reproduces the previous decision.

### Diagnostics — Added
- **Target Unreachable**: an iteration whose items no longer hold the elements its rows were rendered over, reported against the row var.
- **Input Value Not Updated**: a build time decision that no longer holds, with the Mojarra context param per version line.

### faces-migrate — Fixed
- The Faces 4.0 → 4.1 step places the removal of full state saving in Faces 5.0, not 6.0.

### faces-migrate — Added
- Each incremental step now bumps `web.xml` to the target Servlet version and schema, gated on `web.xml` existing and lagging behind: Servlet 4.0 for JSF 2.3, 5.0 for Faces 3.0, 6.0 for Faces 4.0, 6.1 for Faces 4.1. A pre-Servlet 2.4 DTD `DOCTYPE` is dropped in favor of the XML Schema form (#12).
- Each incremental step now also bumps `beans.xml` to the target CDI version and schema: CDI 2.0 for JSF 2.3, 3.0 for Faces 3.0, 4.0 for Faces 4.0, 4.1 for Faces 4.1. `bean-discovery-mode` is required next to `version` up to CDI 3.0, and `annotated` is the recommended value since `@Named` plus a CDI scope is a bean defining annotation.
- The Faces 3.0 → 4.0 step makes `bean-discovery-mode` explicit before the bump. Up to CDI 3.0 an empty or version-less `beans.xml` meant `all`; since CDI 4.0 the attribute defaults to `annotated`, so every bean without a bean defining annotation silently stops being discovered at runtime while the build stays green.
- The verification step checks each of `faces-config.xml`, `web.xml` and `beans.xml` against the Faces, Servlet resp. CDI API version the runtime supplies.

### PrimeFaces — Fixed
- The head resources and CSS rules no longer suggest `target="head"` on `<h:outputStylesheet>`. The tag has no `target` attribute; the standard `Stylesheet` renderer is required to relocate the resource into `<head>` unconditionally, so only `<h:outputScript>` needs it. Both must still be declared inside `<h:body>` to end up after the resources contributed by PrimeFaces components (#10).

## 1.4.2

### Antigravity Support
- New `install-antigravity.sh` installer for [Google Antigravity](https://antigravity.google) users, project and user scope (#9).
- Writes `.agents/rules/jakarta-faces.md` (project) or appends to `~/.gemini/GEMINI.md` (user). Both get the same pointer, an instruction to read `.claude/faces/rules.md` with the agent's file tool rather than the rules themselves: a rule file is capped at 12,000 characters and the knowledge base is several times that, and an `@` reference is expanded into the rule file, so the same cap applies to it. The project rule adds `trigger: always_on` frontmatter to apply it unconditionally, plus a `description` so it still applies by model decision if a future version stops recognising the trigger.
- Copies the skills into `.agents/skills/` (project) or into both `~/.gemini/antigravity/skills/` and `~/.gemini/config/skills/` (user), the global skill directories of respectively the Antigravity IDE and Antigravity 2.0. Antigravity does not read `.claude/skills/`. The project directory is the one `install-codex.sh` already uses and the content is identical, so both installers can run in the same project. Slash invocation there belongs to workflows rather than skills, so as with Codex the skills are inert and the knowledge base is what carries the value.
- In user scope, `~/.gemini/GEMINI.md` is shared with the Gemini CLI, which picks the rules up as well.

## 1.4.1

### faces-migrate — Fixed
- The JSF 2.3 → Faces 3.0 step now migrates the standard message bundle. A project-supplied `javax/faces/Messages*.properties` moves to `jakarta/faces/` and its keys are renamed from `javax.faces.*` to `jakarta.faces.*`; the same key rename applies to a bundle declared by `<message-bundle>` in `faces-config.xml`, and to `javax.validation.constraints.*.message` keys in `ValidationMessages*.properties`. The base name is spec-defined as `FacesMessage.FACES_MESSAGES`, so a bundle left in `javax/faces/` is never found again and the built-in English defaults silently take over — nothing fails at build time (#8).
- The Faces 4.0 → 4.1 step reports the two keys that 4.1 adds to the standard message bundle, `jakarta.faces.converter.UUIDConverter.UUID` and `jakarta.faces.converter.UUIDConverter.UUID_detail`, so an overriding bundle can be completed. Faces 3.0 and 4.0 add none.
- The verification step greps for leftover `javax.` keys in `*.properties` and for a stale `javax/faces/` resource directory.

### faces-migrate — Changed
- The Faces 4.0 → 4.1 step covers `jakarta.faces.PARTIAL_STATE_SAVING` next to `jakarta.faces.FULL_STATE_SAVING_VIEW_IDS`; both are `@Deprecated(forRemoval = true, since = "4.1")` on `StateManager`. Set to `true` the param is removed as the default since 2.0, set to `false` it is reported rather than removed, since dropping it flips the application onto partial state saving and surfaces every custom component that does not save its state correctly, which is to be fixed first.
- The Faces 4.0 → 4.1 step replaces `ActionSource2`, `ActionSource2AttachedObjectHandler` and `ActionSource2AttachedObjectTarget` with their `ActionSource` counterparts, also deprecated for removal since 4.1.

## 1.4.0

### Codex Support
- New `install-codex.sh` installer for [OpenAI Codex](https://developers.openai.com/codex) users, project and user scope.
- Writes `AGENTS.md` (project) or `~/.codex/AGENTS.md` (user). Codex concatenates the `AGENTS.md` files it discovers as literal text and resolves no references to other files, so the rules are inlined between the same `<!-- BEGIN Jakarta Faces Expert -->` / `<!-- END Jakarta Faces Expert -->` markers the Copilot installer uses; re-running replaces only that block.
- Copies the skills into `.agents/skills/` (project) or `~/.agents/skills/` (user). Codex scans `.agents/skills` and `$HOME/.agents/skills` but not `.claude/skills/`, so unlike Cursor it cannot pick them up in place. Both skills set `disable-model-invocation: true` and Codex offers no slash invocation for them, so treat them as inert there; the knowledge base in `AGENTS.md` is what carries the value.
- **install-opencode.sh**: skips writing its `AGENTS.md` pointer when that file already carries the inlined block, so a project using both Codex and OpenCode does not load the rules twice.

### Copilot Support
- New `install-copilot.sh` installer for [GitHub Copilot](https://github.com/features/copilot) users.
- Writes `.github/copilot-instructions.md`, the one instructions mechanism supported across all Copilot surfaces; `.github/instructions/*.instructions.md` is scoped to the cloud agent and code review, so it is not used.
- The rules are **inlined** rather than referenced: Copilot's ask mode has no file access at all, and its agent mode reads a referenced file only after a permission prompt, so a pointer cannot deliver an always-on knowledge base. The topic files stay referenced, since inlining them would put ~1400 further lines into every request for guidance that is consulted rarely.
- The inlined block is delimited by `<!-- BEGIN Jakarta Faces Expert -->` / `<!-- END Jakarta Faces Expert -->`; re-running replaces only that block, so hand-written content elsewhere in the file survives.
- Project scope only: Copilot instructions are per-repository, and `--user` exits with an error rather than pretending otherwise.
- The skills are not installed; Copilot does not read `.claude/skills/`.

### Cursor Support
- New `install-cursor.sh` installer for [Cursor](https://cursor.com) users.
- Creates `.cursor/rules/jakarta-faces.mdc`, an `alwaysApply` rule referencing `.claude/faces/rules.md`, so the knowledge base is loaded without duplicating it.
- Reuses the existing `.claude/faces/` layout via `install.sh`.
- Project scope only; Cursor has no user-scope rules directory (User Rules are settings-UI text), so `--user` installs the knowledge base and prints the line to add manually.
- The skills need no configuration: Cursor reads `.claude/skills/` and `~/.claude/skills/` natively and honours `disable-model-invocation`, so `/faces-review` and `/faces-migrate` work as-is.

### OpenCode — Fixed
- **install-opencode.sh**: `AGENTS.md` now instructs the agent to read `.claude/faces/rules.md` instead of referencing it as `@.claude/faces/rules.md`, which OpenCode does not resolve. The instruction is worded unconditionally ("before answering any question about Jakarta Faces, conceptual ones included") because a conditional one scoped to code work does not fire on a terminology question, which is exactly the case the knowledge base exists to correct. Re-running the installer rewrites the old `@` pointer line in place, so existing installs pick the fix up.

### Documentation — Added
- **README.md**: new "Why not a Plugin?" section, explaining that the product is an always-on knowledge base rather than a command, that the closest plugin equivalent loads at the model's discretion, and that plugins cannot reference files outside their own directory (#2, #7).

### Documentation — Fixed
- **README.md**: the "What's included", "Updating" and "How it Works" sections claimed `/faces-review` and `/faces-migrate` are slash commands in tool-neutral prose, which holds for Claude Code and Cursor only. Both skills set `disable-model-invocation: true`, and neither OpenCode nor Codex exposes a slash invocation for skills, so they are inert there.
- **README.md**: the intro described the project as being for Claude Code alone, though four more tools now have installers.

## 1.3.0

### Knowledge Base — Fixed
- **rules.md**:
  - **Facelets Rules**: the PRG rule read as a mandate to redirect everywhere. It now notes that navigating on a postback at all is rare in a modern Faces application — an action normally stays on the same view, while page-to-page navigation happens by GET via `UIOutcomeTarget` links/buttons — so the rule governs how to navigate when a postback genuinely must, not how often that happens (#6).
  - **System Events and Phase Listeners**: the whole section described Faces 5.0 features as available "since Faces 4.0", causing reviews of Faces 4.x projects to recommend APIs that don't exist yet (#3). CDI `@Observes` support for system events (jakartaee/faces#1501) and phase events (jakartaee/faces#1443) is now correctly gated to Faces 5.0.
  - **System Events and Phase Listeners**: the recommended idiom for system events was wrong in every version — there are no per-event qualifier annotations (`@Observes @PreRenderView` does not exist). `Application.publishEvent()` fires the event instance itself, so the event CLASS is observed: `@Observes PreRenderViewEvent`.
  - **System Events and Phase Listeners**: documented which system events the CDI dispatch actually covers — every event whose source is not a `UIComponent` other than `UIViewRoot` (application-, `Flash`- and `UIViewRoot`-sourced); events sourced on an individual component (`PostAddToViewEvent`, `PreRenderComponentEvent`, `Pre`/`PostValidateEvent`, ...) are skipped and not observable via CDI.
  - **System Events and Phase Listeners**: documented the `@View` qualifier for narrowing a view-sourced system event, including that it is fired with the exact view id so wildcard patterns do not match.
  - **System Events and Phase Listeners**: the GET-only initialization rule recommended the non-existent `@Observes @PreRenderView`; it now prescribes `<f:viewAction>` and explicitly rejects both `<f:event type="preRenderView">` and `@Observes PreRenderViewEvent` for that case.
  - **System Events and Phase Listeners**: noted that the `PhaseId` value of `@BeforePhase`/`@AfterPhase` defaults to `ANY_PHASE`.
- **lifecycle.md**:
  - **PhaseListener**: corrected "prefer CDI `@Observes` over PhaseListener (since Faces 2.3)" to Faces 5.0.
- **conversion-validation.md**:
  - **Custom Converters** / **Custom Validators**: `managed=true` on `@FacesConverter`/`@FacesValidator` is load-bearing up to Faces 4.1 — without it the artifact is not CDI managed and injected fields stay null — but in Faces 5.0 all converters and validators are CDI managed, so the attribute is ignored and deprecated for removal (#6).

### Knowledge Base — Added
- **rules.md**:
  - **Facelets Rules**: messages do not survive a redirect unless kept in the flash and there is no global context-param for it. Navigate from `<f:viewAction>`, since the default `NavigationHandler` algorithm calls `setKeepMessages(true)` itself while a `UIViewAction` is broadcasting; a redirect elsewhere needs `Flash.setKeepMessages(true)` at that call site, for which OmniFaces offers `Messages.addFlashGlobal*` as a one-call equivalent when available (#6).
  - **Facelets Rules**: navigation decorators extend `ConfigurableNavigationHandler` on Faces 4.x but plain `NavigationHandler` on 5.0+, where `getNavigationCase()` moved up and `ConfigurableNavigationHandler` is deprecated for removal (jakartaee/faces#1833) (#6).
  - **XML Namespaces**: stated that the listed prefixes are convention only and that solely the namespace URI binds, so `xmlns:attr="jakarta.faces.passthrough"` with `attr:data-foo` and `xmlns:html="jakarta.faces.html"` with `<html:inputText>` are equivalent to the conventional forms; a prefix must never be used as the recognition pattern (#5).
  - **References**: split per Faces version, with spec, javadoc, VDL and JS API links for 4.0 and 4.1, so the set matching the project's runtime version can be picked instead of whichever is newest. Faces 5.0 is unreleased and has no published docs, so it points at the source repos with their branch names (`5.0`, `master`, `main`) and the relevant paths within `jakartaee/faces`.
  - New **Passthrough Elements** section: decoration is triggered by the `jakarta.faces` namespace and never by the prefix (`faces:` since 4.0, `jsf:` in JSF 2.x, or any other prefix bound to it); an element is decorated only if it carries at least one such attribute (removing the last one silently degrades it to static HTML); one on an element outside the empty or XHTML namespace throws `FaceletException`; `<a>`/`<button>` resolve by attribute; `name` doubles as the id for `<input>`/`<select>`; unmapped elements become `faces:element`. Decorated elements are real components, so `<a faces:action>` is a `UICommand` that must be inside a `UIForm` (#4).
  - **Component Rules**: the "must be inside `UIForm`" rule now calls out decorated elements explicitly, since they are easy to overlook when splitting a large form (#4).
  - **Passthrough Elements**: stated that plain Faces components and passthrough elements are equally valid, that a project's existing style must be detected and followed, and that plain Faces components are the default when a project has no established style, with the trade-off spelled out — a plain component needs the `jakarta.faces.passthrough` namespace for any attribute not in its VDL, a passthrough element takes any attribute directly but degrades silently to static HTML once its last `faces:` attribute is removed (#5).
  - **Passthrough Elements**: documented the limits of the fixed mapping — no `h:selectOneMenu` via `<select>`, `<input type="radio">` silently degrading to `h:inputText`, and no element form for `h:message`/`h:messages` — while tags rendering no markup (`ui:repeat`, `f:ajax`) compose with either style, making `<table>` + `<ui:repeat>` the HTML-first equivalent of `h:dataTable`. Added a ban on the legacy Facelets `jsfc` attribute: it covers those gaps but is not part of the specification, merely Facelets heritage that Mojarra and MyFaces happen to still carry, so nothing stops it from being removed or repurposed in a future version (#5).
  - New **Version Discipline** section: a project has two independent Faces versions — the *runtime* version, which governs which APIs exist, and the *declared* version (`faces-config.xml` `version` + `xsi:schemaLocation`), which caps the API features usable in that descriptor since each `web-facesconfig_*.xsd` enumerates only its own version. `web.xml`/`beans.xml` likewise declare the Servlet resp. CDI version. Also: untagged rules apply to Faces 4.0+; do not infer an API exists because a symmetric one does.

### Skills — Updated
- **faces-review**:
  - New **Step 1: Determine the Faces Version** — detect the *runtime* version (Faces API or implementation artifact, else the Jakarta EE platform version, else app server/namespaces) and the *declared* version (`faces-config.xml`) separately; report both, and fall back to the latest RELEASED version when the runtime version is undeterminable.
  - Suggestions are now held to the detected runtime version; a newer API may only be raised as `info` with its introducing version named.
  - APIs must be verified against the version-matched **References** in `.claude/faces/rules.md` — delegated to an `Agent`, since the skill has no web access — and reported as unconfirmed rather than recommended when verification fails.
  - New **Backing Beans** check: event listeners must use a mechanism that exists in the detected version.
  - Checklist items already covered by `.claude/faces/rules.md` collapsed into pointers to the relevant section ("Component Rules" for form nesting, "CDI and Bean Management" + "Scope Selection" for bean declaration, scope, `Serializable`, `@PostConstruct` and getter purity) instead of restating them.
  - **Configuration**: replaced the `faces-config.xml`-only version check with a check across `faces-config.xml`, `web.xml` and `beans.xml`, each matched against the API version the runtime supplies for ITS OWN specification (Faces, Servlet, CDI respectively — not the Jakarta EE platform version), covering the `version` attribute *and* the `xsi:schemaLocation`, reported as `warning` when lagging.
  - **Step 1** additionally detects the project's markup style (plain Faces components vs passthrough elements) and holds every suggestion to it, reports a mixed view as `info`, and never proposes a wholesale conversion between the two — which can only ever be partial, since some components have no passthrough element form (#5).
  - **Step 2** now opens with a necessity check — is the construct still needed, is the mechanism available in the detected version, is it implemented correctly — answered in that order, reporting at the highest level that fires, so a construct that should be deleted is never merely modernised (#6).
  - New **Superseded Constructs** checklist listing hand-rolled code that a built-in has since replaced (flash messages, `immediate="true"` on ajax commands, `isPostback()` guards, custom converters for standard types, `evaluateExpressionGet()`, hand-rolled server push), with severity tied to whether the hand-rolled version has a defect the built-in lacks, and a requirement to state what the built-in covers so partial replacements are not reported as fixes. Includes the near-miss replacements that are not fixes either: a `PhaseListener` setting `Flash.setKeepMessages(true)` on every postback makes messages echo forward, and a `NavigationHandler` decorator is global machinery for a rare case (#6).
  - New **Step 3: Validate Proposed Fixes** — a fix that restructures markup must be re-checked against the rest of the checklist before being reported. For form-boundary changes, enumerate every `ActionSource` and `EditableValueHolder` in the region first, then draw the boundary so each still has a `UIForm` ancestor, watching for those in headers, toolbars, `ui:fragment`/`ui:include` and for decorated elements (#4).
  - **Output Format**: the report opens with the detected versions and each fix is constrained to the runtime version.

## 1.2.4

### Knowledge Base — Updated
- **rules.md**:
  - **View State**: named the `jakarta.faces.PARTIAL_STATE_SAVING` context-param as the actual PSS/FSS toggle (distinct from `STATE_SAVING_METHOD`, which selects `server` vs `client` storage); Mojarra logs a startup WARNING when set to `false`, a reliable signal that FSS is unintentionally active.
  - **Component Rules**: new `<f:websocket>` rule — `channel`/`scope` must be literal constants (a `ValueExpression` throws `IllegalArgumentException` at view build); `user` must resolve to a `Serializable` value (not e.g. `Optional<Long>`).
- **conversion-validation.md**:
  - **Implicit vs Explicit**: corrected the standard by-type converter list (added `UUID`, since Faces 4.1) and noted that `java.util.Date` and the `java.time.*` types are NOT in it, so an input bound to them without `<f:convertDateTime>` throws a `ConverterException` on submit.
  - **`<f:convertDateTime>`**: documented that `timeZone` defaults to GMT, and that `dateStyle`/`timeStyle` don't apply to partial temporals.
  - **Common Pitfalls**: new pitfall — EL config on an attached `<f:converter>`/`<f:validator>`/`<f:convertDateTime>` is resolved only at initial view build and is NOT re-applied on postback (jakartaee/faces#1499).
- **diagnostics.md**:
  - **ViewExpiredException**: view-count caps are implementation-specific (no `jakarta.faces.` spec param bounds them); added Mojarra/MyFaces param names + defaults, and the Mojarra `com.sun.faces.*` → `org.glassfish.mojarra.*` prefix rename in Faces 5.x.
- **configuration.md**:
  - New **ProjectStage-Derived Defaults**: `FACELETS_REFRESH_PERIOD` defaults to `-1` in Production, `0` in every non-Production stage.
  - New **Content Security Policy (Faces 5.0)**: `jakarta.faces.ENABLE_CSP_NONCE` is application-wide, default off, and toggles inline rendering of all `on*` event-handler attributes.

## 1.2.3

### Knowledge Base — Updated
- **rules.md**:
  - **XML Namespaces**: strengthened to a directive — NEVER add `xmlns="http://www.w3.org/1999/xhtml"` as the default namespace on the page root in Faces 4.0+; it is implied by Facelets and leaks into rendered output.
  - **Facelets Rules**: new rule — composite component definition files (under `resources/<library>/<name>.xhtml`) MUST be rooted at `<ui:component>`, never `<html>`+DOCTYPE.
  - **Facelets Rules**: new rule — inside composite component definitions, use the prefix `cc` for `xmlns:cc="jakarta.faces.composite"`; avoid alternative prefixes (`composite`, `c`, `comp`).
  - **Facelets Rules**: clarified that JSTL tag handlers re-execute on every postback (the view tree is rebuilt each request), not only on the initial GET; `c:if`/`c:forEach` can depend on current request state, but heavy JSTL is a per-request cost.
  - **Component Rules**: new rule banning `prependId="false"` on `UIForm`; deprecated in Faces 5.0 (jakartaee/faces#1972) and breaks the `NamingContainer` namespace contract so `findComponent()` and ajax `execute`/`render` references no longer work from outside the form.
  - **Component Rules**: explained why programmatic tree manipulation is costlier than `rendered`/JSTL — components added/removed after the view is built become dynamic components that force a full-state save of the affected subtree on every postback.

## 1.2.2

### OpenCode Support
- New `install-opencode.sh` installer for [OpenCode](https://opencode.ai/) users.
- Creates `opencode.json` with instructions reference.
- Creates `AGENTS.md` as the primary rules file for OpenCode.
- Reuses the existing `.claude/faces/` layout via `install.sh`.
- Supports both project scope (`./opencode.json`, `./AGENTS.md`) and user scope (`~/.config/opencode/opencode.json`, `~/.config/opencode/AGENTS.md`).

## 1.2.1

### Knowledge Base — Updated
- **rules.md**: make sure that **View Metadata** subsection is self-contained (full explainer of `<f:metadata>` / `<f:viewParam>` / `<f:viewAction>`, lifecycle implication, common use cases, getter purity caveat).
- **lifecycle.md**:
  - Phase Shortcuts corrected: a GET request with at least one `<f:viewParam>` and/or `<f:viewAction>` runs the ENTIRE lifecycle, not just Restore View + Render Response.
  - Restore View phase: `<f:viewAction>` invocation timing fixed — invoked during Invoke Application, AFTER `<f:viewParam>` values are applied (not before Apply Request Values).
- **examples.md**: new **GET Search Form** example demonstrating `<f:metadata>` + `<f:viewParam>` + `<f:viewAction>` with a plain HTML `<form method="GET">` and a matching `@ViewScoped` backing bean.

## 1.2.0

### Installer
- `install.sh` now supports `--user` flag for installing into `~/.claude/` (applies to all projects). Default behavior (project-local install into `./.claude/`) is unchanged.
- Layout under `~/.claude/faces/` mirrors the project layout — same paths, same cross-references; no path rewriting needed.

### Knowledge Base — Updated
- **rules.md**:
  - New **Injectable Faces Types** subsection under CDI: typed `@Inject` targets, Servlet-provided types, `jakarta.faces.annotation` Map qualifiers, `@Inject @ManagedProperty`.
  - New **System Events and Phase Listeners** subsection: CDI `@Observes` with `@BeforePhase`/`@AfterPhase`/`PreRenderViewEvent`, etc.
  - New **View Metadata** subsection: `<f:metadata>`, `<f:viewParam>`, `<f:viewAction>` with placement rules.
  - Faces 5.0 added to version lineage.
  - `@ClientWindowScoped` clarified: `jfwid` mechanism, semantics.
  - `/WEB-INF/resources` rule clarified: requires `WEBAPP_RESOURCES_DIRECTORY` context-param.
  - `UISelectMany` rule annotated with `UnsupportedOperationException` reason.
  - Cross-form `execute`/`process` clarified.

## 1.1.0

### Knowledge Base — New Topics
- **Lifecycle** (`topics/lifecycle.md`): request processing lifecycle — phases, shortcuts, ajax lifecycle, PhaseListener.
- **Conversion & Validation** (`topics/conversion-validation.md`): converters, validators, Bean Validation integration, cross-field validation, custom converters/validators.
- **Examples** (`topics/examples.md`): concrete code examples demonstrating best practices from the rules.

### Knowledge Base — Updated
- **rules.md**: reorganized structure; added `immediate="true"` guidance for `EditableValueHolder` and `UICommand`.
- **configuration.md**: added minimal `faces-config.xml` with `<locale-config>`.
- **omnifaces.md**:
  - New **Converters** section listing all OmniFaces converters.
  - New **Validators** section listing all OmniFaces validators.

## 1.0.0

Initial release.

### Knowledge Base
- Core rules (`rules.md`): terminology, view state, XML namespaces, CDI/bean management, scope selection, page authoring, resource rules, component rules, ajax rules, common errors, third-party library references.
- Configuration (`topics/configuration.md`): minimal web.xml, taglib, directory structure.
- Diagnostics (`topics/diagnostics.md`): decision trees for 6 common errors.
- PrimeFaces (`topics/primefaces.md`): PrimeFaces-specific rules and gotchas.
- OmniFaces (`topics/omnifaces.md`): OmniFaces utilities with when/how to use.

### Slash Commands
- `/faces-review`: review Faces code for mistakes, anti-patterns, and rule violations.
- `/faces-migrate`: migrate between Faces versions (JSF 1.x through Faces 4.1).
