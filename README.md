# Cake

A Kotlin / Jetpack Compose implementation of the Waracle cake-list exercise. Android 9 (API 28) and later. Includes a warm Material 3 light/dark design, accessible clickable rows, descriptions in a dialog, refresh and retry, image fallbacks, and fade/slide entry animations.

## Run

Open this directory in Android Studio with support for AGP 9.2.1. Use Android Studio's bundled JDK (21 or later) and install Android SDK 37.0. `local.properties` should point to your SDK; it is machine-specific and excluded from source control.

Run the `app` configuration on an emulator or device. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Release builds are minified and unsigned; configure your own signing for distribution. INTERNET is the only requested permission. No credentials are needed.

## Architecture

Dependency direction:

```text
Compose screen -> CakeViewModel -> GetCakesUseCase -> CakeRepository
                                                       |
                                               CakeRepositoryImpl -> CakeApi
```

- `domain`: Kotlin cake values, repository contract, typed expected failures, and use case. Deduplication uses full entry equality after whitespace normalization; different recipes with the same title remain distinct. Sorting is deterministic, case insensitive, and locale independent.
- `data`: Retrofit/Kotlin serialization endpoint and straightforward DTO-to-domain mapping (null fields become empty strings). The repository runs work on an injected IO dispatcher and maps network, HTTP and parsing failures. Cancellation propagates. There are no catch-all exception handlers that hide programming errors.
- `presentation`: ViewModel exposes a `StateFlow<CakeUiState>` with explicit `Loading`, `Empty`, `Content` and `Failure` states. `Content` holds the immutable cake collection and refresh status/error; `Failure` holds an immutable `CakeUiError` enum, plus a selected-cake `StateFlow`. Compose collects state with lifecycle awareness and renders a stateless screen. Both refresh and retry fetch a fresh collection. Existing rows remain visible while refreshing and after refresh failures; rapid taps cannot enqueue overlapping requests.
- `di`: Dagger Hilt modules bind `CakeRepository` to `CakeRepositoryImpl`, provide singleton Retrofit/OkHttp/API instances, and provide a qualified IO dispatcher and use case. `CakeApplication` uses `@HiltAndroidApp`, `MainActivity` uses `@AndroidEntryPoint`, and `CakeViewModel` uses `@HiltViewModel` with constructor injection. The activity’s default Hilt ViewModel factory handles Compose `viewModel()`; no custom factory or application cast is needed. KSP generates the dependency graph. Domain code stays independent of Hilt through a use-case provider. Unit tests still construct their subjects directly with mocks and test dispatchers.

The API returns a small complete JSON collection in one response. The app loads it directly and uses `LazyColumn` to compose visible rows efficiently. No pagination library or paging source is needed.

Retaining state in the ViewModel avoids network reloads on activity rotation; lazy list scroll state is saveable, and the selected dialog remains in the ViewModel. Process death starts a fresh network load. Error text is user-facing and resource-backed, without raw exception details. Coil provides image loading/caching and a fallback for broken external image URLs.

## Verification

JUnit 5 + MockK isolate repository/use-case/ViewModel behavior. `CakeViewModelTest` creates a fresh ViewModel and mocked use case in `@BeforeEach`; `GetCakesUseCaseTests` and `CakeRepositoryTests` each construct their own subject with mocked dependencies. Coroutine test dispatchers provide deterministic scheduling; Turbine checks startup loading/success, failure/retry, refresh retention and StateFlow selection/dismissal. Local MockWebServer contract tests check JSON mapping, request path, HTTP failure mapping, malformed JSON, nullable/missing field defaults and unknown-field compatibility. Tests cover deduplication, sorting, empty data, request concurrency, replacing the collection, nullable fields and cancellation.

The `CakeRefreshTest` emulator test scrolls through the collection, refreshes into a smaller dataset, repeats refresh and opens the description dialog. Each list layout captures an immutable list for its count, keys and rows. Run it with `./gradlew :app:connectedDebugAndroidTest`.

Validated locally: all 19 unit/contract tests and both emulator tests pass; debug assembly, minified unsigned release assembly and Android lint complete successfully (lint has warnings about newer dependencies and target API 36).

Successful loads are converted to a persistent immutable list once in the ViewModel. The Compose compiler reports `stable cakes: ImmutableList<Cake>` and infers `Cake` as stable from its string properties; no Compose annotations are added to the domain. Domain failures are mapped to immutable presentation error values in the ViewModel; exception objects never enter Compose state. Compiler stability reports are generated under `app/build/compose-reports`.

Compose previews in `CakeScreen.kt` show light, dark, large-font and error states without fetching network data.
