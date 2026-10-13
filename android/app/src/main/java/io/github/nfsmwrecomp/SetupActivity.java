package io.github.nfsmwrecomp;

import androidx.documentfile.provider.DocumentFile;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * Pantalla de inicio: elige la fuente del juego (ISO o carpeta), muestra de que
 * edicion es este APK y lanza la partida. El resto de ajustes viven en
 * SettingsActivity.
 */
public class SetupActivity extends PantallaBase {

    private static final int PEDIR_ISO = 1;
    private static final int PEDIR_CARPETA = 3;

    private TextView textoIso;
    private TextView textoCarpeta;
    private TextView textoImportar;
    private TextView textoEdicion;
    private MaterialButton tabIso;
    private MaterialButton tabCarpeta;
    private MaterialButton botonIso;
    private MaterialButton botonCarpeta;
    private TextView avisoJugar;
    private MaterialButton botonJugar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        iniciarAjustes();
        setContentView(construir());
        // Una ISO elegida con una version anterior de la app: aun sin mirar.
        mirarIso();
        // Igual para la carpeta: si quedo a medias, se revalida.
        mirarCarpeta();
        bancoDePruebas(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrescar();
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

    private View construir() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int m = dp(16);
        col.setPadding(m, m, m, m);
        col.setBackgroundColor(FUNDO);

        // --- Marca (cartao de identidade)
        TextView marca = new TextView(this);
        marca.setText("NFSMW");
        marca.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        marca.setTypeface(Typeface.DEFAULT_BOLD);
        marca.setTextColor(BLANCO);
        marca.setLetterSpacing(0.08f);
        marca.setPadding(dp(8), dp(12), dp(8), 0);

        TextView marcaSub = resumen(0);
        marcaSub.setText(getString(R.string.marca_sub, BuildConfig.EDICION));
        marcaSub.setLetterSpacing(0.18f);
        marcaSub.setTypeface(Typeface.MONOSPACE);
        marcaSub.setPadding(dp(8), 0, dp(8), dp(4));
        col.addView(tarjeta(marca, marcaSub));

        col.addView(espacio(12));

        // --- Juego: ISO o carpeta extraida
        textoIso = new TextView(this);
        textoIso.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        textoIso.setTextColor(BLANCO);
        textoIso.setTypeface(Typeface.MONOSPACE);

        textoCarpeta = new TextView(this);
        textoCarpeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        textoCarpeta.setTextColor(BLANCO);
        textoCarpeta.setTypeface(Typeface.MONOSPACE);

        textoEdicion = resumen(0);
        textoEdicion.setPadding(0, dp(6), 0, 0);
        textoEdicion.setTypeface(Typeface.MONOSPACE);

        textoImportar = resumen(0);
        textoImportar.setPadding(0, dp(6), 0, 0);

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

        col.addView(tarjeta(
                seccion(R.string.fonte_jogo),
                abas,
                textoIso,
                textoCarpeta,
                textoEdicion,
                textoImportar,
                espacio(10),
                elegirIso,
                botonCarpeta
        ));

        col.addView(espacio(12));

        // --- Pie: contenido desplazable arriba y abajo fijo con Jugar/Config.
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(FUNDO);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(FUNDO);
        scroll.addView(col);
        scroll.setOnApplyWindowInsetsListener((v, ins) -> {
            col.setPadding(m + ins.getSystemWindowInsetLeft(), m + ins.getSystemWindowInsetTop(),
                    m + ins.getSystemWindowInsetRight(), m + ins.getSystemWindowInsetBottom());
            return ins;
        });
        raiz.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // --- Jugar + Configuracoes, siempre a la vista.
        View linea = new View(this);
        linea.setBackgroundColor(PAINEL_LINHA);

        avisoJugar = resumen(0);
        avisoJugar.setPadding(0, 0, 0, dp(6));

        botonJugar = new MaterialButton(this);
        botonJugar.setText(R.string.jugar);
        botonJugar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        botonJugar.setTypeface(Typeface.DEFAULT_BOLD);
        botonJugar.setLetterSpacing(0.14f);
        botonJugar.setMinHeight(dp(68));
        estilarBotonPrimario(botonJugar);
        botonJugar.setOnClickListener(v -> lanzar(false));

        MaterialButton config = new MaterialButton(this);
        config.setText(R.string.configuracoes);
        config.setLetterSpacing(0.1f);
        config.setMinHeight(dp(68));
        estilarBotonSecundario(config);
        config.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.addView(botonJugar, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        fila.addView(config, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout pie = new LinearLayout(this);
        pie.setOrientation(LinearLayout.VERTICAL);
        pie.setBackgroundColor(FUNDO_2);
        pie.setPadding(dp(16), dp(8), dp(16), dp(16));
        pie.addView(linea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        pie.addView(avisoJugar);
        pie.addView(fila, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pie.setOnApplyWindowInsetsListener((v, ins) -> {
            pie.setPadding(dp(16) + ins.getSystemWindowInsetLeft(), dp(8),
                    dp(16) + ins.getSystemWindowInsetRight(),
                    dp(16) + ins.getSystemWindowInsetBottom());
            return ins;
        });
        raiz.addView(pie);

        return raiz;
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

    private void refrescar() {
        if (textoIso == null) return;
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
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            IsoXex.Info info = IsoXex.mirar(app, Uri.parse(iso));
            runOnUiThread(() -> {
                if (iso.equals(ajustes.iso())) {
                    ajustes.isoMirada(info != null ? info.xex : "", info != null ? info.faltan : 0);
                    if (!isFinishing() && !isDestroyed()) {
                        refrescar();
                    }
                }
            });
        }, "mirar-iso").start();
    }

    /**
     * Lee de la carpeta elegida de que edicion es y si esta entera (IsoXex), si
     * aun no se sabe. En otro hilo, como mirarIso.
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
     * progresso no textoImportar.
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
}
