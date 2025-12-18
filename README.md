# Nexus Visual

## License / Activation (Variant A)

### Продакшен (24/7) через Vercel

Локальный `tools/lic-server` подходит только для теста на твоём ПК. Чтобы люди могли активироваться когда твой ПК выключен — нужен хостинг. Самый простой бесплатный вариант: **Vercel + Vercel Postgres**.

#### 1) Залить проект на GitHub

- Создай новый репозиторий на GitHub.
- Запушь этот проект.

#### 2) Создать проект в Vercel

- Vercel → **New Project** → импортируй репозиторий с GitHub → Deploy.

#### 3) Подключить базу

- Vercel → **Storage** → **Postgres** → Create.
- После создания Vercel добавит env-переменные для подключения к Postgres в проект.

Примечание: если у тебя в Storage отображается только **Prisma Postgres**, это ок — сервер использует `POSTGRES_PRISMA_URL`/`POSTGRES_URL_NON_POOLING` автоматически.

#### 4) Сгенерировать RSA ключи (один раз)

Локально на Windows:

```powershell
cd tools/vercel-lic
npm install
node .\keygen.js
```

- `NV_PRIVATE_KEY_PEM` (PEM) вставь в Vercel → Project → **Settings → Environment Variables**.
- `PUBLIC_KEY_X509_BASE64` вставь в мод в константу `PUBLIC_KEY_X509_BASE64`.

#### 5) Добавить admin-token

В Vercel → Project → **Settings → Environment Variables**:

- `NV_ADMIN_TOKEN` = любой длинный секрет (например 32+ символа).

#### 6) Какой URL указывать в моде

Vercel API эндпоинты лежат в `/api/*`, поэтому базовый URL для мода:

```
https://<твой-проект>.vercel.app/api
```

Чтобы не пересобирать мод под новый URL, можно создать файл:

```
<minecraft runDirectory>/simplevisuals/license_server.txt
```

и положить туда строку (одна строка):

```
https://<твой-проект>.vercel.app/api
```

Проверка: открой в браузере

```
https://<твой-проект>.vercel.app/api/health
```

должно вернуть `{ "ok": true }`.

#### 7) Генерация ключей активации (коды)

Создание кода делается через защищённый endpoint:

- `POST https://<проект>.vercel.app/api/admin_create_code`
- Header: `Authorization: Bearer <NV_ADMIN_TOKEN>`

Скрипт для Windows:

```powershell
cd tools/vercel-lic
./create_code.ps1 -BaseUrl "https://<проект>.vercel.app/api" -AdminToken "<NV_ADMIN_TOKEN>"
```

Он выведет новый код формата `NV-XXXX-XXXX-XXXX`.

### Локальный тест (на своём ПК)
# Stable

- Коммиты небольшие делайте (что-то одно сделали и коммитнули, а не переписали пол проекта и коммитнули, коммиты маленькие должны быть)

## КАК КОММИТИТЬ???
### Начало
- качаешь GitHub CLI (https://cli.github.com/)
- устанавливаешь
- перезагрузи комп
- **перейди** в https://github.com/settings/tokens
- нажми “Generate new token (classic)”
- **выбери права**:
- repo (полный доступ к приватным репозиториям)
- workflow (если нужны GitHub Actions)
- скопируй токен (после закрытия страницы он не будет показан снова).
- `gh auth login`
- **выбери**:
- GitHub.com
- HTTPS
- Yes, authenticate with your GitHub credentials
### Сам коммит и пуш
- `git add <файл или папка, в которой изменения сделали>`
- `git commit -m "<комментарий к коммиту>"`

## Активация (вариант A)

При запуске мод покажет экран активации, если нет валидной лицензии.

### Локальный тест (Python lic-server)

1) Установка/запуск сервера:
- Открой папку `tools/lic-server`
- `py -m venv .venv`
- `./.venv/Scripts/python -m pip install -r requirements.txt`
- `./.venv/Scripts/python lic_server.py` (выведет public key и 1 тестовый код)
- `./.venv/Scripts/python -m uvicorn lic_server:APP --host 127.0.0.1 --port 8787`

2) Запусти игру, вставь тестовый код на экране активации и нажми «Активировать».

