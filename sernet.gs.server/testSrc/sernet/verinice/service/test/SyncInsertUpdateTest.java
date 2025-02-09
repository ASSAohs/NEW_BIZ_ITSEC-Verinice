/*******************************************************************************
 * Copyright (c) 2014 Benjamin Weißenfels.
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Benjamin Weißenfels <bw[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import sernet.gs.service.RetrieveInfo;
import sernet.gs.service.Retriever;
import sernet.gs.service.RuntimeCommandException;
import sernet.verinice.interfaces.CommandException;
import sernet.verinice.interfaces.IBaseDao;
import sernet.verinice.model.bsi.Anwendung;
import sernet.verinice.model.bsi.AnwendungenKategorie;
import sernet.verinice.model.bsi.Client;
import sernet.verinice.model.bsi.ITVerbund;
import sernet.verinice.model.common.ChangeLogEntry;
import sernet.verinice.model.common.CnALink;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.iso27k.ControlGroup;
import sernet.verinice.model.iso27k.Organization;
import sernet.verinice.service.commands.LoadElementById;
import sernet.verinice.service.commands.LoadElementByUuid;
import sernet.verinice.service.commands.SyncCommand;
import sernet.verinice.service.commands.SyncParameterException;
import sernet.verinice.service.commands.UpdateElement;
import sernet.verinice.service.test.helper.util.BFSTravers;
import sernet.verinice.service.test.helper.util.CnATreeTraverser;
import sernet.verinice.service.test.helper.util.CnATreeTraverser.CallBack;
import sernet.verinice.service.test.helper.vnaimport.VNAImportHelper;

/**
 * @author Benjamin Weißenfels <bw[at]sernet[dot]de>
 * 
 */
@Transactional
@TransactionConfiguration(transactionManager = "txManager")
public class SyncInsertUpdateTest extends CommandServiceProvider {

    @Resource(name = "cnaLinkDao")
    private IBaseDao<CnALink, Serializable> cnaLinkDao;

    private static final Logger log = Logger.getLogger(SyncInsertUpdateTest.class);

    private static final String VNA_FILE = "SyncInsertUpdateTest.vna";

    private static final String VNA_FILE_INSERT = "SyncInsertUpdateTestInsert.vna";

    private static final String VNA_FILE_UPDATE = "SyncInsertUpdateTestUpdate.vna";

    private static final String VNA_FILE_UPDATE_INSERT = "SyncInsertUpdateTestUpdateInsert.vna";

    private static final String VNA_FILE_DELETE = "SyncInsertUpdateTestDelete.vna";

    private static final String VNA_FILE_RELATION = "SyncInsertUpdateTestRelation.vna";

    private static final String VNA_FILE_RELATION_DUPLICATE_LINK = "SyncInsertUpdateTestRelationDuplicateLink.vna";

    private static final String VNA_FILE_INSERT_CLIENT = "SyncInsertUpdateTestInsertClient.vna";

    private static final String SOURCE_ID = "JUNIT SyncInsertUpdate";

    private static final String IT_VERBUND_EXT_ID = "ENTITY_41713";

    private static final String ANWENDUNGEN_KATEGORIE_EXT_ID = "ENTITY_9146d4ce-26c5-4b03-8f34-44ee4d1deddd";

    private static final String ANWENDUNG_1_EXT_ID = "ENTITY_41763";

    private static final String ANWENDUNG_2_EXT_ID = "ENTITY_8650";

    private static final String ANWENDUNG_INSERT_2_EXT_ID = "ENTITY_42156";

    private static final String CLIENT_EXT_ID = "ENTITY_10072";

    private static final CnATreeTraverser cnATreeTraverser = new BFSTravers();

    @Before
    public void setUp() throws IOException, CommandException, SyncParameterException {
        VNAImportHelper.importFile(VNA_FILE, true, true, true, false);
    }

