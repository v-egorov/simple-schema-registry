package ru.vegorov.schemaregistry.service;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class that registers built-in JSLT functions.
 */
@Configuration
public class JsltFunctionConfiguration {

    private final JsltFunctionRegistry functionRegistry;
    private final JsltBuiltInFunctions builtInFunctions;

    public JsltFunctionConfiguration(JsltFunctionRegistry functionRegistry,
                                   JsltBuiltInFunctions builtInFunctions) {
        this.functionRegistry = functionRegistry;
        this.builtInFunctions = builtInFunctions;
    }

    /**
     * Register all built-in functions after bean initialization.
     */
    @PostConstruct
    public void registerBuiltInFunctions() {
        // Register forecast extraction function
        functionRegistry.register("extract_forecast_today",
            builtInFunctions.createExtractForecastTodayFunction());

        // Register additional metadata fields filter function
        functionRegistry.register("filter_additional_metadata_fields",
            builtInFunctions.createFilterAdditionalMetadataFieldsFunction());

         // Register UUID generation function
         functionRegistry.register("uuid",
             builtInFunctions.createUuidFunction());
    }
}
