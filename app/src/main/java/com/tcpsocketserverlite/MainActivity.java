package com.tcpsocketserverlite;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity {
    // Constants
    public static final String APP_NAME = "TCP Socket Server";
    public static final int DEFAULT_PORT = 7171;
    public static final int DEFAULT_TIMETOKICK = 20000; // Has a copy on client
    // Opcodes (Operation Codes)
    // CTS - Client to Server
    public static final short OPCODE_CTS_SELFCONNECT = 1; // Request
    public static final short OPCODE_CTS_SELFDISCONNECT = 2; // Request
    public static final short OPCODE_CTS_UPDATEDUSERSLIST = 3; // Request
    public static final short OPCODE_CTS_SENDMESSAGE = 4;
    // STC - Server to Client
    public static final short OPCODE_STC_SELFCONNECT = 1; // Answer
    public static final short OPCODE_STC_SELFDISCONNECT = 2; // Answer
    public static final short OPCODE_STC_UPDATEDUSERSLIST = 3; // Answer
    public static final short OPCODE_STC_SENDMESSAGE = 4;
    public static final short OPCODE_STC_FRIENDLOGGEDIN = 5; // Broadcast to friends that self logged in
    public static final short OPCODE_STC_FRIENDLOGGEDOUT = 6; // Broadcast to friends that self logged out

    // Needed stuffs
    public static MainActivity mainActivity;
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;
    public static ConcurrentHashMap<String, User> usersMap = new ConcurrentHashMap<>();
    private static Thread protocolParserThread, ticksThread;

    // Views
    public EditText ipEditText;
    public EditText portEditText;
    public EditText onlineUsersEditText;
    public TextView logTextView;
    public ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Needed stuffs
        mainActivity = this;
        sp = getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        spe = sp.edit();
        // Server listening client
        if (protocolParserThread == null) {
            // protocolParserThread = new Thread(new ProtocolParser(this));
            // protocolParserThread.start();
        }
        // Executes periodically, once per second
        if (ticksThread == null) {
            ticksThread = new Thread() {
                @Override
                public void run() {
                    while (!isInterrupted()) {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    onTick();
                                }
                            });
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            interrupt();
                        }
                    }
                }
            };
            ticksThread.start();
        }

        // Views
        ipEditText = findViewById(R.id.ipEditText);
        portEditText = findViewById(R.id.portEditText);
        onlineUsersEditText = findViewById(R.id.onlineUsersEditText);
        logTextView = findViewById(R.id.logTextView);
        scrollView = findViewById(R.id.scrollView);
        loadDefaultValues();
    }

    private void onTick() {
        // Current time
        long time = System.currentTimeMillis(); // ms
        int timeInSec = (int) (time / 1000); // sec

        // Each 5 seconds
        if (timeInSec % 5 == 0) {
            for (ConcurrentHashMap.Entry<String, User> entry : usersMap.entrySet()) {
                String key = entry.getKey();
                User user = entry.getValue();

                // If elapsed time > DEFAULT_TIMETOKICK, kick user
                if (time - user.lastActionTime > DEFAULT_TIMETOKICK) {
                    logout(key);
                }
            }
        }

        // Online users
        onlineUsersEditText.setText(String.format("%d", usersMap.size()));
    }

    public void login(String username, String clientIP) {
        /*
        User user;
        long lastActionTime = System.currentTimeMillis();

        // Signal to client of friends
        ProtocolSender protocolSender1 = new ProtocolSender(username);
        protocolSender1.execute(String.format("%d", MainActivity.OPCODE_STC_FRIENDLOGGEDIN), username);

        if (!usersMap.containsKey(username)) {
            // Registering user into server data, without overwriting it
            user = new User(username, clientIP, lastActionTime);
            MainActivity.usersMap.put(username, user);
        } else {
            // Get user
            user = usersMap.get(username);
        }
        if (user == null) {
            return; // Never happens
        }

        // Signal to client
        ProtocolSender protocolSender2 = new ProtocolSender(user);
        protocolSender2.execute(String.format("%d", OPCODE_STC_SELFCONNECT));

        // Other
        final Calendar calendar = Calendar.getInstance(); // Last action time
        calendar.setTimeInMillis(lastActionTime);
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        final int minute = calendar.get(Calendar.MINUTE);
        final int day = calendar.get(Calendar.DAY_OF_MONTH);
        final int month = calendar.get(Calendar.MONTH);
        final int year = calendar.get(Calendar.YEAR);

        final int port = sp.getInt("port", MainActivity.DEFAULT_PORT);
        //Toast.makeText(context, String.format("%s:%s/~%s : %s %02d:%02d-%02d/%02d/%04d", clientIP, port, username, message, hour, minute, day, month, year), Toast.LENGTH_LONG).show();
        //Toast.makeText(context, String.format("%s:%s/~%s : %02d:%02d-%02d/%02d/%04d", clientIP, port, username, hour, minute, day, month, year), Toast.LENGTH_LONG).show();
        //System.out.println(String.format("%s:%s/~%s : %02d:%02d-%02d/%02d/%04d", clientIP, port, username, hour, minute, day, month, year));

        // Server Log
        Toast.makeText(getApplicationContext(), String.format("%s iniciou a sessao.", username), Toast.LENGTH_SHORT).show();
        */
    }

    public void logout(String username) {
        // Get user
        User user = usersMap.get(username);
        if (user == null) {
            return;
        }

        // Remove user from map
        usersMap.remove(username);

        /*
        // Signal to client
        ProtocolSender protocolSender1 = new ProtocolSender(user);
        protocolSender1.execute(String.format("%d", OPCODE_STC_SELFDISCONNECT));

        // Signal to client of friends
        ProtocolSender protocolSender2 = new ProtocolSender(username);
        protocolSender2.execute(String.format("%d", MainActivity.OPCODE_STC_FRIENDLOGGEDOUT), username);

        // Server Log
        Toast.makeText(this, String.format("%s encerrou a sessao.", user.username), Toast.LENGTH_SHORT).show();
        */
    }

    public void onClickUpdateButton(View view) {
        String port = portEditText.getText().toString();

        // Saving data at app preferences
        spe.putInt("port", !port.equals("") ? Integer.parseInt(port) : DEFAULT_PORT);
        spe.commit();
    }

    public void loadDefaultValues() {
        ipEditText.setText(Utils.getIPAddress(true));
        portEditText.setText(String.format("%d", sp.getInt("port", DEFAULT_PORT)));
    }

    public void scrollLog(final boolean bottom) {
        // Scrolls to down
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(bottom ? ScrollView.FOCUS_DOWN : ScrollView.FOCUS_UP);
            }
        });
    }
    public void scrollLog() {
        scrollLog(true);
    }
}
