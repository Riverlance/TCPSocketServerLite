package com.tcpsocketserverlite;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;

public class ProtocolParser implements Runnable {
    // Needed stuffs
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;
    // Allows to use UI within this background task
    // This is needed because this class is executed in another thread that is different of the main UI thread
    Handler handler;

    public ProtocolParser(Context context) {
        // Needed stuffs
        sp = context.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);
        spe = sp.edit();
        handler = new Handler();
    }

    @Override
    public void run() {
        // Params to listen a client
        final int port = sp.getInt("port", MainActivity.DEFAULT_PORT);

        try {
            // Socket connection
            ServerSocket serverSocket = new ServerSocket(port);

            while (true) {
                // Stream
                Socket socket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                // Basic data
                short opcode = dataInputStream.readShort(); // Opcode Client to Server
                final String username = dataInputStream.readUTF(); // Username
                final String clientIP = dataInputStream.readUTF(); // Client IPv4

                // Login user for any opcode received
                User user;
                long lastActionTime = System.currentTimeMillis();
                if (!MainActivity.mainActivity.usersMap.containsKey(username)) {
                    // Registering user into server data, without overwriting it
                    user = new User(username, clientIP, lastActionTime);
                    MainActivity.usersMap.put(username, user);
                } else {
                    // Get user
                    user = MainActivity.mainActivity.usersMap.get(username);
                }
                if (user == null) {
                    return; // Never happens
                }

                if (opcode == MainActivity.OPCODE_CTS_SENDMESSAGE) {
                    String param = dataInputStream.readUTF(); // param or targetUsername
                    String message = dataInputStream.readUTF(); // message

                    // Other
                    final Calendar calendar = Calendar.getInstance(); // Last action time
                    calendar.setTimeInMillis(lastActionTime);
                    final int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    final int minute = calendar.get(Calendar.MINUTE);
                    final int day = calendar.get(Calendar.DAY_OF_MONTH);
                    final int month = calendar.get(Calendar.MONTH);
                    final int year = calendar.get(Calendar.YEAR);

                    message = String.format("%s:%s/~%s : %s %02d:%02d-%02d/%02d/%04d", clientIP, port, username, message, hour, minute, day, month, year);

                    if (param.equals("-all")) {
                        ProtocolSender protocolSender = new ProtocolSender();
                        protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_SENDMESSAGE), message);

                    } else {
                        String targetUsername = param;
                        User targetUser = MainActivity.mainActivity.usersMap.get(targetUsername);
                        if (targetUser != null) {
                            // Send message to a single user
                            ProtocolSender protocolSender = new ProtocolSender(targetUser);
                            protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_SENDMESSAGE), message);

                        } else {
                            // Send error message
                            ProtocolSender protocolSender = new ProtocolSender(user);
                            protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_TOAST), String.format("Usuário '%s' não existe.", targetUsername));
                        }
                    }

                } else if (opcode == MainActivity.OPCODE_CTS_SELFDISCONNECT) {

                } else if (opcode == MainActivity.OPCODE_CTS_VIEWUSERS) {

                } else if (opcode == MainActivity.OPCODE_CTS_RENAMESELF) {

                }

                /*
                if (opcode == MainActivity.OPCODE_CTS_SELFCONNECT) {
                    final String clientIP = dataInputStream.readUTF(); // Client IPv4

                    // Execute in UI
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.mainActivity.login(username, clientIP);
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_CTS_SELFDISCONNECT) {
                    // Execute in UI
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.mainActivity.logout(username);
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_CTS_UPDATEDUSERSLIST) {
                    // Execute in UI
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            User user = MainActivity.mainActivity.usersMap.get(username);
                            if (user != null) {
                                ProtocolSender protocolSender = new ProtocolSender(user);
                                protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_UPDATEDUSERSLIST));
                            }
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_CTS_SENDMESSAGE) {
                    System.out.println("recebeu");
                    final User user = MainActivity.mainActivity.usersMap.get(username);
                    if (user == null) {
                        System.out.println("1");
                        return;
                    }
                    System.out.println("2");

                    final String targetUsername = dataInputStream.readUTF(); // Target username (or if is global chat)
                    String _message = dataInputStream.readUTF(); // Message

                    final boolean isGlobalChat = targetUsername.equals("[GLOBAL_CHAT]");
                    final User targetUser = MainActivity.mainActivity.usersMap.get(targetUsername);
                    if (!isGlobalChat && targetUser == null || isGlobalChat && MainActivity.mainActivity.usersMap.size() < 2) {
                        System.out.println("3");
                        return;
                    }
                    System.out.println("4");

                    final Calendar calendar = Calendar.getInstance(); // Last action time
                    calendar.setTimeInMillis(user.lastActionTime);
                    final int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    final int minute = calendar.get(Calendar.MINUTE);
                    final int day = calendar.get(Calendar.DAY_OF_MONTH);
                    final int month = calendar.get(Calendar.MONTH);
                    final int year = calendar.get(Calendar.YEAR);
                    final String message = String.format("%s:%s/~%s : %s %02d:%02d-%02d/%02d/%04d", user.ip, port, username, _message, hour, minute, day, month, year);

                    // Execute in UI
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ProtocolSender protocolSender;
                            System.out.println("5");
                            if (isGlobalChat) {
                                System.out.println("6");
                                protocolSender = new ProtocolSender(targetUsername);
                            } else {
                                System.out.println("7");
                                protocolSender = new ProtocolSender(targetUser);
                            }
                            System.out.println("executou protocol");
                            protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_SENDMESSAGE), username, targetUsername, message);
                        }
                    });
                }
                */
            }

            // Never happens because Server is always listening the Client
            // dataInputStream.close();
            // serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
