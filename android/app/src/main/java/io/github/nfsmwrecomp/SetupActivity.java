package io.github.nfsmwrecomp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Pantalla de inicio
 */
public class SetupActivity extends AppCompatActivity {

    private static final int PEDIR_ISO = 1;
    private static final int PEDIR_DRIVER = 2;
    private static final int PEDIR_CARPETA = 3;

    // Fase 2 (identidade Evolved, tokens do site sem o tema sith): lima neon
    // sobre preto. O texto sobre o lima continua escuro (ver
    // estilarBotonPrimario).
    private static final int ACENTO = 0xFFBFFF00;
    private static final int GRIS = 0xFF8F959E;
    private static final int AVISO = 0xFFFFB74D;
    private static final int FUNDO = 0xFF0A0A0D;
    private static final int PAINEL_LINHA = 0xFF232329;
    private static final String ESTADO_AVANZADO = "avanzado_abierto";

    private Ajustes ajustes;
    private TextView textoIso;
    private TextView textoCarpeta;
    private TextView textoImportar;
    private TextView textoEdicion;
    private MaterialButton tabIso;
    private MaterialButton tabCarpeta;
    private MaterialButton botonIso;
    private MaterialButton botonCarpeta;
    private TextView avisoJugar;
    private RadioGroup grupoDrivers;
    private MaterialButton botonJugar;
    private TextView textoInforme;
    private View filaTurbo;
    private boolean avanzadoAbierto;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Idioma.envolver(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ajustes = new Ajustes(this);
        ajustes.aplicarPerfilGpuDebil(this);
        avanzadoAbierto = savedInstanceState != null
                && savedInstanceState.getBoolean(ESTADO_AVANZADO, false);
        setContentView(construir());
        // Una ISO elegida con una version anterior de la app: aun sin mirar.
        mirarIso();
        // Igual para la carpeta: si quedo a medias, se revalida.
        mirarCarpeta();
        bancoDePruebas(getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putBoolean(ESTADO_AVANZADO, avanzadoAbierto);
    }

    private void bancoDePruebas(Intent in) {
        if (in == null || !in.getBooleanExtra("nfsmw.banco", false)
                || (ajustes.iso() == null && ajustes.carpetaJuego() == null)) {
            return;
        }
        String driver = in.getStringExtra("nfsmw.driver");
        List<String> args = ajustes.argumentos(this, false, driver != null ? driver : ajustes.driver());
        String[] extra = in.getStringArrayExtra("nfsmw.args");
        if (extra != null) {
            for (String e : extra) {
                String nombre = nombreOpcion(e);
                args.removeIf(a -> nombreOpcion(a).equals(nombre));
            }
            args.addAll(Arrays.asList(extra));
        }
        int alto = in.getIntExtra("nfsmw.alto", -1);
        String resolucion = alto < 0 ? ajustes.resolucion()
                : alto == 480 ? Ajustes.RES_480P
                : alto == 720 ? Ajustes.RES_720P
                : Ajustes.RES_NATIVA;
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_ARGUMENTOS, args.toArray(new String[0]));
        if ("carpeta".equals(ajustes.fuenteJuego()) && ajustes.carpetaJuego() != null) {
            i.putExtra(GameActivity.EXTRA_CARPETA, ajustes.carpetaJuego());
        } else {
            i.putExtra(GameActivity.EXTRA_ISO, ajustes.iso());
        }
        i.putExtra(GameActivity.EXTRA_RESOLUCION, resolucion);
        i.putExtra(GameActivity.EXTRA_ESTIRAR, ajustes.estirar());
        i.putExtra(GameActivity.EXTRA_MOSTRAR_FPS, true);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrescar();
    }

