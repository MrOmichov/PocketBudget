# Pocket Budget

Трекер расходов на Java и Spring Boot: регистрация, личный список расходов, фильтры и статистика. Данные хранятся в PostgreSQL. Каждый пользователь работает только со своими записями.

Backend — Spring MVC, Spring Data JPA/Hibernate, Spring Security и Flyway. Интерфейс — HTML, CSS и JavaScript, которые раздаёт то же приложение.

## Возможности

- Регистрация, вход и выход через серверную сессию.
- Создание, просмотр и удаление своих расходов.
- Общие категории food, transport и other.
- Совместные фильтры from, to и categoryId; включительные границы дат.
- Пагинация, сортировка по spentAt DESC и id DESC.
- Общая сумма, количество и разбивка по категориям для всей подходящей выборки.
- Проверка владельца при обращении по ID и CSRF-защита.

Суммы в БД и API — целые копейки. Интерфейс принимает рубли и переводит их в копейки без вычислений с плавающей точкой.

## Запуск

Нужны JDK 21 или новее и PostgreSQL. Проверялось с PostgreSQL 18.6. Maven Wrapper включён в репозиторий; при первой сборке нужен доступ к загрузке Maven и зависимостей.

Создайте пустые БД. Пример для psql под администратором:

~~~sql
CREATE ROLE pocket_budget LOGIN;
\password pocket_budget
CREATE DATABASE pocket_budget OWNER pocket_budget;
CREATE DATABASE pocket_budget_test OWNER pocket_budget;
~~~

Если роль и БД уже существуют, повторять создание не нужно. Пароль задаётся интерактивно и не хранится в репозитории.

В PowerShell из корня проекта:

~~~powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/pocket_budget'
$env:SPRING_DATASOURCE_USERNAME = 'pocket_budget'
$secret = Read-Host 'Пароль PostgreSQL' -AsSecureString
$env:SPRING_DATASOURCE_PASSWORD = [System.Net.NetworkCredential]::new('', $secret).Password
.\mvnw.cmd spring-boot:run
~~~

Откройте [localhost:8080](http://localhost:8080/), зарегистрируйтесь и войдите. Готовых активных аккаунтов нет.
В Linux/macOS задайте те же переменные окружения и выполните ./mvnw spring-boot:run.

### Миграции

На пустой БД Flyway сам выполняет V1–V5. Таблицы вручную создавать не нужно. Hibernate работает в режиме validate: проверяет схему, а не создаёт её.

Для старой непустой БД без истории Flyway сначала нужны резервная копия и сравнение её структуры с миграциями. Baseline отмечает принятую версию, но не выполняет SQL этой версии. Не включайте baseline-on-migrate постоянно и не выбирайте версию наугад.

[Схема БД и связи](docs/database.md) · [Справочный SQL-снимок](docs/schema.sql)

## API

Все API, кроме health, получения CSRF и регистрации, требуют входа.

| Метод | Путь | Назначение |
|---|---|---|
| GET | /api/health | Статус приложения, без проверки подключения к БД |
| GET | /api/auth/csrf | Токен и имя CSRF-заголовка |
| POST | /api/auth/register | Регистрация: 201; занятое имя: 409 |
| POST | /login | Вход: поля формы username и password |
| POST | /logout | Завершение сессии |
| GET | /api/auth/me | ID и имя текущего пользователя |
| GET | /api/categories | Общий список категорий |
| POST | /api/expenses | Создать расход: 201 и Location |
| GET | /api/expenses | Страница своих расходов |
| GET | /api/expenses/{id} | Свой расход; чужой или отсутствующий: 404 |
| DELETE | /api/expenses/{id} | Удалить свой расход: 204 |
| GET | /api/expenses/summary | Общий итог и категории, без пагинации |

Регистрация принимает username (3–50 символов: a–z, цифры, _) и password (8–64 символа). Пароль сохраняется только в виде хеша, ответ содержит id и username. После регистрации нужно войти.

Пример POST расхода:

~~~json
{
  "amount": 12550,
  "categoryId": 2,
  "spentAt": "2026-11-08"
}
~~~

amount — от 1 до 9223372036854775807 копеек. categoryId берётся из списка категорий: ID зависит от БД. Владелец назначается из principal, а не из запроса.

Пример списка:

~~~
/api/expenses?from=2026-11-01&to=2026-11-30&categoryId=2&page=0&size=20
~~~

Фильтры необязательны и работают совместно. Неверная дата, from > to и неположительный categoryId дают 400. Неизвестная положительная категория возвращает пустой результат.
page начинается с 0, size — 1–100; по умолчанию 0 и 20.
Ответ: items, page, size, totalElements, totalPages.

Статистика принимает те же фильтры и возвращает totalAmount, expenseCount, categories. В категории: categoryId, category, totalAmount, expenseCount. Пустой результат — нулевые итоги и пустой список. Сумма может превышать диапазон long.

### Сессия и CSRF

Браузер хранит cookie JSESSIONID. Для POST/DELETE клиент получает /api/auth/csrf и передаёт токен указанным заголовком с той же сессией. После входа и выхода нужно получить свежий токен. Интерфейс делает это сам.

Вход использует стандартный form login Spring Security с перенаправлениями. Запрос к защищённому API без входа получает 401; отсутствие или неверный CSRF в изменяющем запросе может дать 403 раньше проверки входа.

## Тесты

Нужна отдельная настоящая PostgreSQL с именем pocket_budget_test. Flyway создаёт таблицы на пустой тестовой БД.

~~~powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/pocket_budget_test'
# Username и password — как при запуске
.\mvnw.cmd test
~~~

Тесты меняют данные и проверяют имя БД перед такими операциями. Не используйте рабочую БД для тестов. После тестов в том же терминале верните URL приложения на pocket_budget.

Проверяются валидация, ограничения БД, регистрация и настоящий вход, CSRF, доступ к чужим ID, фильтры, пагинация и статистика, включая сумму больше Long.MAX_VALUE. Последний подтверждённый запуск пользователя — 125 тестов.
