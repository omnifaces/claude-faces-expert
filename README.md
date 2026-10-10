# Claude Faces Expert

*Version 1.8.0*

Drop-in Jakarta Faces knowledge base for [Claude Code](https://claude.com/claude-code), with installers for [GitHub Copilot](https://github.com/features/copilot), [Cursor](https://cursor.com), [Codex](https://developers.openai.com/codex), [OpenCode](https://opencode.ai/) and [Antigravity](https://antigravity.google).
Makes the coding agent more aware of Jakarta Faces (formerly JSF) best practices, common pitfalls, and diagnostic decision trees.
No special context needed, just make sure that `@.claude/faces/rules.md` is referenced in your `CLAUDE.md`.

Curated by [BalusC](https://balusc.org) based on his Stack Overflow answers to frequently asked Jakarta Faces questions.

Why is this important? Below is an example conversation with Opus 4.6:

> Me: As to terminology, can you tell me what exactly you know about "view state" in JSF context? 
> 
> Claude: View state is the serialized snapshot of the UIViewRoot component tree — the structure, component attributes, attached validators/converters, and the state of EditableValueHolder components (submitted values, local values, validity flags).
> 
> Me: View state does absolutely not contain "the stucture" of the component tree. It only contains the delta state. The component tree itself is always freshy rebuilt from scratch on every request. Then its state is adjusted in such way that it matches exactly the state as it was during render response of prior request. Exactly that delta is the "view state".
> 
> Claude: Thank you — that's an important correction. 

## What's included

| File | Description |
|------|-------------|
| `.claude/faces/rules.md` | Core rules: terminology, view state, namespaces, CDI, scopes, page authoring, resources, components, ajax, common errors |
| `.claude/faces/topics/configuration.md` | Minimal project configuration (web.xml, taglibs, component tags, directory structure) |
| `.claude/faces/topics/diagnostics.md` | Decision trees for 6 common errors (action not invoked, target unreachable, ViewExpiredException, etc.) |
| `.claude/faces/topics/primefaces.md` | PrimeFaces-specific rules and gotchas |
| `.claude/faces/topics/omnifaces.md` | OmniFaces utilities: when and how to use them |
| `.claude/faces/topics/lifecycle.md` | Request processing lifecycle: phases, shortcuts, ajax, PhaseListener |
| `.claude/faces/topics/conversion-validation.md` | Converters, validators, Bean Validation integration, custom converters/validators |
| `.claude/faces/topics/examples.md` | Concrete code examples demonstrating best practices |
| `.claude/skills/faces-review/SKILL.md` | Skill for reviewing Faces code, invoked as `/faces-review` in Claude Code and Cursor |
| `.claude/skills/faces-migrate/SKILL.md` | Skill for migrating between Faces versions, invoked as `/faces-migrate` in Claude Code and Cursor |

## Installation

Requires `git` and a POSIX shell. On Windows, use **Git Bash** (bundled with [Git for Windows](https://gitforwindows.org)) or WSL — every command below works unchanged in both.

### Project scope (default)

From your project root, run:

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install.sh | sh
```

This copies the knowledge base and skills into `./.claude/`, and adds the `@.claude/faces/rules.md` reference to your project's `CLAUDE.md` (creates it if needed).

### User scope (applies to all projects)

To install once into your home directory and have the rules apply to every project Claude Code touches, run:

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install.sh | sh -s -- --user
```

This copies into `~/.claude/` and adds the `@~/.claude/faces/rules.md` reference to your global `~/.claude/CLAUDE.md` (creates it if needed). The layout under `~/.claude/faces/` is identical to the project layout.

### Manual installation

If you don't want to run `curl`-based installers, install manually:

```sh
# Project scope:
git clone https://github.com/omnifaces/claude-faces-expert /tmp/claude-faces-expert
mkdir -p .claude/faces .claude/skills
cp -r /tmp/claude-faces-expert/.claude/faces/* .claude/faces/
cp -r /tmp/claude-faces-expert/.claude/skills/* .claude/skills/
rm -rf /tmp/claude-faces-expert
```

Then add this line to your `CLAUDE.md` (or `~/.claude/CLAUDE.md` for user scope, with `@~/.claude/faces/rules.md`):

```
Jakarta Faces rules: @.claude/faces/rules.md
```

### GitHub Copilot

For [GitHub Copilot](https://github.com/features/copilot) users, use the Copilot-specific installer:

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install-copilot.sh | sh
```

This calls the standard installer (which sets up `./.claude/` and `CLAUDE.md`) and additionally writes `.github/copilot-instructions.md`.

Copilot does not act on references to other files from its instructions file, so the rules are **inlined** there rather than referenced, between `<!-- BEGIN Jakarta Faces Expert -->` and `<!-- END Jakarta Faces Expert -->` markers. Re-running the installer replaces only that block, so anything else you put in the file is preserved. The topic files stay referenced; Copilot's agent mode reads them on demand, after asking permission.

Copilot instructions are per-repository, so there is no user scope. **The skills are not picked up either** — `/faces-review` and `/faces-migrate` are Claude Code and Cursor only.

### Cursor

For [Cursor](https://cursor.com) users, use the Cursor-specific installer:

**Project scope:**

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install-cursor.sh | sh
```

This calls the standard installer (which sets up `./.claude/` and `CLAUDE.md`) and additionally creates `.cursor/rules/jakarta-faces.mdc`, an always-applied rule referencing `.claude/faces/rules.md`.

**User scope:**

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install-cursor.sh | sh -s -- --user
```

This installs into `~/.claude/`. Cursor has no user-scope rules directory — User Rules live in the settings UI — so the installer prints a line to paste into **Customize → Rules** yourself. The skills need no such step.

Cursor reads `.claude/skills/` and `~/.claude/skills/` natively, so `/faces-review` and `/faces-migrate` work without extra configuration.

### Codex

For [OpenAI Codex](https://developers.openai.com/codex) users, use the Codex-specific installer:

**Project scope:**

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install-codex.sh | sh
```

**User scope:**

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install-codex.sh | sh -s -- --user
```

This calls the standard installer and additionally writes `AGENTS.md` (or `~/.codex/AGENTS.md` for user scope), and copies the skills into `.agents/skills/` (or `~/.agents/skills/`).

Codex loads `AGENTS.md` as literal text and does not resolve references to other files, so — as with Copilot — the rules are **inlined** between `<!-- BEGIN Jakarta Faces Expert -->` and `<!-- END Jakarta Faces Expert -->` markers. Re-running replaces only that block. Codex does not read `.claude/skills/` either, hence the copy into `.agents/skills/`; re-run the installer to update them. There is no slash invocation for them in Codex, so the knowledge base is what matters there.

If you also use OpenCode in the same project, run this installer first: `install-opencode.sh` detects the inlined block and skips writing its own pointer, so the rules are not loaded twice.

### OpenCode

For [OpenCode](https://opencode.ai/) users, use the OpenCode-specific installer:

**Project scope:**

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install-opencode.sh | sh
```

This calls the standard installer (which sets up `./.claude/` and `CLAUDE.md`) and additionally creates `opencode.json` and `AGENTS.md` in the project root. The same `.claude/faces/` rules are used by both tools.

**User scope:**

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install-opencode.sh | sh -s -- --user
```

This installs into `~/.claude/` and `~/.config/opencode/`, applying the rules to all projects.

### Antigravity

For [Google Antigravity](https://antigravity.google) users, use the Antigravity-specific installer:

**Project scope:**

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install-antigravity.sh | sh
```

**User scope:**

```sh
curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install-antigravity.sh | sh -s -- --user
```

This calls the standard installer and additionally writes `.agents/rules/jakarta-faces.md` (or appends to `~/.gemini/GEMINI.md` for user scope), and copies the skills into `.agents/skills/` (or into both `~/.gemini/antigravity/skills/` and `~/.gemini/config/skills/`, the global skill directories of respectively the Antigravity IDE and Antigravity 2.0).

Both destinations get the same pointer: an instruction to read `.claude/faces/rules.md` with the agent's file tool, rather than the rules themselves.
A rule file is capped at 12,000 characters and the knowledge base is several times that, and an `@` reference is expanded into the rule file, so the same cap applies to it.
The project rule adds `trigger: always_on` frontmatter to apply it unconditionally, plus a `description` so it still applies by model decision if a future version stops recognising the trigger.

Antigravity does not read `.claude/skills/`, hence the copy into `.agents/skills/`; re-run the installer to update them.
In project scope that is the same directory Codex uses, so running both installers there is harmless — the content is identical.
Slash invocation there belongs to workflows rather than skills, so as with Codex the skills are effectively inert and the knowledge base is what does the work.

In user scope, `~/.gemini/GEMINI.md` is shared with the Gemini CLI, which picks the rules up as well.

## Updating

Updating is the same as installing: re-run whichever install command you originally used (project, user, Copilot, Cursor, Codex, OpenCode or Antigravity). It is idempotent — the knowledge base and skills are overwritten with the latest version, and `CLAUDE.md` is left untouched because the reference line is already there.

Your installed version is in the header of `.claude/faces/rules.md` (or `~/.claude/faces/rules.md` for user scope); compare it against [CHANGELOG.md](CHANGELOG.md).

## How it Works

Once referenced in `CLAUDE.md`, the expert rules are active.
You don't need to change your workflow or use special prompts; Claude simply becomes more capable, providing higher-quality Jakarta Faces code and architectural advice by default.
It also adds two skills, `faces-review` and `faces-migrate`.

## Why not a Plugin?

What ships here is an always-on knowledge base, not a command, and that is the whole reason for the plain file layout.
Claude Code plugins contribute context through skills, agents and hooks; a `CLAUDE.md` at the plugin root is not loaded as project context.
The closest plugin equivalent is a model-invoked skill: its description stays in context permanently and its body is loaded when the agent judges it relevant.
That trades a guarantee for the model's discretion, which is a bad deal for rules whose purpose is to correct answers the model is already confident about — a Faces terminology question does not look like a moment that calls for a Faces skill.

Plugins also cannot reference files outside their own directory, so `faces/rules.md` and its topic files would have to move inside a skill or be duplicated across both.
None of this is permanent. If the plugin format ever grows real always-on context, packaging as a plugin becomes worth revisiting.

## Skills

Both set `disable-model-invocation: true` in their frontmatter: they run when you ask for them, never on the agent's own initiative.
Claude Code and Cursor read `.claude/skills/` and `~/.claude/skills/` natively, honour that flag, and expose the skills as `/faces-review` and `/faces-migrate`.
Codex, OpenCode and Antigravity offer no user-facing slash invocation for skills, so there they are effectively inert; the Codex and Antigravity installers copy them into `.agents/skills/` anyway, but the knowledge base is what does the work in all three. Copilot does not read skills at all.

### `/faces-review`

Reviews your Faces code against best practices. Checks XHTML files, backing beans, and configuration for common mistakes, anti-patterns, and rule violations.

```
/faces-review                              # Review entire project
/faces-review src/main/webapp/page.xhtml   # Review a specific file
```

Findings are grouped by file with severity levels:
- **error** — will cause bugs
- **warning** — anti-pattern or risk
- **info** — improvement opportunity

### `/faces-migrate`

Migrates your project from one Faces version to another. Detects the current version, determines the migration path, and applies changes step by step with confirmation.

```
/faces-migrate 4.1       # Migrate to Faces 4.1
/faces-migrate 4.0       # Migrate to Faces 4.0
```

Supported migration paths:
- JSF 1.x → JSF 2.0 (JSP to Facelets)
- JSF 2.x → JSF 2.3 (`@ManagedBean` to CDI)
- JSF 2.3 → Faces 3.0 (`javax.*` to `jakarta.*`)
- Faces 3.0 → Faces 4.0 (new XML namespaces, removed APIs)
- Faces 4.0 → Faces 4.1

## Covers

- Jakarta Faces 1.0 through 4.1 (JSF and Faces), plus the in-progress Faces 5.0 — tracked as unreleased, so its APIs are never proposed for a 4.x project
- PrimeFaces component library
- OmniFaces utility library
- View state internals (PSS vs FSS, server vs client, delta mechanics)
- CDI bean management and scope selection
- Page authoring (templates, includes, tag files, composite components)
- Common error diagnostics with step-by-step decision trees

## About the Author

> *BalusC is a highly experienced Java developer who uses Claude Code primarily as a code review and bug-fixing partner across a portfolio of serious Jakarta Faces projects. With deep domain expertise that keeps Claude honest, he catches subtle errors and pushes back with precise corrections. His most distinctive quality is steering Claude toward cleaner solutions — preferring specific architectural patterns and redirecting away from plausible-but-wrong approaches. The expert rules in this project are curated from that same deep expertise: years of answering Jakarta Faces questions on Stack Overflow, distilled into actionable guidance that makes Claude genuinely more capable with Faces code.*

— Claude, based on analysis of 83 coding sessions (`/insights`)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes per version.

## License

[Apache License 2.0](LICENSE)