    private View espacio(int altoDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(altoDp)));
        return v;
    }

    private MaterialCardView tarjeta(View... vistas) {
        MaterialCardView tarjeta = new MaterialCardView(this);
        tarjeta.setCardBackgroundColor(0xFF0E0E12);
        tarjeta.setRadius(dp(16));
        tarjeta.setCardElevation(dp(4));
        tarjeta.setUseCompatPadding(true);
        tarjeta.setStrokeWidth(dp(1));
        tarjeta.setStrokeColor(PAINEL_LINHA);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Filete de acento no topo, como o ::before dos paineis do site.
        View filete = new View(this);
        filete.setBackgroundColor(ACENTO);
        LinearLayout.LayoutParams lpFilete = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        lpFilete.bottomMargin = dp(10);
        layout.addView(filete, lpFilete);

        for (View v : vistas) {
            if (v != null) layout.addView(v);
        }

        tarjeta.addView(layout);
        return tarjeta;
    }

    private void estilarBotonPrimario(MaterialButton b) {
        b.setCornerRadius(dp(12));
        b.setBackgroundTintList(segunActivado(0xFF3A3A3A, ACENTO));
        b.setTextColor(segunActivado(0xFF808080, 0xFF121212));
    }

    private void estilarBotonSecundario(MaterialButton b) {
        b.setCornerRadius(dp(10));
        b.setBackgroundTintList(ColorStateList.valueOf(0xFF2C2C2C));
        b.setStrokeWidth(dp(1));
        b.setStrokeColor(ColorStateList.valueOf(PAINEL_LINHA));
        b.setTextColor(Color.WHITE);
        b.setLetterSpacing(0.06f);
    }

    /** Aba ativa = lima solida; inativa = contorno sutil (pills do site). */
    private void estilarAba(MaterialButton b, boolean ativa) {
        b.setCornerRadius(dp(10));
        if (ativa) {
            estilarBotonPrimario(b);
        } else {
            b.setBackgroundTintList(ColorStateList.valueOf(0x00000000));
            b.setStrokeWidth(dp(1));
            b.setStrokeColor(ColorStateList.valueOf(0xFF3A3A42));
            b.setTextColor(GRIS);
        }
        b.setLetterSpacing(0.1f);
    }

    private void estilarBotonPeligro(MaterialButton b) {
        b.setCornerRadius(dp(10));
        b.setBackgroundTintList(ColorStateList.valueOf(0xFF4A1A1A));
        b.setTextColor(0xFFFF6666);
    }

    @SuppressWarnings("deprecation")
    private View construir() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int m = dp(12);
        col.setPadding(m, m, m, m);
        col.setBackgroundColor(FUNDO);

        // --- Marca (cartao de identidade, como no app modificado)
        TextView marca = new TextView(this);
        marca.setText("NFSMW");
        marca.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        marca.setTypeface(Typeface.DEFAULT_BOLD);
        marca.setTextColor(Color.WHITE);
        marca.setLetterSpacing(0.06f);
        marca.setPadding(dp(8), dp(16), dp(8), 0);

        TextView marcaSub = resumen(0);
        marcaSub.setText(getString(R.string.marca_sub, BuildConfig.EDICION));
        marcaSub.setLetterSpacing(0.18f);
        marcaSub.setTypeface(Typeface.MONOSPACE);

        TextView tResumen = resumen(BuildConfig.CON_JUEGO ? R.string.apk_con_juego : R.string.apk_sonda);
        tResumen.setPadding(dp(8), 0, dp(8), dp(4));
        col.addView(tarjeta(marca, marcaSub, tResumen));

        // --- Juego: ISO o carpeta extraida (abas, como no app modificado)
        textoIso = new TextView(this);
        textoIso.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textoIso.setTextColor(Color.WHITE);
        textoIso.setTypeface(Typeface.MONOSPACE);

        textoCarpeta = new TextView(this);
        textoCarpeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textoCarpeta.setTextColor(Color.WHITE);
        textoCarpeta.setTypeface(Typeface.MONOSPACE);

        // Para que edicion del juego es el APK y, si a fonte e de outra, o aviso.
        textoEdicion = resumen(0);
        textoEdicion.setPadding(0, dp(4), 0, 0);
        textoEdicion.setTypeface(Typeface.MONOSPACE);

        textoImportar = resumen(0);
        textoImportar.setPadding(0, dp(4), 0, 0);

        tabIso = new MaterialButton(this);
        tabIso.setText("ISO");
        tabIso.setLetterSpacing(0.1f);
        tabIso.setOnClickListener(v -> {
            ajustes.fuenteJuego("iso");
            pintarAbas();
            refrescar();
        });
        tabCarpeta = new MaterialButton(this);
        tabCarpeta.setText(R.string.aba_carpeta);
        tabCarpeta.setLetterSpacing(0.1f);
        tabCarpeta.setOnClickListener(v -> {
            ajustes.fuenteJuego("carpeta");
            pintarAbas();
            refrescar();
        });
        LinearLayout abas = new LinearLayout(this);
        abas.setOrientation(LinearLayout.HORIZONTAL);
        abas.addView(tabIso, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        abas.addView(tabCarpeta, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton elegirIso = new MaterialButton(this);
        elegirIso.setText(R.string.elegir_iso);
        estilarBotonSecundario(elegirIso);
        elegirIso.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, PEDIR_ISO);
        });
        botonIso = elegirIso;

        botonCarpeta = new MaterialButton(this);
        botonCarpeta.setText(R.string.elegir_carpeta);
        estilarBotonSecundario(botonCarpeta);
        botonCarpeta.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            String arbol = ajustes.arbolCarpeta();
            if (arbol != null && Build.VERSION.SDK_INT >= 26) {
                i.putExtra("android.provider.extra.INITIAL_URI", Uri.parse(arbol));
            }
            startActivityForResult(i, PEDIR_CARPETA);
        });

        botonJugar = new MaterialButton(this);
        botonJugar.setText(R.string.jugar);
        botonJugar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        botonJugar.setTypeface(Typeface.DEFAULT_BOLD);
        botonJugar.setLetterSpacing(0.12f);
        botonJugar.setMinHeight(dp(64));
        estilarBotonPrimario(botonJugar);
        botonJugar.setOnClickListener(v -> lanzar(false));
        
        LinearLayout.LayoutParams lpJugar = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpJugar.topMargin = dp(16);
        
        avisoJugar = resumen(0);
        avisoJugar.setPadding(0, dp(8), 0, 0);

        LinearLayout boxJugar = new LinearLayout(this);
        boxJugar.setOrientation(LinearLayout.VERTICAL);
        boxJugar.addView(botonJugar, lpJugar);
        boxJugar.addView(avisoJugar);
        
        col.addView(tarjeta(
            seccion(R.string.fonte_jogo),
            abas,
            textoIso,
            textoCarpeta,
            textoEdicion,
            textoImportar,
            espacio(8),
            elegirIso,
            botonCarpeta,
            boxJugar,
            botaoSobre()
        ));

        // --- Pantalla
        col.addView(tarjeta(
            seccion(R.string.seccion_pantalla),
            selectorResolucion(),
            interruptor(R.string.estirar, R.string.estirar_resumen, ajustes.estirar(), ajustes::estirar),
            interruptor(R.string.mostrar_fps, R.string.mostrar_fps_resumen, ajustes.mostrarFps(), ajustes::mostrarFps)
        ));

        // --- Controles tactiles: el editor va en su propia pantalla
        MaterialButton editarTactil = new MaterialButton(this);
        editarTactil.setText(R.string.editar_tactil);
        estilarBotonSecundario(editarTactil);
        editarTactil.setOnClickListener(v -> startActivity(new Intent(this, EditorTactilActivity.class)));
        MaterialButton probarVibracion = new MaterialButton(this);
        probarVibracion.setText(R.string.probar_vibracion);
        estilarBotonSecundario(probarVibracion);
        TextView vibracionProbada = resumen(0);
        vibracionProbada.setVisibility(View.GONE);
        probarVibracion.setOnClickListener(v -> probarVibracion(vibracionProbada));
        LinearLayout filaVibracion = new LinearLayout(this);
        filaVibracion.setOrientation(LinearLayout.VERTICAL);
        filaVibracion.addView(interruptor(R.string.vibracion, R.string.vibracion_resumen,
                ajustes.vibracion(), ajustes::vibracion));
        filaVibracion.addView(probarVibracion);
        filaVibracion.addView(vibracionProbada);
        col.addView(tarjeta(
            seccion(R.string.seccion_tactil),
            resumen(R.string.editar_tactil_resumen),
            espacio(8),
            editarTactil,
            soloNativo(filaVibracion),
            interruptor(R.string.girar_inclinando, R.string.girar_inclinando_resumen,
                    ajustes.girarInclinando(), ajustes::girarInclinando),
            deslizador(R.string.sensibilidad_giro, R.string.sensibilidad_giro_resumen,
                    Ajustes.SENSIBILIDAD_MIN, Ajustes.SENSIBILIDAD_MAX, ajustes.sensibilidadGiro(),
                    s -> getString(R.string.sensibilidad_valor, s, Math.round(Ajustes.anguloGiro(s))),
                    ajustes::sensibilidadGiro)
        ));

        // --- Perfil de Xbox 360
        col.addView(tarjeta(
            seccion(R.string.seccion_perfil),
            resumen(R.string.gamertag_resumen),
            espacio(8),
            campoGamertag()
        ));

        // --- Graficos
        col.addView(tarjeta(
            seccion(R.string.seccion_graficos),
            soloNativo(selector(R.string.resolucion_interna, R.string.resolucion_interna_resumen,
                    Arrays.asList(getString(R.string.res_interna_576), getString(R.string.res_interna_720),
                            getString(R.string.res_interna_1080)),
                    Arrays.asList("1024x576", "1280x720", "1920x1080"), ajustes.resolucionInterna(), false,
                    ajustes::resolucionInterna)),
            soloNativo(desplegable(R.string.limite_fps, R.string.limite_fps_resumen,
                    Arrays.asList(getString(R.string.fps_valor, 30), getString(R.string.fps_valor, 60),
                            getString(R.string.fps_experimental, 90), getString(R.string.fps_experimental, 120),
                            getString(R.string.fps_sin_limite)),
                    Arrays.asList(30, 60, 90, 120, Ajustes.FPS_SIN_LIMITE), ajustes.limiteFps(),
                    ajustes::limiteFps)),
            selectorSuavizado(),
            selector(R.string.escalado, R.string.escalado_resumen,
                    Arrays.asList(getString(R.string.escalado_bilineal), getString(R.string.escalado_fsr),
                            getString(R.string.escalado_cas)),
                    Arrays.asList("bilinear", "fsr", "cas"), ajustes.escalado(), false, ajustes::escalado),
            soloXenos(interruptor(R.string.posprocesado, R.string.posprocesado_resumen, !ajustes.sinPosprocesado(), v -> ajustes.sinPosprocesado(!v))),
            selector(R.string.anisotropico, R.string.anisotropico_resumen,
                    Arrays.asList(getString(R.string.aniso_no), getString(R.string.aniso_2), getString(R.string.aniso_4), getString(R.string.aniso_16)),
                    Arrays.asList(0, 2, 3, 5), ajustes.anisotropico(), true, ajustes::anisotropico),
            soloXenos(selector(R.string.escala, R.string.escala_resumen,
                    Arrays.asList(getString(R.string.escala_valor, 1), getString(R.string.escala_valor, 2)),
                    Arrays.asList(1, 2), ajustes.escala(), true, ajustes::escala)),
            soloNativo(selector(R.string.calidad_sombras, R.string.calidad_sombras_resumen,
                    Arrays.asList(getString(R.string.sombras_rapida), getString(R.string.sombras_suave),
                            getString(R.string.sombras_xbox360)),
                    Arrays.asList(Ajustes.SOMBRAS_RAPIDA, Ajustes.SOMBRAS_SUAVE, Ajustes.SOMBRAS_XBOX),
                    ajustes.calidadSombras(), true, ajustes::calidadSombras)),
            soloNativo(desplegable(R.string.resolucion_sombras, 0,
                    Arrays.asList(getString(R.string.sombras_nativa), getString(R.string.sombras_alta),
                            getString(R.string.sombras_muy_alta), getString(R.string.sombras_ultra)),
                    Arrays.asList(100, 150, 200, 250), ajustes.sombrasEscala(), ajustes::sombrasEscala)),
            soloNativo(selector(R.string.sombras, R.string.sombras_resumen,
                    Arrays.asList(getString(R.string.sombras_siempre), getString(R.string.sombras_cada_2)),
                    Arrays.asList(1, 2), ajustes.sombrasCada(), true, ajustes::sombrasCada)),
            soloNativo(selector(R.string.sombras_distancia, R.string.sombras_distancia_resumen,
                    Arrays.asList(getString(R.string.sombras_lejos), getString(R.string.sombras_media),
                            getString(R.string.sombras_cerca)),
                    Arrays.asList(100, 150, 200), ajustes.sombrasCorte(), true, ajustes::sombrasCorte)),
            soloNativo(selector(R.string.reflejos_coche, R.string.reflejos_coche_resumen,
                    Arrays.asList(getString(R.string.calidad_alta), getString(R.string.calidad_media),
                            getString(R.string.calidad_baja)),
                    Arrays.asList(6, 2, 1), ajustes.reflejosCoche(), true, ajustes::reflejosCoche)),
            soloNativo(interruptor(R.string.reflejo_asfalto, R.string.reflejo_asfalto_resumen,
                    ajustes.reflejoAsfalto(), ajustes::reflejoAsfalto)),
            soloNativo(selector(R.string.resplandor_cielo, R.string.resplandor_cielo_resumen,
                    Arrays.asList(getString(R.string.cielo_natural), getString(R.string.cielo_original),
                            getString(R.string.cielo_suave)),
                    Arrays.asList("natural", "original", "suave"), ajustes.resplandorCielo(), true,
                    ajustes::resplandorCielo)),
            soloNativo(selector(R.string.filtro_color, R.string.filtro_color_resumen,
                    Arrays.asList(getString(R.string.filtro_color_original), getString(R.string.filtro_color_suave),
                            getString(R.string.filtro_color_apagado)),
                    Arrays.asList("original", "suave", "apagado"), ajustes.filtroColor(), true,
                    ajustes::filtroColor)),
            soloNativo(interruptor(R.string.desenfoque, R.string.desenfoque_resumen,
                    ajustes.desenfoque(), ajustes::desenfoque)),
            soloNativo(desplegable(R.string.filtro_imagen, R.string.filtro_imagen_resumen,
                    Arrays.asList(getString(R.string.filtro_imagen_no), getString(R.string.filtro_imagen_cine),
                            getString(R.string.filtro_imagen_vivo), getString(R.string.filtro_imagen_calido),
                            getString(R.string.filtro_imagen_frio), getString(R.string.filtro_imagen_sepia),
                            getString(R.string.filtro_imagen_noir), getString(R.string.filtro_imagen_crt)),
                    Arrays.asList("apagado", "cine", "vivo", "calido", "frio", "sepia", "noir", "crt"),
                    ajustes.filtroImagen(), ajustes::filtroImagen))
        ));

        // --- Sonido (motor nativo: por donde sale el audio)
        col.addView(soloNativo(tarjeta(
            seccion(R.string.seccion_sonido),
            interruptor(R.string.audio_aaudio, R.string.audio_aaudio_resumen,
                    ajustes.audioAAudio(), ajustes::audioAAudio)
        )));

        // --- Rendimiento
        grupoDrivers = new RadioGroup(this);
        LinearLayout filaDriver = new LinearLayout(this);
        filaDriver.setOrientation(LinearLayout.HORIZONTAL);
        filaDriver.setPadding(0, dp(8), 0, dp(8));
        
        MaterialButton importar = new MaterialButton(this);
        importar.setText(R.string.importar_driver);
        estilarBotonSecundario(importar);
        importar.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            startActivityForResult(i, PEDIR_DRIVER);
        });
        
        MaterialButton borrar = new MaterialButton(this);
        borrar.setText(R.string.borrar_driver);
        estilarBotonPeligro(borrar);
        borrar.setOnClickListener(v -> {
            Drivers.Driver d = Drivers.buscar(this, ajustes.driver());
            if (d != null) {
                Drivers.eliminar(d);
                ajustes.driver(null);
                refrescar();
            }
        });
        
        LinearLayout.LayoutParams lpBotones = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpBotones.rightMargin = dp(8);
        filaDriver.addView(importar, lpBotones);
        LinearLayout.LayoutParams lpBorrar = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        filaDriver.addView(borrar, lpBorrar);
        
        filaTurbo = interruptor(R.string.turbo, R.string.turbo_resumen, ajustes.turbo(), ajustes::turbo);
        
        col.addView(tarjeta(
            seccion(R.string.seccion_rendimiento),
            interruptor(R.string.fijar_hilos, R.string.fijar_hilos_resumen, ajustes.fijarHilos(), ajustes::fijarHilos),
            titulo(R.string.driver),
            ayudaDriver(),
            grupoDrivers,
            filaDriver,
            filaTurbo
        ));

        // --- Idioma
        col.addView(tarjeta(
            seccion(R.string.seccion_idioma),
            selector(0, 0,
                    Arrays.asList(getString(R.string.idioma_auto), getString(R.string.idioma_en),
                            getString(R.string.idioma_es), getString(R.string.idioma_pt)),
                    Arrays.asList(Idioma.CODIGOS), Idioma.elegido(this), false, codigo -> {
                        Idioma.elegir(this, codigo);
                        recreate();
                    })
        ));

        // --- Avanzado
        LinearLayout cabecera = seccion(R.string.seccion_avanzado);
        TextView textoCabecera = (TextView) cabecera.getChildAt(0);
        LinearLayout avanzado = new LinearLayout(this);
        avanzado.setOrientation(LinearLayout.VERTICAL);
        Runnable pintarPlegado = () -> {
            avanzado.setVisibility(avanzadoAbierto ? View.VISIBLE : View.GONE);
            textoCabecera.setText((avanzadoAbierto ? "▾ " : "▸ ") + getString(R.string.seccion_avanzado));
        };
        cabecera.setOnClickListener(v -> {
            avanzadoAbierto = !avanzadoAbierto;
            pintarPlegado.run();
        });
        pintarPlegado.run();
        
        MaterialButton sonda = new MaterialButton(this);
        sonda.setText(R.string.probar_vulkan);
        estilarBotonSecundario(sonda);
        sonda.setOnClickListener(v -> lanzar(true));
        
        textoInforme = new TextView(this);
        textoInforme.setTypeface(Typeface.MONOSPACE);
        textoInforme.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textoInforme.setTextColor(0xFFCCCCCC);
        textoInforme.setTextIsSelectable(true);
        textoInforme.setPadding(0, dp(12), 0, 0);
        
        avanzado.addView(resumen(R.string.avanzado_resumen));
        // Los de la GPU de Xbox 360 imitada: con el motor nativo no existen.
        avanzado.addView(soloXenos(interruptor(R.string.una_pasada, R.string.una_pasada_resumen, ajustes.unaPasada(), ajustes::unaPasada)));
        avanzado.addView(soloXenos(interruptor(R.string.edram_fsi, R.string.edram_fsi_resumen, ajustes.edramEnShader(), ajustes::edramEnShader)));
        avanzado.addView(soloXenos(interruptor(R.string.oclusion, R.string.oclusion_resumen, ajustes.oclusion(), ajustes::oclusion)));
        avanzado.addView(soloXenos(interruptor(R.string.exposicion, R.string.exposicion_resumen, ajustes.exposicionFiel(), ajustes::exposicionFiel)));
        avanzado.addView(soloXenos(interruptor(R.string.memexport, R.string.memexport_resumen, ajustes.lecturaMemexport(), ajustes::lecturaMemexport)));
        avanzado.addView(soloXenos(interruptor(R.string.paginas, R.string.paginas_resumen, ajustes.refrescarPaginas(), ajustes::refrescarPaginas)));
        // En release no hay registro: el interruptor no haria nada.
        if (BuildConfig.DEBUG) {
            avanzado.addView(interruptor(R.string.registro_detallado, R.string.registro_detallado_resumen, ajustes.registroDetallado(), ajustes::registroDetallado));
        }
        // Motor nativo: su emulacion de la GPU, para las GPU que no pueden con el renderizador.
        avanzado.addView(soloNativo(interruptor(R.string.compatibilidad, R.string.compatibilidad_resumen,
                ajustes.compatibilidad(), ajustes::compatibilidad)));
        avanzado.addView(espacio(16));
        avanzado.addView(titulo(R.string.probar_vulkan));
        avanzado.addView(resumen(R.string.probar_vulkan_resumen));
        avanzado.addView(espacio(8));
        avanzado.addView(sonda);
        avanzado.addView(textoInforme);

        col.addView(tarjeta(cabecera, avanzado));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF121212);
        scroll.addView(col);
        scroll.setOnApplyWindowInsetsListener((v, ins) -> {
            col.setPadding(m + ins.getSystemWindowInsetLeft(), m + ins.getSystemWindowInsetTop(),
                    m + ins.getSystemWindowInsetRight(), m + ins.getSystemWindowInsetBottom());
            return ins;
        });
        return scroll;
    }

    /**
     * El gamertag. Se guarda segun se escribe, pero solo si es valido para
     * Xbox 360 (Ajustes.gamertagValido); si no, se avisa y se queda el
     * ultimo bueno. Vacio = el de fabrica, "User".
     */
    private View campoGamertag() {
        TextInputLayout caja = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        caja.setHint(R.string.gamertag);
        caja.setCounterEnabled(true);
        caja.setCounterMaxLength(15);
        caja.setBoxStrokeColor(ACENTO);
        caja.setHintTextColor(ColorStateList.valueOf(ACENTO));
        TextInputEditText texto = new TextInputEditText(caja.getContext());
        texto.setSingleLine(true);
        texto.setTextColor(Color.WHITE);
        texto.setFilters(new InputFilter[] {new InputFilter.LengthFilter(15)});
        texto.setText(ajustes.gamertag());
        texto.setHint("User");
        texto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String g = s.toString().trim();
                if (Ajustes.gamertagValido(g)) {
                    caja.setError(null);
                    ajustes.gamertag(g);
                } else {
                    caja.setError(getString(R.string.gamertag_error));
                }
            }
        });
        caja.addView(texto);
        return caja;
    }

    @SuppressWarnings("deprecation")
    private View selectorResolucion() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int largo = Math.max(dm.widthPixels, dm.heightPixels);
        int corto = Math.min(dm.widthPixels, dm.heightPixels);
        List<String> textos = new ArrayList<>();
        List<String> valores = new ArrayList<>();
        if (corto > 480) {
            textos.add(getString(R.string.res_480p));
            valores.add(Ajustes.RES_480P);
        }
        if (corto > 720) {
            textos.add(getString(R.string.res_720p));
            valores.add(Ajustes.RES_720P);
        }
        textos.add(getString(R.string.res_nativa, largo, corto));
        valores.add(Ajustes.RES_NATIVA);
        String actual = valores.contains(ajustes.resolucion())
                ? ajustes.resolucion() : Ajustes.RES_NATIVA;
        return selector(R.string.resolucion, R.string.resolucion_resumen, textos, valores,
                actual, false, ajustes::resolucion);
    }

    /** Un ajuste que solo existe con el motor de Xenos: con el nativo no se muestra. */
    private View soloXenos(View v) {
        if (MotorNativo.ACTIVO) {
            v.setVisibility(View.GONE);
        }
        return v;
    }

    /** Y al reves. */
    private View soloNativo(View v) {
        if (!MotorNativo.ACTIVO) {
            v.setVisibility(View.GONE);
        }
        return v;
    }

    private View selectorSuavizado() {
        View opcionesMsaa = selector(R.string.msaa_muestras, R.string.msaa_muestras_resumen,
                Arrays.asList(getString(R.string.escala_valor, 2), getString(R.string.escala_valor, 4)),
                Arrays.asList(2, 4), ajustes.msaaMuestras(), true, ajustes::msaaMuestras);
        View opcionesFxaa = selector(R.string.fxaa_calidad, R.string.fxaa_calidad_resumen,
                Arrays.asList(getString(R.string.fxaa_normal), getString(R.string.fxaa_alta)),
                Arrays.asList(false, true), ajustes.fxaaAlta(), true, ajustes::fxaaAlta);
        
        opcionesMsaa.setPadding(dp(32), 0, 0, 0);
        opcionesFxaa.setPadding(dp(32), 0, 0, 0);
        Consumer<String> mostrar = aa -> {
            opcionesMsaa.setVisibility(Ajustes.AA_MSAA.equals(aa) ? View.VISIBLE : View.GONE);
            opcionesFxaa.setVisibility(Ajustes.AA_FXAA.equals(aa) ? View.VISIBLE : View.GONE);
        };
        mostrar.accept(ajustes.antialiasing());

        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        if (MotorNativo.ACTIVO) {
            // El motor nativo pinta la escena en una pasada, sin MSAA: solo hay
            // FXAA, y sin niveles.
            caja.addView(selector(R.string.suavizado, R.string.suavizado_resumen,
                    Arrays.asList(getString(R.string.aa_no), getString(R.string.aa_fxaa)),
                    Arrays.asList(Ajustes.AA_NO, Ajustes.AA_FXAA),
                    Ajustes.AA_FXAA.equals(ajustes.antialiasing()) ? Ajustes.AA_FXAA : Ajustes.AA_NO,
                    true, ajustes::antialiasing));
            return caja;
        }
        caja.addView(selector(R.string.suavizado, R.string.suavizado_resumen,
                Arrays.asList(getString(R.string.aa_no), getString(R.string.aa_msaa),
                        getString(R.string.aa_fxaa)),
                Arrays.asList(Ajustes.AA_NO, Ajustes.AA_MSAA, Ajustes.AA_FXAA),
                ajustes.antialiasing(), true, aa -> {
                    ajustes.antialiasing(aa);
                    mostrar.accept(aa);
                }));
        caja.addView(opcionesMsaa);
        caja.addView(opcionesFxaa);
        return caja;
    }

    private LinearLayout seccion(int texto) {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(0, dp(8), 0, dp(16));
        // Linha do titulo: tick de acento + titulo espacado (sec-head do site).
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(android.view.Gravity.CENTER_VERTICAL);
        View tick = new View(this);
        tick.setBackgroundColor(ACENTO);
        LinearLayout.LayoutParams lpTick = new LinearLayout.LayoutParams(dp(44), dp(2));
        lpTick.rightMargin = dp(12);
        linha.addView(tick, lpTick);
        TextView t = new TextView(this);
        t.setText(texto);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(ACENTO);
        t.setLetterSpacing(0.12f);
        linha.addView(t);
        caja.addView(linha);
        return caja;
    }

    /** Turnip es solo para Adreno: con otra GPU, la ayuda lo dice. */
    private TextView ayudaDriver() {
        TextView t = resumen(0);
        if (Gpu.familia(this) == Gpu.Familia.ADRENO) {
            t.setText(R.string.ayuda_driver);
        } else {
            String gpu = Gpu.nombre(this);
            t.setText(getString(R.string.ayuda_driver_otra,
                    gpu.isEmpty() ? getString(R.string.gpu_desconocida) : gpu));
        }
        return t;
    }

    private TextView titulo(int texto) {
        TextView t = new TextView(this);
        if (texto != 0) t.setText(texto);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Color.WHITE);
        t.setPadding(0, dp(12), 0, 0);
        return t;
    }

    /**
     * Un deslizador de enteros, con su valor escrito debajo del titulo
     * mientras se mueve. Se guarda en cada paso.
     */
    private View deslizador(int titulo, int resumen, int min, int max, int actual,
                            java.util.function.IntFunction<String> texto, Consumer<Integer> guardar) {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(0, dp(6), 0, dp(6));
        caja.addView(titulo(titulo));
        TextView valor = resumen(0);
        valor.setTextColor(Color.WHITE);
        valor.setText(texto.apply(actual));
        caja.addView(valor);
        Slider s = new Slider(this);
        s.setValueFrom(min);
        s.setValueTo(max);
        s.setStepSize(1);
        s.setValue(Math.max(min, Math.min(max, actual)));
        s.setLabelBehavior(LabelFormatter.LABEL_GONE);
        s.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        s.setTrackActiveTintList(ColorStateList.valueOf(ACENTO));
        s.setTrackInactiveTintList(ColorStateList.valueOf(0xFF303030));
        s.setHaloTintList(ColorStateList.valueOf(0x336F7432));
        s.addOnChangeListener((slider, v, delUsuario) -> {
            int n = Math.round(v);
            valor.setText(texto.apply(n));
            if (delUsuario) guardar.accept(n);
        });
        caja.addView(s, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (resumen != 0) caja.addView(resumen(resumen));
        return caja;
    }

    /**
     * Probar vibracion: cada mando fisico conectado vibra con su vibrador de Android, el
     * mismo que usa SDL en la partida, y se dice cuantos motores le da Android. Sin
     * motores ahi, en la partida tampoco puede vibrar. Sin mandos, vibra el movil.
     */
    private void probarVibracion(TextView salida) {
        StringBuilder texto = new StringBuilder();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d == null || d.isVirtual() || !TouchControllerView.esFuenteDeMando(d.getSources())) {
                continue;
            }
            String usb = String.format(Locale.ROOT, "%04X:%04X", d.getVendorId(), d.getProductId());
            int motores = 0;
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = d.getVibratorManager();
                for (int v : vm.getVibratorIds()) {
                    if (vibrarUnRato(vm.getVibrator(v))) motores++;
                }
            } else if (vibrarUnRato(d.getVibrator())) {
                motores = 1;
            }
            if (texto.length() > 0) texto.append('\n');
            if (motores > 0) {
                texto.append(getString(R.string.vibracion_mando_motores, d.getName(), usb, motores));
            } else if (esXboxUsb(d.getVendorId(), d.getProductId())) {
                texto.append(getString(R.string.vibracion_mando_xbox_usb, d.getName(), usb));
            } else {
                texto.append(getString(R.string.vibracion_mando_sin_motores, d.getName(), usb));
            }
        }
        if (texto.length() == 0) {
            Vibrator movil;
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                movil = vm == null ? null : vm.getDefaultVibrator();
            } else {
                movil = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }
            vibrarUnRato(movil);
            texto.append(getString(R.string.vibracion_sin_mandos));
        }
        salida.setText(texto);
        salida.setVisibility(View.VISIBLE);
    }

    /** Ese mando esta conectado por USB y es de Xbox: en la partida lo abre SDL. */
    private boolean esXboxUsb(int vendor, int producto) {
        UsbManager um = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (um == null) return false;
        for (UsbDevice u : um.getDeviceList().values()) {
            if (u.getVendorId() == vendor && u.getProductId() == producto
                    && TouchControllerView.esXboxUsb(u)) {
                return true;
            }
        }
        return false;
    }

    /** Medio segundo de vibracion; false si ese vibrador no existe. */
    private static boolean vibrarUnRato(Vibrator v) {
        if (v == null || !v.hasVibrator()) return false;
        try {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (RuntimeException e) {
            v.vibrate(500);
        }
        return true;
    }

    private TextView resumen(int texto) {
        TextView t = new TextView(this);
        if (texto != 0) {
            t.setText(texto);
        }
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setTextColor(GRIS);
        return t;
    }

    private View interruptor(int titulo, int resumen, boolean valor, Consumer<Boolean> guardar) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.VERTICAL);
        fila.setPadding(0, dp(10), 0, dp(6));
        MaterialSwitch s = new MaterialSwitch(this);
        s.setText(titulo);
        s.setTextColor(Color.WHITE);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        
        // Colores personalizados del toggle (Blanco al encender)
        ColorStateList thumbTint = new ColorStateList(
                new int[][] {{android.R.attr.state_checked}, {}},
                new int[] {Color.WHITE, 0xFF9E9E9E});
        ColorStateList trackTint = new ColorStateList(
                new int[][] {{android.R.attr.state_checked}, {}},
                new int[] {0xFF666666, 0xFF303030});
        s.setThumbTintList(thumbTint);
        s.setTrackTintList(trackTint);
        
        s.setChecked(valor);
        s.setOnCheckedChangeListener((b, v) -> guardar.accept(v));
        fila.addView(s, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView r = resumen(resumen);
        r.setPadding(0, 0, dp(64), 0);
        r.setOnClickListener(v -> {
            if (s.isEnabled()) {
                s.toggle();
            }
        });
        fila.addView(r);
        return fila;
    }

    private <T> View selector(int titulo, int resumen, List<String> textos, List<T> valores,
                              T actual, boolean horizontal, Consumer<T> guardar) {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(0, dp(12), 0, dp(12));
        if (titulo != 0) {
            caja.addView(titulo(titulo));
        }
        if (resumen != 0) {
            caja.addView(resumen(resumen));
        }
        // En fila solo si caben: con textos largos los botones se estrechan, el
        // texto se parte en varias lineas y la fila queda alta, con un hueco en
        // blanco debajo. Entonces, en columna.
        int total = 0;
        int mayor = 0;
        for (String t : textos) {
            total += t.length();
            mayor = Math.max(mayor, t.length());
        }
        boolean enFila = horizontal && total <= 30 && mayor <= 14;
        RadioGroup grupo = new RadioGroup(this);
        grupo.setOrientation(enFila ? RadioGroup.HORIZONTAL : RadioGroup.VERTICAL);
        grupo.setPadding(0, dp(8), 0, 0);
        for (int i = 0; i < textos.size(); i++) {
            MaterialRadioButton rb = new MaterialRadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(textos.get(i));
            rb.setTextColor(Color.WHITE);
            rb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            rb.setButtonTintList(new ColorStateList(
                new int[][] {{android.R.attr.state_checked}, {}},
                new int[] {ACENTO, 0xFF9E9E9E}
            ));
            rb.setTag(i);
            grupo.addView(rb);
            if (valores.get(i).equals(actual)) {
                rb.setChecked(true);
            }
        }
        grupo.setOnCheckedChangeListener((g, id) -> {
            MaterialRadioButton rb = g.findViewById(id);
            if (rb != null && rb.isChecked()) {
                guardar.accept(valores.get((Integer) rb.getTag()));
            }
        });
        caja.addView(grupo);
        return caja;
    }

    /**
     * Como selector(), pero en un menu desplegable: para las listas largas, que
     * en botones ocupan media pantalla.
     */
    private <T> View desplegable(int titulo, int resumen, List<String> textos, List<T> valores,
                                 T actual, Consumer<T> guardar) {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(0, dp(12), 0, dp(12));
        if (titulo != 0) {
            caja.addView(titulo(titulo));
        }
        if (resumen != 0) {
            caja.addView(resumen(resumen));
        }
        TextInputLayout campo = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle);
        campo.setBoxStrokeColor(ACENTO);
        MaterialAutoCompleteTextView lista = new MaterialAutoCompleteTextView(campo.getContext());
        // Solo se elige de la lista: ni teclado ni texto libre.
        lista.setInputType(InputType.TYPE_NULL);
        lista.setTextColor(Color.WHITE);
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos));
        int elegido = Math.max(0, valores.indexOf(actual));
        lista.setText(textos.get(elegido), false);
        lista.setOnItemClickListener((padre, vista, posicion, id) -> guardar.accept(valores.get(posicion)));
        campo.addView(lista, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // Despues de meter el campo: la flecha del menu necesita su AutoCompleteTextView.
        campo.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        campo.setEndIconTintList(ColorStateList.valueOf(Color.WHITE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        caja.addView(campo, lp);
        return caja;
    }

    private static ColorStateList segunActivado(int desactivado, int activado) {
        return new ColorStateList(
                new int[][] {{-android.R.attr.state_enabled}, {}},
                new int[] {desactivado, activado});
    }

    private void refrescar() {
        pintarAbas();
        boolean carpeta = "carpeta".equals(ajustes.fuenteJuego());
        String nombre = ajustes.nombreIso();
        textoIso.setText(ajustes.iso() == null
                ? getString(R.string.sin_iso, BuildConfig.EDICION)
                : getString(R.string.iso_elegida, nombre != null ? nombre : ajustes.iso()));
        String nombreCarpeta = ajustes.nombreCarpeta();
        textoCarpeta.setText(ajustes.carpetaJuego() == null
                ? getString(R.string.sin_carpeta, BuildConfig.EDICION)
                : getString(R.string.carpeta_elegida,
                        nombreCarpeta != null ? nombreCarpeta : ajustes.carpetaJuego()));
        // El APK lleva el codigo de una sola edicion del juego: se dice cual y,
        // si el default.xex de la fuente elegida es otro o le faltan archivos,
        // se avisa. No se impide jugar: lo que no se ha podido leer no
        // demuestra nada.
        String xex = carpeta ? ajustes.xexCarpeta() : ajustes.xexIso();
        boolean hayFuente = carpeta ? ajustes.carpetaJuego() != null : ajustes.iso() != null;
        boolean mirada = hayFuente && xex != null;
        boolean incompleta = mirada && (carpeta ? ajustes.carpetaFaltan() > 0 : ajustes.isoFaltan() > 0);
        boolean otraEdicion = mirada && !xex.isEmpty()
                && !BuildConfig.EDICION_XEX.isEmpty() && !xex.equals(BuildConfig.EDICION_XEX);
        if (incompleta) {
            if (carpeta) {
                textoEdicion.setText(getString(R.string.carpeta_incompleta));
            } else {
                textoEdicion.setText(getString(R.string.iso_incompleta,
                        Formatter.formatShortFileSize(this, ajustes.isoFaltan())));
            }
        } else {
            String texto = getString(
                    otraEdicion ? R.string.iso_otra_edicion : R.string.edicion_apk,
                    BuildConfig.EDICION);
            if (MotorNativo.ACTIVO && !otraEdicion) {
                texto += " " + getString(R.string.motor_nativo);
            }
            textoEdicion.setText(texto);
        }
        textoEdicion.setTextColor(incompleta || otraEdicion ? AVISO : GRIS);
        textoEdicion.setVisibility(BuildConfig.CON_JUEGO ? View.VISIBLE : View.GONE);
        boolean puedeJugar = BuildConfig.CON_JUEGO && hayFuente;
        botonJugar.setEnabled(puedeJugar);
        avisoJugar.setText(!BuildConfig.CON_JUEGO ? R.string.jugar_sin_juego
                : carpeta ? R.string.jugar_sin_carpeta : R.string.jugar_sin_iso);
        avisoJugar.setVisibility(puedeJugar ? View.GONE : View.VISIBLE);

        grupoDrivers.setOnCheckedChangeListener(null);
        grupoDrivers.removeAllViews();
        MaterialRadioButton sistema = new MaterialRadioButton(this);
        sistema.setId(View.generateViewId());
        String gpu = Gpu.nombre(this);
        sistema.setText(gpu.isEmpty() ? getString(R.string.driver_sistema_sin_gpu)
                : getString(R.string.driver_sistema, gpu));
        sistema.setTextColor(Color.WHITE);
        sistema.setButtonTintList(new ColorStateList(
            new int[][] {{android.R.attr.state_checked}, {}},
            new int[] {ACENTO, 0xFF9E9E9E}
        ));
        sistema.setTag(null);
        grupoDrivers.addView(sistema);
        MaterialRadioButton marcado = sistema;
        List<Drivers.Driver> drivers = Drivers.lista(this);
        for (Drivers.Driver d : drivers) {
            MaterialRadioButton rb = new MaterialRadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(d.nombre + "  (" + d.libreria + ")");
            rb.setTextColor(Color.WHITE);
            rb.setButtonTintList(new ColorStateList(
                new int[][] {{android.R.attr.state_checked}, {}},
                new int[] {ACENTO, 0xFF9E9E9E}
            ));
            rb.setTag(d.nombre);
            grupoDrivers.addView(rb);
            if (d.nombre.equals(ajustes.driver())) {
                marcado = rb;
            }
        }
        marcado.setChecked(true);
        grupoDrivers.setOnCheckedChangeListener((g, id) -> {
            MaterialRadioButton rb = g.findViewById(id);
            if (rb != null) {
                ajustes.driver((String) rb.getTag());
                actualizarTurbo();
            }
        });
        actualizarTurbo();

        File informe = Ajustes.informeSonda(this);
        if (informe.isFile()) {
            try {
                textoInforme.setText(new String(Files.readAllBytes(informe.toPath()),
                        StandardCharsets.UTF_8));
            } catch (IOException e) {
                textoInforme.setText("");
            }
        } else {
            textoInforme.setText(R.string.sin_informe);
        }
    }

    /** Botao Sobre: versao e creditos (a tela Sobre do app modificado). */
    private View botaoSobre() {
        MaterialButton sobre = new MaterialButton(this);
        sobre.setText(R.string.sobre);
        estilarBotonSecundario(sobre);
        sobre.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.sobre_titulo)
                .setMessage(getString(R.string.sobre_texto,
                        BuildConfig.VERSION_NAME, BuildConfig.EDICION))
                .setPositiveButton(android.R.string.ok, null)
                .show());
        return sobre;
    }

    /** Abas ISO | XEX/PASTA: pinta a ativa e mostra/esconde os controles de cada fonte. */
    private void pintarAbas() {
        boolean carpeta = "carpeta".equals(ajustes.fuenteJuego());
        estilarAba(tabIso, !carpeta);
        estilarAba(tabCarpeta, carpeta);
        textoIso.setVisibility(carpeta ? View.GONE : View.VISIBLE);
        botonIso.setVisibility(carpeta ? View.GONE : View.VISIBLE);
        textoCarpeta.setVisibility(carpeta ? View.VISIBLE : View.GONE);
        botonCarpeta.setVisibility(carpeta ? View.VISIBLE : View.GONE);
        textoImportar.setVisibility(carpeta ? View.VISIBLE : View.GONE);
    }

    private void actualizarTurbo() {
        // Solo con Turnip: es un ajuste de KGSL, la GPU de Adreno.
        Drivers.Driver d = Drivers.buscar(this, ajustes.driver());
        boolean propio = d != null && d.esTurnip();
        MaterialSwitch s = (MaterialSwitch) ((LinearLayout) filaTurbo).getChildAt(0);
        s.setOnCheckedChangeListener(null);
        s.setChecked(propio && ajustes.turbo());
        s.setOnCheckedChangeListener((b, v) -> ajustes.turbo(v));
        s.setEnabled(propio);
        ((LinearLayout) filaTurbo).getChildAt(1).setAlpha(propio ? 1f : 0.5f);
    }

    private void lanzar(boolean sonda) {
        if (sonda) {
            Ajustes.informeSonda(this).delete();
        }
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_ARGUMENTOS,
                ajustes.argumentos(this, sonda).toArray(new String[0]));
        if (!sonda) {
            if ("carpeta".equals(ajustes.fuenteJuego()) && ajustes.carpetaJuego() != null) {
                i.putExtra(GameActivity.EXTRA_CARPETA, ajustes.carpetaJuego());
            } else {
                i.putExtra(GameActivity.EXTRA_ISO, ajustes.iso());
            }
            i.putExtra(GameActivity.EXTRA_RESOLUCION, ajustes.resolucion());
            i.putExtra(GameActivity.EXTRA_ESTIRAR, ajustes.estirar());
            i.putExtra(GameActivity.EXTRA_MOSTRAR_FPS, ajustes.mostrarFps());
        }
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int peticion, int resultado, Intent datos) {
        super.onActivityResult(peticion, resultado, datos);
        if (resultado != RESULT_OK || datos == null || datos.getData() == null) {
            return;
        }
        Uri uri = datos.getData();
        if (peticion == PEDIR_ISO) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                Toast.makeText(this, R.string.iso_sin_permiso, Toast.LENGTH_LONG).show();
                return;
            }
            ajustes.iso(uri.toString());
            ajustes.nombreIso(nombreDe(uri));
            ajustes.isoSinMirar();
            ajustes.fuenteJuego("iso");
            mirarIso();
        } else if (peticion == PEDIR_CARPETA) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                Toast.makeText(this, R.string.carpeta_sin_permiso, Toast.LENGTH_LONG).show();
                return;
            }
            ajustes.arbolCarpeta(uri.toString());
            ajustes.fuenteJuego("carpeta");
            importarCarpeta(uri);
        } else if (peticion == PEDIR_DRIVER) {
            try {
                Drivers.Driver d = Drivers.importar(this, uri);
                ajustes.driver(d.nombre);
                Toast.makeText(this, getString(R.string.driver_importado, d.nombre),
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, getString(R.string.driver_error, e.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        }
        refrescar();
    }

    /**
     * Lee de la ISO elegida de que edicion es y si esta entera (IsoXex), si aun
     * no se sabe. Son unos megas de lectura: en otro hilo.
     */
    private void mirarIso() {
        final String iso = ajustes.iso();
        if (iso == null || ajustes.xexIso() != null) {
            return;
        }
        final Context app = getApplicationContext();
        new Thread(() -> {
            IsoXex.Info info = IsoXex.mirar(app, Uri.parse(iso));
            runOnUiThread(() -> {
                if (iso.equals(ajustes.iso())) {
                    // Vacio = no se pudo leer; asi no se reintenta en cada arranque.
                    ajustes.isoMirada(info != null ? info.xex : "", info != null ? info.faltan : 0);
                    if (!isFinishing() && !isDestroyed()) {
                        refrescar();
                    }
                }
            });
        }, "mirar-iso").start();
    }

    /**
     * Lee de la carpeta elegida de que edicion es y si esta entera (IsoXex),
     * si aun no se sabe. En otro hilo, como mirarIso.
     */
    private void mirarCarpeta() {
        final String ruta = ajustes.carpetaJuego();
        if (ruta == null || ajustes.xexCarpeta() != null) {
            return;
        }
        new Thread(() -> {
            IsoXex.Info info = IsoXex.mirarCarpeta(new File(ruta));
            runOnUiThread(() -> {
                if (ruta.equals(ajustes.carpetaJuego())) {
                    // Vacio = no se pudo leer; asi no se reintenta en cada arranque.
                    ajustes.carpetaMirada(info != null ? info.xex : "", info != null ? info.faltan : 0);
                    if (!isFinishing() && !isDestroyed()) {
                        refrescar();
                    }
                }
            });
        }, "mirar-carpeta").start();
    }

    /** La carpeta final do jogo importado (default.xex + NFS/ + Movies/). */
    private File carpetaJuegoFinal() {
        File externo = getExternalFilesDir(null);
        return new File(externo != null ? externo : getFilesDir(), "juego");
    }

    private static boolean carpetaValida(File carpeta) {
        return new File(carpeta, "default.xex").isFile()
                && new File(carpeta, "NFS").isDirectory()
                && new File(carpeta, "Movies").isDirectory();
    }

    /**
     * Copia a arvore SAF escolhida para o armazenamento do app, com staging
     * atomico: so troca se a copia passar na validacao. Em outro hilo, com
     * progresso no textoImportar. Igual ao importGameFolder do nfsmw-android.
     */
    private void importarCarpeta(Uri arbol) {
        botonCarpeta.setEnabled(false);
        textoImportar.setText(R.string.carpeta_copiando);
        new Thread(() -> {
            File destino = carpetaJuegoFinal();
            File pai = destino.getParentFile();
            File staging = new File(pai, ".juego.importing");
            File anterior = new File(pai, ".juego.previous");
            String erro = null;
            long copiados = 0;
            try {
                borrarArbol(staging);
                borrarArbol(anterior);
                DocumentFile fuente = DocumentFile.fromTreeUri(this, arbol);
                if (fuente == null || !fuente.isDirectory()) {
                    throw new IOException(getString(R.string.carpeta_no_abre));
                }
                copiados = copiarArbol(fuente, staging, staging.getCanonicalFile(), copiados);
                if (!carpetaValida(staging)) {
                    throw new IOException(getString(R.string.carpeta_invalida));
                }
                if (destino.exists()) {
                    if (!destino.renameTo(anterior)) {
                        throw new IOException(getString(R.string.carpeta_no_mueve));
                    }
                }
                if (!staging.renameTo(destino)) {
                    if (anterior.exists()) {
                        anterior.renameTo(destino);
                    }
                    throw new IOException(getString(R.string.carpeta_no_mueve));
                }
                borrarArbol(anterior);
            } catch (Exception e) {
                erro = e.getMessage();
                try {
                    borrarArbol(staging);
                } catch (IOException ignored) {
                }
            }
            final String msgErro = erro;
            final long total = copiados;
            runOnUiThread(() -> {
                botonCarpeta.setEnabled(true);
                if (msgErro != null) {
                    textoImportar.setText(getString(R.string.carpeta_error, msgErro));
                } else {
                    textoImportar.setText(getString(R.string.carpeta_copiada, total / (1024 * 1024)));
                    ajustes.carpetaJuego(destino.getAbsolutePath());
                    DocumentFile fuente = DocumentFile.fromTreeUri(this, arbol);
                    ajustes.nombreCarpeta(fuente != null && fuente.getName() != null
                            ? fuente.getName() : destino.getName());
                    ajustes.carpetaSinMirar();
                    mirarCarpeta();
                }
                if (!isFinishing() && !isDestroyed()) {
                    refrescar();
                }
            });
        }, "importar-carpeta").start();
    }

    private long copiarArbol(DocumentFile fuente, File destino, File raizSegura, long copiados)
            throws IOException {
        if (!destino.getCanonicalFile().toPath().startsWith(raizSegura.toPath())) {
            throw new IOException(getString(R.string.carpeta_ruta_mala));
        }
        if (!destino.mkdirs() && !destino.isDirectory()) {
            throw new IOException(getString(R.string.carpeta_no_crea, destino));
        }
        for (DocumentFile hijo : fuente.listFiles()) {
            String nombre = hijo.getName();
            if (nombre == null || nombre.isEmpty() || nombre.equals(".") || nombre.equals("..")
                    || nombre.contains("/") || nombre.contains("\\")) {
                throw new IOException(getString(R.string.carpeta_nombre_malo));
            }
            File meta = new File(destino, nombre);
            if (hijo.isDirectory()) {
                copiados = copiarArbol(hijo, meta, raizSegura, copiados);
            } else if (hijo.isFile()) {
                File padre = meta.getParentFile();
                if (padre == null || !padre.getCanonicalFile().toPath().startsWith(raizSegura.toPath())) {
                    throw new IOException(getString(R.string.carpeta_ruta_mala));
                }
                if (!padre.mkdirs() && !padre.isDirectory()) {
                    throw new IOException(getString(R.string.carpeta_no_crea, padre));
                }
                try (InputStream in = getContentResolver().openInputStream(hijo.getUri());
                     OutputStream out = new FileOutputStream(meta)) {
                    if (in == null) {
                        throw new IOException(getString(R.string.carpeta_no_lee, nombre));
                    }
                    byte[] bufer = new byte[1024 * 1024];
                    int n;
                    while ((n = in.read(bufer)) != -1) {
                        out.write(bufer, 0, n);
                        copiados += n;
                        if ((copiados & ((32L * 1024 * 1024) - 1)) < n) {
                            final long total = copiados;
                            runOnUiThread(() -> textoImportar.setText(
                                    getString(R.string.carpeta_copiando_mb, total / (1024 * 1024))));
                        }
                    }
                }
            }
        }
        return copiados;
    }

    private static void borrarArbol(File f) throws IOException {
        if (!f.exists()) {
            return;
        }
        File[] hijos = f.listFiles();
        if (hijos != null) {
            for (File h : hijos) {
                borrarArbol(h);
            }
        }
        if (!f.delete()) {
            throw new IOException("No se pudo borrar " + f.getName());
        }
    }

    private String nombreDe(Uri uri) {
        try (Cursor c = getContentResolver().query(uri,
                new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private static String nombreOpcion(String arg) {
        String n = arg.startsWith("--") ? arg.substring(2) : arg;
        int igual = n.indexOf('=');
        if (igual >= 0) {
            n = n.substring(0, igual);
        } else if (n.startsWith("no-")) {
            n = n.substring(3);
        }
        return n;
    }
}
