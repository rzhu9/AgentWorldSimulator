public class perceptron {
	private double value;
	private double[] weight;
	private double weight2;
	private perceptron[] destiny;
	public perceptron(double value, double[] weight, double weight2, perceptron[] destiny){
		this.value = value;
		this.weight = weight;
		this.weight2 = weight2;
		this.destiny = destiny;
	}
	public void setAllWeight(double[] newWeights){
		this.weight = newWeights;
	}
	public void setWeight(int index, double newWeight){
		this.weight[index] = newWeight;
	}
	
	public void setValue(double newValue){
		this.value = newValue;
	}
	
	public void setWeight2(double newWeight2){
		this.weight2 = newWeight2;
	}
	public void setDestiny(perceptron[] output){
		this.destiny = output;
	}
	
	public double[] getAllWeight(){
		return this.weight;
	}
	
	public double getWeight(int index){
		return this.weight[index];
	}
	
	public double getWeight2(){
		return this.weight2;
	}
	
	public double getValue(){
		return this.value;
	}
	
	public perceptron[] getDestiny(){
		return this.destiny;
	}
}
