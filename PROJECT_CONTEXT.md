# QRScannerVault — Технический паспорт проекта

## 1. Обзор
Локальное хранилище QR и штрих-кодов с категоризацией по табам, поиском и генерацией изображений кодов.

## 2. Технологический стек
- **Язык:** Kotlin + Jetpack Compose
- **БД:** Room (Версия 3, поддержка Foreign Keys, уникальные имена категорий)
- **Камера:** CameraX + Google ML Kit (Barcode Scanning)
- **Генерация:** ZXing (MultiFormatWriter)
- **Архитектура:** MVVM (Model-View-ViewModel)

## 3. Структура файлов и ответственности
- `MainActivity.kt`: Входная точка, инициализация БД Room и ViewModelProvider.
- `MainScreen.kt`: Главный экран. Управление стейтами (камера, диалоги, табы, поиск).
- `ScannerViewModel.kt`: Бизнес-логика. Фильтрация поиска (Flow combine), операции с БД.
- `HistoryComponents.kt`: UI списков и карточек. Поддержка режима редактирования (isEditMode).
- `CameraPreview.kt`: Изолированный компонент CameraX для захвата видеопотока.
- `data/`:
    - `CategoryEntity.kt`: Таблица категорий (уникальный индекс на `name`).
    - `ScanEntity.kt`: Таблица сканов (ForeignKey к категориях, индекс на `categoryId`).
    - `ScanDao.kt`: SQL запросы (Flow-based).
    - `AppDatabase.kt`: Синглтон базы, включает `onCreate` Callback для создания таба "General".

## 4. Ключевые особенности логики
- **Дубликаты:** Защита на уровне БД (OnConflict.IGNORE) и колбэка при создании файла базы.
- **Поиск:** Реализован через `combine` потока базы и потока поисковой строки во ViewModel.
- **Обмен:** FileProvider используется для временного сохранения Bitmap в кэш и отправки `ACTION_SEND`.
- **UI:** Автоматическая поддержка Темной/Светлой темы.