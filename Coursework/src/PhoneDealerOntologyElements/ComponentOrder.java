package PhoneDealerOntologyElements;

import jade.content.AgentAction;
import jade.core.AID;

public class ComponentOrder implements AgentAction{
	private Component comp = new Component();
	private int price;
	private int Days;
	private AID seller;
	
	public Component getComp() {
		return comp;
	}
	public void setComp(Component comp) {
		this.comp = comp;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	public int getDays() {
		return Days;
	}
	public void setDays(int days) {
		this.Days = days;
	}
	public void DecreaseDay() {
		Days -= 1;
	}
	public AID getSeller() {
		return seller;
	}
	public void setSeller(AID seller) {
		this.seller = seller;
	}
}
