package io.github.nfsmwrecomp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Base comum das telas de interface (inicio e configuracoes): a paleta do app
 * e os construtores de vistas (cartoes, secoes, selectores, interruptores...)
 * que as duas partilham. As subclasses so montam o seu proprio conteudo.
 */
public abstract class PantallaBase extends AppCompatActivity {

    // Paleta do app (inspirada no site de referencia; sem a tematica original
    // do tema, so a linguagem de cor: preto quase puro e vermelho neon).
    protected static final int ACENTO = 0xFFFF2222;        // vermelho neon
    protected static final int ACENTO_SUAVE = 0xFFFF5B5B;
    protected static final int ACENTO_FUERTE = 0xFF7A0D0D;
    protected static final int GRIS = 0xFF8F959E;
    protected static final int AVISO = 0xFFFFB74D;
    protected static final int FUNDO = 0xFF040405;
    protected static final int FUNDO_2 = 0xFF0A0A0D;
    protected static final int PAINEL = 0xFF0E0E12;
    protected static final int PAINEL_LINHA = 0xFF232329;
    protected static final int BLANCO = 0xFFECECEE;

    protected Ajustes ajustes;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Idioma.envolver(base));
    }

    /** Cria os ajustes e aplica o perfil automatico de GPU fraca. */
    protected void iniciarAjustes() {
        ajustes = new Ajustes(this);
        ajustes.aplicarPerfilGpuDebil(this);
    }

    protected int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    protected View espacio(int altoDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(altoDp)));
        return v;
    }

    /** Cartao escuro com filete de acento no topo (os paineis do site). */
    protected MaterialCardView tarjeta(View... vistas) {
        MaterialCardView tarjeta = new MaterialCardView(this);
        tarjeta.setCardBackgroundColor(PAINEL);
        tarjeta.setRadius(dp(16));
        tarjeta.setCardElevation(dp(4));
        tarjeta.setUseCompatPadding(true);
        tarjeta.setStrokeWidth(dp(1));
        tarjeta.setStrokeColor(ColorStateList.valueOf(PAINEL_LINHA));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));

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

    protected void estilarBotonPrimario(MaterialButton b) {
        b.setCornerRadius(dp(12));
        b.setBackgroundTintList(segunActivado(0xFF3A3A3A, ACENTO));
        b.setTextColor(segunActivado(0xFF808080, 0xFFFFFFFF));
        b.setLetterSpacing(0.08f);
    }

    protected void estilarBotonSecundario(MaterialButton b) {
        b.setCornerRadius(dp(10));
        b.setBackgroundTintList(ColorStateList.valueOf(0xFF16161B));
        b.setStrokeWidth(dp(1));
        b.setStrokeColor(ColorStateList.valueOf(PAINEL_LINHA));
        b.setTextColor(BLANCO);
        b.setLetterSpacing(0.06f);
    }

    /** Aba ativa = vermelho solido; inativa = contorno sutil. */
    protected void estilarAba(MaterialButton b, boolean ativa) {
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

    protected void estilarBotonPeligro(MaterialButton b) {
        b.setCornerRadius(dp(10));
        b.setBackgroundTintList(ColorStateList.valueOf(0xFF3A0A0A));
        b.setStrokeWidth(dp(1));
        b.setStrokeColor(ColorStateList.valueOf(ACENTO_FUERTE));
        b.setTextColor(ACENTO_SUAVE);
    }

    /** Cabecalho de secao: tick de acento + titulo espacado. */
    protected LinearLayout seccion(int texto) {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(0, dp(8), 0, dp(16));
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

    /** Ultimo TextView da linha de titulo devolvida por seccion() (apos o tick). */
    protected TextView tituloDe(LinearLayout seccion) {
        LinearLayout linha = (LinearLayout) seccion.getChildAt(0);
        return (TextView) linha.getChildAt(linha.getChildCount() - 1);
    }

    protected TextView titulo(int texto) {
        TextView t = new TextView(this);
        if (texto != 0) t.setText(texto);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(BLANCO);
        t.setPadding(0, dp(12), 0, 0);
        return t;
    }

    protected TextView resumen(int texto) {
        TextView t = new TextView(this);
        if (texto != 0) {
            t.setText(texto);
        }
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setTextColor(GRIS);
        return t;
    }

    protected View interruptor(int titulo, int resumen, boolean valor, Consumer<Boolean> guardar) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.VERTICAL);
        fila.setPadding(0, dp(10), 0, dp(6));
        MaterialSwitch s = new MaterialSwitch(this);
        s.setText(titulo);
        s.setTextColor(BLANCO);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        ColorStateList thumbTint = new ColorStateList(
                new int[][] {{android.R.attr.state_checked}, {}},
                new int[] {ACENTO, 0xFF9E9E9E});
        ColorStateList trackTint = new ColorStateList(
                new int[][] {{android.R.attr.state_checked}, {}},
                new int[] {0x66FF2222, 0xFF303030});
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

    protected <T> View selector(int titulo, int resumen, List<String> textos, List<T> valores,
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
            rb.setTextColor(BLANCO);
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

    protected <T> View desplegable(int titulo, int resumen, List<String> textos, List<T> valores,
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
        lista.setInputType(InputType.TYPE_NULL);
        lista.setTextColor(BLANCO);
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos));
        int elegido = Math.max(0, valores.indexOf(actual));
        lista.setText(textos.get(elegido), false);
        lista.setOnItemClickListener((padre, vista, posicion, id) -> guardar.accept(valores.get(posicion)));
        campo.addView(lista, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        campo.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        campo.setEndIconTintList(ColorStateList.valueOf(ACENTO_SUAVE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        caja.addView(campo, lp);
        return caja;
    }

    protected View deslizador(int titulo, int resumen, int min, int max, int actual,
                            IntFunction<String> texto, Consumer<Integer> guardar) {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setPadding(0, dp(6), 0, dp(6));
        caja.addView(titulo(titulo));
        TextView valor = resumen(0);
        valor.setTextColor(BLANCO);
        valor.setText(texto.apply(actual));
        caja.addView(valor);
        Slider s = new Slider(this);
        s.setValueFrom(min);
        s.setValueTo(max);
        s.setStepSize(1);
        s.setValue(Math.max(min, Math.min(max, actual)));
        s.setLabelBehavior(LabelFormatter.LABEL_GONE);
        s.setThumbTintList(ColorStateList.valueOf(BLANCO));
        s.setTrackActiveTintList(ColorStateList.valueOf(ACENTO));
        s.setTrackInactiveTintList(ColorStateList.valueOf(0xFF303030));
        s.setHaloTintList(ColorStateList.valueOf(0x33FF2222));
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

    protected View soloXenos(View v) {
        if (MotorNativo.ACTIVO) {
            v.setVisibility(View.GONE);
        }
        return v;
    }

    protected View soloNativo(View v) {
        if (!MotorNativo.ACTIVO) {
            v.setVisibility(View.GONE);
        }
        return v;
    }

    protected static ColorStateList segunActivado(int desactivado, int activado) {
        return new ColorStateList(
                new int[][] {{-android.R.attr.state_enabled}, {}},
                new int[] {desactivado, activado});
    }

    /**
     * El gamertag. Se guarda segun se escribe, pero solo si es valido para
     * Xbox 360 (Ajustes.gamertagValido); si no, se avisa y se queda el
     * ultimo bueno. Vacio = el de fabrica, "User".
     */
    protected View campoGamertag() {
        TextInputLayout caja = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        caja.setHint(R.string.gamertag);
        caja.setCounterEnabled(true);
        caja.setCounterMaxLength(15);
        caja.setBoxStrokeColor(ACENTO);
        caja.setHintTextColor(ColorStateList.valueOf(ACENTO_SUAVE));
        TextInputEditText texto = new TextInputEditText(caja.getContext());
        texto.setSingleLine(true);
        texto.setTextColor(BLANCO);
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
    protected View selectorResolucion() {
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

    protected View selectorSuavizado() {
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

    /** Turnip es solo para Adreno: con otra GPU, la ayuda lo dice. */
    protected TextView ayudaDriver() {
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

    /**
     * Probar vibracion: cada mando fisico conectado vibra con su vibrador de Android, el
     * mismo que usa SDL en la partida, y se dice cuantos motores le da Android. Sin
     * motores ahi, en la partida tampoco puede vibrar. Sin mandos, vibra el movil.
     */
    protected void probarVibracion(TextView salida) {
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

    protected static String nombreOpcion(String arg) {
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
