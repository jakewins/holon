/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package holon.util;

import holon.api.exception.HolonException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest
{

    public static final Charset UTF_8 = Charset.forName( "UTF-8" );

    public static String md5(String input)
    {
        try
        {
            MessageDigest digester = MessageDigest.getInstance( "MD5" );
            digester.update( input.getBytes( UTF_8 ) );
            return bytesToHex( digester.digest() );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new HolonException( "Cannot create MD5 digests, because the platform does not support MD5" );
        }
    }

    public static String md5(FileChannel input) throws IOException
    {
        try
        {
            MessageDigest digester = MessageDigest.getInstance( "MD5" );
            input.position(0);
            ByteBuffer bb = ByteBuffer.allocateDirect( 1024 * 4 );
            while(input.read( bb ) != -1)
            {
                digester.update( bb );
                bb.clear();
            }
            return bytesToHex(digester.digest());
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new HolonException( "Cannot create MD5 digests, because the platform does not support MD5" );
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
