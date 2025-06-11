# BuildFormat
BuildFormat is a build management system for minecraft builds. Allows for streamline integration with your building
server and team to produce builds faster. All requirements for a buildMeta can be defined programmatically and checked
in game prior to being loaded.

## Markers
Markers define points in the buildMeta that are used for in your gamemode. They are set using a sign with the following format:
```
#marker
<name>
<offset>
```
Where:
- `#marker` is the prefix that tells the system this is a marker
- `<name>` is the name of the marker, this is used to identify the marker in the code
- `<offset>` is the offset from the marker to the point in the buildMeta. This can be useful to get more precise locations


## Example buildMeta definition
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

## Building and Releasing

This project uses GitHub Actions to automate the build and release process. The workflow will build all modules and create a GitHub release with the generated JAR files.

### Automatic Releases

To create a new release:

1. Tag your commit with a version number: `git tag v1.0.5`
2. Push the tag to GitHub: `git push origin v1.0.5`

The GitHub Actions workflow will automatically build the project and create a release with all JAR files.

### Manual Releases

You can also trigger a manual build and release:

1. Go to the "Actions" tab in your GitHub repository
2. Select the "Build and Release" workflow
3. Click "Run workflow"
4. Optionally provide a custom release name
5. Click "Run workflow" to start the build process

The workflow will build the project and create a release with all JAR files.
