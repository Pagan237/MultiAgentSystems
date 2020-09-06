package PhoneDealer;

import java.util.ArrayList;
import java.util.List;

import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.content.ContentElement;
import jade.content.Concept;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.content.onto.basic.Action;
import PhoneDealerOntology.ECommerceOntology;
import PhoneDealerOntologyElements.*;

public class SellerAgent extends Agent{
	private Codec codec = new SLCodec();
	private Ontology ontology = ECommerceOntology.getInstance();
	private ArrayList<AID> manufacturers = new ArrayList<>();
	private AID tickerAgent;
	ArrayList<CyclicBehaviour> cbs = new ArrayList<>();
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("seller");
		sd.setName(getLocalName() + "-seller-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch(FIPAException e) {
			e.printStackTrace();
		}
		addBehaviour(new Tick());
	}
	
	public class Tick extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt  = MessageTemplate.or(MessageTemplate.MatchContent("new-day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				if(tickerAgent==null)
					tickerAgent = msg.getSender();
				if (msg.getContent().equals("new-day"))
				{
					System.out.println("Supplier Day Started");
					myAgent.addBehaviour(new FindManufacturer());
					CyclicBehaviour SC = new SendComponent();
					myAgent.addBehaviour(SC);
					cbs.add(SC);
					CyclicBehaviour ED = new EndDay();
					myAgent.addBehaviour(ED);
					cbs.add(ED);
				}
				else if(msg.getContent().equals("terminate")){
					myAgent.doDelete();
				}
			}	
		}
	}
	
	public class FindManufacturer extends OneShotBehaviour{
		public void action() {
			DFAgentDescription manTemplate = new DFAgentDescription();
			ServiceDescription manSD = new ServiceDescription();
			manSD.setType("manufacturer");
			manTemplate.addServices(manSD);
			try{
				manufacturers.clear();
				DFAgentDescription[] agentType = DFService.search(myAgent, manTemplate);
				for (int i = 0; i < agentType.length; i++)
					manufacturers.add(agentType[i].getName());
			}
			catch(FIPAException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class SendComponent extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("CompOrder");
			ACLMessage msg = myAgent.receive(mt);
			if(msg!= null)
			{
				try {
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if(ce instanceof Action)
					{
						Concept action = ((Action)ce).getAction();
						if(action instanceof ComponentOrder)
						{
							ComponentOrder comp = (ComponentOrder)action;
							ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
							reply.setConversationId("Delivery");
							reply.addReceiver(msg.getSender());
							reply.setLanguage(codec.getName());
							reply.setOntology(ontology.getName());
							
							Action myReply = new Action();
							myReply.setAction(comp);
							myReply.setActor(getAID());
							
							getContentManager().fillContent(reply, myReply);
							send(reply);
						}
					}
				}
				catch(CodecException ce) {
					ce.printStackTrace();
				}
				catch(OntologyException oe) {
					oe.printStackTrace();
				}
			}
		}
	}
	
	public class EndDay extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("End Day");
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				ACLMessage end = new ACLMessage(ACLMessage.INFORM);
				end.addReceiver(tickerAgent);
				end.setContent("done");
				myAgent.send(end);
				for(Behaviour b : cbs)
					myAgent.removeBehaviour(b);
				
			}
		}
	}
}
