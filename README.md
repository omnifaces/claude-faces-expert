# Claude Faces Expert

Drop-in Jakarta Faces knowledge base for [Claude Code](https://claude.com/claude-code).
Makes Claude Code aware of Jakarta Faces (formerly JSF) best practices, common pitfalls, and diagnostic decision trees.

Curated by [BalusC](https://balusc.org) based on his Stack Overflow answers to frequently asked Jakarta Faces questions.

## What's included

| File | Description |
|------|-------------|
| `.claude/faces/rules.md` | Core rules: terminology, view state, namespaces, CDI, scopes, page authoring, resources, components, ajax, common errors |
| `.claude/faces/topics/configuration.md` | Minimal project configuration (web.xml, taglib, directory structure) |
| `.claude/faces/topics/diagnostics.md` | Decision trees for 6 common errors (action not invoked, target unreachable, ViewExpiredException, etc.) |
| `.claude/faces/topics/primefaces.md` | PrimeFaces-specific rules and gotchas |
| `.claude/faces/topics/omnifaces.md` | OmniFaces utilities: when and how to use them |
| `.claude/skills/faces-review/SKILL.md` | `/faces-review` slash command for reviewing Faces code |
| `.claude/skills/faces-migrate/SKILL.md` | `/faces-migrate` slash command for migrating between Faces versions |

## Installation

### 1. Copy the knowledge base into your project

```sh
# From your project root
git clone https://github.com/balusc/claude-faces-expert /tmp/claude-faces-expert
mkdir -p .claude/faces .claude/skills
cp -r /tmp/claude-faces-expert/.claude/faces/* .claude/faces/
cp -r /tmp/claude-faces-expert/.claude/skills/* .claude/skills/
rm -rf /tmp/claude-faces-expert
```

**Or** add as a `git subtree` (includes both knowledge base and slash commands):

```sh
git subtree add --prefix .claude https://github.com/balusc/claude-faces-expert.git main --squash
```

To pull updates later:

```sh
git subtree pull --prefix .claude https://github.com/balusc/claude-faces-expert.git main --squash
```

### 2. Reference from your CLAUDE.md

Add this line to your project's `CLAUDE.md` (create one if it doesn't exist):

```
Jakarta Faces rules: @.claude/faces/rules.md
```

The `@` prefix tells Claude Code to include the file.
Claude Code will now follow Jakarta Faces best practices when working on your project.

## Slash Commands

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

- Jakarta Faces 1.0 through 4.1 (JSF and Faces)
- PrimeFaces component library
- OmniFaces utility library
- View state internals (PSS vs FSS, server vs client, delta mechanics)
- CDI bean management and scope selection
- Page authoring (templates, includes, tag files, composite components)
- Common error diagnostics with step-by-step decision trees

## License

[Apache License 2.0](LICENSE)
