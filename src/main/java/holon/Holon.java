package holon;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import holon.api.Bootstrap;
import holon.api.config.Config;
import holon.api.config.Setting;
import holon.api.exception.HolonException;
import holon.api.logging.Logging;
import holon.internal.HolonFactory;
import holon.internal.HolonFiles;
import holon.internal.config.JsonConfigParser;
import holon.internal.di.Components;
import holon.internal.http.common.StaticContentRoute;
import holon.internal.http.netty.Netty5Engine;
import holon.internal.redeploy.HolonRedeploy;
import holon.internal.routing.annotated.RouteCompiler;
import holon.internal.routing.annotated.RouteScanner;
import holon.spi.HolonEngine;
import holon.spi.Route;
import holon.util.scheduling.StandardScheduler;

import static holon.api.config.Setting.NO_DEFAULT;
import static holon.api.config.Setting.defaultValue;
import static holon.api.config.Setting.setting;
import static holon.api.config.SettingConverters.bool;
import static holon.api.config.SettingConverters.mapOf;
import static holon.api.config.SettingConverters.path;
import static holon.api.config.SettingConverters.string;
import static holon.internal.HolonFiles.DEFAULT_CONFIG_NAME;
import static holon.internal.HolonFiles.findHomeDirectory;

public class Holon
{
    public static class Configuration
    {
        public static Setting<String> app_name =
                setting( "application.name", string(), NO_DEFAULT );

        public static Setting<Path> config_path =
                setting( "application.config_path", path(), NO_DEFAULT);

        public static Setting<Path> home_dir =
                setting( "application.home", path(), HolonFiles::defaultHome );

        public static Setting<Map<String, String>> endpoint_packages =
                setting( "application.endpoints", mapOf( string() ), HolonFiles::defaultEndpointPackages );

        public static Setting<Path> template_path =
                setting( "application.template_path", path(), HolonFiles::defaultTemplatePath );

        public static Setting<Path> static_path =
                setting( "application.public_files", path(), HolonFiles::defaultStaticPath );

        public static Setting<String> http_engine =
                setting( "application.http_engine", string(),
                         defaultValue( Netty5Engine.class.getCanonicalName() ));

        public static Setting<Boolean> auto_redeploy =
                setting( "application.auto_redeploy", bool(), defaultValue( "true" ) );
    }

    private final Components components;
    private final HolonEngine http;
    private final Config config;
    private final Logging logging;
    private final StandardScheduler scheduler;

    public Holon( HolonEngine http, Components components, Config config, Logging logging )
    {
        this.http = http;
        this.config = config;
        this.logging = logging;
        this.components = components;
        this.scheduler = new StandardScheduler();
    }

    public static void run( Class<? extends Bootstrap> bootstrapClass )
    {
        Config config = createConfiguration();

        if(config.get( Configuration.auto_redeploy ))
        {
            new HolonRedeploy(bootstrapClass, config, new File( findHomeDirectory(), "src/main/java" ) )
                    .runWithAutoRecompile();
        }
        else
        {
            try
            {
                run(bootstrapClass.newInstance().bootstrap());
            }
            catch ( InstantiationException |  IllegalAccessException e )
            {
                throw new HolonException( "Unable to launch Holon, failed to instantiate bootstrap class. " +
                        "Make sure your bootstrap class has a public no-arg constructor.", e);
            }
        }
    }

    public static void run( Object ... injectables )
    {
        run( createConfiguration(), injectables );
    }

    public static void run( Config config, Object ... injectables )
    {
        Holon holon = new HolonFactory().newHolon( config, injectables );
        holon.start();
        holon.join();
    }

    public void start()
    {
        logging.logger( "main" ).info( String.format("Launching '%s' [%s]!",
                config.get(Configuration.app_name), config.get(Configuration.home_dir ) ) );
        http.serve( loadRoutes() );
    }

    public void join()
    {
        http.join();
    }

    public void stop()
    {
        http.shutdown();
    }

    private static Config createConfiguration()
    {
        try
        {
            File configPath = new File( findHomeDirectory(), DEFAULT_CONFIG_NAME );
            Config config = new JsonConfigParser().parse( configPath.toURI().toURL() );
            config.set( Configuration.config_path, configPath.getAbsolutePath() );
            return config;
        }
        catch(MalformedURLException e)
        {
            throw new HolonException( "Unable to launch Holon, failed to set up directory and config paths.", e);
        }
    }

    private List<Route> loadRoutes()
    {
        List<Route> routes = new ArrayList<>();
        RouteScanner routeScanner = new RouteScanner( new RouteCompiler( components ) );
        config.get( Configuration.endpoint_packages ).forEach( ( path, pkg ) ->
                routeScanner.scan( path, pkg, routes::add) );
        routes.add( new StaticContentRoute(config.get( Configuration.static_path ), scheduler) );
        return routes;
    }
}
