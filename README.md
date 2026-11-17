<div align="center">

  <img src="assets/readme/logo.svg" alt="Mobile Harness Logo" width="104" height="104" style="border-radius: 24px;" />

  # Mobile Harness

  ### *The complete autonomous AI development workspace for Android.*

  **Chat with coding agents, edit projects, execute real Linux commands, and preview live web servers — all directly on your phone.**

  <br />

  [![Latest release](https://img.shields.io/github/v/release/techjarves/Mobile-Harness?label=Release&color=F28C52&style=flat-square&logo=github&logoColor=white)](https://github.com/techjarves/Mobile-Harness/releases/latest)
  [![Android 9+](https://img.shields.io/badge/Android-9%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#system-requirements)
  [![ARM64](https://img.shields.io/badge/CPU-ARM64-5B8DEF?style=flat-square)](#system-requirements)
  [![MIT License](https://img.shields.io/badge/License-MIT-8B7CF6?style=flat-square&logo=opensourceinitiative&logoColor=white)](LICENSE)
  [![Watch demo](https://img.shields.io/badge/Watch-Demo-FF0000?style=flat-square&logo=youtube&logoColor=white)](https://www.youtube.com/techjarves)

  <br />

  [**Download Online APK**](https://github.com/techjarves/Mobile-Harness/releases/latest/download/mobile-harness-online.apk) &nbsp;•&nbsp;
  [**Watch Walkthrough (3 min)**](https://youtu.be/QzAau52Z7yQ) &nbsp;•&nbsp;
  [**Quickstart Guide**](#quickstart) &nbsp;•&nbsp;
  [**Architecture**](#architecture) &nbsp;•&nbsp;
  [**Build from Source**](#developer-guides)

</div>

<br />

---

<p align="center">
  <a href="https://youtu.be/x5T56timmQw" target="_blank" rel="noopener noreferrer">
    <img src="https://img.youtube.com/vi/x5T56timmQw/maxresdefault.jpg" alt="Mobile Harness Walkthrough and Live Product Demo" width="920" />
  </a>
  <br />
  <sub>Watch the product walkthrough and demo &nbsp;|&nbsp; <i>Setting up Ubuntu, connecting Claude Code, and building an app on Android</i></sub>
</p>

---

<br />

> [!IMPORTANT]
> **Environment Security Notice**  
> Mobile Harness runs on **ARM64 Android devices** using a private userspace PRoot layer. While isolated from other apps via standard Android sandbox permissions, PRoot is not a virtualization boundary or hardened security jail. Only execute projects and dependencies you own or trust.

<br />

> **Fork.** Este repositório (`luisnovais-dev/Mobile-Harness`) é um fork de [`techjarves/Mobile-Harness`](https://github.com/techjarves/Mobile-Harness). Todos os créditos do projeto original pertencem ao seu criador, techjarves. O código segue a licença MIT ([LICENSE](LICENSE)). As mudanças deste fork estão nos commits e na PR de adoção do processo.

## Download Mobile Harness

<div align="center">
  <p><strong>89.1 MB</strong> · Core, Python, and Android runtime bundles are downloaded only when needed. Supports secure in-app updates.</p>
  <a href="https://github.com/techjarves/Mobile-Harness/releases/latest/download/mobile-harness-online.apk">
    <img src="https://img.shields.io/badge/Download-Online_APK-F28C52?style=for-the-badge&logo=android&logoColor=white" alt="Download Online APK" />
  </a>
</div>

<p align="center">
  <strong>ARM64 Android 9+</strong><br />
  <sub>Direct APK installation · No root required · No USB or wireless ADB pairing</sub>
</p>

<br />

## Capabilities

Mobile Harness unites modern **Jetpack Compose UI** with a self-contained **Ubuntu 20.04 LTS subsystem**. It gives you a desktop-class software development environment in your pocket without requiring root access, unlocked bootloaders, or external applications like Termux.

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>Autonomous Agent Coding</h3>
      <p>Native integrations with Claude Code, DeepSeek Harness, and Antigravity CLI. Each agent has an isolated driver, settings, and resumable project conversations.</p>
    </td>
    <td width="50%" valign="top">
      <h3>Isolated Linux Subsystem</h3>
      <p>A full Ubuntu 20.04 ARM64 userspace running inside PRoot. Includes Node.js, npm, Git, OpenSSL, and essential shell tooling out of the box.</p>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>Instant Web Preview</h3>
      <p>Spun up a Vite, Next.js, or Express server? Test web interfaces in real-time within a restricted, sandboxed mobile WebView with console telemetry.</p>
    </td>
    <td width="50%" valign="top">
      <h3>Keystore-Grade Encryption</h3>
      <p>Your API keys and credentials are encrypted using Android Keystore-backed AES-256 GCM. No telemetry, no remote proxies, and zero plain-text leaks.</p>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>Persistent Project Sessions</h3>
      <p>Keep projects, chat history, files, and task context together so work can continue across app sessions.</p>
    </td>
    <td width="50%" valign="top">
      <h3>Native File Workflow</h3>
      <p>Browse, edit, search, and attach files directly from the app interface. Interoperate with system storage via Android Storage Access Framework (SAF), and reach shared device storage from the project terminal after granting storage access.</p>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>On-device Android Builds</h3>
      <p>Build, install, and launch Android APKs directly on the phone without USB or wireless ADB pairing.</p>
    </td>
    <td width="50%" valign="top">
      <h3>Optional Toolchains</h3>
      <p>Add Python, Android, C/C++, and PHP tooling only when a project needs it.</p>
    </td>
  </tr>
</table>

<br />

## Workspace Interface

<table>
  <tr>
    <th width="33%" align="center">Projects</th>
    <th width="33%" align="center">Terminal</th>
    <th width="33%" align="center">Settings</th>
  </tr>
  <tr>
    <td align="center" valign="top">
      <img src="assets/readme/projects.png" alt="Projects workspace overview" width="100%" />
    </td>
    <td align="center" valign="top">
      <img src="assets/readme/terminal.png" alt="Linux terminal execution" width="100%" />
    </td>
    <td align="center" valign="top">
      <img src="assets/readme/settings.png" alt="Runtime and provider configuration" width="100%" />
    </td>
  </tr>
  <tr>
    <td align="center"><sub>Create, organize, and resume isolated workspace sessions.</sub></td>
    <td align="center"><sub>Execute real Linux commands and scripts with instant output.</sub></td>
    <td align="center"><sub>Manage AI providers, installed toolchains, themes, and runtime health.</sub></td>
  </tr>
</table>

<br />

## Quickstart

Get up and running in 3 guided steps:

### 1. Download & Install
Download the latest signed release APK from [GitHub Releases](https://github.com/techjarves/Mobile-Harness/releases/latest).

```text
Target Architecture : ARM64 (arm64-v8a)
Package Version     : latest release
Minimum OS Level    : Android 9.0 (API 28)
```

### 2. Guided Bootstrap (~10 Minutes)
Launch the application and follow the interactive setup wizard:

<table>
  <tr>
    <th width="33%" align="center">1 · System Readiness</th>
    <th width="33%" align="center">2 · Toolchains</th>
    <th width="33%" align="center">3 · AI Provider</th>
  </tr>
  <tr>
    <td align="center" valign="top">
      <img src="assets/readme/setup-notifications.png" alt="System compatibility check" width="100%" />
    </td>
    <td align="center" valign="top">
      <img src="assets/readme/setup-toolchains.png" alt="Toolchain selection" width="100%" />
    </td>
    <td align="center" valign="top">
      <img src="assets/readme/setup-provider.png" alt="Provider connection" width="100%" />
    </td>
  </tr>
  <tr>
    <td align="center"><sub>Verifies device storage, CPU architecture, and background service permissions.</sub></td>
    <td align="center"><sub>Select core Ubuntu runtime and optional development stacks.</sub></td>
    <td align="center"><sub>Securely store your API keys in Android Keystore.</sub></td>
  </tr>
</table>

### 3. Create & Build
1. Tap **New Project** or launch an instant **Quick Project**.
2. Open the **AI Workspace** and describe what you want to build.
3. Watch the agent inspect files, draft code, run builds, and launch local web previews.

<br />

## Model Providers

Mobile Harness uses Claude Code's Anthropic-compatible API protocol. You can connect official endpoints or route requests through compatible translation proxies:

| Provider | Integration Type | Streaming | Tool Calling | Status | Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Anthropic API** | Direct Key | Supported | Supported | `Recommended` | Primary supported backend |
| **OpenRouter** | Gateway | Supported | Supported | `Supported` | Routes compatible models through one API key |
| **DeepSeek** | Direct Key | Supported | Supported | `Supported` | Anthropic-compatible endpoint |
| **Kimi** | Direct Key | Supported | Supported | `Supported` | Anthropic-compatible Moonshot endpoint |
| **Custom API** | Endpoint Override | Compatible | Compatible | `Experimental` | User-configured gateway |

> [!NOTE]
> API keys are stored with hardware-backed Android Keystore AES-256-GCM encryption. Antigravity Google OAuth credentials are created and retained only by the official `agy` CLI in its persistent Linux home; Mobile Harness never reads or copies its tokens.

### Coding agents

| Agent | Authentication | Installation | Isolation |
| :--- | :--- | :--- | :--- |
| **Claude Code** | Claude account or API-key providers | Included in Core | Existing Claude bridge and settings |
| **DeepSeek Harness** | API-key providers | On demand | Existing DSH bridge and settings |
| **Antigravity CLI** | Official Google OAuth flow | Version-pinned online download | Dedicated `agy` bridge, model, effort, and conversation IDs |

For Antigravity, select **Antigravity CLI**, install it, and tap **Sign in with Google**. Mobile Harness starts the official CLI login, opens the freshly generated Google URL in the system browser, and sends the returned one-time code back to that waiting process. The app does not embed Google login in a WebView and does not construct its own OAuth request.

> [!WARNING]
> Antigravity tasks currently launch with `--dangerously-skip-permissions`. This gives the official agent permission to run tools without individual PocketDev approval prompts. Use it only with projects and prompts you trust. Account quotas and service limits still apply; signing in does not provide unlimited usage.

When Android is selected during onboarding, Mobile Harness installs that complete toolchain into its private Ubuntu environment. Android projects can then be built with the workspace play button. The resulting debug APK is passed directly to Android's system package installer and launched after installation; USB debugging, wireless debugging, an ADB port, and a pairing code are not required. Android still requires the user to allow installs from Mobile Harness and confirm each installation.

<br />

## Architecture

Mobile Harness bridges native Android Jetpack Compose to an isolated PRoot Linux execution layer via an optimized C++ JNI bridge:

```mermaid
flowchart TB
    subgraph Host[" Android Native Host (Kotlin + Jetpack Compose) "]
        UI["Modern UI Layer<br/>Projects • Chat • Terminal • Web Preview"]
        Service["Foreground Runtime Service<br/>Process Lifecycle & WakeLocks"]
        Keystore["Android Keystore<br/>AES-256 GCM Credentials"]
        Bridge["C++ JNI Process Bridge<br/>Native Launcher & Pipe Multiplexer"]
    end

    subgraph Subsystem[" Private Linux Subsystem (PRoot ARM64) "]
        Ubuntu["Ubuntu 20.04 LTS Subsystem<br/>Rootless Userspace Environment"]
        Agent["Agent Registry<br/>Claude • DeepSeek • Antigravity"]
        Tools["Development Toolchains<br/>Node.js • Git • Python • C++"]
        Workspace["Local Project Workspace<br/>Files • Git History • Checkpoints"]
    end

    subgraph Cloud[" Model Providers "]
        Anthropic["Anthropic / API Gateways"]
        Gateways["Google Antigravity Service"]
    end

    UI <--> Service
    Service <--> Bridge
    Bridge <--> Subsystem
    Keystore -.-> UI
    Agent <--> Cloud
    Agent --> Tools
    Tools <--> Workspace
    UI -.-> Workspace

    classDef hostStyle fill:#0f172a,stroke:#38bdf8,stroke-width:1.5px,color:#f8fafc;
    classDef subStyle fill:#1e1b4b,stroke:#818cf8,stroke-width:1.5px,color:#f8fafc;
    classDef cloudStyle fill:#18181b,stroke:#f59e0b,stroke-width:1.5px,color:#f8fafc;

    class UI,Service,Keystore,Bridge hostStyle;
    class Ubuntu,Agent,Tools,Workspace subStyle;
    class Anthropic,Gateways cloudStyle;
```

### Core Runtime Components
* **Base Environment**: Ubuntu 20.04 ARM64 verified rootfs
* **Agent Engine**: Registry-selected, isolated drivers for Claude Code, DeepSeek Harness, and the official Antigravity CLI
* **Native Tooling**: Node.js LTS, npm, Git, OpenSSL, curl, and GNU coreutils
* **Process Virtualization**: PRoot user-space architecture emulation with zero kernel modifications

<br />

## System Requirements

| Metric | Minimum Specification | Recommended Specification |
| :--- | :--- | :--- |
| **Operating System** | Android 9.0 (API level 28) | Android 13.0+ (API level 33+) |
| **CPU Architecture** | 64-bit ARM (`arm64-v8a`) | High-performance 8-Core ARM64 (Snapdragon 8 Gen 1+ / Dimensity) |
| **RAM** | 4 GB | 8 GB or more |
| **Free Storage** | 2.5 GB (Base Runtime) | 8.0 GB+ (For multi-language toolchains and build caches) |
| **Network** | Stable connection for setup & API | High-speed Wi-Fi during initial rootfs provisioning |

<br />

---

## Developer Guides

<details>
<summary><b>Building from source (Android Studio & NDK)</b></summary>

<br />

### Prerequisites
* **Java Development Kit**: JDK 17 (Eclipse Temurin ou OpenJDK). Ubuntu/Debian: `sudo apt install openjdk-17-jdk-headless`.
* **Android SDK**: platform API 36 (`compileSdk 36`), `platform-tools` e `cmdline-tools`. Build-tools 35.0.0 é instalado pelo Gradle na primeira compilação, desde que as licenças estejam aceitas.
* **Android NDK**: `28.2.13676358` (valor padrão de `app/build.gradle.kts`; para outra versão, passe `-PmhNdkVersion=<versão>`).
* **CMake**: `3.22.1`, instalado pelo SDK Manager.
* **Android Studio** (opcional): Ladybug / Hedgehog ou mais recente. A linha de comando basta.
* **Submódulos**: `third_party/proot` e `third_party/libandroid-shmem`. Sem eles, o CMake falha.

Instalação do SDK pela linha de comando (com `cmdline-tools` em `$ANDROID_HOME/cmdline-tools/latest`):

```bash
# Caminho do SDK nesta máquina. Cada máquina define o seu.
export ANDROID_HOME=/caminho/para/o/Android/Sdk
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "ndk;28.2.13676358" "cmake;3.22.1"
```

O build por linha de comando precisa localizar o SDK. Duas formas, que valem para qualquer shell, inclusive o de agentes:

* Definir `ANDROID_HOME` no perfil do shell (`~/.profile` ou `~/.bashrc`), apontando para o SDK instalado nesta máquina. O caminho pode variar entre máquinas, mas precisa estar definido localmente em cada uma.
* Criar `local.properties` na raiz do checkout com `sdk.dir=<caminho do SDK>`. O arquivo é ignorado pelo git, então vale por checkout e por worktree.

### Clone & Build Debug APK
```bash
# Clone the repository
git clone https://github.com/luisnovais-dev/Mobile-Harness.git
cd Mobile-Harness

# Inicializa os submódulos (também dentro de cada worktree)
git submodule update --init --recursive

# Build dos dois flavors de debug (offline e online)
./gradlew assembleOfflineDebug assembleOnlineDebug

# Instalar o APK do flavor escolhido em um dispositivo conectado
adb install -r app/build/outputs/apk/offline/debug/app-offline-debug.apk
```

### Quality Assurance & Testing
```bash
# Run unit tests (os dois flavors)
./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest

# Run static analysis linter
./gradlew lint
```

### Target Profiles
* **Direct Sideload APK** (Default): Targets API 28 to preserve proven userspace execution paths under Android 10-14.
* **Google Play Compliance Build**:
  ```bash
  ./gradlew -PplayBuild=true assembleDebug
  ```
  Verificado neste fork: compila os dois flavors de debug (`offline` e `online`) com `targetSdk 36`. Não gera APK de release assinado. O bundle da Play exige o keystore de upload (`MH_UPLOAD_STORE_FILE`, `MH_UPLOAD_STORE_PASSWORD`, `MH_UPLOAD_KEY_ALIAS`, `MH_UPLOAD_KEY_PASSWORD`) e a URL pública da política de privacidade (`-PprivacyPolicyUrl`). Refer to the [Google Play Release Checklist](docs/PLAY_STORE_CHECKLIST.md) for signing and permission policies.

</details>

<details>
<summary><b>Optional toolchains and development stacks</b></summary>

<br />

Mobile Harness allows downloading optional developer packs on demand to conserve space:

* **Python Suite**: Python 3.10+, pip, virtualenv, and essential scientific C-extensions.
* **Android & JVM**: OpenJDK 17 headless runtime and Gradle build tools.
* **C / C++ Compiler Suite**: GCC/G++, Clang, Make, and CMake for native tool compilation.
* **PHP Development**: PHP CLI runtime, Composer, and standard database extensions.

> *Note: Kernel-level virtualization technologies such as Docker, KVM, systemd services, and nested hardware emulators are not supported under PRoot.*

</details>

<details>
<summary><b>Repository directory structure</b></summary>

<br />

```text
Mobile-Harness/
├── app/src/main/
│   ├── java/com/jarves/mh/
│   │   ├── data/       # Preferences, Keystore AES encryption, SQLite persistence
│   │   ├── model/      # Data entities: Projects, Chats, Files, Tool calls
│   │   ├── runtime/    # PRoot installer, C++ agent bridge, foreground services
│   │   └── ui/         # Jetpack Compose screens, Material 3 theme, ViewModels
│   ├── cpp/            # Native C++ launcher, pseudo-terminal pipe handler
│   ├── assets/         # Verified rootfs checksums, licenses, base configuration
│   └── res/            # Android icons, XML drawables, vector assets
├── fastlane/           # Play Store metadata, graphics, and release automation
└── docs/               # In-depth architectural notes & Play Store review guides
```

</details>

<details>
<summary><b>Security model and data privacy</b></summary>

<br />

* **Zero Cloud Intermediaries**: Mobile Harness connects your device directly to your chosen AI endpoint. No intermediate relays or telemetry servers collect your prompts or code.
* **Scoped Storage**: Project imports and exports use Android's official Storage Access Framework (SAF). A project can be created in a folder of the device storage, chosen by the user after granting storage access: sideload builds request `READ`/`WRITE` storage permission, and Play builds request All files access (`MANAGE_EXTERNAL_STORAGE`). The session mounts only that project folder, never the whole shared storage. Without the grant, projects stay in app-private storage. Revoking the grant shows a warning when the project opens, and the session uses the private folder.
* **Cryptographic Checksums**: Root filesystem archives and Claude Code CLI packages are verified via SHA-256 checksums prior to extraction.
* **Encrypted Secrets**: API tokens are encrypted in hardware-backed storage via Android Keystore.

- ARM64 phones only
- No hardened isolation for hostile code
- Terminal input/output currently uses a process bridge rather than a complete PTY emulator; full-screen interactive programs may render incorrectly
- Background tasks are subject to Android process and battery policies
- Custom providers may lack Claude-compatible thinking, tool use, token counting, or streaming behavior
- Large builds can be slow and memory-intensive under PRoot
- Runtime installation requires a substantial download and free storage
- Project-specific Android libraries may still be downloaded by Gradle when they are not already in the bundled Maven cache

Read our complete [Privacy Policy](PRIVACY.md).

</details>

Mobile Harness is currently intended for signed direct APK distribution and private testing. Its Android-project workflow requests permission to submit user-built APKs to Android's package installer, which requires a dedicated Google Play policy declaration and approval if distributed through Play.

<br />

## Current Limitations

* **Architecture**: Exclusively supports 64-bit ARM (`arm64-v8a`) hardware.
* **Process Isolation**: PRoot maps file systems and IDs in user space; it is not a cryptographically hardened container or VM.
* **Terminal Emulation**: The process bridge handles standard CLI workflows and REPLs; specialized ncurses applications may experience minor layout artifacts.
* **OS Process Management**: Heavy compilation workloads may be throttled if Android applies aggressive battery optimization. It is recommended to exempt Mobile Harness from battery optimization in device settings.

<br />

## Legal & Trademarks

* Mobile Harness is an independent open-source project and is not affiliated with, endorsed by, or sponsored by Anthropic.
* **Claude** and **Claude Code** are trademarks of Anthropic, PBC. Claude Code CLI is downloaded directly from Anthropic's official distribution endpoints during setup and remains governed by Anthropic's license terms.
* Ubuntu, Android, Kotlin, Node.js, Git, and other registered trademarks belong to their respective copyright holders.
* Third-party open-source licenses are compiled in [`app/src/main/assets/licenses`](app/src/main/assets/licenses).

<br />

## License

This project is licensed under the [MIT License](LICENSE). Third-party runtime binaries and packages remain governed by their respective upstream licenses.

<br />

---

<div align="center">
  <sub>Crafted for developers who want a serious, uncompromised development environment wherever they go.</sub>
  <br />
  <sub>Copyright © 2026 Tech Jarves. All rights reserved.</sub>
</div>
