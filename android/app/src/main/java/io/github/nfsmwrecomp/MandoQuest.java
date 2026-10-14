package io.github.nfsmwrecomp;

import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.Locale;

/**
 * Mando nativo del Meta Quest (build "-quest").
 *
 * El sistema del Quest puede simular un gamepad de Xbox como un dispositivo de
 * entrada VIRTUAL para las apps que lo pidan. Android le entrega los eventos a
 * la activity (por eso funciona en Ajustes), pero SDL no registra ese
 * dispositivo como joystick -solo mira /dev/input- y descarta sus eventos en
 * silencio. Aqui se capturan en Java y se inyectan en el joystick virtual que
 * ya usa el control tactil (TouchControllerBridge), asi el juego sigue viendo
 * un unico mando y no hay que tocar SDL.
 *
 * Solo se instancia en GameActivity cuando BuildConfig.ESCENARIO_VR es true.
 * En la edicion normal esta clase existe pero no recibe nunca eventos: su
 * estado (activo()) es siempre falso, y el juego se comporta exactamente igual.
 */
final class MandoQuest {

    // Misma disposicion de botones que TouchControllerView (15 botones en el
    // joystick virtual nativo), para que el juego lea la misma cara de Xbox.
    private static final int A = 1 << 0;
    private static final int B = 1 << 1;
    private static final int X = 1 << 2;
    private static final int Y = 1 << 3;
    private static final int BACK = 1 << 4;
    private static final int START = 1 << 6;
    private static final int L3 = 1 << 7;
    private static final int R3 = 1 << 8;
    private static final int LB = 1 << 9;
    private static final int RB = 1 << 10;
    private static final int DPAD_UP = 1 << 11;
    private static final int DPAD_DOWN = 1 << 12;
    private static final int DPAD_LEFT = 1 << 13;
    private static final int DPAD_RIGHT = 1 << 14;

    /** Lo ultimo que se toco en el pad; con ello se rehace el envio completo. */
    private int mascara;
    private float lx, ly, rx, ry, lt, rt;

    // Cuando llega el ultimo evento del pad, para que el control tactil no
    // mande un "todo suelto" encima mientras seesta jugando con los Quest.
    private static volatile long ultimaActividad;

    // Diagnostico (gradlew ... -Pnfsmw.registro=true): deja en logcat lo que
    // llega de verdad desde el pad, para saber si el sistema lo esta entregando.
    private static final String TAG = "nfsmw";
    private static final boolean REGISTRO = BuildConfig.REGISTRO;
    private long ultimoLog;

    /** Falso en la edicion normal: no hay pad de Quest y el toque no cambia nada. */
    static boolean activo() {
        return SystemClock.uptimeMillis() - ultimaActividad < 2000L;
    }

    /** Ese dispositivo de entrada es el pad de XBOX que simula el Quest. */
    boolean esDeQuest(InputDevice d) {
        if (d == null) return false;
        int fuentes = d.getSources();
        if ((fuentes & (InputDevice.SOURCE_GAMEPAD | InputDevice.SOURCE_JOYSTICK)) == 0) {
            return false;
        }
        if (d.isVirtual()) return true;                 // el simulado de Meta es virtual
        if (d.getVendorId() == 0x2833) return true;     // Meta/Oculus
        String nombre = d.getName();
        if (nombre != null) {
            String n = nombre.toLowerCase(Locale.ROOT);
            if (n.contains("quest") || n.contains("oculus") || n.contains("meta")
                    || n.contains("touch controller")) {
                return true;
            }
        }
        return false;
    }

    /** Boton del pad del Quest. Devuelve true si se consumio. */
    boolean mandaBoton(KeyEvent e) {
        ultimaActividad = SystemClock.uptimeMillis();
        int bit = bitDeBoton(e.getKeyCode());
        if (bit == 0) return false;
        int accion = e.getAction();
        if (accion == KeyEvent.ACTION_DOWN) mascara |= bit;
        else if (accion == KeyEvent.ACTION_UP) mascara &= ~bit;
        else return true;
        if (REGISTRO && accion == KeyEvent.ACTION_DOWN) {
            Log.i(TAG, "Quest boton: code=" + e.getKeyCode() + " bit=0x"
                    + Integer.toHexString(bit) + " dev=" + nombre(e.getDevice())
                    + " mascara=0x" + Integer.toHexString(mascara));
        }
        enviar();
        return true;
    }

