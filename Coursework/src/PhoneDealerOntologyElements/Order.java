package PhoneDealerOntologyElements;

import jade.content.AgentAction;
import jade.core.AID;

public class Order implements AgentAction{
	private AID buyer;
	private Phone phone = new Phone();
	private int quantity;
	private int price;
	private int DaysUntilDue;
	private int LateFee;
	
	public AID getBuyer() {
		return buyer;
	}
	
	public void setBuyer(AID buyer) {
		this.buyer = buyer;
	}
	
	public Phone getPhone() {
		return phone;
	}
	
	public void setPhone(Phone phone) {
		this.phone = phone;
	}
	
	public int getQuantity() {
		return quantity;
	}
	
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	
	public int getPrice() {
		return price;
	}
	
	public void setPrice(int price) {
		this.price = price;
	}
	
	public int getDueDate() {
		return DaysUntilDue;
	}
	
	public void setDueDate(int due) {
		this.DaysUntilDue = due;
	}
	
	public int getLateFee() {
		return LateFee;
	}
	
	public void setLateFee(int fee) {
		this.LateFee = fee;
	}
}
