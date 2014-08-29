package holon.util.scheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StandardScheduler implements Scheduler
{
    private final ExecutorService threadPool;

    public StandardScheduler()
    {
        threadPool = Executors.newCachedThreadPool();
    }

    @Override
    public void schedule( Job job )
    {
        threadPool.execute( job );
    }
}
