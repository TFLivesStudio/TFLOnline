# TFLOnline

**Your proxy already knows where every player is. Your placeholders should too.**

TFLOnline brings network-wide player statistics directly to PlaceholderAPI, with updates triggered by player activity instead of relying only on periodic requests from each backend.

One central configuration. Built-in server groups and occupancy bars. No Redis, database, or additional ports required.

**[Download](https://github.com/TFLivesStudio/TFLOnline/releases) · [Configuration](docs/CONFIGURATION.md) · [Roadmap](docs/ROADMAP.md) · [Source code](https://github.com/TFLivesStudio/TFLOnline)**

English | [Español](#español)

---

# English

## Why TFLOnline?

You can already display network player counts using [Bungee Expansion](https://ecloud.placeholderapi.com/expansions/bungee/). So why install something else?

**Because getting a player count and keeping an entire network synchronized are two different things.**

Bungee Expansion requests player counts at configurable intervals, every 30 seconds by default. It works well for basic placeholders, but each backend is responsible for requesting and caching that information.

TFLOnline approaches this from the proxy.

When players join, leave, or switch servers, the proxy can notify connected backends about the change. Periodic synchronization remains available as a fallback, while every backend reads the latest received data from its local cache.

The result is a more flexible system for server selectors, menus, scoreboards, and holograms.

### What's different?

| Feature | Bungee Expansion | TFLOnline |
|---|---|---|
| Player counts | Periodic requests | Event-based updates + periodic sync |
| Network total | Sum of cached server counts | Directly from the proxy |
| Custom server groups | Not built in | Built in |
| Occupancy bars | Not built in | Configurable |
| Centralized configuration | No | Yes, on the proxy |
| Data freshness | No explicit sync status | Sync status and cache expiration |
| Additional ports | Not required | Not required |

Bungee Expansion is still a good choice if all you need is `%bungee_total%` or `%bungee_<servername>%`.

**TFLOnline makes more sense when you want your network statistics managed together, with greater control over their updates and presentation.**

It isn't a promise of instantaneous updates on every scoreboard or menu. Display plugins have their own refresh intervals, and the default transport still depends on Minecraft's existing player connections.

## Features

### Network-wide player counts

Get the total number of connected players directly from the proxy, or check individual backends without configuring separate counters manually.

### Event-driven synchronization

Player connections, disconnections, and server transfers can trigger updates. A configurable periodic broadcast helps keep backend caches current.

### Automatic server discovery

TFLOnline reads the servers registered in your proxy and adds them to its configuration.

When you register a new backend, its entry is generated automatically. Existing aliases and settings are preserved.

### Custom server groups

Combine multiple backends into a single counter.

For example, a `survival` group can include Skyblock, Lifesteal, and other game modes without needing separate placeholder calculations.

### Configurable occupancy bars

Display a server's occupancy using a simple text progress bar.

Choose its length, filled character, empty character, and the capacity of each backend.

### Centralized configuration

The proxy holds the configuration for the network.

Settings, server aliases, groups, synchronization intervals, and occupancy bars are managed through one documented `config.yml`.

### Synchronization diagnostics

See whether the backend has received valid information and how recently it was updated.

Missing or expired data returns `N/A` rather than silently displaying an unreliable count.

## Compatibility

TFLOnline is distributed as three separate plugins.

| Component | Platforms |
|---|---|
| **TFLOnline Velocity** | Velocity and API-compatible forks |
| **TFLOnline Bungee** | BungeeCord and Waterfall |
| **TFLOnline Paper** | Paper, Purpur, and compatible forks, including Folia |

The project targets **Minecraft 1.20+** and Java 17-compatible plugin bytecode. Your Minecraft server version may require a newer Java runtime.

PlaceholderAPI is required on backend servers where TFLOnline placeholders will be used.

Compatibility testing across versions and forks is ongoing.

## Getting started

### 1. Download

Get the latest development build from [GitHub Releases](https://github.com/TFLivesStudio/TFLOnline/releases).

Download the appropriate files:

- `TFLOnline-Velocity-*.jar` for Velocity-based proxies.
- `TFLOnline-Bungee-*.jar` for BungeeCord-based proxies.
- `TFLOnline-Paper-*.jar` for your backend servers.

You only need **one proxy component**. Do not install both proxy variants together.

### 2. Install

Place the corresponding JAR in your proxy's `plugins/` directory.

Then install TFLOnline Paper on every backend where you want to display its placeholders.

```text
Proxy/
└── plugins/
    └── TFLOnline-Velocity.jar

Lobby/
└── plugins/
    ├── PlaceholderAPI.jar
    └── TFLOnline-Paper.jar

Skyblock/
└── plugins/
    ├── PlaceholderAPI.jar
    └── TFLOnline-Paper.jar
```

The Paper component registers its PlaceholderAPI expansion internally.

**Do not place the Paper JAR inside `PlaceholderAPI/expansions/`.**

### 3. Start your network

Start or restart your proxy and backend servers.

TFLOnline will generate its configuration and discover the servers registered in your proxy.

No manual registration is required for a standard setup.

### 4. Customize

Open the generated `config.yml` in your proxy's TFLOnline plugin directory.

Adjust the settings you need, then run:

```text
/tflonline reload
```

Once synchronization is established, your placeholders are ready to use.

## Placeholders

TFLOnline placeholders work with PlaceholderAPI-compatible plugins such as TAB, DeluxeMenus, FancyHolograms, and scoreboards.

| Placeholder | Description |
|---|---|
| `%tflonline_total%` | Total players on the proxy |
| `%tflonline_server:skyblock:online%` | Players connected to Skyblock |
| `%tflonline_group:survival:online%` | Players across a configured server group |
| `%tflonline_server:skyblock:capacity%` | Configured server capacity |
| `%tflonline_server:skyblock:state%` | Configured administrative state |
| `%tflonline_bar:skyblock%` | Occupancy progress bar |
| `%tflonline_sync_status%` | Synchronization status |
| `%tflonline_sync_age%` | Seconds since the last valid update |

Replace `skyblock` and `survival` with your server or group names.

**Important details:**

- Server groups refer to collections of backends, not LuckPerms ranks.
- Occupancy bars require a configured capacity greater than zero.
- Server states currently represent administrative settings, not automatic reachability checks.
- Unknown or expired information returns `N/A`.
- Synchronization status can be `WAITING`, `CONNECTED`, or `STALE`.

## Configuration

TFLOnline uses one `config.yml` on the proxy, with explanations and examples throughout the file.

Here is a simplified example:

```yaml
synchronization:
  transport: plugin-messaging
  broadcast-interval-seconds: 3
  cache-timeout-seconds: 15
  broadcast-on-events: true
  event-debounce-millis: 150

bars:
  default:
    segments: 20
    filled: "▌"
    empty: "░"

groups:
  survival:
    - skyblock
    - lifesteal

servers:
  skyblock:
    alias: skyblock
    capacity: 100
    state: AUTO

  lifesteal:
    alias: lifesteal
    capacity: 75
    state: AUTO
```

You don't need to write every server entry yourself. TFLOnline discovers registered backends and preserves your existing configuration.

For all available options, see the **[configuration guide](docs/CONFIGURATION.md)**.

## Commands

Administrative commands are available directly on the proxy.

| Command | Description |
|---|---|
| `/tflonline status` | View the network summary |
| `/tflonline servers` | List registered servers and player counts |
| `/tflonline groups` | View the number of configured server groups |
| `/tflonline sync` | Request synchronization |
| `/tflonline reload` | Reload the configuration |
| `/tflonline debug` | Check the current debug setting |

**Permission:** `tflonline.admin`

Administrative messages support English and Spanish through the `general.language` setting.

## How does it work?

TFLOnline uses Minecraft's existing **Plugin Messaging** system.

The proxy collects network information and sends snapshots to connected backends. Each backend stores the latest valid snapshot locally, allowing PlaceholderAPI to retrieve values without performing a network request for every placeholder.

Event-driven updates are complemented by periodic synchronization.

This approach requires no additional listener ports or external services.

**There is one limitation:** Plugin Messaging relies on player connections. An empty backend cannot receive new data until a player connects. TFLOnline requests fresh information when a player joins.

## Development status

**Current version: `0.2.0-SNAPSHOT`**

TFLOnline is actively being developed. Current builds include the core synchronization system, network and backend counts, server groups, occupancy bars, automatic discovery, and proxy administration tools.

The following features are planned:

- Global and per-server LuckPerms rank counters.
- Separate Java and Bedrock player counts.
- Automatic backend availability and latency checks.
- Optional Redis support for multi-proxy networks.
- Extended formatting and localization.

Development builds pass automated compilation and unit tests. Live compatibility testing across different server platforms is still in progress.

Follow development in the **[roadmap](docs/ROADMAP.md)**.

## Building from source

TFLOnline uses Gradle and Java 17 for its build process.

```bash
gradle clean build
```

The compiled JARs are generated in the corresponding module's `build/libs/` directory.

Automated development builds are also available through [GitHub Actions](https://github.com/TFLivesStudio/TFLOnline/actions).

## License

TFLOnline is developed by **tflives** and distributed under the **[GNU General Public License v3.0](LICENSE)**.

---

# Español

## ¿Por qué TFLOnline?

Ya puedes mostrar los jugadores de tu Network utilizando [Bungee Expansion](https://ecloud.placeholderapi.com/expansions/bungee/). Entonces, ¿qué sentido tiene instalar otra alternativa?

**Porque consultar cuántos jugadores hay conectados y mantener sincronizada la información de toda una Network son dos cosas diferentes.**

Bungee Expansion consulta los contadores del proxy mediante intervalos configurables, cada 30 segundos por defecto. Funciona bien para placeholders básicos, pero cada backend se encarga de solicitar y almacenar esos datos.

TFLOnline propone un funcionamiento diferente.

El proxy puede enviar actualizaciones cuando un jugador entra, sale o cambia de servidor. Las sincronizaciones periódicas siguen funcionando como respaldo, mientras cada backend consulta los últimos datos recibidos desde su propia caché.

Esto permite administrar de forma más completa los contadores utilizados en selectores de modalidades, menús, scoreboards y hologramas.

### ¿Qué cambia realmente?

| Característica | Bungee Expansion | TFLOnline |
|---|---|---|
| Contadores | Consultas periódicas | Eventos + sincronización periódica |
| Total de la Network | Suma de contadores almacenados | Directamente desde el proxy |
| Grupos personalizados | No incluidos | Integrados |
| Barras de ocupación | No incluidas | Configurables |
| Configuración centralizada | No | Sí, desde el proxy |
| Datos desactualizados | Sin estado explícito de sincronización | Diagnóstico y caducidad de caché |
| Puertos adicionales | No necesarios | No necesarios |

Bungee Expansion sigue siendo una buena elección si únicamente necesitas `%bungee_total%` o `%bungee_<servername>%`.

**TFLOnline resulta más interesante cuando buscas administrar las estadísticas de toda tu Network desde un mismo lugar, con mayor control sobre su actualización y presentación.**

Esto no significa que todos los menús o scoreboards cambien instantáneamente. Cada plugin visual tiene sus propios intervalos de actualización y el transporte predeterminado sigue dependiendo de las conexiones de Minecraft.

## Características

### Contadores de toda la Network

Consulta los jugadores conectados directamente desde el proxy o muestra la cantidad de usuarios de un backend específico.

No necesitas crear manualmente un sistema de consultas para cada modalidad.

### Sincronización por eventos

Las conexiones, desconexiones y cambios de servidor pueden generar actualizaciones.

Además, TFLOnline realiza sincronizaciones periódicas configurables para mantener actualizada la información.

### Detección automática de servidores

TFLOnline identifica los servidores registrados en el proxy y los añade a su configuración.

Cuando agregas una modalidad nueva, su entrada se genera automáticamente sin sobrescribir los alias ni los ajustes existentes.

### Grupos personalizados

Combina varias modalidades en un único contador.

Por ejemplo, puedes crear un grupo llamado `survival` que incluya Skyblock, Lifesteal y otras modalidades, sin necesidad de sumar sus placeholders manualmente.

### Barras de ocupación

Representa la cantidad de jugadores conectados mediante una barra de progreso de texto.

Puedes personalizar su longitud, los caracteres utilizados y la capacidad de cada servidor.

### Configuración centralizada

Toda la configuración se administra desde el proxy.

Alias, grupos, intervalos de sincronización, capacidades y barras de ocupación se gestionan desde un único `config.yml` documentado.

### Diagnóstico de sincronización

Comprueba si un backend está recibiendo información válida y cuánto tiempo ha pasado desde la última actualización.

Si los datos faltan o han caducado, TFLOnline devuelve `N/A` en lugar de mostrar un contador poco fiable.

## Compatibilidad

TFLOnline se distribuye mediante tres plugins independientes.

| Componente | Plataformas |
|---|---|
| **TFLOnline Velocity** | Velocity y forks compatibles con su API |
| **TFLOnline Bungee** | BungeeCord y Waterfall |
| **TFLOnline Paper** | Paper, Purpur y forks compatibles, incluido Folia |

El proyecto está orientado a **Minecraft 1.20+** y genera código compatible con Java 17. Tu versión de Minecraft podría necesitar una versión de Java más reciente.

PlaceholderAPI es necesario en los servidores donde quieras utilizar los placeholders de TFLOnline.

La compatibilidad con distintas versiones y forks continúa en fase de pruebas.

## Instalación

### 1. Descarga los plugins

Accede a [GitHub Releases](https://github.com/TFLivesStudio/TFLOnline/releases) y descarga los archivos correspondientes:

- `TFLOnline-Velocity-*.jar` para proxies basados en Velocity.
- `TFLOnline-Bungee-*.jar` para proxies basados en BungeeCord.
- `TFLOnline-Paper-*.jar` para tus servidores backend.

Solo necesitas **uno de los dos componentes de proxy**. No instales ambos en el mismo proxy.

### 2. Instala los archivos

Coloca el JAR correspondiente dentro de la carpeta `plugins/` de tu proxy.

Después, instala TFLOnline Paper en cada backend donde quieras mostrar los placeholders.

```text
Proxy/
└── plugins/
    └── TFLOnline-Velocity.jar

Lobby/
└── plugins/
    ├── PlaceholderAPI.jar
    └── TFLOnline-Paper.jar

Skyblock/
└── plugins/
    ├── PlaceholderAPI.jar
    └── TFLOnline-Paper.jar
```

El componente de Paper registra automáticamente su integración con PlaceholderAPI.

**No debes colocarlo dentro de `PlaceholderAPI/expansions/`.**

### 3. Inicia tu Network

Inicia o reinicia el proxy y los servidores backend.

TFLOnline generará su configuración y detectará automáticamente los servidores registrados en el proxy.

Para una instalación normal, no necesitas registrarlos uno por uno.

### 4. Personaliza los ajustes

Abre el archivo `config.yml` generado dentro del directorio de TFLOnline en el proxy.

Modifica lo que necesites y aplica los cambios mediante:

```text
/tflonline reload
```

Una vez establecida la sincronización, podrás utilizar los placeholders.

## Placeholders

Los placeholders de TFLOnline funcionan con plugins compatibles con PlaceholderAPI, como TAB, DeluxeMenus, FancyHolograms y otros sistemas de scoreboards.

| Placeholder | Descripción |
|---|---|
| `%tflonline_total%` | Jugadores conectados al proxy |
| `%tflonline_server:skyblock:online%` | Jugadores conectados a Skyblock |
| `%tflonline_group:survival:online%` | Jugadores de un grupo de servidores |
| `%tflonline_server:skyblock:capacity%` | Capacidad configurada del servidor |
| `%tflonline_server:skyblock:state%` | Estado administrativo configurado |
| `%tflonline_bar:skyblock%` | Barra de ocupación |
| `%tflonline_sync_status%` | Estado de sincronización |
| `%tflonline_sync_age%` | Segundos desde la última actualización válida |

Sustituye `skyblock` o `survival` por el nombre del servidor o grupo correspondiente.

**Información importante:**

- Los grupos de servidores representan conjuntos de backends, no rangos de LuckPerms.
- Las barras necesitan una capacidad configurada superior a cero.
- Los estados actuales representan valores administrativos, no comprobaciones automáticas de disponibilidad.
- Los datos inexistentes o caducados devuelven `N/A`.
- El estado de sincronización puede ser `WAITING`, `CONNECTED` o `STALE`.

## Configuración

TFLOnline utiliza un único archivo `config.yml` ubicado en el proxy.

Este archivo incluye explicaciones, ejemplos y valores recomendados para facilitar su personalización.

Ejemplo simplificado:

```yaml
synchronization:
  transport: plugin-messaging
  broadcast-interval-seconds: 3
  cache-timeout-seconds: 15
  broadcast-on-events: true
  event-debounce-millis: 150

bars:
  default:
    segments: 20
    filled: "▌"
    empty: "░"

groups:
  survival:
    - skyblock
    - lifesteal

servers:
  skyblock:
    alias: skyblock
    capacity: 100
    state: AUTO

  lifesteal:
    alias: lifesteal
    capacity: 75
    state: AUTO
```

No tienes que escribir manualmente cada servidor. TFLOnline detecta los backends registrados y conserva las configuraciones que ya hayas personalizado.

Puedes consultar todas las opciones en la **[guía de configuración](docs/CONFIGURATION.md)**.

## Comandos

Los comandos administrativos se ejecutan directamente desde el proxy.

| Comando | Función |
|---|---|
| `/tflonline status` | Consultar el resumen de la Network |
| `/tflonline servers` | Mostrar servidores y jugadores |
| `/tflonline groups` | Consultar la cantidad de grupos configurados |
| `/tflonline sync` | Solicitar una sincronización |
| `/tflonline reload` | Recargar la configuración |
| `/tflonline debug` | Consultar el ajuste de depuración |

**Permiso:** `tflonline.admin`

Los mensajes administrativos están disponibles en español e inglés mediante `general.language`.

## ¿Cómo funciona?

TFLOnline utiliza el sistema **Plugin Messaging** de Minecraft.

El proxy obtiene la información de la Network y envía snapshots a los backends conectados. Cada backend almacena la última información válida en memoria, permitiendo que PlaceholderAPI consulte los valores sin realizar una petición de red por cada placeholder.

Las actualizaciones por eventos se complementan con sincronizaciones periódicas.

Este funcionamiento no necesita abrir nuevos puertos ni instalar servicios externos.

**Existe una limitación:** Plugin Messaging depende de las conexiones de los jugadores. Un backend vacío no puede recibir información nueva hasta que alguien se conecte. TFLOnline solicita datos actualizados cuando entra un jugador.

## Estado del desarrollo

**Versión actual: `0.2.0-SNAPSHOT`**

TFLOnline continúa en desarrollo. Las compilaciones actuales incluyen el sistema principal de sincronización, contadores globales y por backend, grupos de servidores, barras de ocupación, detección automática y herramientas administrativas.

Las siguientes funcionalidades están previstas:

- Contadores por rangos de LuckPerms, globales y por servidor.
- Conteos separados para jugadores Java y Bedrock.
- Comprobación automática de disponibilidad y latencia.
- Redis opcional para Networks con múltiples proxies.
- Más opciones de formato y personalización.

Las compilaciones superan pruebas automatizadas, pero las pruebas de compatibilidad en instalaciones reales y distintas plataformas todavía están en desarrollo.

Puedes seguir las próximas mejoras en el **[roadmap](docs/ROADMAP.md)**.

## Compilación desde el código fuente

TFLOnline utiliza Gradle y Java 17 para su compilación.

```bash
gradle clean build
```

Los JAR se generan dentro de la carpeta `build/libs/` de cada módulo.

También puedes consultar las compilaciones automáticas en [GitHub Actions](https://github.com/TFLivesStudio/TFLOnline/actions).

## Licencia

TFLOnline es un proyecto desarrollado por **tflives** y distribuido bajo la **[GNU General Public License v3.0](LICENSE)**.

---

**TFLOnline** · Developed by [tflives](https://github.com/TFLivesStudio) · GPL-3.0
