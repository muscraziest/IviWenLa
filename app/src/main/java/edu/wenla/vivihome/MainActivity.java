package edu.wenla.vivihome;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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
    private Switch techo, lectura, regulable;
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

        setSettings();
        openSettings();
        checkState();
    }


    /////////////////////////////////////////////////////////////////////////////////////
    ///                                  TÁCTIL                                       ///
    /////////////////////////////////////////////////////////////////////////////////////
    public void setSettings(){

        String state = "";

        for (int i=0; i < 3; ++i){

            state = getUrl(i);

            Log.d("E","i: " + Integer.toString(i) + "\tstate: " + state);

            if(state.equals("0")){

                switch (i){

                    case 0:
                        techo.setChecked(false);
                        break;
                    case 1:
                        //techo.setChecked(false);
                        break;
                    case 2:
                        lectura.setChecked(false);
                        break;
                    case 3:
                        regulable.setChecked(false);
                        break;
                }
            }

            else{

                switch (i){

                    case 0:
                        techo.setChecked(true);
                        break;
                    case 1:
                        //techo.setChecked(true);
                        break;
                    case 2:
                        lectura.setChecked(true);
                        break;
                    case 3:
                        regulable.setChecked(true);
                        break;
                }
            }
        }
    }


    @TargetApi(VERSION_CODES.M)
    public void openSettings(){

        opciones = findViewById(R.id.settings_btn);

        opciones.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){

                techo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked) {
                            setUrl(0, 1);
                            setUrl(1, 1);
                        }
                        else {
                            setUrl(0, 0);
                            setUrl(1, 0);
                        }
                    }
                });

                lectura.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked)
                            setUrl(2, 1);

                        else
                            setUrl(2, 0);

                    }
                });

                regulable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked)
                            setUrl(2, 1);

                        else
                            setUrl(2, 0);

                    }
                });

                if(!techo.isShown()) {
                    mensaje.setVisibility(View.INVISIBLE);
                    techo.setVisibility(View.VISIBLE);
                    lectura.setVisibility(View.VISIBLE);
                    regulable.setVisibility(View.VISIBLE);

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


                    if (techo.isShown()) {
                        techo.setVisibility(View.INVISIBLE);
                        lectura.setVisibility(View.INVISIBLE);
                        regulable.setVisibility(View.INVISIBLE);
                    }

                    mensaje.setVisibility(View.VISIBLE);
                    mensaje.setText(msg_techo + "\n" + msg_lectura + "\n" + msg_regulable + "\n");

                    try {
                        speak(msg_techo + ". " + msg_lectura + ". " + msg_regulable + ". ", "ES", ID_PROMPT_INFO);

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
                    speak("¿Qué quieres hacer?", "ES", ID_PROMPT_INFO);
                    openSettings();
                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }

            else if(nBestList.get(0).equals("consultar estado") || nBestList.get(0).equals("consultar")){

                try {
                    String msg_techo;
                    String msg_lectura;
                    String msg_regulable;

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


                    if(techo.isShown()){
                        techo.setVisibility(View.INVISIBLE);
                        lectura.setVisibility(View.INVISIBLE);
                        regulable.setVisibility(View.INVISIBLE);
                    }

                    mensaje.setVisibility(View.VISIBLE);
                    mensaje.setText(msg_techo + "\n" + msg_lectura + "\n" + msg_regulable + "\n");

                    speak(msg_techo + ". " + msg_lectura + ". " + msg_regulable + ". ", "ES", ID_PROMPT_INFO);

                } catch (Exception e) {
                    Log.e(LOGTAG, "TTS not accessible");
                }
            }

            else if(nBestList.get(0).equals("enciende la luz del techo") || nBestList.get(0).equals("enciende las luces del techo")) {

                techo.setVisibility(View.VISIBLE);
                lectura.setVisibility(View.VISIBLE);
                regulable.setVisibility(View.VISIBLE);
                mensaje.setVisibility(View.INVISIBLE);

                try {

                    if (!techo.isChecked()) {

                        techo.setChecked(true);
                        setUrl(0,1);
                        //setUrl(1,1);
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
                mensaje.setVisibility(View.INVISIBLE);

                try {

                    if (techo.isChecked()) {

                        techo.setChecked(false);
                        setUrl(0,0);
                        //setUrl(1,0);
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
                mensaje.setVisibility(View.INVISIBLE);

                try {

                    if (!lectura.isChecked()) {

                        lectura.setChecked(true);
                        setUrl(2,1);
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
                mensaje.setVisibility(View.INVISIBLE);

                try {

                    if (lectura.isChecked()) {

                        lectura.setChecked(false);
                        setUrl(2,0);
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
                mensaje.setVisibility(View.INVISIBLE);

                try {

                    if (!regulable.isChecked()) {

                        regulable.setChecked(true);
                        setUrl(3,60);
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
                mensaje.setVisibility(View.INVISIBLE);

                try {

                    if (regulable.isChecked()) {

                        regulable.setChecked(false);
                        setUrl(3,0);
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
                mensaje.setVisibility(View.INVISIBLE);

                try {

                    setUrl(4,1);
                    speak(getResources().getString(R.string.mensaje_abrir_puerta), "ES", ID_PROMPT_INFO);
                }
                catch(Exception e){
                    Log.e(LOGTAG, "TTS not accessible");

                }
            }


            else if(nBestList.get(0).equals("gracias")) {

                techo.setVisibility(View.INVISIBLE);
                lectura.setVisibility(View.INVISIBLE);
                regulable.setVisibility(View.INVISIBLE);

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

    public void setUrl(int cod, int value) {


        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "";

        switch (cod){

            //Luces techo 1
            case 0:
                url = getResources().getString(R.string.set_techo_1);
                break;

            //Luces techo 2
            case 1:
                url = getResources().getString(R.string.set_techo_2);
                break;

            //Luz lectura
            case 2:
                url = getResources().getString(R.string.set_letura);
                break;

            //Luz regulable
            case 3:
                url = getResources().getString(R.string.set_regulable);
                break;

            //Puerta
            case 4:
                url = getResources().getString(R.string.set_puerta);

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

    public String getUrl(int code) {


        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "";
        final String[] salida = {""};

        switch (code){

            //Luces techo 1
            case 0:
                url = getResources().getString(R.string.get_techo_1);
                break;

            //Luces techo 2
            case 1:
                url = getResources().getString(R.string.get_techo_2);
                break;

            //Luz lectura
            case 2:
                url = getResources().getString(R.string.get_lectura);
                break;

            //Luz regulable
            case 3:
                url = getResources().getString(R.string.get_regulable);
                break;

            default:
                break;
        }

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Display the first 500 characters of the response string.
                //mensaje.setText("Response is: " + response.substring(0, 500));
                Log.d("response",response);
                Log.d("substring",response.substring(response.indexOf("=")+1,response.length()-2));
                salida[0] = response.substring(response.indexOf("=")+1,response.length()-2);
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

        Log.d("asdsa","Salida: " + salida[0]);

        return salida[0];
    }
}