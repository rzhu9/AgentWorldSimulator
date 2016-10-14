import java. util.Random;
//each state can be a neuron
//create a neural network that connects different states
//output of ANN is the decision of which direction for the agent to go
//every time when time is up, update the ANN using the RL
//when run the game again, apply the updated ANN to calculate the output
public final class ruihaoPlayer extends Player{
	final double EAST = 0;
	final double SOUTHEAST = 0.25 * Math.PI;
	final double SOUTH = 0.5 * Math.PI;
	final double SOUTHWEST = 0.75 * Math.PI;
	final double WEST = Math.PI;
	final double NORTHWEST = 1.25 * Math.PI;
	final double NORTH = 1.5 * Math.PI;
	final double NORTHEAST = 1.75 * Math.PI;
	perceptron input[] = new perceptron[144];
	perceptron hidden[] = new perceptron[100];
	perceptron output[] = new perceptron[8];
	private ruihaoANN ruihao;
	private ruihaoANN ruihao2;
	private Sensors sensors, prevSensors;
	private double  reward;
	private boolean debugging = false;
	double ANNinput[] = new double[144];
	public ruihaoPlayer(AgentWindow agentWindow){
		this(agentWindow, false);
	}
	
	public ruihaoPlayer(AgentWindow agentWindow, boolean showSensors){
		super(agentWindow);
		setShowSensors(showSensors); // Display this player's sensors?
		prevSensors = new Sensors();
		ruihao = new ruihaoANN(input, hidden, output);
		ruihao.initialization();
	}
	
	public void run(){
		//ruihao.initialization();
		double expected = 0;
		double predicted = 0;
		double reward = 0;
		while(threadAlive()){
			//use the ANN to find the direction to go
			predicted = ApplyANN();
			//System.out.println(predicted);
			reward = getReward();
			setMoveVector(Utils.convertToPositiveRadians(predicted));
			ruihao2 = ruihao;
			double dir2 = ApplyANN2();
			//Reinforcement Learning
			expected = dir2 * 0.95 + reward; 
			ruihao.trainingANN(predicted, expected);
		}
	}
	
	//36 *4 bits to represent the current state
	//every 4 bits stands for animal, mineral, vegetable, wall
	public void getCurrentState(){
		int index = 0;
		sensors = getSensors();
		for (int i=0; i<Sensors.NUMBER_OF_SENSORS; i++){
			if (sensors.getObjectType(i) == Sensors.NOTHING){
				ANNinput[index] = 1;
				ANNinput[index+1] = 1;
				ANNinput[index+2] = 1;
				ANNinput[index+3] = 1;
				index = index+4;
				//System.out.println(index);
			}
			if (sensors.getObjectType(i) == Sensors.ANIMAL){
				ANNinput[index] = sensors.getDistance(i);
				ANNinput[index+1] = 1;
				ANNinput[index+2] = 1;
				ANNinput[index+3] = 1;
				index = index+4;
			}
			if (sensors.getObjectType(i) == Sensors.MINERAL){
				ANNinput[index] = 1;
				ANNinput[index+1] = sensors.getDistance(i);
				ANNinput[index+2] = 1;
				ANNinput[index+3] = 1;
				index = index+4;
			}
			if (sensors.getObjectType(i) == Sensors.VEGETABLE){
				ANNinput[index] = 1;
				ANNinput[index+1] = 1;
				ANNinput[index+2] = sensors.getDistance(i);
				ANNinput[index+3] = 1;
				index = index+4;
			}
			if (sensors.getObjectType(i) == Sensors.WALL){
				ANNinput[index] = 1;
				ANNinput[index+1] = 1;
				ANNinput[index+2] = 1;
				ANNinput[index+3] = sensors.getDistance(i);
				index = index+4;
			}
		}
	}
	public double ApplyANN(){
		getCurrentState();
		/*for (int i =0; i<144; i++){
			System.out.println(i +": " + ANNinput[i]);
		}
		System.out.println("*********");*/
		for (int i=0; i<144; i++){
			ruihao.setInputValue(i, ANNinput[i]);
		}
		for (int i=0; i<100; i++){
			ruihao.calculateHidden(i);
		}
		for (int i=0; i<8; i++){
			ruihao.calculateOutput(i);
		}
		double max = ruihao.getOutputValue(0);
		int maxIndex = 0;
		for (int i=0; i<8; i++){
			if (ruihao.getOutputValue(i)> max){
				max = ruihao.getOutputValue(i);
				maxIndex = i;
			}
		}
		double dir = maxIndex * (0.25 *Math.PI);
		return dir;
	}
	
	public double ApplyANN2(){
		getCurrentState();
		for (int i=0; i<144; i++){
			ruihao2.setInputValue(i, ANNinput[i]);
		}
		for (int i=0; i<100; i++){
			ruihao2.calculateHidden(i);
		}
		for (int i=0; i<8; i++){
			ruihao2.calculateOutput(i);
		}
		double max = ruihao.getOutputValue(0);
		int maxIndex = 0;
		for (int i=0; i<8; i++){
			if (ruihao2.getOutputValue(i)> max){
				max = ruihao2.getOutputValue(i);
				maxIndex = i;
			}
		}
		double dir = maxIndex * (0.25 *Math.PI);
		return dir;
	}
}
