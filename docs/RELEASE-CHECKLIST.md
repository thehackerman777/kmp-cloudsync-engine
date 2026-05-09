# Release Checklist

## Pre-release

- [ ] All commits merged to `main`
- [ ] Version bumped in `gradle.properties`
- [ ] CHANGELOG updated with release notes
- [ ] CI passes on target commit

## Build

```bash
# Build all artifacts
./gradlew :engine:buildAllArtifacts --no-daemon

# Publish to MavenLocal (for local testing)
./scripts/publish-local.sh
```

## Verify

```bash
# Validate artifact sizes and existence
./scripts/validate-artifacts.sh

# Test each platform
./scripts/test-desktop.sh    # Run desktop sample
./scripts/test-web.sh        # Validate JS bundle
./scripts/test-android.sh    # Compile Android sample
```

## Release Steps

1. **Tag the release**
   ```bash
   git tag -a v0.2.0 -m "Release v0.2.0"
   git push origin v0.2.0
   ```

2. **Publish to Maven Central (if configured)**
   ```bash
   ./gradlew :engine:publish --no-daemon
   ```

3. **Create GitHub Release**
   - Generate release notes from CHANGELOG
   - Attach artifacts:
     - `engine/build/libs/kmp-cloudsync-engine-desktop-*-all.jar`
     - `engine/build/outputs/aar/engine-release.aar`
     - `engine/build/outputs/js/engine.js`
     - `engine/build/outputs/aar/engine-fat-release.aar`

4. **Verify published artifacts**
   - [ ] Maven Central / GitHub Packages accessible
   - [ ] Desktop sample builds with published version
   - [ ] Android sample builds with published version

## Post-release

- [ ] Update sample apps to use new version
- [ ] Update docs to reflect any API changes
- [ ] Announce release on relevant channels
