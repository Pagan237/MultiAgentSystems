package PhoneDealer;

import java.util.ArrayList;

import PhoneDealerOntology.ECommerceOntology;
import PhoneDealerOntologyElements.Component;
import PhoneDealerOntologyElements.Deliver;
import PhoneDealerOntologyElements.Item;
import PhoneDealerOntologyElements.Order;
import PhoneDealerOntologyElements.Phone;
import PhoneDealerOntologyElements.ComponentOrder;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ManufacturerAgent extends Agent {
	private Codec codec = new SLCodec();
	private Ontology ontology = ECommerceOntology.getInstance();
	private ArrayList<AID> sellers = new ArrayList<>();
	private ArrayList<AID> customers = new ArrayList<>();
	private AID tickerAgent;
	private int ordersCompleted;
	private ArrayList<Component> Screens5 = new ArrayList<>();
	private ArrayList<Component> Screens7 = new ArrayList<>();
	private ArrayList<Component> Batteries2000 = new ArrayList<>();
	private ArrayList<Component> Batteries3000 = new ArrayList<>();
	private ArrayList<Component> RAMs4 = new ArrayList<>();
	private ArrayList<Component> RAMs8 = new ArrayList<>();
	private ArrayList<Component> Storage64 = new ArrayList<>();
	private ArrayList<Component> Storage256 = new ArrayList<>();
	private ArrayList<Order> orders = new ArrayList<>();
	private ArrayList<Order> todaysOrders = new ArrayList<>();
	private ArrayList<Component> compsNeeded = new ArrayList<>();
	private long profits;
	int days = 1;
	int LateFeeTotal = 0;
	int ActualOrdered = 0;
	int ExpectedOrdered = 0;
	int ComponentsUsed = 0;
	private long startDayProfits;
	private long orderCost;
	private long DailyIntake;
	private int Invoices = 0;
	private int ordersReceived;
	private int DailyLimit = 50;
	private int deliveries = 0;
	ArrayList<ComponentOrder> OrdersSent = new ArrayList<>();
	ArrayList<ComponentOrder> OrdersPlaced = new ArrayList<>();
	ArrayList<ComponentOrder> DeliveriesExpected = new ArrayList<>();
	ArrayList<CyclicBehaviour> cbs = new ArrayList<>();

	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("manufacturer");
		sd.setName(getLocalName() + "-manufacturer-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		addBehaviour(new Tick(this));
	}

	public class Tick extends CyclicBehaviour {
		public Tick(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new-day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (tickerAgent == null)
					tickerAgent = msg.getSender();
				if (msg.getContent().equals("new-day")) {
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					dailyActivity.addSubBehaviour(new FindCustomers());
					dailyActivity.addSubBehaviour(new findSellers());
					dailyActivity.addSubBehaviour(new OrderReview());
					dailyActivity.addSubBehaviour(new OrderComponents());
					dailyActivity.addSubBehaviour(new DeliveryListener());
					dailyActivity.addSubBehaviour(new CompleteOrder(myAgent));
					dailyActivity.addSubBehaviour(new InvoiceListener());
					dailyActivity.addSubBehaviour(new CalculateProfits());
					dailyActivity.addSubBehaviour(new EndDay());
					myAgent.addBehaviour(dailyActivity);
				} else if (msg.getContent().equals("terminate")) {
					myAgent.doDelete();
				}
			}
		}
	}

	public class FindCustomers extends OneShotBehaviour {
		public void action() {
			DFAgentDescription custTemplate = new DFAgentDescription();
			ServiceDescription custSD = new ServiceDescription();
			custSD.setType("customer");
			custTemplate.addServices(custSD);
			try {
				customers.clear();
				DFAgentDescription[] agentType = DFService.search(myAgent, custTemplate);
				for (int i = 0; i < agentType.length; i++)
					customers.add(agentType[i].getName());
			} catch (FIPAException e) {
				e.printStackTrace();
			}
		}
	}

	public class findSellers extends OneShotBehaviour {
		public void action() {
			DFAgentDescription sellerTemplate = new DFAgentDescription();
			ServiceDescription sellSD = new ServiceDescription();
			sellSD.setType("seller");
			sellerTemplate.addServices(sellSD);
			try {
				sellers.clear();
				DFAgentDescription[] agentType = DFService.search(myAgent, sellerTemplate);
				for (int i = 0; i < agentType.length; i++)
					sellers.add(agentType[i].getName());
			} catch (FIPAException e) {
				e.printStackTrace();
			}
		}
	}

	public class OrderReview extends Behaviour {
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = receive(mt);
			if (msg != null) {
				try {
					ContentElement ce = null;
					// convert string to object
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Action) {
						Concept action = ((Action) ce).getAction();
						if (action instanceof Order) {
							Order ord = (Order) action;
							Phone it = ord.getPhone();
							if (it instanceof Phone) {
								ordersReceived++;
								int costOfPhone = 0;
								Component comp = new Component();
								comp = it.getBattery();
								if (comp.getValue() == 2000)
									costOfPhone += 70;
								else
									costOfPhone += 100;
								comp = it.getScreenSize();
								if (comp.getValue() == 5)
									costOfPhone += 100;
								else
									costOfPhone += 150;
								comp = it.getScreenSize();
								if (ord.getDueDate() < 4) {
									comp = it.getRAM();
									if (comp.getValue() == 4)
										costOfPhone += 30;
									else
										costOfPhone += 60;
									comp = it.getStorage();
									if (comp.getValue() == 64)
										costOfPhone += 25;
									else
										costOfPhone += 50;
								} else {
									comp = it.getRAM();
									if (comp.getValue() == 4)
										costOfPhone += 20;
									else
										costOfPhone += 35;
									comp = it.getStorage();
									if (comp.getValue() == 64)
										costOfPhone += 15;
									else
										costOfPhone += 40;
								}
								if (it.getPrice() > 0 && orders.size() < 7 && days <= 95) {
									ExpectedOrdered += ord.getQuantity()*4;
									ACLMessage reply = msg.createReply();
									reply.setPerformative(ACLMessage.INFORM);
									reply.setContent("order accepted");
									reply.setConversationId(msg.getConversationId());
									send(reply);
									orders.add(ord);
									todaysOrders.add(ord);
									for (int i = 0; i < ord.getQuantity(); i++) {
										Component RAM = ord.getPhone().getRAM();
										Component Storage = ord.getPhone().getStorage();
										Component Battery = ord.getPhone().getBattery();
										Component Screen = ord.getPhone().getScreenSize();
										compsNeeded.add(RAM);
										compsNeeded.add(Storage);
										compsNeeded.add(Battery);
										compsNeeded.add(Screen);
									}

									System.out.println("Cost to make Phone: " + costOfPhone + ". Invoice: "
											+ (ord.getPrice() / ord.getQuantity()) + " Accepted");
								} else {
									ACLMessage reply = msg.createReply();
									reply.setPerformative(ACLMessage.INFORM);
									reply.setContent("order declined");
									reply.setConversationId(msg.getConversationId());
									send(reply);
									System.out.println("Cost to make Phone: " + costOfPhone + ". Invoice: "
											+ (ord.getPrice() / ord.getQuantity()) + " Declined");
								}
							}
						}
					}
				} catch (CodecException ce) {
					ce.printStackTrace();
				} catch (OntologyException oe) {
					oe.printStackTrace();
				}
			} else {
				block();
			}
		}

		public boolean done() {
			return ordersReceived == customers.size();
		}
	}

	public class OrderComponents extends OneShotBehaviour {
		public void action() {
			if (todaysOrders.size() > 0) {
				for (Order ord : todaysOrders) {
					for (int i = 0; i < ord.getQuantity() * 4; i++) {
						ComponentOrder comps = new ComponentOrder();
						Component comp = compsNeeded.get(0);
						if (comp.getType().equals("RAM")) {
							if (ord.getDueDate() < 4) {
								// supplier1
								if (comp.getValue() == 4) {
									comp.setPrice(30);
									comps.setComp(comp);
									comps.setDays(1);
									comps.setSeller(sellers.get(0));
								} else {
									comp.setPrice(60);
									comps.setComp(comp);
									comps.setDays(1);
									comps.setSeller(sellers.get(0));
								}
							} else {
								if (comp.getValue() == 4) {
									comp.setPrice(20);
									comps.setComp(comp);
									comps.setDays(4);
									comps.setSeller(sellers.get(1));
								} else {
									comp.setPrice(35);
									comps.setComp(comp);
									comps.setDays(4);
									comps.setSeller(sellers.get(1));
								}
							}
						}
						if (comp.getType().equals("storage")) {
							if (ord.getDueDate() < 4) {
								// supplier1
								if (comp.getValue() == 64) {
									comp.setPrice(25);
									comps.setComp(comp);
									comps.setDays(1);
									comps.setSeller(sellers.get(0));
								} else {
									comp.setPrice(50);
									comps.setComp(comp);
									comps.setDays(1);
									comps.setSeller(sellers.get(0));
								}
							} else {
								if (comp.getValue() == 64) {
									comp.setPrice(15);
									comps.setComp(comp);
									comps.setDays(4);
									comps.setSeller(sellers.get(1));
								} else {
									comp.setPrice(40);
									comps.setComp(comp);
									comps.setDays(4);
									comps.setSeller(sellers.get(1));
								}
							}
						}
						if (comp.getType().equals("ScreenSize")) {
							if (comp.getValue() == 5) {
								comp.setPrice(100);
								comps.setComp(comp);
								comps.setDays(1);
								comps.setSeller(sellers.get(0));
							} else {
								comp.setPrice(150);
								comps.setComp(comp);
								comps.setDays(1);
								comps.setSeller(sellers.get(0));
								
							}
						}
						if (comp.getType().equals("battery")) {
							if (comp.getValue() == 2000) {
								comp.setPrice(70);
								comps.setComp(comp);
								comps.setDays(1);
								comps.setSeller(sellers.get(0));
							} else {
								comp.setPrice(100);
								comps.setComp(comp);
								comps.setDays(1);
								comps.setSeller(sellers.get(0));
							}
						}
						comps.setPrice(comp.getPrice());
						OrdersPlaced.add(comps);
						orderCost += comp.getPrice();
						compsNeeded.remove(0);
					}
				}
			}
			try {
				for (int i = 0; i < DeliveriesExpected.size(); i++) {
					Action action = new Action();
					action.setAction(DeliveriesExpected.get(i));
					action.setActor(getAID());
					ACLMessage buy = new ACLMessage(ACLMessage.REQUEST);
					buy.setLanguage(codec.getName());
					buy.setOntology(ontology.getName());
					buy.setConversationId("CompOrder");
					buy.addReceiver(DeliveriesExpected.get(i).getSeller());
					getContentManager().fillContent(buy, action);
					send(buy);
				}
			} catch (CodecException ce) {
				ce.printStackTrace();
			} catch (OntologyException oe) {
				oe.printStackTrace();
			}
		}
	}

	public class DeliveryListener extends Behaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("Delivery");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				try {
					ContentElement ce = null;

					ce = getContentManager().extractContent(msg);
					if (ce instanceof Action) {
						Concept action = ((Action) ce).getAction();
						if (action instanceof ComponentOrder) {
							
							deliveries++;
							ComponentOrder compOrd = (ComponentOrder) action;
							Component comp = compOrd.getComp();
							if (comp.getType().equals("RAM")) {
								if (comp.getValue() == 4)
									RAMs4.add(comp);
								else
									RAMs8.add(comp);
							}
							if (comp.getType().equals("battery")) {
								if(comp.getValue() == 2000)
									Batteries2000.add(comp);
								else
									Batteries3000.add(comp);
							}
							if (comp.getType().equals("storage")) {
								if(comp.getValue() == 64)
									Storage64.add(comp);
								else
									Storage256.add(comp);
							}
							if (comp.getType().equals("ScreenSize")) {
								if(comp.getValue() == 5)
									Screens5.add(comp);
								else
									Screens7.add(comp);
							}
						}
					}
				} catch (CodecException ce) {
					ce.printStackTrace();
				} catch (OntologyException oe) {
					oe.printStackTrace();
				}
			}
		}

		public boolean done() {
			return deliveries == DeliveriesExpected.size();
		}
	}

	public class CompleteOrder extends Behaviour {
		public CompleteOrder(Agent a) {
			super(a);
		}

		int Pending = 0;

		@Override
		public void action() {
			if (orders.isEmpty() == false) {
				try {
					for (int i = 0; i < orders.size(); i++) {
						Order o = orders.get(i);
						int screenCount = 0;
						int batCount = 0;
						int ramCount = 0;
						int storageCount = 0;
						if(o.getPhone().getScreenSize().getValue() == 5)
							screenCount = Screens5.size();
						if(o.getPhone().getScreenSize().getValue() == 7)
							screenCount = Screens7.size();
						if(o.getPhone().getBattery().getValue() == 2000)
							batCount = Batteries2000.size();
						if(o.getPhone().getBattery().getValue() == 3000)
							batCount = Batteries3000.size();
						if(o.getPhone().getStorage().getValue() == 64)
							storageCount = Storage64.size();
						if(o.getPhone().getStorage().getValue() == 256)
							storageCount = Storage256.size();
						if(o.getPhone().getRAM().getValue() == 4)
							ramCount = RAMs4.size();
						if(o.getPhone().getRAM().getValue() == 8)
							ramCount = RAMs8.size();
						if (o.getQuantity() <= DailyLimit) {
							if (storageCount >= o.getQuantity() && screenCount >= o.getQuantity()
									&& ramCount >= o.getQuantity() && batCount >= o.getQuantity()) {
								DailyLimit -= o.getQuantity();
								Phone phone = new Phone();
								phone.setBattery(o.getPhone().getBattery());
								phone.setScreenSize(o.getPhone().getScreenSize());
								phone.setStorage(o.getPhone().getStorage());
								phone.setRAM(o.getPhone().getRAM());
								phone.setName(o.getPhone().getName());
								ordersCompleted += 1;
								ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
								msg.addReceiver(o.getBuyer());
								msg.setLanguage(codec.getName());
								msg.setOntology(ontology.getName());

								Deliver deliver = new Deliver();
								deliver.setOrder(o);

								Action action = new Action();
								action.setAction(deliver);
								action.setActor(getAID());

								getContentManager().fillContent(msg, action);
								send(msg);
								for(int j = 0; j < o.getQuantity(); j++) {
									if(o.getPhone().getRAM().getValue() == 4)
										RAMs4.remove(0);
									else if(o.getPhone().getRAM().getValue() == 8)
										RAMs8.remove(0);
									if(o.getPhone().getBattery().getValue() == 2000)
										Batteries2000.remove(0);
									else if(o.getPhone().getBattery().getValue() == 3000)
										Batteries3000.remove(0);
									if(o.getPhone().getScreenSize().getValue() == 5)
										Screens5.remove(0);
									else if(o.getPhone().getScreenSize().getValue() == 7)
										Screens7.remove(0);
									if(o.getPhone().getStorage().getValue() == 64)
										Storage64.remove(0);
									else if(o.getPhone().getStorage().getValue() == 256)
										Storage256.remove(0);
								}
								orders.remove(i);
								i--;
							} else {
								System.out.println("Components Not Available");
								Pending++;
							}
						} else {
							System.out.println("Order too big to complete");
							Pending++;
						}
					}
				} catch (CodecException ce) {
					ce.printStackTrace();
				} catch (OntologyException oe) {
					oe.printStackTrace();
				}
			}
		}

		@Override
		public boolean done() {
			return DailyLimit == 0 || orders.isEmpty() || Pending == orders.size();
		}
	}

	public class InvoiceListener extends Behaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("Invoice");
			ACLMessage msg = receive(mt);
			if (msg != null) {
				int invoice = Integer.parseInt(msg.getContent());
				DailyIntake += invoice;
				Invoices += 1;
			}
		}

		public boolean done() {
			return Invoices == ordersCompleted;
		}
	}

	public class CalculateProfits extends OneShotBehaviour {
		public void action() {
			int LateFees = 0;
			for(Order o : orders) {
				if(o.getDueDate() <= 0) {
					LateFees += o.getLateFee();
				}
			}
			LateFeeTotal += LateFees;
			System.out.println("Late Fees: " + LateFees);
			int WarehouseFee = (Screens5.size() + Screens7.size() + Batteries2000.size() + Batteries3000.size() +
					RAMs4.size() + RAMs8.size() + Storage64.size() + Storage256.size())*5;
			System.out.println("Warehouse fees: " + WarehouseFee);
			profits = profits + DailyIntake - WarehouseFee - LateFees - orderCost;
		}
	}

	public class EndDay extends OneShotBehaviour {
		public void action() {
			startDayProfits = profits;
			DailyIntake = 0;
			orderCost = 0;
			ordersCompleted = 0;
			ordersReceived = 0;
			deliveries = 0;
			DailyLimit = 50;
			todaysOrders.clear();
			Invoices = 0;
			days++;
			DeliveriesExpected.clear();
			for (Order o : orders) {
				o.setDueDate(o.getDueDate() - 1);
			}
			for (int i = 0; i < OrdersPlaced.size(); i++) {
				ComponentOrder comps = OrdersPlaced.get(i);
				comps.DecreaseDay();
				if (comps.getDays() == 0) {
					DeliveriesExpected.add(comps);
					OrdersPlaced.remove(i);
					i--;
				}
			}
			
			for(int i = 0; i < orders.size(); i++)
			{
				if(orders.get(i).getDueDate() <= orders.get(0).getDueDate()) {
					Order o = orders.get(i);
					orders.remove(i);
					orders.add(0, o);
				}
			}
			
			System.out.println("Late fees acca: " + LateFeeTotal);
			System.out.println("Profits: " + profits);
			System.out.println("Orders to complete: " + orders.size());
			ACLMessage end = new ACLMessage(ACLMessage.INFORM);
			end.setContent("done");
			end.setConversationId("End Day");
			end.addReceiver(tickerAgent);
			for (AID cust : customers)
				end.addReceiver(cust);
			for (AID sup : sellers)
				end.addReceiver(sup);
			myAgent.send(end);
			for (Behaviour b : cbs)
				myAgent.removeBehaviour(b);
		}
	}
}
