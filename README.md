# PiscinaMap 🏊‍♂️
**PiscinaMap** es una aplicación nativa Android para descubrir y explorar piscinas cerca de ti. Utiliza Google Maps para mostrar las ubicaciones y Google Places API para obtener información detallada de cada piscina.
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack-Compose-orange?logo=jetpackcompose)
![Material3](https://img.shields.io/badge/Material-3-purple?logo=materialdesign)
---
## ✨ Características Principales
- 🗺️ **Mapa Interactivo:** Explora un mapa con todas las piscinas cercanas a tu ubicación actual.
- 🔍 **Búsqueda de Ubicaciones:** Encuentra piscinas en cualquier lugar usando Google Places Autocomplete.
- 📍 **Búsqueda Manual de Área:** Descubre piscinas en cualquier región del mapa arrastrando y buscando.
- 🏷️ **Filtrado Visual:** Las piscinas nuevas se destacan en el mapa, y puedes marcar piscinas como ocultas.
- 📋 **Detalles Completos:** Ver fotos, valoración, reseñas, horarios y obtener direcciones a cada piscina.
- 📱 **Navegación a Detalles:** Pantalla detallada con información ampliada de cada piscina.
---
## 🛠 Stack Tecnológico
Este proyecto utiliza las tecnologías más punteras del desarrollo Android actual:
- **Lenguaje:** [Kotlin](https://kotlinlang.org/) con las últimas características del lenguaje.
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) con **Material 3**.
- **Arquitectura:** **Clean Architecture** con separación en capas (features/core) y patrón **MVVM**.
- **Inyección de Dependencias:** [Hilt](https://dagger.dev/hilt/) para una gestión limpia y testeable.
- **Librerías Core:**
  - `Google Maps SDK`: Mapas interactivos y marcadores personalizados.
  - `Google Places API`: Búsqueda de ubicaciones y detalles de lugares.
  - `Retrofit` + `OkHttp`: Comunicación con APIs REST.
  - `Coil`: Carga eficiente de imágenes.
  - `Room`: Persistencia local para el estado de piscinas ocultas.
---
## 🏗️ Estructura del Proyecto
El código está organizado siguiendo principios de arquitectura limpia:
```
app/src/main/java/com/burixer85/piscinamap/
├── features/
│   ├── home/          # Pantalla del mapa principal
│   │   ├── data/      # Repository implementation
│   │   ├── domain/    # Use cases e interfaces
│   │   └── presentation/ # ViewModel y Screen
│   ├── detail/        # Pantalla de detalles de piscina
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   └── explore/       # Funcionalidad adicional de exploración
│       ├── data/
│       ├── domain/
│       └── presentation/
├── core/
│   ├── data/          # DTOs y API interfaces
│   ├── di/           # Módulos de Hilt
│   ├── domain/       # Modelos de dominio
│   └── presentation/ # Componentes compartidos
├── navigation/        # Configuración de navegación
└── ui/theme/          # Temas y colores de Material 3
```
---
## 👤 Autor
**BuriDeveloper**
- **LinkedIn:** [Mi Perfil de LinkedIn](https://www.linkedin.com/in/david-sevillano-domínguez-a7a432244/)
---
## 📄 Licencia
Este proyecto está bajo la Licencia MIT.
