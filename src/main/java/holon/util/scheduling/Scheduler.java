package holon.util.scheduling;

public interface Scheduler
{
    interface Job extends Runnable
    {
        void stop();
    }

    void schedule( Job job );
}
