# Hoja de ruta

## Próximas mejoras

- Dictado por voz también en el Punto de Retorno.
- Andamio: pulir las plantillas (más naturales, menos "rellenar huecos") y añadir escenarios.

## Para la beta

- Actualizador en Ajustes: comprobar si hay versión nueva en GitHub Releases y actualizar desde la app (descargar APK e instalar).
  - Pendiente decidir: el repo es privado y la app no puede leer sus releases sin token. Opciones: hacer público el repo, o publicar los APK en un repo público aparte.
  - Necesita permisos de Internet e "instalar apps desconocidas"; Play Protect puede seguir pidiendo desactivarse al instalar.

## Pendiente / ideas

- Botón de correo electrónico si el contacto tiene email en la agenda.
- Instagram, Messenger, etc.: solo con usuario guardado a mano por contacto.
- Actualizar las acciones de GitHub (setup-java v5, etc.) por los avisos de deprecación.
- Las 2 habilidades restantes de la pantalla principal.
- Tarjeta de comunicación: texto editable desde Ajustes.
- iOS: de momento no (publicar exige pagar a Apple y Sphera es libre y gratuita). Si algún día se hace, valorar Compose Multiplatform para reutilizar el código Kotlin/Compose. En iOS no se pueden leer llamadas ni notificaciones de otras apps: el universo iría solo con el botón manual y los mensajes enviados desde Sphera.
