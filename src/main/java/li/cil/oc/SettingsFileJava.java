package li.cil.oc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Configuration file reading, migration setup, and formatted output for the Scala facade. */
public final class SettingsFileJava {
    private static final Pattern INDENT = Pattern.compile("^(\\s*)");

    private SettingsFileJava() { }

    public static Config loadDefaults() throws IOException {
        InputStream input = Settings.class.getResourceAsStream("/application.conf");
        if (input == null) throw new IOException("Missing /application.conf");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return ConfigFactory.parseString(readLines(reader));
        }
    }

    public static Config parseFile(File file, Config defaults) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Config parsed = ConfigFactory.parseString(readLines(reader));
            return SettingsConfigMigration.patch(parsed, defaults).withFallback(defaults);
        }
    }

    public static void saveConfig(File file, Config config) {
        try {
            ConfigRenderOptions renderOptions = ConfigRenderOptions.defaults()
                .setJson(false)
                .setOriginComments(false);
            String newline = System.getProperty("line.separator");
            file.getParentFile().mkdirs();
            String rendered = config.root().render(renderOptions).lines()
                .map(line -> {
                    Matcher indent = INDENT.matcher(line);
                    if (!indent.find()) return line;
                    return indent.group(1).replace("  ", " ") + line.substring(indent.end());
                })
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining(newline));
            String separatedValues = rendered.replaceAll(
                "((?:\\s*#.*" + Pattern.quote(newline) + ")(?:\\s*[^#\\s].*"
                    + Pattern.quote(newline) + ")+)",
                "$1" + Matcher.quoteReplacement(newline));
            try (PrintWriter output = new PrintWriter(file)) {
                output.write(separatedValues);
            }
        } catch (Throwable error) {
            OpenComputers.log().warn("Failed saving config.", error);
        }
    }

    private static String readLines(BufferedReader reader) throws IOException {
        String newline = System.lineSeparator();
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line).append(newline);
        if (result.length() == 0) result.append(newline);
        return result.toString();
    }
}
