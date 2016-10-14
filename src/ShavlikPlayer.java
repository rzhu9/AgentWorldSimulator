import java.awt.*;

//
//
// ShavlikPlayer - walk towards Vegetables; if none, move 180 degrees away from closest Player.
//
//

public final class ShavlikPlayer extends Player
{ private static final boolean debuggingThisClass = false;

  public static Color ShavlikPlayerColor = Color.yellow;

  private boolean debugging;
  private Sensors sensors;
  private int     standStillsInaRow = 0, // Watch for being stuck, indicating drastic measures are needed.
                  safeK       = (int)Math.round(40.0 / Sensors.DEGREES_BETWEEN_SENSORS), // Look for alleys about 90 degrees wide.
                  safeKveggie = (int)Math.round(20.0 / Sensors.DEGREES_BETWEEN_SENSORS),
                  stepSize    = Utils.getPlayerStepSize();
  private double  runAwayThreshold       = 4.0 * stepSize, // Only run from other players closer than this.
                  beingChasedThreshold   = 2.5 * stepSize, // Try not to let another player get this close.
                  dontRunTowardThreshold = 3.0 * stepSize, // Won't hit a NON-MOVING object if 3 steps away.
                  freeSpaceThreshold     = 3.0 * stepSize,
                  runToVeggieThreshold   = 6.0 * stepSize, // Make sure there is a lot of space when chasing vegetables.
		  runToDaylightThreshold = Utils.getSensorRange() - 1.0, // Should be fully clear.
                  nearMineralThreshold   = 3.0 * stepSize, // (Recall that distances are center-to-center.)
                  moveEastMoves          = 1.0;            // Count (indirectly) moves to the left.

  ShavlikPlayer(AgentWindow agentWindow)
  {
    this(agentWindow, false);
  }
  ShavlikPlayer(AgentWindow agentWindow, boolean showSensors)
  {
    super(agentWindow);
    setShowSensors(showSensors);

    // Make sure all the thresholds are in range.
    if (runAwayThreshold       > Utils.getSensorRange()) runAwayThreshold       = 0.9 * runAwayThreshold;
    if (freeSpaceThreshold     > Utils.getSensorRange()) freeSpaceThreshold     = 0.9 * freeSpaceThreshold;
    if (runToVeggieThreshold   > Utils.getSensorRange()) runToVeggieThreshold   = 0.9 * runToVeggieThreshold;
    if (dontRunTowardThreshold > Utils.getSensorRange()) dontRunTowardThreshold = 0.9 * dontRunTowardThreshold;

    debugging = (debuggingThisClass && AgentWindow.masterDebugging);
  }

  void setPriority()
  { // Built-in players may have lower priority than the default. 
    playerThread.setPriority(builtinPriority);
  }

  private double chooseRandomMove()
  { int counter = 0, maxCount = 5, dir;
  
    // Look for some free space, across SEVERAL sensors.  Stand still if can't find any.
    do
    {
      dir = Utils.getRandomDirection();
    }
    while (!safeDirection(dir) && ++counter < maxCount); // Only sample a few times before giving up.

    if (counter >= maxCount) return -1.0;
    else
    {
      double result = Utils.convertSensorIDtoRadians(dir);
      
      if (debugging) Utils.println("A random safe direction is "
                                   + Utils.convertSensorIDtoDegrees(dir));
      // Don't always stay on the sampled directions.
      return result + Utils.getRandomIntInRange(-Sensors.DEGREES_BETWEEN_SENSORS, Sensors.DEGREES_BETWEEN_SENSORS) * 0.1;
    }
  }

