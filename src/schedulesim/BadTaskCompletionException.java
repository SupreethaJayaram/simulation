package schedulesim;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 * 
 * @author paul moggridge (paulmogs398@gmail.com)
 */
public class BadTaskCompletionException extends Exception {
  private int tasksSent;
  private int tasksCompleted;
  public BadTaskCompletionException(String message, int tasksSent, int tasksCompleted){
    super(message);
    this.tasksSent = tasksSent;
    this.tasksCompleted = tasksCompleted;
  }

  public int getTasksSent() {
    return tasksSent;
  }

  public int getTasksCompleted() {
    return tasksCompleted;
  }
}
