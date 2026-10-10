# TFLOnline

**Network-wide player counts for Minecraft, directly in PlaceholderAPI.**

Keep player counts synchronized across your Minecraft network without installing Redis, setting up a database, or opening additional ports.

Built for Velocity, BungeeCord, and compatible server platforms.

[Downloads](https://github.com/TFLivesStudio/TFLOnline/releases) · [Configuration](docs/CONFIGURATION.md) · [Roadmap](docs/ROADMAP.md) · [License](LICENSE)

**English** | [Español](#español)

---

## English

### Overview

TFLOnline connects your Minecraft proxy to PlaceholderAPI, making network player counts available on your backend servers.

Display the number of online players in your lobby, show how many people are playing Skyblock, or combine multiple servers into a single counter.

Everything is managed from the proxy. TFLOnline automatically detects registered servers and keeps their information synchronized.

### Features

- **Network-wide counters.** Check how many players are connected to the proxy or a specific backend.
- **Automatic server discovery.** New servers are added to the configuration without overwriting existing settings.
- **Server groups.** Combine several backends into one counter.
- **Occupancy bars.** Display player counts as customizable progress bars.
- **Event-based synchronization.** Update counts when players connect, disconnect, or switch servers.
- **Built-in PlaceholderAPI support.** The Paper plugin registers its placeholders automatically.
- **Centralized configuration.** Manage everything from a single, documented `config.yml` on the proxy.
- **Lightweight installation.** No Redis, database, or additional port required.

### Compatibility

TFLOnline provides three separate plugins:

| Component | Platforms |
|---|---|
| TFLOnline Velocity | Velocity and API-compatible forks |
| TFLOnline Bungee | BungeeCord and Waterfall |
| TFLOnline Paper | Paper, Purpur, and compatible forks, including Folia |

**Requirements**

- PlaceholderAPI on backend servers where placeholders will be used.
- Java 17-compatible plugin runtime. Your Minecraft server version may require a newer Java version.
- A proxy connecting the backend servers.

The project targets Minecraft 1.20 and newer. Compatibility across individual versions and forks is still being tested.

### Installation

**1. Download the plugins**

Visit the [Releases page](https://github.com/TFLivesStudio/TFLOnline/releases) and download:

- `TFLOnline-Velocity-*.jar` or `TFLOnline-Bungee-*.jar`, depending on your proxy.
- `TFLOnline-Paper-*.jar` for your backend servers.

**2. Install the files**

Place the corresponding proxy JAR in its `plugins/` folder.

Then install the Paper JAR on every backend where you want to use TFLOnline placeholders.

The Paper component is a regular plugin. **Do not place it inside `PlaceholderAPI/expansions/`.**

**3. Start your servers**

Start or restart your proxy and backends.

TFLOnline will generate its configuration and automatically discover the servers registered in your proxy.

**4. Customize your setup**

Open the proxy's `config.yml`, adjust your settings, and apply them with:

```text
/tflonline reload
```

That's it. Your placeholders are ready to use once synchronization has been established.

### Available placeholders

Use these placeholders with TAB, DeluxeMenus, FancyHolograms, scoreboards, and other compatible plugins.

| Placeholder | Description |
|---|---|
| `%tflonline_total%` | Total players connected to the proxy |
| `%tflonline_server:skyblock:online%` | Players connected to Skyblock |
| `%tflonline_group:survival:online%` | Combined players across a configured server group |
| `%tflonline_server:skyblock:capacity%` | Configured server capacity |
| `%tflonline_server:skyblock:state%` | Configured administrative server state |
| `%tflonline_bar:skyblock%` | Server occupancy bar |
| `%tflonline_sync_status%` | Synchronization status |
| `%tflonline_sync_age%` | Seconds since the last synchronization |

Replace `skyblock` or `survival` with the corresponding server or group name.

**A few things to know:**

- Server groups are collections of backends, not LuckPerms permission groups.
- Occupancy bars require a configured capacity greater than zero.
- Server states currently reflect configured administrative values, not live reachability checks.
- Missing or outdated data returns `N/A` instead of an incorrect player count.

### Configuration

TFLOnline keeps its settings in a single `config.yml` on the proxy.

The file includes explanations, examples, and recommended values for each section.

Here is a small example:

```yaml
synchronization:
  transport: plugin-messaging
  broadcast-interval-seconds: 3
  cache-timeout-seconds: 15
  broadcast-on-events: true
  event-debounce-millis: 150

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

You don't need to add every server manually. TFLOnline detects registered backends and preserves your existing configuration.

For every available option, see the [configuration guide](docs/CONFIGURATION.md).

### Commands

All administrative commands use `/tflonline`.

| Command | Description |
|---|---|
| `/tflonline status` | View the current network summary |
| `/tflonline servers` | List registered servers and player counts |
| `/tflonline groups` | View the configured server group count |
| `/tflonline sync` | Request a new synchronization |
| `/tflonline reload` | Reload the proxy configuration |
| `/tflonline debug` | Check the current debug setting |

**Permission:** `tflonline.admin`

Administrative messages support English and Spanish through the `general.language` setting.

### How synchronization works

TFLOnline uses Minecraft's existing Plugin Messaging system to communicate between your proxy and backends.

The proxy collects player counts and shares them with connected servers. Each backend keeps a local cache, allowing PlaceholderAPI to retrieve values without requesting information over the network every time.

Changes are sent through events, with periodic synchronization as a fallback.

**One limitation:** Plugin Messaging requires a player connection. Empty backends cannot receive updates until a player connects again. TFLOnline requests fresh information when a player joins.

This makes the default setup simple and avoids additional infrastructure.

### Project status

**Current version: `0.2.0-SNAPSHOT` — Development**

The current builds provide the core player-count synchronization, server groups, occupancy bars, automatic discovery, and proxy administration tools.

The following features are planned but are **not yet available**:

- LuckPerms rank counters, globally and per server.
- Separate Java and Bedrock player counts.
- Automatic backend health checks and latency.
- Optional Redis synchronization for multi-proxy networks.
- Extended formatting and localization options.

Development builds pass automated compilation and tests, but live compatibility testing across supported platforms is still in progress.

You can follow upcoming improvements in the [roadmap](docs/ROADMAP.md).

### Downloads and building

Download the latest development JARs from [GitHub Releases](https://github.com/TFLivesStudio/TFLOnline/releases).

Each development release includes separate Velocity, BungeeCord, and Paper JARs, along with SHA-256 checksums.

If you prefer building from source, use JDK 17 and Gradle:

```bash
gradle clean build
```

The compiled plugins will be available in their respective module directories under `build/libs/`.

### License and credits

TFLOnline is developed by **tflives** and distributed under the [GNU General Public License v3.0](LICENSE).

Source code, documentation, and development history are available in this repository.

---

## Español

### Descripción general

TFLOnline conecta tu proxy de Minecraft con PlaceholderAPI para que puedas consultar los jugadores conectados a toda tu Network desde cualquier servidor backend.

Muestra los jugadores totales en el lobby, consulta cuántas personas están jugando Skyblock o agrupa varias modalidades para obtener un único contador.

Todo se administra desde el proxy. TFLOnline detecta automáticamente los servidores registrados y mantiene sus datos sincronizados.

### Características

- **Contadores globales.** Consulta los jugadores conectados a toda la Network o a un servidor específico.
- **Detección automática.** Los servidores nuevos se añaden a la configuración sin sobrescribir tus ajustes.
- **Grupos de servidores.** Combina varias modalidades en un mismo contador.
- **Barras de ocupación.** Representa la cantidad de jugadores mediante barras personalizables.
- **Sincronización por eventos.** Actualiza los contadores cuando los jugadores entran, salen o cambian de servidor.
- **Integración con PlaceholderAPI.** El plugin de Paper registra automáticamente sus placeholders.
- **Configuración centralizada.** Administra todo desde un único `config.yml` en el proxy.
- **Instalación sencilla.** No necesitas Redis, bases de datos ni puertos adicionales.

### Compatibilidad

TFLOnline dispone de tres plugins independientes:

| Componente | Plataformas |
|---|---|
| TFLOnline Velocity | Velocity y forks compatibles con su API |
| TFLOnline Bungee | BungeeCord y Waterfall |
| TFLOnline Paper | Paper, Purpur y forks compatibles, incluido Folia |

**Requisitos**

- PlaceholderAPI en los servidores donde quieras utilizar los placeholders.
- Un entorno compatible con Java 17 para ejecutar el plugin. Algunas versiones de Minecraft requieren una versión más reciente de Java.
- Un proxy que conecte los servidores backend.

El proyecto está orientado a Minecraft 1.20 en adelante. La compatibilidad con versiones y forks específicos continúa en fase de pruebas.

### Instalación

**1. Descarga los plugins**

Entra en la sección de [Releases](https://github.com/TFLivesStudio/TFLOnline/releases) y descarga:

- `TFLOnline-Velocity-*.jar` o `TFLOnline-Bungee-*.jar`, según tu proxy.
- `TFLOnline-Paper-*.jar` para los servidores backend.

**2. Instala los archivos**

Coloca el JAR correspondiente en la carpeta `plugins/` de tu proxy.

Después, instala el JAR de Paper en cada servidor donde quieras utilizar los placeholders de TFLOnline.

El componente de Paper funciona como un plugin normal. **No debes colocarlo dentro de `PlaceholderAPI/expansions/`.**

**3. Inicia los servidores**

Inicia o reinicia el proxy y los backends.

TFLOnline generará su configuración y detectará automáticamente los servidores registrados en el proxy.

**4. Personaliza la configuración**

Abre el `config.yml` del proxy, modifica los ajustes necesarios y aplica los cambios mediante:

```text
/tflonline reload
```

Una vez establecida la sincronización, podrás utilizar los placeholders.

### Placeholders disponibles

Puedes utilizarlos en TAB, DeluxeMenus, FancyHolograms, scoreboards y otros plugins compatibles.

| Placeholder | Descripción |
|---|---|
| `%tflonline_total%` | Jugadores conectados a toda la Network |
| `%tflonline_server:skyblock:online%` | Jugadores conectados a Skyblock |
| `%tflonline_group:survival:online%` | Jugadores de un grupo de servidores |
| `%tflonline_server:skyblock:capacity%` | Capacidad configurada del servidor |
| `%tflonline_server:skyblock:state%` | Estado administrativo configurado |
| `%tflonline_bar:skyblock%` | Barra de ocupación del servidor |
| `%tflonline_sync_status%` | Estado de sincronización |
| `%tflonline_sync_age%` | Segundos desde la última sincronización |

Sustituye `skyblock` o `survival` por el nombre del servidor o grupo que corresponda.

**Ten en cuenta lo siguiente:**

- Los grupos de servidores permiten agrupar backends; no representan rangos de LuckPerms.
- Las barras de ocupación necesitan una capacidad configurada superior a cero.
- Los estados muestran valores administrativos configurados, no una comprobación automática de disponibilidad.
- Si falta información o los datos están desactualizados, TFLOnline devuelve `N/A` en lugar de mostrar un contador incorrecto.

### Configuración

Todos los ajustes se administran desde un único `config.yml` ubicado en el proxy.

Este archivo incluye explicaciones, ejemplos y valores recomendados para facilitar su personalización.

Ejemplo básico:

```yaml
synchronization:
  transport: plugin-messaging
  broadcast-interval-seconds: 3
  cache-timeout-seconds: 15
  broadcast-on-events: true
  event-debounce-millis: 150

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

No necesitas registrar manualmente todas las modalidades. TFLOnline detecta los servidores del proxy y conserva las configuraciones que hayas personalizado.

Puedes consultar todas las opciones disponibles en la [guía de configuración](docs/CONFIGURATION.md).

### Comandos

Todos los comandos administrativos utilizan `/tflonline`.

| Comando | Función |
|---|---|
| `/tflonline status` | Consultar el resumen de la Network |
| `/tflonline servers` | Ver los servidores y sus jugadores |
| `/tflonline groups` | Consultar la cantidad de grupos de servidores configurados |
| `/tflonline sync` | Solicitar una nueva sincronización |
| `/tflonline reload` | Recargar la configuración del proxy |
| `/tflonline debug` | Consultar el estado de depuración |

**Permiso:** `tflonline.admin`

Los mensajes administrativos están disponibles en español e inglés mediante `general.language`.

### Cómo funciona la sincronización

TFLOnline utiliza el sistema Plugin Messaging de Minecraft para comunicar el proxy con sus servidores backend.

El proxy obtiene los contadores y envía la información a los servidores conectados. Cada backend conserva una copia temporal de los datos, de modo que PlaceholderAPI puede consultarlos sin realizar una petición de red por cada placeholder.

Los cambios se envían mediante eventos y se complementan con sincronizaciones periódicas.

**Existe una limitación:** Plugin Messaging necesita una conexión de jugador. Si un backend está vacío, no podrá recibir actualizaciones hasta que vuelva a conectarse alguien. TFLOnline solicita información actualizada cuando entra un jugador.

Gracias a este funcionamiento, la instalación predeterminada no requiere infraestructura adicional.

### Estado del proyecto

**Versión actual: `0.2.0-SNAPSHOT` — En desarrollo**

Las compilaciones actuales incluyen sincronización de jugadores, grupos de servidores, barras de ocupación, detección automática y herramientas de administración desde el proxy.

Las siguientes funcionalidades están previstas, pero **todavía no están disponibles**:

- Contadores por rangos de LuckPerms, globales y por servidor.
- Conteos separados para jugadores Java y Bedrock.
- Comprobación automática del estado y la latencia de los backends.
- Redis opcional para Networks con múltiples proxies.
- Más opciones de formato y personalización de mensajes.

Las compilaciones superan pruebas automatizadas, pero todavía estamos comprobando su funcionamiento en instalaciones reales y distintas plataformas.

Puedes consultar las próximas mejoras en el [roadmap](docs/ROADMAP.md).

### Descargas y compilación

Descarga los JAR de desarrollo desde [GitHub Releases](https://github.com/TFLivesStudio/TFLOnline/releases).

Cada publicación incluye los archivos independientes para Velocity, BungeeCord y Paper, además de sus verificaciones SHA-256.

Si prefieres compilar el proyecto desde el código fuente, utiliza JDK 17 y Gradle:

```bash
gradle clean build
```

Encontrarás los JAR compilados dentro de `build/libs/` en el directorio de cada módulo.

### Licencia y créditos

TFLOnline es un proyecto desarrollado por **tflives** y distribuido bajo la licencia [GNU General Public License v3.0](LICENSE).

El código fuente, la documentación y el historial de desarrollo están disponibles públicamente en este repositorio.

---

**TFLOnline** · Developed by [tflives](https://github.com/TFLivesStudio) · Licensed under GPL-3.0
