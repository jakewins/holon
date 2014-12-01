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
package holon.internal.http.netty.work;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;

import java.util.Map;

public class NettyWorkEvent
{
    private HttpRequest request;
    private Channel ch;
    private Map<String,Object> formParams;

    public void initialize( HttpRequest request, Channel ch, Map<String, Object> formParams )
    {
        this.request = request;
        this.ch = ch;
        this.formParams = formParams;
    }

    public HttpRequest request()
    {
        return request;
    }

    public Channel channel()
    {
        return ch;
    }

    public Map<String,Object> formParams()
    {
        return formParams;
    }
}
