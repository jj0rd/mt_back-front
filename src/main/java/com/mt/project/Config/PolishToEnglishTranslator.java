//package com.mt.project.Config;
//
//import ai.djl.Model;
//import ai.djl.inference.Predictor;
//import ai.djl.modality.nlp.translator.*;
//import ai.djl.modality.nlp.NlpUtils;
//import ai.djl.translate.Translator;
//import ai.djl.translate.TranslatorContext;
//import ai.djl.translate.TranslateException;
//import ai.djl.repository.zoo.Criteria;
//import ai.djl.repository.zoo.ZooModel;
//import ai.djl.repository.zoo.ModelZoo;
//
//public class PolishToEnglishTranslator {
//
//    private ZooModel<String, String> model;
//
//    public PolishToEnglishTranslator() throws Exception {
//        Criteria<String, String> criteria = Criteria.builder()
//                .setTypes(String.class, String.class)
//                .optModelUrls("path/to/pl-en-opus-mt") // lokalny folder modelu HuggingFace
//                .optTranslator(new Translator<String, String>() {
//                    @Override
//                    public String processOutput(TranslatorContext ctx, ai.djl.ndarray.NDList list) {
//                        return list.singletonOrThrow().toString();
//                    }
//                    @Override
//                    public ai.djl.ndarray.NDList processInput(TranslatorContext ctx, String input) {
//                        return NlpUtils.toNDList(ctx.getNDManager(), input);
//                    }
//                })
//                .build();
//
//        model = ModelZoo.loadModel(criteria);
//    }
//
//    public String translate(String text) throws TranslateException {
//        try (Predictor<String, String> predictor = model.newPredictor()) {
//            return predictor.predict(text);
//        }
//    }
//}