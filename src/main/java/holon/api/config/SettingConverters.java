package holon.api.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SettingConverters
{

    public static <T> Function<Iterable<Object>, List<T>> listOf( Function<? extends Object, T> itemConverter )
    {
        return ( Iterable<Object> value ) -> {
            List<T> out = new ArrayList<>();
            value.forEach( (val) -> out.add( (T) ((Function)itemConverter).apply( val ) ));
            return out;
        };
    }

    public static <T> Function<Map<String, Object>, Map<String, T>> mapOf( Function<? extends Object, T> itemConverter )
    {
        return ( Map<String, Object> value ) -> {
            Map<String, T> out = new HashMap<>();
            value.forEach( (String key, Object val) -> out.put( key, (T) ((Function)itemConverter).apply( val ) ));
            return out;
        };
    }

    public static Function<String, Path> path()
    {
        return Paths::get;
    }

    public static Function<String, String> string()
    {
        return str -> str;
    }

    public static Function<String, Integer> integer()
    {
        return Integer::valueOf;
    }

    public static Function<String, Boolean> bool()
    {
        return Boolean::valueOf;
    }
}
