package edu.wenla.vivihome;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends VoiceActivity {

    private static final String LOGTAG = "TALKBACK";
    private static Integer ID_PROMPT_QUERY = 0;
    private static Integer ID_PROMPT_INFO = 1;
    private long startListeningTime = 0;
    TextView t_nivel, t_accion;
    ImageView icono;
    //Variables para los ajustes
    private static final int RESULT_SETTINGS = 1;
    private int luz_techo_1;
    private int luz_techo_2;
    private int luz_lectura;
    private int luz_regulable;
    private int abrir_puerta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view);

        initSpeechInputOutput(this);
        setSpeakButton();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
/*
        switch (item.getItemId()) {

            case R.id.menu_settings:
                Intent i = new Intent(this, UserSettingsActivity.class);
                startActivity(i);
                break;
        }
*/
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    void dosisDiaria(){

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        int peso = Integer.parseInt(sharedPrefs.getString("pesoUsuario", "NULL"));

        dosis_diaria = (int)(peso*0.4);
        dosis_mañana = dosis_diaria*2/3;
        dosis_noche = dosis_diaria/3;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    ///                                  VOZ                                          ///
    /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initializes the search button and its listener. When the button is pressed, a feedback is shown to the user
     * and the recognition starts
     */

    private void setSpeakButton() {
        // gain reference to speak button
        Button speak = (Button) findViewById(R.id.speech_btn);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ask the user to speak
                try {
                    speak(getResources().getString(R.string.mensaje_inicial), "ES", ID_PROMPT_QUERY);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }
        });
    }
