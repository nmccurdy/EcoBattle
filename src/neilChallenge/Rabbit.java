package neilChallenge;

import java.io.PrintStream;
import java.util.AbstractList;

import starjava.Agent;
import starjava.SmellHandler;
import application.Animal;
import application.Carnivore;
import application.DemoApp;
import application.EcoObject;
import application.Grass;
import application.Herbivore;

public class Rabbit extends Herbivore {

	private DemoApp app;

	private Controller controller;

	// remember if the rabbit is colliding with grass
	private boolean collidingWithGrass = false;

	// remember if the rabbit is eating
	private boolean isEating = false;

	// remember how long the rabbit has been running away
	private int runAwayTime = 0;
	private static final int MAX_RUN_AWAY_TIME = 5;

	// age is used for reproduction
	private int age;
	private static final int REPRO_CYCLE = 50;
	private static final double REPRO_CHANCE_PER_REPRO_CYCLE = .25;

	private static final int MAX_CLUMPING_SIZE = 17;

	private static final int LOW_DENSITY_REPRODUCE = 17;

	private boolean runningAwayFromBorder = false;

	// used for debugging
	private int lastBabyWho = -1;

	/**
	 * This is the constructor for rabbits. It is the setup code for rabbits.
	 * 
	 * @param app
	 * @param controller
	 */
	public Rabbit(DemoApp app, Controller controller) {
		super(app, controller, "Rabbit", "animals/rabbit-default");

		this.app = app;
		this.controller = controller;

		addCollisionHandler(RabbitGrassCollision.collision);
		addCollisionHandler(RabbitHerbivoreCollision.collision);

		double width = controller.getRightBoundary()
				- controller.getLeftBoundary();
		double height = controller.getTopBoundary()
				- controller.getBottomBoundary();

		setXY(controller.getLeftBoundary() + Math.random() * width, controller
				.getBottomBoundary()
				+ Math.random() * height);

		setSize(.5);

		// we have to remember to explicity set the age for the babies to
		// 0 otherwise we will be creating reproductive-age babies
		age = (int) (Math.random() * REPRO_CYCLE);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see application.Herbivore#execute()
	 */
	@Override
	public void doAnimalActions() {

		reproduce();
		move();

		// call the super class's execute function so that the rabbits
		// can do what herbivores and animals do
		super.doAnimalActions();
	}

	/**
	 * Only reproduce if it's the right time in the repro cycle
	 */
	private void reproduce() {
		if (getEnergy() > .7 * MAX_ENERGY) {
			AbstractList<Agent> otherRabbits = smell(10, new SmellHandler() {
				@Override
				public boolean smellCondition(Agent smellee) {
					return smellee instanceof Rabbit;
				}
			});

			if (otherRabbits.size() < LOW_DENSITY_REPRODUCE) {
				AbstractList<Agent> grasses = smell(10, new SmellHandler() {
					@Override
					public boolean smellCondition(Agent smellee) {
						return smellee instanceof Grass
								&& ((Grass) smellee).getEnergy() > .7 * Grass.MAX_ENERGY;
					}
				});

				if (grasses.size() > 0) {
					createNewBaby();
				}

			}
		}

	}

	/**
	 * Create a new baby rabbit and give it some of the parent's energy
	 */
	private void createNewBaby() {
		Rabbit baby = new Rabbit(app, controller);
		app.addCollidableAgent(baby);
		app.addExecutable(baby);

		// energy has to be given to the baby or else it will die
		giveEnergyToBaby(baby, .25);

		baby.age = (int) (Math.random() * 10);
		baby.copyPositionAndHeading(this);

		lastBabyWho = baby.getWho();
	}

	private boolean spreadOut() {
		AbstractList<Agent> otherRabbits = smell(5, new SmellHandler() {
			@Override
			public boolean smellCondition(Agent smellee) {
				return smellee instanceof Rabbit;
			}
		});

		if (otherRabbits.size() > MAX_CLUMPING_SIZE) {
			// find center of mass

			// run away from the center of mass of the enemies

			// Find the average location of the coyotes that were smelled so
			// that
			// we can make the rabbit run away from that location.

			double sumX = 0;
			double sumY = 0;

			// Find the sum of the X and Y locations of all the smelled rabbits.
			for (Agent rabbit : otherRabbits) {
				sumX = sumX + rabbit.getX();
				sumY = sumY + rabbit.getY();
			}

			// Find the average by dividing the sums by the number of rabbits
			double avgX = sumX / otherRabbits.size();
			double avgY = sumY / otherRabbits.size();

			// Find the heading (direction) that our rabbit would have to travel
			// in
			// to get to the coyotes.
			double heading = this.getHeadingTowards(avgX, avgY);

			// Make our rabbit head in the exact opposite direction.
			setHeading(heading - 180);
			right(Math.random() * 20);
			left(Math.random() * 20);

			forward(1);

			return true;
		}

		return false;
	}

	/**
	 * 
	 */

	private void move() {
		if (runningAwayFromBorder) {
			keepRunning();
		} else {
			if (!keepRunning()) {
				if (!runningAwayFromEnemy()) {
					if (shouldLookForFood()) {
						if (!collidingWithGrass) {
							if (!moveTowardsLongGrass()) {
								if (isReallyHungry()) {
									if (!moveTowardsAnyGrass()) {
										if (!spreadOut()) {
											moveRandomly();
										}
									}
								} else {
									if (!spreadOut()) {
										moveRandomly();
									}
								}
							}
						}
					} else {
						if (!fightEnemy()) {
							spreadOut();
						}
					}
				}
			}
		}

		collidingWithGrass = false;
		isEating = false;

		// if the Rabbit reaches the edge of the world, have it turn around.
		// the || in the line below is the Java way of saying OR.
		if (!runningAwayFromBorder) {
			if (getX() <= controller.getLeftBoundary() + 3
					|| getX() >= controller.getRightBoundary() - 3
					|| getY() <= controller.getBottomBoundary() + 3
					|| getY() >= controller.getTopBoundary() - 3) {
				// send back to center of screen
				setHeading(getHeadingTowards(controller.getLeftBoundary()
						+ (controller.getRightBoundary() - controller
								.getLeftBoundary()) / 2, 0));
				right(Math.random() * 20);
				left(Math.random() * 20);
				forward(1);
				runAwayTime = MAX_RUN_AWAY_TIME;
				runningAwayFromBorder = true;
			}
		}
	}

	private boolean fightEnemy() {
		AbstractList<Agent> weakRabbits = smell(5, new SmellHandler() {
			@Override
			public boolean smellCondition(Agent smellee) {
				return smellee instanceof Herbivore
						&& !(smellee instanceof Rabbit)
						&& ((Animal) smellee).getEnergy() < .75 * getEnergy();
			}
		});

		if (weakRabbits.size() > 0) {
			// sort by closest
			app.sortByClosestTo(getX(), getY(), weakRabbits);
			Agent closest = weakRabbits.get(0);
			setHeading(getHeadingTowards(closest.getX(), closest.getY()));
			forward(1);
			return true;
		} else {
			return false;
		}
	}

	private void moveRandomly() {
		left(Math.random() * 60);
		right(Math.random() * 60);
		forward(1);
	}

	private boolean moveTowardsAnyGrass() {
		// look for any grass
		AbstractList<Agent> anyGrasses = smell(10, new SmellHandler() {
			@Override
			public boolean smellCondition(Agent smellee) {
				return smellee instanceof Grass
						&& smellee.getX() >= controller.getLeftBoundary()
						&& smellee.getX() <= controller.getRightBoundary();
			}
		});

		if (anyGrasses.size() > 0) {
			app.sortByClosestTo(getX(), getY(), anyGrasses);

			Agent first = anyGrasses.get(0);
			setHeading(getHeadingTowards(first.getX(), first.getY()));
			right(Math.random() * 20);
			left(Math.random() * 20);

			forward(1);

			return true;
		} else {
			return false;
		}
	}

	private boolean moveTowardsLongGrass() {
		AbstractList<Agent> grasses = smell(10, new SmellHandler() {
			@Override
			public boolean smellCondition(Agent smellee) {
				return smellee instanceof Grass
						&& smellee.getX() >= controller.getLeftBoundary()
						&& smellee.getX() <= controller.getRightBoundary()
						&& ((EcoObject) (smellee)).getEnergy() > .30 * Grass.MAX_ENERGY;
			}
		});

		if (grasses.size() > 0) {
			// sort the grass
			app.sortByClosestTo(getX(), getY(), grasses);

			Agent closest = grasses.get(0);
			setHeading(getHeadingTowards(closest.getX(), closest.getY()));
			right(Math.random() * 20);
			left(Math.random() * 20);
			forward(1);

			return true;
		} else {
			return false;
		}
	}

	private boolean keepRunning() {
		runAwayTime--;
		if (runAwayTime > 0) {
			// if you were running away from a carnivore but can
			// no longer smell it, keep walking away for awhile.
			forward(1);

			return true;
		} else {
			runningAwayFromBorder = false;
			return false;
		}
	}

	private boolean runningAwayFromEnemy() {
		AbstractList<Agent> enemies = smell(5, new SmellHandler() {
			@Override
			public boolean smellCondition(Agent smellee) {
				return smellee instanceof Carnivore
						&& !(smellee instanceof Coyote)
						&& smellee.getX() >= controller.getLeftBoundary()
						&& smellee.getX() <= controller.getRightBoundary();
			}
		});

		if (enemies.size() > 0) {
			// run away from the center of mass of the enemies

			// Find the average location of the coyotes that were smelled so
			// that
			// we can make the rabbit run away from that location.

			double sumX = 0;
			double sumY = 0;

			// Find the sum of the X and Y locations of all the smelled
			// rabbits.
			for (Agent enemy : enemies) {
				sumX = sumX + enemy.getX();
				sumY = sumY + enemy.getY();
			}

			// Find the average by dividing the sums by the number of
			// rabbits
			double avgX = sumX / enemies.size();
			double avgY = sumY / enemies.size();

			// Find the heading (direction) that our rabbit would have to
			// travel
			// in
			// to get to the coyotes.
			double heading = this.getHeadingTowards(avgX, avgY);

			// Make our rabbit head in the exact opposite direction.
			setHeading(heading - 180);
			right(Math.random() * 20);
			left(Math.random() * 20);

			// how fast we run away depends on how much energy we have
			double energyPercent = getEnergy() / MAX_ENERGY;
			if (energyPercent > .70) {
				forward(3);
			} else if (energyPercent > .5) {
				forward(2);
			} else {
				forward(1);
			}

			runAwayTime = MAX_RUN_AWAY_TIME;
			runningAwayFromBorder = false;

			return true;
		}
		return false;
	}

	public boolean isHungry() {
		if (isEating) {
			return energy < 98;
		} else {
			return energy < 80;
		}
	}

	public boolean shouldLookForFood() {
		return energy < 85;

	}

	public void setCollidingWithGrass(boolean collidingWithGrass) {
		this.collidingWithGrass = collidingWithGrass;
	}

	public boolean isReallyHungry() {
		if (isEating) {
			return energy < 50;
		} else {
			return energy < 30;
		}
	}

	public boolean isEating() {
		return isEating;
	}

	public void setEating(boolean isEating) {
		this.isEating = isEating;
	}

	@Override
	public void outputStatusInfo(PrintStream os) {
		super.outputStatusInfo(os);

		os.format("%nRabbit:%n");
		os.format("collidingWithGrass: %1b%n", collidingWithGrass);
		os.format("isEating: %1b%n", isEating);
		os.format("age: %1d%n", age);
		os.format("last baby id: %1d%n", lastBabyWho);
		os.format("runAwayTime: %1d%n", runAwayTime);
		os.format("runningAwayFromBorder: %1b%n", runningAwayFromBorder);
	}
}
