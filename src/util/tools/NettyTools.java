package util.tools;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import org.tinylog.Logger;

import java.util.concurrent.TimeUnit;

public class NettyTools {

    public static void updateReaderOnlyIdleTime(Channel channel, long newIdleTimeSeconds) {
        ChannelPipeline pipeline = channel.pipeline();

        // Get a reference to the existing IdleStateHandler
        ChannelHandler idleHandler = pipeline.get("idleStateHandler");

        if ( idleHandler instanceof IdleStateHandler) {
            // Create a new IdleStateHandler with the updated time
            IdleStateHandler newIdleHandler = new IdleStateHandler(newIdleTimeSeconds, 0, 0, TimeUnit.SECONDS);

            // Atomically replace the old handler with the new one
            pipeline.replace("idleStateHandler", "idleStateHandler", newIdleHandler);

            Logger.info("IdleStateHandler timeout updated to " + newIdleTimeSeconds + " seconds.");
        } else {
            Logger.info("IdleStateHandler not found in pipeline.");
        }
    }
}