/*
    /**
     * Checks whether the device is connected to Internet (returns true) or not (returns false)
     * From: http://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
     */

    public boolean deviceConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }
    /**
     * Starts listening for any user input.
     * When it recognizes something, the <code>processAsrResult</code> method is invoked.
     * If there is any error, the <code>onAsrError</code> method is invoked.
     */

    private void startListening(){

        if(deviceConnectedToInternet()){
            try {

                /*Start listening, with the following default parameters:
                 * Language = English
                 * Recognition model = Free form,
                 * Number of results = 1 (we will use the best result to perform the search)
                 */
                startListeningTime = System.currentTimeMillis();
                listen(Locale.ENGLISH, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, 1); //Start listening
            } catch (Exception e) {
                this.runOnUiThread(new Runnable() {  //Toasts must be in the main thread
                    public void run() {
                        Toast.makeText(getApplicationContext(),"ASR could not be started", Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e(LOGTAG,"ASR could not be started");
                try { speak("Speech recognition could not be started", "EN", ID_PROMPT_INFO); } catch (Exception ex) { Log.e(LOGTAG, "TTS not accessible"); }

            }
        } else {

            this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
                public void run() {
                    Toast.makeText(getApplicationContext(),"Please check your Internet connection", Toast.LENGTH_SHORT).show();
                }
            });
            try { speak("Please check your Internet connection", "EN", ID_PROMPT_INFO); } catch (Exception ex) { Log.e(LOGTAG, "TTS not accessible"); }
            Log.e(LOGTAG, "Device not connected to Internet");

        }
    }

    @Override
    public void showRecordPermissionExplanation() {

    }

    @Override
    public void onRecordAudioPermissionDenied() {

    }

    @Override
    public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {
/*
        if(nBestList != null){

            t_nivel = (TextView)findViewById(R.id.nivel);
            t_accion = (TextView)findViewById(R.id.accion);
            icono = (ImageView)findViewById(R.id.icono);
            int nivel = Integer.parseInt(nBestList.get(0));
            dosisDiaria();

            t_nivel.setText("Su nivel de azúcar es "+Integer.toString(nivel));

            try {
                speak("Su nivel de azúcar es " + nBestList.get(0), "ES", ID_PROMPT_INFO);
            } catch (Exception e) {
                Log.e(LOGTAG, "TTS not accessible");
            }

            if (nivel > 145) {

                t_accion.setText("Póngase en contacto con su médico");
                icono.setImageResource(R.drawable.erojo);

                try {
                    speak(getResources().getString(R.string.mensaje_azucar_alta), "ES", ID_PROMPT_INFO);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }

            }

            else if (nivel >= 100 && nivel <= 145) {

                t_accion.setText("Tomar "+dosis_mañana+" unidades por la mañana y "+dosis_noche+ " unidades por la noche, antes de desayunar y cenar");
                icono.setImageResource(R.drawable.amari);

                try {
                    speak("Su nivel de azúcar está un poco alto. Debe tomar "+dosis_mañana+" unidades de insulina por la mañana y "+dosis_noche+ " unidades por la noche, antes de desayunar y cenar","ES", ID_PROMPT_INFO);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }

            }

            else if (nivel < 100 && nivel >= 70) {

                t_accion.setText("Tomar "+dosis_mañana+" unidades por la mañana antes de desayunar");
                icono.setImageResource(R.drawable.verde);

                try {
                    speak("Su nivel de azúcar es normal. Debe tomar "+dosis_mañana+" unidades de insulina por la mañana antes de desayunar","ES", ID_PROMPT_INFO);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }

            }

            else {

                t_accion.setText("Tomar "+dosis_mañana+" unidades y un vaso de agua con azúcar antes de desayunar");
                icono.setImageResource(R.drawable.amari);

                try {
                    speak("Su nivel de azúcar está un poco bajo. Debe tomar "+dosis_mañana+" unidades de insulina y un vaso de agua con azúcar antes de desayunar","ES", ID_PROMPT_INFO);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }

            }
        }
        */
    }

    @Override
    public void processAsrReadyForSpeech() {

    }

    @Override
    public void processAsrError(int errorCode) {
/*
        //Possible bug in Android SpeechRecognizer: NO_MATCH errors even before the the ASR
        // has even tried to recognized. We have adopted the solution proposed in:
        // http://stackoverflow.com/questions/31071650/speechrecognizer-throws-onerror-on-the-first-listening
        long duration = System.currentTimeMillis() - startListeningTime;
        if (duration < 500 && errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
            Log.e(LOGTAG, "Doesn't seem like the system tried to listen at all. duration = " + duration + "ms. Going to ignore the error");
            stopListening();
        } else {
            String errorMsg = "";
            switch (errorCode) {
                case SpeechRecognizer.ERROR_AUDIO:
                    errorMsg = "Audio recording error";
                case SpeechRecognizer.ERROR_CLIENT:
                    errorMsg = "Unknown client side error";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errorMsg = "Insufficient permissions";
                case SpeechRecognizer.ERROR_NETWORK:
                    errorMsg = "Network related error";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    errorMsg = "Network operation timed out";
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMsg = "No recognition result matched";
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    errorMsg = "RecognitionService busy";
                case SpeechRecognizer.ERROR_SERVER:
                    errorMsg = "Server sends error status";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMsg = "No speech input";
                default:
                    errorMsg = ""; //Another frequent error that is not really due to the ASR, we will ignore it
            }
            if (errorMsg != "") {
                this.runOnUiThread(new Runnable() { //Toasts must be in the main thread
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Speech recognition error", Toast.LENGTH_LONG).show();
                    }
                });

                Log.e(LOGTAG, "Error when attempting to listen: " + errorMsg);
                try {
                    speak(errorMsg, "EN", ID_PROMPT_INFO);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }
        }
        */
    }

    @Override
    public void onTTSDone(String uttId) {
        if(uttId.equals(ID_PROMPT_QUERY.toString())) {
            runOnUiThread(new Runnable() {
                public void run() {
                    startListening();
                }
            });
        }
    }

    @Override
    public void onTTSError(String uttId) {
        Log.e(LOGTAG, "TTS error");
    }

    @Override
    public void onTTSStart(String uttId) {
        Log.e(LOGTAG, "TTS starts speaking");
    }
}