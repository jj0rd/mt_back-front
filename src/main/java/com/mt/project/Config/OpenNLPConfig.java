package com.mt.project.Config;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class OpenNLPConfig {
    @Bean
    public POSTaggerME posTagger() throws Exception {
        InputStream modelStream =
                getClass().getResourceAsStream("/models/en-pos-maxent.bin");

        if (modelStream == null) {
            throw new RuntimeException("Model file not found!");
        }

        POSModel model = new POSModel(modelStream);
        return new POSTaggerME(model);
    }
}
