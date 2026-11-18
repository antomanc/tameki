# Spare

Spare is an open source Android app that tracks expenses and subscriptions and shows where you
can save. Local-first: no account, no server, no API keys, nothing leaves the phone unless you
export it. Italian and English.

This repository currently holds the module skeleton: the build, the module boundaries and a
placeholder shell with four tabs. Product scope is in `PRODUCT.md`, domain contracts in
`docs/spec/domain.md`.

## Build

Install an Android SDK with platform 37, then:

```bash
scripts/verify.sh
```

That runs the convention checks, unit tests, lint and both debug and release APKs. For a quick
loop use `./gradlew :core:model:test` or `./gradlew :app:assembleDebug`.

## License

Spare is free software: you can redistribute it and/or modify it under the terms of the GNU
General Public License as published by the Free Software Foundation, version 3. See `LICENSE`.

`LICENSE-EXCEPTION.md` adds the permission needed to link Spare with Google Play services,
Google ML Kit and the Android Jetpack libraries, which are not GPL-compatible on their own.
