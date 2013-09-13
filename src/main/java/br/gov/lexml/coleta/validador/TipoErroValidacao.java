
package br.gov.lexml.coleta.validador;

public enum TipoErroValidacao {

    // Manter esta ordem (os ids no banco estão na ordem a partir de 1)
    ERRO_GENERICO,
    XML_MAL_FORMADO,
    XML_INVALIDO,
    URN_MAL_FORMADO,
    URN_INVALIDO,
    URN_INCOMPATIVEL;

    public int getIdTipoErro() {
        return ordinal() + 1;
    }

}
