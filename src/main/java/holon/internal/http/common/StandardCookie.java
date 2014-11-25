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
package holon.internal.http.common;

import java.util.Date;

import holon.api.http.Cookie;

public class StandardCookie implements Cookie
{
    private String name;
    private String value;
    private String domain;
    private String path;
    private int maxAge;
    private boolean secure = false;
    private boolean httpOnly = false;

    public StandardCookie( String name, String value, String domain, String path, Date expiryDate )
    {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
    }

    public StandardCookie( String name, String value, String domain, String path, int maxAge, boolean secure,
                           boolean httpOnly )
    {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.maxAge = maxAge;
        this.secure = secure;
        this.httpOnly = httpOnly;
    }

    public StandardCookie( String name, String value )
    {
        this.name = name;
        this.value = value;
    }

    @Override
    public String value()
    {
        return value;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public String path()
    {
        return path;
    }

    @Override
    public String domain()
    {
        return domain;
    }

    @Override
    public int maxAge()
    {
        return maxAge;
    }

    @Override
    public boolean secure()
    {
        return secure;
    }

    @Override
    public boolean httpOnly()
    {
        return httpOnly;
    }
}
