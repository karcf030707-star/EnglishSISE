<#
.SYNOPSIS
  Genera docs/DOCUMENTACION_TECNICA.docx a partir de docs/DOCUMENTACION_TECNICA.md.

.DESCRIPTION
  1. Convierte el Markdown a HTML. Cada bloque ```mermaid se reemplaza por su imagen de docs/uml/NN-*.png,
     en el mismo orden en que aparecen.
  2. Abre el HTML con Microsoft Word, incrusta las imágenes, agrega portada e índice automático,
     y guarda el .docx.

  Requiere Microsoft Word instalado. Las imágenes se regeneran con docs/tools/renderizar-uml.sh.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File docs/tools/generar-docx.ps1
#>
param(
    [string]$Markdown = (Join-Path $PSScriptRoot '..\DOCUMENTACION_TECNICA.md'),
    [string]$Salida   = (Join-Path $PSScriptRoot '..\DOCUMENTACION_TECNICA.docx')
)

$ErrorActionPreference = 'Stop'
$Markdown = (Resolve-Path $Markdown).Path
$docsDir  = Split-Path $Markdown -Parent
$umlDir   = Join-Path $docsDir 'uml'
$Salida   = [System.IO.Path]::GetFullPath($Salida)
$imagenes = @(Get-ChildItem $umlDir -Filter '*.png' | Sort-Object Name)

# ---------------------------------------------------------------- Markdown -> HTML

function Escapar([string]$t) { [System.Net.WebUtility]::HtmlEncode($t) }

function Convertir-Linea([string]$texto) {
    # Protege el código en línea para no aplicarle otros formatos.
    $codigos = New-Object System.Collections.Generic.List[string]
    $texto = [regex]::Replace($texto, '`([^`]+)`', {
        param($m) $codigos.Add($m.Groups[1].Value); "@@CODE$($codigos.Count - 1)@@" })
    $texto = Escapar $texto
    # Enlaces: los externos se conservan; los internos o relativos quedan como texto.
    $texto = [regex]::Replace($texto, '\[([^\]]+)\]\(([^)]+)\)', {
        param($m)
        if ($m.Groups[2].Value -match '^https?://') { "<a href=""$($m.Groups[2].Value)"">$($m.Groups[1].Value)</a>" }
        else { $m.Groups[1].Value } })
    $texto = [regex]::Replace($texto, '\*\*(.+?)\*\*', '<b>$1</b>')
    $texto = [regex]::Replace($texto, '(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])', '<i>$1</i>')
    for ($i = 0; $i -lt $codigos.Count; $i++) {
        $texto = $texto.Replace("@@CODE$i@@", "<code>$(Escapar $codigos[$i])</code>")
    }
    $texto
}

function Celdas([string]$fila) {
    # Separa una fila de tabla por "|", sin cortar dentro de `código`.
    $celdas = @(); $actual = ''; $enCodigo = $false
    foreach ($c in $fila.Trim().Trim('|').ToCharArray()) {
        if ($c -eq '`') { $enCodigo = -not $enCodigo }
        if ($c -eq '|' -and -not $enCodigo) { $celdas += $actual.Trim(); $actual = '' } else { $actual += $c }
    }
    $celdas + $actual.Trim()
}

$lineas = [System.IO.File]::ReadAllLines($Markdown, [System.Text.Encoding]::UTF8)
$html = New-Object System.Text.StringBuilder
$parrafo = New-Object System.Collections.Generic.List[string]
$listas = New-Object System.Collections.Stack   # elementos: @{Tipo='ul'|'ol'; Sangria=n}
$ultimoTitulo = ''
$figura = 0
$enContenido = $false

function Cerrar-Parrafo {
    if ($parrafo.Count -gt 0) {
        [void]$html.AppendLine('<p>' + (Convertir-Linea ($parrafo -join ' ')) + '</p>')
        $parrafo.Clear()
    }
}
function Cerrar-Listas([int]$hasta = -1) {
    while ($listas.Count -gt 0 -and $listas.Peek().Sangria -gt $hasta) {
        [void]$html.AppendLine('</li></' + $listas.Pop().Tipo + '>')
    }
}

