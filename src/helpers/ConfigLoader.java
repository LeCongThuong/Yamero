package helpers;

import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private final Properties configFile;

    public ConfigLoader(String configPath) throws Exception {
        this.configFile = new Properties();
        this.configFile.load(this.getClass().getClassLoader().getResourceAsStream(configPath));
    }

    public String getProperty(String key) {
        return this.configFile.getProperty(key, null);
    }
}
