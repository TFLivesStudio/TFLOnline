package com.tflives.tflonline.common;

public final class Messages {
    private Messages() {}

    public static String text(String language, String key) {
        boolean spanish = !"en".equalsIgnoreCase(language);
        return switch (key) {
            case "help" -> spanish
                    ? "Uso: /tflonline status | servers | groups | sync | reload | debug"
                    : "Usage: /tflonline status | servers | groups | sync | reload | debug";
            case "reloaded" -> spanish ? "Configuracion recargada correctamente." : "Configuration reloaded.";
            case "reload-failed" -> spanish ? "No se pudo recargar config.yml; los ajustes anteriores siguen activos: "
                    : "Could not reload config.yml; previous settings are still active: ";
            case "sync" -> spanish ? "Sincronizacion solicitada." : "Synchronization requested.";
            case "disabled" -> spanish ? "TFLOnline no ha iniciado correctamente." : "TFLOnline failed to initialize.";
            case "none" -> spanish ? "Ninguno." : "None.";
            default -> key;
        };
    }
}