$i = 0
while ($i -lt $lineas.Count) {
    $l = $lineas[$i]

    # El índice manual se reemplaza por el índice automático de Word.
    if ($l -match '^## Contenido') { Cerrar-Parrafo; Cerrar-Listas; [void]$html.AppendLine('<p class=indice-titulo>Contenido</p><p>[[INDICE]]</p>'); $enContenido = $true; $i++; continue }
    if ($enContenido) { if ($l -match '^---\s*$') { $enContenido = $false }; $i++; continue }

    # Bloques de código y diagramas Mermaid.
    if ($l -match '^```(\w*)') {
        Cerrar-Parrafo; Cerrar-Listas
        $lenguaje = $Matches[1]; $bloque = @(); $i++
        while ($i -lt $lineas.Count -and $lineas[$i] -notmatch '^```') { $bloque += $lineas[$i]; $i++ }
        $i++
        if ($lenguaje -eq 'mermaid') {
            $img = $imagenes[$figura]; $figura++
            [void]$html.AppendLine("<p class=figura><img src=""$($img.FullName)""></p>")
            [void]$html.AppendLine("<p class=pie>Figura $figura. $(Escapar $ultimoTitulo)</p>")
        } else {
            [void]$html.AppendLine('<pre>' + (Escapar ($bloque -join "`n")) + '</pre>')
        }
        continue
    }

    # Títulos.
    if ($l -match '^(#{1,4})\s+(.*)$') {
        Cerrar-Parrafo; Cerrar-Listas
        $nivel = $Matches[1].Length; $titulo = $Matches[2]
        $ultimoTitulo = ($titulo -replace '^[\d.]+\s+', '' -replace '`', '')
        if ($nivel -eq 1 -and $titulo -match '^Parte') {
            [void]$html.AppendLine("<br clear=all style='page-break-before:always'><h1>$(Convertir-Linea $titulo)</h1>")
        } elseif ($nivel -eq 1) {
            # El título principal ya está en la portada.
        } else {
            [void]$html.AppendLine("<h$nivel>$(Convertir-Linea $titulo)</h$nivel>")
        }
        $i++; continue
    }

    # Línea horizontal.
    if ($l -match '^---\s*$') { Cerrar-Parrafo; Cerrar-Listas; $i++; continue }

    # Tablas.
    if ($l -match '^\s*\|') {
        Cerrar-Parrafo; Cerrar-Listas
        $filas = @()
        while ($i -lt $lineas.Count -and $lineas[$i] -match '^\s*\|') { $filas += $lineas[$i]; $i++ }
        [void]$html.AppendLine('<table>')
        [void]$html.AppendLine('<tr>' + ((Celdas $filas[0] | ForEach-Object { "<th>$(Convertir-Linea $_)</th>" }) -join '') + '</tr>')
        foreach ($f in $filas[2..($filas.Count - 1)]) {
            if ($f) { [void]$html.AppendLine('<tr>' + ((Celdas $f | ForEach-Object { "<td>$(Convertir-Linea $_)</td>" }) -join '') + '</tr>') }
        }
        [void]$html.AppendLine('</table>')
        continue
    }

    # Citas.
    if ($l -match '^>\s?(.*)$') {
        Cerrar-Parrafo; Cerrar-Listas
        $cita = @()
        while ($i -lt $lineas.Count -and $lineas[$i] -match '^>\s?(.*)$') { $cita += $Matches[1]; $i++ }
        [void]$html.AppendLine('<p class=nota>' + (Convertir-Linea ($cita -join ' ')) + '</p>')
        continue
    }

    # Listas (con anidación por sangría).
    if ($l -match '^(\s*)([-*]|\d+\.)\s+(.*)$') {
        Cerrar-Parrafo
        $sangria = $Matches[1].Length; $marca = $Matches[2]; $texto = $Matches[3]
        $tipo = if ($marca -match '^\d') { 'ol' } else { 'ul' }
        $apertura = if ($tipo -eq 'ol') { "<ol start=$([int]$marca.TrimEnd('.'))>" } else { '<ul>' }
        if ($listas.Count -eq 0 -or $sangria -gt $listas.Peek().Sangria) {
            [void]$html.Append("$apertura<li>")
            $listas.Push(@{ Tipo = $tipo; Sangria = $sangria })
        } else {
            Cerrar-Listas $sangria
            if ($listas.Count -eq 0) { [void]$html.Append("$apertura<li>"); $listas.Push(@{ Tipo = $tipo; Sangria = $sangria }) }
            else { [void]$html.Append('</li><li>') }
        }
        [void]$html.Append((Convertir-Linea $texto))
        $i++; continue
    }

    # Línea en blanco.
    if ($l.Trim() -eq '') {
        Cerrar-Parrafo
        # Una lista termina si lo siguiente no es otro elemento ni texto con sangría.
        $sig = if ($i + 1 -lt $lineas.Count) { $lineas[$i + 1] } else { '' }
        if ($listas.Count -gt 0 -and $sig -notmatch '^(\s*)([-*]|\d+\.)\s+' -and $sig -notmatch '^\s{2,}\S') { Cerrar-Listas }
        $i++; continue
    }

    # Texto que continúa un elemento de lista.
    if ($listas.Count -gt 0 -and $l -match '^\s{2,}(\S.*)$') {
        [void]$html.Append(' ' + (Convertir-Linea $Matches[1])); $i++; continue
    }

    Cerrar-Listas
    $parrafo.Add($l.Trim())
    $i++
}
Cerrar-Parrafo; Cerrar-Listas

if ($figura -ne $imagenes.Count) {
    throw "Hay $figura diagramas en el Markdown pero $($imagenes.Count) imágenes en $umlDir. Vuelve a renderizar los UML."
}

