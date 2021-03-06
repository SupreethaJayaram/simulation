package schedulesim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 * 
 * @author paul moggridge (paulmogs398@gmail.com)
 */
public class Producer extends SimEntity {

  private final String name;
  private HashMap<Integer, MetataskPattern> metatasks;

  private int tasksSubmittedCount;

  public Producer(String name) {
    super();
    this.name = name;
    metatasks = new HashMap<>();
    tasksSubmittedCount = 0;
  }

  public String getName() {
    return name;
  }

  public void addMetatask(int step, MetataskPattern metataskPattern) {
    metatasks.put(step, metataskPattern);
  }

  public int getTasksSubmittedCount(){
    return tasksSubmittedCount;
  }

  @Override
  public boolean isFinished() {
    for (Entry<Integer, MetataskPattern> stepTask : metatasks.entrySet()) {
      if (stepTask.getKey() > ScheduleSim.getSimulationStep()) {
        // The there a wave of tasks to come in the future
        return false;
      }
    }
    // No more waves to come
    return true;
  }

  @Override
  public void addChild(ConsumingEntity entity){
    // Producer can only have one child
    if(super.getChildren().size() > 0){
      for(SimEntity child : super.getChildren()){
        child.setParent(null);
      }
      super.getChildren().clear();
    }
    super.addChild(entity);
  }

  @Override
  public void step() {
    super.step();
    // check if the current sim step has any task to deploy
    MetataskPattern taskPattern = metatasks.get(ScheduleSim.getSimulationStep());
    if (taskPattern != null) {
      // Place tasks on top scheduler
      ArrayList<Task> tasks = taskPattern.generateMetatask();
      for (Task task : tasks) {
        // Producer has only one child, this is where the task entry the system
        ((ConsumingEntity) super.getChildren().get(0)).submitTask(task);
        tasksSubmittedCount++;
      }
    }
  }
}
