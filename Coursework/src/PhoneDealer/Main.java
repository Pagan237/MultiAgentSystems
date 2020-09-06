package PhoneDealer;
import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;



public class Main {
	public static void main(String[] args) {
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		
		try {
			ContainerController myContainer = myRuntime.createMainContainer(myProfile);
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
			for(int i = 0; i < 3; i++) {
				AgentController customer = myContainer.createNewAgent("customer1" + i, CustomerAgent.class.getCanonicalName(),
						null);
				customer.start(); 
			}
			AgentController dayCounter = myContainer.createNewAgent("dayCounter", DayCounter.class.getCanonicalName(),
					null);
			dayCounter.start();
			AgentController ManufacturerAgent = myContainer.createNewAgent("manufacturer", ManufacturerAgent.class.getCanonicalName(),
					null);
			ManufacturerAgent.start();
			AgentController SellerAgent1 = myContainer.createNewAgent("seller1", SellerAgent.class.getCanonicalName(), 
					null);
			SellerAgent1.start();
			AgentController SellerAgent2 = myContainer.createNewAgent("seller2", SellerAgent.class.getCanonicalName(), 
					null);
			SellerAgent2.start();
			
				
		}
		catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}
	}
}
