# Security Model

## Threat Model

| Threat | Impact | Mitigation |
|--------|--------|------------|
| Token interception | Credential theft | OAuth2 PKCE, HTTPS-only |
| Token replay | Unauthorized API access | Short-lived access tokens (1h), refresh rotation |
| Local data access | Configuration leakage | AES-256 encryption at rest (platform keychain) |
| Network eavesdropping | Data exfiltration | TLS 1.3, certificate pinning |
| Drive API breach | Data exposure | appDataFolder isolation, no user visibility |
| Malicious app injection | API misuse | Scope-limited tokens (drive.appdata only) |
| Replay attacks | Stale data injection | Version checks, timestamp validation |
| Man-in-the-middle | Credential/Data theft | Certificate validation, HTTPS enforced |

## Security Layers

```
┌──────────────────────────────────────────────┐
│             Application Layer                  │
│  ┌────────────────────────────────────────┐   │
│  │         OAuth2 PKCE Flow               │   │
│  │  - S256 Code Challenge                 │   │
│  │  - State parameter                     │   │
│  │  - Redirect URI validation             │   │
│  └────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────┐   │
│  │         Token Management               │   │
│  │  - In-memory only (not persisted)      │   │
│  │  - Automatic refresh before expiry     │   │
│  │  - Immediate revocation on logout      │   │
│  └────────────────────────────────────────┘   │
├──────────────────────────────────────────────┤
│             Transport Layer                    │
│  ┌────────────────────────────────────────┐   │
│  │  TLS 1.3 (forced)                      │   │
│  │  Certificate validation                │   │
│  │  HSTS headers                          │   │
│  │  HTTP/2 multiplexing                   │   │
│  └────────────────────────────────────────┘   │
├──────────────────────────────────────────────┤
│             Storage Layer                      │
│  ┌────────────────────────────────────────┐   │
│  │  Platform Secure Storage:              │   │
│  │  - Android: EncryptedSharedPreferences │   │
│  │  - Desktop: OS keychain/Secret Service │   │
│  │  - iOS: Keychain Services              │   │
│  │  - Web: localStorage (encrypted)       │   │
│  └────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────┐   │
│  │  Payload Encryption (future):          │   │
│  │  - AES-256-GCM                         │   │
│  │  - Per-config encryption keys          │   │
│  │  - Key derivation (PBKDF2)             │   │
│  └────────────────────────────────────────┘   │
├──────────────────────────────────────────────┤
│             Provider Layer                     │
│  ┌────────────────────────────────────────┐   │
│  │  Google Drive appDataFolder:           │   │
│  │  - Invisible to users                  │   │
│  │  - No accidental sharing               │   │
│  │  - App-only access scope               │   │
│  │  - No quota consumption                │   │
│  └────────────────────────────────────────┘   │
└──────────────────────────────────────────────┘
```

## Best Practices

### For SDK Consumers

1. **Never hardcode client secrets** — Use environment variables or secure config servers
2. **Validate sync results** — Always check `SyncResult` before trusting data
3. **Handle auth errors** — Implement `AUTH_TOKEN_EXPIRED` and `AUTH_REQUIRED` callbacks
4. **Secure your redirect URI** — Use `http://localhost:<random-port>` to prevent URI conflict
5. **Enable encryption in production** — Set `enableEncryption = true` in `SyncPolicy`

### For Contributors

1. **No secrets in code** — All credentials must enter via configuration, never in source
2. **Audit dependency vulnerabilities** — Run `./gradlew dependencyCheckAnalyze` before merging
3. **Security regression tests** — Add tests for each security boundary change
4. **Token sanitization** — Never log or display token values

## Vulnerability Disclosure

Report security vulnerabilities to **security@cloudsync.io**.

We follow responsible disclosure:
1. Report received → 48h acknowledgment
2. Triage → 5 business days severity assessment
3. Fix → Timeline based on severity (Critical: 7 days)
4. Disclosure → Coordinated public release

## Compliance

- **GDPR** — All data stored via appDataFolder complies with GDPR right to erasure
- **SOC 2** — Token lifecycle and access logging support audit requirements
- **OWASP** — Follows OWASP Mobile Security Testing Guide (MSTG)
