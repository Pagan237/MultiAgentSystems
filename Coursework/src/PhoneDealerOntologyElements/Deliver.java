package PhoneDealerOntologyElements;

import jade.content.AgentAction;

public class Deliver implements AgentAction{
	private Order order;
	
	public Order getOrder(){
		return order;
	}
	
	public void setOrder(Order order) {
		this.order = order;
	}
}
