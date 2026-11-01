package org.telegram.ui.Components;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Patterns;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognizes text on a photo on the device and finds things worth one tap:
 * links, phone numbers, e-mails and Wi-Fi passwords.
 *
 * Telegram for Android integration: PhotoViewer runs {@link #recognize} on the bitmap it
 * already shows (on long press, or automatically when the "Aa" hint is tapped); the overlay
 * is drawn above the image with PhotoViewer's current scale/translation; results are cached
 * per photo id.
 */
public class PhotoText {

    public static final int LINK = 0, PHONE = 1, EMAIL = 2, PASSWORD = 3;

    public static class Line {
        public final String text;
        public final Rect box;

        Line(String text, Rect box) {
            this.text = text;
            this.box = box;
        }
    }

    public static class Entity {
        public final int type;
        public final String value;

        Entity(int type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    public interface Callback {
        void onResult(ArrayList<Line> lines, ArrayList<Entity> entities);
    }

    private static final Pattern PASSWORD_LINE = Pattern.compile("(?i)(?:password|pass|pwd|пароль)\\s*[:=]\\s*(\\S+)");

    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public void recognize(Bitmap bitmap, Callback callback) {
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener(result -> {
            ArrayList<Line> lines = new ArrayList<>();
            for (Text.TextBlock b : result.getTextBlocks()) {
                for (Text.Line l : b.getLines()) {
                    if (l.getBoundingBox() != null) {
                        lines.add(new Line(l.getText(), l.getBoundingBox()));
                    }
                }
            }
            callback.onResult(lines, entities(lines));
        }).addOnFailureListener(e -> callback.onResult(new ArrayList<>(), new ArrayList<>()));
    }

    public static ArrayList<Entity> entities(ArrayList<Line> lines) {
        ArrayList<Entity> out = new ArrayList<>();
        for (Line l : lines) {
            Matcher m = PASSWORD_LINE.matcher(l.text);
            if (m.find()) {
                out.add(new Entity(PASSWORD, m.group(1)));
                continue;
            }
            m = Patterns.EMAIL_ADDRESS.matcher(l.text);
            if (m.find()) {
                out.add(new Entity(EMAIL, m.group()));
                continue;
            }
            m = Patterns.WEB_URL.matcher(l.text);
            if (m.find() && m.group().contains(".") && !m.group().matches("[\\d.]+")) {
                out.add(new Entity(LINK, m.group()));
                continue;
            }
            m = Patterns.PHONE.matcher(l.text);
            if (m.find() && m.group().replaceAll("\\D", "").length() >= 7) {
                out.add(new Entity(PHONE, m.group().trim()));
            }
        }
        return out;
    }

    public void close() {
        recognizer.close();
    }
}
