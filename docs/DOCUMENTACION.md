# EnglishSISE — Documentación técnica

**EnglishSISE** es el *Sistema de control de cursos de Inglés* de la Universidad Tecnológica de Ciudad Juárez (UTCJ). Es una aplicación Android nativa escrita en Kotlin.

> **Estado actual: prototipo visual.** Hay dos pantallas: *Acceso inicial* (RF-01) e *Inicio del Alumno UTCJ* (Tabla 4 del ERS). Todavía no hay inicio de sesión (RF-02), datos reales ni backend. Las opciones que aún no existen muestran el mensaje «*X*: próximamente».

Los diagramas están en [Mermaid](https://mermaid.js.org/). GitHub y Android Studio (con el plugin de Markdown) los muestran directamente.

---

## 1. Inicio rápido

| Requisito | Valor |
|---|---|
| IDE | Android Studio (versión reciente) |
| JDK | 11 o superior |
| `minSdk` / `targetSdk` / `compileSdk` | 24 / 37 / 37 |
| Paquete | `com.example.englishsise` |

1. Abre la carpeta del proyecto en Android Studio y espera a que termine la sincronización de Gradle.
2. Elige un emulador o dispositivo con Android 7.0 (API 24) o superior.
3. Ejecuta la configuración **app**. También puedes hacerlo desde la terminal:

```bash
./gradlew installDebug
```

---

## 2. Estructura del proyecto

```
app/src/main/
├── AndroidManifest.xml                 # Declara las 2 Activities; MainActivity es la LAUNCHER
├── java/com/example/englishsise/
│   ├── MainActivity.kt                 # Pantalla 1 · Acceso inicial (RF-01)
│   └── InicioAlumnoUtcjActivity.kt     # Pantalla 2 · Inicio Alumno UTCJ (Tabla 4)
└── res/
    ├── layout/
    │   ├── activity_main.xml           # Botones de tipo de ingreso
    │   └── activity_inicio_alumno_utcj.xml  # Encabezado, tarjetas de módulos y barra inferior
    ├── menu/menu_nav_alumno_utcj.xml   # Elementos de la BottomNavigationView
    ├── drawable/                       # Íconos vectoriales y fondos (gradiente, tarjetas, banner)
    ├── color/nav_item_color.xml        # Selector de color de la barra inferior
    └── values/ (strings, colors, themes)
```

**Dependencias principales:** AndroidX AppCompat, Activity KTX, Core KTX, ConstraintLayout y Material Components (`MaterialButton`, `BottomNavigationView`).

---

## 3. Diagrama de casos de uso

Muestra los actores definidos en RF-01 y lo que cada uno puede hacer. Los casos con línea punteada están planeados pero todavía no se implementan.

```mermaid
flowchart LR
    AU(["👤 Alumno UTCJ"])
    AE(["👤 Alumno Externo"])
    DO(["👤 Docente"])
    AD(["👤 Administrador"])

    subgraph Sistema["EnglishSISE"]
        CU1(("Elegir tipo de ingreso<br/>(RF-01)"))
        CU2(("Iniciar sesión<br/>(RF-02)"))
        CU3(("Ver inicio del alumno"))
        CU4(("Consultar cursos disponibles"))
        CU5(("Ver mi perfil"))
        CU6(("Ver mi credencial"))
        CU7(("Pagar libros"))
        CU8(("Ver notificaciones"))
    end

    AU --- CU1
    AE --- CU1
    DO --- CU1
    AD --- CU1
    AU --- CU3
    CU1 -. "«include» (pendiente)" .-> CU2
    CU3 -. "«extend»" .-> CU4
    CU3 -. "«extend»" .-> CU5
    CU3 -. "«extend»" .-> CU6
    CU3 -. "«extend»" .-> CU7
    CU3 -. "«extend»" .-> CU8

    classDef pendiente stroke-dasharray: 5 5
    class CU2,CU4,CU5,CU6,CU7,CU8 pendiente
```

| Caso de uso | Estado en el prototipo |
|---|---|
| Elegir tipo de ingreso | ✅ Implementado (`MainActivity`) |
| Iniciar sesión | ⏳ No implementado. *Alumno UTCJ* entra directo a su inicio |
| Ver inicio del alumno | ✅ Implementado (`InicioAlumnoUtcjActivity`) |
| Cursos, Perfil, Credencial, Pago de libros, Notificaciones | ⏳ Solo muestran «próximamente» |
| Accesos de Alumno Externo, Docente y Administrador | ⏳ Solo muestran «próximamente» |

---

## 4. Diagrama de clases

```mermaid
classDiagram
    direction LR

    class AppCompatActivity {
        <<AndroidX>>
        +onCreate(savedInstanceState: Bundle?)
        +startActivity(intent: Intent)
        +findViewById(id: Int) View
    }

    class MainActivity {
        <<Activity · LAUNCHER>>
        +onCreate(savedInstanceState: Bundle?)
    }

    class InicioAlumnoUtcjActivity {
        <<Activity>>
        +onCreate(savedInstanceState: Bundle?)
        -mostrarProximamente(modulo: Int)
    }

    class activity_main {
        <<layout>>
        btnAlumnoUtcj : MaterialButton
        btnAlumnoExterno : MaterialButton
        btnDocente : MaterialButton
        btnAdministrador : MaterialButton
    }

    class activity_inicio_alumno_utcj {
        <<layout>>
        btnMenu : View
        txtSaludo : TextView
        cardCursos : View
        cardPerfil : View
        cardCredencial : View
        cardPagoLibros : View
        cardNotificaciones : View
        bottomNav : BottomNavigationView
    }

    class menu_nav_alumno_utcj {
        <<menu>>
        nav_inicio
        nav_perfil
        nav_credencial
        nav_cursos
        nav_mas
    }

    AppCompatActivity <|-- MainActivity
    AppCompatActivity <|-- InicioAlumnoUtcjActivity
    MainActivity ..> InicioAlumnoUtcjActivity : «Intent» abre
    MainActivity --> activity_main : setContentView
    InicioAlumnoUtcjActivity --> activity_inicio_alumno_utcj : setContentView
    activity_inicio_alumno_utcj *-- menu_nav_alumno_utcj : bottomNav usa
```

**Responsabilidades**

- **`MainActivity`**: activa el modo *edge-to-edge* y ajusta el padding a las barras del sistema. El botón *Alumno UTCJ* abre `InicioAlumnoUtcjActivity`. Los otros tres botones muestran un `Toast` con el nombre del perfil.
- **`InicioAlumnoUtcjActivity`**: relaciona cada tarjeta de módulo con su texto (`Map<Int, Int>`) y le asigna un listener que llama a `mostrarProximamente()`. En la `BottomNavigationView`, *Inicio* queda siempre seleccionado: el listener devuelve `false` mientras no existan las demás pantallas.

---

## 5. Diagramas de secuencia

### 5.1 Ingreso como Alumno UTCJ

```mermaid
sequenceDiagram
    actor U as Usuario
    participant SO as Android
    participant MA as MainActivity
    participant IA as InicioAlumnoUtcjActivity

    U->>SO: Abre la app
    SO->>MA: onCreate()
    MA->>MA: enableEdgeToEdge()<br/>setContentView(activity_main)
    MA->>MA: Registra listeners de los 4 botones
    U->>MA: Toca «Alumno UTCJ»
    MA->>SO: startActivity(Intent → InicioAlumnoUtcjActivity)
    SO->>IA: onCreate()
    IA->>IA: setContentView(activity_inicio_alumno_utcj)
    IA->>IA: Registra listeners de tarjetas, menú y bottomNav
    IA-->>U: Muestra el inicio del alumno
```

### 5.2 Elegir un módulo que aún no existe

```mermaid
sequenceDiagram
    actor U as Usuario
    participant IA as InicioAlumnoUtcjActivity
    participant T as Toast

    alt Toca una tarjeta (p. ej. «Cursos disponibles»)
        U->>IA: onClick(cardCursos)
        IA->>IA: mostrarProximamente(R.string.modulo_cursos_disponibles)
        IA->>T: makeText("Cursos disponibles: próximamente").show()
    else Toca un elemento de la barra inferior distinto de Inicio
        U->>IA: onItemSelected(nav_cursos)
        IA->>T: makeText("Cursos: próximamente").show()
        IA-->>U: return false (Inicio sigue seleccionado)
    end
```

---

## 6. Diagrama de estados (navegación)

```mermaid
stateDiagram-v2
    [*] --> AccesoInicial : LAUNCHER

    state "Acceso inicial\n(MainActivity)" as AccesoInicial
    state "Inicio Alumno UTCJ\n(InicioAlumnoUtcjActivity)" as InicioAlumno

    AccesoInicial --> InicioAlumno : Alumno UTCJ
    AccesoInicial --> AccesoInicial : Alumno Externo / Docente /\nAdministrador → Toast «próximamente»
    InicioAlumno --> InicioAlumno : Tarjeta, menú o bottomNav\n→ Toast «próximamente»
    InicioAlumno --> AccesoInicial : Botón Atrás
    AccesoInicial --> [*] : Botón Atrás
```

---

## 7. Diagrama de componentes

```mermaid
flowchart TB
    subgraph APK["app (com.example.englishsise)"]
        direction TB
        MF["AndroidManifest.xml"]
        subgraph UI["Capa de presentación (Kotlin)"]
            MA["MainActivity"]
            IA["InicioAlumnoUtcjActivity"]
        end
        subgraph RES["Recursos"]
            L["layouts"]
            M["menu"]
            S["strings / colors / themes"]
            D["drawables"]
        end
    end

    subgraph LIB["Bibliotecas"]
        AX["AndroidX<br/>AppCompat · Activity · Core · ConstraintLayout"]
        MAT["Material Components"]
    end

    FUT[("Backend / datos<br/>(pendiente)")]

    MF --> UI
    UI --> RES
    UI --> AX
    UI --> MAT
    UI -. futuro .-> FUT
```

---

## 8. Próximos pasos sugeridos

1. **Inicio de sesión (RF-02):** agregar una pantalla de login entre *Acceso inicial* y cada inicio por perfil.
2. **Módulos del alumno:** crear una Activity o Fragment para Cursos, Perfil, Credencial, Pago de libros y Notificaciones, y conectarlos a las tarjetas y a la `BottomNavigationView`.
3. **Datos reales:** reemplazar el texto fijo `saludo_ejemplo` («Hola, Karla Cruz») por el nombre del usuario autenticado.
4. **Otros perfiles:** pantallas de inicio para Alumno Externo, Docente y Administrador.
5. **Arquitectura:** cuando haya datos, conviene agregar `ViewModel` y repositorios (MVVM) para no meter la lógica en las Activities.
