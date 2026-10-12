This is a Kotlin Multiplatform project targeting Android, iOS.

## Tela inicial

O app pede acesso à localização enquanto está em uso, consulta
`https://monitorar-backend.mma.gov.br/v1/estacao/todas` e escolhe a estação
geograficamente mais próxima pelas coordenadas da estação e pela fórmula de
Haversine. Depois, consulta `/v1/estacao/por-ids?ids=<id>` para obter o detalhe
da estação selecionada. O mapa incorporado do OpenStreetMap marca a estação;
a distância exibida é em linha reta, e o IQAr é uma medição **na estação**.

O campo `semInfoMaisDeTresDias` sinaliza dados antigos. A data da medição é
mostrada sem converter fuso horário, pois o endpoint não fornece um offset.
Se o serviço ou a localização estiverem indisponíveis, a tela mostra o estado
correspondente. Android e iOS precisam de conexão com a internet.

Permissões: `INTERNET`, `ACCESS_COARSE_LOCATION` e `ACCESS_FINE_LOCATION` no
Android; `NSLocationWhenInUseUsageDescription` no iOS. O usuário pode conceder
localização aproximada no Android. O mapa usa o serviço de incorporação do
OpenStreetMap e depende da disponibilidade desse serviço.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
