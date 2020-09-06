package PhoneDealer;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class DayCounter extends Agent{
	public static final int NUM_DAYS = 100;
	
	@Override
	protected void setup() {
	//add to yellow pages
	DFAgentDescription dfd = new DFAgentDescription();
	dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType("ticker-agent");
	sd.setName(getLocalName() + "-ticker-agent");
	dfd.addServices(sd);
	try {
		DFService.register(this, dfd);
	}
	catch(FIPAException e) {
		e.printStackTrace();
	}
	doWait(1000);
	addBehaviour(new SynchAgentBehaviour(this));
	}
	
	@Override public void takeDown() {
		//deregister from yellow pages
		try {
			DFService.deregister(this);
		}catch(FIPAException e){
			e.printStackTrace();
		}
	}
	
	public class SynchAgentBehaviour extends Behaviour{
		private int step = 0;
		private int numFinReceived = 0;
		private int day = 0;
		private ArrayList<AID> simulationAgents = new ArrayList<>();
		
		public SynchAgentBehaviour(Agent a) {
			super(a);
		}
		
		@Override
		public void action() {
			switch(step) {
			case 0:
				//find agents registered with directory
				DFAgentDescription template1 = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("customer");
				template1.addServices(sd);
				DFAgentDescription template2 = new DFAgentDescription();
				ServiceDescription sd2 = new ServiceDescription();
				sd2.setType("manufacturer");
				template2.addServices(sd2);
				DFAgentDescription template3 = new DFAgentDescription();
				ServiceDescription sd3 = new ServiceDescription();
				sd3.setType("seller");
				template3.addServices(sd3);
				try {
					DFAgentDescription[] agentTypes1 = DFService.search(myAgent, template1);
					for (int i = 0; i < agentTypes1.length; i++)
						simulationAgents.add(agentTypes1[i].getName());//This is the AID
					DFAgentDescription[] agentTypes2 = DFService.search(myAgent, template2);
					for (int i = 0; i < agentTypes2.length; i++)
						simulationAgents.add(agentTypes2[i].getName());//This is the AID
					DFAgentDescription[] agentTypes3 = DFService.search(myAgent, template3);
					for (int i = 0; i < agentTypes3.length; i++)
						simulationAgents.add(agentTypes3[i].getName());//This is the AID
				}
				catch(FIPAException e) {
					e.printStackTrace();
				}
				//send new day message to agents
				ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
				tick.setContent("new-day");
				for(AID id : simulationAgents)
				{
					tick.addReceiver(id);
				}
				myAgent.send(tick);
				System.out.println("Start of day " + (day + 1));
				step++;
				day++;
				break;
			case 1:
				//wait to receive done message from all agents
				MessageTemplate mt = MessageTemplate.MatchContent("done");
				ACLMessage msg = myAgent.receive(mt);
				if(msg != null) {
					numFinReceived++;
					if(numFinReceived >= simulationAgents.size())
						step++;
				}
				else
					block();
			}
		}
		@Override
		public boolean done() {
			return step == 2;
		}
		@Override
		public void reset() {
			super.reset();
			step = 0;
			simulationAgents.clear();
			numFinReceived = 0;
		}
		
		@Override
		public int onEnd()
		{
			System.out.println("End of Day " + day);
			if(day == NUM_DAYS) {
				//send termination message
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("terminate");
				for(AID agent : simulationAgents)
					msg.addReceiver(agent);
				myAgent.send(msg);
				myAgent.doDelete();
			}
			else
			{
				reset();
				myAgent.addBehaviour(this);
			}
			
			return 0;
		}
	}
}
