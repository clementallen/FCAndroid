# Flight Club

Cross-country hang gliding and paragliding. Find the lift, climb to cloudbase,
glide to the next thermal, get round the course before the day dies.

Originally a Java applet by Dan Burton (2001–2003), ported to Android, and now
also to the web. GPL.

## What is where

    core/     the engine: flight physics, thermals, glider AI, 3D geometry.
              Plain Java, no platform dependencies at all - `:core:checkPortable`
              fails the build if a platform import creeps in.
    android/  the Android app: activities, the in-app game server, and the
              GLES20 and SharedPreferences backends for core's interfaces.
    web/      the browser build: TeaVM compiles :core to JavaScript, plus the
              WebGL, Web Audio and localStorage backends.
    shell/    the web UI in TypeScript: menus, HUD, settings, controls.
    worker/   the Cloudflare Worker that serves it.

The engine is compiled, not translated. The same Java drives both platforms,
which is what keeps them honest about behaving the same.

## Building the web version

    cd shell
    npm install
    npm run dev      # Vite dev server, rebuilds the engine first
    npm run build    # -> dist/
    npm run deploy   # -> Cloudflare

`npm run engine` alone runs the Gradle TeaVM build. Requires JDK 17+ (TeaVM's
compiler needs it; the engine itself is Java 8).

Vite's hot reload watches `shell/` only - it has no idea the engine came from
Java. `dev` compiles it once at startup, so **after editing anything under
`core/` or `web/`, run `npm run engine` and reload**. Editing TypeScript, CSS
or HTML hot-reloads as usual.

## Building the Android app

    ./gradlew :android:assembleDebug

Needs an Android SDK. `:android` is only included in the build when one is
present, so `:core` and `:web` work on a machine without it. Where there is no
SDK at all, `tools/typecheck-android.sh` compiles the module against published
stubs - a typecheck, not a build, but it catches wiring mistakes.

## Checking the port did not change the flight

There is no unit test suite. What there is instead is a trace: run the
simulation headless for 2000 frames and write down where the glider went.

    ./gradlew :web:checkTrace

This runs it on the JVM and again compiled to JavaScript and compares the two,
against the golden traces in `core/src/test/golden/`. Regenerate those with:

    ./gradlew :core:jar
    java -cp core/build/libs/core.jar com.cloudwalk.harness.TraceMain \
        assets t001 0 2000 25 > core/src/test/golden/t001-pg.trace

The comparison has a tolerance, because the two can never be bit-identical:
`Cloud.getLift` raises base proximity to the 1000th power, the JVM and V8
disagree by an ulp on `Math.pow`, and altitude feeds back into the result. The
drift is around 0.05% over a full flight - far below anything visible, and far
below the 0.1 at which the multiplayer protocol resyncs a position anyway.

## Multiplayer

Not in the web build yet. The protocol is line-based ASCII and `XCNet` now
holds all of it behind a `NetLink`, so the browser needs a WebSocket
implementation of that interface and a server. `XCGameServer` does not port -
Workers cannot listen on a TCP socket - but it is a relay with a 10-second
heartbeat, which is close to what a Durable Object does natively. `/ws` is
reserved for it.
