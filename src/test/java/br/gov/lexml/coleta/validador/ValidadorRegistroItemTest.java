
package br.gov.lexml.coleta.validador;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class ValidadorRegistroItemTest {

    private ValidadorRegistroItem validador;

    @Before
    public void init() {
        validador = new ValidadorRegistroItem();
        validador.setValidadorService(new ValidadorServiceParaTeste());
    }

    @Test
    public void testSucesso() throws IOException {
        Assert.assertTrue(valida("oai:acordao.stf.jus.br:aco/100005"));
    }

    @Test
    public void testErroXML() throws IOException {
        Assert.assertFalse(valida("oai:acordao.stf.jus.br:aco/100005-erro-xml"));
    }

    @Test
    public void testErroSchema() throws IOException {
        Assert.assertFalse(valida("oai:acordao.stf.jus.br:aco/100005-erro-schema"));
    }

    @Test
    public void testDocumentoIndividual() throws IOException {
        Assert.assertFalse(valida("oai:acordao.stf.jus.br:aco/100005-erro-documentoIndividual"));
    }

    @Test
    public void testErroRelacionamento() throws IOException {
        Assert.assertFalse(valida("oai:acordao.stf.jus.br:aco/100005-erro-relacionamento"));
    }

    @Test
    public void testEmentaComHtml() throws IOException {
        Assert.assertTrue(valida("oai:cojur.tse.gov.br:acordao/000037322"));
    }

    @Test
    public void testItemComNotaSemUri() throws IOException {
    	Assert.assertTrue(valida("item_com_nota_sem_uri"));
    }
    
    @Test
    public void testItemRVBIOk() throws IOException {
    	Assert.assertTrue(valida("oai:rvbi-ok"));
    }
    
    @Test
    public void testItemRVBIFalhaOrdem() throws IOException {
    	Assert.assertFalse(valida("oai:rvbi-falha-ordem"));
    }
    
    @Test
    public void testSapl1() throws IOException {
    	Assert.assertTrue(valida("teofilootoni"));
    }
    
    private boolean valida(final String idRegistroItem) throws IOException {

        String fileName = "/oai/" + idRegistroItem.replace('/', '_').replace(':', '_') + ".xml";
        
        InputStream is = getClass().getResourceAsStream(fileName);
        
        String xml = IOUtils.toString(is);

        is.close();

        return validador.validar(idRegistroItem, xml, "contexto");
    }

}