    @Test
    public void insertTest() throws IOException, CommandException, SyncParameterException {

        SyncCommand syncCommand = VNAImportHelper.importFile(VNA_FILE_INSERT, true, false, false,
                false);

        Anwendung anwendung1 = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);
        Anwendung anwendung2 = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_INSERT_2_EXT_ID);

        assertEquals(
                "inserted Object anwendung must have the same parent id as the already existing",
                anwendung1.getParent().getDbId(), anwendung2.getParent().getDbId());

        assertEquals("updated flag is set to false", 0, syncCommand.getPotentiallyUpdated());
        assertEquals("internalAdmin", anwendung1.getEntity().getCreatedBy());
        Assert.assertNotNull(anwendung1.getEntity().getCreatedAt());
        Assert.assertNull(anwendung1.getEntity().getChangedBy());
        Assert.assertNull(anwendung1.getEntity().getChangedAt());
        assertEquals("internalAdmin", anwendung2.getEntity().getCreatedBy());
        Assert.assertNotNull(anwendung2.getEntity().getCreatedAt());
        Assert.assertNull(anwendung2.getEntity().getChangedBy());
        Assert.assertNull(anwendung2.getEntity().getChangedAt());

    }

    @Test
    public void countInsertedAnwendungTest()
            throws SyncParameterException, IOException, CommandException {
        SyncCommand syncCommand = VNAImportHelper.importFile(VNA_FILE_INSERT, true, false, false,
                false);
        assertEquals("only one object should have been inserted", 6, syncCommand.getInserted());
    }

    @Test
    public void countInsertedClientTest()
            throws SyncParameterException, IOException, CommandException {
        SyncCommand syncCommand = VNAImportHelper.importFile(VNA_FILE_INSERT_CLIENT, true, false,
                false, false);
        assertEquals("only one object should have been inserted", 1, syncCommand.getInserted());
    }

    @Test
    public void countUpdatedZeroTest()
            throws SyncParameterException, IOException, CommandException {
        SyncCommand syncCommand = VNAImportHelper.importFile(VNA_FILE_UPDATE, true, false, false,
                false);
        assertEquals("updated flag is set to false", 0, syncCommand.getPotentiallyUpdated());
    }

    @Test
    public void countPotentiallyUpdatedTest()
            throws SyncParameterException, IOException, CommandException {

        final int OBJECTS_IN_VNA_ARCHIV = getAmountOfElements(
                loadElement(SOURCE_ID, IT_VERBUND_EXT_ID));

        SyncCommand syncCommand = VNAImportHelper.importFile(VNA_FILE_UPDATE, false, true, false,
                false);

        assertEquals("only " + " elements can be updated", OBJECTS_IN_VNA_ARCHIV,
                syncCommand.getPotentiallyUpdated());
    }

    @Test
    public void updateTest() throws IOException, CommandException, SyncParameterException {

        Anwendung anwendungBeforeImport = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);
        String anwendungsPersonenBezogenBefore = anwendungBeforeImport.getEntity()
                .getPropertyValue("anwendung_persbez");
        String anwendungsStatusBefore = anwendungBeforeImport.getEntity()
                .getPropertyValue("anwendung_status");
        String anwendungsKuerzelBefore = anwendungBeforeImport.getKuerzel();
        AnwendungenKategorie anwendungenKategorieBeforeImport = (AnwendungenKategorie) loadElement(
                SOURCE_ID, ANWENDUNGEN_KATEGORIE_EXT_ID);

        VNAImportHelper.importFile(VNA_FILE_UPDATE, false, true, false, false);
        Anwendung anwendungAfterImport = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);

        String anwendungsStatusAfter = anwendungAfterImport.getEntity()
                .getPropertyValue("anwendungs_status");
        assertFalse(anwendungsStatusBefore.equals(anwendungsStatusAfter));

        String anwendungsPersonenBezogenAfter = anwendungAfterImport.getEntity()
                .getPropertyValue("anwendung_persbez");
        assertFalse(anwendungsPersonenBezogenBefore.equals(anwendungsPersonenBezogenAfter));

        assertEquals(anwendungsKuerzelBefore, anwendungAfterImport.getKuerzel());
        assertEquals("AnwendungenKategorie must still have only 1 child", 1,
                anwendungenKategorieBeforeImport.getChildren().size());

        Assert.assertEquals("internalAdmin", anwendungAfterImport.getEntity().getChangedBy());
        Assert.assertNotNull(anwendungAfterImport.getEntity().getChangedAt());

    }

    @Test
    public void insertUpdateTest() throws IOException, CommandException, SyncParameterException {

        Anwendung anwendungBeforeImport = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);

        VNAImportHelper.importFile(VNA_FILE_UPDATE_INSERT, true, true, false, false);

        Anwendung anwendung1 = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);
        Anwendung anwendung2 = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_2_EXT_ID);

        validateImportExtId(anwendungBeforeImport);
        validateImportExtId(anwendung1);
        validateImportExtId(anwendung2);

        assertEquals(
                "inserted Object anwendung must have the same parent id as the already existing",
                anwendung1.getParent().getDbId(), anwendung2.getParent().getDbId());

        String anwendungsStatusBefore = anwendungBeforeImport.getEntity()
                .getPropertyValue("anwendung_status");
        String anwendungsStatusAfter = anwendung2.getEntity().getPropertyValue("anwendungs_status");
        assertFalse(anwendungsStatusBefore.equals(anwendungsStatusAfter));

        assertEquals("internalAdmin", anwendung1.getEntity().getCreatedBy());
        Assert.assertNotNull(anwendung1.getEntity().getCreatedAt());
        assertEquals("internalAdmin", anwendung1.getEntity().getChangedBy());
        Assert.assertNotNull(anwendung1.getEntity().getChangedAt());
        assertEquals("internalAdmin", anwendung2.getEntity().getCreatedBy());
        Assert.assertNotNull(anwendung2.getEntity().getCreatedAt());
        Assert.assertNull(anwendung2.getEntity().getChangedBy());
        Assert.assertNull(anwendung2.getEntity().getChangedAt());
    }

    @Test
    public void relationImported() throws SyncParameterException, IOException, CommandException {

        VNAImportHelper.importFile(VNA_FILE_RELATION, true, true, false, false);

        Anwendung anwendungWithLink = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);
        validateImportExtId(anwendungWithLink);

        Client clientWithLink = (Client) loadElement(SOURCE_ID, CLIENT_EXT_ID);
        validateImportExtId(clientWithLink);

        anwendungWithLink = (Anwendung) Retriever.retrieveElement(anwendungWithLink,
                new RetrieveInfo().setLinksDown(true));
        Set<CnALink> links = anwendungWithLink.getLinksDown();
        Client client = (Client) links.iterator().next().getDependency();

        assertEquals(client.getDbId(), clientWithLink.getDbId());
    }

    @Test
    public void import_file_with_duplicate_link()
            throws SyncParameterException, IOException, CommandException {

        VNAImportHelper.importFile(VNA_FILE_RELATION_DUPLICATE_LINK, true, true, false, false);

        Anwendung anwendungWithLink = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);
        validateImportExtId(anwendungWithLink);

        Client clientWithLink = (Client) loadElement(SOURCE_ID, CLIENT_EXT_ID);
        validateImportExtId(clientWithLink);

        anwendungWithLink = (Anwendung) Retriever.retrieveElement(anwendungWithLink,
                new RetrieveInfo().setLinksDown(true));
        Set<CnALink> links = anwendungWithLink.getLinksDown();
        Client client = (Client) links.iterator().next().getDependency();

        assertEquals(client.getDbId(), clientWithLink.getDbId());
    }

    @Test
    public void delete() throws SyncParameterException, IOException, CommandException {

        Anwendung anwendung = null;
        anwendung = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);

        validateImportExtId(anwendung);
        assertNotNull(anwendung);

        VNAImportHelper.importFile(VNA_FILE_DELETE, false, false, true, false);

        try {
            anwendung = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);
            fail("extected an assertion error, due the element should be deleted");
        } catch (AssertionError ex) {
            log.info("element " + (anwendung != null ? anwendung.getUuid() : "") + " is deleted");
        }
    }

    @Test
    public void integratedDelete() throws CommandException, SyncParameterException, IOException {

        ITVerbund itVerbund = (ITVerbund) loadElement(SOURCE_ID, IT_VERBUND_EXT_ID);
        Anwendung anwendung = (Anwendung) loadElement(SOURCE_ID, ANWENDUNG_1_EXT_ID);
        deleteSourceId(itVerbund);

        VNAImportHelper.importFile(VNA_FILE_DELETE, false, false, true, false);

        LoadElementById<Anwendung> loadElementByUuid = new LoadElementById<Anwendung>(
                anwendung.getDbId());
        commandService.executeCommand(loadElementByUuid);

        assertTrue("Element " + anwendung.getUuid() + " may not deleted",
                loadElementByUuid.getElement() != null);
    }

    @Test
    public void deleteEverythingWithSameSourceIdAndUnequalExtId()
            throws CommandException, SyncParameterException, IOException {
        List<String> victims = new ArrayList<String>(0);
        Organization organization = createOrganization();
        victims.add(organization.getUuid());

        victims.addAll(
                createInOrganisation(organization, GROUP_TYPE_MAP.get(ControlGroup.TYPE_ID), 2));
        setSourceId(organization);

        VNAImportHelper.importFile(VNA_FILE_DELETE, false, false, true, false);

        for (String uuid : victims) {
            LoadElementByUuid<CnATreeElement> loadElementByUuid = new LoadElementByUuid<CnATreeElement>(
                    uuid);
            commandService.executeCommand(loadElementByUuid);

            assertTrue("element should have been deleted from database",
                    loadElementByUuid.getElement() == null);
        }

    }

    @Test
    // VN-2873
    public void importWithLinksToElementsNotIncludedInImport()
            throws CommandException, SyncParameterException, IOException {
        SyncCommand syncCommand = VNAImportHelper.importFile("Informationsverbund_VN-2873.vna");
        Assert.assertEquals(3, syncCommand.getInserted());
    }

    private void setSourceId(CnATreeElement organization) {
        cnATreeTraverser.traverse(organization, new CallBack() {
            @Override
            public void execute(CnATreeElement v) {
                v.setSourceId(SOURCE_ID);
                UpdateElement<CnATreeElement> updateElement = new UpdateElement<CnATreeElement>(v,
                        true, ChangeLogEntry.STATION_ID);
                try {
                    SyncInsertUpdateTest.this.commandService.executeCommand(updateElement);
                } catch (CommandException e) {
                    throw new RuntimeCommandException("update element " + v.getUuid() + " failed");
                }
            }
        });
    }

    private void deleteSourceId(ITVerbund itVerbund) {
        cnATreeTraverser.traverse(itVerbund, new CallBack() {
            @Override
            public void execute(CnATreeElement element) {
                element.setSourceId(null);
                try {
                    log.info("delete source id from element " + element.getUuid());
                    UpdateElement<CnATreeElement> updateElement = new UpdateElement<CnATreeElement>(
                            element, true, ChangeLogEntry.STATION_ID);
                    SyncInsertUpdateTest.this.commandService.executeCommand(updateElement);
                } catch (CommandException e) {
                    throw new RuntimeCommandException(
                            "update element " + element.getUuid() + " failed");
                }
            }
        });
    }

    private void validateImportExtId(CnATreeElement element) throws CommandException {
        assertEquals(element.getScopeId(), loadElement(SOURCE_ID, IT_VERBUND_EXT_ID).getScopeId());
    }

    private int getAmountOfElements(CnATreeElement root) throws CommandException {

        root = (ITVerbund) Retriever.retrieveElement(root, new RetrieveInfo().setChildren(true));

        int i = 0;
        Queue<CnATreeElement> queue = new LinkedList<CnATreeElement>();
        queue.add(root);
        CnATreeElement current;

        while (!queue.isEmpty()) {
            current = queue.poll();
            current = (CnATreeElement) Retriever.retrieveElement(current,
                    new RetrieveInfo().setChildren(true));
            queue.addAll(current.getChildren());
            i++;
        }

        return i;
    }
}
