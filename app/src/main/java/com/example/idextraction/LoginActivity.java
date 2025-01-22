package com.example.idextraction;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {
    // UI Components
    private EditText usernameEditText;
    private EditText passwordEditText;
    private TextInputLayout verificationCodeLayout;
    private EditText verificationCodeEditText;
    private Button loginButton;
    private TextView registerLink;
    private ProgressBar progressBar;
    private TextView countdownTextView;
    private TextView verificationMessageText;

    // Variables
    private String generatedCode;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        verificationCodeLayout = findViewById(R.id.verificationCodeLayout);
        verificationCodeEditText = findViewById(R.id.verificationCodeEditText);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
        progressBar = findViewById(R.id.progressBar);
        countdownTextView = findViewById(R.id.countdownTextView);
        verificationMessageText = findViewById(R.id.verificationMessageText);

        verificationCodeLayout.setVisibility(View.GONE);
        countdownTextView.setVisibility(View.GONE);
        verificationMessageText.setVisibility(View.GONE);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(view -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String verificationCode = verificationCodeEditText.getText().toString().trim();

            if (verificationCodeLayout.getVisibility() == View.VISIBLE) {
                if (!verificationCode.isEmpty()) {
                    verifyCode(verificationCode);
                } else {
                    showError("Veuillez entrer le code de vérification");
                }
            } else {
                if (!username.isEmpty() && !password.isEmpty()) {
                    loginUser(username, password);
                } else {
                    showError("Veuillez remplir tous les champs");
                }
            }
        });

        registerLink.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser(String email, String password) {
        if (!isNetworkConnected()) {
            showError("Vérifiez votre connexion internet");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                URL url = new URL("http://172.20.10.12:5050/api/public/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();

                int responseCode = conn.getResponseCode();
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    generatedCode = jsonResponse.getString("verificationCode");

                    runOnUiThread(() -> {
                        verificationCodeLayout.setVisibility(View.VISIBLE);
                        countdownTextView.setVisibility(View.VISIBLE);
                        verificationMessageText.setVisibility(View.VISIBLE);
                        verificationMessageText.setText("Un code de vérification a été envoyé à " + email);
                        startCountdown();
                        loginButton.setText("Vérifier le code");
                    });
                } else {
                    handleErrorResponse(responseCode);
                }
            } catch (Exception e) {
                handleNetworkError(e);
            } finally {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }

    private void verifyCode(String enteredCode) {
        if (!isNetworkConnected()) {
            showError("Vérifiez votre connexion internet");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.8.104:8082/api/public/verification");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("email", usernameEditText.getText().toString().trim());
                json.put("code", enteredCode);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Code vérifié avec succès!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        showError("Code de vérification invalide");
                        verificationCodeEditText.setText("");
                    });
                }
            } catch (Exception e) {
                handleNetworkError(e);
            } finally {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }

    private void startCountdown() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                countdownTextView.setText("Temps restant: " + millisUntilFinished / 1000 + "s");
            }

            public void onFinish() {
                countdownTextView.setText("Temps écoulé!");
                verificationCodeLayout.setVisibility(View.GONE);
                verificationMessageText.setVisibility(View.GONE);
                loginButton.setEnabled(false);
                loginButton.setText("Se connecter");
            }
        }.start();
    }

    private void handleErrorResponse(int responseCode) {
        String errorMessage;
        switch (responseCode) {
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                errorMessage = "Email ou mot de passe incorrect";
                break;
            case HttpURLConnection.HTTP_BAD_REQUEST:
                errorMessage = "Veuillez vérifier vos informations";
                break;
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                errorMessage = "Erreur serveur, veuillez réessayer plus tard";
                break;
            default:
                errorMessage = "Impossible de se connecter au serveur";
        }
        runOnUiThread(() -> showError(errorMessage));
    }

    private void handleNetworkError(Exception e) {
        String errorMessage;
        if (e instanceof java.net.SocketTimeoutException) {
            errorMessage = "Le serveur met trop de temps à répondre";
        } else if (e instanceof java.net.UnknownHostException) {
            errorMessage = "Impossible de joindre le serveur";
        } else {
            errorMessage = "Une erreur est survenue, veuillez réessayer";
        }
        runOnUiThread(() -> showError(errorMessage));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}