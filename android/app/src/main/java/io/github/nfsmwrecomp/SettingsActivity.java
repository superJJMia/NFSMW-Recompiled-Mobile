package io.github.nfsmwrecomp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * Ajustes: pantalla, controles de toque, perfil, graficos, sonido, rendimiento,
 * idioma y avanzado. La pantalla de inicio solo lleva la fuente del juego y el
 * boton de jugar; todo lo demas vive aqui.
 */
public class SettingsActivity extends PantallaBase {

    private static final int PEDIR_DRIVER = 2;

    private android.widget.RadioGroup grupoDrivers;
    private View filaTurbo;
    private TextView textoInforme;
    private LinearLayout avanzado;
    private TextView textoCabecera;
    private boolean avanzadoAbierto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        iniciarAjustes();
        if (savedInstanceState != null) {
            avanzadoAbierto = savedInstanceState.getBoolean("avanzado_abierto", false);
        }
        setContentView(construir());
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putBoolean("avanzado_abierto", avanzadoAbierto);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrescar();
    }

    private View construir() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int m = dp(16);
        col.setPadding(m, m, m, m);
        col.setBackgroundColor(FUNDO);

        // --- Barra superior: voltar + titulo
        MaterialButton volver = new MaterialButton(this);
        volver.setText(R.string.volver);
        estilarBotonSecundario(volver);
        volver.setOnClickListener(v -> finish());
        TextView titulo = new TextView(this);
        titulo.setText(R.string.configuracoes);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextColor(BLANCO);
        titulo.setLetterSpacing(0.06f);
        LinearLayout barra = new LinearLayout(this);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setGravity(android.view.Gravity.CENTER_VERTICAL);
        barra.addView(volver, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lpTitulo = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpTitulo.leftMargin = dp(12);
        barra.addView(titulo, lpTitulo);
        col.addView(barra);
        col.addView(espacio(12));

        // --- Pantalla
        col.addView(tarjeta(
                seccion(R.string.seccion_pantalla),
                selectorResolucion(),
                interruptor(R.string.estirar, R.string.estirar_resumen, ajustes.estirar(), ajustes::estirar),
                interruptor(R.string.mostrar_fps, R.string.mostrar_fps_resumen, ajustes.mostrarFps(), ajustes::mostrarFps)
        ));

        // --- Controles tactiles
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

        // --- Sonido
        col.addView(soloNativo(tarjeta(
                seccion(R.string.seccion_sonido),
                interruptor(R.string.audio_aaudio, R.string.audio_aaudio_resumen,
                        ajustes.audioAAudio(), ajustes::audioAAudio)
        )));

        // --- Rendimiento
        grupoDrivers = new android.widget.RadioGroup(this);
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
        textoCabecera = tituloDe(cabecera);
        avanzado = new LinearLayout(this);
        avanzado.setOrientation(LinearLayout.VERTICAL);
        cabecera.setOnClickListener(v -> {
            avanzadoAbierto = !avanzadoAbierto;
            pintarPlegado();
        });

        MaterialButton sonda = new MaterialButton(this);
        sonda.setText(R.string.probar_vulkan);
        estilarBotonSecundario(sonda);
        sonda.setOnClickListener(v -> lanzarSonda());

        textoInforme = new TextView(this);
        textoInforme.setTypeface(Typeface.MONOSPACE);
        textoInforme.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textoInforme.setTextColor(GRIS);
        textoInforme.setTextIsSelectable(true);
        textoInforme.setPadding(0, dp(12), 0, 0);

        avanzado.addView(resumen(R.string.avanzado_resumen));
        avanzado.addView(soloXenos(interruptor(R.string.una_pasada, R.string.una_pasada_resumen, ajustes.unaPasada(), ajustes::unaPasada)));
        avanzado.addView(soloXenos(interruptor(R.string.edram_fsi, R.string.edram_fsi_resumen, ajustes.edramEnShader(), ajustes::edramEnShader)));
        avanzado.addView(soloXenos(interruptor(R.string.oclusion, R.string.oclusion_resumen, ajustes.oclusion(), ajustes::oclusion)));
        avanzado.addView(soloXenos(interruptor(R.string.exposicion, R.string.exposicion_resumen, ajustes.exposicionFiel(), ajustes::exposicionFiel)));
        avanzado.addView(soloXenos(interruptor(R.string.memexport, R.string.memexport_resumen, ajustes.lecturaMemexport(), ajustes::lecturaMemexport)));
        avanzado.addView(soloXenos(interruptor(R.string.paginas, R.string.paginas_resumen, ajustes.refrescarPaginas(), ajustes::refrescarPaginas)));
        if (BuildConfig.DEBUG) {
            avanzado.addView(interruptor(R.string.registro_detallado, R.string.registro_detallado_resumen, ajustes.registroDetallado(), ajustes::registroDetallado));
        }
        avanzado.addView(soloNativo(interruptor(R.string.compatibilidad, R.string.compatibilidad_resumen,
                ajustes.compatibilidad(), ajustes::compatibilidad)));
        avanzado.addView(espacio(16));
        avanzado.addView(titulo(R.string.probar_vulkan));
        avanzado.addView(resumen(R.string.probar_vulkan_resumen));
        avanzado.addView(espacio(8));
        avanzado.addView(sonda);
        avanzado.addView(textoInforme);

        col.addView(tarjeta(cabecera, avanzado));
        pintarPlegado();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(FUNDO);
        scroll.addView(col);
        scroll.setOnApplyWindowInsetsListener((v, ins) -> {
            col.setPadding(m + ins.getSystemWindowInsetLeft(), m + ins.getSystemWindowInsetTop(),
                    m + ins.getSystemWindowInsetRight(), m + ins.getSystemWindowInsetBottom());
            return ins;
        });
        return scroll;
    }

    private void pintarPlegado() {
        if (avanzado == null) return;
        avanzado.setVisibility(avanzadoAbierto ? View.VISIBLE : View.GONE);
        textoCabecera.setText((avanzadoAbierto ? "▾ " : "▸ ") + getString(R.string.seccion_avanzado));
    }

    private void refrescar() {
        if (grupoDrivers == null) return;
        grupoDrivers.setOnCheckedChangeListener(null);
        grupoDrivers.removeAllViews();
        MaterialRadioButton sistema = new MaterialRadioButton(this);
        sistema.setId(View.generateViewId());
        String gpu = Gpu.nombre(this);
        sistema.setText(gpu.isEmpty() ? getString(R.string.driver_sistema_sin_gpu)
                : getString(R.string.driver_sistema, gpu));
        sistema.setTextColor(BLANCO);
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
            rb.setTextColor(BLANCO);
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

    private void actualizarTurbo() {
        Drivers.Driver d = Drivers.buscar(this, ajustes.driver());
        boolean propio = d != null && d.esTurnip();
        com.google.android.material.materialswitch.MaterialSwitch s =
                (com.google.android.material.materialswitch.MaterialSwitch)
                        ((LinearLayout) filaTurbo).getChildAt(0);
        s.setOnCheckedChangeListener(null);
        s.setChecked(propio && ajustes.turbo());
        s.setOnCheckedChangeListener((b, v) -> ajustes.turbo(v));
        s.setEnabled(propio);
        ((LinearLayout) filaTurbo).getChildAt(1).setAlpha(propio ? 1f : 0.5f);
    }

    private void lanzarSonda() {
        Ajustes.informeSonda(this).delete();
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_ARGUMENTOS,
                ajustes.argumentos(this, true).toArray(new String[0]));
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int peticion, int resultado, Intent datos) {
        super.onActivityResult(peticion, resultado, datos);
        if (peticion != PEDIR_DRIVER || resultado != RESULT_OK
                || datos == null || datos.getData() == null) {
            return;
        }
        try {
            Drivers.Driver d = Drivers.importar(this, datos.getData());
            ajustes.driver(d.nombre);
            Toast.makeText(this, getString(R.string.driver_importado, d.nombre),
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.driver_error, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
        refrescar();
    }
}
