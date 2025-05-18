# BuildFormat
BuildFormat is a build management system for minecraft builds. Allows for streamline integration with your building
server and team to produce builds faster. All requirements for a build can be defined programmatically and checked
in game prior to being loaded.

## Markers
Markers define points in the build that are used for in your gamemode. They are set using a sign with the following format:
```
#marker
<name>
<offset>
```
Where:
- `#marker` is the prefix that tells the system this is a marker
- `<name>` is the name of the marker, this is used to identify the marker in the code
- `<offset>` is the offset from the marker to the point in the build. This can be useful to get more precise locations


## Example build definition
```java
public record TestFormat(
        @Requirement(name = "test-a") Marker single,
        @Requirement(startsWith = "test-b") List<Marker> startsWith,
        @Requirement(startsWith = "test-c", minimum = 2) List<Marker> minimum,
        @Requirement InnerTest inner
) implements BuildFormat {}
```

## Setting up the database
Flyway is used to manage the database, currently there is no user-friendly way to set this up.

1. Install the latest version of the Flyway CLI from https://flywaydb.org/download/
2. Navigate to the `postgres` directory
3. Run `flyway migrate` to create the database and tables
