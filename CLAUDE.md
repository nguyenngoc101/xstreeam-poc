# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Build
mvn clean compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=XMLSignatureUtilTest

# Run a single test method
mvn test -Dtest=XMLSignatureUtilTest#signRootNodeWithKeyPairAndValidate

# Run demo
mvn exec:java -Dexec.mainClass="demo.XmlJsonDemo"
```

## Architecture Overview

This is a Java 17 POC for XML digital signatures and XStream serialization.

### Core Components

**signature package** - XML Digital Signature operations using Java XML-DSig API:
- `XMLSignatureUtil` - Signs/validates XML documents with RSA-SHA256. Uses `XMLSignatureFactory` with DOM provider. Supports enveloped signatures with KeyInfo.
- `KryptoUtil` - RSA 2048-bit key pair generation and file storage (PKCS8 for private, X509 for public keys)
- `DocumentUtil` - XML parsing with XXE protection enabled via `createSecureDocumentBuilderFactory()`

**model package** - Domain objects with XStream annotations:
- Classes use `@XStreamAlias` and `@XStreamAsAttribute` for XML mapping

**demo package** - Conversion examples:
- `XmlJsonDemo` - Demonstrates Object <-> XML <-> JSON using XStream + FastJSON/Jettison

### Security Considerations

- XStream is configured with explicit type whitelisting via `NoTypePermission.NONE` + `allowTypes()` to prevent RCE
- DocumentUtil protects against XXE by disabling DOCTYPE declarations and external entities
- Keys are stored as raw encoded bytes (not PEM format)
