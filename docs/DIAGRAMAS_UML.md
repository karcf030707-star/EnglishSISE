# EnglishSISE — Diagramas UML

Diagramas del estado actual de la app (inicio de sesión con Firebase y pantalla de inicio del Alumno UTCJ).
Para instalar, configurar y mantener el proyecto, consulta el [Manual técnico](MANUAL_TECNICO.md).

Los diagramas están en [Mermaid](https://mermaid.js.org/). GitHub y Android Studio (con el plugin de Markdown)
los muestran directamente.

| # | Diagrama | Qué responde |
|---|---|---|
| 1 | [Casos de uso](#1-diagrama-de-casos-de-uso) | ¿Quién usa el sistema y para qué? |
| 2 | [Clases](#2-diagrama-de-clases) | ¿Qué clases hay y cómo se relacionan? |
| 3 | [Secuencia](#3-diagramas-de-secuencia) | ¿En qué orden se llaman las piezas en cada flujo? |
| 4 | [Actividad](#4-diagrama-de-actividad-validación-del-acceso) | ¿Qué decide la app al iniciar sesión? |
| 5 | [Estados](#5-diagrama-de-estados-navegación) | ¿Cómo se navega entre pantallas? |
| 6 | [Componentes](#6-diagrama-de-componentes) | ¿Cómo se divide la app en capas? |
| 7 | [Despliegue](#7-diagrama-de-despliegue) | ¿Dónde se ejecuta cada parte? |
| 8 | [Modelo de datos](#8-modelo-de-datos-firestore) | ¿Qué se guarda en Firestore? |

---

## 1. Diagrama de casos de uso

Los actores son los cuatro perfiles de RF-01. Los casos con borde punteado están planeados y todavía muestran «próximamente».

```mermaid
flowchart LR
    AU(["👤 Alumno UTCJ"])
    AE(["👤 Alumno Externo"])
    DO(["👤 Docente"])
    AD(["👤 Administrador"])
    FB[["☁️ Firebase<br/>(Auth + Firestore)"]]

    subgraph Sistema["EnglishSISE"]
        CU1(("Elegir tipo de ingreso<br/>RF-01"))
        CU2(("Iniciar sesión<br/>RF-02"))
        CU3(("Restablecer contraseña<br/>RF-03"))
        CU4(("Ver inicio del alumno"))
        CU5(("Cerrar sesión"))
        CU6(("Cursos disponibles"))
        CU7(("Mi perfil"))
        CU8(("Mi credencial"))
        CU9(("Pago de libros"))
        CU10(("Notificaciones"))
        CU11(("Inicio de Externo,<br/>Docente y Administrador"))
    end

    AU --- CU1
    AE --- CU1
    DO --- CU1
    AD --- CU1
    CU1 -- "«include»" --> CU2
    CU2 -. "«extend»" .-> CU3
    AU --- CU4
    AU --- CU5
    AE --- CU11
    DO --- CU11
    AD --- CU11
    CU4 -. "«extend»" .-> CU6
    CU4 -. "«extend»" .-> CU7
    CU4 -. "«extend»" .-> CU8
    CU4 -. "«extend»" .-> CU9
    CU4 -. "«extend»" .-> CU10
    CU2 --- FB
    CU3 --- FB
    CU4 --- FB

    classDef pendiente stroke-dasharray: 5 5
    class CU6,CU7,CU8,CU9,CU10,CU11 pendiente
```

| Caso de uso | Estado | Dónde está |
|---|---|---|
| Elegir tipo de ingreso (RF-01) | ✅ Implementado | `MainActivity` |
| Iniciar sesión (RF-02) | ✅ Los 4 perfiles | `LoginActivity` + `UsuarioRepository.iniciarSesion()` |
| Restablecer contraseña (RF-03) | ✅ Implementado | `UsuarioRepository.restablecerContrasena()` |
| Ver inicio del alumno | ✅ Con nombre y matrícula reales | `InicioAlumnoUtcjActivity` |
| Cerrar sesión | ✅ Menú ☰ o *Más* | `InicioAlumnoUtcjActivity.cerrarSesion()` |
| Cursos, Perfil, Credencial, Pago de libros, Notificaciones | ⏳ «próximamente» | — |
| Inicio de Alumno Externo, Docente y Administrador | ⏳ Valida la cuenta y luego muestra «próximamente» | — |

---

## 2. Diagrama de clases

```mermaid
classDiagram
    direction LR

    class AppCompatActivity {
        <<AndroidX>>
    }

    class MainActivity {
        <<Activity · LAUNCHER>>
        +onCreate(savedInstanceState)
    }

    class LoginActivity {
        <<Activity>>
        -repositorio : UsuarioRepository
        -perfil : Perfil
        +onCreate(savedInstanceState)
        -aplicarEstiloDePerfil()
        -iniciarSesion()
        -restablecerContrasena()
        -mostrarCargando(cargando)
        -mostrarError(mensaje)
        +intent(context, perfil)$ Intent
    }

    class EstiloPerfil {
        <<enumeration>>
        ALUMNO_UTCJ
        ALUMNO_EXTERNO
        DOCENTE
        ADMINISTRADOR
        +color : Int
        +icono : Int
        +pista : Int
        +de(perfil)$ EstiloPerfil
    }

    class InicioAlumnoUtcjActivity {
        <<Activity>>
        -repositorio : UsuarioRepository
        +onCreate(savedInstanceState)
        -cargarUsuario(uid)
        -mostrarUsuario(usuario)
        -mostrarMenu(ancla)
        -cerrarSesion()
        -irAlAcceso()
        -mostrarProximamente(modulo)
    }

    class UsuarioRepository {
        <<Repositorio>>
        -auth : FirebaseAuth
        -db : FirebaseFirestore
        +uidActual : String?
        +iniciarSesion(identificador, contrasena, perfil, alTerminar)
        +obtenerUsuario(uid, alTerminar)
        +restablecerContrasena(identificador, alTerminar)
        +cerrarSesion()
        -correoDesde(identificador) String
        -traducir(e) ErrorAcceso
        +COLECCION_USUARIOS$ = "usuarios"
        +DOMINIO_CUENTAS$ = "utcj.edu.mx"
        +nombrePerfil(perfil)$ String
    }

    class Usuario {
        <<data class>>
        +uid : String
        +nombre : String
        +apellidos : String
        +perfil : Perfil?
        +matricula : String
        +correo : String
        +carrera : String
        +telefono : String
        +activo : Boolean
        +nombreCompleto : String
    }

    class Perfil {
        <<enumeration>>
        ALUMNO_UTCJ
        ALUMNO_EXTERNO
        DOCENTE
        ADMINISTRADOR
        +clave : String
        +desdeClave(clave)$ Perfil?
    }

    class ErrorAcceso {
        <<sealed>>
        +mensaje : String
    }
    class CredencialesInvalidas { <<object>> }
    class DemasiadosIntentos { <<object>> }
    class SinConexion { <<object>> }
    class SinRegistro { <<object>> }
    class CuentaInactiva { <<object>> }
    class PerfilDistinto { +PerfilDistinto(perfilReal) }
    class Desconocido { +Desconocido(detalle) }

    class FirebaseAuth { <<Firebase SDK>> }
    class FirebaseFirestore { <<Firebase SDK>> }

    AppCompatActivity <|-- MainActivity
    AppCompatActivity <|-- LoginActivity
    AppCompatActivity <|-- InicioAlumnoUtcjActivity

    MainActivity ..> LoginActivity : abre con Perfil
    MainActivity ..> InicioAlumnoUtcjActivity : abre si hay sesión
    LoginActivity ..> InicioAlumnoUtcjActivity : abre tras el acceso
    InicioAlumnoUtcjActivity ..> MainActivity : regresa al cerrar sesión

    LoginActivity *-- EstiloPerfil
    LoginActivity --> UsuarioRepository
    InicioAlumnoUtcjActivity --> UsuarioRepository
    MainActivity ..> UsuarioRepository : consulta uidActual

    UsuarioRepository --> FirebaseAuth
    UsuarioRepository --> FirebaseFirestore
    UsuarioRepository ..> Usuario : crea
    UsuarioRepository ..> ErrorAcceso : devuelve
    Usuario --> Perfil

    ErrorAcceso <|-- CredencialesInvalidas
    ErrorAcceso <|-- DemasiadosIntentos
    ErrorAcceso <|-- SinConexion
    ErrorAcceso <|-- SinRegistro
    ErrorAcceso <|-- CuentaInactiva
    ErrorAcceso <|-- PerfilDistinto
    ErrorAcceso <|-- Desconocido
```

**Notas**

- Los parámetros `alTerminar` son *callbacks* de Kotlin: `(Usuario?, ErrorAcceso?) -> Unit` en `iniciarSesion` y `obtenerUsuario`, y `(ErrorAcceso?) -> Unit` en `restablecerContrasena`. Siempre llega uno de los dos valores, nunca ambos.
- `EstiloPerfil` es un `enum` privado anidado en `LoginActivity`. Relaciona cada perfil con su color, ícono y pista del campo de usuario.
- Las Activities nunca llaman a Firebase directamente: todo pasa por `UsuarioRepository` (RNF-17).

---

## 3. Diagramas de secuencia

### 3.1 Inicio de sesión correcto (Alumno UTCJ)

```mermaid
sequenceDiagram
    actor U as Alumno
    participant MA as MainActivity
    participant LA as LoginActivity
    participant R as UsuarioRepository
    participant AU as Firebase Auth
    participant FS as Firestore
    participant IA as InicioAlumnoUtcjActivity

    U->>MA: Toca «Alumno UTCJ»
    MA->>LA: startActivity(LoginActivity.intent(ALUMNO_UTCJ))
    LA->>LA: aplicarEstiloDePerfil()
    U->>LA: Escribe «2103456» y contraseña, toca «Iniciar sesión»
    LA->>LA: Valida campos vacíos y mostrarCargando(true)
    LA->>R: iniciarSesion("2103456", contraseña, ALUMNO_UTCJ)
    R->>R: correoDesde() → 2103456@utcj.edu.mx
    R->>AU: signInWithEmailAndPassword(correo, contraseña)
    AU-->>R: AuthResult (uid)
    R->>FS: usuarios/{uid}.get()
    FS-->>R: DocumentSnapshot
    R->>R: aUsuario() · comprueba perfil == ALUMNO_UTCJ y activo
    R-->>LA: alTerminar(usuario, null)
    LA->>LA: mostrarCargando(false)
    LA->>IA: startActivity(NEW_TASK | CLEAR_TASK)
    IA->>R: uidActual
    IA->>R: obtenerUsuario(uid)
    R->>FS: usuarios/{uid}.get()
    FS-->>R: DocumentSnapshot
    R-->>IA: alTerminar(usuario, null)
    IA-->>U: «Hola, Karla Cruz» · «Alumno UTCJ · Matrícula: 2103456»
```

### 3.2 Inicio de sesión rechazado

```mermaid
sequenceDiagram
    actor U as Usuario
    participant LA as LoginActivity
    participant R as UsuarioRepository
    participant AU as Firebase Auth
    participant FS as Firestore

    U->>LA: Toca «Iniciar sesión»
    LA->>R: iniciarSesion(identificador, contraseña, perfil)
    R->>AU: signInWithEmailAndPassword()

    alt Credenciales incorrectas, demasiados intentos o sin red
        AU-->>R: Exception
        R->>R: traducir(e) → CredencialesInvalidas / DemasiadosIntentos / SinConexion
        R-->>LA: alTerminar(null, error)
    else Autenticó, pero la cuenta no es válida para este perfil
        AU-->>R: AuthResult (uid)
        R->>FS: usuarios/{uid}.get()
        FS-->>R: Documento inexistente, otro perfil o activo = false
        R->>AU: signOut()
        R-->>LA: alTerminar(null, SinRegistro / PerfilDistinto / CuentaInactiva)
    end

    LA->>LA: mostrarError(error.mensaje)
    LA-->>U: Mensaje en rojo sobre el botón
```

### 3.3 Restablecer contraseña (RF-03)

```mermaid
sequenceDiagram
    actor U as Usuario
    participant LA as LoginActivity
    participant R as UsuarioRepository
    participant AU as Firebase Auth

    U->>LA: Toca «¿Olvidaste tu contraseña?»
    alt Campo de usuario vacío
        LA-->>U: tilUsuario.error = «Escribe tu usuario…»
    else Hay usuario
        LA->>R: restablecerContrasena(identificador)
        R->>AU: sendPasswordResetEmail(correoDesde(identificador))
        alt Éxito
            AU-->>R: OK
            R-->>LA: alTerminar(null)
            LA-->>U: Diálogo «Te enviamos un enlace…»
        else Falla
            AU-->>R: Exception
            R-->>LA: alTerminar(traducir(e))
            LA-->>U: mostrarError(mensaje)
        end
    end
```

### 3.4 Abrir la app con una sesión guardada

Firebase Auth conserva la sesión entre aperturas. `MainActivity` envía siempre al inicio del Alumno UTCJ, y ahí se valida el perfil.

```mermaid
sequenceDiagram
    actor U as Usuario
    participant MA as MainActivity
    participant R as UsuarioRepository
    participant IA as InicioAlumnoUtcjActivity
    participant FS as Firestore

    U->>MA: Abre la app
    MA->>R: uidActual
    alt Sin sesión
        R-->>MA: null
        MA-->>U: Muestra «¿Cómo deseas ingresar?»
    else Con sesión
        R-->>MA: uid
        MA->>IA: startActivity() y finish()
        IA->>R: obtenerUsuario(uid)
        R->>FS: usuarios/{uid}.get()
        alt Alumno UTCJ activo
            R-->>IA: usuario
            IA-->>U: Muestra el inicio con sus datos
        else Sin conexión
            R-->>IA: SinConexion
            IA-->>U: Toast con el error · la sesión se conserva
        else Sin registro, otro perfil o cuenta inactiva
            R-->>IA: error o usuario no válido
            IA->>R: cerrarSesion()
            IA->>MA: irAlAcceso() (NEW_TASK | CLEAR_TASK)
        end
    end
```

### 3.5 Cerrar sesión

```mermaid
sequenceDiagram
    actor U as Alumno
    participant IA as InicioAlumnoUtcjActivity
    participant R as UsuarioRepository
    participant AU as Firebase Auth
    participant MA as MainActivity

    U->>IA: Toca ☰ (btnMenu) o «Más» en la barra inferior
    IA->>IA: mostrarMenu(ancla) → PopupMenu «Cerrar sesión»
    U->>IA: Toca «Cerrar sesión»
    IA->>R: cerrarSesion()
    R->>AU: signOut()
    IA->>MA: irAlAcceso() (NEW_TASK | CLEAR_TASK)
    IA->>IA: finish()
```

---

## 4. Diagrama de actividad: validación del acceso

Muestra las decisiones de `LoginActivity.iniciarSesion()` y `UsuarioRepository.iniciarSesion()`.

```mermaid
flowchart TD
    A([Toca «Iniciar sesión»]) --> B{¿Usuario y contraseña<br/>tienen texto?}
    B -- No --> B1[Marca «Este campo es obligatorio»] --> Z([Fin])
    B -- Sí --> C{¿El usuario<br/>contiene «@»?}
    C -- Sí --> D[Usa el correo tal cual, en minúsculas]
    C -- No --> E[Forma valor@utcj.edu.mx]
    D --> F[signInWithEmailAndPassword]
    E --> F
    F --> G{¿Autenticó?}
    G -- No --> G1[traducir: CredencialesInvalidas,<br/>DemasiadosIntentos, SinConexion o Desconocido]
    G1 --> M[Muestra el mensaje de error] --> Z
    G -- Sí --> H[Lee usuarios/uid en Firestore]
    H --> I{¿Existe el documento?}
    I -- No --> X[signOut] --> I1[SinRegistro] --> M
    I -- Sí --> J{¿perfil coincide con<br/>el botón elegido?}
    J -- No --> X2[signOut] --> J1[PerfilDistinto] --> M
    J -- Sí --> K{¿activo?}
    K -- No --> X3[signOut] --> K1[CuentaInactiva] --> M
    K -- Sí --> L{¿Perfil = Alumno UTCJ?}
    L -- Sí --> L1[Abre InicioAlumnoUtcjActivity] --> Z
    L -- No --> L2[cerrarSesion y Toast<br/>«Inicio …: próximamente»] --> Z
```

---

## 5. Diagrama de estados (navegación)

```mermaid
stateDiagram-v2
    state "Acceso inicial\n(MainActivity)" as Acceso
    state "Inicio de sesión\n(LoginActivity · perfil)" as Login
    state "Validando\n(Auth + Firestore)" as Validando
    state "Inicio Alumno UTCJ\n(InicioAlumnoUtcjActivity)" as Inicio

    [*] --> Acceso : LAUNCHER sin sesión
    [*] --> Inicio : LAUNCHER con sesión guardada

    Acceso --> Login : Elige uno de los 4 perfiles
    Login --> Acceso : Regresar / botón Atrás
    Login --> Validando : Iniciar sesión
    Validando --> Login : Error (mensaje en pantalla)
    Validando --> Login : Otro perfil válido → «próximamente»
    Validando --> Inicio : Alumno UTCJ válido
    Login --> Login : ¿Olvidaste tu contraseña? → diálogo

    Inicio --> Inicio : Tarjeta o barra inferior → «próximamente»
    Inicio --> Acceso : Cerrar sesión
    Inicio --> Acceso : Sesión no válida (sin registro, otro perfil, inactiva)
    Inicio --> [*] : Botón Atrás (es la raíz de la tarea)
    Acceso --> [*] : Botón Atrás
```

---

## 6. Diagrama de componentes

```mermaid
flowchart TB
    subgraph APK["app · com.example.englishsise"]
        direction TB
        MF["AndroidManifest.xml<br/>3 Activities"]
        subgraph UI["Presentación"]
            MA["MainActivity"]
            LA["LoginActivity"]
            IA["InicioAlumnoUtcjActivity"]
        end
        subgraph DATA["Datos · paquete data/"]
            R["UsuarioRepository"]
            MOD["Usuario · Perfil · ErrorAcceso"]
        end
        subgraph RES["Recursos"]
            L["layouts · menu"]
            S["strings · colors · themes"]
            D["drawables"]
        end
        GS["google-services.json"]
    end

    subgraph LIB["Bibliotecas"]
        AX["AndroidX<br/>AppCompat · Activity · Core · ConstraintLayout"]
        MAT["Material Components"]
        FSDK["Firebase SDK (BoM 34.19.0)<br/>Auth · Firestore · Analytics"]
    end

    MF --> UI
    UI --> RES
    UI --> AX
    UI --> MAT
    UI --> R
    R --> MOD
    R --> FSDK
    GS -. "plugin google-services" .-> FSDK
```

---

## 7. Diagrama de despliegue

```mermaid
flowchart LR
    subgraph DEV["📱 Dispositivo Android · API 24 o superior"]
        APP["EnglishSISE.apk"]
        CACHE[("Sesión de Auth<br/>y caché de Firestore")]
        APP --- CACHE
    end

    subgraph GCP["☁️ Proyecto Firebase · englishsise-c8f66 · plan Spark"]
        AUTH["Authentication<br/>correo y contraseña"]
        FS[("Cloud Firestore<br/>colección usuarios")]
        RULES["firestore.rules"]
        GA["Google Analytics<br/>(sin vincular)"]
        RULES -. protege .- FS
    end

    ADM["🧑‍💻 Área de Inglés<br/>Firebase Console"]

    APP -- "HTTPS" --> AUTH
    APP -- "HTTPS / gRPC" --> FS
    APP -. "eventos" .-> GA
    ADM -- "alta de usuarios y documentos" --> AUTH
    ADM --> FS
```

---

## 8. Modelo de datos (Firestore)

Cada cuenta de Firebase Auth tiene un documento en `usuarios` cuyo ID es el `uid` de esa cuenta.

```mermaid
erDiagram
    CUENTA_AUTH ||--|| USUARIOS : "uid = id del documento"

    CUENTA_AUTH {
        string uid PK
        string email "matricula@utcj.edu.mx o correo propio"
        string password "lo guarda Firebase, nunca la app"
    }

    USUARIOS {
        string id PK "uid de Firebase Auth"
        string nombre
        string apellidos
        string perfil "alumno_utcj · alumno_externo · docente · administrador"
        string matricula
        string correo
        string carrera
        string telefono
        boolean activo "si falta, la app lo toma como true"
    }
```

**Permisos (`firestore.rules`)**

| Operación sobre `usuarios/{uid}` | Quién puede |
|---|---|
| Leer | El propio usuario o un Administrador |
| Actualizar | El propio usuario, solo los campos `telefono` y `correo` (RF-05) |
| Crear o borrar | Solo un Administrador |
| Cualquier otra colección | Nadie (cerrado hasta que existan los módulos) |
