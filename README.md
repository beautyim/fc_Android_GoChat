# demoproject

Independent Android skeleton (not BerryCam).

- Package root: `com.example.demoproject`
- App config lives under `config/` (externalized placeholders + gitignored local overlays)

## Modules

| Module | Role |
|--------|------|
| `:platform:network` | Full HTTP stack (OkHttp/Retrofit, sign+encrypt interceptors, SafeApiCall, ApiResponse, paging, serializers) |
| `:platform:data` | Retrofit API interfaces + DTOs, ApiServiceModule, SessionManager / AuthToken / device / locale bindings |
| `:platform:common` | Shared utilities (e.g. AppLogger) |
| `:platform:analytics` | Adjust config slot |
| `:ui:*` | Design tokens / theme |
| `:product:feature-home` | Sample UDF feature that injects `AppApi` and can ping `/app/stat` |
| `:app` | Thin shell + Hilt (`@HiltAndroidApp`) |

## Network / API

Ported from BerryCam’s `:core:network` + API/DTO surface of `:core:data` (packages renamed to `com.example.demoproject.platform.*`).

- All Retrofit APIs are provided by Hilt via `ApiServiceModule` (`AppApi`, `AuthApi`, `CallApi`, `CoinApi`, `FeedApi`, `FirebaseApi`, `GooglePayApi`, `MatchApi`, `MessageApi`, `NotificationApi`, `PostApi`, `ProfileApi`, `ReportApi`, `TranslationApi`, `VipApi`).
- Session token: in-memory + DataStore (`SessionManager` implements `AuthTokenProvider`) — **no Tink / SQLCipher**.
- Secrets stay in `config/network.local.properties` (gitignored). Tracked `network.properties` keeps placeholders.

Not ported: Room, MQTT, Agora, billing flows, full repositories, production UI screens.

## Config

```text
config/
  app.properties          # APPLICATION_ID
  network.properties      # BASE_URL, CHANNEL_NAME, keys…
  adjust.properties
  agora.properties
  *.local.properties      # gitignored secrets
config/common/release-log-stripping.pro
```

Gradle loads `*.properties` then overlays `*.local.properties` via `buildSrc` `BrandConfig`.

## Build

```bash
./gradlew :app:assembleDebug
```

## Domain progress (toward UI-only)

Done:
- AuthRepository / AppSessionRepository / ProfileRepository
- MessageRepository (network + in-memory chat store; Room later)
- FeedRepository (real `/feeds/list`)
- MatchRepository (+ MatchStartPayloadParser / recharge page types)
- CoinRepository (`/coin/index` → RechargePageData)
- CallRepository (call records) + CallSessionRepository (create/heart/end/in-call msg)
- VipRepository (VIP page catalog)
- NotificationRepository (`/msg/notice`)
- Domain: User/Session/AlbumPhoto/Message/Post/Gift/CallRoom/CallRecord/Notification…
- Home demo: + Call Records / VIP / Notices

Still missing for UI-only product:
- Billing full flow; Post create/like; Room+SQLCipher; MQTT; RTC/CallKit; S3; FCM
- Feature ViewModels + navigation contract for each flow

## MQTT (toward UI-only)

- `:platform:mqtt` — Paho connection + prefs + message bus (`MqttRuntime`)
- Starts in `DemoApplication`; Home **Init MQTT** saves socket from `/app/init`
- Not included yet: CallKit signaling client, MQTT action log upload

## Local DB / Post

- Room + SQLCipher (`DemoDatabase` v1) via `LocalDatabaseFactory` + Tink-wrapped passphrase
- `RoomUserCache` backs `ProfileRepository` (replaces in-memory user cache)
- `PostRepositoryImpl` talks to `/moment/*` + `/user-operate/like` (comments still stub)
- Home: **Probe Room DB** / **Post Categories**

## Chat local cache

- `RoomChatStore` backs Message + CallSession (shared instance)
- Conversations / messages persist in SQLCipher; `lastSyncMtime` still process-local

## Report / Block

- `ReportRepository` — `/report/index` + `/report/handle` (photos as remote URLs; S3 later)
- `BlockRepository` + `BlockedUsersStore` — `/user/black-list` + `/user/black`
- Inbox observe/list filters blocked peers
- Home: **Black List** / **Report Init**
