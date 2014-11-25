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
package holon.util.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

public class Ports
{
    public static int findUnusedPort()
    {
        Random rand = new Random();
        int port = 8080;
        for (int i=1000; i --> 0 ;) {
            try {
                ServerSocket serverSocket = new ServerSocket( port );
                serverSocket.close();
                return port;
            } catch (IOException ex) {}
            port = 1024 + rand.nextInt( 10_000 );
        }

        throw new RuntimeException("no free port found");
    }
}
