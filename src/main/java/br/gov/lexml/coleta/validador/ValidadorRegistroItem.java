
package br.gov.lexml.coleta.validador;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ValidadorRegistroItem {

    private static final Logger log = LoggerFactory.getLogger(ValidadorRegistroItem.class);

    private static final String SEP = ":";

    private static final String TIPO_PERFIL_DOCUMENTO_INDIVIDUAL = "D";
    private static final String TIPO_PERFIL_RELACIONAMENTO = "R";

    private ValidadorService svc;

    private final ValidadorXMLHelper xmlHelper;

    public ValidadorRegistroItem() {
        xmlHelper = new ValidadorXMLHelper();
    }

    public void setValidadorService(final ValidadorService validadorService) {
        svc = validadorService;
    }

    /**
     * Validação de REGISTRO_ITEM
     * <p/>
     * <u>Regras de Validação:</u> *
     * <li>RV#1: O ID_REGISTRO_ITEM <b>não</b> pode conter espaços em branco</li>
     * <li>RV#2: O TX_METADADO_XML <b>não</b> pode ser nulo</li>
     * <li>RV#3: O TX_METADADO_XML <b>deve</b> ser válido segundo o schema/xmlbeans</li>
     * <li>RV#4: O id do RegistroItem <b>não</b> pode ser vazio</li>
     * <li>RV#5: A URN de DocumentoIndividual deve ser válida para <b>todos</b> os idPublicador do registro</li>
     * <li>RV#6: A URN não é válida para DocumentoIndividual de acordo com o perfil, usando o idPublicador de Item</li>
     * <li>RV#7: A URN não é valida para Relacionamento de acordo com o perfil</li>
     * <li>RV#8: Se o Relacionamento não possuir idPublicador será considerado o idPublicador do primeiro Item do
     * registro</li>
     * 
     * @param idRegistroItem Identificador do registro a ser validado
     * @param xml XML do registro para validação
     * @param ctxUsr Objeto de contexto a ser repassado para o método ValidadorService.logError
     */
    public boolean validar(final String idRegistroItem, final String xml, final Object ctxUsr) {

        if (svc == null) {
            throw new RuntimeException("ValidatorService não informado.");
        }

        if (StringUtils.isEmpty(idRegistroItem)) {
            log.error("RV#4 Objeto RegistroItem ri passado é nulo");
            return false;
        }

        if (idRegistroItem.indexOf(' ') != -1) {
            svc.logError(idRegistroItem, TipoErroValidacao.ERRO_GENERICO,
                "RV#1 ID_REGISTRO_ITEM não pode conter espaços em branco", ctxUsr);
            return false;
        }

        if (StringUtils.isEmpty(xml)) {
            svc.logError(idRegistroItem, TipoErroValidacao.XML_MAL_FORMADO,
                "RV#2 XML nulo foi passado para validação", ctxUsr);
            return false;
        }

        Document doc = parseDocument(idRegistroItem, xml, ctxUsr);
        if (doc == null) {
            return false;
        }

        return validaDocumento(idRegistroItem, doc, ctxUsr);
    }

    private Document parseDocument(final String idRegistroItem, final String xml, final Object ctxUsr) {

        Document doc = null;

        try {
            InputStream is = new ByteArrayInputStream(xml.getBytes());
            doc = xmlHelper.parse(is);
            is.close();

            String parseErrors = xmlHelper.getParseErrorsAsString();
            if (parseErrors != null) {
                svc.logError(idRegistroItem, TipoErroValidacao.XML_INVALIDO,
                    "RV#3 Xml não é válido segundo o schema: " + parseErrors, ctxUsr);
                doc = null;
            }
        }
        catch (Exception e) {
            svc.logError(idRegistroItem, TipoErroValidacao.XML_MAL_FORMADO, "XML mal formado: " + e.getMessage(),
                ctxUsr);
        }

        return doc;
    }

    private boolean validaDocumento(final String idRegistroItem, final Document doc, final Object ctxUsr) {

        Node root = doc.getDocumentElement();

        ContextoValidacao ctx = new ContextoValidacao();

        String tstURNDocumentoIndividual = xmlHelper.getString(root, "lexml:DocumentoIndividual/text()");

        if (!isEstruturalmenteValid(ctx, tstURNDocumentoIndividual)) {
            svc.logError(idRegistroItem, TipoErroValidacao.URN_INVALIDO, "RV#5 A URN de DocumentoIndividual \""
                + tstURNDocumentoIndividual + "\" é estruturalmente inválida", ctxUsr);
            return false;
        }

        // Publicadores já testados como compatíveis com a URN do doc individual (evita testar
        // novamente)
        List<Integer> idPublicadoresTestados = new ArrayList<Integer>();

        // --------------------------------
        // Itens

        NodeList itens = xmlHelper.getNodeList(root, "lexml:Item");

        // RV#5 e RV6
        // Todos os DocumentoIndividual's devem ser válidos para TODOS os idPublicador
        // existentes no registro (tanto de Item quanto Relacionamento).
        for (int i = 0; i < itens.getLength(); i++) {
            Node item = itens.item(i);
            Integer idPublicador = xmlHelper.getAttributeAsInteger(item, "idPublicador");
            if (idPublicadoresTestados.contains(idPublicador)) {
                continue;
            }
            if (!isDocumentoIndividualValid(ctx, idPublicador)) {
                svc.logError(idRegistroItem, TipoErroValidacao.URN_INCOMPATIVEL,
                    "RV#5 A URN de DocumentoIndividual \"" + tstURNDocumentoIndividual
                        + "\" não é compatível com a configuração atual " + "em relação ao perfil para o "
                        + "publicador \"" + idPublicador + "\"", ctxUsr);
                return false;
            }
            idPublicadoresTestados.add(idPublicador);
        }

        // --------------------------------
        // Relacionamentos

        NodeList relacionamentos = xmlHelper.getNodeList(root, "lexml:Relacionamento");

        Integer idPublicadorDefault = null;
        if (!idPublicadoresTestados.isEmpty()) {
            idPublicadorDefault = idPublicadoresTestados.get(0);
        }

        // Testamos a URN do DocumentoIndividual contra os relacionamentos
        // RV#5 e RV#7
        for (int i = 0; i < relacionamentos.getLength(); i++) {
            Node relacionamento = relacionamentos.item(i);
            Integer idPublicador = getIdPublicadorRelacionamento(relacionamento, idPublicadorDefault);

            if (!idPublicadoresTestados.contains(idPublicador)) {
                if (!isDocumentoIndividualValid(ctx, idPublicador)) {
                    svc.logError(idRegistroItem, TipoErroValidacao.URN_INCOMPATIVEL,
                        "RV#5 A URN de DocumentoIndividual \"" + tstURNDocumentoIndividual
                            + "\" não é compatível com a configuração atual "
                            + "em relação ao perfil para o relacionamento do publicador \"" + idPublicador + "\"",
                        ctxUsr);
                    return false;
                }
                idPublicadoresTestados.add(idPublicador);
            }
        }

        // E testamos tambem as URNs de cada Relacionamento
        // Este loop foi separado do anterior para permitir a reutilização do parse da URN do doc
        // individual no loop anterior pelo método isDocumentoIndividualValid
        for (int i = 0; i < relacionamentos.getLength(); i++) {
            Node relacionamento = relacionamentos.item(i);
            Integer idPublicador = getIdPublicadorRelacionamento(relacionamento, idPublicadorDefault);

            String tstURNRelacionamento = relacionamento.getTextContent().trim();

            if (null == idPublicador) {
                svc.logError(idRegistroItem, TipoErroValidacao.URN_INCOMPATIVEL, "RV#8 A URN de Relacionamento \""
                    + tstURNRelacionamento + "\" não pode ser validada por ausência de idPublicador", ctxUsr);
                return false;
            }

            if (!isRelacionamentoValid(ctx, tstURNRelacionamento, idPublicador)) {
                svc.logError(idRegistroItem, TipoErroValidacao.URN_INCOMPATIVEL, "RV#7 A URN de Relacionamento \""
                    + tstURNRelacionamento + "\" não é compatível com a configuração atual de perfil", ctxUsr);
                return false;
            }
        }

        return true;
    }

    /**
     * Metodo de validação das URNs dos campos "DocumentosIndividual" e "Relacionamento" passe em p_tipoURN os
     * valores possíveis "D"ou "R" É necessário inserir o conjunto de perfis válidos antes de realizar a chamada a
     * este método.
     */
    private boolean isEstruturalmenteValid(final ContextoValidacao ctx, final String urn) {

        // TODO - Voltar a fazer a validação após OK do João Lima
        // Deve estar em minúsculas
        // if (!urn.equals(urn.toLowerCase())) {
        // return false;
        // }

        if (!urn.startsWith("urn:lex:")) {
            return false;
        }

        String[] part = urn.substring(8).split(SEP);
        if (part.length < 3) {
            return false;
        }

        ctx.tstNucleoURN = part[0] + SEP + part[1] + SEP + part[2];

        if (part[1].contains(";") && part[2].contains(";")) {
            String autoridade = part[1].substring(0, part[1].indexOf(";"));
            String tipo = part[2].substring(0, part[2].indexOf(";"));
            ctx.tstNucleoURNAutoridadeReduzida = part[0] + SEP + autoridade + SEP + part[2];
            ctx.tstNucleoURNTipoReduzida = part[0] + SEP + part[1] + SEP + tipo;
        }

        return true;
    }

    /**
     * Valida a p_urn segundo as regras para URN de DocumentoIndividual
     * 
     * @param p_urn
     * @param p_idPublicador
     * @return
     * @throws ArrayIndexOutOfBoundsException
     */
    private boolean isDocumentoIndividualValid(final ContextoValidacao ctx, final Integer p_idPublicador) {

        if (null == p_idPublicador) {
            return false;
        }

        if (isCoreValid(p_idPublicador, ctx.tstNucleoURN, TIPO_PERFIL_DOCUMENTO_INDIVIDUAL)) {
            return true;
        }

        // Se a validação básica não funciona somente a junção dos 2 testes abaixo valida a
        // p_urn.
        return isCoreValid(p_idPublicador, ctx.tstNucleoURNAutoridadeReduzida, TIPO_PERFIL_DOCUMENTO_INDIVIDUAL)
            && isCoreValid(p_idPublicador, ctx.tstNucleoURNTipoReduzida, TIPO_PERFIL_DOCUMENTO_INDIVIDUAL);
    }

    /**
     * Valida a p_urn segundo as regras para URN de Relacionamento
     * 
     * @param p_urn
     * @param p_idPublicador
     * @return
     */
    private boolean isRelacionamentoValid(final ContextoValidacao ctx, final String p_urn,
        final Integer p_idPublicador) {

        if (!isEstruturalmenteValid(ctx, p_urn)) {
            return false;
        }

        if (isCoreValid(p_idPublicador, ctx.tstNucleoURN, TIPO_PERFIL_RELACIONAMENTO)) {
            return true;
        }

        // Se a validação básica não funciona somente a junção dos 2 testes abaixo valida a
        // p_urn.
        return isCoreValid(p_idPublicador, ctx.tstNucleoURNAutoridadeReduzida, TIPO_PERFIL_RELACIONAMENTO)
            && isCoreValid(p_idPublicador, ctx.tstNucleoURNTipoReduzida, TIPO_PERFIL_RELACIONAMENTO);
    }

    /**
     * Valida a p_tstNucleoURN segundo pelo menos uma das regras:
     * <p/>
     * <li>OU: Se a p_tstNucleoURN existe no perfil do idPublicador para o tipo de perfil TODOS</li>
     * <li>OU: Se a p_tstNucleoURN existe no perfil do idPublicador para o tipo de perfil p_tipoCore</li>
     * 
     * @param p_urn
     * @param p_idPublicador
     * @param nucleoURN
     * @return
     */
    private boolean isCoreValid(final Integer p_idPublicador, final String nucleoURN, final String p_tipoCore) {

        if (null == nucleoURN) {
            return false;
        }

        return svc.isNucleoValido(p_idPublicador + SEP + p_tipoCore + SEP + nucleoURN);
    }

    private Integer getIdPublicadorRelacionamento(final Node relacionamento, final Integer idDefault) {
        Integer idPublicador = xmlHelper.getAttributeAsInteger(relacionamento, "idPublicador");
        return idPublicador == null ? idDefault : idPublicador;
    }

    private static class ContextoValidacao {

        String tstNucleoURN;
        String tstNucleoURNAutoridadeReduzida;
        String tstNucleoURNTipoReduzida;

    }
}
