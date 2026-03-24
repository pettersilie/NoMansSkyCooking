package de.nms.nmsrecipes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NmsRecipesApplication {

    public static void main(String[] args) {
        SpringApplication.run(NmsRecipesApplication.class, args);
    }
}