    /** Sticks, gatillos y cruceta (llegan como movimiento, no como boton). */
    boolean mandaMovimiento(MotionEvent e) {
        ultimaActividad = SystemClock.uptimeMillis();

        // Cruceta: los Quest la mandan por ejes HAT... y a veces por botones.
        // Se pinta sobre la mascara sin pisar lo que haya llegado por botones.
        final int sinCruceta = mascara & ~(DPAD_UP | DPAD_DOWN | DPAD_LEFT | DPAD_RIGHT);
        final float hx = e.getAxisValue(MotionEvent.AXIS_HAT_X);
        final float hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y);
        int nueva = sinCruceta;
        if (hy < -0.5f) nueva |= DPAD_UP;
        else if (hy > 0.5f) nueva |= DPAD_DOWN;
        if (hx < -0.5f) nueva |= DPAD_LEFT;
        else if (hx > 0.5f) nueva |= DPAD_RIGHT;
        boolean cambio = nueva != mascara;
        mascara = nueva;

        // Sticks y gatillos, en el mismo orden que el toque (-1..1 y 0..1).
        float v = e.getAxisValue(MotionEvent.AXIS_X);
        if (v != lx) { lx = v; cambio = true; }
        v = e.getAxisValue(MotionEvent.AXIS_Y);
        if (v != ly) { ly = v; cambio = true; }
        v = e.getAxisValue(MotionEvent.AXIS_Z);
        if (v != rx) { rx = v; cambio = true; }
        v = e.getAxisValue(MotionEvent.AXIS_RZ);
        if (v != ry) { ry = v; cambio = true; }
        v = e.getAxisValue(MotionEvent.AXIS_LTRIGGER);
        if (v == 0f) v = e.getAxisValue(MotionEvent.AXIS_BRAKE);
        if (v != lt) { lt = v; cambio = true; }
        v = e.getAxisValue(MotionEvent.AXIS_RTRIGGER);
        if (v == 0f) v = e.getAxisValue(MotionEvent.AXIS_GAS);
        if (v != rt) { rt = v; cambio = true; }

        if (cambio) {
            if (REGISTRO) logEjes(hx, hy);
            enviar();
        }
        return true;
    }

    /** Diagnostico: como mucho un registro cada 500 ms para no inundar. */
    private void logEjes(float hx, float hy) {
        long ahora = SystemClock.uptimeMillis();
        if (ahora - ultimoLog < 500L) return;
        ultimoLog = ahora;
        Log.i(TAG, String.format(Locale.ROOT,
                "Quest ejes: L(%.2f,%.2f) R(%.2f,%.2f) T(%.2f,%.2f) hat(%.1f,%.1f) mascara=0x%x",
                lx, ly, rx, ry, lt, rt, hx, hy, mascara));
    }

    private void enviar() {
        TouchControllerBridge.trySetState(mascara, lx, ly, rx, ry, lt, rt);
    }

    private static String nombre(InputDevice d) {
        if (d == null) return "?";
        return d.getName() + " v" + Integer.toHexString(d.getVendorId())
                + ":p" + Integer.toHexString(d.getProductId())
                + (d.isVirtual() ? " virtual" : "");
    }

    private static int bitDeBoton(int codigo) {
        switch (codigo) {
            case KeyEvent.KEYCODE_BUTTON_A: return A;
            case KeyEvent.KEYCODE_BUTTON_B: return B;
            case KeyEvent.KEYCODE_BUTTON_X: return X;
            case KeyEvent.KEYCODE_BUTTON_Y: return Y;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_BACK: return BACK;
            case KeyEvent.KEYCODE_BUTTON_START: return START;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: return L3;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: return R3;
            case KeyEvent.KEYCODE_BUTTON_L1: return LB;
            case KeyEvent.KEYCODE_BUTTON_R1: return RB;
            case KeyEvent.KEYCODE_DPAD_UP: return DPAD_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return DPAD_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT: return DPAD_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return DPAD_RIGHT;
            default: return 0;
        }
    }
}