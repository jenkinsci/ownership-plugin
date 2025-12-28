# Тестирование исправлений безопасности SECURITY-2062

Этот документ описывает, как протестировать исправления уязвимостей CSRF и проверки прав доступа.

## Обзор исправлений

Исправлены две уязвимости:
- **SECURITY-2062 (1)**: CSRF и отсутствие проверки прав в `doOwnersSubmit` и `doProjectSpecificSecuritySubmit`
- **SECURITY-2062 (2)**: CSRF уязвимость в `doRestoreDefaultSpecificSecuritySubmit`

### Что было исправлено:
1. Добавлена проверка POST-метода (`req.checkMethod(StaplerRequest.POST)`) во все три метода
2. Проверка прав доступа `MANAGE_ITEMS_OWNERSHIP` уже была на месте и работает корректно

## Автоматические тесты

Создан тестовый класс `JobOwnerJobActionSecurityTest`, который проверяет:

### 1. Защита от CSRF (требование POST)
- `doOwnersSubmit_requiresPOST()` - проверяет, что GET-запросы отклоняются
- `doProjectSpecificSecuritySubmit_requiresPOST()` - проверяет, что GET-запросы отклоняются
- `doRestoreDefaultSpecificSecuritySubmit_requiresPOST()` - проверяет, что GET-запросы отклоняются

### 2. Проверка прав доступа
- `doOwnersSubmit_requiresManageOwnershipPermission()` - пользователи без `MANAGE_ITEMS_OWNERSHIP` не могут изменять владельцев
- `doProjectSpecificSecuritySubmit_requiresManageOwnershipPermission()` - пользователи без прав не могут изменять item-specific security
- `doRestoreDefaultSpecificSecuritySubmit_requiresManageOwnershipPermission()` - пользователи без прав не могут восстанавливать дефолтные настройки
- `configureUser_cannotModifyOwnership()` - пользователи с `CONFIGURE` но без `MANAGE_ITEMS_OWNERSHIP` не могут изменять ownership

### 3. Положительные тесты
- `doOwnersSubmit_allowsPOSTWithProperPermissions()` - POST-запросы с правильными правами работают

## Запуск тестов

### Запуск всех тестов безопасности:
```bash
mvn test -Dtest=JobOwnerJobActionSecurityTest
```

### Запуск конкретного теста:
```bash
mvn test -Dtest=JobOwnerJobActionSecurityTest#doOwnersSubmit_requiresPOST
```

### Запуск всех тестов проекта:
```bash
mvn test
```

## Ручное тестирование

### 1. Тестирование защиты от CSRF

#### Тест 1: Попытка GET-запроса к `ownersSubmit`
```bash
# Должен вернуть 405 Method Not Allowed
curl -X GET "http://localhost:8080/job/test-project/ownersSubmit" \
  --user admin:admin \
  --cookie-jar cookies.txt \
  --cookie cookies.txt
```

#### Тест 2: Попытка GET-запроса к `projectSpecificSecuritySubmit`
```bash
curl -X GET "http://localhost:8080/job/test-project/projectSpecificSecuritySubmit" \
  --user admin:admin \
  --cookie cookies.txt
```

#### Тест 3: Попытка GET-запроса к `restoreDefaultSpecificSecuritySubmit`
```bash
curl -X GET "http://localhost:8080/job/test-project/restoreDefaultSpecificSecuritySubmit" \
  --user admin:admin \
  --cookie cookies.txt
```

### 2. Тестирование проверки прав доступа

#### Создание пользователей для тестирования:

1. **Пользователь с только READ правами:**
   - Создайте пользователя `readonly-user`
   - Дайте ему только `Item/Read` и `Overall/Read`

2. **Пользователь с CONFIGURE но без MANAGE_ITEMS_OWNERSHIP:**
   - Создайте пользователя `configure-user`
   - Дайте ему `Item/Configure`, `Item/Read`, `Overall/Read`
   - НЕ давайте `Manage Ownership/Jobs`

