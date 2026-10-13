# Meu Ar

Aplicativo para Android e iOS que encontra a estação de monitoramento da
qualidade do ar mais próxima da sua localização e apresenta a leitura disponível
nela.

A interface segue o [design do Meu Ar no Figma](https://www.figma.com/design/HUnkhWQY0z7h91a773T6dQ/MeuAr?node-id=0-1),
com temas claro e escuro, mapa em tons neutros e estados claros para carregamento
e falhas.

## Como funciona

1. O app solicita acesso à localização durante o uso.
2. Consulta as estações no MonitorAr e calcula a distância até cada uma pela
   **fórmula de Haversine**, usando as coordenadas da estação.
3. Busca os detalhes da estação mais próxima e mostra sua medição, classificação,
   poluente, horário e fonte, quando disponíveis.
4. Marca no mapa o usuário e a estação, com uma linha entre os pontos. A
   distância em linha reta aparece no cartão da estação.

| Endpoint do MonitorAr | Uso |
| --- | --- |
| [`/v1/estacao/todas`](https://monitorar-backend.mma.gov.br/v1/estacao/todas) | Descobrir as estações e suas coordenadas. |
| `/v1/estacao/por-ids?ids=<id>` | Consultar a estação selecionada. |

O mapa geográfico usa Leaflet e mapas do OpenStreetMap. Enquanto a localização
ou as estações carregam, a tela apresenta o mapa esquemático do design. Quando
os dados chegam, é possível mover, ampliar e recentralizar o mapa.

## Estados e limites dos dados

A tela distingue leitura recente, leitura com mais de três dias, estação sem
medição, falha de conexão e falta de permissão de localização. Os estados de
falha oferecem uma ação adequada, como **Tentar novamente** ou
**Abrir configurações**.

- A leitura de qualidade do ar pertence **à estação**, não ao endereço exato do usuário.
- A distância exibida é geográfica, em linha reta; não representa um trajeto.
- O campo `semInfoMaisDeTresDias` identifica dados antigos. O horário da medição
  é exibido sem conversão de fuso, pois a resposta da API não informa um
  deslocamento de horário.

## Executar o projeto

O projeto usa Kotlin Multiplatform e Compose Multiplatform. Para o Android, é
necessário um ambiente de desenvolvimento com o SDK Android. Para o iOS, use
macOS com Xcode. O app precisa de internet e de permissão de localização no
dispositivo.

### Android

Abra o projeto no Android Studio e execute a configuração do `androidApp`, ou
gere um APK de debug:

```bash
./gradlew :androidApp:assembleDebug
```

O [manifesto Android](androidApp/src/main/AndroidManifest.xml) já declara
`INTERNET`, `ACCESS_COARSE_LOCATION` e `ACCESS_FINE_LOCATION`. O usuário pode
conceder localização aproximada.

### iOS

Abra [`iosApp/iosApp.xcodeproj`](iosApp/iosApp.xcodeproj) no Xcode e execute o
app em um simulador ou dispositivo. A descrição da permissão de localização
está em [`Info.plist`](iosApp/iosApp/Info.plist).

### Testes

```bash
./gradlew :shared:testAndroidHostTest
```

No macOS, para executar os testes no simulador iOS:

```bash
./gradlew :shared:iosSimulatorArm64Test
```

## Estrutura

| Diretório | Responsabilidade |
| --- | --- |
| [`shared/src/commonMain`](shared/src/commonMain) | Interface compartilhada, modelos, seleção da estação e cálculo de distância. |
| [`shared/src/androidMain`](shared/src/androidMain) | API, localização e mapa no Android. |
| [`shared/src/iosMain`](shared/src/iosMain) | API, localização e mapa no iOS. |
| [`androidApp`](androidApp) e [`iosApp`](iosApp) | Entradas e configurações de cada plataforma. |
