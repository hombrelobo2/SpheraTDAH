# Sphera

App de Android, libre y gratuita, que reúne herramientas para el día a día con TDAH (y que también ayudan con el autismo). **Cero fricción, cero gamificación**: sin puntos, rachas ni medallas. Sin cuentas ni anuncios: tus datos se quedan en tu móvil.

📄 **[Guía de instalación y uso (PDF)](docs/Sphera-Guia-de-uso.pdf)**: explicada paso a paso, con la instalación, cada módulo, los widgets y qué hacer si algo no va.

⬇️ **[Descargar la última versión](https://github.com/hombrelobo2/SpheraTDAH/releases/download/ultima/Sphera.apk)** · [todas las versiones](https://github.com/hombrelobo2/SpheraTDAH/releases)

## Qué hace

- 🪐 **Universo social**: tus contactos son planetas que se alejan cuanto más tiempo pasa sin hablar. Resumen diario, avisos, cumpleaños y mensajes para empezar.
- 🌑 **Santuario**: pantalla negra con No molestar, ruido de fondo o mezclador ambiental de 24 sonidos, frases tranquilas y respiración guiada.
- 📍 **Punto de Retorno**: apunta dónde lo dejas y qué toca después; al volver, es lo primero que ves.
- 🐇 **No sigas al conejo**: aparca una curiosidad sin perder el hilo y explórala luego.
- 👣 **Paso a paso**: rutinas en pasos pequeños y «primer paso» para tareas que bloquean.
- 🫀 **Chequeo del cuerpo**: hambre, sed, baño, sueño, estímulos o bajón de dopamina, con ideas sencillas.
- 🗣️ **Tarjetas**: mensajes a pantalla completa para cuando no puedes hablar.
- 🚪 **Puerta lenta**: una pausa con respiración y una pregunta antes de abrir las apps que enganchan.

Idiomas: español, inglés y euskera. En Ajustes puedes ordenar y ocultar las casillas del inicio. Widgets: Accesos (hasta 4, a elegir), Punto de Retorno y Santuario (1×1).

## Instalar y actualizar

Descarga `sphera_X.Y.Z.apk` desde [Releases](https://github.com/hombrelobo2/SpheraTDAH/releases) e instálalo (la guía explica cómo, incluido el aviso de Play Protect). Las versiones nuevas se instalan encima sin perder nada.

Una vez por semana, con internet, la app consulta `version.json` en la publicación [`ultima`](https://github.com/hombrelobo2/SpheraTDAH/releases/tag/ultima) y avisa si hay versión nueva (se puede desactivar en Ajustes → Acerca de). Es la única conexión a internet de la app y no envía ningún dato.

Requiere Android 8.0 o superior. No hay versión para iOS.

## Privacidad

Todo se guarda en el móvil. De las notificaciones de WhatsApp, Telegram, Signal, Viber y SMS solo se usa quién escribe, para acercar su planeta; el contenido no se guarda.

## Para desarrollar

- Kotlin + Jetpack Compose. Cada cambio en `main` compila el APK firmado y lo publica en Releases (`.github/workflows/build.yml`).
- Versión en `app/build.gradle.kts`: sube `versionCode` siempre en 1 y `versionName` según el cambio.
- Textos en `app/src/main/res/values/strings.xml` (español), `values-en/` (inglés) y `values-eu/` (euskera batua). Para otro idioma, copia la carpeta a `values-XX/`, tradúcela y añade el código en `res/xml/locales_config.xml` y en Ajustes.
- La guía está en `docs/guia/` (`portada.html` y `guia.html`). Para regenerar el PDF: `python3 docs/guia/generar_pdf.py` (necesita Chrome o Chromium y `pypdf`).

## Licencia

Sphera es software libre bajo la [GPL-3.0](LICENSE). Usa código adaptado de [Blankee](https://github.com/itsPronay/blankee) (mezclador), [Visual Routines](https://github.com/oikumene-works/visual-routines) (Paso a paso), [Aucards](https://github.com/vadimerenkov/Aucards) (Tarjetas) y [Brethap](https://github.com/jithware/brethap) (respiración), todos GPL-3.0. Los sonidos tienen sus propias licencias, detalladas en [SOUNDS_LICENSING.md](SOUNDS_LICENSING.md). Tipografía Atkinson Hyperlegible (SIL OFL 1.1).
