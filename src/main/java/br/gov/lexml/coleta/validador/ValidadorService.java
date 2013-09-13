
package br.gov.lexml.coleta.validador;

public interface ValidadorService {

    /**
     * Registra erro de validação.
     * 
     * @param idRegistroItem
     * @param tipoErro
     * @param msg
     * @param ctxUsr Objeto de contexto do usuário passado em ValidadorRegistroItem.validar
     */
    void logError(String idRegistroItem, TipoErroValidacao tipoErro, String msg, Object ctxUsr);

    /**
     * Verifica se o núcleo passado é válido.
     * 
     * @param nucleo no formato ID_PUBLICADOR:TIPO_PERFIL:LOCALIDADE:AUTORIDADE:TIPO_DOCUMENTO
     * @return se exitste a combinação informada para o núcleo da urn
     */
    boolean isNucleoValido(String nucleo);

}
