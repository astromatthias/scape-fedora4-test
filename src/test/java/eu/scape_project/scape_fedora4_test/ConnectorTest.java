package eu.scape_project.scape_fedora4_test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import gov.loc.mix.v20.Mix;
import info.lc.xmlns.textmd_v3.TextMD;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.purl.dc.elements._1.ElementContainer;
import org.purl.dc.elements._1.SimpleLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.harvard.hul.ois.xml.ns.fits.fits_output.Fits;
import eu.scape_project.model.*;
import eu.scape_project.model.LifecycleState.State;
import eu.scape_project.util.ScapeMarshaller;

public class ConnectorTest {
	
	private static final String BASE_URL = "http://141.52.218.65:8000/";
	//private static final String BASE_URL = "http://localhost:8080/";
	
	private static final String SCAPE_URL = BASE_URL + "fcrepo/rest/scape";

    private static final String FEDORA_URL = BASE_URL + "fcrepo/rest/";

    private final DefaultHttpClient client = new DefaultHttpClient();
    private final HttpHost proxy = new HttpHost("141.66.19.131",8888);

    

    private ScapeMarshaller marshaller;

    private static final Logger LOG = LoggerFactory
            .getLogger(ConnectorTest.class);

    @Before
    public void setup() throws Exception {
        this.marshaller = ScapeMarshaller.newInstance();
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,proxy);
    }

    @Test
    public void testIngestIntellectualEntityAndCheckinFedora() throws Exception {
        IntellectualEntity ie = TestUtil.createTestEntity("entity-1");
        this.postEntity(ie);

        HttpGet get =
                new HttpGet(FEDORA_URL + "/objects/scape/entities/entity-1");
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertTrue(EntityUtils.toString(resp.getEntity()).length() > 0);
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveIntellectualEntity() throws Exception {
        IntellectualEntity ie =
                TestUtil.createTestEntityWithMultipleRepresentations("entity-2");
        this.postEntity(ie);

        HttpGet get = new HttpGet(SCAPE_URL + "/entity/entity-2");
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        IntellectualEntity fetched =
                this.marshaller.deserialize(IntellectualEntity.class, resp
                        .getEntity().getContent());
        assertEquals(ie.getIdentifier(), fetched.getIdentifier());
        assertEquals(LifecycleState.State.INGESTED, fetched.getLifecycleState()
                .getState());
        assertEquals(ie.getRepresentations().size(), fetched
                .getRepresentations().size());
        assertNotNull(ie.getDescriptive());
        assertEquals(ElementContainer.class, ie.getDescriptive().getClass());

        for (Representation r : fetched.getRepresentations()) {
            assertTrue(r.getFiles().size() > 0);
            assertNotNull(r.getTechnical());
            assertNotNull(r.getProvenance());
            assertNotNull(r.getRights());
            assertNotNull(r.getSource());

            for (File f : r.getFiles()) {
                assertNotNull(f.getBitStreams());
                assertTrue(f.getBitStreams().size() > 0);
                assertNotNull(f.getTechnical());
                for (BitStream bs : f.getBitStreams()) {
                    assertNotNull(bs.getTechnical());
                }
            }
        }
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveIntellectualEntityWithRefs()
            throws Exception {
        IntellectualEntity ie =
                TestUtil.createTestEntityWithMultipleRepresentations("entity-ref-1");
        this.postEntity(ie);

        HttpGet get =
                new HttpGet(SCAPE_URL +
                        "/entity/entity-ref-1?useReferences=yes");
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String xml = EntityUtils.toString(resp.getEntity());
        System.out.println(xml);
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveRepresentation() throws Exception {
        IntellectualEntity ie =
                TestUtil.createTestEntityWithMultipleRepresentations("entity-3");
        this.postEntity(ie);

        Representation rep = ie.getRepresentations().get(0);
        HttpGet get =
                new HttpGet(SCAPE_URL + "/representation/entity-3/" +
                        rep.getIdentifier().getValue());
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        Representation fetched =
                this.marshaller.deserialize(Representation.class, resp
                        .getEntity().getContent());
        assertEquals(rep.getIdentifier().getValue(), fetched.getIdentifier()
                .getValue());
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveFile() throws Exception {
        IntellectualEntity ie = TestUtil.createTestEntity("entity-4");
        this.postEntity(ie);

        Representation rep = ie.getRepresentations().get(0);
        File f = ie.getRepresentations().get(0).getFiles().get(0);
        HttpGet get =
                new HttpGet(SCAPE_URL + "/file/entity-4/" +
                        rep.getIdentifier().getValue() + "/" +
                        f.getIdentifier().getValue());
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("image/png", resp.getFirstHeader("Content-Type")
                .getValue());
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveBitstream() throws Exception {
        IntellectualEntity ie =
                TestUtil.createTestEntityWithMultipleRepresentations("entity-5");
        this.postEntity(ie);

        Representation rep = ie.getRepresentations().get(0);
        File f = rep.getFiles().get(0);
        BitStream bs = f.getBitStreams().get(0);
        HttpGet get =
                new HttpGet(SCAPE_URL + "/bitstream/entity-5/" +
                        rep.getIdentifier().getValue() + "/" +
                        f.getIdentifier().getValue() + "/" +
                        bs.getIdentifier().getValue());
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        BitStream fetched =
                (BitStream) this.marshaller.deserialize(resp.getEntity()
                        .getContent());
        assertEquals(bs.getIdentifier().getValue(), fetched.getIdentifier()
                .getValue());
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveLifeCycle() throws Exception {
        IntellectualEntity ie =
                TestUtil.createTestEntityWithMultipleRepresentations("entity-6");
        this.postEntity(ie);

        /* check the lifecycle state */
        HttpGet get = new HttpGet(SCAPE_URL + "/lifecycle/entity-6");
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        LifecycleState state =
                (LifecycleState) this.marshaller.deserialize(resp.getEntity()
                        .getContent());
        assertEquals(LifecycleState.State.INGESTED, state.getState());
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveMetadata() throws Exception {
        IntellectualEntity ie =
                TestUtil.createTestEntityWithMultipleRepresentations("entity-7");
        this.postEntity(ie);

        /* check the desc metadata of the entity */
        HttpGet get = new HttpGet(SCAPE_URL + "/metadata/entity-7/DESCRIPTIVE");
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        Object md = this.marshaller.deserialize(resp.getEntity().getContent());
        assertEquals(md.getClass(), ElementContainer.class);
        get.releaseConnection();

        /* check the tech metadata of the rep */
        get =
                new HttpGet(SCAPE_URL +
                        "/metadata/entity-7/" +
                        ie.getRepresentations().get(0).getIdentifier()
                                .getValue() + "/TECHNICAL");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        md = this.marshaller.deserialize(resp.getEntity().getContent());
        assertEquals(md.getClass(), TextMD.class);
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveIntellectualEntityCollection()
            throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-8");
        this.postEntity(ie1);

        IntellectualEntity ie2 = TestUtil.createTestEntity("entity-9");
        this.postEntity(ie2);

        /* check the desc metadata of the entity */
        HttpPost post = new HttpPost(SCAPE_URL + "/entity-list");
        String uriList =
                FEDORA_URL + "/scape/entity/entity-8\n" + FEDORA_URL +
                        "scape/entity/entity-9";
        post.setEntity(new StringEntity(uriList, ContentType
                .parse("text/uri-list")));
        HttpResponse resp = this.client.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        IntellectualEntityCollection coll =
                this.marshaller.deserialize(IntellectualEntityCollection.class,
                        resp.getEntity().getContent());
        post.releaseConnection();
        assertEquals(2, coll.getEntities().size());
    }

    @Test
    public void testIngestAsyncAndRetrieveLifeCycle() throws Exception {
        IntellectualEntity ie =
                TestUtil.createTestEntityWithMultipleRepresentations("entity-10");
        HttpPost post = new HttpPost(SCAPE_URL + "/entity-async");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(ie, sink);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size()));
        HttpResponse resp = this.client.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String id = EntityUtils.toString(resp.getEntity());
        assertTrue(id.length() > 0);

        /* check the lifecycle state and wait for the entity to be ingested */
        LifecycleState state;
        long start = System.currentTimeMillis();
        do {
            HttpGet get = new HttpGet(SCAPE_URL + "/lifecycle/" + id);
            resp = this.client.execute(get);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            state =
                    (LifecycleState) this.marshaller.deserialize(resp
                            .getEntity().getContent());
            get.releaseConnection();
        } while (!state.getState().equals(State.INGESTED) &&
                (System.currentTimeMillis() - start) < 15000);
        assertEquals(State.INGESTED, state.getState());
    }

    @Test
    public void testIngestAndSearchRepresentation() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-13");
        this.postEntity(ie1);

        IntellectualEntity ie2 = TestUtil.createTestEntity("entity-14");
        this.postEntity(ie2);

        /* search via SRU */
        HttpGet get =
                new HttpGet(SCAPE_URL +
                        "/sru/representations?version=1&operation=searchRetrieve&query=*");
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String xml = EntityUtils.toString(resp.getEntity(), "UTF-8");
        System.out.println(xml);
        assertTrue(0 < xml.length());
        assertTrue(xml
                .indexOf("<scape:identifier type=\"String\"><scape:value>representation-1</scape:value></scape:identifier>") > 0);
        get.releaseConnection();

    }

    @Test
    public void testIngestAndSearchFile() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-15");
        this.postEntity(ie1);

        IntellectualEntity ie2 = TestUtil.createTestEntity("entity-16");
        this.postEntity(ie2);

        /* search via SRU */
        HttpGet get =
                new HttpGet(SCAPE_URL +
                        "/sru/files?version=1&operation=searchRetrieve&query=*");
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String xml = EntityUtils.toString(resp.getEntity(), "UTF-8");
        System.out.println(xml);
        assertTrue(0 < xml.length());
        assertTrue(xml
                .indexOf("<scape:identifier type=\"String\"><scape:value>file-1</scape:value></scape:identifier>") > 0);
        get.releaseConnection();

    }

    @Test
    public void testIngestAndUpdateIntellectualEntity() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-17");
        this.postEntity(ie1);

        org.purl.dc.elements._1.ObjectFactory dcFac =
                new org.purl.dc.elements._1.ObjectFactory();
        ElementContainer cnt = dcFac.createElementContainer();
        SimpleLiteral lit_title = new SimpleLiteral();
        lit_title.getContent().add("Object Updated");
        cnt.getAny().add(dcFac.createTitle(lit_title));

        IntellectualEntity ie2 =
                new IntellectualEntity.Builder(ie1).descriptive(cnt).build();

        /* update the current object */
        HttpPut put = new HttpPut(SCAPE_URL + "/entity/entity-17");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(ie2, sink);
        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* check that the new version is returned */
        HttpGet get = new HttpGet(SCAPE_URL + "/entity/entity-17");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        IntellectualEntity fetched =
                this.marshaller.deserialize(IntellectualEntity.class, resp
                        .getEntity().getContent());
        get.releaseConnection();
        assertNotNull(fetched.getDescriptive());
        assertEquals(ElementContainer.class, fetched.getDescriptive()
                .getClass());
        ElementContainer dc = (ElementContainer) fetched.getDescriptive();
        assertEquals("Object Updated", dc.getAny().get(0).getValue()
                .getContent().get(0));

        /* check that the old version is returned when specifically asked for */
        get = new HttpGet(SCAPE_URL + "/entity/entity-17/1");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        fetched =
                this.marshaller.deserialize(IntellectualEntity.class, resp
                        .getEntity().getContent());
        get.releaseConnection();
        assertNotNull(fetched.getDescriptive());
        assertEquals(ElementContainer.class, fetched.getDescriptive()
                .getClass());
        dc = (ElementContainer) fetched.getDescriptive();
        assertEquals("Object 1", dc.getAny().get(0).getValue().getContent()
                .get(0));

        /* check that the new version is returned when specifically asked for */
        get = new HttpGet(SCAPE_URL + "/entity/entity-17/2");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        fetched =
                this.marshaller.deserialize(IntellectualEntity.class, resp
                        .getEntity().getContent());
        get.releaseConnection();
        assertNotNull(fetched.getDescriptive());
        assertEquals(ElementContainer.class, fetched.getDescriptive()
                .getClass());
        dc = (ElementContainer) fetched.getDescriptive();
        assertEquals("Object Updated", dc.getAny().get(0).getValue()
                .getContent().get(0));
    }

    @Test
    public void testIngestAndUpdateRepresentation() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-18");
        this.postEntity(ie1);

        Representation r =
                new Representation.Builder(ie1.getRepresentations().get(0))
                        .title("title update").build();

        HttpPut put =
                new HttpPut(SCAPE_URL + "/representation/entity-18/" +
                        r.getIdentifier().getValue());
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(r, sink);
        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* fetch the representation and check that the title has been updated */
        HttpGet get =
                new HttpGet(SCAPE_URL + "/representation/entity-18/" +
                        r.getIdentifier().getValue());
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        Representation fetched =
                this.marshaller.deserialize(Representation.class, resp
                        .getEntity().getContent());
        assertEquals(r.getIdentifier().getValue(), fetched.getIdentifier()
                .getValue());
        assertEquals("title update", fetched.getTitle());
        get.releaseConnection();

    }

    @Test
    public void testIngestAndUpdateEntityMetadata() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-19");
        this.postEntity(ie1);

        org.purl.dc.elements._1.ObjectFactory dcFac =
                new org.purl.dc.elements._1.ObjectFactory();
        ElementContainer cnt = dcFac.createElementContainer();
        SimpleLiteral lit_title = new SimpleLiteral();
        lit_title.getContent().add("DC metadata updated");
        cnt.getAny().add(dcFac.createTitle(lit_title));

        HttpPut put =
                new HttpPut(SCAPE_URL + "/metadata/entity-19/DESCRIPTIVE");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(cnt, sink);

        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* fetch the entity and check that the title has been updated */
        HttpGet get =
                new HttpGet(SCAPE_URL + "/metadata/entity-19/DESCRIPTIVE");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        ElementContainer fetched =
                (ElementContainer) this.marshaller.deserialize(resp.getEntity()
                        .getContent());
        assertEquals("DC metadata updated", fetched.getAny().get(0).getValue()
                .getContent().get(0));
        get.releaseConnection();

    }

    @Test
    public void testIngestAndUpdateRepresentationMetadata() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-20");
        this.postEntity(ie1);

        org.purl.dc.elements._1.ObjectFactory dcFac =
                new org.purl.dc.elements._1.ObjectFactory();
        ElementContainer cnt = dcFac.createElementContainer();
        SimpleLiteral lit_title = new SimpleLiteral();
        lit_title.getContent().add("SOURCE metadata updated");
        cnt.getAny().add(dcFac.createTitle(lit_title));

        HttpPut put =
                new HttpPut(SCAPE_URL +
                        "/metadata/entity-20/representation-1/SOURCE");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(cnt, sink);

        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* fetch the entity and check that the title has been updated */
        HttpGet get =
                new HttpGet(SCAPE_URL +
                        "/metadata/entity-20/representation-1/SOURCE");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        ElementContainer fetched =
                (ElementContainer) this.marshaller.deserialize(resp.getEntity()
                        .getContent());
        assertEquals("SOURCE metadata updated", fetched.getAny().get(0)
                .getValue().getContent().get(0));
        get.releaseConnection();

    }

    @Test
    public void testIngestAndUpdateFileMetadata() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-21");
        this.postEntity(ie1);

        HttpPut put =
                new HttpPut(SCAPE_URL +
                        "/metadata/entity-21/representation-1/file-1/TECHNICAL");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(TestUtil.createFITSRecord(), sink);

        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* fetch the entity and check that the title has been updated */
        HttpGet get =
                new HttpGet(SCAPE_URL +
                        "/metadata/entity-21/representation-1/file-1/TECHNICAL");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        Object fetched =
                this.marshaller.deserialize(resp.getEntity().getContent());
        assertEquals(Fits.class, fetched.getClass());
        get.releaseConnection();

    }

    @Test
    public void testIngestAndUpdateBistreamMetadata() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-22");
        this.postEntity(ie1);

        HttpPut put =
                new HttpPut(SCAPE_URL +
                        "/metadata/entity-22/representation-1/file-1/bitstream-1/TECHNICAL");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(TestUtil.createMIXRecord(), sink);

        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* fetch the entity and check that the title has been updated */
        HttpGet get =
                new HttpGet(SCAPE_URL +
                        "/metadata/entity-22/representation-1/file-1/bitstream-1/TECHNICAL");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        Object fetched =
                this.marshaller.deserialize(resp.getEntity().getContent());
        assertEquals(Mix.class, fetched.getClass());
        get.releaseConnection();

    }

    @Test
    public void testIngestAndFetchVersionList() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-23");
        this.postEntity(ie1);

        HttpPut put =
                new HttpPut(SCAPE_URL +
                        "/metadata/entity-23/representation-1/file-1/bitstream-1/TECHNICAL");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(TestUtil.createMIXRecord(), sink);

        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* fetch the entity and check that the title has been updated */
        HttpGet get = new HttpGet(SCAPE_URL + "/entity-version-list/entity-23");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        VersionList fetched =
                (VersionList) this.marshaller.deserialize(resp.getEntity()
                        .getContent());
        assertEquals(2, fetched.getVersionIdentifiers().size());
        get.releaseConnection();

    }

    @Test
    public void testIngestAndUpdateRepresentationAndFetchOldVersion()
            throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-24");
        this.postEntity(ie1);

        Representation r =
                new Representation.Builder(ie1.getRepresentations().get(0))
                        .title("title update").build();

        HttpPut put =
                new HttpPut(SCAPE_URL + "/representation/entity-24/" +
                        r.getIdentifier().getValue());
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(r, sink);
        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* fetch the representation and check that the title has been updated */
        HttpGet get =
                new HttpGet(SCAPE_URL + "/representation/entity-24/" +
                        r.getIdentifier().getValue() + "/1");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        Representation fetched =
                this.marshaller.deserialize(Representation.class, resp
                        .getEntity().getContent());
        assertEquals(r.getIdentifier().getValue(), fetched.getIdentifier()
                .getValue());
        assertEquals("Text representation", fetched.getTitle());
        get.releaseConnection();

    }

    @Test
    public void testIngestAndUpdateFileAndFetchOldVersion() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-25");
        this.postEntity(ie1);

        File f = new File.Builder(ie1.getRepresentations().get(0).getFiles().get(0))
            .uri(URI.create("http://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Wikipedia_wordmark.svg/174px-Wikipedia_wordmark.svg.png"))
            .filename("wikipedia.png")
            .mimetype("image/png")
            .build();

        Representation r = new Representation.Builder(ie1.getRepresentations().get(0))
            .title("title update")
            .files(Arrays.asList(f))
            .build();

        HttpPut put = new HttpPut(SCAPE_URL + "/representation/entity-25/" + r.getIdentifier().getValue());
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(r, sink);
        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* fetch the file */
        HttpGet get =new HttpGet(SCAPE_URL + "/file/entity-25/" + r.getIdentifier().getValue() + "/" + f.getIdentifier().getValue() + "/1");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        get.releaseConnection();

    }

    @Test
    public void testIngestAndUpdateBitstreamAndFetchOldVersion() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-26");
        this.postEntity(ie1);

        HttpPut put =
                new HttpPut(SCAPE_URL +
                        "/metadata/entity-26/representation-1/file-1/bitstream-1/TECHNICAL");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(TestUtil.createMIXRecord(), sink);

        put.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size(), ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        put.releaseConnection();

        /* fetch the entity and check that the title has been updated */
        HttpGet get =
                new HttpGet(SCAPE_URL +
                        "/bitstream/entity-26/representation-1/file-1/bitstream-1/1");
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        BitStream fetched =
                (BitStream) this.marshaller.deserialize(resp.getEntity().getContent());
        assertEquals(Fits.class, fetched.getTechnical().getClass());
        get.releaseConnection();

    }

    @Test
    public void testIngestAndSearchEntity() throws Exception {
        IntellectualEntity ie1 = TestUtil.createTestEntity("entity-11");
        this.postEntity(ie1);

        IntellectualEntity ie2 = TestUtil.createTestEntity("entity-12");
        this.postEntity(ie2);

        /* search via SRU */
        HttpGet get =
                new HttpGet(SCAPE_URL +
                        "/sru/entities?version=1&operation=searchRetrieve&query=*");
        HttpResponse resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String xml = EntityUtils.toString(resp.getEntity(), "UTF-8");
        System.out.println(xml);
        assertTrue(0 < xml.length());
        get.releaseConnection();

    }

    //@Test
    // Test läuft nicht, weil er die Dateien nicht findet. Musst mal gucken an was das liegt. 
    public void testIngestAndRetrieveONBIntellectualEntity() throws Exception {
        /* create the test files */
        new java.io.File("00000001.jp2").createNewFile();
        new java.io.File("00000001.txt").createNewFile();
        new java.io.File("00000001.html").createNewFile();

        HttpPost post = new HttpPost(SCAPE_URL + "/entity");
        post.setEntity(new InputStreamEntity(this.getClass().getClassLoader().getResourceAsStream("ONB_mets_example.xml"), -1, ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(post);
        String id = EntityUtils.toString(resp.getEntity());
        assertEquals(201, resp.getStatusLine().getStatusCode());
        post.releaseConnection();

        HttpGet get =
                new HttpGet(SCAPE_URL + "/entity/" + id);
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        IntellectualEntity ie = this.marshaller.deserialize(IntellectualEntity.class, resp.getEntity().getContent());
        assertEquals(id, ie.getIdentifier().getValue());
        get.releaseConnection();
    }

    private void postEntity(IntellectualEntity ie) throws IOException {
        HttpPost post = new HttpPost(SCAPE_URL + "/entity");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try {
            this.marshaller.serialize(ie, sink);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size()));
        HttpResponse resp = this.client.execute(post);
        assertEquals(201, resp.getStatusLine().getStatusCode());
        String id = EntityUtils.toString(resp.getEntity());
        assertTrue(id.length() > 0);
        post.releaseConnection();
    }



}
