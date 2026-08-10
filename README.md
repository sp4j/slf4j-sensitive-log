# slf4j-sensitive-log

Backend-agnostic SLF4J 2.x provider that encrypts sensitive arguments in logs.

## Why you need this

Modern applications ship logs to centralized log platforms — **Elastic/Kibana**, **Grafana Loki**, **Datadog**, **Splunk**, **AWS CloudWatch**, **Google Cloud Logging** and others. These systems are designed for broad access: developers, ops, support teams and sometimes third-party contractors all read the same log streams.

Without protection, a single `log.info("User {} authenticated, token {}", username, token)` line means:
- JWTs, API keys and session tokens are stored in plain text in your log index forever.
- Personally identifiable information (PII) — emails, phone numbers, passport numbers, payment card numbers (PAN) — leaks to everyone with log read access.
- You are in breach of **GDPR**, **PCI DSS**, **HIPAA** and similar regulations that require you to protect personal data at rest and in transit.

`slf4j-sensitive-log` solves this **without changing your logging infrastructure**:

| Scenario | Marker | What you get in the log                         |
|---|---|-------------------------------------------------|
| Auth token, password | `[SENSITIVE]` | AES-256 encrypted hex — only you can decrypt it |
| Credit card number (PAN) | `[MASKED_4]` | `****56789012****`                              |
| Email address | `[MASKED_FIRST_3]` | `***r@example.com`                              |
| Phone number | `[MASKED_LAST_4]` | `+48 796 ***-**00`                              |
| Any secret value | `[MASKED]` | `**************`                                |

You keep **full observability** (you can still correlate events, debug issues, decrypt when needed) while keeping **logs safe to ship** to any cloud platform and safe to share with third parties.

## What it does

If a log message contains marker `[SENSITIVE]`, matching `{}` arguments are encrypted with AES-256 and written as lowercase hex.
You can also use masking markers for non-cryptographic obfuscation:

- `[MASKED] {}` -> replace the whole value with `*`
- `[MASKED_N] {}` -> replace first and last `N` chars with `*`
- `[MASKED_FIRST_N] {}` -> replace first `N` chars with `*`
- `[MASKED_LAST_N] {}` -> replace last `N` chars with `*`

Example:

```java
log.info("My password is [SENSITIVE] {}", "secret");
log.info("PAN [MASKED_4] {}", "1234567890123456");
log.info("Login [MASKED_FIRST_3] {}", "john.doe");
log.info("Token [MASKED_LAST_6] {}", "abc123def456");
```

Result (example):

```text
My password is [SENSITIVE] 53616c7465645f5f7c1e673a368c34436b8fb922dcb999c9e099ed6cc99f4e6b
PAN [MASKED_4] ****56789012****
Login [MASKED_FIRST_3] ***n.doe
Token [MASKED_LAST_6] abc123******
```

## Backend-agnostic behavior

- Works with plain Java and any framework using SLF4J 2.x (Spring, Guice, Micronaut, Quarkus, etc.).
- Works with different SLF4J backends (for example `slf4j-simple`, Logback).
- No hard runtime dependency on Logback when another backend is used.

## How it works

- Registered via Java SPI: `META-INF/services/org.slf4j.spi.SLF4JServiceProvider`.
- `SensitiveLogServiceProvider` discovers the next SLF4J provider and delegates to it.
- Non-Logback backends are wrapped by `SensitiveLoggerFactory` / `SensitiveLogger`.
- For Logback, a turbo filter is installed (via reflection) to sanitize arguments before formatting.

## Java version

- Compiled for Java 8 (`maven.compiler.release=8`).

## Dependency

```xml
<dependency>
    <groupId>io.github.sp4j</groupId>
    <artifactId>slf4j-sensitive-log</artifactId>
    <version>0.4.0</version>
</dependency>
```

## Required key configuration

Set AES key via one of:

1. Environment variable `SENSITIVELOG_AES_KEY`
2. JVM property `-Dsensitivelog.aes-key=...`

Examples:

```bash
export SENSITIVELOG_AES_KEY=0123456789abcdef0123456789abcdef
```

```bash
java -Dsensitivelog.aes-key=0123456789abcdef0123456789abcdef -jar app.jar
```

## Build and test

```bash
mvn clean test
```

## CLI tools

Build jar:

```bash
mvn -q -DskipTests package
```

### Generate key

```bash
java -cp target/slf4j-sensitive-log-0.4.0.jar io.github.sp4j.sensitivelog.tool.SensitiveLogKeyGenerator
```

### Encrypt

```bash
java -cp target/slf4j-sensitive-log-0.4.0.jar io.github.sp4j.sensitivelog.tool.SensitiveLogEncrypt "0123456789abcdef0123456789abcdef" "secret"
```

### Decrypt

```bash
java -cp target/slf4j-sensitive-log-0.4.0.jar io.github.sp4j.sensitivelog.tool.SensitiveLogDecrypt "0123456789abcdef0123456789abcdef" "<encrypted-hex>"
```

### Decrypt with OpenSSL (no Java)

```bash
KEY="0123456789abcdef0123456789abcdef"
ENC="<encrypted-hex-from-log>"
printf '%s' "$ENC" | xxd -r -p | openssl enc -d -aes-256-cbc -pbkdf2 -md sha256 -iter 10000 -pass pass:"$KEY"
```

## Provider selection

Set `-Dslf4j.provider=io.github.sp4j.sensitivelog.provider.SensitiveLogServiceProvider` only when deterministic provider selection is needed and multiple providers exist on classpath.
