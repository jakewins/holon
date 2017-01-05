# Holon

A fast JVM HTTP application server with an annotation-based API.

Key design drivers are:

- Latency & scaling throughput linearly with cores
- Ease of use

## Example

    @GET("/hello/{name}")
    public void lessSpecific( Request req, @PathParam('name') String name ) {
        req.respond(Status.Code.OK);
    }

For now, see `integration` for examples and `holon.api.http.*` for API.
Docs contributions very welcome!

## Features

- Annotation-based API with support for middlewares and dependency injection
- Tree-based HTTP router is allocation-free and performant
- Minimal coordination uses all available cores under load

## Included add-ons

- JSON serialization and de-serialization
- Caching using memory-mapped pages allows zero-copy responses managed directly by OS, bypassing the JVM
- HTTP sessions
- Mustache templates
- Static files

## Contributing

Contributions are super welcome. However, if you are doing anything other than minor polish or small bugfixes, *please* first open a ticket to discuss the approach. Having to decline good contributions because they are not in line with project goals sucks.

As you can tell, the project is very short on good documentation. There are also
notable middleware items missing, like gzip compression. See the `contrib` package
of other middleware for inspiration!

## License

See LICENSE