  // See if a vegetable or free space lies in this direction.
  private boolean safeDirection(int dir)
  {
    return safeDirection(dir, safeK, freeSpaceThreshold);
  }
  private boolean safeDirection(int dir, int K, double distanceThreshold)
  { 
    return (safeSensorDir(dir, distanceThreshold) && 
            allDirectionsSafeToTheLeft(dir,  K, distanceThreshold) && 
            allDirectionsSafeToTheRight(dir, K, distanceThreshold));
  }
  private boolean safeSensorDir(int dir, double distanceThreshold)
  { 
    return (sensors.getDistance(dir)   >  distanceThreshold ||
            sensors.getObjectType(dir) == Sensors.VEGETABLE);
  }
  private boolean allDirectionsSafeToTheLeft(int dir, int K, double thresh)
  { 
    for(int i = 1; i <= K; i++) if (!safeSensorDir(Utils.leftK_sensor(dir,  i), thresh)) return false;

    return true;
  }
  private boolean allDirectionsSafeToTheRight(int dir, int K, double thresh)
  { 
    for(int i = 1; i <= K; i++) if (!safeSensorDir(Utils.rightK_sensor(dir, i), thresh)) return false;

    return true;
  }

  public void run()
  { double  reward;
    boolean lastStepBad         = false; // Was the last step a bad one? If so, do a random walk.
    int     lastDirMoved        = -1,
            lastVegetableChased = -1,
            lastVegetableChosen = -1,
            currentTime; // Can use this to notice that a game is over.
    try
    {
      while(threadAlive())
      { int    dirOfClosestVegetable = -1,
               dirOfClosestPlayer    = -1, 
               dirOfClosestMineral   = -1,
               dirOfClosestWall      = -1,
               dirOfClosestNothing   = -1,
               wallsSensed           =  0;
               
        double distanceOfClosestVegetable = Double.MAX_VALUE,
               distanceOfClosestPlayer    = runAwayThreshold,
               distanceOfClosestMineral   = nearMineralThreshold,
	       distanceOfClosestWall      = Double.MAX_VALUE,
	       distanceOfClosestNothing   = Double.MAX_VALUE,
               fractionWalls, distance, radians;

	// Allow this to be turned on at times
        debugging = debuggingThisClass && playingField.singleStepping && AgentWindow.masterDebugging;

        currentTime = getCurrentTime();
        sensors     = getSensors();

        if (!lastStepBad) for(int dir = 0; dir < Sensors.NUMBER_OF_SENSORS; dir++)
        { 
          distance = sensors.getDistance(dir);
          switch (sensors.getObjectType(dir)) {
          case Sensors.VEGETABLE:
            // If chasing a vegetable, don't get distracted!  (Can occillate due to occlusions coming and going.)
            if (lastVegetableChased >= 0 && dir != lastVegetableChased) continue;
	    // If abandoned the chasing of a vegetable, don't resume it (along the same direction).
	    if (lastVegetableChased < 0  && lastVegetableChosen >= 0 && dir == lastVegetableChosen) continue;
            if (distance < distanceOfClosestVegetable &&
		safeDirection(dir, safeKveggie, Math.min(distance, runToVeggieThreshold)))
            {
              distanceOfClosestVegetable = distance;
              dirOfClosestVegetable      = dir;
            }
            break;
          case Sensors.ANIMAL:
            if (distance < distanceOfClosestPlayer)
            {
              distanceOfClosestPlayer = distance;
              dirOfClosestPlayer      = dir;
            }
            break;
          case Sensors.MINERAL:
            if (distance < distanceOfClosestMineral)
            {
              distanceOfClosestMineral = distance;
              dirOfClosestMineral      = dir;
            }
            break;
          case Sensors.WALL:
            wallsSensed++;
            if (distance < distanceOfClosestWall)
            {
              distanceOfClosestWall = distance;
              dirOfClosestWall      = dir;
            }
            break;
          case Sensors.NOTHING:
            if (distance < distanceOfClosestNothing &&
		safeDirection(dir, safeK, runToDaylightThreshold))
            {
              distanceOfClosestNothing = distance;
              dirOfClosestNothing      = dir;
            }
            break;

          }
        }

        // See if 'back against the wall.'
        fractionWalls = wallsSensed / (double)sensors.NUMBER_OF_SENSORS;
        
        lastVegetableChased = -1; // Need to unset in case this chase is abandoned.
        if (dirOfClosestVegetable >= 0 && distanceOfClosestPlayer > beingChasedThreshold)
        { // Move toward closest vegetable, if one in sight.  (Already confirmed it is safe.)

          if (debugging) Utils.println("Closest (safe) vegetable is at "
                                       + Utils.convertSensorIDtoDegrees(dirOfClosestVegetable));
          radians             = Utils.convertSensorIDtoRadians(dirOfClosestVegetable);
          lastDirMoved        = dirOfClosestVegetable;
          lastVegetableChased = dirOfClosestVegetable;
	  lastVegetableChosen = dirOfClosestVegetable;
        }
        else if (dirOfClosestPlayer >= 0)
        { int    dirToUse = -1, // Otherwise move away from the closest player.
	         K        = safeK;
          double thresh   = dontRunTowardThreshold;
          
          if (safeDirection(Utils.oppositeDirection(dirOfClosestPlayer), K, thresh))
          {
            dirToUse = Utils.oppositeDirection(dirOfClosestPlayer);
          }
          else if (safeDirection(Utils.leftTurn(dirOfClosestPlayer),     K, thresh))
          {
            dirToUse = Utils.leftTurn(dirOfClosestPlayer);
          }
          else if (safeDirection(Utils.rightTurn(dirOfClosestPlayer),    K, thresh))
          {
            dirToUse = Utils.rightTurn(dirOfClosestPlayer);
          }

          if (dirToUse >= 0)
          {
            lastDirMoved = dirToUse;
            if (debugging) Utils.println("Safest direction for running from other players is "
                                         + Utils.convertSensorIDtoDegrees(dirToUse));
            radians = Utils.convertSensorIDtoRadians(dirToUse);
          }
          else radians = -1.0; // Stand still if something is too close in that direction.
        }
        else if ((!lastStepBad || lastDirMoved !=0) &&
                 currentTime < 1000 &&
                 // Math.random() < moveEastMoves      &&
                 safeDirection(0, safeKveggie, freeSpaceThreshold))
        { // When it is safe, move to the right (to get near food), early on at least,
          // if there is a lot of free space that way.

          if (debugging) Utils.println("Moving to the right, since that is safe");
          lastDirMoved = 0;
          radians      = 0.0;
          if (moveEastMoves > 0.004) moveEastMoves = 0.95 * moveEastMoves;	  
        }
        else if (dirOfClosestMineral >= 0            &&
                 lastDirMoved != dirOfClosestMineral &&
                 (standStillsInaRow >= 5 || Math.random() < 0.25))
        { // Occasionally push if adjacent to a mineral.  Don't repeatedly do so, though.

          if (debugging) Utils.println("Trying to push the mineral at " + dirOfClosestMineral);
          lastDirMoved = dirOfClosestMineral;
          radians      = Utils.convertSensorIDtoRadians(dirOfClosestMineral);
        }
        else if (dirOfClosestNothing >= 0 && Math.random() < 0.25)
	{ // If a clear path, basically follow it amidst random walking.

          if (debugging) Utils.println("Running to daylight: " + dirOfClosestNothing);
          lastDirMoved = dirOfClosestNothing;
          radians      = Utils.convertSensorIDtoRadians(dirOfClosestNothing);
	}
        else if (fractionWalls >= 0.33 && Math.random() < 0.10) // Ina corner?
        { // Run away from closest wall, even if dangerous.

          radians = Utils.convertSensorIDtoRadians(Utils.oppositeDirection(dirOfClosestWall));
        }
        else if (standStillsInaRow >= 10)
        { // Try anything when really stuck.  (Should really NEVER move into a wall, though.)
          radians = 2 * Math.PI * Math.random();
        }
        else radians = chooseRandomMove(); // Otherwise do a random walk into free space (if none, stand still).
        
        if (radians < 0)
        { // Don't reset lastDirMove - that variable indicates last direction ACTUALLY moved.

          standStillsInaRow++;
          if (debugging) Utils.println("No move looks good, so standing still (" + standStillsInaRow + ")");
          setMoveVector(-1.0); // Indicate 'stand still.'
        }
        else
        {
          standStillsInaRow = 0;
          setMoveVector(Utils.convertToPositiveRadians(radians));
        }

        reward = getReward();
        if (reward < 0) lastStepBad = true; else lastStepBad = false;
      }
    }
    catch(Exception e)
    {
      Utils.errorMsg(e + ", " + toString() + " has stopped running");
    }

    Utils.errorMsg(getName() + " is no longer alive for some reason");
    Utils.exit(-1);
  }

}