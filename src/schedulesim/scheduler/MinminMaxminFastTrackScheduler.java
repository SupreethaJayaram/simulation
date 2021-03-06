package schedulesim.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import schedulesim.Consumer;
import schedulesim.ConsumingEntity;
import schedulesim.ConsumingEntityMinFirstComparator;
import schedulesim.Task;
import schedulesim.TaskMaxFirstComparator;
import schedulesim.Log;
import schedulesim.Scheduler;
import schedulesim.TaskMinFirstComparator;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 * 
 * @author paul moggridge (paulmogs398@gmail.com)
 */
public class MinminMaxminFastTrackScheduler extends Scheduler {

  private float margin;

  public MinminMaxminFastTrackScheduler(){
    this(0.4f);
  }
  
  public MinminMaxminFastTrackScheduler(float margin){
    super();
    this.margin = margin;
  }

  @Override
  public void step() {
    super.step();
    if(super.getChildren().size() > 0 && super.getWaitingTasks().size() > 0){

      // This will store the delays on children below.
      HashMap<ConsumingEntity, Double> childDelay = new HashMap<>();

      // Find any exsisting delay on child from previous waves
      for (ConsumingEntity child : super.getChildren()) {

        // Create entry for child
        childDelay.put(child, child.getDelay());
      }

      // Sort the task max first, smallest tasks first. The task that come first here will
      // be palced in the normal track
      Collections.sort(super.getWaitingTasks(), new TaskMaxFirstComparator());

      // Decicde which tasks are small and eligible for fast tracking using margin
      // if fast track margin 0.3, this means the smallest 30% of tasks are fast
      // tracked the rest, 70% are processed on the normal track.
      ArrayList<Task> normalTrackTasks = new ArrayList<>();
      ArrayList<Task> fastTrackTasks = new ArrayList<>();
      int taskUnitsInNormalTrack = 0; // The amount of work in the normal track
      int taskUnitsInFastTrack = 0;  // The amount of work in the fast track

      // Number of tasks to go in Normal
      int countTasksToNormalTrack = Math.round(super.getWaitingTasks().size() * (1.0f-margin));

      // Using the sorted task list (biggest length first), put the biggest tasks into the normal track,
      while(super.getWaitingTasks().size() > 0){
        if(normalTrackTasks.size() <= countTasksToNormalTrack){
          taskUnitsInNormalTrack += super.getWaitingTasks().get(0).getRemaingUnits();
          normalTrackTasks.add(super.getWaitingTasks().remove(0));
        } else {
          taskUnitsInFastTrack += super.getWaitingTasks().get(0).getRemaingUnits();
          fastTrackTasks.add(super.getWaitingTasks().remove(0));
        }
      }

      if (super.getWaitingTasks().size() != 0) {
        Log.println("Error, all tasks should have been assigned a track in the above");
      }

//      Log.println("Fast Track Tasks: ");
//      for(Task task : fastTrackTasks){
//        Log.println(task.getTid() + "," + task.getRemaingUnits() + "u");
//      }
//      Log.println("Normal Track Tasks: ");
//      for(Task task : normalTrackTasks){
//        Log.println(task.getTid() + "," + task.getRemaingUnits() + "u");
//      }

      // Count total Unit Per Step
      int totalUPS = 0;
      for(ConsumingEntity child : super.getChildren()){
        totalUPS+= child.getUnitsPerStep();
      }

      // Based on the distribution of fast trackable task units to normal task units
      // calculate percentages of consumer units per step for the normal and fast tracks.
      double consumerUPSToTaskUPS = totalUPS / ((double) taskUnitsInNormalTrack + (double) taskUnitsInFastTrack);
      // Below is used later to decide how many UPS to put in the fast track
      double fastTrackTargetMips = consumerUPSToTaskUPS * taskUnitsInFastTrack;

      // Sort the child fastest (min. time) first (for building the FastTrack) i.e.
      // The ConsumingEntities with biggest UPS go first and thus into the fast track
      Collections.sort(super.getChildren(), new ConsumingEntityMinFirstComparator());

      // Assign VMs to fast or normal track
      ArrayList<ConsumingEntity> copyChildren = new ArrayList<>(super.getChildren());
      ArrayList<ConsumingEntity> fastTrack = createFastTrack(copyChildren, fastTrackTargetMips);
      ArrayList<ConsumingEntity> normalTrack = createNormalTrack(copyChildren);

      if (copyChildren.size() > 0) {
        Log.println(copyChildren.size() + " Consuming Entities where not assigned a track.");
      }

      // MaxMin tasks onto FastTrack
      //Collections.sort(fastTrackTasks , new TaskMaxFirstComparator());

      //Minmin tasks onto the Fast Track (rather than Maxmin)
      // Sort the task min first, smallest tasks first
      Collections.sort(fastTrackTasks, new TaskMinFirstComparator());
      
      while(fastTrackTasks.size()>0){
        Task task = fastTrackTasks.get(0);

        // Which ConsumingEntity can finish it first? i.e in the min. time,
        // takes into account the UnitPerStep of the ConsumingEntity and tasks
        // already scheduled to it.
        double minChildDelay = 0.0;
        ConsumingEntity minChild = null;

        // Which child can complete task first
        for(ConsumingEntity child : fastTrack){
          double taskMakespanWithChildDelay = ((double)task.getRemaingUnits() / (double)child.getUnitsPerStep()) + childDelay.get(child);
          // Can this child finish the task faster
          if(taskMakespanWithChildDelay < minChildDelay || minChild == null){
            minChild = child;
            minChildDelay = taskMakespanWithChildDelay;
          }
        }

        // Update child delays map
        childDelay.put(minChild, minChildDelay);

        // Submit task to child
        minChild.submitTask(fastTrackTasks.remove(0));
        // Update gif, if we are scheduling to a Consumer
        if(minChild instanceof Consumer){
            super.updateGif();
        }
      }

      // MaxMin tasks onto NormalTrack
      Collections.sort(normalTrackTasks , new TaskMaxFirstComparator());

      while(normalTrackTasks.size()>0){
        Task task = normalTrackTasks.get(0);

        // Which ConsumingEntity can finish it first? i.e in the min. time,
        // takes into account the UnitPerStep of the ConsumingEntity and tasks
        // already scheduled to it.
        double minChildDelay = 0.0;
        ConsumingEntity minChild = null;

        // Which child can complete task first
        for(ConsumingEntity child : normalTrack){
          double taskMakespanWithChildDelay = ((double)task.getRemaingUnits() / (double)child.getUnitsPerStep()) + childDelay.get(child);
          // Can this child finish the task faster
          if(taskMakespanWithChildDelay < minChildDelay || minChild == null){
            minChild = child;
            minChildDelay = taskMakespanWithChildDelay;
          }
        }

        // Update child delays map
        childDelay.put(minChild, minChildDelay);

        // Submit task to child
        minChild.submitTask(normalTrackTasks.remove(0));
        
        // Update gif, if we are scheduling to a Consumer
        if(minChild instanceof Consumer){
            super.updateGif();
        }
      }
    }
  }

