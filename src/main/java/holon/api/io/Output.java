package holon.api.io;

import java.io.Writer;
import java.nio.channels.SeekableByteChannel;

public interface Output
{
    Writer asWriter();

    void write( SeekableByteChannel channel );
}
