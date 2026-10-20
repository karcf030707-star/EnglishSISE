#!/usr/bin/env bash
# Renderiza como PNG los diagramas Mermaid de docs/DOCUMENTACION_TECNICA.md y los deja en docs/uml/.
# Las imágenes llevan la paleta de la app y las usa docs/tools/generar-docx.ps1 para el Word.
#
# Usa el servicio público https://mermaid.ink, que solo recibe el texto de cada diagrama.
# Uso (Git Bash):  bash docs/tools/renderizar-uml.sh
set -euo pipefail

DOCS="$(cd "$(dirname "$0")/.." && pwd)"
MD="$DOCS/DOCUMENTACION_TECNICA.md"
UML="$DOCS/uml"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Nombres de archivo en el orden en que aparecen los diagramas en el Markdown.
NOMBRES=(
  01-casos-de-uso 02-clases 03-secuencia-inicio-sesion 04-secuencia-inicio-rechazado
  05-secuencia-restablecer-contrasena 06-secuencia-sesion-guardada 07-secuencia-cerrar-sesion
  08-actividad-validacion-acceso 09-estados-navegacion 10-componentes 11-despliegue 12-modelo-datos
)
# El diagrama de clases es muy ancho: se renderiza a más resolución para que se lea en Word.
declare -A ANCHO=([02-clases]=2600)

INIT="%%{init: {'theme':'base','themeVariables':{'primaryColor':'#E6F4EE','primaryBorderColor':'#2E8B67','primaryTextColor':'#2D2D2D','lineColor':'#4A5651','secondaryColor':'#FEEFE3','tertiaryColor':'#F6F7F6','noteBkgColor':'#FEEFE3','noteBorderColor':'#F08C3C','actorBkg':'#E6F4EE','actorBorder':'#2E8B67','fontFamily':'Arial'}}}%%"

awk -v dir="$TMP" '/^```mermaid/{n++; f=sprintf("%s/%02d.mmd", dir, n); inb=1; next} inb && /^```/{inb=0; close(f); next} inb{print > f}' "$MD"

total=$(ls "$TMP"/*.mmd | wc -l)
if [ "$total" -ne "${#NOMBRES[@]}" ]; then
  echo "El Markdown tiene $total diagramas y el script espera ${#NOMBRES[@]}. Actualiza NOMBRES." >&2
  exit 1
fi

mkdir -p "$UML"
for i in "${!NOMBRES[@]}"; do
  nombre="${NOMBRES[$i]}"
  fuente="$TMP/$(printf '%02d' $((i + 1))).mmd"
  ancho="${ANCHO[$nombre]:-1600}"
  b64=$( { echo "$INIT"; cat "$fuente"; } | base64 -w0 | tr '+/' '-_' | tr -d '=')
  for intento in 1 2 3; do
    codigo=$(curl -s -o "$UML/$nombre.png" -w '%{http_code}' "https://mermaid.ink/img/$b64?type=png&bgColor=FFFFFF&width=$ancho")
    [ "$codigo" = 200 ] && break
    sleep 5
  done
  echo "$nombre: HTTP $codigo"
  [ "$codigo" = 200 ] || { echo "No se pudo renderizar $nombre" >&2; exit 1; }
done