  public ArrayList<ConsumingEntity> createFastTrack(ArrayList<ConsumingEntity> allConsumingEntities, double fastTrackTargetUPS){
    ArrayList<ConsumingEntity> fastTrack = new ArrayList<>();
    int fastTrackUPS = 0;

    // Loop over biggest ConsumingEntities first, if it fits in the fast track place in there
    while((fastTrackTargetUPS - fastTrackUPS) > allConsumingEntities.get(0).getUnitsPerStep()) {
      // Add VM to fast track
      fastTrackUPS += allConsumingEntities.get(0).getUnitsPerStep();
      fastTrack.add(allConsumingEntities.remove(0));
    }


    // Check we have at least one VM in fast track
    if (fastTrack.size() <= 0) {
      // If we not, add one smallest VM
      fastTrackUPS += allConsumingEntities.get(allConsumingEntities.size() - 1).getUnitsPerStep();
      fastTrack.add(allConsumingEntities.remove(allConsumingEntities.size() - 1));
    }

    return fastTrack;
  }

  public ArrayList<ConsumingEntity> createNormalTrack(ArrayList<ConsumingEntity> remainingConsumingEntities){

    // Create the NormalTrack and FastTrack as close to the target MIPS as possible
    ArrayList<ConsumingEntity> normalTrack = new ArrayList<>();

    // Add all remaining VMs
    while (remainingConsumingEntities.size() > 0) {
      normalTrack.add(remainingConsumingEntities.remove(0));
    }

    return normalTrack;
  }
}