#### Тест 4: Попытка изменения ownership пользователем с только READ
```bash
# Должен вернуть 403 Forbidden
curl -X POST "http://localhost:8080/job/test-project/ownersSubmit" \
  --user readonly-user:readonly-user \
  --cookie cookies.txt \
  --data "owners={\"ownershipEnabled\":true,\"primaryOwnerId\":\"readonly-user\"}" \
  --header "Content-Type: application/x-www-form-urlencoded"
```

#### Тест 5: Попытка изменения ownership пользователем с CONFIGURE
```bash
# Должен вернуть 403 Forbidden
curl -X POST "http://localhost:8080/job/test-project/ownersSubmit" \
  --user configure-user:configure-user \
  --cookie cookies.txt \
  --data "owners={\"ownershipEnabled\":true,\"primaryOwnerId\":\"configure-user\"}" \
  --header "Content-Type: application/x-www-form-urlencoded"
```

### 3. Тестирование успешных операций

#### Тест 6: Успешное изменение ownership администратором
```bash
# Должен успешно выполниться
curl -X POST "http://localhost:8080/job/test-project/ownersSubmit" \
  --user admin:admin \
  --cookie cookies.txt \
  --data "owners={\"ownershipEnabled\":true,\"primaryOwnerId\":\"new-owner\"}" \
  --header "Content-Type: application/x-www-form-urlencoded"
```

## Проверка через веб-интерфейс

1. **Создайте тестовый проект:**
   - Создайте FreeStyle проект с именем `test-project`
   - Установите владельца через "Manage Ownership"

2. **Проверьте CSRF защиту:**
   - Войдите как администратор
   - Попробуйте открыть в браузере (GET-запрос):
     - `http://localhost:8080/job/test-project/ownersSubmit`
     - `http://localhost:8080/job/test-project/projectSpecificSecuritySubmit`
     - `http://localhost:8080/job/test-project/restoreDefaultSpecificSecuritySubmit`
   - Должна быть ошибка 405 Method Not Allowed

3. **Проверьте права доступа:**
   - Войдите как пользователь с только READ правами
   - Попробуйте изменить ownership через форму "Manage Ownership"
   - Должна быть ошибка доступа

4. **Проверьте успешную операцию:**
   - Войдите как администратор
   - Измените ownership через форму "Manage Ownership"
   - Операция должна успешно выполниться

## Ожидаемые результаты

### Успешные тесты должны показать:
- ✅ GET-запросы к эндпоинтам возвращают 405 Method Not Allowed
- ✅ POST-запросы от пользователей без `MANAGE_ITEMS_OWNERSHIP` возвращают 403 Forbidden
- ✅ POST-запросы от администраторов успешно выполняются
- ✅ Ownership не изменяется при неудачных попытках

### Признаки успешного исправления:
1. Все автоматические тесты проходят
2. GET-запросы отклоняются с кодом 405
3. Пользователи без прав не могут изменять ownership
4. POST-запросы с правильными правами работают корректно

## Дополнительные проверки

### Проверка через Jenkins CLI:
```bash
# Попытка изменения через CLI должна также проверять права
java -jar jenkins-cli.jar -s http://localhost:8080 -auth readonly-user:readonly-user \
  update-job test-project < job-config.xml
# Должна быть ошибка доступа
```

### Проверка логирования:
Проверьте логи Jenkins на наличие сообщений об ошибках доступа:
```bash
tail -f $JENKINS_HOME/logs/jenkins.log | grep -i "access denied\|forbidden\|405"
```

## Отчетность

Если вы нашли проблемы при тестировании, создайте issue с:
- Версией Jenkins
- Версией плагина
- Шагами для воспроизведения
- Ожидаемым и фактическим поведением
- Логами ошибок



