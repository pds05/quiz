INSERT INTO actions (COMMAND, DESCRIPTION)
VALUES ('w', 'персональная отправка сообщения'),
       ('kick', 'отключение пользователя из чата'),
       ('auth', 'аутентификация пользователя'),
       ('register', 'создание нового пользователя'),
       ('ban', ',блокировка пользователя'),
       ('add_user_role', 'добавление роли пользователю'),
       ('del_user_role', 'удаление роли пользователя'),
       ('exit', 'выход из чата'),
       ('activate', 'активирование пользователя'),
       ('quizes', 'отобразить список викторин'),
       ('join', 'подключиться к викторине'),
       ('help', 'справка с описанием команд'),
       ('upload', 'загрузка файла викторины')
;

INSERT INTO user_roles (AUTH_ROLE, DESCRIPTION, PRIORITY)
VALUES ('admin', 'полный доступ', 0),
       ('manager', 'расширенный доступ', 1),
       ('user', 'стандартный доступ', 2);
INSERT INTO user_roles_actions_rel (USER_ROLE_ID, ACTION_ID)
VALUES (3, 1),
       (2, 2),
       (3, 3),
       (3, 4),
       (2, 5),
       (1, 6),
       (1, 7),
       (3, 8),
       (2, 9),
       (3, 10),
       (3, 11),
       (3, 12),
       (3, 13);

INSERT INTO users (USERNAME, LOGIN, PASSWORD, IS_ACTIVE)
VALUES ('admin', 'admin','12345678', 1),
       ('manager', 'manager','12345678', 1),
       ('user1', 'u1login','12345678', 1);
;
INSERT INTO users_user_roles_rel (USER_ID, USER_ROLE_ID)
VALUES (1, 1),
       (2, 2),
       (3, 3)
;