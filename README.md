# Система управления банковскими картами

Backend-приложение на Java (Spring Boot) для управления банковскими картами:
- Создание и управление картами
- Просмотр карт (админ – всех, пользователь – своих)
- Переводы между своими картами
- Шифрование номеров карт, маскирование, ролевой доступ (ADMIN / USER)

 [Полное ТЗ можно посмотреть здесь](docs/TECHNICAL_SPECIFICATION.md)

## Стек технологий
- Java 21
- Spring Boot 3.4.5
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL 15
- Liquibase (миграции)
- Lombok
- Swagger (OpenAPI 3)
- Docker / Docker Compose
- Maven
- JUnit 5 + Mockito

## Запуск приложения

### Требования
- Docker & Docker Compose (рекомендуемый способ)
- Или Java 21 + Maven + PostgreSQL (локальный запуск)

### Запуск через Docker Compose

# Собрать и запустить контейнеры
```
docker-compose up --build -d
```
# Просмотр логов
```
docker-compose logs -f app
```
Приложение станет доступно по адресу: http://localhost:8080

## Локальный запуск (без Docker)
```
mvn clean spring-boot:run
```

## Аутентификация и роли

    JWT – токен передаётся в заголовке: Authorization: Bearer <token>

    Роли:

        ADMIN – полное управление картами и пользователями

        USER – работа только со своими картами

Тестовые учётные данные (создаются Liquibase)

admin@example.com	пароль: admin123  роль: ADMIN

user@example.com	пароль: test	 роль: USER


## Документация

[OpenAPI (yaml)](docs/openapi.yaml)

После запуска приложения доступны:

[Swagger UI](http://localhost:8080/swagger-ui.html)

[OpenAPI спецификация](http://localhost:8080/v3/api-docs)

## Основные эндпоинты

### Аутентификация
POST /api/auth/register — регистрация пользователя

POST /api/auth/login — вход в систему, получение JWT токена

### Администратор (роль ADMIN)
POST /api/admin/cards — создать карту

GET /api/admin/cards — список всех карт (фильтрация + пагинация)

PUT /api/admin/cards/{id}/status — изменить статус карты

DELETE /api/admin/cards/{id} — удалить карту

POST /api/admin/users — создать пользователя

GET /api/admin/users — список всех пользователей

DELETE /api/admin/users/{id} — удалить пользователя

PATCH /api/admin/users/{id}/enable — включить или отключить пользователя

### Пользователь (роль USER)
GET /api/user/cards — мои карты (пагинация, фильтр по статусу)

POST /api/user/cards/{id}/block — запрос на блокировку своей карты

GET /api/user/cards/{id}/balance — получить баланс карты

POST /api/user/transfer — перевод между своими картами

Фильтрация и пагинация поддерживаются через query-параметры:
page, size, sortBy, direction, status (для карт).

## Тестирование
### Юнит-тесты
```
mvn clean test
```
Покрыты сервисы: CardService, TransferService, UserService, утилиты шифрования и маскирования.

## Структура проекта (основные модули)

```text
src/main/java/com/example/bankcards/
├── config          – конфигурации (Security, Swagger)
├── controller      – REST контроллеры (Auth, Admin, User)
├── dto             – объекты передачи данных
├── entity          – JPA сущности (User, Card, Role)
├── exception       – глобальный обработчик ошибок и кастомные исключения
├── repository      – Spring Data JPA репозитории
├── security        – JWT фильтры, провайдер, UserDetailsService
├── service         – бизнес-логика (CardService, TransferService, UserService)
└── util            – утилиты (EncryptionUtil, MaskingUtil)
```

## Разработка и сборка

    Миграции БД – Liquibase (changelog в src/main/resources/db/migration)

    Сборка JAR: mvn clean package

    Запуск без Docker: java -jar target/bankcards-1.0.0.jar