# Hoja de ruta

## Próxima versión (0.0.3-alpha)

- Universo social: mostrar nombre + inicial del apellido en el planeta ("Unai B.") para distinguir contactos con el mismo nombre.
- Telegram Plus (`org.telegram.plus`): comprobar que aparece en "Escribir por" y añadirlo a las apps cuyas notificaciones cuentan como contacto.
- "¿Cada cuánto quieres hablar?" como desplegable en vez de botones (al añadir y en la ficha del planeta).
- Alias por contacto: nombre editable que se muestra en el planeta y en la ficha, sin tocar la agenda. Si hay alias, sustituye a "nombre + inicial".
  - Ojo: la detección por notificaciones compara con el nombre de la agenda, no con el alias; hay que guardar los dos.

## Para la beta

- Actualizador en Ajustes: comprobar si hay versión nueva en GitHub Releases y actualizar desde la app (descargar APK e instalar).
  - Pendiente decidir: el repo es privado y la app no puede leer sus releases sin token. Opciones: hacer público el repo, o publicar los APK en un repo público aparte.
  - Necesita permisos de Internet e "instalar apps desconocidas"; Play Protect puede seguir pidiendo desactivarse al instalar.

## Pendiente / ideas

- Botón de correo electrónico si el contacto tiene email en la agenda.
- Instagram, Messenger, etc.: solo con usuario guardado a mano por contacto.
- Actualizar las acciones de GitHub (setup-java v5, etc.) por los avisos de deprecación.
- Las otras 5 habilidades de la pantalla principal.
