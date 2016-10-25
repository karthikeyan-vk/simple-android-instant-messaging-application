package onion.chat;

        import android.os.Bundle;
        import android.app.Activity;
        import android.speech.tts.TextToSpeech;
        import android.widget.Button;
        import android.content.Intent;
        import android.view.View;

        import android.widget.EditText;

        import android.widget.Toast;

        import android.content.pm.PackageManager;
        import android.content.pm.ResolveInfo;

        import android.speech.RecognizerIntent;

        import java.util.ArrayList;
        import java.util.List;
        import java.util.Locale;

public class LogginActivity extends Activity {


    TextToSpeech t1;
    Database db;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        db=Database.getInstance(this);
        if(db.getPassword()=="") {
        db.setPassword("password");
        }
        setContentView(R.layout.activity_loggin);
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });
        Button login = (Button) findViewById(R.id.btn_login);
                    login.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String password = ((EditText) findViewById(R.id.input_password)).getText().toString();
                            String pass=db.getPassword();
                            if(password.equals(pass)){
                                inform();
                                Intent myIntent = new Intent(LogginActivity.this, MainActivity.class);
                                LogginActivity.this.startActivity(myIntent);
                            } else {
                                Toast.makeText(getApplicationContext(), "Invalid password", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        inform();

    }
    @Override
    protected void onStart() {
        super.onStart();
        listen();
    }
    @Override
    protected void onResume() {
        super.onResume();
       // listen();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
//        finish();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            //If Voice recognition is successful then it returns RESULT_OK
            if (resultCode == RESULT_OK) {
                ArrayList<String> textMatchList = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (!textMatchList.isEmpty()) {
                    String pass=db.getPassword();
                    if(textMatchList.get(0).contains(pass)){
                        inform();
                        Intent myIntent = new Intent(LogginActivity.this, MainActivity.class);
                        LogginActivity.this.startActivity(myIntent);
                    }
                    else if(textMatchList.get(0).contains("close") || textMatchList.get(0).contains("CLOSE")){
                        String toSpeak = "Closing voice recognizer";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                       // t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                    }
                    else {
                        String toSpeak = "wrong password";
                        Toast.makeText(getApplicationContext(),
                                toSpeak, Toast.LENGTH_SHORT).show();
                      //  t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                        listen();
                    }
                }
                //Result code for various error.
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
    private void inform(){
        String toSpeak="Welcome to Its Ur's Instant Messenger app ";
        Toast.makeText(getApplicationContext(),
                toSpeak, Toast.LENGTH_SHORT).show();
        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }
}