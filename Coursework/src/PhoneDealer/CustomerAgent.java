package PhoneDealer;

import java.util.ArrayList;
import java.util.List;

import jade.content.Concept;
import jade.content.ContentElement;
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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.content.onto.basic.Action;
import PhoneDealerOntology.ECommerceOntology;
import PhoneDealerOntologyElements.*;
import java.util.Random;

public class CustomerAgent extends Agent{
	private Codec codec = new SLCodec();
	private Ontology ontology = ECommerceOntology.getInstance();
	private ArrayList<AID> manufacturers = new ArrayList<>();
	private AID tickerAgent;
	private Order order = new Order();
	private int ordersSent;
	private int ordersReceived;
	ArrayList<CyclicBehaviour> cbs = new ArrayList<>();
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("customer");
		sd.setName(getLocalName() + "-customer-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch(FIPAException e)
		{
			e.printStackTrace();
		}
		
		addBehaviour(new Tick(this));
		}
	
	public class Tick extends CyclicBehaviour{
		public Tick(Agent a) {
			super(a);
		}
		
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new-day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null)
			{
				if(tickerAgent == null)
					tickerAgent = msg.getSender();
				if(msg.getContent().equals("new-day"))
				{
					myAgent.addBehaviour(new FindManufacturer());
					myAgent.addBehaviour(new GenerateOrder());
					CyclicBehaviour GO = new GetOrder();
					myAgent.addBehaviour(GO);
					cbs.add(GO);
					myAgent.addBehaviour(new EndDay());
					
				}
				else if(msg.getContent().equals("terminate")) {
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
	
	public class GenerateOrder extends OneShotBehaviour
	{
		@Override
		public void action() {
			//prepare phone
			Phone phone = new Phone();
			double typeRand = Math.random();
			double StoreRand = Math.random();
			double RAMRand = Math.random();
			Component storage = new Component();
			storage.setType("storage");
			if(StoreRand < 0.5)
				storage.setValue(64);
			else
				storage.setValue(256);
			Component screenSize = new Component();
			screenSize.setType("ScreenSize");
			Component RAM = new Component();
			RAM.setType("RAM");
			if(RAMRand < 0.5)
				RAM.setValue(4);
			else
				RAM.setValue(8);
			Component battery = new Component();
			battery.setType("battery");
			String name = "";
			if(typeRand> 0.5)
			{
				name = "Small";
				battery.setValue(2000);
				screenSize.setValue(5);
			}
			else
			{
				name = "Phablet";
				battery.setValue(3000);
				screenSize.setValue(7);
			}
			phone.setName(name);
			phone.setRAM(RAM);
			phone.setStorage(storage);
			phone.setScreenSize(screenSize);
			phone.setBattery(battery);
			int OrderSize = (int) Math.floor(1 + 50 * Math.random());
			//int OrderSize = 10;
			int price = (int) Math.floor(100 + 500 * Math.random());
			int DaysDue = (int) Math.floor(1 + 10 * Math.random());
			int PenaltyFee = ((int) Math.floor(1 + 50 * Math.random())) * OrderSize;
			phone.setPrice(price);
			order.setQuantity(OrderSize);
			order.setDueDate(DaysDue);
			order.setLateFee(PenaltyFee);
			order.setPrice(price * OrderSize);
			order.setBuyer(myAgent.getAID());
			//prepare request message
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			for(int i = 0; i < manufacturers.size(); i++)
			for(AID man : manufacturers)
				msg.addReceiver(man);
			msg.setLanguage(codec.getName());
			msg.setOntology(ontology.getName());
			//create wrapper action object using action and AID
			Action request = new Action();
			request.setAction(order);
			request.setActor(getAID());
			try {
				order.setPhone(phone);
				getContentManager().fillContent(msg, request);
				send(msg);
				ordersSent++;
			}
			catch(CodecException ce) {
				ce.printStackTrace();
			}
			catch(OntologyException oe){
				oe.printStackTrace();
			}
		}
	}
	public class GetOrder extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
			ACLMessage msg = receive(mt);
			if(msg != null)
			{
				try {
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					
					if(ce instanceof Action) {
						Concept action = ((Action)ce).getAction();
						if(action instanceof Deliver)
						{
							ordersReceived++;
							Deliver delivery = (Deliver)action;
							
							ACLMessage invoice = new ACLMessage(ACLMessage.INFORM);
							invoice.addReceiver(msg.getSender());
							invoice.setConversationId("Invoice");
							
							invoice.setContent(Integer.toString(delivery.getOrder().getPrice()));
							myAgent.send(invoice);
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
