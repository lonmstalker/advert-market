# Versioning (JitPack)

JitPack поддерживает три режима версионирования:

- **Git tag**: стабильный вариант для интеграции (пример: `v0.1.0`)
- **Commit SHA**: pin на конкретный снимок кода
- **Branch-SNAPSHOT**: "живая" ветка, нестабильно (пример: `main-SNAPSHOT`)

Рекомендация для consumer-проектов: tag или commit SHA.