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

                User _user = null;
                long lastActionTime = System.currentTimeMillis();

                boolean _getUser = true;
                String _newUsername = "";
                if (opcode == MainActivity.OPCODE_CTS_RENAMESELF) {
                    _newUsername = dataInputStream.readUTF();

                    if (username.equals("")) {
                        _user = new User(_newUsername, clientIP, lastActionTime);
                        MainActivity.usersMap.put(_newUsername, _user);
                        _getUser = false;
                    }
                }

                if (_getUser) {
                    // Login user for any opcode received (except opcode of rename)
                    if (!MainActivity.mainActivity.usersMap.containsKey(username)) {
                        // Registering user into server data, without overwriting it
                        _user = new User(username, clientIP, lastActionTime);
                        MainActivity.usersMap.put(username, _user);
                    } else {
                        // Get user
                        _user = MainActivity.mainActivity.usersMap.get(username);
                    }
                }

                // Update lastActionTime
                _user.lastActionTime = lastActionTime;

                final User user = _user;

                if (opcode == MainActivity.OPCODE_CTS_SENDMESSAGE) {
                    final String param = dataInputStream.readUTF(); // param or targetUsername
                    String _message = dataInputStream.readUTF(); // message

                    // Other
                    final Calendar calendar = Calendar.getInstance(); // Last action time
                    calendar.setTimeInMillis(lastActionTime);
                    final int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    final int minute = calendar.get(Calendar.MINUTE);
                    final int day = calendar.get(Calendar.DAY_OF_MONTH);
                    final int month = calendar.get(Calendar.MONTH);
                    final int year = calendar.get(Calendar.YEAR);

                    _message = String.format("%s:%s/~%s : %s %02d:%02d-%02d/%02d/%04d", clientIP, port, username, _message, hour, minute, day, month, year);

                    final String message = _message;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
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
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_CTS_SELFDISCONNECT) {
                    MainActivity.mainActivity.usersMap.remove(username);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Not needed because we have nothing to change on client when disconnected
                            // ProtocolSender protocolSender = new ProtocolSender(user);
                            // protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_SELFDISCONNECT));

                            // Send success message (force sending this message)
                            ProtocolSender protocolSender = new ProtocolSender(new User(username, clientIP, 0L));
                            protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_TOAST), "Desconectado com sucesso.");
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_CTS_VIEWUSERS) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ProtocolSender protocolSender = new ProtocolSender(user);
                            protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_VIEWUSERS));
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_CTS_RENAMESELF) {
                    final String newUsername = _newUsername;
                    final boolean getUser = _getUser;

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getUser) {
                                if (!MainActivity.mainActivity.usersMap.containsKey(newUsername)) {
                                    MainActivity.mainActivity.usersMap.remove(user.username);

                                    User newUser = user;
                                    newUser.username = newUsername;
                                    MainActivity.mainActivity.usersMap.put(newUsername, newUser);

                                    // Send success message
                                    ProtocolSender protocolSender = new ProtocolSender(newUser);
                                    protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_RENAMESELF), newUser.username);

                                } else {
                                    // Send error message
                                    ProtocolSender protocolSender = new ProtocolSender(user);
                                    protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_TOAST), String.format("Usuário '%s' já existe.", newUsername));
                                }

                            } else {
                                // Send success message
                                ProtocolSender protocolSender = new ProtocolSender(user);
                                protocolSender.execute(String.format("%d", MainActivity.OPCODE_STC_RENAMESELF), user.username);
                            }
                        }
                    });
                }
            }

            // Never happens because Server is always listening the Client
            // dataInputStream.close();
            // serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
