package onion.chat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ChatActivity extends AppCompatActivity {

    ChatAdapter adapter;
    RecyclerView recycler;
    Cursor cursor;
    Database db;
    Tor tor;
    String address;
    Client client;
    TextToSpeech t1;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private EditText metTextHint;
    private String lastMessage = "No new messages";

    String myname = "", othername = "";
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;

    private Spinner msTextMatches;

    long idLastLast = -1;

    long rep = 0;
    Timer timer;

    void update() {
        Cursor oldCursor = cursor;

        myname = db.getName().trim();
        othername = db.getContactName(address).trim();

        //cursor = db.getReadableDatabase().query("messages", null, "((sender=? AND receiver=?) OR (sender=? AND receiver=?)) AND sender != '' AND receiver != ''", new String[] { tor.getID(), address, address, tor.getID() }, null, null, "time ASC");

        String a = tor.getID();
        String b = address;
        //cursor = db.getReadableDatabase().query("messages", null, "(sender=? AND receiver=?) OR (sender=? AND receiver=?)", new String[] { a, b, b, a }, null, null, "time ASC");
        //cursor = db.getReadableDatabase().rawQuery("SELECT * FROM (SELECT * FROM messages WHERE ((sender=? AND receiver=?) OR (sender=? AND receiver=?)) ORDER BY time DESC LIMIT 64) ORDER BY time ASC", new String[]{a, b, b, a});
        cursor = db.getMessages(a, b);

        cursor.moveToLast();
        long idLast = -1;

        int i = cursor.getColumnIndex("_id");
        if (i >= 0 && cursor.getCount() > 0) {
            idLast = cursor.getLong(i);
        }

        //if(oldCursor == null || cursor.getCount() != oldCursor.getCount())
        if (idLast != idLastLast) {
            idLastLast = idLast;

            if (oldCursor == null || oldCursor.getCount() == 0)
                recycler.scrollToPosition(Math.max(0, cursor.getCount() - 1));
            else
                recycler.smoothScrollToPosition(Math.max(0, cursor.getCount() - 1));

            //client.startSendPendingMessages(address);
        }

        int lastMsgIndex = cursor.getColumnIndex("content");
        if (lastMsgIndex > -1 && cursor.getCount() > 0)
            lastMessage = cursor.getString(lastMsgIndex);

        adapter.notifyDataSetChanged();

        if (oldCursor != null)
            oldCursor.close();

        findViewById(R.id.noMessages).setVisibility(cursor.getCount() > 0 ? View.GONE : View.VISIBLE);
    }

    void sendPendingAndUpdate() {
        //if(!client.isBusy()) {
        client.startSendPendingMessages(address);
        //}
        update();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);


        db = Database.getInstance(this);
        tor = Tor.getInstance(this);

        client = Client.getInstance(this);

        address = getIntent().getDataString();

        if (address.contains(":"))
            address = address.substring(address.indexOf(':') + 1);

        Log.i("ADDRESS", address);

        String name = db.getContactName(address);
        if (name.isEmpty()) {
            getSupportActionBar().setTitle(address);
        } else {
            getSupportActionBar().setTitle(name);
            getSupportActionBar().setSubtitle(address);
        }

        recycler = (RecyclerView) findViewById(R.id.recycler);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChatAdapter();
        recycler.setAdapter(adapter);

        final View send = findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sender = tor.getID();
                if (sender == null || sender.trim().equals("")) {
                    sendPendingAndUpdate();
                    return;
                }

                String message = ((EditText) findViewById(R.id.editmessage)).getText().toString();
                message = message.trim();
                if (message.equals("")) return;

                db.addPendingOutgoingMessage(sender, address, message);

                ((EditText) findViewById(R.id.editmessage)).setText("");

                sendPendingAndUpdate();

                //recycler.scrollToPosition(cursor.getCount() - 1);

                recycler.smoothScrollToPosition(Math.max(0, cursor.getCount() - 1));

                rep = 0;
            }
        });

        startService(new Intent(this, HostService.class));


        final EditText editmessage = (EditText) findViewById(R.id.editmessage);
        final float a = 0.5f;
        send.setAlpha(a);
        send.setClickable(false);
        editmessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    send.setAlpha(a);
                    send.setClickable(false);
                } else {
                    send.setAlpha(0.7f);
                    send.setClickable(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });
        listen();
    }

    private void listen() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            Toast.makeText(this, "Voice recognizer not present",
                    Toast.LENGTH_SHORT).show();
        } else {
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
                        .getPackage().getName());

                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Its Ur's");

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
            } catch (Exception e) {
                String toSpeak = "Oops your device doesn't support Voice recognition";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Server.getInstance(this).setListener(new Server.Listener() {
            @Override
            public void onChange() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        });

        Tor.getInstance(this).setListener(new Tor.Listener() {
            @Override
            public void onChange() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!client.isBusy()) {
                            sendPendingAndUpdate();
                        }
                    }
                });
            }
        });

        client.setStatusListener(new Client.StatusListener() {
            @Override
            public void onStatusChange(final boolean loading) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("LOADING", "" + loading);

                        findViewById(R.id.progressbar).setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
                        if (!loading) update();
                    }
                });
            }
        });

        sendPendingAndUpdate();


        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {

                rep++;

                if (rep > 5 && rep % 5 != 0) {
                    log("wait");
                    return;
                }


                log("update");


                if (client.isBusy()) {
                    log("abort update, client busy");
                    return;
                } else {
                    log("do update");
                    client.startSendPendingMessages(address);
                }

            }
        }, 0, 1000 * 60);

        Notifier.getInstance(this).onResumeActivity();

        db.clearIncomingMessageCount(address);

        ((TorStatusView) findViewById(R.id.torStatusView)).update();

        startService(new Intent(this, HostService.class));
    }

    void log(String s) {
        Log.i("Chat", s);
    }

    @Override
    protected void onPause() {
        db.clearIncomingMessageCount(address);
        Notifier.getInstance(this).onPauseActivity();
        timer.cancel();
        timer.purge();
        Server.getInstance(this).setListener(null);
        tor.setListener(null);
        client.setStatusListener(null);
        super.onPause();
        if (speech != null) {
            speech.destroy();
            Log.i(LOG_TAG, "destroy");
        }
    }

    private String date(String str) {
        long t = 0;
        try {
            t = Long.parseLong(str);
        } catch (Exception ex) {
            return "";
        }
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new Date(t));
    }

    class ChatHolder extends RecyclerView.ViewHolder {
        public TextView message, time, status;
        public View left, right;
        public CardView card;
        public View abort;

        public ChatHolder(View v) {
            super(v);
            message = (TextView) v.findViewById(R.id.message);
            //sender = (TextView)v.findViewById(R.id.sender);
            time = (TextView) v.findViewById(R.id.time);
            status = (TextView) v.findViewById(R.id.status);
            left = v.findViewById(R.id.left);
            right = v.findViewById(R.id.right);
            card = (CardView) v.findViewById(R.id.card);
            abort = v.findViewById(R.id.abort);
            try {
                message.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String toSpeak = message.getText().toString();
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                    }
                });
            } catch (Exception ex) {

            }

        }


    }

    class ChatAdapter extends RecyclerView.Adapter<ChatHolder> {

        @Override
        public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ChatHolder(getLayoutInflater().inflate(R.layout.item_message, parent, false));
        }

        @Override
        public void onBindViewHolder(ChatHolder holder, int position) {
            if (cursor == null) return;

            cursor.moveToFirst();
            cursor.moveToPosition(position);

            final long id = cursor.getLong(cursor.getColumnIndex("_id"));
            String content = cursor.getString(cursor.getColumnIndex("content"));
            String sender = cursor.getString(cursor.getColumnIndex("sender"));
            String time = date(cursor.getString(cursor.getColumnIndex("time")));
            boolean pending = cursor.getInt(cursor.getColumnIndex("pending")) > 0;
            boolean tx = sender.equals(tor.getID());


            if (sender.equals(tor.getID())) sender = "You";

            if (tx) {
                holder.left.setVisibility(View.VISIBLE);
                holder.right.setVisibility(View.GONE);
            } else {
                holder.left.setVisibility(View.GONE);
                holder.right.setVisibility(View.VISIBLE);
            }

            if (pending)
                //holder.card.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()));
                holder.card.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
            else
                holder.card.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()));


            String status = "";
            if (sender.equals(address)) {
                if (othername.isEmpty())
                    status = address;
                else
                    status = othername;
            } else {
                if (pending) {
                    status = getString(R.string.message_pending);
                    //status = "...";
                    //status = "Waiting...";
                } else {
                    status = getString(R.string.message_sent);
                    //status = "\u2713";
                    //status = "Sent.";
                }
            }


            int color = pending ? 0xff000000 : 0xff888888;
            holder.time.setTextColor(color);
            holder.status.setTextColor(color);


            //holder.message.setText(content);


            holder.message.setMovementMethod(LinkMovementMethod.getInstance());
            holder.message.setText(Utils.linkify(ChatActivity.this, content));


            holder.time.setText(time);


            holder.status.setText(status);


            if (pending) {
                holder.abort.setVisibility(View.VISIBLE);
                holder.abort.setClickable(true);
                holder.abort.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean ok = db.abortOutgoingMessage(id);
                        update();
                        Toast.makeText(ChatActivity.this, ok ? "Pending message aborted." : "Error: Message already sent.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                holder.abort.setVisibility(View.GONE);
                holder.abort.setClickable(false);
                holder.abort.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {

            //If Voice recognition is successful then it returns RESULT_OK
            if (resultCode == RESULT_OK) {

                ArrayList<String> textMatchList = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String[] textTyped= new String[textMatchList.size()];
                String typeText="";
                for(int i=1;i<textMatchList.size();i++){
                    textTyped[i]= textMatchList.get(i);
                    typeText += textTyped[i];
                }
                if (!textMatchList.isEmpty()) {
                    if (textMatchList.get(0).contains("read") || textMatchList.get(0).contains("READ") ||textMatchList.get(0).contains("lead") ||textMatchList.get(0).contains("reed") ) {
                        Toast.makeText(getApplicationContext(),
                                lastMessage, Toast.LENGTH_SHORT).show();
                        t1.speak(lastMessage, TextToSpeech.QUEUE_FLUSH, null);
                        listen();
                    } else if (textMatchList.get(0).contains("type") || textMatchList.get(0).contains("TYPE")|| textMatchList.get(0).contains("send")) {
                        String sender = tor.getID();
                        String message="";
                        if(typeText!="") {
                            message = typeText;
                        }
                        else{
                            message =textMatchList.get(0).replaceFirst("send ", "");
                        }
                        message = message.trim();
                        if (message.equals("")) return;
                        db.addPendingOutgoingMessage(sender, address, message);
                        sendPendingAndUpdate();
                        String toSpeak = "Message Sent";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                        listen();
                    } else if(textMatchList.get(0).contains("HELP") || textMatchList.get(0).contains("help")){
                        String toSpeak = "Voice Commands that can be used are 1 Read 2 Type";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                        listen();
                    }
                    else if(textMatchList.get(0).contains("close") || textMatchList.get(0).contains("CLOSE") ){
                        String toSpeak = "Closing Voice command";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                    }
                    else {

                        String toSpeak = "I Can't Understand";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                       // t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                        listen();
                    }


                }

            } else if (resultCode == RecognizerIntent.RESULT_AUDIO_ERROR) {

                String toSpeak = "Audio Error";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            } else if (resultCode == RecognizerIntent.RESULT_CLIENT_ERROR) {

                String toSpeak = "Client Error";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            } else if (resultCode == RecognizerIntent.RESULT_NETWORK_ERROR) {

                String toSpeak = "Network Error";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            } else if (resultCode == RecognizerIntent.RESULT_NO_MATCH) {

                String toSpeak = "No Match";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            } else if (resultCode == RecognizerIntent.RESULT_SERVER_ERROR) {

                String toSpeak = "Server Error";
                Toast.makeText(getApplicationContext(),
                        toSpeak, Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }

        }
    }

}