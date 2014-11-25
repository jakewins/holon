package holon.internal.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import holon.api.config.Config;
import holon.api.config.Setting;
import holon.spi.HolonEngine;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static holon.api.config.Setting.NO_DEFAULT;
import static holon.api.config.Setting.defaultValue;
import static holon.api.config.Setting.setting;
import static holon.api.config.SettingConverters.listOf;
import static holon.api.config.SettingConverters.mapOf;
import static holon.api.config.SettingConverters.path;
import static holon.api.config.SettingConverters.string;
import static holon.util.collection.Maps.map;

public class JsonConfigTest
{
    public static class Configuration
    {
        public static Setting<List<String>> list =
                setting( "application.somelist", listOf( string() ), defaultValue( Arrays.asList("one", "two") ) );


        public static Setting<Map<String, Path>> map_path =
                setting( "application.template_paths", mapOf( path() ), NO_DEFAULT );

        public static Setting<String> string =
                setting( "application.http_engine", string(),
                        defaultValue( HolonEngine.class.getCanonicalName() ));
    }

    @Test
    public void shouldLoadJsonConfig() throws Exception
    {
        // Given
        Config config = new JsonConfigParser().parse( getClass().getClassLoader().getResource( "config/simple.json" ) );

        // When & then
        Assert.assertThat( config.get( Configuration.string ), CoreMatchers.equalTo( "myEngine" ) );
        Assert.assertThat( config.get( Configuration.map_path ), CoreMatchers.equalTo( map( "/",
                Paths.get( "eatlocal.frontend.application" ) ) ) );
        Assert.assertThat( config.get( Configuration.list ), CoreMatchers.equalTo( Arrays.asList( "one", "two" ) ) );
    }

}
