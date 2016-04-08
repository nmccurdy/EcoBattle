package application;

import java.awt.Color;
import java.io.PrintStream;
import java.util.LinkedList;

import starjava.Agent;
import starjava.AppMonitor;
import starjava.Application;
import torusworld.SLCameraMovement;

public class DemoApp extends Application {

	private int counter = 0;

	// these variables are used to keep score
	private double sumBlueEnergy = 0;
	private double sumRedEnergy = 0;

	private double numDataPoints = 0;

	private boolean battleStarted = false;
	private boolean battleFinished = false;

	private Controller controllerA;
	private Controller controllerB;

	private boolean doesFenceExist = true;

	// this variable tells us how long the fence will be up
	private static final int FENCE_TIME = 300 + (int) (Math.random() * 200);
//	private static final int FENCE_TIME = 10;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// NOTE: to run this, you need to point the working directory
		// to the starlogo directory

		// do this in run configurations in the arguments tab
		Application app = new DemoApp();
	}

	public DemoApp() {
		super();

		// this determines who will be the A team and who will be the B team

		controllerA = new basic.Controller(this, -50, 0, 50, -50, Color.blue);
		controllerB = new basic.Controller(this, 0, 50, 50, -50, Color.red);

//		controllerA = new nuthall.Controller(this, -50, 0, 50, -50, Color.blue);
//		controllerB = new stanton.Controller(this, 0, 50, 50, -50, Color.red);

//		controllerA = new lung.Controller(this, -50, 0, 50, -50, Color.blue);
//		controllerB = new stanton.Controller(this, 0, 50, 50, -50, Color.red);
		
//		controllerA = new horwitz.Controller(this, -50, 0, 50, -50, Color.blue);
//		controllerB = new lung.Controller(this, 0, 50, 50, -50, Color.red);

		init();
	}

	public void setup() {
		super.setup();

		/***** Focus on this *****/
		System.out.println("setting up");
		// This section of code is similar to the Setup block
		// in Star Logo

		// This is how you open the new app monitor that shows app-specific
		// (shared)
		// variables
		AppMonitor appMon = new AppMonitor(mainWindow, this, this.getLock());
		appMon.init();

		clearAll();

		// This sets up the grass
		setupGrass();

		// Build the fence (this is visual only)
		buildFence();

		// This sets up the A team
		controllerA.setup();

		// This sets up the B team
		controllerB.setup();

		// zoom out by pretending like we move the mouse wheel
		SLCameraMovement.tryScheduleWheel(400, 320, 4);
		/***** Focus on this (end) *****/
	}

	/*
	 * Grass is set up in a grid pattern
	 */
	private void setupGrass() {

		double xStart = -50 + Grass.GRASS_SPACING / 2.0;
		double yStart = -50 + Grass.GRASS_SPACING / 2.0;

		for (int i = 0; i < (100 / Grass.GRASS_SPACING); i++) {
			for (int j = 0; j < (100 / Grass.GRASS_SPACING); j++) {
				Grass grass = new Grass(this);
				addCollidableAgent(grass);
				addExecutable(grass);

//				addDrawableAgent(grass);  // no longer need this because of backwards compatibility hack.
				
				grass.setXY(xStart + j * Grass.GRASS_SPACING, yStart + i
						* Grass.GRASS_SPACING);
			}
		}
	}

	@Override
	public void execute() {

		/***** Focus on this *****/
//		System.out.println("executing");
		if (!battleFinished) {
			// This is where we put logic that we want to affect our shared
			// variables. There is no corresponding section in Star Logo, but
			// we were able to force Star Logo to do something similar by
			// creating
			// a Timer breed, creating a single Timer agent and then putting
			// this
			// code in the Timer section of the Run Forever block.
			counter = counter + 1;

			if (counter == FENCE_TIME) {
				battleStarted = true;
				removeFence();
			}
			/***** Focus on this (end) *****/

			controllerA.execute();
			controllerB.execute();
			super.execute();
		} else {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	/*
	 * This function outputs the score. It can be used to output any
	 * app-specific variables (shared variables)
	 * 
	 * @see starjava.Application#outputStatusInfo(java.io.PrintStream)
	 */
	@Override
	public void outputStatusInfo(PrintStream os) {
		super.outputStatusInfo(os);

		// go through agents and tally how many of each there are

		int numGrass = 0;
		int numRedHerbivores = 0;
		int numRedCarnivores = 0;
		int numBlueHerbivores = 0;
		int numBlueCarnivores = 0;

		double totalBlueEnergy = 0;
		double totalRedEnergy = 0;

		LinkedList<Agent> agentsClone = (LinkedList<Agent>)agents.clone();
		for (Agent agent : agentsClone) {
			if (agent instanceof EcoObject) {
				EcoObject eco = (EcoObject) agent;

				if (agent instanceof Grass) {
					numGrass++;
				} else if (agent instanceof Herbivore) {
					if (agent.isColor(Color.blue)) {
						numBlueHerbivores++;
						totalBlueEnergy += eco.getEnergy();
					} else {
						numRedHerbivores++;
						totalRedEnergy += eco.getEnergy();
					}
				} else if (agent instanceof Carnivore) {
					if (agent.isColor(Color.blue)) {
						numBlueCarnivores++;
						totalBlueEnergy += eco.getEnergy();
					} else {
						numRedCarnivores++;
						totalRedEnergy += eco.getEnergy();
					}
				}
			}
		}

		if (battleStarted && !battleFinished) {
			if (numBlueHerbivores == 0 || numBlueCarnivores == 0
					|| numRedHerbivores == 0 || numRedCarnivores == 0) {
				battleFinished = true;
			}

			sumBlueEnergy += totalBlueEnergy;
			sumRedEnergy += totalRedEnergy;

			numDataPoints++;
		}

		os.format("Blue score: %1.2f%n", sumBlueEnergy / numDataPoints);
		os.format("Red score: %1.2f%n%n", sumRedEnergy / numDataPoints);
		os.format("Total blue energy: %1.2f%n", totalBlueEnergy);
		os.format("Total red energy: %1.2f%n", totalRedEnergy);
		os.format("Num blue herbivores: %1d%n", numBlueHerbivores);
		os.format("Num blue carnivores: %1d%n", numBlueCarnivores);
		os.format("Num red herbivores: %1d%n", numRedHerbivores);
		os.format("Num red carnivores: %1d%n", numRedCarnivores);
		os.format("Num grass: %1d%n", numGrass);
		os.format("Time until battle: %1d%n", FENCE_TIME - getCounter());

	}

	/*
	 * This fence doesn't do anything. The real boundaries for an animal are
	 * determined by the boundaries given to the controller. Students may choose
	 * to use the color of the terrain to govern animal behavior, though.
	 */
	public void buildFence() {
		for (int y = -50; y < 51; y++) {
			
			this.setPatchColor(0, y, Color.black);
//			this.setPatchHeight(0, y, 2);
		}

		controllerA.setRightBoundary(0);
		controllerB.setLeftBoundary(0);

		doesFenceExist = true;
	}

	/**
	 * Remove the fence and set the controller boundaries as appropriate
	 */
	public void removeFence() {
		Color groundColor = getPatchColor(-50, -50);
		for (int y = -50; y < 51; y++) {
			this.setPatchColor(0, y, groundColor);
		}

		controllerA.setRightBoundary(50);
		controllerB.setLeftBoundary(-50);

		doesFenceExist = false;
	}

	public boolean doesFenceExist() {
		return doesFenceExist;
	}

	/**
	 * This code is in here to make sure that no one cheats by killing agents
	 * without going through the fight code.
	 */
	@Override
	public void killAgent(Agent agent) {
		java.lang.StackTraceElement[] trace = (new Throwable()).getStackTrace();
		if (trace.length >= 2) {
			String className = trace[1].getClassName();

			if (!className.startsWith("starjava")) {
				System.out.println("Cheater!  Trying to call die from: "
						+ trace[1].getClassName() + "."
						+ trace[1].getMethodName() + "()");
			} else {
				super.killAgent(agent);
			}
		} else {
			System.out.println("Cheater?");
		}
	}
}
