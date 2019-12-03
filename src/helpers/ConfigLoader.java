package helpers;

import java.util.Properties;

public class ConfigLoader {
    private final Properties configFile;

    public ConfigLoader(String configPath) {
        this.configFile = new Properties();
        try {
            this.configFile.load(this.getClass().getClassLoader().getResourceAsStream(configPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getProperty(String key) {
        return this.configFile.getProperty(key, null);
    }
}
