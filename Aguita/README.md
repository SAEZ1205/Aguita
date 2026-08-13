# 💧 Agüita — alarma real de hidratación para Android

Agüita es una app Android nativa, sin backend y sin cuentas, diseñada para recordar tomar agua con una experiencia tipo despertador: alarma exacta, sonido de alarma en bucle, vibración, pantalla completa cuando Android lo permite y repetición si el usuario no responde.

> No es una web/PWA. Vercel no es necesario para la función de alarma. GitHub guarda el código y GitHub Actions compila el APK instalable.

## Perfil inicial
La primera instalación viene preparada para el caso original:

- Nombre: Mamá
- Edad: 52
- Peso: 84 kg
- Talla: 162 cm
- Actividad: ligera (caminar / ejercicio suave)
- Vaso: 250 ml
- Horario: 08:00–22:00
- Objetivo automático orientativo: ~2.52 L/día
- Distribución automática de alarmas: activada (con este perfil queda cerca de cada 90 min)
- Modo insistente: activado

Todos esos datos se pueden editar; otra persona puede instalar el mismo APK en su teléfono y colocar sus propios datos.

## Qué hace de verdad la alarma

1. `AlarmManager` programa una alarma exacta tipo `AlarmClock`.
2. El `BroadcastReceiver` se ejecuta aunque la interfaz no esté abierta.
3. Se inicia `AlarmService` como servicio en primer plano.
4. El servicio reproduce el sonido predeterminado de ALARMA en bucle y vibra.
5. En modo insistente eleva temporalmente el volumen del stream ALARM al máximo y lo restaura al detenerse.
6. Se publica una notificación `CATEGORY_ALARM` con `fullScreenIntent` para encender/mostrar la pantalla de alarma cuando Android concede ese acceso.
7. Si no hay respuesta, se programa otro aviso en 5 minutos, hasta 3 repeticiones.
8. La alarma puede marcarse como tomada, posponerse 10 min o silenciarse desde la pantalla o la notificación.
9. Si el teléfono se reinicia, las alarmas se programan de nuevo.

Por seguridad y batería, un sonido individual tiene un límite interno de 3 minutos; si el modo insistente está activo, el siguiente aviso vuelve a aparecer después.

## Permisos importantes en Samsung / Android

La app muestra accesos directos para configurar:

- Notificaciones.
- **Alarmas y recordatorios** (`SCHEDULE_EXACT_ALARM`).
- **Alertas a pantalla completa** (`USE_FULL_SCREEN_INTENT`) en Android 14+.
- **No molestar** (`ACCESS_NOTIFICATION_POLICY`) si quieres permitir que el canal de alarma interrumpa DND.

Android siempre conserva el control final: el usuario puede revocar estos permisos. La app no intenta saltarse controles sin consentimiento.

## Hidratación personalizada

La cifra automática es solo una estimación organizativa, no una prescripción médica:

- punto de partida simple: `30 ml × kg de peso`;
- ajuste pequeño solo para actividad moderada/alta;
- límite automático entre 1.5 y 3.5 L;
- el usuario puede usar un **objetivo manual** si un profesional le indicó otro valor.

El horario automático calcula aproximadamente cuántos vasos corresponden al objetivo y distribuye los recordatorios entre la hora de inicio y fin. La edad y la talla se guardan como contexto del perfil, pero no se usan para aumentar por sí solas la cantidad de agua.

Si existe restricción de líquidos o enfermedad renal, cardíaca o hepática, debe usarse la indicación profesional y no aumentar la ingesta basándose solo en la app.

## Compilar APK desde GitHub

Cada push a `main` ejecuta `.github/workflows/build-apk.yml`.

1. Entra a **Actions**.
2. Abre **Build Android APK**.
3. Cuando termine, descarga el artefacto **Aguita-debug-apk**.
4. Dentro estará `Aguita-v1.1-debug.apk`.
5. Instálalo en el Samsung y abre Agüita.
6. Completa los permisos de “Configuración crítica”.
7. Pulsa **PROBAR ALARMA REAL AHORA** antes de activar los recordatorios.

## Stack

- Java 17
- Android nativo (sin librerías externas)
- minSdk 26
- target/compileSdk 36
- Gradle 8.13
- Android Gradle Plugin 8.13.2
