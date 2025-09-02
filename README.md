# GhostlyPath 👻
*Your reliable guide back to your lost items.*

---

[EN](#-english-version) | [RU](#-ghostlypath)

---

## 🇷🇺 GhostlyPath

**GhostlyPath** — это легкий и минималистичный плагин для серверов Minecraft (Spigot/Paper и форки), который решает одну из самых неприятных проблем в выживании: потерю вещей после смерти.

Вместо того чтобы просто показывать координаты в чате, GhostlyPath создает на месте вашей смерти "призрака" (голову вашего персонажа), который хранит все ваши вещи в безопасности. Ваш компас автоматически начнет указывать на призрака, помогая вам быстро найти дорогу назад. Вещи больше не исчезнут через 5 минут!

### ✨ Особенности (Features)
- **Безопасное хранение вещей**: Ваш инвентарь сохраняется внутри призрака, а не выпадает на землю, что защищает его от исчезновения или кражи.
- **Визуальный маркер**: На месте смерти появляется ваша голова со свечением, которую легко заметить издалека.
- **Компас-навигатор**: После возрождения ваш компас автоматически указывает на место смерти.
- **Защита от перезагрузок:** Призраки и их вещи теперь сохраняются при перезапуске сервера или плагина. Вы больше никогда не потеряете вещи из-за выключения сервера.
- **Настраиваемое PvP-лутание:** Разрешите другим игрокам забирать ваши вещи для рискованного PvP-геймплея или оставьте эту опцию выключенной для классического PvE-выживания.
- **Полная настраиваемость**: Настройте время жизни призрака, дистанцию для сбора вещей и другие параметры в `config.yml`.
- **Минимальная нагрузка**: Плагин оптимизирован и активен только тогда, когда это необходимо, не создавая нагрузки на сервер.
- **Поддержка версий**: Работает на версиях от 1.19.4 до 1.21+.

### ⚙️ Как это работает?
1.  Вы умираете.
2.  Плагин отменяет стандартное выпадение вещей.
3.  На месте смерти появляется ваш "призрак" с вашими вещами внутри.
4.  Вы возрождаетесь, и ваш компас начинает указывать на призрака.
5.  Вы добираетесь до призрака. Как только вы подходите достаточно близко, он исчезает, а все ваши вещи возвращаются в инвентарь.

### 🛠️ Установка (Installation)
1.  Скачайте последнюю версию плагина со страницы [SpigotMC](https://www.spigotmc.org/resources/ghostlypath.128547/) или ...
2.  Поместите скачанный `.jar` файл в папку `plugins` вашего сервера.
3.  Перезапустите или перезагрузите сервер.
4.  Настройте файл `config.yml` в папке `/plugins/GhostlyPath/` по вашему желанию.

### 📋 Команды и права (Commands & Permissions)

| Команда              | Описание                          | Право (Permission)           | Алиасы      |
| -------------------- | --------------------------------- | ---------------------------- | ----------- |
| `/ghostlypath reload` | Перезагружает конфигурацию плагина. | `ghostlypath.admin.reload`   | `gp`, `gpath` |

### 🔧 Конфигурация (`config.yml`)
Вы можете полностью настроить плагин под себя.
```yaml
# Должен ли призрак хранить вещи игрока? Если false, будет только маркер.
save-items-in-ghost: true
# Время в секундах, через которое призрак и вещи исчезнут. 0 = вечно.
ghost-timeout-seconds: 1800 # 30 минут
# На каком расстоянии (в блоках) призрак исчезнет и вернет вещи.
remove-distance-blocks: 3
# Включить ли компас-указатель.
enable-compass-tracking: true
# Сообщения плагина (поддерживают цветовые коды)
messages:
  death-location-set: "&aВаши вещи в безопасности! Призрак ждет вас. Компас укажет путь."
  ghost-removed-nearby: "&eВы нашли свои вещи! Они были возвращены в ваш инвентарь."
  # ... и другие сообщения
```

### ❓ Проблемы и предложения
Нашли баг или есть идея? Создайте [Issue](https://github.com/lrdcxdes/GhostlyPath/issues) на нашей странице GitHub.

---
---

## 🇬🇧 English Version

**GhostlyPath** is a lightweight, minimalistic plugin for Minecraft servers (Spigot/Paper and forks) that solves one of the most frustrating problems in survival mode: losing your items upon death.

Instead of just printing coordinates in chat, GhostlyPath creates a "ghost" (your player head) at your death location, which safely stores all your items. Your compass will automatically point to the ghost, helping you find your way back quickly. No more 5-minute despawn timers for your precious gear!

### ✨ Features
- **Safe Item Storage**: Your inventory is stored inside the ghost instead of dropping on the ground, protecting it from despawning or being stolen.
- **Visual Marker**: A glowing player head appears at your death location, making it easy to spot from a distance.
- **Compass Navigation**: Upon respawning, your compass automatically points to your death location.
- **Server Restart Proof:** Ghosts and their items will persist through server restarts and reloads. You will never lose your items due to a server shutdown.
- **Configurable PvP Looting:** Enable looting by other players for a high-risk PvP experience, or keep it disabled for a classic, safe PvE feeling.
- **Fully Configurable**: Customize the ghost's lifespan, pickup distance, and other settings in `config.yml`.
- **Lightweight & Optimized**: The plugin is designed to be efficient and is only active when needed, ensuring zero server lag.
- **Version Support**: Works on versions 1.19.4 through 1.21+.

### ⚙️ How It Works
1.  You die.
2.  The plugin cancels the standard item drop.
3.  A "ghost" containing your items appears at your death location.
4.  You respawn, and your compass now points to the ghost.
5.  You travel back to the ghost. Once you get close enough, it vanishes, and all your items are returned to your inventory.

### 🛠️ Installation
1.  Download the latest version from the [SpigotMC page](https://www.spigotmc.org/resources/ghostlypath.128547/) or ...
2.  Place the downloaded `.jar` file into your server's `plugins` folder.
3.  Restart or reload your server.
4.  (Optional) Configure the `config.yml` file located in `/plugins/GhostlyPath/` to your liking.

### 📋 Commands & Permissions

| Command              | Description                     | Permission                   | Aliases     |
| -------------------- | ------------------------------- | ---------------------------- | ----------- |
| `/ghostlypath reload` | Reloads the plugin's configuration. | `ghostlypath.admin.reload`   | `gp`, `gpath` |

### 🔧 Configuration (`config.yml`)
You have full control over how the plugin behaves.
```yaml
# Should the ghost store items? If false, it will only be a marker.
save-items-in-ghost: true
# Time in seconds before the ghost and its items disappear. 0 = forever.
ghost-timeout-seconds: 1800 # 30 minutes
# Distance in blocks to trigger ghost removal and item return.
remove-distance-blocks: 3
# Enable the compass tracking feature.
enable-compass-tracking: true
# Plugin messages (supports color codes)
messages:
  death-location-set: "&aYour items are safe! A ghost is waiting for you. Your compass will guide you."
  ghost-removed-nearby: "&eYou found your items! They have been returned to your inventory."
  # ... and other messages
```

### ❓ Issues & Suggestions
Found a bug or have an idea? Please create an [Issue](https://github.com/lrdcxdes/GhostlyPath/issues) on our GitHub page.
