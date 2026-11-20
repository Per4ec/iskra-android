# ИСКРА — Android (Java, без WebView)

Нативный порт браузерной арена-игры «ИСКРА — выживи в рое» на Android.
Вся игра (меню, бой, прокачка, магазин, оружейная, настройки, звук) реализована
на Java с отрисовкой через `Canvas` — WebView и HTML не используются.

## Структура

```
app/src/main/java/com/iskra/game/
├── MainActivity.java   — полноэкранный ландшафтный режим, immersive
├── GameView.java       — игровой цикл, симуляция, рендер, ввод, экраны
├── Upgrade.java        — усиления уровня (45 шт.)
├── ShopItems.java      — магазин за звёзды (14 улучшений)
├── Weapons.java        — оружейная: 16 видов оружия, 17 эффектов
└── Sfx.java            — синтезатор звука (AudioTrack)
```

## Как собрать APK

### Вариант 1. Android Studio (проще всего)

1. Установите [Android Studio](https://developer.android.com/studio) (SDK Platform 35 ставится из неё).
2. `File → Open…` → выберите папку проекта.
3. Дождитесь окончания Gradle Sync (снизу справа).
4. `Build → Build APK(s)`. Готовый файл:
   `app/build/outputs/apk/debug/app-debug.apk` — его можно сразу скопировать на телефон и установить.

### Вариант 2. Командная строка

Нужны JDK 17 и Android SDK (путь к нему — в переменной `ANDROID_HOME`).

```bash
gradle wrapper          # один раз: создаст gradlew
./gradlew assembleDebug # соберёт debug APK
# app/build/outputs/apk/debug/app-debug.apk
```

Релизная сборка с подписью:

```bash
# создать ключ (один раз)
keytool -genkey -v -keystore iskra.keystore -alias iskra -keyalg RSA -keysize 2048 -validity 10000
./gradlew assembleRelease -Pandroid.injected.signing.store.file=iskra.keystore \
  -Pandroid.injected.signing.store.password=ПАРОЛЬ \
  -Pandroid.injected.signing.key.alias=iskra \
  -Pandroid.injected.signing.key.password=ПАРОЛЬ
```

### Вариант 3. GitHub Actions (ничего не устанавливая)

В репозитории откройте вкладку **Actions → Build APK → Run workflow**.
Через несколько минут в артефактах запуска появится `iskra-apk` с готовым файлом.

## Управление

- **Движение** — веди пальцем по экрану (виртуальный джойстик появляется там, где коснулся).
- **Стрельба** — автоматически по ближайшему врагу.
- **Рывок** — круглая кнопка справа внизу (неуязвимость на миг).
- **Пауза** — кнопка ⏸ справа вверху (игра также паузится при сворачивании).
- Уровни дают выбор из трёх усилений; убийства — валюта оружейной; звёзды — валюта магазина.
- Забег автоматически сохраняется — в меню появится «Продолжить забег».
