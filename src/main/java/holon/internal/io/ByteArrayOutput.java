package holon.internal.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;

import holon.api.exception.HolonException;
import holon.api.io.Output;

/**
 * In-memory output backed by a byte array.
 */
public class ByteArrayOutput implements Output
{
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(128);

    @Override
    public Writer asWriter()
    {
        return new OutputStreamWriter( baos, Charset.forName("UTF-8") );
    }

    @Override
    public void write( SeekableByteChannel channel )
    {
        try
        {
            ByteBuffer buffer = ByteBuffer.allocate( (int) channel.size() );
            channel.read( buffer );
            baos.write( buffer.array(), 0, (int) channel.size() );
        }
        catch(IOException e)
        {
            throw new HolonException( "Unable to read file.", e );
        }
    }

    public byte[] toByteArray()
    {
        return baos.toByteArray();
    }
}