$portada = @"
<p class=portada-inst>Universidad Tecnológica de Ciudad Juárez</p>
<p class=portada-sub>Tecnologías de la Información · Análisis y Diseño de Software · Grupo DSM41</p>
<p class=portada-titulo>EnglishSISE</p>
<p class=portada-sub2>Documentación técnica</p>
<p class=portada-sub>Manual técnico, configuración de Firebase y diagramas UML</p>
<p class=portada-fecha>Versión 1.0 · $((Get-Date).ToString('MMMM yyyy', [System.Globalization.CultureInfo]'es-MX'))</p>
<br clear=all style='page-break-before:always'>
"@

$css = @'
body { font-family: Calibri, Arial, sans-serif; font-size: 11pt; color: #2D2D2D; }
h1 { font-size: 24pt; color: #2E8B67; border-bottom: 2pt solid #F08C3C; padding-bottom: 4pt; }
h2 { font-size: 16pt; color: #2E8B67; margin-top: 18pt; }
h3 { font-size: 13pt; color: #3FA27D; }
h4 { font-size: 11.5pt; color: #2D2D2D; }
p.indice-titulo { font-size: 20pt; color: #2E8B67; font-weight: bold; }
code { font-family: Consolas, monospace; font-size: 10pt; color: #2E8B67; }
pre { font-family: Consolas, monospace; font-size: 9.5pt; background: #F6F7F6; border: 1px solid #E6E9E7; padding: 6pt; }
table { border-collapse: collapse; width: 100%; margin: 6pt 0; }
th { background: #E6F4EE; color: #2E8B67; border: 1px solid #B9D9CB; padding: 4pt; text-align: left; font-size: 10pt; }
td { border: 1px solid #D5DDD9; padding: 4pt; vertical-align: top; font-size: 10pt; }
p.nota { background: #FEEFE3; border-left: 4pt solid #F08C3C; padding: 6pt; }
p.figura { text-align: center; margin-top: 8pt; }
p.pie { text-align: center; font-size: 9pt; color: #8A908D; font-style: italic; }
p.portada-inst { text-align: center; font-size: 16pt; color: #2E8B67; font-weight: bold; margin-top: 120pt; }
p.portada-sub { text-align: center; font-size: 11pt; color: #8A908D; }
p.portada-titulo { text-align: center; font-size: 40pt; color: #2E8B67; font-weight: bold; margin-top: 80pt; }
p.portada-sub2 { text-align: center; font-size: 20pt; color: #F08C3C; }
p.portada-fecha { text-align: center; font-size: 11pt; margin-top: 160pt; }

'@

$htmlCompleto = "<!DOCTYPE html><html lang=es><head><meta charset=utf-8><title>EnglishSISE — Documentación técnica</title><style>$css</style></head><body>$portada$($html.ToString())</body></html>"
$htmlTemp = Join-Path ([System.IO.Path]::GetTempPath()) 'englishsise-documentacion.html'
[System.IO.File]::WriteAllText($htmlTemp, $htmlCompleto, (New-Object System.Text.UTF8Encoding $true))

# ---------------------------------------------------------------- HTML -> DOCX con Word

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $doc = $word.Documents.Open($htmlTemp, $false, $true)
    $doc.ActiveWindow.View.Type = 3   # Diseño de impresión

    # Márgenes de 2.5 cm.
    $ps = $doc.PageSetup
    $margen = $word.CentimetersToPoints(2.5)
    $ps.TopMargin = $margen; $ps.BottomMargin = $margen; $ps.LeftMargin = $margen; $ps.RightMargin = $margen
    $anchoUtil = $ps.PageWidth - $ps.LeftMargin - $ps.RightMargin
    $altoUtil  = $ps.PageHeight - $ps.TopMargin - $ps.BottomMargin - 60

    # Incrusta las imágenes y las ajusta a la página.
    foreach ($forma in @($doc.InlineShapes)) {
        if ($forma.LinkFormat) { $forma.LinkFormat.SavePictureWithDocument = $true; $forma.LinkFormat.BreakLink() }
    }
    foreach ($forma in @($doc.InlineShapes)) {
        $forma.LockAspectRatio = -1
        if ($forma.Width -gt $anchoUtil) { $forma.Width = $anchoUtil }
        if ($forma.Height -gt $altoUtil) { $forma.Height = $altoUtil }
    }

    # Índice automático.
    $buscar = $doc.Content
    if ($buscar.Find.Execute('[[INDICE]]')) {
        $buscar.Text = ''
        [void]$doc.TablesOfContents.Add($buscar, $true, 1, 2)
    }

    # Número de página en el pie.
    $pie = $doc.Sections.Item(1).Footers.Item(1)
    [void]$pie.PageNumbers.Add(2, $false)

    foreach ($t in @($doc.TablesOfContents)) { $t.Update() }

    if (Test-Path $Salida) { Remove-Item $Salida -Force }
    $doc.SaveAs2([ref]$Salida, [ref]16)   # wdFormatXMLDocument
    $doc.Close([ref]0)
    Write-Output "Generado: $Salida ($figura diagramas)"
} finally {
    $word.Quit()
    [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
    Remove-Item $htmlTemp -ErrorAction SilentlyContinue
}
