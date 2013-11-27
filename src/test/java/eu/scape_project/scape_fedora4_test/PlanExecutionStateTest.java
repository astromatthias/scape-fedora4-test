package eu.scape_project.scape_fedora4_test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.xml.bind.JAXBException;

import junit.framework.Assert;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.scape_project.model.*;
import eu.scape_project.model.plan.PlanExecutionState;
import eu.scape_project.model.plan.PlanExecutionState.ExecutionState;
import eu.scape_project.util.ScapeMarshaller;

/**
 * Produces XML output of a ExecutionState needed to POST the execution state to Plan Managmenet API 
 * meant for manual test (the PlanTest does it automatically
 * @author mhn
 *
 */

public class PlanExecutionStateTest {
	
   private ScapeMarshaller marshaller;
   private static final Logger LOG = LoggerFactory.getLogger(PlanExecutionStateTest.class);	
   
   
   @Before
   public void setup() throws Exception {
       this.marshaller = ScapeMarshaller.newInstance();
   }

   @Test
   public void generate() throws UnsupportedEncodingException, JAXBException { 
	 for (ExecutionState state : ExecutionState.values()) {
		 this.create(state);  	
	}
   }
   
   
   public void create(ExecutionState state ) throws JAXBException, UnsupportedEncodingException {
	   ByteArrayOutputStream sink = new ByteArrayOutputStream();	
	   PlanExecutionState planState = new PlanExecutionState(new Date(), state);
	   marshaller.newInstance().serialize(planState,sink);
	   String plan_execution_state =  new String(sink.toByteArray());
	   assertTrue(plan_execution_state.contains(state.toString()));
	   //LOG.info(plan_execution_state);
	   System.out.println(plan_execution_state);
	}

}
