package PhoneDealerOntologyElements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Component implements Concept{
	private String type;
	private int value;
	private int price;
	
	public void setType(String type) {
		this.type = type;
	}
	
	@Slot(mandatory = true)
	public String getType(){
		return type;
	}
	
	@Slot(mandatory = true)
	public int getPrice() {
		return price;
	}
	
	public void setPrice(int price) {
		this.price = price;
	}
	@Slot(mandatory = true)
	public int getValue() {
		return value;
	}
	
	public void setValue(int value) {
		this.value = value;
	}
}
