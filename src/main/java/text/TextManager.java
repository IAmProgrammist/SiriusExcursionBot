package text;

import org.json.JSONException;
import org.json.JSONObject;
import others.Places;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextManager {
    JSONObject main;

    public TextManager(String fileName) {
        try {
            main = new JSONObject(readStream(getClass().getClassLoader().getResource(fileName).openStream()));
        } catch (JSONException e) {
            Logger.getLogger("Text.TextManager").log(Level.WARNING, "JSON is incorrect! Fix it dummy!");
        } catch (Exception e) {
            Logger.getLogger("Text.TextManager").log(Level.WARNING, "Couldn't read localization files! Should exit immediately!");
        }
    }

    public String getLine(TextTypes type, TextLangs lang) {
        return main.getJSONObject(lang.toString().toLowerCase(Locale.ROOT)).getString(type.toString().toLowerCase(Locale.ROOT));
    }

    public String getLine(Places type, TextLangs lang) {
        return main.getJSONObject(lang.toString().toLowerCase(Locale.ROOT)).getString(type.toString().toLowerCase(Locale.ROOT));
    }

    private static String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder(512);
        Reader r = new InputStreamReader(is, "UTF-8");
        int c = 0;
        while ((c = r.read()) != -1) {
            sb.append((char) c);
        }
        return sb.toString();
    }
}
