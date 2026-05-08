# Contributing to KMP CloudSync Engine

## Code of Conduct

We are committed to providing a welcoming and inclusive experience. Be respectful, constructive, and professional.

## Quick Start

```bash
# Fork & clone
git clone git@github.com:YOUR_USERNAME/kmp-cloudsync-engine.git
cd kmp-cloudsync-engine

# Build
./gradlew build

# Run tests
./gradlew allTests
```

## Development Workflow

### 1. Commit Convention

We follow **Conventional Commits** for automatic changelog and versioning:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature (→ minor version bump)
- `fix`: Bug fix (→ patch version bump)
- `docs`: Documentation only
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding or improving tests
- `chore`: Build process, CI, dependencies
- `perf`: Performance improvement
- `security`: Security fix
- `BREAKING CHANGE`: Breaking API change (→ major version bump)

**Examples:**
```
feat(sync): add adaptive backoff scheduling
fix(auth): handle token refresh race condition
docs(readme): add multi-cloud setup guide
BREAKING CHANGE: rename SyncConfiguration to SyncPolicy
```

### 2. Branch Strategy

```
main          ── Production-ready releases
  │
develop       ── Integration branch
  │
feature/*     ── New features
fix/*         ── Bug fixes
docs/*        ── Documentation
refactor/*    ── Code refactoring
release/*     ── Release preparation
```

### 3. PR Workflow

1. Create branch from `develop`
2. Make changes following the architecture
3. Write/update tests
4. Run `./gradlew detekt allTests` locally
5. Create PR to `develop`
6. CI runs lint, test, build
7. Squash-merge and delete branch

### 4. Code Standards

**Kotlin Style:**
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- 4-space indentation
- Maximum line length: 120 characters
- Explicit visibility modifiers (no `public` omission)
- Document all public API with KDoc

**Architecture Rules:**
- Domain layer: ZERO platform dependencies
- Data layer: Repository pattern (interface in domain, impl in data)
- Modules: Circular dependencies strictly forbidden
- Testing: Public API + critical internals must be tested

### 5. Testing Requirements

```
src/
├── commonMain/     # Production code
└── commonTest/     # Tests (mirrors production structure)

Test types:
├── UnitTests       # Domain use cases, sync logic
├── IntegrationTests # Data sources, repositories
└── PropertyTests   # State machines, version logic
```

Minimum coverage: **80%** for domain, **70%** for data layer.

### 6. Release Process

```
develop ──(squash)──▶ main ──(tag)──▶ v1.0.0
                            │
                            └──▶ GitHub Release + Maven Central
```

1. Create `release/x.y.z` branch from `develop`
2. Update version in `gradle.properties`
3. Run full test suite
4. Generate changelog
5. Create PR to `main`
6. After merge, tag `vx.y.z`
7. CI/CD publishes and creates release

## Questions?

Open a [Discussion](https://github.com/cloudsync/kmp-cloudsync-engine/discussions) or join our [Discord](https://discord.gg/cloudsync).
