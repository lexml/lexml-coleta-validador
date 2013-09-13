
package br.gov.lexml.coleta.validador;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ValidadorServiceParaTeste implements ValidadorService {
	
	private static final Log log = LogFactory.getLog(ValidadorServiceParaTeste.class);

    public void logError(final String idRegistroItem, final TipoErroValidacao tipoErro, final String msg,
                         final Object ctx) {
        log.error("Erro " + tipoErro + " para REGISTRO_ITEM " + idRegistroItem);
        log.error("\tMsg: " + msg);
        log.error("\tCtx: " + ctx);
    }

    public boolean isNucleoValido(final String nucleo) {
        return nucleo.indexOf("erro") == -1;
    }

}
