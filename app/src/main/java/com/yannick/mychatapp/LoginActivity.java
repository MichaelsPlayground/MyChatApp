package com.yannick.mychatapp;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.thebluealliance.spectrum.SpectrumDialog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String theme;
    private ImageView imgsplash;
    private MaterialButton loginbutton, createbutton;
    private EditText inputemail, inputpassword;
    private TextInputLayout inputemail_layout, inputpassword_layout;

    private ImageButton profileimage;
    private ImageButton profilebanner;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private static String userID = "";
    private static String geburtstag = "01.01.2000";
    private static String ownpi = "0";
    private static int color = 0;
    private int tmpcolor = -1;
    private DatabaseReference userroot = FirebaseDatabase.getInstance().getReference().getRoot().child("users");
    private StorageReference pathReference_image;
    private StorageReference pathReference_banner;

    private String img = "";
    private String banner = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeTheme();
        setContentView(R.layout.activity_login);

        imgsplash = findViewById(R.id.imgsplash);
        loginbutton = findViewById(R.id.loginbutton);
        createbutton = findViewById(R.id.createbutton);
        inputemail = findViewById(R.id.login_email);
        inputpassword = findViewById(R.id.login_password);
        inputemail_layout = findViewById(R.id.login_email_layout);
        inputpassword_layout = findViewById(R.id.login_password_layout);

        inputemail.setTextColor(getResources().getColor(R.color.black));
        inputpassword.setTextColor(getResources().getColor(R.color.black));

        if (theme.equals("1")) {
            imgsplash.setImageResource(R.drawable.ic_splash_dark);
        } else {
            imgsplash.setImageResource(R.drawable.ic_splash);
        }

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        inputemail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    inputemail_layout.setError(null);
                }
            }
        });

        inputpassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    inputpassword_layout.setError(null);
                }
            }
        });

        loginbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputemail.getText().toString().trim();
                String password = inputpassword.getText().toString().trim();
                if (!email.isEmpty()) {
                    if (!password.isEmpty()) {
                        login(email, password);
                    } else {
                        inputpassword_layout.setError(getResources().getString(R.string.enterpassword));
                    }
                } else {
                    inputemail_layout.setError(getResources().getString(R.string.enteremail));
                }
            }
        });

        createbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });
    }

    private void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("Auth", "successful");
                            mAuth.getCurrentUser().reload();
                            if (mAuth.getCurrentUser().isEmailVerified()) {
                                Intent homeIntent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(homeIntent);
                                finish();
                            } else {
                                openResendEmailDialog();
                            }
                        } else {
                            Log.d("Auth", "failed");
                            Toast.makeText(getApplicationContext(), R.string.loginfailed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void resendEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification();
        Toast.makeText(LoginActivity.this, R.string.verificationmailsent, Toast.LENGTH_SHORT).show();
        mAuth.signOut();
    }

    private void openResendEmailDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.resend_email, null);

        final MaterialButton resendemailbutton = view.findViewById(R.id.resendemailbutton);

        AlertDialog.Builder builder;
        if (theme.equals("1")) {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
        } else {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialog));
        }

        builder.setCustomTitle(setupHeader(getResources().getString(R.string.pleaseverifyemail)));
        builder.setCancelable(false);
        builder.setView(view);
        builder.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mAuth.signOut();
            }
        });

        final AlertDialog alert = builder.create();

        resendemailbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendEmail();
                alert.cancel();
            }
        });

        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        alert.show();
    }

    private void createAccount() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.create_account, null);

        final EditText email = view.findViewById(R.id.account_email);
        final EditText password = view.findViewById(R.id.account_password);
        final EditText password_repeat = view.findViewById(R.id.account_password_repeat);

        final TextInputLayout email_layout = view.findViewById(R.id.account_email_layout);
        final TextInputLayout password_layout = view.findViewById(R.id.account_password_layout);
        final TextInputLayout password_repeat_layout = view.findViewById(R.id.account_password_repeat_layout);

        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    email_layout.setError(null);
                }
            }
        });
        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    password_layout.setError(null);
                }
            }
        });
        password_repeat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    password_repeat_layout.setError(null);
                }
            }
        });

        img = "";
        banner = "";
        ownpi = "0";

        AlertDialog.Builder builder;
        if (theme.equals("1")) {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
        } else {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialog));
        }

        builder.setCustomTitle(setupHeader(getResources().getString(R.string.createprofile)));
        builder.setCancelable(false);
        builder.setView(view);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                View view = ((AlertDialog) dialogInterface).getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                dialogInterface.cancel();
            }
        });

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button b = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!email.getText().toString().isEmpty()) {
                            if (!password.getText().toString().trim().isEmpty()) {
                                if(password.getText().toString().trim().length()>=6)
                                    if (!password_repeat.getText().toString().trim().isEmpty()) {
                                        if (password.getText().toString().trim().equals(password_repeat.getText().toString().trim())) {
                                            createAccountData(email.getText().toString().trim(), password.getText().toString().trim());
                                            alert.cancel();
                                        } else {
                                            Toast.makeText(getApplicationContext(), R.string.passwordsdontmatch, Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        password_repeat_layout.setError(getResources().getString(R.string.repeatpassword));
                                    }
                                else {
                                    password_layout.setError(getResources().getString(R.string.passwordmustcontainatleastsixcharacters));
                                }
                            } else {
                                password_layout.setError(getResources().getString(R.string.enterpassword));
                            }
                        } else {
                            email_layout.setError(getResources().getString(R.string.enteremail));
                        }
                    }
                });
            }
        });

        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        alert.show();
    }

    private void createAccountAuth(final String email, final String password, final String name, final String bio, final String wohnort, final String geburtstag) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            userID = user.getUid();

                            if (ownpi.equals("0")) {
                                String imguuid = UUID.randomUUID().toString();
                                img = imguuid;
                            }

                            DatabaseReference user_root = userroot.child(userID);
                            Map<String, Object> map = new HashMap<>();
                            map.put("name", name);
                            map.put("bio", bio);
                            map.put("loc", wohnort);
                            map.put("bday", geburtstag.substring(6, 10) + geburtstag.substring(3, 5) + geburtstag.substring(0, 2));
                            map.put("favc", String.valueOf(color));
                            map.put("img", img);
                            map.put("banner", banner);
                            if (ownpi.equals("0")) {
                                map.put("ownpi", "0");
                            } else {
                                map.put("ownpi", "1");
                            }
                            user_root.updateChildren(map);
                            Toast.makeText(getApplicationContext(), R.string.profilecreated, Toast.LENGTH_SHORT).show();

                            if (!ownpi.equals("1")) {
                                storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
                                TextDrawable drawable = TextDrawable.builder()
                                        .beginConfig()
                                        .bold()
                                        .endConfig()
                                        .buildRect(name.substring(0, 1), getResources().getIntArray(R.array.favcolors)[color]);
                                Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
                                Canvas canvas = new Canvas(bitmap);
                                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                                drawable.draw(canvas);

                                byte[] byteArray;
                                final StorageReference pathReference_image = storageRef.child("profile_images/" + img);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                byteArray = stream.toByteArray();
                                try {
                                    stream.close();
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                                pathReference_image.putBytes(byteArray);
                            }

                            user.sendEmailVerification();
                            login(email, password);
                        } else {
                            Toast.makeText(LoginActivity.this, R.string.profilecreationfailed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createAccountData(final String email, final String password) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.edit_profile, null);

        final EditText username = view.findViewById(R.id.user_name);
        final EditText profilbio = view.findViewById(R.id.user_bio);
        final EditText birthday = view.findViewById(R.id.user_birthday);
        final EditText location = view.findViewById(R.id.user_location);

        final TextInputLayout username_layout = view.findViewById(R.id.user_name_layout);
        final TextInputLayout profilbio_layout = view.findViewById(R.id.user_bio_layout);
        final TextInputLayout location_layout = view.findViewById(R.id.user_location_layout);

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    username_layout.setError(null);
                }
            }
        });
        profilbio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    profilbio_layout.setError(null);
                }
            }
        });
        location.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() != 0) {
                    location_layout.setError(null);
                }
            }
        });

        final ImageButton favcolor = view.findViewById(R.id.user_favcolor);
        profileimage = view.findViewById(R.id.user_profile_image);
        profilebanner = view.findViewById(R.id.user_profile_banner);

        storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
        final StorageReference pathReference_image = storageRef.child("profile_images/" + img);
        final StorageReference pathReference_banner = storageRef.child("profile_banners/" + banner);

        birthday.setText(geburtstag);

        if (theme.equals("1")) {
            GlideApp.with(getApplicationContext())
                    //.using(new FirebaseImageLoader())
                    .load(pathReference_image)
                    .placeholder(R.drawable.side_nav_bar_dark)
                    .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                    .centerCrop()
                    .into(profileimage);

            GlideApp.with(getApplicationContext())
                    //.using(new FirebaseImageLoader())
                    .load(pathReference_banner)
                    .placeholder(R.drawable.side_nav_bar_dark)
                    .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                    .centerCrop()
                    .into(profilebanner);
        } else {
            GlideApp.with(getApplicationContext())
                    //.using(new FirebaseImageLoader())
                    .load(pathReference_image)
                    .placeholder(R.drawable.side_nav_bar)
                    .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                    .centerCrop()
                    .into(profileimage);

            GlideApp.with(getApplicationContext())
                    //.using(new FirebaseImageLoader())
                    .load(pathReference_banner)
                    .placeholder(R.drawable.side_nav_bar)
                    .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                    .centerCrop()
                    .into(profilebanner);
        }

        profileimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Bild auswählen"), 0);
            }
        });

        profilebanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Bild auswählen"), 1);
            }
        });

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(getResources().getIntArray(R.array.favcolors)[color]);
        favcolor.setBackground(shape);

        birthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog datePicker = new DatePickerDialog(view.getContext(), new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String datum;
                        if (dayOfMonth < 10) {
                            datum = "0" + dayOfMonth;
                        } else {
                            datum = "" + dayOfMonth;
                        }
                        monthOfYear = monthOfYear + 1;
                        if (monthOfYear < 10) {
                            datum = datum + ".0" + monthOfYear + "." + year;
                        } else {
                            datum = datum + "." + monthOfYear + "." + year;
                        }

                        birthday.setText(datum);
                    }
                }, Integer.parseInt(geburtstag.substring(6, 10)), Integer.parseInt(geburtstag.substring(3, 5)) - 1, Integer.parseInt(geburtstag.substring(0, 2)));
                if (theme.equals("1")) {
                    datePicker.getWindow().setBackgroundDrawableResource(R.color.dark_background);
                }
                Calendar c = Calendar.getInstance();
                c.set(2004, 11, 31);
                datePicker.getDatePicker().setMaxDate(c.getTimeInMillis());
                datePicker.show();
            }
        });

        favcolor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SpectrumDialog.Builder builder;
                if (theme.equals("1")) {
                    builder = new SpectrumDialog.Builder(getApplicationContext(), R.style.AlertDialogDark);
                } else {
                    builder = new SpectrumDialog.Builder(getApplicationContext(), R.style.AlertDialog);
                }
                builder.setColors(R.array.favcolors).setTitle(R.string.chooseacolor).setSelectedColor(getResources().getIntArray(R.array.favcolors)[color]).setFixedColumnCount(5).setOnColorSelectedListener(new SpectrumDialog.OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(boolean positiveResult, @ColorInt int scolor) {
                        if (positiveResult) {
                            int i = 0;
                            for (int c : getResources().getIntArray(R.array.favcolors)) {
                                if (c == scolor) {
                                    tmpcolor = i;
                                    GradientDrawable shape = new GradientDrawable();
                                    shape.setShape(GradientDrawable.OVAL);
                                    shape.setColor(getResources().getIntArray(R.array.favcolors)[i]);
                                    favcolor.setBackground(shape);
                                }
                                i++;
                            }
                        }
                    }
                }).build().show(getSupportFragmentManager(), "ColorPicker");
            }
        });

        AlertDialog.Builder builder;
        if (theme.equals("1")) {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogDark));
        } else {
            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialog));
        }

        builder.setCustomTitle(setupHeader(getResources().getString(R.string.createprofile)));
        builder.setCancelable(false);
        builder.setView(view);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                View view = ((AlertDialog) dialogInterface).getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                dialogInterface.cancel();
                userID = "";
            }
        });

        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button b = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!username.getText().toString().isEmpty()) {
                            if (!profilbio.getText().toString().isEmpty()) {
                                if (!location.getText().toString().isEmpty()) {
                                    if (!birthday.getText().toString().isEmpty()) {
                                        String name = username.getText().toString();
                                        String bio = profilbio.getText().toString();
                                        String wohnort = location.getText().toString();
                                        String geburtstag = birthday.getText().toString();

                                        createAccountAuth(email, password, name, bio, wohnort, geburtstag);

                                        if (view != null) {
                                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                                        }

                                        alert.cancel();
                                    } else {
                                        Toast.makeText(getApplicationContext(), R.string.incompletedata, Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    location_layout.setError(getResources().getString(R.string.enterlocation));
                                }
                            } else {
                                profilbio_layout.setError(getResources().getString(R.string.enterbio));
                            }
                        } else {
                            username_layout.setError(getResources().getString(R.string.entername));
                        }
                    }
                });
            }
        });
        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        alert.show();
    }

    private void changeTheme() {
        theme = readFromFile("mychatapp_theme.txt");
        if (theme.equals("1")) {
            setTheme(R.style.SplashDark);
        } else {
            setTheme(R.style.Splash);
        }
    }

    public String readFromFile(String datei) {
        Context context = this;
        String erg = "";

        try {
            InputStream inputStream = context.openFileInput(datei);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                erg = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return erg;
    }

    private TextView setupHeader(String title) {
        TextView header = new TextView(this);

        if (theme.equals("1")) {
            header.setBackgroundColor(getResources().getColor(R.color.dark_button));
        } else {
            header.setBackgroundColor(getResources().getColor(R.color.red));
        }

        header.setText(title);
        header.setPadding(30, 30, 30, 30);
        header.setTextSize(20F);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(Color.WHITE);

        return header;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && data != null && data.getData() != null ) {
            Uri filePath = data.getData();
            if(filePath != null) {
                uploadImage(filePath, requestCode);
            }
        }
    }

    private void uploadImage(Uri filePath, final int type) {
        final ProgressDialog progressDialog;
        if (theme.equals("1")) {
            progressDialog = new ProgressDialog(new ContextThemeWrapper(this, R.style.AlertDialogDark));
        } else {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setTitle(R.string.upload);
        progressDialog.show();

        storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
        StorageReference ref;
        if (type == 0) {
            img = UUID.randomUUID().toString();
            ref = storageRef.child("profile_images/" + img);
        } else {
            banner = UUID.randomUUID().toString();
            ref = storageRef.child("profile_banners/" + banner);
        }

        byte[] byteArray;
        InputStream imageStream = null;
        try {
            imageStream = getContentResolver().openInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap bmp = BitmapFactory.decodeStream(imageStream);

        if (bmp.getWidth() < bmp.getHeight() && type == 0) {
            bmp = Bitmap.createBitmap(bmp, 0, bmp.getHeight()/2-bmp.getWidth()/2, bmp.getWidth(), bmp.getWidth());
        } else if (bmp.getWidth() > bmp.getHeight() && type == 0) {
            bmp = Bitmap.createBitmap(bmp, bmp.getWidth()/2-bmp.getHeight()/2, 0, bmp.getHeight(), bmp.getHeight());
        } else if (bmp.getWidth()/16*9 < bmp.getHeight() && type == 1) {
            bmp = Bitmap.createBitmap(bmp, 0, bmp.getHeight()/2-bmp.getWidth()/16*9/2, bmp.getWidth(), bmp.getWidth()/16*9);
        } else if (bmp.getWidth()/16*9 > bmp.getHeight() && type == 1) {
            bmp = Bitmap.createBitmap(bmp, bmp.getWidth()/2-bmp.getHeight()/9*16/2, 0, bmp.getHeight()/9*16, bmp.getHeight());
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int compression = 100;
        int compressFactor = 2;
        int height = bmp.getHeight();
        int width = bmp.getWidth();
        if (getImgSize(filePath) > height * width) {
            compressFactor = 4;
        }
        if (type == 0) {
            while (height * width > 500 * 500) {
                height /= 1.1;
                width /= 1.1;
                compression -= compressFactor;
            }
        } else {
            while (height * width > 1920 * 1080) {
                height /= 1.1;
                width /= 1.1;
                compression -= compressFactor;
            }
        }
        bmp = Bitmap.createScaledBitmap(bmp, width, height, false);
        try {
            bmp = rotateImageIfRequired(this, bmp, filePath);
        } catch (IOException e) { }
        bmp.compress(Bitmap.CompressFormat.JPEG, compression, stream);
        byteArray = stream.toByteArray();
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        UploadTask uploadTask = ref.putBytes(byteArray);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressDialog.dismiss();
                if (type == 0) {
                    ownpi = "1";
                    DatabaseReference user_root = userroot.child(userID);
                    Map<String, Object> map = new HashMap<>();
                    map.put("ownpi", "1");
                    user_root.updateChildren(map);
                }
                updateEditProfileImages();
                Toast.makeText(LoginActivity.this, R.string.imageuploaded, Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, R.string.imagetoolarge, Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                        .getTotalByteCount());
                progressDialog.setMessage((int)progress+"% " + getResources().getString(R.string.uploaded));
            }
        });
    }

    private Long getImgSize(Uri filePath) {
        Cursor returnCursor = getContentResolver().query(filePath, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        return returnCursor.getLong(sizeIndex);
    }

    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {

        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23) {
            ei = new ExifInterface(input);
        } else {
            ei = new ExifInterface(selectedImage.getPath());
        }

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private void updateEditProfileImages() {
        storageRef = storage.getReferenceFromUrl(FirebaseStorage.getInstance().getReference().toString());
        pathReference_image = storageRef.child("profile_images/" + img);
        pathReference_image.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                GlideApp.with(getApplicationContext())
                        //.using(new FirebaseImageLoader())
                        .load(pathReference_image)
                        .signature(new ObjectKey(String.valueOf(storageMetadata.getCreationTimeMillis())))
                        .centerCrop()
                        .into(profileimage);
            }
        });
        pathReference_banner = storageRef.child("profile_banners/" + banner);
        pathReference_banner.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                GlideApp.with(getApplicationContext())
                        //.using(new FirebaseImageLoader())
                        .load(pathReference_banner)
                        .signature(new ObjectKey(String.valueOf(storageMetadata.getCreationTimeMillis())))
                        .centerCrop()
                        .thumbnail(0.05f)
                        .into(profilebanner);
            }
        });
    }
}
