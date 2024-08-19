package ru.otus.java.basic.project.server.logic.operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.java.basic.project.server.ClientHandler;
import ru.otus.java.basic.project.server.Server;
import ru.otus.java.basic.project.server.logic.LogicProcessor;
import ru.otus.java.basic.project.server.logic.quiz.QuizLogic;
import ru.otus.java.basic.project.server.providers.AuthException;
import ru.otus.java.basic.project.server.providers.db.jdbc.UserDAO;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.Action;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.User;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.UserRole;

import java.util.List;

public class OperationLogic {
    public static final Logger logger = LoggerFactory.getLogger(OperationLogic.class.getName());

    public interface AuthProcessor {
        boolean process(Server server, ClientHandler clientHandler, String inputMessage);
    }

    public enum Authentication implements AuthProcessor {
        auth {
            @Override
            public boolean process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ");
                if (elements.length != 3) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: /auth login password");
                    return false;
                }
                try {
                    server.getUserService().authenticate(clientHandler, elements[1], elements[2]);
                    return true;
                } catch (AuthException ae) {
                    clientHandler.sendMessage("/auth-nok: " + ae.getMessage());
                }
                return false;
            }
        },

        register {
            @Override
            public boolean process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ");
                if (elements.length != 6) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: /register login password username email phoneNumber");
                    return false;
                }
                try {
                    server.getUserService().registration(clientHandler, elements[1], elements[2], elements[3], elements[4], elements[5]);
                    clientHandler.sendMessage("/register-ok: создана учетная запись " + clientHandler.getUser().getUsername());
                    return true;
                } catch (AuthException ae) {
                    clientHandler.sendMessage("/register-nok: " + ae.getMessage());
                }
                return false;
            }
        }
    }

    public enum Operations implements LogicProcessor {
        kick {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ");
                if (elements.length != 2) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: /kick username");
                    return;
                }
                String targetUsername = elements[1];
                if (server.kickUsername(targetUsername)) {
                    logger.info("Пользователь {} отключил из чата пользователя {}", clientHandler.getUser().getUsername(), targetUsername);
                    clientHandler.sendMessage("/kick-ok пользователь " + targetUsername + " отключен от чата");
                } else {
                    clientHandler.sendMessage("Пользователь " + targetUsername + " отсутствует в чате");
                }
            }
        },

        add_user_role {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ");
                if (elements.length != 3) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: '/add_user_role username userRole'");
                    return;
                }
                UserService userService = server.getUserService();
                String username = elements[1];
                User user = userService.getUser(username, false);
                if (user == null) {
                    clientHandler.sendMessage("/" + name() + "-nok пользователь " + elements[1] + " не обнаружен");
                    return;
                }
                String roleName = elements[2];
                UserRole userRole = userService.getUserRole(roleName);
                if (userRole == null) {
                    clientHandler.sendMessage("/" + name() + "-nok роль " + roleName + " не обнаружена");
                    return;
                }
                userService.addUserRoleToUser(user, userRole);
                clientHandler.sendMessage("/" + name() + "-ok " + user.getUsername() + " roles = " + user.getUserRoles());
            }
        },

        del_user_role {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ");
                if (elements.length != 3) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: '/del_user_role username userRole'");
                    return;
                }
                String username = elements[1];
                UserService userService = server.getUserService();
                User user = userService.getUser(username, false);
                if (user == null) {
                    clientHandler.sendMessage("/" + name() + "-nok пользователь " + username + " не обнаружен");
                    return;
                }
                String roleName = elements[2];
                UserRole userRole = userService.getUserRole(roleName);
                if (userRole == null) {
                    clientHandler.sendMessage("/" + name() +"-nok роль " + roleName + " не обнаружена");
                    return;
                }
                userService.removeUserRoleFromUser(user, userRole);
                clientHandler.sendMessage("/" + name() + "-ok " + user.getUsername() + " roles = " + user.getUserRoles());
            }
        },

        ban {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ");
                if (elements.length != 2) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: /ban username");
                    return;
                }
                String targetUsername = elements[1];
                UserService userService = server.getUserService();
                User user = userService.getUser(targetUsername, false);
                if (user == null) {
                    clientHandler.sendMessage("/ban-nok пользователь " + targetUsername + " не обнаружен");
                    return;
                }
                ((UserDAO) userService).deactivateUser(user);
                server.kickUsername(targetUsername);
                clientHandler.sendMessage("/ban-ok " + user.getUsername());
                logger.info("Пользователь {} заблокировал пользователя {}", clientHandler.getUser().getUsername(), targetUsername);
            }
        },

        activate {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ");
                if (elements.length != 2) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: /activate username");
                    return;
                }
                UserService userService = server.getUserService();
                String username = elements[1];
                User user = userService.getUser(username, false);
                if (user == null) {
                    clientHandler.sendMessage("/activate-nok пользователь " + username + " не обнаружен");
                    return;
                }
                ((UserDAO) userService).activateUser(user);
                clientHandler.sendMessage("/activate-ok " + user.getUsername());
            }
        },

        w {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ");
                if (elements.length != 3) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: /w username message");
                    return;
                }
                ClientHandler targetClient = server.getClientHandler(elements[1]);
                if (targetClient != null) {
                    if (!server.getQuizLogic().isClientQuizBusy(targetClient)) {
                        targetClient.sendMessage(clientHandler.getUsername() + ": " + elements[2]);
                        clientHandler.sendMessage("/w-ok");
                    } else {
                        clientHandler.sendMessage("/w-nok пользователь занят");
                    }
                } else {
                    clientHandler.sendMessage("/w-nok пользователь отсутсвует");
                }
            }
        },

        upload {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                QuizLogic.Operations.upload.process(server, clientHandler, inputMessage);
            }
        },

        quizes {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                QuizLogic.Operations.quizes.process(server, clientHandler, inputMessage);
            }
        },

        join {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                QuizLogic.Operations.join.process(server, clientHandler, inputMessage);
            }
        },

        shutdown {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                logger.info("Пользователь {} инициировал остановку сервера", clientHandler.getUser().getUsername());
                server.shutdown();
            }
        },

        help {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                List<Action> actions = server.getUserService().getUserActions(clientHandler.getUser());
                StringBuilder builder = new StringBuilder("Справка команд:\r\n");
                for (Action action : actions) {
                    builder.append("/")
                            .append(action.getCommand())
                            .append(" - ")
                            .append(action.getDescription())
                            .append("\r\n");
                }
                clientHandler.sendMessage(builder.toString());
            }
        }
    }
}
