package com.facundo.reconocimientorostros2;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ImageView imgFoto;
    Paint pinturaLetra;
    Paint pinturaBordeLetra;
    Paint pinturaRecuadro;
    Paint pinturaMarca;

    boolean mostrarMarcasImagen = false;
    boolean mostrarMarcasConsola = false;
    boolean mostrarNombreMarcasImagen = false;
    boolean mostrarRecuadroCaraImagen = false;

    private Uri imageUri;
    private Bitmap imagen_original;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enlazarUI();

        construirPinturas();

        agregarImagenPorDefecto();
    }

    private void enlazarUI() {
        imgFoto = (ImageView) findViewById(R.id.imageViewFoto);
        Button btnGaleria = findViewById(R.id.agregarImagenBtn);
        Button btnEliminarMascara = findViewById(R.id.btnEliminarMascara);
        ImageButton btnMascaraLentes = findViewById(R.id.btnMascaraLentes);
        ImageButton btnMascaraElegante = findViewById(R.id.btnMascaraElegante);
        ImageButton btnMascaraMickey = findViewById(R.id.btnMascaraMickey);
        ImageButton btnMascaraFlores = findViewById(R.id.btnMascaraFlores);
        ImageButton btnMascaraSnowman = findViewById(R.id.btnMascaraSnowman);

        btnGaleria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(gallery, 50);
            }
        });

        btnEliminarMascara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap imagen_mutable = imagen_original.copy(Bitmap.Config.ARGB_8888, true);
                imgFoto.setAdjustViewBounds(true);
                imgFoto.setImageBitmap(imagen_mutable);
                dro("");
            }
        });

        btnMascaraLentes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dro("lentes");
            }
        });

        btnMascaraElegante.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dro("elegante");
            }
        });

        btnMascaraMickey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dro("mickey");
            }
        });

        btnMascaraFlores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dro("flores");
            }
        });

        btnMascaraSnowman.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dro("snowman");
            }
        });
    }

    private void dro(String imagenSeleccionada) {
        Bitmap imagen_mutable = imagen_original.copy(Bitmap.Config.ARGB_8888, true);
        imgFoto.setAdjustViewBounds(true);
        imgFoto.setImageBitmap(imagen_mutable);

        Canvas canvas = new Canvas(imagen_mutable);

        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build();

        if (!faceDetector.isOperational()) {
            Toast.makeText(getApplicationContext(), "Error, intente de nuevo", Toast.LENGTH_SHORT).show();
            return;
        }

        Frame frame = new Frame.Builder().setBitmap(imagen_original).build();
        SparseArray<Face> sparseArray = faceDetector.detect(frame);

        for (int i = 0; i < sparseArray.size(); i++) {
            Face face = sparseArray.valueAt(i);

            double anguloCara = Math.toRadians(face.getEulerZ());
            double anguloCuello = Math.toRadians(face.getEulerY());

            if (mostrarRecuadroCaraImagen) {
                float xSupIzqCara = face.getPosition().x;
                float ySupIzqCara = face.getPosition().y;
                float xInfDerCara = xSupIzqCara + face.getWidth();
                float yInfDerCara = ySupIzqCara + face.getHeight();

                canvas.drawRect(xSupIzqCara, ySupIzqCara, xInfDerCara, yInfDerCara, pinturaRecuadro);
            }

            Map<Integer, Punto> puntos = new HashMap<>();

            StringBuilder mensajeConsola = new StringBuilder("---FACE " + i + "---");

            for (Landmark landmark : face.getLandmarks()) {
                int x = (int) landmark.getPosition().x;
                int y = (int) landmark.getPosition().y;
                int tipo = landmark.getType();

                puntos.put(tipo, new Punto(x, y));

                if (mostrarMarcasConsola) {
                    agregarMarcaATexto(mensajeConsola, x, y, tipo);
                }

                if (mostrarMarcasImagen) {
                    canvas.drawCircle(x, y, face.getWidth()/30, pinturaMarca);
                }

                dibujarMascara(canvas, puntos, anguloCara, imagenSeleccionada, anguloCuello);

                if (mostrarNombreMarcasImagen) {
                    agregaNombreMarcaAImagen(canvas, x, y, tipo);
                }
            }

            if (mostrarMarcasConsola) {
                Log.i(TAG, mensajeConsola.toString());
            }

        }

    }

    private void dibujarMascara(Canvas canvas, Map<Integer, Punto> puntos, double anguloCara, String imagenSeleccionada, double anguloCuello) {
        switch (imagenSeleccionada) {
            case "lentes":
                dibujarLentesDealWithIt(canvas, puntos, anguloCara);
                break;
            case "elegante":
                dibujarElegante(canvas, puntos, anguloCara);
                break;
            case "mickey":
                dibujarMickey(canvas, puntos, anguloCara);
                break;
            case "flores":
                dibujarFlores(canvas, puntos, anguloCara);
                break;
            case "snowman":
                dibujarSnowman(canvas, puntos, anguloCara, anguloCuello);
                break;
            default:
                break;
        }
    }

    private void dibujarLentesDealWithIt(Canvas canvas, Map<Integer, Punto> puntos, double anguloCara) {
        Punto ojoIzquierdo = puntos.get(Landmark.LEFT_EYE);
        Punto ojoDerecho = puntos.get(Landmark.RIGHT_EYE);

        if (ojoIzquierdo == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo izquierdo", Toast.LENGTH_LONG).show();
            return;
        }
        if (ojoDerecho == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo derecho", Toast.LENGTH_LONG).show();
            return;
        }
        int xOjoDerCorregido = (int) (ojoDerecho.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoDerCorregido = (int) (ojoDerecho.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));
        int xOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));

        int ancho = (int) ((100.0 / 50) * Math.sqrt(Math.pow(xOjoDerCorregido - xOjoIzqCorregido, 2) + Math.pow(yOjoIzqCorregido - yOjoDerCorregido, 2)));
        int alto = ancho / 5;
        int xInicio = (int) (xOjoDerCorregido - (22.0 / 100 * ancho));
        int yInicio = (int) (yOjoDerCorregido - (10.0 / 20) * alto);
        int xFin = xInicio + ancho;
        int yFin = yInicio + alto;

        canvas.save(Canvas.ALL_SAVE_FLAG); //Saving the canvas and later restoring it so only this image will be rotated.
        canvas.rotate((float) -Math.toDegrees(anguloCara));

        Drawable d = getResources().getDrawable(R.drawable.lentes);
        d.setBounds(xInicio, yInicio, xFin, yFin);
        d.draw(canvas);

        canvas.restore();
    }

    private void dibujarElegante(Canvas canvas, Map<Integer, Punto> puntos, double anguloCara) {
        Punto ojoIzquierdo = puntos.get(Landmark.LEFT_EYE);
        Punto ojoDerecho = puntos.get(Landmark.RIGHT_EYE);
        Punto bocaIzquierda = puntos.get(Landmark.LEFT_MOUTH);
        Punto bocaDerecha = puntos.get(Landmark.RIGHT_MOUTH);

        if (ojoIzquierdo == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo izquierdo", Toast.LENGTH_LONG).show();
            return;
        }
        if (ojoDerecho == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo derecho", Toast.LENGTH_LONG).show();
            return;
        }
        if (bocaIzquierda == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra boca izquierda", Toast.LENGTH_LONG).show();
            return;
        }
        if (bocaDerecha == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra boca derecha", Toast.LENGTH_LONG).show();
            return;
        }

        int xOjoDerCorregido = (int) (ojoDerecho.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoDerCorregido = (int) (ojoDerecho.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));
        int xOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));

        int ancho1 = (int) ((30.0 / 10) * Math.sqrt(Math.pow(xOjoDerCorregido - xOjoIzqCorregido, 2) + Math.pow(yOjoIzqCorregido - yOjoDerCorregido, 2)));
        int alto1 = (int) ((16.0 / 30) * ancho1);
        int xInicio1 = (int) (xOjoDerCorregido - (10.0 / 30) * ancho1);
        int yInicio1 = (int) (yOjoDerCorregido - (23.0 / 16) * alto1);
        int xFin1 = xInicio1 + ancho1;
        int yFin1 = yInicio1 + alto1;

        int ancho2 = (int) ((20.0 / 60) * ancho1);
        int alto2 = (int) ((50.0 / 20) * ancho2);
        int xInicio2 = (int) (xOjoDerCorregido - (12.0 / 20) * ancho2);
        int yInicio2 = (int) (yOjoDerCorregido - (8.0 / 50) * alto2);
        int xFin2 = xInicio2 + ancho2;
        int yFin2 = yInicio2 + alto2;

        int xBocaDerCorregido = (int) (bocaDerecha.getX() * Math.cos(anguloCara) - bocaDerecha.getY() * Math.sin(anguloCara));
        int yBocaDerCorregido = (int) (bocaDerecha.getX() * Math.sin(anguloCara) + bocaDerecha.getY() * Math.cos(anguloCara));
        int xBocaIzqCorregido = (int) (bocaIzquierda.getX() * Math.cos(anguloCara) - bocaIzquierda.getY() * Math.sin(anguloCara));
        int yBocaIzqCorregido = (int) (bocaIzquierda.getX() * Math.sin(anguloCara) + bocaIzquierda.getY() * Math.cos(anguloCara));
        int ancho3 = (int) ((1150.0 / 350) * Math.sqrt(Math.pow(xBocaDerCorregido - xBocaIzqCorregido, 2) + Math.pow(yBocaIzqCorregido - yBocaDerCorregido, 2)));
        int alto3 = (int) ((330.0 / 1150) * ancho3);
        int xInicio3 = (int) (xBocaDerCorregido - (400.0 / 1150) * ancho3);
        int yInicio3 = (int) (yBocaDerCorregido - (185.0 / 330) * alto3);
        int xFin3 = xInicio3 + ancho3;
        int yFin3 = yInicio3 + alto3;

        canvas.save(Canvas.ALL_SAVE_FLAG); //Saving the canvas and later restoring it so only this image will be rotated.
        canvas.rotate((float) -Math.toDegrees(anguloCara));

        Drawable d = getResources().getDrawable(R.drawable.elegante1);
        d.setBounds(xInicio1, yInicio1, xFin1, yFin1);
        d.draw(canvas);
        d = getResources().getDrawable(R.drawable.elegante2);
        d.setBounds(xInicio2, yInicio2, xFin2, yFin2);
        d.draw(canvas);
        d = getResources().getDrawable(R.drawable.elegante3);
        d.setBounds(xInicio3, yInicio3, xFin3, yFin3);
        d.draw(canvas);

        canvas.restore();
    }

    private void dibujarMickey(Canvas canvas, Map<Integer, Punto> puntos, double anguloCara) {
        Punto ojoIzquierdo = puntos.get(Landmark.LEFT_EYE);
        Punto ojoDerecho = puntos.get(Landmark.RIGHT_EYE);

        if (ojoIzquierdo == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo izquierdo", Toast.LENGTH_LONG).show();
            return;
        }
        if (ojoDerecho == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo derecho", Toast.LENGTH_LONG).show();
            return;
        }
        int xOjoDerCorregido = (int) (ojoDerecho.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoDerCorregido = (int) (ojoDerecho.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));
        int xOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));

        int ancho = (int) ((440.0 / 140) * Math.sqrt(Math.pow(xOjoDerCorregido - xOjoIzqCorregido, 2) + Math.pow(yOjoIzqCorregido - yOjoDerCorregido, 2)));
        int alto = (int) ((192.0 / 440) * ancho);
        int xInicio = (int) (xOjoDerCorregido - (150.0 / 440) * ancho);
        int yInicio = (int) (yOjoDerCorregido - (322.0 / 192) * alto);
        int xFin = xInicio + ancho;
        int yFin = yInicio + alto;

        canvas.save(Canvas.ALL_SAVE_FLAG); //Saving the canvas and later restoring it so only this image will be rotated.
        canvas.rotate((float) -Math.toDegrees(anguloCara));

        Drawable d = getResources().getDrawable(R.drawable.micky_mouse);
        d.setBounds(xInicio, yInicio, xFin, yFin);
        d.draw(canvas);

        canvas.restore();
    }

    private void dibujarFlores(Canvas canvas, Map<Integer, Punto> puntos, double anguloCara) {
        Punto ojoIzquierdo = puntos.get(Landmark.LEFT_EYE);
        Punto ojoDerecho = puntos.get(Landmark.RIGHT_EYE);

        if (ojoIzquierdo == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo izquierdo", Toast.LENGTH_LONG).show();
            return;
        }
        if (ojoDerecho == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo derecho", Toast.LENGTH_LONG).show();
            return;
        }

        int xOjoDerCorregido = (int) (ojoDerecho.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoDerCorregido = (int) (ojoDerecho.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));
        int xOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));

        int ancho = (int) ((470.0 / 150) * Math.sqrt(Math.pow(xOjoDerCorregido - xOjoIzqCorregido, 2) + Math.pow(yOjoIzqCorregido - yOjoDerCorregido, 2)));
        int alto = (int) ((175.0 / 470) * ancho);
        int xInicio = (int) (xOjoDerCorregido - (170.0 / 470) * ancho);
        int yInicio = (int) (yOjoDerCorregido - (325.0 / 175) * alto);
        int xFin = xInicio + ancho;
        int yFin = yInicio + alto;

        canvas.save(Canvas.ALL_SAVE_FLAG); //Saving the canvas and later restoring it so only this image will be rotated.
        canvas.rotate((float) -Math.toDegrees(anguloCara));

        Drawable d = getResources().getDrawable(R.drawable.flower_crown);
        d.setBounds(xInicio, yInicio, xFin, yFin);
        d.draw(canvas);

        canvas.restore();
    }

    private void dibujarSnowman(Canvas canvas, Map<Integer, Punto> puntos, double anguloCara, double anguloCuello) {
        Punto ojoIzquierdo = puntos.get(Landmark.LEFT_EYE);
        Punto ojoDerecho = puntos.get(Landmark.RIGHT_EYE);
        Punto baseNariz = puntos.get(Landmark.NOSE_BASE);
        Punto medioBoca = puntos.get(Landmark.BOTTOM_MOUTH);

        if (ojoIzquierdo == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo izquierdo", Toast.LENGTH_LONG).show();
            return;
        }
        if (ojoDerecho == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra ojo derecho", Toast.LENGTH_LONG).show();
            return;
        }
        if (baseNariz == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra base nariz", Toast.LENGTH_LONG).show();
            return;
        }
        if (medioBoca == null) {
//            Toast.makeText(this.getApplicationContext(), "No se encuentra medio boca", Toast.LENGTH_LONG).show();
            return;
        }
        int xOjoDerCorregido = (int) (ojoDerecho.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoDerCorregido = (int) (ojoDerecho.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));
        int xOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.cos(anguloCara) - ojoDerecho.getY() * Math.sin(anguloCara));
        int yOjoIzqCorregido = (int) (ojoIzquierdo.getX() * Math.sin(anguloCara) + ojoDerecho.getY() * Math.cos(anguloCara));
        int xNarizCorregido = (int) (baseNariz.getX() * Math.cos(anguloCara) - baseNariz.getY() * Math.sin(anguloCara));
        int yNarizCorregido = (int) (baseNariz.getX() * Math.sin(anguloCara) + baseNariz.getY() * Math.cos(anguloCara));
        int xBocaCorregido = (int) (medioBoca.getX() * Math.cos(anguloCara) - medioBoca.getY() * Math.sin(anguloCara));
        int yBocaCorregido = (int) (medioBoca.getX() * Math.sin(anguloCara) + medioBoca.getY() * Math.cos(anguloCara));

        int ancho1 = (int) ((30.0 / 20) * Math.sqrt(Math.pow(xOjoDerCorregido - xOjoIzqCorregido, 2) + Math.pow(yOjoIzqCorregido - yOjoDerCorregido, 2)));
        int alto1 = (int) ((16.0 / 30) * ancho1);
        int xInicio1 = (int) (xOjoDerCorregido - (5.0 / 30) * ancho1);
        int yInicio1 = (int) (yOjoDerCorregido - (50.0 / 16) * alto1);
        int xFin1 = xInicio1 + ancho1;
        int yFin1 = yInicio1 + alto1;

        int ancho2 = (int) ((52.0 / 30) * ancho1 * 0.8);
        int alto2 = (int) ((13.0 / 52) * ancho2);
        int xInicio2 = (int) (xNarizCorregido - (45.0 / 52) * ancho2);
        if (anguloCuello > 0) {
            xInicio2 = (int) (xNarizCorregido - (7.0 / 52) * ancho2);
        }
        int yInicio2 = (int) (yNarizCorregido - (9.0 / 13) * alto2);
        int xFin2 = xInicio2 + ancho2;
        int yFin2 = yInicio2 + alto2;

        int ancho3 = (int) ((90.0 / 30) * ancho1 * 1.1);
        int alto3 = (int) ((82.0 / 90) * ancho3);
        int xInicio3 = (int) (xBocaCorregido - (45.0 / 90) * ancho3);
        int yInicio3 = (int) (yBocaCorregido - (-5.0 / 82) * alto3);
        int xFin3 = xInicio3 + ancho3;
        int yFin3 = yInicio3 + alto3;

        canvas.save(Canvas.ALL_SAVE_FLAG); //Saving the canvas and later restoring it so only this image will be rotated.
        canvas.rotate((float) -Math.toDegrees(anguloCara));

        Drawable d = getResources().getDrawable(R.drawable.elegante1);
        d.setBounds(xInicio1, yInicio1, xFin1, yFin1);
        d.draw(canvas);

        if (anguloCuello > 0) {
            d = getResources().getDrawable(R.drawable.snowman_2_2);
        } else {
            d = getResources().getDrawable(R.drawable.snowman_2);
        }
        d.setBounds(xInicio2, yInicio2, xFin2, yFin2);
        d.draw(canvas);
        d = getResources().getDrawable(R.drawable.snowman_3);
        d.setBounds(xInicio3, yInicio3, xFin3, yFin3);
        d.draw(canvas);

        canvas.restore();

        int wid = canvas.getWidth();
        int hei = canvas.getHeight();
        for (int i = 0; i < 200; i++) {
            switch ((int) (Math.random() * 3)) {
                case 0:
                    d = getResources().getDrawable(R.drawable.snowflake_1);
                    break;
                case 1:
                    d = getResources().getDrawable(R.drawable.snowflake_2);
                    break;
                case 2:
                    d = getResources().getDrawable(R.drawable.snowflake_3);
                    break;
            }
            int xCopo = (int) (Math.random() * wid);
            int yCopo = (int) (Math.random() * hei);
            int escala = wid / 70;
            d.setBounds(xCopo, yCopo, xCopo + escala, yCopo + escala);
            d.draw(canvas);
        }
    }

    private void construirPinturas() {
        pinturaMarca = new Paint();
        pinturaMarca.setAntiAlias(true);
        pinturaMarca.setColor(Color.RED);

        pinturaBordeLetra = new Paint();
        pinturaBordeLetra.setAntiAlias(true);
        pinturaBordeLetra.setColor(Color.BLACK);
        pinturaBordeLetra.setStyle(Paint.Style.STROKE);
        pinturaBordeLetra.setStrokeWidth(2);
        pinturaBordeLetra.setTextSize(12);

        pinturaLetra = new Paint();
        pinturaLetra.setAntiAlias(true);
        pinturaLetra.setColor(Color.WHITE);
        pinturaLetra.setTextSize(12);

        pinturaRecuadro = new Paint();
        pinturaRecuadro.setColor(Color.YELLOW);
        pinturaRecuadro.setStyle(Paint.Style.STROKE);
        pinturaRecuadro.setStrokeWidth(3);
    }

    private void agregaNombreMarcaAImagen(Canvas canvas, int x, int y, int tipo) {
        String tipoString;
        switch (tipo) {
            case Landmark.BOTTOM_MOUTH:
                tipoString = "BOTTOM_MOUTH";
                canvas.drawText(tipoString, x - (tipoString.length() * 4), y + 16, pinturaBordeLetra);
                canvas.drawText(tipoString, x - (tipoString.length() * 4), y + 16, pinturaLetra);
                break;
            case Landmark.LEFT_CHEEK:
                tipoString = "LEFT_CHEEK";
                canvas.drawText(tipoString, x + 5, y + 4, pinturaBordeLetra);
                canvas.drawText(tipoString, x + 5, y + 4, pinturaLetra);
                break;
            case Landmark.LEFT_EAR:
                tipoString = "LEFT_EAR";
                canvas.drawText(tipoString, x + 5, y + 4, pinturaBordeLetra);
                canvas.drawText(tipoString, x + 5, y + 4, pinturaLetra);
                break;
            case Landmark.LEFT_EAR_TIP:
                tipoString = "LEFT_EAR_TIP";
                canvas.drawText(tipoString, x + 5, y + 4, pinturaBordeLetra);
                canvas.drawText(tipoString, x + 5, y + 4, pinturaLetra);
                break;
            case Landmark.LEFT_EYE:
                tipoString = "LEFT_EYE";
                canvas.drawText(tipoString, x, y - 5, pinturaBordeLetra);
                canvas.drawText(tipoString, x, y - 5, pinturaLetra);
                break;
            case Landmark.LEFT_MOUTH:
                tipoString = "LEFT_MOUTH";
                canvas.drawText(tipoString, x + 5, y + 4, pinturaBordeLetra);
                canvas.drawText(tipoString, x + 5, y + 4, pinturaLetra);
                break;
            case Landmark.NOSE_BASE:
                tipoString = "NOSE_BASE";
                canvas.drawText(tipoString, x + 6, y + 4, pinturaBordeLetra);
                canvas.drawText(tipoString, x + 6, y + 4, pinturaLetra);
                break;
            case Landmark.RIGHT_CHEEK:
                tipoString = "RIGHT_CHEEK";
                canvas.drawText(tipoString, x - (tipoString.length() * 8), y + 4, pinturaBordeLetra);
                canvas.drawText(tipoString, x - (tipoString.length() * 8), y + 4, pinturaLetra);
                break;
            case Landmark.RIGHT_EAR:
                tipoString = "RIGHT_EAR";
                canvas.drawText(tipoString, x - (tipoString.length() * 8), y + 4, pinturaBordeLetra);
                canvas.drawText(tipoString, x - (tipoString.length() * 8), y + 4, pinturaLetra);
                break;
            case Landmark.RIGHT_EAR_TIP:
                tipoString = "RIGHT_EAR_TIP";
                canvas.drawText(tipoString, x - (tipoString.length() * 8), y + 4, pinturaBordeLetra);
                canvas.drawText(tipoString, x - (tipoString.length() * 8), y + 4, pinturaLetra);
                break;
            case Landmark.RIGHT_EYE:
                tipoString = "RIGHT_EYE";
                canvas.drawText(tipoString, x - (tipoString.length() * 7), y - 5, pinturaBordeLetra);
                canvas.drawText(tipoString, x - (tipoString.length() * 7), y - 5, pinturaLetra);
                break;
            case Landmark.RIGHT_MOUTH:
                tipoString = "RIGHT_MOUTH";
                canvas.drawText(tipoString, x - (tipoString.length() * 8), y + 4, pinturaBordeLetra);
                canvas.drawText(tipoString, x - (tipoString.length() * 8), y + 4, pinturaLetra);
                break;
        }
    }

    private void agregarMarcaATexto(StringBuilder mensajeConsola, int x, int y, int tipo) {
        mensajeConsola.append("\n\t\tLANDMARK: (" + x + ", " + y + "), type: ");
        String tipoString;

        switch (tipo) {
            case Landmark.BOTTOM_MOUTH:
                tipoString = "BOTTOM_MOUTH";
                break;
            case Landmark.LEFT_CHEEK:
                tipoString = "LEFT_CHEEK";
                break;
            case Landmark.LEFT_EAR:
                tipoString = "LEFT_EAR";
                break;
            case Landmark.LEFT_EAR_TIP:
                tipoString = "LEFT_EAR_TIP";
                break;
            case Landmark.LEFT_EYE:
                tipoString = "LEFT_EYE";
                break;
            case Landmark.LEFT_MOUTH:
                tipoString = "LEFT_MOUTH";
                break;
            case Landmark.NOSE_BASE:
                tipoString = "NOSE_BASE";
                break;
            case Landmark.RIGHT_CHEEK:
                tipoString = "RIGHT_CHEEK";
                break;
            case Landmark.RIGHT_EAR:
                tipoString = "RIGHT_EAR";
                break;
            case Landmark.RIGHT_EAR_TIP:
                tipoString = "RIGHT_EAR_TIP";
                break;
            case Landmark.RIGHT_EYE:
                tipoString = "RIGHT_EYE";
                break;
            case Landmark.RIGHT_MOUTH:
                tipoString = "RIGHT_MOUTH";
                break;
            default:
                tipoString = "Ninguno de los tipos reconocidos";
        }
        mensajeConsola.append(tipoString);

    }

    private void agregarImagenPorDefecto() {
        BitmapFactory.Options myOptions = new BitmapFactory.Options();
        myOptions.inDither = true;
        myOptions.inScaled = false;
        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important
        myOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.terry2, myOptions);

        imagen_original = Bitmap.createBitmap(bitmap);

        Bitmap imagen_mutable = imagen_original.copy(Bitmap.Config.ARGB_8888, true);
        imgFoto.setAdjustViewBounds(true);
        imgFoto.setImageBitmap(imagen_mutable);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 50) {
            imageUri = data.getData();
            try {
                imagen_original = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap imagen_mutable = imagen_original.copy(Bitmap.Config.ARGB_8888, true);
            imgFoto.setAdjustViewBounds(true);
            imgFoto.setImageBitmap(imagen_mutable);
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRotar:
                imagen_original = rotateBitmap(imagen_original, 90);
                Bitmap imagen_mutable = imagen_original.copy(Bitmap.Config.ARGB_8888, true);
                imgFoto.setAdjustViewBounds(true);
                imgFoto.setImageBitmap(imagen_mutable);
                return true;
            case R.id.menuMostrarMarcas:
                mostrarMarcasImagen = !mostrarMarcasImagen;
                return true;
            case R.id.menuMostrarConsola:
                mostrarMarcasConsola = !mostrarMarcasConsola;
                return true;
            case R.id.menuMostrarNombre:
                mostrarNombreMarcasImagen = !mostrarNombreMarcasImagen;
                return true;
            case R.id.menuMostrarRecuadro:
                mostrarRecuadroCaraImagen = !mostrarRecuadroCaraImagen;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
