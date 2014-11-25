package holon.api.http;

import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;

public interface Output
{
    Writer asWriter();

    void write( FileChannel channel ) throws IOException;
}
