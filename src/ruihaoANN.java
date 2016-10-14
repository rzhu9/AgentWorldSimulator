import java. util.Random;
public class ruihaoANN {
	private perceptron input[] = new perceptron[144];
	private perceptron hidden[] = new perceptron[100];
	private perceptron output[] = new perceptron[8];
	
	public ruihaoANN(perceptron input[], perceptron hidden[], perceptron output[]){
		this.input = input;
		this.hidden = hidden;
		this.output = output;
	}
	public void initialization(){
		//initialize output perceptrons
		for (int i= 0; i < 8; i++){
			Random RNG = new Random();
			double number = -1 + 2 * RNG.nextDouble();
			double number2 = (double)number/10;
			//System.out.println(number2);
			this.output[i] = new perceptron(0, null, number2, null);
			
		}
		//initializae hidden perceptrons
		for (int i= 0; i < 100; i++){
			double[] allWeight = new double[8];
			Random RNG = new Random();
			for (int j=0; j<8; j++){			
				double number = -1 + 2 * RNG.nextDouble();
				double number2 = (double)number/10;
				//System.out.println( i +": " + j +": "+number2);
				allWeight[j] = number2;
			}
			double number = -1 + 2 * RNG.nextDouble();
			double number2 = (double)number/10;
			this.hidden[i] = new perceptron(0, allWeight, number2, output);
			//System.out.println(number2);
		}
		//initializae input perceptrons
		for (int i=0; i<144; i++){
			double[] allWeight = new double[100];
			Random RNG = new Random();
			for (int j=0; j < 100; j++){
				double number = -1 + 2 * RNG.nextDouble();
				double number2 = (double)number/10;
				System.out.println( i +": " + j +": "+number2);
				allWeight[j] = number2;
			}
			this.input[i] = new perceptron(0, allWeight, 0, hidden);
		}
	}
	public void setInputValue(int index, double newValue){
		this.input[index].setValue(newValue);
	}
	public void setInputWeight(int inputIndex, int weightIndex, double newWeight){
		this.input[inputIndex].setWeight(weightIndex, newWeight);
	}
	public void setHiddenValue(int index, double newValue){
		this.hidden[index].setValue(newValue);
	}
	public void setHiddenWeight(int hiddenIndex, int weightIndex, double newWeight){
		this.hidden[hiddenIndex].setWeight(weightIndex, newWeight);
	}
	public void setOutputValue(int outputIndex, double newValue){
		this.output[outputIndex].setValue(newValue);
	}
	public double logisticThreshold(double input){
		double output = 1/(1+ Math.pow(Math.E, (input*(-1))));
		return output;
	}
	public void calculateHidden(int index){
		double result = 0;
		for (int i= 0; i<100; i++){
			result = result + (this.input[i].getValue() * this.input[i].getWeight(index));
		}
		result = result + this.hidden[index].getWeight2();
		result = logisticThreshold(result);
		setHiddenValue(index, result);
	}
	public void calculateOutput(int index){
		double result = 0;
		for (int i= 0; i<8; i++){
			result = result + (this.hidden[i].getValue() * this.hidden[i].getWeight(index));
		}
		result = result + this.output[index].getWeight2();
		result = logisticThreshold(result);
		setOutputValue(index, result);
	}
	public double getOutputValue(int index){
		return this.output[index].getValue();
	}
	public void trainingANN(double predicted, double expected){
		//training input perceptron weight
		for (int i=0; i<144; i++){
			for (int j = 0; j<100; j++){
				double currWeight = this.input[i].getWeight(j);
				currWeight = currWeight + this.input[i].getValue() * (expected = predicted) * 0.01; 
				this.input[i].setWeight(j, currWeight);
			}
		}
		//training hidden perceptron weight
		for (int i=0; i<00; i++){
			for (int j = 0; j<8; j++){
				double currWeight = this.hidden[i].getWeight(j);
				currWeight = currWeight + this.hidden[i].getValue() * (expected = predicted) * 0.01; 
				this.hidden[i].setWeight(j, currWeight);
			}
		}
		//training weight2
		for (int i=0; i<100; i++){
			double currWeight = this.hidden[i].getWeight2();
			currWeight = currWeight + (expected-predicted) * 0.01;
		}
		for (int i=0; i<8; i++){
			double currWeight = this.output[i].getWeight2();
			currWeight = currWeight + (expected-predicted) * 0.01;
		}
	}
}
