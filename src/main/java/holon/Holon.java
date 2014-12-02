package holon;

import holon.api.Application;
import holon.api.config.Config;
import holon.api.config.Setting;
import holon.api.exception.HolonException;
import holon.api.logging.Logging;
import holon.internal.HolonFactory;
import holon.internal.HolonFiles;
import holon.internal.config.JsonConfigParser;
import holon.internal.redeploy.HolonRedeploy;
import holon.spi.HolonEngine;
import holon.spi.Route;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import static holon.api.config.Setting.NO_DEFAULT;
import static holon.api.config.Setting.defaultValue;
import static holon.api.config.Setting.setting;
import static holon.api.config.SettingConverters.bool;
import static holon.api.config.SettingConverters.integer;
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
                setting( "application.name", string(), defaultValue( "holon" ) );

        public static Setting<Path> config_path =
                setting( "application.config_path", path(), NO_DEFAULT);

        public static Setting<Path> home_dir =
                setting( "application.home", path(), HolonFiles::defaultHome );

        public static Setting<Integer> workers =
                setting( "application.workers", integer(),
                        defaultValue( ""+(Runtime.getRuntime().availableProcessors() * 4 )) );

        public static Setting<Map<String, String>> endpoint_packages =
                setting( "application.endpoints", mapOf( string() ), HolonFiles::defaultEndpointPackages );

        public static Setting<Path> template_path =
                setting( "application.template_path", path(), HolonFiles::defaultTemplatePath );

        public static Setting<Path> static_path =
                setting( "application.public_files", path(), HolonFiles::defaultStaticPath );

        public static final Setting<Integer> http_port =
                setting( "application.http.port", integer(), defaultValue( "8080" ) );

        public static Setting<Boolean> auto_redeploy =
                setting( "application.auto_redeploy", bool(), defaultValue( "false" ) );
    }

    private final HolonEngine http;
    private final Config config;
    private final Logging logging;
    private final Supplier<Iterable<Route>> routes;

    public Holon( HolonEngine http, Config config, Logging logging, Supplier<Iterable<Route>> routes )
    {
        this.http = http;
        this.config = config;
        this.logging = logging;
        this.routes = routes;
    }

    public static void run( Class<? extends Application> applicationClass )
    {
        Config config = createConfiguration();

        if(config.get( Configuration.auto_redeploy ))
        {
            new HolonRedeploy(applicationClass, config, new File( findHomeDirectory(), "src/main/java" ) )
                    .runWithAutoRecompile();
        }
        else
        {
            Application application = null;
            try
            {
                application = applicationClass.newInstance();
                Collection<Object> injectables = application.startup( config );
                run( config, injectables.toArray(), (Class[])application.middleware().toArray() );
            }
            catch ( Exception e )
            {
                throw new HolonException( "Unable to launch Holon, failed to instantiate bootstrap class. " +
                        "Make sure your bootstrap class has a public no-arg constructor.", e);
            }
            finally
            {
                if(application != null)
                {
                    application.shutdown();
                }
            }
        }
    }

    public static void run( Object ... injectables )
    {
        run( createConfiguration(), injectables );
    }

    public static void run( Config config, Object ... injectables )
    {
        run(config, injectables, new Class[0]);
    }

    private static void run(Config config, Object[] injectables, Class[] globalMiddleware)
    {
        Holon holon = new HolonFactory().newHolon( config, injectables, globalMiddleware );
        holon.start();
        holon.join();
    }

    public void start()
    {
        logging.logger( "main" ).info( String.format("Launching '%s' [%s]!",
                config.get(Configuration.app_name), config.get(Configuration.home_dir ) ) );
        http.serve( routes );
    }

    public void join()
    {
        http.join();
    }

    public void stop()
    {
        http.shutdown();
    }

    public String httpUrl()
    {
        return "http://localhost:" + config.get( Configuration.http_port ); // TODO
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
}
