#!/usr/bin/env python3
"""Genera docs/Sphera-Guia-de-uso.pdf: la portada seguida de la guía, impresas con Chrome/Chromium.
Uso: python3 docs/guia/generar_pdf.py   (necesita google-chrome o chromium y pypdf)"""
import datetime, pathlib, re, shutil, subprocess, sys, tempfile
from pypdf import PdfWriter

here = pathlib.Path(__file__).resolve().parent
root = here.parent.parent
version = re.search(r'versionName = "([^"]+)"', (root / "app/build.gradle.kts").read_text()).group(1)
meses = "enero febrero marzo abril mayo junio julio agosto septiembre octubre noviembre diciembre".split()
hoy = datetime.date.today()
chrome = next((c for c in ("google-chrome", "chromium", "chromium-browser") if shutil.which(c)), None)
if not chrome:
    sys.exit("Falta Chrome o Chromium")

with tempfile.TemporaryDirectory() as tmp:
    tmp = pathlib.Path(tmp)
    portada = (here / "portada.html").read_text()
    portada = portada.replace("{{QR}}", (here / "qr.svg").read_text()).replace("{{VERSION}}", version) \
        .replace("{{FECHA}}", f"{meses[hoy.month - 1]} {hoy.year}")
    # Se escribe junto a las originales para que las rutas de la tipografía sigan valiendo.
    filled = here / "_portada.html"
    filled.write_text(portada)
    try:
        pdfs = []
        for name, src in (("portada", filled), ("guia", here / "guia.html")):
            out = tmp / f"{name}.pdf"
            subprocess.run([chrome, "--headless", "--disable-gpu", "--no-sandbox", "--no-pdf-header-footer",
                            f"--print-to-pdf={out}", src.as_uri()], check=True, capture_output=True)
            pdfs.append(out)
    finally:
        filled.unlink()
    w = PdfWriter()
    for p in pdfs:
        w.append(str(p))
    w.add_metadata({"/Title": "Sphera · Guía de instalación y uso", "/Author": "Sphera"})
    dest = root / "docs/Sphera-Guia-de-uso.pdf"
    with open(dest, "wb") as f:
        w.write(f)
    print(dest)
