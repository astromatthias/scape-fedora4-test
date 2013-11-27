package eu.scape_project.scape_fedora4_test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.http.conn.params.ConnRoutePNames;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA_2_3.portable.OutputStream;

import eu.scape_project.model.*;
import eu.scape_project.util.ScapeMarshaller;

public class TestdatenTest {
	
	private ScapeMarshaller marshaller;
	
	private static String dir = "/tmp/testdaten/";
	private static int count = 1000;
	
	private static String xmldcl = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
	
	@Before
    public void setup() throws Exception {
        this.marshaller = ScapeMarshaller.newInstance();
    }
	
	@Test
	public void create() throws JAXBException, IOException {
		 
		 // create tmp directory
		 File dirs = new File(dir);
		 if (dirs.exists()) { 
			 dirs.delete();
		 }
		 dirs.mkdirs();
		 
		 //create digital objects
		 for (int i = 10; i < count; i++) {
			 IntellectualEntity ie = TestUtil.createTestEntity("entity-"+i );
			 ByteArrayOutputStream sink = new ByteArrayOutputStream();
		     this.marshaller.serialize(ie, sink);
		       
			 File file = new File(dir + "entity-"+i+".xml");
			 FileOutputStream os = new FileOutputStream(file);
			 String result =  xmldcl + sink.toString();
			 os.write(result.getBytes());
			 os.close();
		}
		 
		// create tesdaten.csv for JMETER tests  
		 File tdfile = new File(dir + "testdaten.csv");
		 StringBuilder testdaten = new StringBuilder();
		 for (int i = 1; i < count; i++) {
			testdaten.append(dir + "entity-"+i+".xml,entity-"+i+",text/xml\n");	
		 }
		 FileOutputStream tdos = new FileOutputStream(tdfile);
		 tdos.write(testdaten.toString().getBytes());
		 tdos.close();
		 
		 
	}

}
