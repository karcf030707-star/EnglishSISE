# Configuración de Firebase (proyecto `englishsise-c8f66`)

La app usa **Firebase Authentication** (correo y contraseña) para el inicio de sesión (RF-02, RF-03)
y **Cloud Firestore** para los datos de los usuarios.

## 1. Activar los servicios

1. Firebase Console → **Authentication** → *Comenzar* → *Método de acceso* → habilitar **Correo electrónico/contraseña**.
2. Firebase Console → **Firestore Database** → *Crear base de datos* → modo **producción** → región `nam5` (o la más cercana).
3. Firestore → pestaña **Reglas** → pegar el contenido de [`firestore.rules`](../firestore.rules) → *Publicar*.

## 2. Crear un alumno de prueba

1. **Authentication → Usuarios → Agregar usuario**
   - Correo: `<matrícula>@utcj.edu.mx` (p. ej. `2103456@utcj.edu.mx`)
   - Contraseña: la que elijas (mínimo 6 caracteres)
   - Copia el **UID de usuario** que aparece en la lista.
2. **Firestore → Iniciar colección** `usuarios` → ID del documento = **el UID copiado**, con estos campos:

| Campo       | Tipo    | Ejemplo                      |
|-------------|---------|------------------------------|
| `nombre`    | string  | `Karla`                      |
| `apellidos` | string  | `Cruz`                       |
| `perfil`    | string  | `alumno_utcj`                |
| `matricula` | string  | `2103456`                    |
| `correo`    | string  | `2103456@utcj.edu.mx`        |
| `carrera`   | string  | `Desarrollo de Software`     |
| `telefono`  | string  | `6561234567`                 |
| `activo`    | boolean | `true`                       |

Valores válidos de `perfil`: `alumno_utcj`, `alumno_externo`, `docente`, `administrador`.

## 3. Probar

En la app: **Alumno UTCJ** → escribe `2103456` (o el correo completo) y la contraseña → debe abrir
el Inicio con "Hola, Karla Cruz" y la matrícula. Para salir: menú ☰ o **Más** → *Cerrar sesión*.

## Cómo funciona el acceso

- Si el usuario escribe su matrícula o número de empleado (sin `@`), la app inicia sesión con
  `<valor>@utcj.edu.mx` (constante `DOMINIO_CUENTAS` en `UsuarioRepository`).
- Tras autenticar, se lee `usuarios/{uid}` y se verifica que el `perfil` coincida con el botón
  elegido en la pantalla 1 y que la cuenta esté `activo`.
- "¿Olvidaste tu contraseña?" envía el enlace de Firebase al correo de la cuenta (RF-03).
- Por ahora solo existe el Inicio del Alumno UTCJ; los otros perfiles validan el acceso y muestran
  "próximamente".
