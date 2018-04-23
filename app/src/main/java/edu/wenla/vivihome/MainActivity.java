package edu.wenla.vivihome;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import static android.view.View.*;

public class MainActivity extends VoiceActivity implements View.OnClickListener {

    //Variables para actividad por voz
    private static final String LOGTAG = "TALKBACK";
    private static Integer ID_PROMPT_QUERY = 0;
    private static Integer ID_PROMPT_INFO = 1;
    private long startListeningTime = 0;
    private boolean primera_vez;

    //Variables para los ajustes
    private static final int RESULT_SETTINGS = 1;
    private Button opciones, consultar;
    private Switch techo, lectura, regulable, puerta;
    private TextView mensaje;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view);
        initSpeechInputOutput(this);
        setSpeakButton();
        mensaje = (TextView)findViewById(R.id.vivi_message);

        primera_vez = true;

        techo = (Switch)findViewById(R.id.switch_techo);
        lectura = (Switch)findViewById(R.id.switch_lectura);
        regulable = (Switch)findViewById(R.id.switch_regulable);
        puerta = (Switch)findViewById(R.id.switch_puerta);

        openSettings();
        checkState();
    }


    /////////////////////////////////////////////////////////////////////////////////////
    ///                                  TÁCTIL                                       ///
    /////////////////////////////////////////////////////////////////////////////////////
    @TargetApi(VERSION_CODES.M)
    public void openSettings(){

        opciones = findViewById(R.id.settings_btn);

        opciones.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){

                if(!techo.isShown()) {
                    mensaje.setVisibility(View.INVISIBLE);
                    techo.setVisibility(View.VISIBLE);
                    lectura.setVisibility(View.VISIBLE);
                    regulable.setVisibility(View.VISIBLE);
                    puerta.setVisibility(View.VISIBLE);
                }
                else {
                    techo.setVisibility(View.INVISIBLE);
                    lectura.setVisibility(View.INVISIBLE);
                    regulable.setVisibility(View.INVISIBLE);
                    puerta.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void checkState(){

        consultar = findViewById(R.id.state_btn);

        consultar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){

                if(!mensaje.isShown()) {
                    String msg_techo;
                    String msg_lectura;
                    String msg_regulable;
                    String msg_puerta;

                    if (techo.isChecked()) {
                        msg_techo = getResources().getString(R.string.mensaje_techo) + " encendidas.";
                        getUrl(0,1);
                        getUrl(1,1);
                    }

                    else {
                        msg_techo = getResources().getString(R.string.mensaje_techo) + " apagadas.";
                        getUrl(0,0);
                        getUrl(1,0);
                    }

                    if (lectura.isChecked()){
                        msg_lectura = getResources().getString(R.string.mensaje_lectura) + " encendida.";
                        getUrl(2,1);
                    }
                    else{
                        msg_lectura = getResources().getString(R.string.mensaje_lectura) + " apagada.";
                        getUrl(2,0);
                    }

                    if (regulable.isChecked()){
                        msg_regulable = getResources().getString(R.string.mensaje_regulable) + " encendida.";
                        getUrl(3,1);
                    }
                    else {
                        msg_regulable = getResources().getString(R.string.mensaje_regulable) + " apagada.";
                        getUrl(3,0);
                    }

                    if (puerta.isChecked()){
                        msg_puerta = getResources().getString(R.string.mensaje_puerta) + " abierta.";
                        getUrl(4,1);
                    }
                    else {
                        msg_puerta = getResources().getString(R.string.mensaje_puerta) + " cerrada.";
                        getUrl(4, 0);
                    }

                    if (techo.isShown()) {
                        techo.setVisibility(View.INVISIBLE);
                        lectura.setVisibility(View.INVISIBLE);
                        regulable.setVisibility(View.INVISIBLE);
                        puerta.setVisibility(View.INVISIBLE);
                    }

                    mensaje.setVisibility(View.VISIBLE);
                    mensaje.setText(msg_techo + "\n" + msg_lectura + "\n" + msg_regulable + "\n" + msg_puerta);

                    try {
                        speak(msg_techo + ". " + msg_lectura + ". " + msg_regulable + ". " + msg_puerta, "ES", ID_PROMPT_INFO);

                    } catch (Exception e) {
                        Log.e(LOGTAG, "TTS not accessible");
                    }
                }

                else
                    mensaje.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdown();
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
        ImageButton speak = (ImageButton) findViewById(R.id.speech_btn);
        speak.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Ask the user to speak
                try {
                    if(primera_vez) {
                        speak(getResources().getString(R.string.mensaje_inicial), "ES", ID_PROMPT_QUERY);
                        primera_vez = false;
                    }
                    else
                        speak("Dime", "ES", ID_PROMPT_QUERY);
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

        if(nBestList != null){

            if(nBestList.get(0).equals("modificar estado") || nBestList.get(0).equals("modificar")){

                try {
                    speak("Esta es la pantalla de control de tus dispositivos. ¿Qué quieres hacer?", "ES", ID_PROMPT_INFO);
                    techo.setVisibility(View.VISIBLE);
                    lectura.setVisibility(View.VISIBLE);
                    regulable.setVisibility(View.VISIBLE);
                    puerta.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }

            if(nBestList.get(0).equals("consultar estado") || nBestList.get(0).equals("consultar")){

                try {
                    String msg_techo;
                    String msg_lectura;
                    String msg_regulable;
                    String msg_puerta;

                    if (techo.isChecked())
                        msg_techo = getResources().getString(R.string.mensaje_techo) + " encendidas.";
                    else
                        msg_techo = getResources().getString(R.string.mensaje_techo) + " apagadas.";

                    if (lectura.isChecked())
                        msg_lectura = getResources().getString(R.string.mensaje_lectura) + " encendida.";
                    else
                        msg_lectura = getResources().getString(R.string.mensaje_lectura) + " apagada.";

                    if (regulable.isChecked())
                        msg_regulable = getResources().getString(R.string.mensaje_regulable) + " encendida.";
                    else
                        msg_regulable = getResources().getString(R.string.mensaje_regulable) + " apagada.";

                    if (puerta.isChecked())
                        msg_puerta = getResources().getString(R.string.mensaje_puerta) + " abierta.";
                    else
                        msg_puerta = getResources().getString(R.string.mensaje_puerta) + " cerrada.";


                    if(techo.isShown()){
                        techo.setVisibility(View.INVISIBLE);
                        lectura.setVisibility(View.INVISIBLE);
                        regulable.setVisibility(View.INVISIBLE);
                        puerta.setVisibility(View.INVISIBLE);
                    }

                    mensaje.setVisibility(View.VISIBLE);
                    mensaje.setText(msg_techo + "\n" + msg_lectura + "\n" + msg_regulable + "\n" + msg_puerta);


                    speak(msg_techo + ". " + msg_lectura + ". " + msg_regulable + ". " + msg_puerta, "ES", ID_PROMPT_INFO);

                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }



            else if(nBestList.get(0).equals("enciende la luz del techo") || nBestList.get(0).equals("enciende las luces del techo")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                puerta.setVisibility(View.VISIBLE);

                try {

                    if (!techo.isChecked()) {

                        techo.setChecked(true);
                        getUrl(0,1);
                        getUrl(1,1);
                        speak(getResources().getString(R.string.mensaje_encender_techo), "ES", ID_PROMPT_INFO);
                    }

                    else
                        speak("Las luces del techo ya están encendidas", "ES", ID_PROMPT_INFO);

                }
                catch(Exception e){
                        Log.e(LOGTAG, "TTS not accessible");

                }
            }

            else if(nBestList.get(0).equals("apaga la luz del techo") || nBestList.get(0).equals("apaga las luces del techo")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                puerta.setVisibility(View.VISIBLE);

                try {

                    if (techo.isChecked()) {

                        techo.setChecked(false);
                        getUrl(0,0);
                        getUrl(1,0);
                        speak(getResources().getString(R.string.mensaje_apagar_techo), "ES", ID_PROMPT_INFO);
                    }

                    else
                        speak("Las luces del techo ya están apagadas", "ES", ID_PROMPT_INFO);

                }
                catch(Exception e){
                    Log.e(LOGTAG, "TTS not accessible");

                }
            }

            else if(nBestList.get(0).equals("enciende la luz de lectura")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                puerta.setVisibility(View.VISIBLE);

                try {

                    if (!lectura.isChecked()) {

                        lectura.setChecked(true);
                        getUrl(2,1);
                        speak(getResources().getString(R.string.mensaje_encender_lectura), "ES", ID_PROMPT_INFO);
                    }

                    else
                        speak("La luz de lectura ya está encendida", "ES", ID_PROMPT_INFO);

                }
                catch(Exception e){
                    Log.e(LOGTAG, "TTS not accessible");

                }
            }

            else if(nBestList.get(0).equals("apaga la luz de lectura")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                puerta.setVisibility(View.VISIBLE);

                try {

                    if (lectura.isChecked()) {

                        lectura.setChecked(false);
                        getUrl(2,0);
                        speak(getResources().getString(R.string.mensaje_apagar_lectura), "ES", ID_PROMPT_INFO);
                    }

                    else
                        speak("La luz de lectura ya está apagada", "ES", ID_PROMPT_INFO);

                }
                catch(Exception e){
                    Log.e(LOGTAG, "TTS not accessible");

                }
            }

            else if(nBestList.get(0).equals("enciende la luz regulable")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                puerta.setVisibility(View.VISIBLE);

                try {

                    if (!regulable.isChecked()) {

                        regulable.setChecked(true);
                        getUrl(3,1);
                        speak(getResources().getString(R.string.mensaje_encender_regulable), "ES", ID_PROMPT_INFO);
                    }

                    else
                        speak("La luz regulable ya está encendida", "ES", ID_PROMPT_INFO);

                }
                catch(Exception e){
                    Log.e(LOGTAG, "TTS not accessible");

                }
            }

            else if(nBestList.get(0).equals("apaga la luz regulable")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                puerta.setVisibility(View.VISIBLE);

                try {

                    if (regulable.isChecked()) {

                        regulable.setChecked(false);
                        getUrl(3,0);
                        speak(getResources().getString(R.string.mensaje_apagar_regulable), "ES", ID_PROMPT_INFO);
                    }

                    else
                        speak("La luz regulable ya esa apagada", "ES", ID_PROMPT_INFO);

                }
                catch(Exception e){
                    Log.e(LOGTAG, "TTS not accessible");

                }
            }

            else if(nBestList.get(0).equals("abre la puerta")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                puerta.setVisibility(View.VISIBLE);

                try {

                    if (!puerta.isChecked()) {

                        puerta.setChecked(true);
                        getUrl(4,1);
                        speak(getResources().getString(R.string.mensaje_abrir_puerta), "ES", ID_PROMPT_INFO);
                    }

                    else
                        speak("Las luces del techo ya están encendidas", "ES", ID_PROMPT_INFO);

                }
                catch(Exception e){
                    Log.e(LOGTAG, "TTS not accessible");

                }
            }

            else if(nBestList.get(0).equals("cierra la puerta")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                puerta.setVisibility(View.VISIBLE);

                try {

                    if (puerta.isChecked()) {

                        puerta.setChecked(false);
                        getUrl(4,0);
                        speak(getResources().getString(R.string.mensaje_cerrar_puerta), "ES", ID_PROMPT_INFO);
                    }

                    else
                        speak("La puerta ya está cerrada", "ES", ID_PROMPT_INFO);

                }
                catch(Exception e){
                    Log.e(LOGTAG, "TTS not accessible");

                }
            }

            else if(nBestList.get(0).equals("gracias")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                puerta.setVisibility(View.VISIBLE);

                try {
                    speak("No hay de que.", "ES", ID_PROMPT_INFO);
                }
                catch(Exception e){
                    Log.e(LOGTAG, "TTS not accessible");

                }
            }
        }
    }


    @Override
    public void processAsrReadyForSpeech() {

    }

    @Override
    public void processAsrError(int errorCode) {

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
                    errorMsg = "Error al grabar audio";
                case SpeechRecognizer.ERROR_CLIENT:
                    errorMsg = "Error desconocido";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errorMsg = "Permisos insofucientes";
                case SpeechRecognizer.ERROR_NETWORK:
                    errorMsg = "Error de red";
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
                        Toast.makeText(getApplicationContext(), "Error de reconocimiento de voz", Toast.LENGTH_LONG).show();
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

    @Override
    public void onClick(View v) {

    }

    /////////////////////////////////////////////////////////////////////////////////////
    ///                                  HTTP                                         ///
    /////////////////////////////////////////////////////////////////////////////////////

    public void getUrl(int cod, int value) {


        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        String url="";

        switch (cod){

            //Luces techo 1
            case 0:
                url = getResources().getString(R.string.http_techo_1);
                break;

            //Luces techo 2
            case 1:
                url = getResources().getString(R.string.http_techo_2);
                break;

            //Luz lectura
            case 2:
                url = getResources().getString(R.string.http_letura);
                break;

            //Luz regulable
            case 3:
                url = getResources().getString(R.string.http_regulable);
                break;

            //Puerta
            case 4:
                url = getResources().getString(R.string.http_puerta);

            default:
                break;
        }

        url+= Integer.toString(value);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Display the first 500 characters of the response string.
                //mensaje.setText("Response is: " + response.substring(0, 500));
                //System.out.println("ësto funciona");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //mensaje.setText("That didn't work!");
                //System.out.println("älgo mas");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }
}