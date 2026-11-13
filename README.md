# Sphera

App de Android para centralizar soluciones a problemas relacionados con el TDAH.

La pantalla principal tiene 8 habilidades:

- **Universo social**: tus contactos son planetas que se alejan cuanto más tiempo pasa sin hablar.
- **Santuario**: pantalla negra o mezclador ambiental, No molestar, frases de calma y respiración guiada.
- **Punto de Retorno**: apunta qué estabas haciendo y la app te lo recuerda al volver.
- **No sigas al conejo**: aparca una idea sin perder el hilo y explórala después.
- **Paso a paso**: rutinas visuales y el primer paso de una tarea.
- **Chequeo corporal**: hambre, sed, sueño, dolor… antes de seguir.
- **Tarjetas**: mensajes a pantalla completa para cuando no puedes hablar.
- **Puerta lenta**: accesos a las apps que enganchan con una pausa de respiración y una pregunta para pensar antes de abrirlas.

## Instalar

Cada cambio en `main` compila automáticamente un APK y lo publica en **Releases** (`sphera_<versión>.apk`). Descárgalo en el móvil e instálalo.

## Versiones

Están en `app/build.gradle.kts`:

- `versionCode`: súbelo **siempre** en 1. Si no lo subes, Android no deja actualizar encima.
- `versionName`: `0.0.x-alpha` para los cambios pequeños. Pasaremos a `0.1.0` cuando Universo Social esté completo.

Si subes un cambio sin cambiar `versionName`, se sustituye el APK de esa misma release.

## Idiomas

Los textos están en `app/src/main/res/values/strings.xml` (español) y `values-en/strings.xml` (inglés). Para añadir un idioma, copia la carpeta a `values-XX/`, traduce el archivo y añade el código en `res/xml/locales_config.xml` y en `SettingsScreen.kt`.

## Cómo detecta el contacto

- Llamadas en las que se habló (registro de llamadas).
- Mensajes que llegan por WhatsApp, Telegram o SMS (acceso a notificaciones).
- Mensajes que envías desde Sphera, y el botón "Ya he hablado con esta persona".

Todos los datos se guardan solo en el móvil.

## Licencia

Sphera es software libre bajo la [GPL-3.0](LICENSE). El mezclador ambiental del Santuario usa código e iconos de [Blankee](https://github.com/itsPronay/blankee) (GPL-3.0); los sonidos tienen sus propias licencias, detalladas en [SOUNDS_LICENSING.md](SOUNDS_LICENSING.md).
