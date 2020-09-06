package PhoneDealerOntologyElements;

import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;
import jade.content.Concept;



public class Phone implements Concept{
	private String name;
	private int price;
	private Component ScreenSize = new Component();
	private Component storage = new Component();
	private Component RAM = new Component();
	private Component battery = new Component();
	
	@Slot(mandatory = true)
	public String getName() {
		return name;
	}
	
	@Slot(mandatory = true)
	public Component getScreenSize(){
		return ScreenSize;
	}
	
	@Slot(mandatory = true)
	public Component getBattery() {
		return battery;
	}
	
	@Slot(mandatory = true)
	public Component getStorage() {
		return storage;
	}
	
	@Slot(mandatory = true)
	public Component getRAM() {
		return RAM;
	}
	@Slot(mandatory = true)
	public int getPrice() {
		return price;
	}
	
	public void setPrice(int price)
	{
		this.price = price;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setScreenSize(Component screenSize) {
		this.ScreenSize = screenSize;
	}

	public void setStorage(Component storage) {
		this.storage = storage;
	}

	public void setRAM(Component RAM) {
		this.RAM = RAM;
	}

	public void setBattery(Component battery) {
		this.battery = battery;

	}
}
