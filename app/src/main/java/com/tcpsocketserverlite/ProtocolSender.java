package com.tcpsocketserverlite;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolSender extends AsyncTask<String, Void, String> { // <Params, Progress, Result>
    // Needed stuffs
    private SharedPreferences sp;
    // Allows to use UI within this background task
    // This is needed because this class is executed in another thread that is different of the main UI thread
    Handler handler = new Handler();
    ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>(); // Thread-safe

    // To send directly for a single user
    public ProtocolSender(User... users) {
        // Needed stuffs
        sp = MainActivity.mainActivity.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);

        for (int i = 0; i < users.length; i++) {
            User user = users[i];
            this.users.put(user.username, user);
        }
        handler = new Handler();
    }

    // To send for multiple users
    public ProtocolSender() {
        // Needed stuffs
        sp = MainActivity.mainActivity.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);
        users = MainActivity.usersMap;
        handler = new Handler();
    }

    @Override
    protected String doInBackground(String... strings) {
        // Params to connect to client(s)
        short opcode = Short.parseShort(strings[0]);
        final int port = sp.getInt("port", MainActivity.DEFAULT_PORT);

        for (ConcurrentHashMap.Entry<String, User> targetEntry : users.entrySet()) {
            final String targetKey = targetEntry.getKey();
            final User targetUser = targetEntry.getValue();

            try {
                // Socket connection and stream
                Socket socket = new Socket(targetUser.ip, port);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                // Basic data
                dataOutputStream.writeShort(opcode);
                dataOutputStream.writeUTF(targetUser.username);

                if (opcode == MainActivity.OPCODE_STC_SENDMESSAGE) {
                    String message = strings[1];
                    dataOutputStream.writeUTF(message);

                } else if (opcode == MainActivity.OPCODE_STC_SELFDISCONNECT) {
                    MainActivity.usersMap.remove(targetKey);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.mainActivity.updateOnlineUsersSize();
                        }
                    });

                } else if (opcode == MainActivity.OPCODE_STC_VIEWUSERS) {
                    String usersOnlineStr = "Usuários online:";
                    if (MainActivity.usersMap.size() > 0) {
                        for (ConcurrentHashMap.Entry<String, User> entry : MainActivity.usersMap.entrySet()) {
                            // String key = entry.getKey();
                            User user = entry.getValue();
                            usersOnlineStr += String.format("\n%s", user.username);
                        }
                    } else {
                        usersOnlineStr += "\nNão há usuários online.";
                    }
                    dataOutputStream.writeUTF(usersOnlineStr);

                } else if (opcode == MainActivity.OPCODE_STC_RENAMESELF) {
                    String newUsername = strings[1];
                    dataOutputStream.writeUTF(newUsername);

                } else if (opcode == MainActivity.OPCODE_STC_TOAST) {
                    String message = strings[1];
                    dataOutputStream.writeUTF(message);
                }

                // Close stream and socket connection
                dataOutputStream.close();
                socket.close();

            } catch (ConnectException e) {
                // Log
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.mainActivity.log(String.format("Cliente não encontrado ou ocupado.\nIP: %s (%d)\nUser: %s", targetUser.ip, port, targetUser.username));
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
