<<<<<<< HEAD
# myco
=======
# Myco 🍄

**Red mesh de mensajería descentralizada sin internet ni datos móviles.**

Myco permite enviar mensajes cifrados usando Bluetooth LE como red mesh. Los mensajes saltan de teléfono en teléfono — como el micelio de los hongos — hasta llegar al destinatario, sin servidores, sin internet, sin rastreo.

> Desarrollado por **Batalla Group**

---

## Stack técnico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Clean Architecture |
| DI | Hilt 2.50 |
| Base de datos | Room 2.6.1 |
| Transporte | Android BLE (BluetoothLE API) |
| Cifrado | ECDH P-256 + AES-256-GCM + ECDSA |
| Almacenamiento seguro | EncryptedSharedPreferences |
| Serialización | Gson |
| QR | ZXing |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |

---

## Compilar

### Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- SDK Android 34

### Pasos

```bash
# 1. Clonar el repositorio
git clone https://github.com/batallagroup/myco.git
cd myco

# 2. Abrir en Android Studio y dejar que Gradle sincronice
# File > Open > seleccionar la carpeta Myco/

# 3. Compilar debug
./gradlew assembleDebug

# 4. Compilar release (requiere keystore configurado)
./gradlew assembleRelease
```

### Configurar release signing

Crea un keystore con keytool:
```bash
keytool -genkeypair -v -keystore myco-release.jks \
  -alias myco -keyalg RSA -keysize 2048 -validity 10000
```

Agrega en `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../myco-release.jks")
        storePassword = System.getenv("KEYSTORE_PASS")
        keyAlias = "myco"
        keyPassword = System.getenv("KEY_PASS")
    }
}
```

---

## Protocolo Myco v1.0

### Estructura del paquete

```
MycoPacket {
    messageId:       UUID único del mensaje
    senderId:        Hash(encPublicKey)[0..7] — ID del remitente
    recipientId:     Hash(encPublicKey)[0..7] — ID del destinatario
    hopCount:        Saltos dados (0 al enviar)
    maxHops:         15
    createdAt:       Timestamp de creación (ms)
    expiresAt:       createdAt + 24h
    encryptedContent: Contenido cifrado (base64)
    senderSignature:  Firma ECDSA del remitente (base64)
    senderEncPublicKey: Clave pública del remitente para ECDH (base64)
}
```

### Flujo de un mensaje

```
[Remitente] → cifra con ECDH(encPK_destinatario, encSK_remitente) + AES-256-GCM
            → firma con ECDSA(signSK_remitente)
            → crea MycoPacket
            → lo envía por BLE a nodos cercanos

[Nodo intermedio] → recibe paquete
                  → verifica que no sea duplicado (anti-loop cache)
                  → verifica TTL (expiredAt > now)
                  → verifica hopCount < maxHops
                  → incrementa hopCount
                  → almacena en transit_messages
                  → reenvía a sus nodos cercanos (si relay activado)

[Destinatario] → recibe paquete
              → recognoce su recipientId
              → descifra con ECDH(encPK_remitente, encSK_destinatario)
              → guarda en messages DB
```

### Seguridad

- **Confidencialidad**: ECDH Diffie-Hellman + AES-256-GCM. Los nodos intermedios solo ven metadatos (sender/recipient ID), nunca el contenido.
- **Autenticidad**: Firma ECDSA SHA-256 del remitente incluida en cada paquete.
- **Anti-loop**: Cada nodo mantiene un caché de IDs de mensajes ya procesados.
- **TTL**: Los mensajes expiran a las 24h y se eliminan automáticamente.
- **Almacenamiento**: Claves privadas guardadas en `EncryptedSharedPreferences` (respaldado por Android Keystore).

> **Nota de producción**: La implementación actual usa ECDH P-256 + AES-256-GCM para compatibilidad con Android 8.0+. La librería `lazysodium-android` está incluida como dependencia para migrar a Curve25519 / XChaCha20-Poly1305 en la v1.1, que ofrece mayor resistencia cuántica y rendimiento en móviles.

---

## Estructura del proyecto

```
app/src/main/java/com/batallagroup/myco/
├── core/
│   ├── crypto/         CryptoManager — cifrado, firma, derivación de ID
│   └── utils/          Extensions, PreferenceManager
├── data/
│   ├── ble/            BleManager, BleAdvertiser, BleScanner,
│   │                   GattServer, MeshPacketProcessor
│   └── local/
│       ├── dao/        MessageDao, ContactDao, TransitMessageDao
│       ├── database/   MycoDatabase (Room)
│       ├── entity/     MessageEntity, ContactEntity, TransitMessageEntity
│       └── repository/ Implementaciones de repositorios
├── di/                 Módulos Hilt (AppModule, DatabaseModule, RepositoryModule)
├── domain/
│   ├── model/          Message, Contact, MycoNode, MycoPacket, MessageStatus
│   ├── repository/     Interfaces de repositorio
│   └── usecase/        Casos de uso de la aplicación
├── presentation/
│   ├── splash/         SplashScreen + logo Canvas
│   ├── onboarding/     4 pantallas de introducción + aviso legal
│   ├── identity/       Generación de identidad criptográfica
│   ├── chats/          Lista de conversaciones
│   ├── chat/           Pantalla de chat con indicadores de estado
│   ├── contacts/       Gestión de contactos
│   ├── network/        Visualización de la red mesh
│   ├── settings/       Ajustes del nodo
│   ├── qr/             Generar y escanear QR de contactos
│   ├── navigation/     NavGraph + Screen sealed class
│   └── theme/          Colores, tipografía, shapes oscuros
└── service/
    ├── MycoBleService  ForegroundService que mantiene BLE activo
    └── BootReceiver    Reinicia el servicio tras reboot
```

---

## Permisos requeridos

| Permiso | Motivo |
|---|---|
| `BLUETOOTH_SCAN` | Detectar nodos Myco cercanos |
| `BLUETOOTH_CONNECT` | Conectar vía GATT para intercambiar mensajes |
| `BLUETOOTH_ADVERTISE` | Anunciar presencia en la red |
| `FOREGROUND_SERVICE` | Mantener la red activa en segundo plano |
| `RECEIVE_BOOT_COMPLETED` | Reiniciar el servicio tras reboot |
| `CAMERA` | Escanear QR de contactos |

---

## Roadmap

- [ ] **v1.1** — Migrar a Curve25519/XChaCha20 con Lazysodium
- [ ] **v1.1** — CameraX + ML Kit para escáner QR real
- [ ] **v1.2** — Confirmación de entrega (ACK packets)
- [ ] **v1.2** — Fuente Urbanist (agregar TTF a `res/font/`)
- [ ] **v1.3** — Mensajes de grupo
- [ ] **v2.0** — iOS (Swift + CoreBluetooth)

---

## Licencia

MIT — Batalla Group, 2024
>>>>>>> 43808a4 (feat: initial commit — Myco v1.0.0)
