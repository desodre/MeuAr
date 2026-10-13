This is a Kotlin Multiplatform project targeting Android, iOS.

## Tela inicial

O app pede acesso à localização enquanto está em uso, consulta
`https://monitorar-backend.mma.gov.br/v1/estacao/todas` e escolhe a estação
geograficamente mais próxima pelas coordenadas da estação e pela fórmula de
Haversine. Depois, consulta `/v1/estacao/por-ids?ids=<id>` para obter o detalhe
da estação selecionada. A tela segue o [design Meu Ar no Figma](https://www.figma.com/design/HUnkhWQY0z7h91a773T6dQ/MeuAr?node-id=0-1):
paleta clara e escura, mapa em tons neutros e cartões para os estados de consulta.
Enquanto a localização ou as estações carregam, aparece o mapa esquemático
do design. Quando há usuário e estação, o mapa geográfico mostra ruas reais,
os dois marcadores e a linha entre eles. É possível arrastar, ampliar por
gesto e centralizar novamente. A distância exibida é em linha reta, e o IQAr é uma medição
**na estação**.

O campo `semInfoMaisDeTresDias` sinaliza dados antigos. A data da medição é
mostrada sem converter fuso horário, pois o endpoint não fornece um offset.
Se o serviço ou a localização estiverem indisponíveis, a tela mostra o estado
correspondente. Permissão negada oferece acesso às configurações; falha de
conexão e ausência de medição oferecem “Tentar novamente”. Android e iOS
precisam de conexão com a internet.

Permissões: `INTERNET`, `ACCESS_COARSE_LOCATION` e `ACCESS_FINE_LOCATION` no
Android; `NSLocationWhenInUseUsageDescription` no iOS. O usuário pode conceder
localização aproximada no Android.
O mapa geográfico usa Leaflet 1.9.4 e tiles do OpenStreetMap, com atribuição
visível. Não precisa de chave de API. Precisa de acesso a `unpkg.com` ou
`cdn.jsdelivr.net` para carregar o Leaflet e a `tile.openstreetmap.org` para
mostrar as ruas; o mapa apresenta “Tentar novamente” se esses recursos
falharem. No Android, o HTML usa uma origem HTTPS no WebView, cache HTTP padrão
e um User-Agent identificando Meu Ar.

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
