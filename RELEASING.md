# Releasing Aktimetrix

Releases are published to [Maven Central](https://central.sonatype.com/) by
[`.github/workflows/release.yml`](.github/workflows/release.yml) when a version tag is pushed. The `release` Maven
profile builds the sources and javadoc jars, signs everything with GPG, and uploads it through the Sonatype Central
Portal.

## One-time setup

1. **Claim the namespace.** Sign in to [central.sonatype.com](https://central.sonatype.com/) and add the namespace of
   the `groupId`. `com.aktimetrix` is verified with a DNS TXT record on the `aktimetrix.com` domain. Without that
   domain, change the `groupId` in both `pom.xml` files to `io.github.arun406`, which is verified through the GitHub
   account.
2. **Create a publishing token** under *View Account → Generate User Token*.
3. **Create a GPG key** for signing and publish its public part:

   ```bash
   gpg --quick-gen-key "Aktimetrix Releases <you@example.com>" rsa4096 sign 2y
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID>        # the value of GPG_PRIVATE_KEY
   ```

4. **Add the repository secrets** under *Settings → Secrets and variables → Actions*:

   | Secret | Value |
   |---|---|
   | `CENTRAL_USERNAME` | the token's username |
   | `CENTRAL_TOKEN` | the token's password |
   | `GPG_PRIVATE_KEY` | the armored private key |
   | `GPG_PASSPHRASE` | its passphrase |

## Releasing a version

```bash
git checkout main && git pull
git tag v0.1.0
git push origin v0.1.0
```

The workflow sets the project version from the tag, runs the tests, and publishes. The release appears on Maven
Central within about 30 minutes. Then update the version in the README and the reference project to the new release.

## Checking a release build locally

```bash
./mvnw -Prelease -Dgpg.skip package     # builds the jar, sources and javadoc, without signing
```
