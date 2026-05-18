# GraalVM PGO pipeline (scripts stubbed, profile wired)

Profile-guided-optimized native binary build for securedoc. The Maven profile (`native-release-pgo`) and Windows override (`native-release-pgo-platform-windows`) are wired in `pom.xml`; the supporting `.sh` / `.ps1` scripts are placeholders -- the build wrapper, db reset, comparison, and winner-selection scripts are nearly drop-in, but `workload.sh` / `workload.ps1` runs a placeholder GET against `/` and needs a real auth + endpoint mix before this pipeline produces a meaningful PGO profile. See the TODO list at the bottom.

## What this is

The pipeline builds three native binaries (normal, instrumented, optimized), load-tests each against a docker-compose MySQL stack for a configurable workload window, and ships the higher-throughput of normal-vs-optimized as the deploy artifact.

## Canonical invocation

```bash
bash scripts/graalvm-pgo/pgo-build.sh install -DskipTests
# Windows:
pwsh scripts/graalvm-pgo/pgo-build.ps1 install -DskipTests
```

The wrapper invokes `./mvnw -P native-release-pgo` with an `EXIT/INT/TERM` trap that tears down the docker compose stack on every exit path: success, failure, or interruption. Calling `mvn` directly with the profile also works for the happy path -- the post-integration-test execution handles teardown -- but a halted build leaves MySQL orphaned, and you'd have to `docker compose -f scripts/graalvm-pgo/docker-compose.yml down -v` by hand.

## Maven lifecycle layout

Single reactor, no nested mvn, no shell orchestrator. The profile builds three native binaries (normal, instrumented, optimized), runs a 10-worker x 5-minute load workload against each, truncates the app tables between tests so each binary starts from an equivalent empty-app state, prints a side-by-side comparison, and copies the better of normal-vs-optimized to `target/securedoc-*-runner`.

| Phase | Action |
|---|---|
| `prepare-package` | `docker compose up` (MySQL 8); `quarkus:build` #1 -> **normal** native binary (no PGO flags) |
| `package` | save normal-runner; `pgo-test-binary.sh normal` runs the load workload and captures metrics; `pgo-reset-db.sh` truncates app tables; `quarkus:build` #2 -> **instrumented** native binary (`--pgo-instrument`) |
| `pre-integration-test` | save instrumented-runner; `pgo-test-binary.sh instrumented` runs the load workload (captures `default.iprof`); `pgo-reset-db.sh` truncates app tables; `quarkus:build` #3 -> **optimized** native binary (`--pgo=target/pgo-run/default.iprof`) |
| `integration-test` | save optimized-runner; `pgo-test-binary.sh optimized` runs the load workload; `pgo-compare.sh` prints the table; `pgo-select-winner.sh` copies the higher-throughput binary back to `target/securedoc-*-runner` and deletes the throwaway instrumented binary |
| `post-integration-test` | `docker compose down -v` |

The relaxed gate in `parallel-workload.sh` accepts any iprof with >= 1 successful worker, so a transient flake doesn't abort the run. `exec-maven-plugin` is declared before `quarkus-maven-plugin` in the profile so within any shared phase the exec executions (save + test) run before the next `quarkus:build`, giving the interleaved build -> save -> test -> build -> save -> test -> build -> save -> test sequence in a single reactor.

## Load workload per binary

`pgo-test-binary.sh` -> `parallel-workload.sh` -> `workload.sh`: N concurrent sessions x DURATION_SEC seconds of representative traffic. Binary launches with prod-matched flags (`-Xmx512m`, `-Djdk.virtualThreadScheduler.parallelism=$(nproc)`).

The actual workload steps live in `workload.sh` and currently do a placeholder GET against `/`. The real workload needs decisions documented in the `workload.sh` header.

## Files in this directory

| File | Role |
|---|---|
| `pgo-build.sh` / `.ps1` | Canonical wrapper with EXIT/INT/TERM teardown trap |
| `pgo-test-binary.sh` / `.ps1` | Boots one binary, drives `parallel-workload.sh` against it, captures metrics |
| `parallel-workload.sh` / `.ps1` | Spawns N concurrent `workload.sh` instances |
| `workload.sh` / `.ps1` | Per-worker traffic loop against the local binary (STUB: placeholder) |
| `pgo-reset-db.sh` / `.ps1` | Truncates app tables between test runs |
| `pgo-compare.sh` / `.ps1` | Prints the side-by-side comparison table |
| `pgo-select-winner.sh` / `.ps1` | Copies the higher-throughput binary to the canonical runner path |
| `docker-compose.yml` | MySQL 8, the only backing service securedoc needs |
| `db-init/01-grants.sql` | First-boot init script that grants `securedoc_user` privileges |

## TODO -- what's missing before this pipeline produces a meaningful PGO profile

1. **Workload semantics in `workload.sh` / `.ps1`** -- placeholder is `GET /`. Real workload needs:
   - Admin row seeding (direct MySQL insert vs. a pgo-only seed endpoint)
   - Login flow that bypasses email OTP (either short-circuit gate, or pluck the latest `staff_otp` row from MySQL)
   - Representative endpoint mix (admin nav, staff management, resident request landing, verifier)
2. **Quarkus health probe wiring** -- `pgo-test-binary.sh` polls `/` for readiness; switch to `/q/health/ready` once `quarkus-smallrye-health` is added.
3. **Pre-pull `mysql:8`** at `process-test-classes` (like the existing `pull-mysql-image` exec-maven-plugin execution does for the test suite) so a slow Docker Hub connection doesn't trip the 30s no-progress watchdog the first time.
4. **Decide on tighter gate** -- current "1 worker succeeded" gate is for bring-up. Once the workload is real, raise to a percentage of expected throughput.
5. **Wire `deploy.yml` to invoke the PGO profile** -- the existing workflow uses `-Pnative-release`. Switching to `-Pnative-release-pgo` is the deploy-side step once the workload is real.
