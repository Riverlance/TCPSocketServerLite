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
    String ignoredUsername = null;

    // Needed stuffs
    private SharedPreferences sp;
    // Allows to use UI within this background task
    // This is needed because this class is executed in another thread that is different of the main UI thread
    Handler handler = new Handler();
    ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>(); // Thread-safe

    // To send directly for a single user
    public ProtocolSender(User user) {
        // Needed stuffs
        sp = MainActivity.mainActivity.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);
        users.put(user.username, user);
    }

    // To send for multiple users
    public ProtocolSender(String ignoredUsername) {
        this.users = users;
        this.ignoredUsername = ignoredUsername;
        // Needed stuffs
        sp = MainActivity.mainActivity.getSharedPreferences(MainActivity.APP_NAME, Context.MODE_PRIVATE);
        users = MainActivity.usersMap;
    }

    @Override
    protected String doInBackground(String... strings) {
        // Params to connect to client(s)
        final int port = sp.getInt("port", MainActivity.DEFAULT_PORT);
        short opcode = Short.parseShort(strings[0]);

        for (ConcurrentHashMap.Entry<String, User> targetEntry : users.entrySet()) {
            String targetKey = targetEntry.getKey();
            User targetUser = targetEntry.getValue();

            // Ignore self username (skip to next)
            if (targetKey.equals(ignoredUsername)) {
                continue;
            }

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
                    // Do nothing else

                } else if (opcode == MainActivity.OPCODE_STC_VIEWUSERS) {
                    String usersOnlineStr = "Usuários online:";
                    if (MainActivity.mainActivity.usersMap.size() > 0) {
                        for (ConcurrentHashMap.Entry<String, User> entry : MainActivity.mainActivity.usersMap.entrySet()) {
                            String key = entry.getKey();
                            User user = entry.getValue();
                            usersOnlineStr += String.format("\n%s", user.username);
                        }
                    } else {
                        usersOnlineStr += "\nNão há usuários online.";
                    }
                    dataOutputStream.writeUTF(usersOnlineStr);

                } else if (opcode == MainActivity.OPCODE_STC_RENAMESELF) {
                    // To do
                }

                // Close stream and socket connection
                dataOutputStream.close();
                socket.close();

            } catch (ConnectException e) {
                // Log
                MainActivity.mainActivity.log(String.format("Cliente nao encontrado ou ocupado.\nIP: %s (%d)\nUser: %s", targetUser.ip, port, targetUser.username));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
