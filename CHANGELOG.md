# Changelog

## 1.2.0

### Installer
- `install.sh` now supports `--user` flag for installing into `~/.claude/` (applies to all projects). Default behavior (project-local install into `./.claude/`) is unchanged.
- Layout under `~/.claude/faces/` mirrors the project layout — same paths, same cross-references; no path rewriting needed.

### Knowledge Base — Updated
- **rules.md**:
  - New **Injectable Faces Types** subsection under CDI: typed `@Inject` targets, Servlet-provided types, `jakarta.faces.annotation` Map qualifiers, `@Inject @ManagedProperty`.
  - New **System Events and Phase Listeners** subsection: CDI `@Observes` with `@BeforePhase`/`@AfterPhase`/`@PreRenderView`, etc.
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
