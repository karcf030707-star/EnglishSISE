---
name: guardar-version
description: Guarda la versión actual de la app EnglishSISE para llevar control de versiones. Sube versionCode y versionName en app/build.gradle.kts, registra los cambios en CHANGELOG.md, hace commit y crea un tag de git vX.Y.Z. Úsala cuando el usuario diga "guarda la versión", "nueva versión", "sube la versión", "release", "/guardar-version" o similar.
argument-hint: "[patch|minor|major|X.Y.Z] [nota opcional]"
---

# Guardar versión de EnglishSISE

La versión de la app vive en `app/build.gradle.kts`, dentro de `defaultConfig`:

```kotlin
versionCode = 1        // entero, sube +1 en cada versión guardada
versionName = "1.0"    // versión semántica visible al usuario
```

Cada versión guardada queda registrada en tres lugares: `build.gradle.kts`, `CHANGELOG.md` (raíz del repo) y un tag anotado de git `vX.Y.Z`.

## Pasos

### 1. Revisar el estado

```bash
git status --short
git describe --tags --abbrev=0 2>/dev/null   # último tag; vacío si es la primera versión
grep -nE "version(Code|Name)" app/build.gradle.kts
```

- Si hay cambios sin commitear en código de la app, pregunta al usuario si deben incluirse en esta versión (se commitean primero con su propio mensaje) o si debe esperar. Ignora `.idea/` y `build/`.
- Si `versionName` tiene sólo dos partes (`"1.0"`), trátalo como `1.0.0`.

### 2. Calcular la nueva versión

Según el argumento (`$ARGUMENTS`):

| Argumento | Ejemplo desde 1.2.3 | Cuándo |
|---|---|---|
| `patch` (por defecto) | 1.2.4 | Correcciones, ajustes pequeños |
| `minor` | 1.3.0 | Pantallas o funciones nuevas |
| `major` | 2.0.0 | Cambios grandes o incompatibles |
| `X.Y.Z` | la indicada | El usuario la da explícitamente |

Si no se dio argumento, sugiere el tipo mirando los commits desde el último tag (funciones nuevas → `minor`, sólo arreglos → `patch`) y confirma con el usuario antes de seguir.

- `versionCode` nuevo = actual + 1 (nunca se reutiliza ni baja).
- Verifica que el tag `vX.Y.Z` no exista ya (`git tag -l vX.Y.Z`).

### 3. Actualizar `app/build.gradle.kts`

Edita sólo las dos líneas `versionCode` y `versionName` con la herramienta Edit.

### 4. Actualizar `CHANGELOG.md`

Obtén los cambios desde el último tag (o todo el historial si es la primera versión):

```bash
git log <ultimo-tag>..HEAD --pretty=format:"%s" --no-merges
```

Agrega una entrada **arriba** de las anteriores (crea el archivo si no existe, con el encabezado `# Historial de versiones`):

```markdown
## [X.Y.Z] - AAAA-MM-DD (versionCode N)

### Agregado
- ...
### Cambiado
- ...
### Corregido
- ...
```

Agrupa los mensajes de commit en esas secciones, redactados en español y de forma breve; omite las secciones vacías. Si el usuario dio una nota, inclúyela al inicio de la entrada.

### 5. Commit y tag

```bash
git add app/build.gradle.kts CHANGELOG.md
git commit -m "Versión X.Y.Z (versionCode N)"
git tag -a vX.Y.Z -m "Versión X.Y.Z (versionCode N)"
```

- No hagas `git push` ni `git push --tags` sin que el usuario lo pida.
- Si la rama actual no es `main`, avisa que el tag queda sobre esta rama.

### 6. Opcional: compilar el APK

Sólo si el usuario lo pide, genera el APK de la versión:

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/`.

### 7. Resumen

Informa al usuario: versión anterior → nueva, versionCode, tag creado, rama, y si falta hacer push.

## Consultar versiones guardadas

- Lista de versiones: `git tag -l "v*" --sort=-v:refname`
- Ver una versión: `git show vX.Y.Z --stat`
- Volver temporalmente a una versión: `git checkout vX.Y.Z` (avisar que deja HEAD desacoplado)
