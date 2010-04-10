package org.apache.maven.archetype.source;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * @plexus.component role-hint="remote-catalog"
 * @author Jason van Zyl
 */
public class RemoteCatalogArchetypeDataSource
    extends CatalogArchetypeDataSource
{
    /** @plexus.requirement */
    private WagonManager wagonManager;

    public static final String REPOSITORY_PROPERTY = "repository";

    public ArchetypeCatalog getArchetypeCatalog( Properties properties )
        throws ArchetypeDataSourceException
    {
        String repository = properties.getProperty( REPOSITORY_PROPERTY );

        if ( repository == null )
        {
            throw new ArchetypeDataSourceException( "To use the remote catalog you must specify the 'repository' property with an URL." );
        }

        try
        {
            if ( repository.endsWith( "/" ) )
            {
                repository = repository.substring( 0, repository.length() - 1 );
            }

            getLogger().debug( "Searching for remote catalog: " + repository + "/archetype-catalog.xml" );
            // We use wagon to take advantage of a Proxy that has already been setup in a Maven environment.
            Repository wagonRepository = new Repository( "archetype", repository );
            Wagon wagon = wagonManager.getWagon( wagonRepository );
            File catalog = File.createTempFile( "archetype-catalog", ".xml" );
            try
            {
                wagon.connect( wagonRepository );
                wagon.get( "archetype-catalog.xml", catalog );
            }
            finally
            {
                disconnectWagon( wagon );
            }
            return readCatalog( ReaderFactory.newXmlReader( catalog ) );
        }
        catch ( ArchetypeDataSourceException e )
        {
            throw e;
        }
        catch ( Exception e )
        { // When the default archetype catalog names doesn't works, we assume the repository is the URL to a file
            try
            {
                String repositoryPath = repository.substring( 0, repository.lastIndexOf( "/" ) );
                String fileName = repository.substring( repository.lastIndexOf( "/" ) + 1 );

                getLogger().debug( "Searching for remote catalog: " + repositoryPath + "/" + fileName );
                // We use wagon to take advantage of a Proxy that has already been setup in a Maven environment.
                Repository wagonRepository = new Repository( "archetype", repositoryPath );
                Wagon wagon = wagonManager.getWagon( wagonRepository );
                File catalog = File.createTempFile( "archetype-catalog", ".xml" );
                try
                {
                    wagon.connect( wagonRepository );
                    wagon.get( fileName, catalog );
                }
                finally
                {
                    disconnectWagon( wagon );
                }
                return readCatalog( ReaderFactory.newXmlReader( catalog ) );
            }
            catch ( Exception ex )
            {
                getLogger().warn( "Error reading archetype catalog " + repository, ex );
                return new ArchetypeCatalog();
            }
        }
    }

    public List getArchetypes( Properties properties )
        throws ArchetypeDataSourceException
    {
        String repository = properties.getProperty( REPOSITORY_PROPERTY );

        if ( repository == null )
        {
            throw new ArchetypeDataSourceException( "To use the remote catalog you must specify the 'remote-catalog.repository' property correctly in your ~/.m2/archetype-catalog.properties file." );
        }

        try
        {
            if ( repository.endsWith( "/" ) )
            {
                repository = repository.substring( 0, repository.length() - 1 );
            }

            // We use wagon to take advantage of a Proxy that has already been setup in a Maven environment.

            Repository wagonRepository = new Repository( "archetype", repository );

            Wagon wagon = wagonManager.getWagon( wagonRepository );

            File catalog = File.createTempFile( "archetype-catalog", ".xml" );

            wagon.connect( wagonRepository );

            wagon.get( "archetype-catalog.xml", catalog );

            wagon.disconnect();

            return createArchetypeMap( readCatalog( ReaderFactory.newXmlReader( catalog ) ) );
        }
        catch ( Exception e )
        {
            throw new ArchetypeDataSourceException( "Error reading archetype registry.", e );
        }
    }

    public void updateCatalog( Properties properties, Archetype archetype )
        throws ArchetypeDataSourceException
    {
        throw new ArchetypeDataSourceException( "Not supported yet." );
    }

    private void disconnectWagon( Wagon wagon )
    {
        try
        {
            wagon.disconnect();
        }
        catch ( Exception e )
        {
            getLogger().warn( "Problem disconnecting from wagon - ignoring: " + e.getMessage() );
        }
    }

}