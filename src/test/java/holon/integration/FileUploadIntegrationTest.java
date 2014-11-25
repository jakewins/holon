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
package holon.integration;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import holon.api.http.FormParam;
import holon.api.http.POST;
import holon.api.http.Request;
import holon.api.http.Status;
import holon.api.http.UploadedFile;
import holon.util.HTTP;
import holon.util.HolonRule;
import holon.util.io.FileTools;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static holon.util.HTTP.form;
import static holon.util.collection.Maps.map;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileUploadIntegrationTest
{
    @Rule
    public HolonRule holon = new HolonRule(Endpoint.class);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static AtomicReference<File> uploadTo = new AtomicReference<>();
    private static AtomicReference<File> uploadedFile = new AtomicReference<>();
    private static AtomicReference<String> fileName = new AtomicReference<>();
    private static AtomicReference<String> fileType = new AtomicReference<>();

    public static class Endpoint
    {
        @POST("/")
        public void recieveFile( Request req, @FormParam("file") UploadedFile file )
        {
            File dest = new File( uploadTo.get(), "uploaded.file" );
            assertTrue( file.file().renameTo( dest ) );

            uploadedFile.set( dest );
            fileName.set( file.fileName() );
            fileType.set( file.contentType() );
            req.respond( Status.Code.OK );
        }
    }

    @Test
    public void shouldUploadFile() throws Exception
    {
        // Given
        uploadTo.set( folder.getRoot() );
        File fileToUpload = folder.newFile();

        FileTools.write( fileToUpload, "Hello, World!", Charset.forName( "UTF-8" ) );

        // When
        HTTP.Response response = HTTP.POST( holon.httpUrl(), form( map( "file", fileToUpload ) ) );

        // Then
        assertThat(response.status(), equalTo(200));
        assertThat(FileTools.read( uploadedFile.get(), Charset.forName( "UTF-8" ) ), equalTo("Hello, World!") );
        assertThat(fileName.get(), equalTo(fileToUpload.getName()));
    }
}