[ERROR] Rule 4: org.apache.maven.enforcer.rules.dependency.RequireUpperBoundDeps failed with message:
[ERROR] Failed while enforcing RequireUpperBoundDeps. The error(s) are [
[ERROR] Require upper bound dependencies error for io.jenkins.plugins:caffeine-api:2.9.2-29.v717aac953ff3 paths to dependency are:
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins:matrix-project:785.v06b_7f47b_c631
[ERROR]     +-org.jenkins-ci.plugins:script-security:1175.v4b_d517d6db_f0
[ERROR]       +-io.jenkins.plugins:caffeine-api:2.9.2-29.v717aac953ff3
[ERROR] and
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins.workflow:workflow-cps:3520.va_8fc49e2f96f
[ERROR]     +-org.jenkins-ci.plugins.workflow:workflow-support:839.v35e2736cfd5c
[ERROR]       +-io.jenkins.plugins:caffeine-api:2.9.3-65.v6a_47d0f4d1fe
[ERROR] , 
[ERROR] Require upper bound dependencies error for org.jenkins-ci.plugins:script-security:1175.v4b_d517d6db_f0 paths to dependency are:
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins:matrix-project:785.v06b_7f47b_c631
[ERROR]     +-org.jenkins-ci.plugins:script-security:1175.v4b_d517d6db_f0
[ERROR] and
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins.workflow:workflow-cps:3520.va_8fc49e2f96f
[ERROR]     +-org.jenkins-ci.plugins:script-security:1189.vb_a_b_7c8fd5fde
[ERROR] and
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins:matrix-project:785.v06b_7f47b_c631
[ERROR]     +-org.jenkins-ci.plugins:junit:1119.1121.vc43d0fc45561
[ERROR]       +-org.jenkins-ci.plugins:script-security:1158.v7c1b_73a_69a_08
[ERROR] and
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins.workflow:workflow-cps:3520.va_8fc49e2f96f
[ERROR]     +-org.jenkins-ci.plugins.workflow:workflow-support:839.v35e2736cfd5c
[ERROR]       +-org.jenkins-ci.plugins:script-security:1175.v4b_d517d6db_f0
[ERROR] ]

ERROR] Failed to execute goal org.apache.maven.plugins:maven-enforcer-plugin:3.4.1:enforce (display-info) on project ownership: 
[ERROR] Rule 4: org.apache.maven.enforcer.rules.dependency.RequireUpperBoundDeps failed with message:
[ERROR] Failed while enforcing RequireUpperBoundDeps. The error(s) are [
[ERROR] Require upper bound dependencies error for org.jenkins-ci.plugins.workflow:workflow-api:1200.v8005c684b_a_c6 paths to dependency are:
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins.workflow:workflow-cps:3520.va_8fc49e2f96f
[ERROR]     +-org.jenkins-ci.plugins.workflow:workflow-api:1200.v8005c684b_a_c6
[ERROR] and
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins:matrix-project:789.v57a_725b_63c79
[ERROR]     +-org.jenkins-ci.plugins:junit:1189.v1b_e593637fa_e
[ERROR]       +-org.jenkins-ci.plugins.workflow:workflow-api:1208.v0cc7c6e0da_9e
[ERROR] and
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins.workflow:workflow-cps:3520.va_8fc49e2f96f
[ERROR]     +-org.jenkins-ci.plugins.workflow:workflow-support:839.v35e2736cfd5c
[ERROR]       +-org.jenkins-ci.plugins.workflow:workflow-api:1164.v760c223ddb_32
[ERROR] , 
[ERROR] Require upper bound dependencies error for io.jenkins.plugins:font-awesome-api:6.1.2-1 paths to dependency are:
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins:matrix-project:789.v57a_725b_63c79
[ERROR]     +-org.jenkins-ci.plugins:junit:1189.v1b_e593637fa_e
[ERROR]       +-io.jenkins.plugins:echarts-api:5.4.0-1
[ERROR]         +-io.jenkins.plugins:font-awesome-api:6.1.2-1
[ERROR] and
[ERROR] +-com.synopsys.jenkinsci:ownership:0.13.1-SNAPSHOT
[ERROR]   +-org.jenkins-ci.plugins:matrix-project:789.v57a_725b_63c79
[ERROR]     +-org.jenkins-ci.plugins:junit:1189.v1b_e593637fa_e
[ERROR]       +-io.jenkins.plugins:bootstrap5-api:5.2.1-3
[ERROR]         +-io.jenkins.plugins:font-awesome-api:6.2.0-3
[ERROR] ]