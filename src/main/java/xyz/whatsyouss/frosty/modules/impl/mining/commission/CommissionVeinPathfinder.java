package xyz.whatsyouss.frosty.modules.impl.mining.commission;

import net.minecraft.core.BlockPos;
import xyz.whatsyouss.frosty.utility.commission.pathfinder.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommissionVeinPathfinder {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "frosty-commission-pathfinder");
        thread.setDaemon(true);
        return thread;
    });

    CompletableFuture<Path> find(BlockPos start, BlockPos end) {
        return CompletableFuture.supplyAsync(() -> {
            CalculationContext context = new CalculationContext(0.13, 0.10, 0.03);
            Goal goal = new Goal(end.getX(), end.getY(), end.getZ(), context);

            if (start.getY() == end.getY() && BlockUtil.canWalkBetween(context, start, end)) {
                PathNode first = new PathNode(start.getX(), start.getY(), start.getZ(), goal);
                PathNode last = new PathNode(end.getX(), end.getY(), end.getZ(), goal);
                last.parentNode = first;
                return new Path(first, last, goal, context);
            }

            Path path = new AStarPathFinder(start.getX(), start.getY(), start.getZ(), goal, context).calculatePath();
            return path;
        }, EXECUTOR);
    }
}
